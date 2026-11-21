package org.ironsight.wpplugin.macromachine.Layers.PathBuilder;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.swing.SwingUtilities;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import org.ironsight.wpplugin.macromachine.Gui.MapToolOverlay;
import org.ironsight.wpplugin.macromachine.operations.PreviewOperation;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.TestDimension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.pepsoft.util.AttributeKey;
import org.pepsoft.util.undo.UndoManager;
import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.brushes.Brush;
import static org.junit.jupiter.api.Assertions.*;

class PathToolSelectionTest
{
    private static final AttributeKey<ArrayList<Float>> HANDLES = new AttributeKey<>("PATHTOOL-PATHHANDLES",
            new ArrayList<>());
    private WorldPainterView previousInstance;

    private static PathTool synchronousTool() {
        return new PathTool(() -> new PathPreviewWorker(Runnable::run, Runnable::run, PathPreviewMask::calculate));
    }

    @Test
    void asynchronousEditsCoalesceAndPublishOnlyOnDispatch() throws Exception {
        var work = new ArrayList<Runnable>();
        var callbacks = new ArrayList<Runnable>();
        var tool = new PathTool[]{null};
        var dim = new Dimension[]{null};
        SwingUtilities.invokeAndWait(() -> {
            try {
                dim[0] = dimension();
                tool[0] = new PathTool(
                        () -> new PathPreviewWorker(work::add, callbacks::add, PathPreviewMask::calculate));
                tool[0].setView(new TestView(dim[0]));
                tool[0].setBrush(brush());
                tool[0].setActive(true);
                for (int i = 0; i < 10; i++)
                    tool[0].tick(10 + i * 5, 20, true, true, 1f);
                assertEquals(40, dim[0].getAttribute(HANDLES).size());
                assertEquals(1, work.size());
                assertFalse(dim[0].getBitLayerValueAt(PreviewOperation.annotationLayer, 10, 20));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        work.remove(0).run();
        assertEquals(1, callbacks.size());
        SwingUtilities.invokeAndWait(() -> {
            dim[0].setEventsInhibited(true);
            callbacks.remove(0).run();
            assertTrue(dim[0].isEventsInhibited());
            assertTrue(dim[0].getBitLayerValueAt(PreviewOperation.annotationLayer, 10, 20));
            assertTrue(dim[0].getBitLayerValueAt(PreviewOperation.annotationLayer, 55, 20));
            dim[0].setEventsInhibited(false);
            dim[0].setAttribute(HANDLES, new ArrayList<>(), true);
            tool[0].tick(70, 60, true, true, 1f);
            assertTrue(dim[0].getBitLayerValueAt(PreviewOperation.annotationLayer, 10, 20));
            assertFalse(dim[0].getBitLayerValueAt(PreviewOperation.annotationLayer, 70, 60));
        });
        work.remove(0).run();
        SwingUtilities.invokeAndWait(() -> {
            callbacks.remove(0).run();
            assertFalse(dim[0].getBitLayerValueAt(PreviewOperation.annotationLayer, 10, 20));
            assertTrue(dim[0].getBitLayerValueAt(PreviewOperation.annotationLayer, 70, 60));
            assertDoesNotThrow(() -> tool[0].setActive(false));
        });
    }

    @Test
    void queuedResultsCannotReappearAfterResetOrExternalPathChange() throws Exception {
        var work = new ArrayList<Runnable>();
        var callbacks = new ArrayList<Runnable>();
        var tool = new PathTool[]{null};
        var dim = new Dimension[]{null};
        SwingUtilities.invokeAndWait(() -> {
            try {
                dim[0] = dimension();
                tool[0] = new PathTool(
                        () -> new PathPreviewWorker(work::add, callbacks::add, PathPreviewMask::calculate));
                tool[0].setView(new TestView(dim[0]));
                tool[0].setBrush(brush());
                tool[0].setActive(true);
                tool[0].tick(20, 20, true, true, 1f);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        work.remove(0).run();
        SwingUtilities.invokeAndWait(() -> {
            dim[0].setAttribute(HANDLES, new ArrayList<>(), true);
            callbacks.remove(0).run();
            assertFalse(dim[0].getBitLayerValueAt(PreviewOperation.annotationLayer, 20, 20));
            tool[0].tick(30, 20, true, true, 1f);
        });
        work.remove(0).run();
        SwingUtilities.invokeAndWait(() -> {
            findButton(tool[0].getOptionsPanel(), "PathTool.NewPath").doClick();
            callbacks.remove(0).run();
            assertFalse(dim[0].getBitLayerValueAt(PreviewOperation.annotationLayer, 30, 20));
            assertDoesNotThrow(() -> tool[0].setActive(false));
        });
    }

    @Test
    void queuedResultsAreInvalidatedByLayerOffDimensionChangeDeactivateAndApply() throws Exception {
        for (String action : List.of("off", "dimension", "deactivate", "apply")) {
            var work = new ArrayList<Runnable>();
            var callbacks = new ArrayList<Runnable>();
            var tool = new PathTool[]{null};
            var dim = new Dimension[]{null};
            var view = new TestView[]{null};
            SwingUtilities.invokeAndWait(() -> {
                try {
                    dim[0] = dimension();
                    view[0] = new TestView(dim[0]);
                    tool[0] = new PathTool(
                            () -> new PathPreviewWorker(work::add, callbacks::add, PathPreviewMask::calculate));
                    tool[0].setView(view[0]);
                    tool[0].setBrush(brush());
                    tool[0].setActive(true);
                    tool[0].tick(20, 20, true, true, 1f);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            work.remove(0).run();
            SwingUtilities.invokeAndWait(() -> {
                switch (action) {
                    case "off" -> findToggle(tool[0].getOptionsPanel(), "PathTool.ShowLayer").doClick();
                    case "dimension" -> {
                        view[0].setDimension(dimension());
                        tool[0].selectedNodes();
                    }
                    case "deactivate" -> assertDoesNotThrow(() -> tool[0].setActive(false));
                    case "apply" -> findButton(tool[0].getOptionsPanel(), "PathTool.Apply").doClick();
                    default -> throw new AssertionError(action);
                }
                callbacks.remove(0).run();
                assertFalse(dim[0].getBitLayerValueAt(PreviewOperation.annotationLayer, 20, 20), action);
                assertDoesNotThrow(() -> tool[0].setActive(false));
            });
            var field = WorldPainterView.class.getDeclaredField("instance");
            field.setAccessible(true);
            field.set(null, null);
        }
    }

    @Test
    void hudLayersAreIndependentAndDoNotChangePathOrTerrain() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Dimension dimension = dimension();
                var handles = List.of(30f, 30f, 70f, 20f, 90f, 30f, 70f, 20f);
                dimension.setAttribute(HANDLES, new ArrayList<>(handles), true);
                TestView view = new TestView(dimension);
                PathTool tool = synchronousTool();
                tool.setView(view);
                tool.setActive(true);
                try {
                    var overlay = (MapToolOverlay) java.util.Arrays.stream(view.getComponents())
                            .filter(MapToolOverlay.class::isInstance)
                            .findFirst()
                            .orElseThrow();
                    overlay.setSize(200, 200);
                    var label = findToggle(tool.getOptionsPanel(), "PathTool.ShowLabel");
                    var radius = findToggle(tool.getOptionsPanel(), "PathTool.ShowRadius");
                    var line = findToggle(tool.getOptionsPanel(), "PathTool.ShowLine");
                    var nodes = findToggle(tool.getOptionsPanel(), "PathTool.ShowNodes");
                    var layer = findToggle(tool.getOptionsPanel(), "PathTool.ShowLayer");
                    assertEquals(List.of("Label", "Radius", "Line", "Nodes", "Layer"), List.of(label.getText(),
                            radius.getText(), line.getText(), nodes.getText(), layer.getText()));
                    for (var toggle : List.of(label, radius, line, nodes, layer))
                        assertTrue(toggle.isSelected());
                    Point center = view.worldToView(30, 30);
                    Point ring = view.worldToView(30, 10);
                    Point middle = view.worldToView(60, 30);
                    var all = paintOverlay(overlay);
                    assertNotEquals(0, all.getRGB(center.x, center.y));
                    assertNotEquals(0, all.getRGB(ring.x, ring.y));
                    assertNotEquals(0, all.getRGB(middle.x, middle.y));
                    nodes.doClick();
                    var withoutNodes = paintOverlay(overlay);
                    assertNotEquals(all.getRGB(center.x, center.y - 5), withoutNodes.getRGB(center.x, center.y - 5));
                    assertEquals(all.getRGB(ring.x, ring.y), withoutNodes.getRGB(ring.x, ring.y));
                    radius.doClick();
                    var withoutRadius = paintOverlay(overlay);
                    assertNotEquals(withoutNodes.getRGB(ring.x, ring.y), withoutRadius.getRGB(ring.x, ring.y));
                    assertEquals(withoutNodes.getRGB(middle.x, middle.y), withoutRadius.getRGB(middle.x, middle.y));
                    line.doClick();
                    var labelOnly = paintOverlay(overlay);
                    assertNotEquals(withoutRadius.getRGB(middle.x, middle.y), labelOnly.getRGB(middle.x, middle.y));
                    label.doClick();
                    var empty = paintOverlay(overlay);
                    assertTrue(differentPixels(labelOnly, empty, center.x + 14, center.y - 12, 12, 24) > 0);
                    assertTrue(overlay.getComponent(0).isVisible());
                    assertEquals(handles, dimension.getAttribute(HANDLES));
                    assertEquals(70f, dimension.getHeightAt(30, 30));
                    assertEquals(0, tool.nearestNode(new Point(center.x + 18, center.y)));
                    assertEquals(-1, tool.nearestNode(new Point(center.x + 19, center.y)));
                } finally {
                    tool.setActive(false);
                }
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    @Test
    void hiddenLayerStaysClearAcrossEditsSettingsAndApply() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Dimension dimension = dimension();
                TestView view = new TestView(dimension);
                PathTool tool = synchronousTool();
                tool.setView(view);
                tool.setBrush(brush());
                tool.setActive(true);
                try {
                    tool.tick(20, 20, true, true, 1f);
                    assertTrue(dimension.getBitLayerValueAt(PreviewOperation.annotationLayer, 20, 20));
                    var handles = List.copyOf(dimension.getAttribute(HANDLES));
                    var layer = findToggle(tool.getOptionsPanel(), "PathTool.ShowLayer");
                    var overlay = (MapToolOverlay) java.util.Arrays.stream(view.getComponents())
                            .filter(MapToolOverlay.class::isInstance)
                            .findFirst()
                            .orElseThrow();
                    overlay.setSize(200, 200);
                    var before = paintOverlay(overlay);
                    dimension.setEventsInhibited(true);
                    layer.doClick();
                    assertTrue(dimension.isEventsInhibited());
                    dimension.setEventsInhibited(false);
                    assertFalse(dimension.getBitLayerValueAt(PreviewOperation.annotationLayer, 20, 20));
                    assertEquals(handles, dimension.getAttribute(HANDLES));
                    assertEquals(70f, dimension.getHeightAt(20, 20));
                    assertEquals(0, differentPixels(before, paintOverlay(overlay), 0, 0, 200, 200));
                    tool.tick(60, 20, true, true, 1f);
                    var spinner = PathTool.class.getDeclaredField("handleFactorSpinner");
                    spinner.setAccessible(true);
                    ((JSpinner) spinner.get(tool)).setValue(1.0d);
                    assertFalse(dimension.getBitLayerValueAt(PreviewOperation.annotationLayer, 60, 20));
                    dimension.setHeightAt(60, 20, 90f);
                    findButton(tool.getOptionsPanel(), "PathTool.Apply").doClick();
                    assertNotEquals(90f, dimension.getHeightAt(60, 20));
                    assertFalse(dimension.getBitLayerValueAt(PreviewOperation.annotationLayer, 60, 20));
                    tool.setActive(false);
                    tool.setActive(true);
                    layer.doClick();
                    assertTrue(dimension.getBitLayerValueAt(PreviewOperation.annotationLayer, 60, 20));
                    findButton(tool.getOptionsPanel(), "PathTool.NewPath").doClick();
                    assertFalse(dimension.getBitLayerValueAt(PreviewOperation.annotationLayer, 60, 20));
                } finally {
                    tool.setActive(false);
                }
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    private static int differentPixels(BufferedImage a, BufferedImage b, int x, int y, int width, int height) {
        int count = 0;
        for (int px = x; px < x + width; px++)
            for (int py = y; py < y + height; py++)
                if (a.getRGB(px, py) != b.getRGB(px, py))
                    count++;
        return count;
    }

    private static JToggleButton findToggle(java.awt.Container container, String name) {
        for (var component : container.getComponents()) {
            if (component instanceof JToggleButton button && name.equals(button.getName()))
                return button;
            if (component instanceof java.awt.Container child) {
                var result = findToggle(child, name);
                if (result != null)
                    return result;
            }
        }
        return null;
    }

    @Test
    void overlayPaintsAndCachesRestingAndDraggedCurves() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Dimension dimension = dimension();
                dimension.setAttribute(HANDLES, new ArrayList<>(List.of(10f, 10f, 70f, 4f, 70f, 10f, 70f, 4f)), true);
                TestView view = new TestView(dimension);
                PathTool tool = synchronousTool();
                tool.setView(view);
                tool.setActive(true);
                try {
                    var overlay = (MapToolOverlay) java.util.Arrays.stream(view.getComponents())
                            .filter(MapToolOverlay.class::isInstance)
                            .findFirst()
                            .orElseThrow();
                    overlay.setSize(200, 200);
                    var cacheField = overlay.getClass().getDeclaredField("cachedCurve");
                    cacheField.setAccessible(true);
                    var image = paintOverlay(overlay);
                    Point midpoint = view.worldToView(40, 10);
                    assertNotEquals(0, image.getRGB(midpoint.x, midpoint.y));
                    Object resting = cacheField.get(overlay);
                    click(view, 10, 10);
                    paintOverlay(overlay);
                    assertSame(resting, cacheField.get(overlay));
                    tool.handleSelectionPress(view.worldToView(10, 10));
                    tool.handleSelectionDrag(view.worldToView(20, 10));
                    paintOverlay(overlay);
                    Object dragged = cacheField.get(overlay);
                    assertNotSame(resting, dragged);
                    assertEquals(PathToolBackend.getPathFromNodes(
                            List.of(new javax.vecmath.Point4f(20, 10, 70, 4), new javax.vecmath.Point4f(70, 10, 70, 4)),
                            0).path, dragged);
                    tool.dispatchKeyEvent(new KeyEvent(view, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_ESCAPE,
                            KeyEvent.CHAR_UNDEFINED));
                    paintOverlay(overlay);
                    assertEquals(resting, cacheField.get(overlay));
                    var spinnerField = PathTool.class.getDeclaredField("handleFactorSpinner");
                    spinnerField.setAccessible(true);
                    ((JSpinner) spinnerField.get(tool)).setValue(1.0d);
                    paintOverlay(overlay);
                    assertEquals(PathToolBackend.getPathFromNodes(
                            List.of(new javax.vecmath.Point4f(10, 10, 70, 4), new javax.vecmath.Point4f(70, 10, 70, 4)),
                            1).path, cacheField.get(overlay));
                    var line = findToggle(tool.getOptionsPanel(), "PathTool.ShowLine");
                    line.doClick();
                    Object cached = cacheField.get(overlay);
                    dimension.setAttribute(HANDLES, new ArrayList<>(List.of(20f, 10f, 70f, 4f, 70f, 10f, 70f, 4f)),
                            true);
                    paintOverlay(overlay);
                    assertSame(cached, cacheField.get(overlay));
                    line.doClick();
                    paintOverlay(overlay);
                    assertEquals(PathToolBackend.getPathFromNodes(
                            List.of(new javax.vecmath.Point4f(20, 10, 70, 4), new javax.vecmath.Point4f(70, 10, 70, 4)),
                            1).path, cacheField.get(overlay));
                } finally {
                    tool.setActive(false);
                }
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    private static BufferedImage paintOverlay(MapToolOverlay overlay) {
        var image = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        try {
            overlay.paint(graphics);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    @Test
    void shortcutsAllowOtherMapWindowControlsButNotEditorsOrDialogs() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JFrame frame = new JFrame();
            JDialog dialog = new JDialog(frame);
            try {
                JPanel view = new JPanel();
                JPanel controls = new JPanel();
                JButton button = new JButton();
                JTextField text = new JTextField();
                JSpinner spinner = new JSpinner();
                frame.add(view, java.awt.BorderLayout.CENTER);
                frame.add(controls, java.awt.BorderLayout.SOUTH);
                controls.add(button);
                controls.add(text);
                controls.add(spinner);
                JButton dialogButton = new JButton();
                dialog.add(dialogButton);
                assertTrue(PathTool.allowsMapShortcut(view, button));
                assertFalse(PathTool.allowsMapShortcut(view, text));
                assertFalse(PathTool.allowsMapShortcut(view, spinner.getEditor()));
                assertFalse(PathTool.allowsMapShortcut(view, dialogButton));
            } finally {
                dialog.dispose();
                frame.dispose();
            }
        });
    }

    @Test
    void previewMovesAndResetClearsFootprintAndApplyUsesFreshBaseline() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Dimension dimension = dimension();
                TestView view = new TestView(dimension);
                PathTool tool = synchronousTool();
                tool.setView(view);
                tool.setBrush(brush());
                tool.setActive(true);
                try {
                    tool.tick(20, 20, true, true, 1f);
                    assertTrue(dimension.getBitLayerValueAt(PreviewOperation.annotationLayer, 20, 20));
                    assertEquals(70f, dimension.getHeightAt(20, 20));
                    event(view, MouseEvent.MOUSE_PRESSED, 20, 20, MouseEvent.BUTTON1, InputEvent.BUTTON1_DOWN_MASK);
                    event(view, MouseEvent.MOUSE_DRAGGED, 60, 20, MouseEvent.NOBUTTON, InputEvent.BUTTON1_DOWN_MASK);
                    event(view, MouseEvent.MOUSE_RELEASED, 60, 20, MouseEvent.BUTTON1, 0);
                    assertFalse(dimension.getBitLayerValueAt(PreviewOperation.annotationLayer, 20, 20));
                    assertTrue(dimension.getBitLayerValueAt(PreviewOperation.annotationLayer, 60, 20));
                    var handles = List.copyOf(dimension.getAttribute(HANDLES));
                    dimension.setHeightAt(100, 100, 100f);
                    dimension.setEventsInhibited(true);
                    findButton(tool.getOptionsPanel(), "PathTool.Apply").doClick();
                    assertTrue(dimension.isEventsInhibited());
                    dimension.setEventsInhibited(false);
                    assertEquals(handles, dimension.getAttribute(HANDLES));
                    assertEquals(100f, dimension.getHeightAt(100, 100));
                    findButton(tool.getOptionsPanel(), "PathTool.NewPath").doClick();
                    assertEquals(List.of(), dimension.getAttribute(HANDLES));
                    assertFalse(dimension.getBitLayerValueAt(PreviewOperation.annotationLayer, 60, 20));
                } finally {
                    tool.setActive(false);
                }
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    private static Brush brush() {
        return (Brush) java.lang.reflect.Proxy.newProxyInstance(Brush.class.getClassLoader(),
                new Class<?>[]{Brush.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getEffectiveRadius", "getRadius" -> 4;
                    default -> null;
                });
    }

    @Test
    void undoManagerRestoresHandlesAndAnnotationTogether() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Dimension dimension = dimension();
                UndoManager manager = new UndoManager();
                dimension.registerUndoManager(manager);
                TestView view = new TestView(dimension);
                PathTool tool = synchronousTool();
                tool.setView(view);
                tool.setBrush(brush());
                tool.setActive(true);
                try {
                    tool.tick(20, 20, true, true, 1f);
                    dimension.armSavePoint();
                    tool.tick(60, 20, true, true, 1f);
                    assertEquals(8, dimension.getAttribute(HANDLES).size());
                    assertTrue(dimension.getBitLayerValueAt(PreviewOperation.annotationLayer, 60, 20));
                    assertTrue(manager.undo());
                    assertEquals(List.of(20f, 20f, 70f, 4f), dimension.getAttribute(HANDLES));
                    assertFalse(dimension.getBitLayerValueAt(PreviewOperation.annotationLayer, 60, 20));
                    assertTrue(manager.redo());
                    assertEquals(List.of(20f, 20f, 70f, 4f, 60f, 20f, 70f, 4f), dimension.getAttribute(HANDLES));
                    assertTrue(dimension.getBitLayerValueAt(PreviewOperation.annotationLayer, 60, 20));
                } finally {
                    tool.setActive(false);
                    dimension.unregisterUndoManager();
                }
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    @Test
    void outsideComponentReleaseFinishesDragAndRemovesCapture() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Dimension dimension = dimension();
                dimension.setAttribute(HANDLES, new ArrayList<>(List.of(10f, 10f, 70f, 4f)), true);
                TestView view = new TestView(dimension);
                JPanel container = new JPanel(null);
                JPanel outside = new JPanel();
                container.add(view);
                container.add(outside);
                view.setBounds(0, 0, 100, 100);
                outside.setBounds(100, 0, 100, 100);
                PathTool tool = synchronousTool();
                tool.setView(view);
                tool.setActive(true);
                try {
                    int listeners = java.awt.Toolkit.getDefaultToolkit()
                            .getAWTEventListeners(java.awt.AWTEvent.MOUSE_EVENT_MASK).length;
                    event(view, MouseEvent.MOUSE_PRESSED, 10, 10, MouseEvent.BUTTON1, InputEvent.BUTTON1_DOWN_MASK);
                    assertEquals(listeners + 1, java.awt.Toolkit.getDefaultToolkit()
                            .getAWTEventListeners(java.awt.AWTEvent.MOUSE_EVENT_MASK).length);
                    Point end = SwingUtilities.convertPoint(view, view.worldToView(20, 10), outside);
                    outside.dispatchEvent(new MouseEvent(outside, MouseEvent.MOUSE_RELEASED, 0, 0, end.x, end.y, 1,
                            false, MouseEvent.BUTTON1));
                    assertEquals(List.of(20f, 10f, 70f, 4f), dimension.getAttribute(HANDLES));
                    assertEquals(listeners, java.awt.Toolkit.getDefaultToolkit()
                            .getAWTEventListeners(java.awt.AWTEvent.MOUSE_EVENT_MASK).length);
                } finally {
                    tool.setActive(false);
                }
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }

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
                PathTool tool = synchronousTool();
                tool.setView(view);
                tool.setActive(true);
                try {
                    click(view, 10, 10);
                    click(view, 30, 30);
                    assertEquals(Set.of(0, 1), tool.selectedNodes());
                    click(view, 10, 10);
                    assertEquals(Set.of(1), tool.selectedNodes());
                    event(view, MouseEvent.MOUSE_PRESSED, -10, -10, MouseEvent.BUTTON1, InputEvent.BUTTON1_DOWN_MASK);
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
    void oneShotPlacementAndEscapeCancellation() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Dimension dimension = dimension();
                TestView view = new TestView(dimension);
                PathTool tool = synchronousTool();
                tool.setView(view);
                Brush brush = (Brush) java.lang.reflect.Proxy.newProxyInstance(Brush.class.getClassLoader(),
                        new Class<?>[]{Brush.class}, (proxy, method, args) -> switch (method.getName()) {
                            case "getEffectiveRadius", "getRadius" -> 4;
                            default -> null;
                        });
                tool.setBrush(brush);
                tool.setActive(true);
                try {
                    tool.tick(10, 10, true, true, 1f);
                    tool.tick(20, 20, true, false, 1f);
                    tool.tick(30, 30, false, true, 1f);
                    tool.tick(30, 30, true, true, 1f);
                    var expected = List.of(10f, 10f, 70f, 4f, 30f, 30f, 70f, 4f);
                    assertEquals(expected, dimension.getAttribute(HANDLES));
                    event(view, MouseEvent.MOUSE_PRESSED, 10, 10, MouseEvent.BUTTON1, InputEvent.BUTTON1_DOWN_MASK);
                    event(view, MouseEvent.MOUSE_DRAGGED, 20, 10, MouseEvent.NOBUTTON, InputEvent.BUTTON1_DOWN_MASK);
                    event(view, MouseEvent.MOUSE_ENTERED, 20, 10, MouseEvent.NOBUTTON, 0);
                    event(view, MouseEvent.MOUSE_EXITED, 20, 10, MouseEvent.NOBUTTON, 0);
                    assertTrue(tool.dispatchKeyEvent(new KeyEvent(view, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_ESCAPE,
                            KeyEvent.CHAR_UNDEFINED)));
                    event(view, MouseEvent.MOUSE_RELEASED, 20, 10, MouseEvent.BUTTON1, 0);
                    assertEquals(expected, dimension.getAttribute(HANDLES));
                } finally {
                    tool.setActive(false);
                }
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    @Test
    void backspaceDuringDragCannotRestoreDeletedNodeOnRelease() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Dimension dimension = dimension();
                dimension.setAttribute(HANDLES, new ArrayList<>(List.of(10f, 10f, 70f, 4f, 30f, 30f, 70f, 6f)), true);
                TestView view = new TestView(dimension);
                PathTool tool = synchronousTool();
                tool.setView(view);
                tool.setActive(true);
                try {
                    event(view, MouseEvent.MOUSE_ENTERED, 10, 10, MouseEvent.NOBUTTON, 0);
                    event(view, MouseEvent.MOUSE_PRESSED, 10, 10, MouseEvent.BUTTON1, InputEvent.BUTTON1_DOWN_MASK);
                    event(view, MouseEvent.MOUSE_DRAGGED, 20, 10, MouseEvent.NOBUTTON, InputEvent.BUTTON1_DOWN_MASK);
                    assertTrue(tool.dispatchKeyEvent(new KeyEvent(view, KeyEvent.KEY_PRESSED, 0, 0,
                            KeyEvent.VK_BACK_SPACE, KeyEvent.CHAR_UNDEFINED)));
                    event(view, MouseEvent.MOUSE_RELEASED, 20, 10, MouseEvent.BUTTON1, 0);
                    assertEquals(List.of(10f, 10f, 70f, 4f), dimension.getAttribute(HANDLES));
                } finally {
                    tool.setActive(false);
                }
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    @Test
    void draggingGroupIsAtomicAndResamplesHeightsOnRelease() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Dimension dimension = dimension();
                var initial = List.of(10f, 10f, 55f, 4f, 30f, 30f, 56f, 6f);
                dimension.setAttribute(HANDLES, new ArrayList<>(initial), true);
                dimension.setHeightAt(20, 10, 81f);
                dimension.setHeightAt(40, 30, 92f);
                TestView view = new TestView(dimension);
                PathTool tool = synchronousTool();
                tool.setView(view);
                tool.setActive(true);
                try {
                    click(view, 10, 10);
                    click(view, 30, 30);
                    event(view, MouseEvent.MOUSE_PRESSED, 10, 10, MouseEvent.BUTTON1, InputEvent.BUTTON1_DOWN_MASK);
                    event(view, MouseEvent.MOUSE_DRAGGED, 20, 10, MouseEvent.NOBUTTON, InputEvent.BUTTON1_DOWN_MASK);
                    assertEquals(initial, dimension.getAttribute(HANDLES));
                    event(view, MouseEvent.MOUSE_RELEASED, 20, 10, MouseEvent.BUTTON1, 0);
                    assertEquals(List.of(20f, 10f, 81f, 4f, 40f, 30f, 92f, 6f), dimension.getAttribute(HANDLES));
                    assertEquals(Set.of(0, 1), tool.selectedNodes());
                    boolean snapEnabled = false;
                    for (var component : tool.getOptionsPanel().getComponents())
                        if (component instanceof javax.swing.JCheckBox checkbox
                                && "snap to terrain".equals(checkbox.getText())) {
                            checkbox.doClick();
                            snapEnabled = checkbox.isSelected();
                        }
                    assertTrue(snapEnabled);
                    dimension.setHeightAt(30, 10, 83f);
                    dimension.setHeightAt(50, 30, 94f);
                    event(view, MouseEvent.MOUSE_PRESSED, 20, 10, MouseEvent.BUTTON1, InputEvent.BUTTON1_DOWN_MASK);
                    event(view, MouseEvent.MOUSE_DRAGGED, 30, 10, MouseEvent.NOBUTTON, InputEvent.BUTTON1_DOWN_MASK);
                    event(view, MouseEvent.MOUSE_RELEASED, 30, 10, MouseEvent.BUTTON1, 0);
                    assertEquals(List.of(30f, 10f, 83f, 4f, 50f, 30f, 94f, 6f), dimension.getAttribute(HANDLES));
                    event(view, MouseEvent.MOUSE_PRESSED, 30, 10, MouseEvent.BUTTON1, InputEvent.BUTTON1_DOWN_MASK);
                    event(view, MouseEvent.MOUSE_DRAGGED, 120, 10, MouseEvent.NOBUTTON, InputEvent.BUTTON1_DOWN_MASK);
                    event(view, MouseEvent.MOUSE_RELEASED, 120, 10, MouseEvent.BUTTON1, 0);
                    assertEquals(List.of(30f, 10f, 83f, 4f, 50f, 30f, 94f, 6f), dimension.getAttribute(HANDLES));
                } finally {
                    tool.setActive(false);
                }
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    @Test
    void keyboardDeletionAndResetClearHandlesAndSelection() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Dimension dimension = dimension();
                dimension.setAttribute(HANDLES,
                        new ArrayList<>(List.of(10f, 10f, 70f, 4f, 30f, 30f, 70f, 6f, 50f, 50f, 70f, 8f)), true);
                TestView view = new TestView(dimension);
                PathTool tool = synchronousTool();
                tool.setView(view);
                tool.setActive(true);
                try {
                    click(view, 30, 30);
                    KeyEvent delete = new KeyEvent(view, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_DELETE,
                            KeyEvent.CHAR_UNDEFINED);
                    assertFalse(tool.dispatchKeyEvent(delete));
                    event(view, MouseEvent.MOUSE_ENTERED, 30, 30, MouseEvent.NOBUTTON, 0);
                    assertTrue(tool.dispatchKeyEvent(delete));
                    assertEquals(List.of(10f, 10f, 70f, 4f, 50f, 50f, 70f, 8f), dimension.getAttribute(HANDLES));
                    assertEquals(Set.of(), tool.selectedNodes());
                    assertTrue(tool.dispatchKeyEvent(new KeyEvent(view, KeyEvent.KEY_PRESSED, 0,
                            InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_A, 'a')));
                    assertEquals(Set.of(0, 1), tool.selectedNodes());
                    JButton reset = findButton(tool.getOptionsPanel(), "PathTool.NewPath");
                    assertNotNull(reset);
                    reset.doClick();
                    assertEquals(List.of(), dimension.getAttribute(HANDLES));
                    assertEquals(Set.of(), tool.selectedNodes());
                    assertTrue(tool.cachedTiles.isEmpty());
                } finally {
                    tool.setActive(false);
                }
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    private static JButton findButton(java.awt.Container container, String name) {
        for (var component : container.getComponents()) {
            if (component instanceof JButton button && name.equals(button.getName()))
                return button;
            if (component instanceof java.awt.Container child) {
                JButton result = findButton(child, name);
                if (result != null)
                    return result;
            }
        }
        return null;
    }

    @Test
    void nearestNodeUsesScreenPixelsAndStableOrderAndLifecycleCleansUp() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Dimension dimension = dimension();
                dimension.setAttribute(HANDLES, new ArrayList<>(List.of(10f, 10f, 70f, 4f, 12f, 10f, 70f, 6f)), true);
                TestView view = new TestView(dimension);
                var previous = view.getViewListener();
                PathTool tool = synchronousTool();
                tool.setView(view);
                tool.setActive(true);
                assertEquals(0, tool.nearestNode(view.worldToView(11, 10)));
                assertEquals(-1, tool.nearestNode(view.worldToView(50, 50)));
                assertEquals(1,
                        java.util.Arrays.stream(view.getComponents()).filter(MapToolOverlay.class::isInstance).count());
                MapToolOverlay overlay = (MapToolOverlay) java.util.Arrays.stream(view.getComponents())
                        .filter(MapToolOverlay.class::isInstance)
                        .findFirst()
                        .orElseThrow();
                assertTrue(overlay.getComponent(0).isVisible());
                click(view, 10, 10);
                view.setDimension(dimension());
                assertEquals(Set.of(), tool.selectedNodes());
                tool.setActive(false);
                assertSame(previous, view.getViewListener());
                assertEquals(0,
                        java.util.Arrays.stream(view.getComponents()).filter(MapToolOverlay.class::isInstance).count());
                assertFalse(overlay.getComponent(0).isVisible());
                tool.setActive(true);
                assertTrue(overlay.getComponent(0).isVisible());
                tool.setActive(false);
                assertFalse(overlay.getComponent(0).isVisible());
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

    static class TestView extends WorldPainterView
    {
        private Dimension dimension;
        private final MapDragControl mapDragControl = new MapDragControl() {
            private boolean inhibited;

            @Override
            public boolean isMapDraggingInhibited() {
                return inhibited;
            }

            @Override
            public void setMapDraggingInhibited(boolean inhibited) {
                this.inhibited = inhibited;
            }
        };
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
            return mapDragControl;
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
