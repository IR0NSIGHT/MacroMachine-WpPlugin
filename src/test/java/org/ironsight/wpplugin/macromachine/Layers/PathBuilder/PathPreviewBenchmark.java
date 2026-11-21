package org.ironsight.wpplugin.macromachine.Layers.PathBuilder;

import java.awt.Container;
import java.awt.Rectangle;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import javax.swing.JButton;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.TestDimension;
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

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                run(true, false);
                run(true, true);
                run(false, true);
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            }
        });
    }

    @Test
    @EnabledIfSystemProperty(named = "pathBenchmark", matches = "true")
    void benchmark() throws Exception {
        main(new String[0]);
    }

    private static void run(boolean layer, boolean report) throws Exception {
        var instance = WorldPainterView.class.getDeclaredField("instance");
        instance.setAccessible(true);
        Object previous = instance.get(null);
        instance.set(null, null);
        Dimension dimension = TestDimension.createDimension(new TestDimension.DimensionParams(new Rectangle(1024, 1024),
                -256, 512, 70, 123456789, 62, DefaultPlugin.JAVA_ANVIL_1_19, Terrain.GRASS));
        var view = new PathToolSelectionTest.TestView(dimension);
        var tool = new PathTool();
        tool.setView(view);
        tool.setBrush((Brush) Proxy.newProxyInstance(Brush.class.getClassLoader(), new Class<?>[]{Brush.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getEffectiveRadius", "getRadius" -> 14;
                    default -> null;
                }));
        tool.setActive(true);
        try {
            JButton preset = (JButton) find(tool.getOptionsPanel(), "River preset", false);
            preset.doClick();
            if (!layer)
                ((JToggleButton) find(tool.getOptionsPanel(), "PathTool.ShowLayer", true)).doClick();
            var initial = new ArrayList<Float>();
            for (int i = 0; i < 19; i++) {
                initial.add((float) x(i));
                initial.add((float) y(i));
                initial.add(dimension.getHeightAt(x(i), y(i)));
                initial.add(14f);
            }
            dimension.setAttribute(HANDLES, initial, true);
            tool.measurePreview = true;
            for (int i = 19; i < 30; i++) {
                tool.previewTiming = null;
                long start = System.nanoTime();
                tool.tick(x(i), y(i), true, true, 1f);
                long elapsed = System.nanoTime() - start;
                if (report) {
                    var timing = tool.previewTiming;
                    System.out.printf("layer=%s nodes=%d edit_ms=%.3f", layer ? "ON" : "OFF", i + 1, elapsed / 1e6);
                    if (timing != null)
                        System.out.printf(
                                " samples=%d candidates=%d existing=%d sampling_ms=%.3f collection_ms=%.3f calculation_ms=%.3f publication_ms=%.3f",
                                timing.samples(), timing.candidateTiles(), timing.existingTiles(),
                                timing.samplingNanos() / 1e6, timing.collectionNanos() / 1e6,
                                timing.calculationNanos() / 1e6, timing.publicationNanos() / 1e6);
                    System.out.println();
                }
            }
        } finally {
            tool.setActive(false);
            instance.set(null, previous);
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
