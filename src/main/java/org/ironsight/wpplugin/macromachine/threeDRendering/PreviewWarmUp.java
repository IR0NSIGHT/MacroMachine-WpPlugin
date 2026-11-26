package org.ironsight.wpplugin.macromachine.threeDRendering;

import java.awt.GraphicsEnvironment;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public final class PreviewWarmUp
{
    private static final System.Logger LOGGER = System.getLogger(PreviewWarmUp.class.getName());
    static final PreviewWarmUp STARTUP = new PreviewWarmUp(PreviewWarmUp::warmRenderer);

    final Supplier<CompletableFuture<Void>> warmUp;
    final CompletableFuture<Void> completion = new CompletableFuture<>();
    boolean started;

    PreviewWarmUp(Supplier<CompletableFuture<Void>> warmUp) {
        this.warmUp = warmUp;
    }

    public static void startInBackground() {
        if (!GraphicsEnvironment.isHeadless())
            STARTUP.start();
    }

    synchronized CompletableFuture<Void> start() {
        if (!started) {
            started = true;
            // Renderer class loading and native initialization must never run on the EDT.
            Thread worker = new Thread(this::runWarmUp, "3D-preview-startup");
            worker.setDaemon(true);
            worker.setPriority(Thread.MIN_PRIORITY);
            worker.start();
        }
        return completion.copy();
    }

    void runWarmUp() {
        long start = System.nanoTime();
        LOGGER.log(System.Logger.Level.INFO, "Starting hidden 3D preview warm-up");
        try {
            warmUp.get().whenComplete((ignored, error) -> finishWarmUp(start, error));
        } catch (Throwable error) {
            finishWarmUp(start, error);
        }
    }

    void finishWarmUp(long start, Throwable error) {
        long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        if (error != null) {
            LOGGER.log(System.Logger.Level.ERROR, "3D preview warm-up failed after " + elapsed + " ms", error);
            completion.completeExceptionally(error);
            return;
        }
        LOGGER.log(System.Logger.Level.INFO, "Hidden 3D preview ready after {0} ms", elapsed);
        completion.complete(null);
    }

    static CompletableFuture<Void> warmRenderer() {
        return CubeArrayRenderer.getInstance().warmUp();
    }
}
