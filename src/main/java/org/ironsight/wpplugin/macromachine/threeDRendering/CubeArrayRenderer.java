package org.ironsight.wpplugin.macromachine.threeDRendering;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.function.Supplier;
import org.ironsight.cubearray.render.CubeSetup;
import org.ironsight.cubearray.render.RendererService;
import org.ironsight.cubearray.schematic.SchemReader;
import org.pepsoft.worldpainter.objects.WPObject;

public class CubeArrayRenderer
{
    private static final System.Logger LOGGER = System.getLogger(CubeArrayRenderer.class.getName());

    // The startup worker initializes this holder without loading CubeArray on the
    // EDT.
    static class Holder
    {
        static final CubeArrayRenderer INSTANCE = createRenderer();
    }

    final Function<WPObject, CubeSetup> prepare;
    final Function<CubeSetup, CompletableFuture<Void>> warm;
    final Function<CubeSetup, CompletableFuture<Void>> show;
    final Function<CubeSetup, CompletableFuture<Void>> showOrReplace;
    final Supplier<CompletableFuture<Void>> stop;
    final CompletableFuture<Void> ready = new CompletableFuture<>();
    final CompletableFuture<Void> terminated = new CompletableFuture<>();
    Thread worker;
    Request pending;
    Request active;
    long latestId;
    boolean stopping;
    Throwable failure;
    CubeSetup lastCubeSetup;
    CubeSetup welcomeSetup;

    CubeArrayRenderer(Function<WPObject, CubeSetup> prepare, Function<CubeSetup, CompletableFuture<Void>> warm,
            Function<CubeSetup, CompletableFuture<Void>> show,
            Function<CubeSetup, CompletableFuture<Void>> showOrReplace, Supplier<CompletableFuture<Void>> stop) {
        this.prepare = prepare;
        this.warm = warm;
        this.show = show;
        this.showOrReplace = showOrReplace;
        this.stop = stop;
    }

    static CubeArrayRenderer createRenderer() {
        RendererService service = RendererService.getInstance();
        CubeArrayRenderer renderer = new CubeArrayRenderer(CubeArrayRenderer::prepareObject, service::warmUp,
                service::show, service::showOrReplace, service::shutdownAsync);
        Runtime.getRuntime().addShutdownHook(new Thread(renderer::shutdownOnExit, "3D-preview-shutdown"));
        return renderer;
    }

    public static CubeArrayRenderer getInstance() {
        return Holder.INSTANCE;
    }

    public synchronized CompletableFuture<Void> warmUp() {
        if (stopping)
            return CompletableFuture.failedFuture(rejection());
        ensureStarted();
        return ready.copy();
    }

    public CompletableFuture<RenderOutcome> render(WPObject object) {
        return render(() -> object);
    }

    public CompletableFuture<RenderOutcome> render(Supplier<? extends WPObject> prepareObject) {
        Objects.requireNonNull(prepareObject);
        return submit(prepareObject, null, RequestKind.TERRAIN);
    }

    public synchronized CompletableFuture<RenderOutcome> openPreview() {
        if (stopping)
            return CompletableFuture.failedFuture(rejection());
        LOGGER.log(System.Logger.Level.INFO, "Opening 3D preview (renderer ready: {0})",
                ready.isDone() && !ready.isCompletedExceptionally());
        // Opening is lower priority than rendering and must not replay an older scene.
        if (pending != null)
            return pending.result.copy();
        if (active != null && !active.result.isDone() && current(active))
            return active.result.copy();
        return submit(null, null, RequestKind.OPEN);
    }

    CompletableFuture<RenderOutcome> submit(Supplier<? extends WPObject> prepareObject, CubeSetup setup,
            RequestKind kind) {
        Request replaced;
        Request request;
        synchronized (this) {
            if (stopping)
                return CompletableFuture.failedFuture(rejection());
            request = new Request(++latestId, prepareObject, setup, kind, new CompletableFuture<>());
            replaced = pending;
            pending = request;
            ensureStarted();
            notifyAll();
        }
        // Complete outside the monitor: observers may call back into this coordinator.
        if (replaced != null)
            replaced.result.complete(RenderOutcome.SUPERSEDED);
        return request.result.copy();
    }

    public CompletableFuture<RenderOutcome> reRender() {
        CubeSetup setup;
        synchronized (this) {
            setup = lastCubeSetup;
        }
        if (setup == null)
            return CompletableFuture.completedFuture(RenderOutcome.SUPERSEDED);
        return submit(null, setup, RequestKind.TERRAIN);
    }

    public CompletableFuture<Void> shutdownAsync() {
        Request discarded;
        Request running;
        boolean neverStarted;
        synchronized (this) {
            if (stopping)
                return terminated.copy();
            stopping = true;
            discarded = pending;
            pending = null;
            running = active;
            neverStarted = worker == null;
            notifyAll();
            if (worker != null)
                worker.interrupt();
        }
        CancellationException cancelled = new CancellationException("3D preview has shut down");
        ready.completeExceptionally(cancelled);
        if (discarded != null)
            discarded.result.completeExceptionally(cancelled);
        if (running != null)
            running.result.completeExceptionally(cancelled);
        if (neverStarted)
            finishShutdown();
        return terminated.copy();
    }

    void ensureStarted() {
        if (worker == null) {
            worker = new Thread(this::runWorker, "3D-preview-preparation");
            worker.setDaemon(true);
            worker.setPriority(Thread.MIN_PRIORITY);
            worker.start();
        }
    }

    void runWorker() {
        try {
            welcomeSetup = prepare.apply(PreviewScene.welcomeObject());
            warm.apply(welcomeSetup).get();
            ready.complete(null);
            Request request;
            while ((request = takeLatest()) != null) {
                process(request);
                // Release captured tile snapshots before waiting for another request.
                request = null;
            }
        } catch (InterruptedException e) {
            if (!isStopping())
                failRenderer(e);
            Thread.currentThread().interrupt();
        } catch (Throwable e) {
            failRenderer(unwrap(e));
        } finally {
            finishShutdown();
        }
    }

    synchronized Request takeLatest() throws InterruptedException {
        while (!stopping && pending == null)
            wait();
        if (stopping)
            return null;
        active = pending;
        pending = null;
        return active;
    }

    void process(Request request) throws InterruptedException {
        try {
            if (!current(request)) {
                request.result.complete(RenderOutcome.SUPERSEDED);
                return;
            }
            CubeSetup setup = prepareRequest(request);
            if (!current(request)) {
                request.result.complete(RenderOutcome.SUPERSEDED);
                return;
            }
            CompletableFuture<Void> submitted;
            synchronized (this) {
                if (!current(request)) {
                    submitted = null;
                } else {
                    submitted = request.kind == RequestKind.TERRAIN && lastCubeSetup == null
                            ? show.apply(setup)
                            : showOrReplace.apply(setup);
                }
            }
            if (submitted == null) {
                request.result.complete(RenderOutcome.SUPERSEDED);
                return;
            }
            submitted.get();
            synchronized (this) {
                if (request.kind == RequestKind.TERRAIN)
                    lastCubeSetup = setup;
            }
            request.result.complete(RenderOutcome.SUBMITTED);
        } catch (InterruptedException e) {
            request.result.completeExceptionally(e);
            throw e;
        } catch (Error e) {
            request.result.completeExceptionally(e);
            throw e;
        } catch (Exception e) {
            Throwable cause = unwrap(e);
            if (current(request))
                request.result.completeExceptionally(cause);
            else
                request.result.complete(RenderOutcome.SUPERSEDED);
            LOGGER.log(System.Logger.Level.WARNING, "3D preview request failed", cause);
        } finally {
            synchronized (this) {
                active = null;
            }
        }
    }

    CubeSetup prepareRequest(Request request) {
        if (request.kind == RequestKind.OPEN)
            return lastCubeSetup != null ? lastCubeSetup : welcomeSetup;
        if (request.setup != null)
            return request.setup;
        WPObject object = request.object.get();
        if (!current(request))
            return null;
        return prepare.apply(Objects.requireNonNull(object, "Preview object cannot be null"));
    }

    synchronized boolean current(Request request) {
        return !stopping && request.id == latestId;
    }

    synchronized boolean isStopping() {
        return stopping;
    }

    void failRenderer(Throwable cause) {
        Request discarded;
        Request running;
        synchronized (this) {
            failure = cause;
            stopping = true;
            discarded = pending;
            pending = null;
            running = active;
            active = null;
        }
        ready.completeExceptionally(cause);
        if (discarded != null)
            discarded.result.completeExceptionally(cause);
        if (running != null)
            running.result.completeExceptionally(cause);
        LOGGER.log(System.Logger.Level.ERROR, "3D preview renderer failed", cause);
    }

    Throwable rejection() {
        return failure != null ? failure : new IllegalStateException("3D preview has shut down");
    }

    void finishShutdown() {
        synchronized (this) {
            stopping = true;
            lastCubeSetup = null;
            welcomeSetup = null;
        }
        try {
            stop.get().whenComplete(this::completeShutdown);
        } catch (Throwable error) {
            terminated.completeExceptionally(error);
        }
    }

    void completeShutdown(Void unused, Throwable error) {
        if (error == null)
            terminated.complete(null);
        else
            terminated.completeExceptionally(unwrap(error));
    }

    void shutdownOnExit() {
        try {
            shutdownAsync().get(3, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            LOGGER.log(System.Logger.Level.WARNING, "3D preview shutdown did not complete", e);
        }
    }

    static CubeSetup prepareObject(WPObject object) {
        try {
            SchemReader.preloadSynchronously();
            return SchemReader.prepareData(List.of(object));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to prepare 3D preview data", e);
        }
    }

    static Throwable unwrap(Throwable error) {
        return (error instanceof ExecutionException || error instanceof CompletionException) && error.getCause() != null
                ? error.getCause()
                : error;
    }

    public enum RenderOutcome {
        SUBMITTED, SUPERSEDED
    }

    enum RequestKind {
        OPEN, TERRAIN
    }

    record Request(long id, Supplier<? extends WPObject> object, CubeSetup setup, RequestKind kind,
            CompletableFuture<RenderOutcome> result) {
    }
}
