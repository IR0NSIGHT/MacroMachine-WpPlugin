package org.ironsight.wpplugin.macromachine.Layers.PathBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;

class PathPreviewWorkerTest
{
    static class ManualExecutor implements Executor
    {
        final ArrayDeque<Runnable> tasks = new ArrayDeque<>();
        public void execute(Runnable task) {
            tasks.add(task);
        }
        void runNext() {
            tasks.remove().run();
        }
    }

    static final CrossSectionShape PROFILE = new CrossSectionShape("linear", "") {
    };
    static final List<PathPreviewMask.TilePosition> TILES = List.of(new PathPreviewMask.TilePosition(0, 0));

    void submit(PathPreviewWorker worker, int value, List<Integer> published) {
        worker.submit(List.of(new PathPreviewMask.Sample(value, 0, 1)), TILES, 1, PROFILE,
                result -> published.add(result.keySet().iterator().next().x()));
    }

    Map<PathPreviewMask.TilePosition, BitSet> result(int value) {
        return Map.of(new PathPreviewMask.TilePosition(value, 0), new BitSet());
    }

    @Test
    void runningRequestIsCancelledAndOnlyLatestPendingRuns() {
        var executor = new ManualExecutor();
        var dispatcher = new ManualExecutor();
        var published = new ArrayList<Integer>();
        var calculated = new ArrayList<Integer>();
        var workerRef = new PathPreviewWorker[1];
        workerRef[0] = new PathPreviewWorker(executor, dispatcher, (samples, tiles, multiplier, profile, cancelled) -> {
            int value = (int) samples.getFirst().x();
            calculated.add(value);
            if (value == 0) {
                for (int i = 1; i <= 10; i++)
                    submit(workerRef[0], i, published);
                PathPreviewMask.checkCancelled(cancelled);
            }
            return result(value);
        });
        submit(workerRef[0], 0, published);
        assertEquals(1, executor.tasks.size());
        executor.runNext();
        assertEquals(List.of(0, 10), calculated);
        assertEquals(1, dispatcher.tasks.size());
        dispatcher.runNext();
        assertEquals(List.of(10), published);
    }

    @Test
    void invalidationAndCloseDiscardQueuedCompletions() {
        var executor = new ManualExecutor();
        var dispatcher = new ManualExecutor();
        var published = new ArrayList<Integer>();
        var worker = new PathPreviewWorker(executor, dispatcher,
                (samples, tiles, multiplier, profile, cancelled) -> result((int) samples.getFirst().x()));
        submit(worker, 1, published);
        executor.runNext();
        worker.invalidate();
        dispatcher.runNext();
        submit(worker, 2, published);
        executor.runNext();
        worker.close();
        dispatcher.runNext();
        assertEquals(List.of(), published);
        assertThrows(IllegalStateException.class, () -> submit(worker, 3, published));
    }

    @Test
    void calculationFailureDoesNotPreventLaterRequests() {
        var executor = new ManualExecutor();
        var dispatcher = new ManualExecutor();
        var published = new ArrayList<Integer>();
        var worker = new PathPreviewWorker(executor, dispatcher, (samples, tiles, multiplier, profile, cancelled) -> {
            int value = (int) samples.getFirst().x();
            if (value == 1)
                throw new IllegalArgumentException("expected test failure");
            return result(value);
        });
        submit(worker, 1, published);
        executor.runNext();
        submit(worker, 2, published);
        executor.runNext();
        dispatcher.runNext();
        assertEquals(List.of(2), published);
    }
}
