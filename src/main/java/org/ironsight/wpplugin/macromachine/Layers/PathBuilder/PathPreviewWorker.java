package org.ironsight.wpplugin.macromachine.Layers.PathBuilder;

import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;

final class PathPreviewWorker implements AutoCloseable
{
    interface Calculation
    {
        Map<PathPreviewMask.TilePosition, BitSet> calculate(List<PathPreviewMask.Sample> samples,
                List<PathPreviewMask.TilePosition> tiles, float multiplier, CrossSectionShape profile,
                BooleanSupplier cancelled);
    }

    record Request(long revision, List<PathPreviewMask.Sample> samples, List<PathPreviewMask.TilePosition> tiles,
            float multiplier, CrossSectionShape profile, Consumer<Map<PathPreviewMask.TilePosition, BitSet>> callback) {
    }

    static final System.Logger LOGGER = System.getLogger(PathPreviewWorker.class.getName());
    final Executor executor;
    final Executor dispatcher;
    final Calculation calculation;
    final ExecutorService ownedExecutor;
    volatile long revision;
    boolean closed;
    boolean draining;
    Request pending;

    PathPreviewWorker() {
        this(Executors.newSingleThreadExecutor(task -> {
            var thread = new Thread(task, "path-preview");
            thread.setDaemon(true);
            return thread;
        }));
    }

    PathPreviewWorker(ExecutorService executor) {
        this(executor, SwingUtilities::invokeLater, PathPreviewMask::calculate, executor);
    }

    PathPreviewWorker(Executor executor, Executor dispatcher, Calculation calculation) {
        this(executor, dispatcher, calculation, null);
    }

    PathPreviewWorker(Executor executor, Executor dispatcher, Calculation calculation, ExecutorService ownedExecutor) {
        this.executor = executor;
        this.dispatcher = dispatcher;
        this.calculation = calculation;
        this.ownedExecutor = ownedExecutor;
    }

    synchronized void submit(List<PathPreviewMask.Sample> samples, List<PathPreviewMask.TilePosition> tiles,
            float multiplier, CrossSectionShape profile, Consumer<Map<PathPreviewMask.TilePosition, BitSet>> callback) {
        if (closed)
            throw new IllegalStateException("Path preview worker is closed");
        pending = new Request(++revision, List.copyOf(samples), List.copyOf(tiles), multiplier, profile, callback);
        if (!draining) {
            draining = true;
            try {
                executor.execute(this::drain);
            } catch (RuntimeException e) {
                draining = false;
                pending = null;
                throw e;
            }
        }
    }

    synchronized void invalidate() {
        revision++;
        pending = null;
    }

    @Override
    public synchronized void close() {
        if (closed)
            return;
        closed = true;
        invalidate();
        if (ownedExecutor != null)
            ownedExecutor.shutdown();
    }

    void drain() {
        while (true) {
            Request request;
            synchronized (this) {
                request = pending;
                pending = null;
                if (request == null) {
                    draining = false;
                    return;
                }
            }
            try {
                var result = calculation.calculate(request.samples(), request.tiles(), request.multiplier(),
                        request.profile(), () -> revision != request.revision());
                if (revision == request.revision())
                    dispatcher.execute(() -> publish(request, result));
            } catch (CancellationException e) {
                // A newer request or invalidation made this calculation obsolete.
            } catch (RuntimeException e) {
                LOGGER.log(System.Logger.Level.ERROR, "Path preview calculation failed", e);
            }
        }
    }

    void publish(Request request, Map<PathPreviewMask.TilePosition, BitSet> result) {
        synchronized (this) {
            if (closed || revision != request.revision())
                return;
        }
        request.callback().accept(result);
    }
}
