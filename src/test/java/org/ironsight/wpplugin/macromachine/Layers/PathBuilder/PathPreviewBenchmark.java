package org.ironsight.wpplugin.macromachine.Layers.PathBuilder;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Container;
import java.awt.Rectangle;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import javax.swing.JButton;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.TestDimension;
import org.ironsight.wpplugin.macromachine.operations.PreviewOperation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.pepsoft.util.AttributeKey;
import org.pepsoft.worldpainter.DefaultPlugin;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Terrain;
import org.pepsoft.worldpainter.WorldPainterView;
import org.pepsoft.worldpainter.brushes.Brush;

/**
 * Run explicitly with -Dtest=PathPreviewBenchmark -DpathBenchmark=true test.
 */
public class PathPreviewBenchmark
{
    private static final AttributeKey<ArrayList<Float>> HANDLES = new AttributeKey<>("PATHTOOL-PATHHANDLES",
            new ArrayList<>());

    @Test
    @EnabledIfSystemProperty(named = "pathBenchmark", matches = "true")
    void benchmark() throws Exception {
        run(true, false); // Warm up the same production pipeline and fixture.
        run(true, true);
        run(false, true);
    }

    private static void run(boolean layer, boolean report) throws Exception {
        var instance = WorldPainterView.class.getDeclaredField("instance");
        instance.setAccessible(true);
        Object previous = instance.get(null);
        var executor = Executors.newSingleThreadExecutor();
        var started = new AtomicInteger();
        var completed = new AtomicInteger();
        var cancelled = new AtomicInteger();
        var workerNanos = new AtomicLong();
        var state = new State();
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    instance.set(null, null);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
                state.dimension = TestDimension
                        .createDimension(new TestDimension.DimensionParams(new Rectangle(1024, 1024), -256, 512, 70,
                                123456789, 62, DefaultPlugin.JAVA_ANVIL_1_19, Terrain.GRASS));
                var view = new PathToolSelectionTest.TestView(state.dimension);
                state.tool = new PathTool(
                        () -> new PathPreviewWorker(executor, task -> SwingUtilities.invokeLater(() -> {
                            var before = state.tool.previewTiming;
                            task.run();
                            var after = state.tool.previewTiming;
                            if (after != null && after != before) {
                                state.publishes++;
                                if (state.awaiting != null) {
                                    int px = x(state.expected), py = y(state.expected);
                                    boolean endpoint = state.dimension.getTile(px >> 7, py >> 7)
                                            .getBitLayerValue(PreviewOperation.annotationLayer, px & 127, py & 127);
                                    if (endpoint
                                            && state.dimension.getAttribute(HANDLES).size() == (state.expected + 1) * 4)
                                        state.awaiting.complete(System.nanoTime());
                                }
                            }
                        }), (samples, tiles, multiplier, profile, obsolete) -> {
                            started.incrementAndGet();
                            long begin = System.nanoTime();
                            try {
                                var result = PathPreviewMask.calculate(samples, tiles, multiplier, profile, obsolete);
                                completed.incrementAndGet();
                                return result;
                            } catch (CancellationException e) {
                                cancelled.incrementAndGet();
                                throw e;
                            } finally {
                                workerNanos.addAndGet(System.nanoTime() - begin);
                            }
                        }));
                state.tool.setView(view);
                state.tool.setBrush((Brush) Proxy.newProxyInstance(Brush.class.getClassLoader(),
                        new Class<?>[]{Brush.class}, (proxy, method, args) -> switch (method.getName()) {
                            case "getEffectiveRadius", "getRadius" -> 14;
                            default -> null;
                        }));
                setActive(state.tool, true);
                state.tool.applyRiverPreset();
                if (!layer)
                    state.tool.overlayParts()
                            .stream()
                            .filter(part -> "PathTool.ShowLayer".equals(part.getName()))
                            .findFirst()
                            .orElseThrow()
                            .doClick();
                var initial = new ArrayList<Float>();
                for (int i = 0; i < 19; i++) {
                    initial.add((float) x(i));
                    initial.add((float) y(i));
                    initial.add(state.dimension.getHeightAt(x(i), y(i)));
                    initial.add(14f);
                }
                state.dimension.setAttribute(HANDLES, initial, true);
                state.tool.measurePreview = true;
            });
            // Drain setup's deferred settings refresh before starting the measured edit.
            SwingUtilities.invokeAndWait(() -> {
            });
            var first = new CompletableFuture<Long>();
            long[] times = new long[15];
            SwingUtilities.invokeAndWait(() -> {
                state.awaiting = first;
                state.expected = 19;
                times[0] = System.nanoTime();
                state.tool.addNode(x(19), y(19));
                times[1] = System.nanoTime();
            });
            if (layer)
                times[2] = first.get(60, TimeUnit.SECONDS);
            SwingUtilities.invokeAndWait(() -> {
                state.awaiting = null;
                if (report && layer) {
                    var timing = state.tool.previewTiming;
                    System.out.printf(Locale.ROOT,
                            "20 nodes: edit_ms=%.3f completion_ms=%.3f samples=%d candidates=%d existing=%d sampling_ms=%.3f collection_ms=%.3f worker_wall_ms=%.3f publication_ms=%.3f%n",
                            (times[1] - times[0]) / 1e6, (times[2] - times[0]) / 1e6, timing.samples(),
                            timing.candidateTiles(), timing.existingTiles(), timing.samplingNanos() / 1e6,
                            timing.collectionNanos() / 1e6, workerNanos.get() / 1e6, timing.publicationNanos() / 1e6);
                } else if (report)
                    System.out.printf(Locale.ROOT, "layer OFF: 20-node edit_ms=%.3f%n", (times[1] - times[0]) / 1e6);
            });
            int startJobs = started.get(), startCompleted = completed.get(), startCancelled = cancelled.get();
            long startWorker = workerNanos.get();
            var last = new CompletableFuture<Long>();
            int[] publications = new int[1];
            SwingUtilities.invokeAndWait(() -> {
                publications[0] = state.publishes;
                state.awaiting = last;
                state.expected = 29;
                times[3] = System.nanoTime();
                for (int i = 20; i < 30; i++) {
                    long begin = System.nanoTime();
                    state.tool.addNode(x(i), y(i));
                    times[14] = System.nanoTime();
                    times[i - 16] = times[14] - begin;
                }
                times[2] = System.nanoTime();
            });
            long published = layer ? last.get(60, TimeUnit.SECONDS) : 0;
            SwingUtilities.invokeAndWait(() -> {
                state.awaiting = null;
                if (report) {
                    System.out.printf(Locale.ROOT, "layer %s burst: total_edit_ms=%.3f individual_edit_ms=",
                            layer ? "ON" : "OFF", (times[2] - times[3]) / 1e6);
                    for (int i = 4; i < 14; i++)
                        System.out.printf(Locale.ROOT, "%s%.3f", i == 4 ? "" : ",", times[i] / 1e6);
                    if (layer)
                        System.out.printf(Locale.ROOT,
                                " final_catchup_ms=%.3f jobs_started=%d completed=%d cancelled=%d accepted_publishes=%d worker_wall_ms=%.3f",
                                (published - times[14]) / 1e6, started.get() - startJobs,
                                completed.get() - startCompleted, cancelled.get() - startCancelled,
                                state.publishes - publications[0], (workerNanos.get() - startWorker) / 1e6);
                    System.out.println();
                }
                if (layer)
                    assertTrue(last.isDone(), "latest endpoint was not published");
            });
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                if (state.tool != null)
                    setActive(state.tool, false);
                try {
                    instance.set(null, previous);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            });
            executor.shutdown();
            if (!executor.awaitTermination(60, TimeUnit.SECONDS))
                throw new IllegalStateException("Preview worker did not terminate");
        }
    }

    private static class State
    {
        PathTool tool;
        Dimension dimension;
        CompletableFuture<Long> awaiting;
        int expected;
        int publishes;
    }

    private static void setActive(PathTool tool, boolean active) {
        try {
            tool.setActive(active);
        } catch (java.beans.PropertyVetoException e) {
            throw new IllegalStateException(e);
        }
    }

    private static int x(int index) {
        int row = index / 6;
        int column = index % 6;
        return 100 + 150 * (row % 2 == 0 ? column : 5 - column);
    }

    private static int y(int index) {
        return 100 + 170 * (index / 6);
    }

    private static java.awt.Component find(Container container, String value, boolean name) {
        for (var component : container.getComponents()) {
            if (name
                    ? value.equals(component.getName())
                    : component instanceof JButton button && value.equals(button.getText()))
                return component;
            if (component instanceof Container child) {
                var found = find(child, value, name);
                if (found != null)
                    return found;
            }
        }
        return null;
    }
}
