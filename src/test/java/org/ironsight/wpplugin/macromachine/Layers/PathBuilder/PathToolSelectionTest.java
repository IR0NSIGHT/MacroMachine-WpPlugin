package org.ironsight.wpplugin.macromachine.Layers.PathBuilder;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.swing.SwingUtilities;
import org.ironsight.wpplugin.macromachine.Gui.MapToolOverlay;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.TestDimension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.pepsoft.util.AttributeKey;
import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.Dimension;
import static org.junit.jupiter.api.Assertions.*;

class PathToolSelectionTest
{
    private static final AttributeKey<ArrayList<Float>> HANDLES = new AttributeKey<>("PATHTOOL-PATHHANDLES",
            new ArrayList<>());
    private WorldPainterView previousInstance;

    @BeforeEach
    void isolateView() throws Exception {
        var field = WorldPainterView.class.getDeclaredField("instance");
        field.setAccessible(true);
        previousInstance = (WorldPainterView) field.get(null);
        field.set(null, null);
    }

    @AfterEach
    void restoreView() throws Exception {
        var field = WorldPainterView.class.getDeclaredField("instance");
        field.setAccessible(true);
        field.set(null, previousInstance);
    }

    @Test
    void mapEventsToggleAndReplaceSelectionWithoutAppending() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Dimension dimension = dimension();
                dimension.setAttribute(HANDLES, new ArrayList<>(List.of(10f, 10f, 70f, 4f, 30f, 30f, 70f, 6f)), true);
                TestView view = new TestView(dimension);
                PathTool tool = new PathTool();
                tool.setView(view);
                tool.setActive(true);
                try {
                    click(view, 10, 10);
                    click(view, 30, 30);
                    assertEquals(Set.of(0, 1), tool.selectedNodes());
                    click(view, 10, 10);
                    assertEquals(Set.of(1), tool.selectedNodes());
                    event(view, MouseEvent.MOUSE_PRESSED, 0, 0, MouseEvent.BUTTON1, InputEvent.BUTTON1_DOWN_MASK);
                    event(view, MouseEvent.MOUSE_DRAGGED, 20, 20, MouseEvent.NOBUTTON, InputEvent.BUTTON1_DOWN_MASK);
                    event(view, MouseEvent.MOUSE_RELEASED, 20, 20, MouseEvent.BUTTON1, 0);
                    assertEquals(Set.of(0), tool.selectedNodes());
                    click(view, 80, 80);
                    assertEquals(Set.of(), tool.selectedNodes());
                    tool.tick(40, 40, false, true, 1f);
                    assertEquals(List.of(10f, 10f, 70f, 4f, 30f, 30f, 70f, 6f), dimension.getAttribute(HANDLES));
                } finally {
                    tool.setActive(false);
                }
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    @Test
    void nearestNodeUsesScreenPixelsAndStableOrderAndLifecycleCleansUp() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Dimension dimension = dimension();
                dimension.setAttribute(HANDLES, new ArrayList<>(List.of(10f, 10f, 70f, 4f, 12f, 10f, 70f, 6f)), true);
                TestView view = new TestView(dimension);
                var previous = view.getViewListener();
                PathTool tool = new PathTool();
                tool.setView(view);
                tool.setActive(true);
                assertEquals(0, tool.nearestNode(view.worldToView(11, 10)));
                assertEquals(-1, tool.nearestNode(view.worldToView(50, 50)));
                assertEquals(1,
                        java.util.Arrays.stream(view.getComponents()).filter(MapToolOverlay.class::isInstance).count());
                click(view, 10, 10);
                view.setDimension(dimension());
                assertEquals(Set.of(), tool.selectedNodes());
                tool.setActive(false);
                assertSame(previous, view.getViewListener());
                assertEquals(0,
                        java.util.Arrays.stream(view.getComponents()).filter(MapToolOverlay.class::isInstance).count());
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    private static void click(TestView view, int x, int y) {
        event(view, MouseEvent.MOUSE_PRESSED, x, y, MouseEvent.BUTTON1, InputEvent.BUTTON1_DOWN_MASK);
        event(view, MouseEvent.MOUSE_RELEASED, x, y, MouseEvent.BUTTON1, 0);
    }

    private static void event(TestView view, int id, int x, int y, int button, int modifiers) {
        Point point = view.worldToView(x, y);
        view.dispatchEvent(
                new MouseEvent(view, id, System.currentTimeMillis(), modifiers, point.x, point.y, 1, false, button));
    }

    private static Dimension dimension() {
        return TestDimension.createDimension(new TestDimension.DimensionParams(new Rectangle(128, 128), -256, 512, 70,
                123456789, 62, org.pepsoft.worldpainter.DefaultPlugin.JAVA_ANVIL_1_19,
                org.pepsoft.worldpainter.Terrain.GRASS));
    }

    private static class TestView extends WorldPainterView
    {
        private Dimension dimension;
        TestView(Dimension dimension) {
            this.dimension = dimension;
            setSize(100, 100);
        }
        @Override
        public Dimension getDimension() {
            return dimension;
        }
        @Override
        public void setDimension(Dimension dimension) {
            this.dimension = dimension;
        }
        @Override
        public void updateStatusBar(int x, int y) {
        }
        @Override
        public boolean isDrawBrush() {
            return false;
        }
        @Override
        public void setDrawBrush(boolean drawBrush) {
        }
        @Override
        public MapDragControl getMapDragControl() {
            return null;
        }
        @Override
        public BrushControl getBrushControl() {
            return null;
        }
        @Override
        public RadiusControl getRadiusControl() {
            return null;
        }
    }
}
