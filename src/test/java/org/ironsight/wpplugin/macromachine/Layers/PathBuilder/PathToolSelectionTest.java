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
                    tool[0].addNode(10 + i * 5, 20);
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
            tool[0].addNode(70, 60);
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
    void selectionDoesNotInvalidatePendingPreviewOrChangeHandles() throws Exception {
        var work = new ArrayList<Runnable>();
        var callbacks = new ArrayList<Runnable>();
        var tool = new PathTool[]{null};
        var dim = new Dimension[]{null};
        SwingUtilities.invokeAndWait(() -> {
            try {
                dim[0] = dimension();
                var view = new TestView(dim[0]);
                tool[0] = new PathTool(
                        () -> new PathPreviewWorker(work::add, callbacks::add, PathPreviewMask::calculate));
                tool[0].setView(view);
                tool[0].setBrush(brush());
                tool[0].setActive(true);
                tool[0].addNode(20, 20);
                var handles = List.copyOf(dim[0].getAttribute(HANDLES));
                click(view, 20, 20);
                click(view, 20, 20, InputEvent.CTRL_DOWN_MASK);
                assertEquals(Set.of(), tool[0].selectedNodes());
                click(view, 20, 20, InputEvent.CTRL_DOWN_MASK);
                assertEquals(Set.of(0), tool[0].selectedNodes());
                assertEquals(handles, dim[0].getAttribute(HANDLES));
                assertEquals(1, work.size());
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
        work.remove(0).run();
        SwingUtilities.invokeAndWait(() -> {
            callbacks.remove(0).run();
            assertTrue(dim[0].getBitLayerValueAt(PreviewOperation.annotationLayer, 20, 20));
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
                tool[0].addNode(20, 20);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        work.remove(0).run();
        SwingUtilities.invokeAndWait(() -> {
            dim[0].setAttribute(HANDLES, new ArrayList<>(), true);
            callbacks.remove(0).run();
            assertFalse(dim[0].getBitLayerValueAt(PreviewOperation.annotationLayer, 20, 20));
            tool[0].addNode(30, 20);
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
                    tool[0].addNode(20, 20);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            work.remove(0).run();
            SwingUtilities.invokeAndWait(() -> {
                switch (action) {
                    case "off" -> overlayPart(tool[0], "PathTool.ShowLayer").doClick();
                    case "dimension" -> {
                        view[0].setDimension(dimension());
                        tool[0].selectedNodes();
                    }
                    case "deactivate" -> assertDoesNotThrow(() -> tool[0].setActive(false));
                    case "apply" -> findButton(tool[0].pathSettingsPanel(), "PathTool.Apply").doClick();
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
                    // the editor button sits over this test path's top-left corner
                    for (String name : List.of("PathTool.EditorMode", "PathTool.NodeMenu", "PathTool.SegmentMenu"))
                        findComponent(view, javax.swing.JButton.class, name).setVisible(false);
                    var label = overlayPart(tool, "PathTool.ShowLabel");
                    var radius = overlayPart(tool, "PathTool.ShowRadius");
                    var line = overlayPart(tool, "PathTool.ShowLine");
                    var nodes = overlayPart(tool, "PathTool.ShowNodes");
                    var layer = overlayPart(tool, "PathTool.ShowLayer");
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
                    tool.addNode(20, 20);
                    assertTrue(dimension.getBitLayerValueAt(PreviewOperation.annotationLayer, 20, 20));
                    var handles = List.copyOf(dimension.getAttribute(HANDLES));
                    var layer = overlayPart(tool, "PathTool.ShowLayer");
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
                    tool.addNode(60, 20);
                    var spinner = PathTool.class.getDeclaredField("handleFactorSpinner");
                    spinner.setAccessible(true);
                    ((JSpinner) spinner.get(tool)).setValue(1.0d);
                    assertFalse(dimension.getBitLayerValueAt(PreviewOperation.annotationLayer, 60, 20));
                    dimension.setHeightAt(60, 20, 90f);
                    findButton(tool.pathSettingsPanel(), "PathTool.Apply").doClick();
                    assertNotEquals(90f, dimension.getHeightAt(60, 20));
                    assertFalse(dimension.getBitLayerValueAt(PreviewOperation.annotationLayer, 60, 20));
                    tool.setActive(false);
                    tool.setActive(true);
                    // overlay switches are not persisted: re-activation turns the layer back on
                    assertTrue(layer.isSelected());
                    layer.doClick();
                    layer.doClick(); // off and on again refreshes the preview synchronously
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

    @Test
    void viewportOverlayButtonHidesAllOverlaysAndMenuTogglesParts() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Dimension dimension = dimension();
                TestView view = new TestView(dimension);
                PathTool tool = synchronousTool();
                tool.setView(view);
                tool.setBrush(brush());
                tool.setActive(true);
                try {
                    tool.addNode(120, 120);
                    tool.addNode(150, 140);
                    assertTrue(dimension.getBitLayerValueAt(PreviewOperation.annotationLayer, 120, 120));
                    // switches left the options panel for the viewport
                    assertNull(findToggle(tool.getOptionsPanel(), "PathTool.ShowLabel"));
                    var editor = findComponent(view, JButton.class, "PathTool.EditorMode");
                    var overlaysButton = findComponent(view, JToggleButton.class, "PathTool.OverlaysButton");
                    var menuButton = findComponent(view, JButton.class, "PathTool.OverlaysMenu");
                    assertNotNull(overlaysButton);
                    assertNotNull(menuButton);
                    assertTrue(overlaysButton.getX() > editor.getX() + editor.getWidth());
                    assertEquals(overlaysButton.getX() + overlaysButton.getWidth(), menuButton.getX());
                    var overlay = (MapToolOverlay) overlaysButton.getParent();
                    assertTrue(overlay.contains(overlaysButton.getX() + 2, overlaysButton.getY() + 2));
                    assertTrue(overlay.contains(menuButton.getX() + 2, menuButton.getY() + 2));
                    overlay.setSize(200, 200);

                    // buttons share the switches' state; single clicks toggle / only / all but
                    var menu = tool.overlaysMenu();
                    var lineItem = findComponent(menu, JToggleButton.class, "PathTool.ShowLine");
                    assertEquals("Path line", lineItem.getText());
                    lineItem.doClick();
                    assertFalse(overlayPart(tool, "PathTool.ShowLine").isSelected());
                    lineItem.doClick();
                    assertTrue(overlayPart(tool, "PathTool.ShowLine").isSelected());
                    findComponent(menu, JButton.class, "PathTool.ShowLine.Only").doClick();
                    for (var part : tool.overlayParts())
                        assertEquals("PathTool.ShowLine".equals(part.getName()), part.isSelected(),
                                "only: " + part.getName());
                    findComponent(menu, JButton.class, "PathTool.ShowLine.AllBut").doClick();
                    for (var part : tool.overlayParts())
                        assertEquals(!"PathTool.ShowLine".equals(part.getName()), part.isSelected(),
                                "all but: " + part.getName());
                    tool.overlaysSwitch().setSelected(false);
                    findComponent(menu, JButton.class, "PathTool.Overlays.All").doClick();
                    assertTrue(tool.overlaysSwitch().isSelected(), "choosing parts switches overlays on");
                    for (var part : tool.overlayParts())
                        assertTrue(part.isSelected(), "all: " + part.getName());

                    // select node 0 so its radius handle is shown
                    click(view, 120, 120);
                    Point node = view.worldToView(120, 120);
                    var withOverlays = paintOverlay(overlay);
                    // path area only: buttons (top) and the transient info message (bottom) are not
                    // overlays
                    var blank = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB);
                    assertNotEquals(0, differentPixels(withOverlays, blank, 95, 95, 85, 65));
                    var handle = new Point(node.x + 22, node.y);
                    assertEquals(new PathTool.Hit(PathTool.HitKind.RADIUS, 0, 0), tool.hitTest(handle));

                    // master switch off: nothing drawn, no yellow layer, handles not grabbable,
                    // nodes still clickable (like Blender)
                    overlaysButton.doClick();
                    assertFalse(tool.overlaysSwitch().isSelected());
                    assertEquals(0, differentPixels(paintOverlay(overlay), blank, 95, 95, 85, 65));
                    assertFalse(dimension.getBitLayerValueAt(PreviewOperation.annotationLayer, 120, 120));
                    assertNull(tool.hitTest(handle));
                    assertEquals(new PathTool.Hit(PathTool.HitKind.NODE, 0, 0), tool.hitTest(node));
                    assertTrue(overlayPart(tool, "PathTool.ShowNodes").isSelected(), "parts keep their state");

                    overlaysButton.doClick();
                    assertTrue(dimension.getBitLayerValueAt(PreviewOperation.annotationLayer, 120, 120));
                    assertEquals(0, differentPixels(paintOverlay(overlay), withOverlays, 95, 95, 85, 65));
                } finally {
                    tool.setActive(false);
                }
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    @Test
    void reverseButtonFlipsNodeOrderAndKeepsSelectedNodes() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Dimension dimension = dimension();
                var initial = List.of(10f, 10f, 70f, 4f, 40f, 10f, 70f, 6f, 70f, 10f, 70f, 8f);
                dimension.setAttribute(HANDLES, new ArrayList<>(initial), true);
                TestView view = new TestView(dimension);
                PathTool tool = synchronousTool();
                tool.setView(view);
                tool.setActive(true);
                try {
                    click(view, 10, 10);
                    click(view, 40, 10, InputEvent.CTRL_DOWN_MASK);
                    var reverse = findButton(tool.getOptionsPanel(), "PathTool.ReversePath");
                    assertNotNull(reverse);
                    reverse.doClick();
                    assertEquals(List.of(70f, 10f, 70f, 8f, 40f, 10f, 70f, 6f, 10f, 10f, 70f, 4f),
                            dimension.getAttribute(HANDLES));
                    // same nodes stay selected under their new indices
                    assertEquals(Set.of(2, 1), tool.selectedNodes());
                    reverse.doClick();
                    assertEquals(initial, dimension.getAttribute(HANDLES));
                    assertEquals(Set.of(0, 1), tool.selectedNodes());
                } finally {
                    tool.setActive(false);
                }
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    private static boolean type(PathTool tool, TestView view, int keyCode, char keyChar) {
        return tool.dispatchKeyEvent(new KeyEvent(view, KeyEvent.KEY_PRESSED, 0, 0, keyCode, keyChar));
    }

    private static void type(PathTool tool, TestView view, String text) {
        for (char c : text.toCharArray())
            assertTrue(type(tool, view, KeyEvent.VK_UNDEFINED, c), "consumed " + c);
    }

    @Test
    void typingANumberInAnEditorSetsTheValueAbsolutely() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Dimension dimension = dimension();
                dimension.setAttribute(HANDLES, new ArrayList<>(List.of(50f, 50f, 70f, 10f, 90f, 90f, 70f, 6f)), true);
                TestView view = new TestView(dimension);
                PathTool tool = synchronousTool();
                tool.setView(view);
                tool.setBrush(brush()); // a right click would append a node
                tool.setActive(true);
                try {
                    event(view, MouseEvent.MOUSE_MOVED, 10, 90, MouseEvent.NOBUTTON, 0);
                    // nothing selected: number keys are left to WorldPainter
                    assertFalse(type(tool, view, KeyEvent.VK_1, '1'));
                    click(view, 50, 50);
                    click(view, 90, 90, InputEvent.CTRL_DOWN_MASK);
                    // '-' does not start typing (WorldPainter zooms out with it)
                    assertFalse(type(tool, view, KeyEvent.VK_MINUS, '-'));

                    // Radius editor, type 12 -> every selected radius is 12
                    type(tool, view, "1");
                    assertTrue(tool.typingValue());
                    assertEquals("Radius: 1|", tool.gestureHeader());
                    type(tool, view, "2");
                    assertEquals("Radius: 12|", tool.gestureHeader());
                    assertEquals(List.of(50f, 50f, 70f, 10f, 90f, 90f, 70f, 6f), dimension.getAttribute(HANDLES),
                            "only previewed until applied");
                    assertTrue(type(tool, view, KeyEvent.VK_ENTER, '\n'));
                    assertEquals(List.of(50f, 50f, 70f, 12f, 90f, 90f, 70f, 12f), dimension.getAttribute(HANDLES));
                    assertFalse(tool.typingValue());
                    assertNull(tool.gestureHeader());
                    assertEquals(Set.of(0, 1), tool.selectedNodes());

                    // Escape cancels and keeps the selection
                    type(tool, view, "7");
                    assertTrue(type(tool, view, KeyEvent.VK_ESCAPE, KeyEvent.CHAR_UNDEFINED));
                    assertEquals(List.of(50f, 50f, 70f, 12f, 90f, 90f, 70f, 12f), dimension.getAttribute(HANDLES));
                    assertEquals(Set.of(0, 1), tool.selectedNodes());
                    assertFalse(tool.typingValue());

                    // a left click on the map applies, and selects nothing else
                    type(tool, view, "5");
                    click(view, 10, 90);
                    assertEquals(List.of(50f, 50f, 70f, 5f, 90f, 90f, 70f, 5f), dimension.getAttribute(HANDLES));
                    assertEquals(Set.of(0, 1), tool.selectedNodes());

                    // a right click cancels instead of appending a node
                    type(tool, view, "9");
                    tool.tick(30, 30, true, true, 1f);
                    assertEquals(List.of(50f, 50f, 70f, 5f, 90f, 90f, 70f, 5f), dimension.getAttribute(HANDLES));
                    assertFalse(tool.typingValue());

                    // also while dragging a handle the typed number is the value
                    var handle = view.worldToView(50 + 22, 50);
                    event(view, MouseEvent.MOUSE_PRESSED, handle.x, handle.y, MouseEvent.BUTTON1,
                            InputEvent.BUTTON1_DOWN_MASK);
                    type(tool, view, "2.5");
                    assertEquals("Radius: 2.5|", tool.gestureHeader());
                    event(view, MouseEvent.MOUSE_RELEASED, handle.x + 30, handle.y, MouseEvent.BUTTON1, 0);
                    assertEquals(List.of(50f, 50f, 70f, 2.5f, 90f, 90f, 70f, 2.5f), dimension.getAttribute(HANDLES));
                } finally {
                    tool.setActive(false);
                }
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    @Test
    void typedHeightAboveGroundAndCurveStrengthAreAbsolute() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Dimension dimension = dimension();
                dimension.setHeightAt(50, 50, 70f);
                dimension.setHeightAt(90, 90, 60f);
                dimension.setAttribute(HANDLES, new ArrayList<>(List.of(50f, 50f, 72f, 4f, 90f, 90f, 60f, 4f)), true);
                TestView view = new TestView(dimension);
                PathTool tool = synchronousTool();
                tool.setView(view);
                tool.setActive(true);
                try {
                    click(view, 50, 50);
                    click(view, 90, 90, InputEvent.CTRL_DOWN_MASK);

                    // height editor: 5 -> every selected node 5 blocks above its own ground
                    tool.setEditMode(PathTool.EditMode.HEIGHT);
                    type(tool, view, "5");
                    assertEquals("Height above ground: 5|", tool.gestureHeader());
                    assertTrue(type(tool, view, KeyEvent.VK_ENTER, '\n'));
                    assertEquals(List.of(50f, 50f, 75f, 4f, 90f, 90f, 65f, 4f), dimension.getAttribute(HANDLES));
                    // '-' negates once typing: 2 then '-' = 2 blocks below ground
                    type(tool, view, "2");
                    assertTrue(type(tool, view, KeyEvent.VK_MINUS, '-'));
                    assertEquals("Height above ground: -(2|)", tool.gestureHeader());
                    assertTrue(type(tool, view, KeyEvent.VK_ENTER, '\n'));
                    assertEquals(List.of(50f, 50f, 68f, 4f, 90f, 90f, 58f, 4f), dimension.getAttribute(HANDLES));

                    // curve strength editor: 0.8 -> strength 0.8
                    tool.setEditMode(PathTool.EditMode.CURVE);
                    type(tool, view, "0.8");
                    assertEquals("Curve strength: 0.8|", tool.gestureHeader());
                    assertTrue(type(tool, view, KeyEvent.VK_ENTER, '\n'));
                    assertEquals(0.8f, tool.curveStrength(), 1e-6);
                } finally {
                    tool.setActive(false);
                }
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    @Test
    void insertionIndexFollowsHighestSelectedNode() {
        assertEquals(3, PathTool.insertionIndex(Set.of(), 3), "nothing selected: append");
        assertEquals(1, PathTool.insertionIndex(Set.of(0), 3));
        assertEquals(3, PathTool.insertionIndex(Set.of(0, 2), 3), "after the last node: append");
        assertEquals(2, PathTool.insertionIndex(Set.of(1, 0), 3));
        assertEquals(3, PathTool.insertionIndex(Set.of(7), 3), "stale indices are ignored");
    }

    @Test
    void ctrlClickOnEmptyMapInsertsOneNodeAfterTheHighestSelectedNode() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Dimension dimension = dimension();
                dimension.setAttribute(HANDLES,
                        new ArrayList<>(List.of(10f, 10f, 70f, 4f, 50f, 10f, 70f, 4f, 90f, 10f, 70f, 4f)), true);
                TestView view = new TestView(dimension);
                PathTool tool = synchronousTool();
                tool.setView(view);
                tool.setBrush(brush());
                tool.setActive(true);
                try {
                    // two selected nodes, one new node: inserted into the segment after node 1
                    click(view, 10, 10);
                    click(view, 50, 10, InputEvent.CTRL_DOWN_MASK);
                    click(view, 70, 40, InputEvent.CTRL_DOWN_MASK);
                    var handles = dimension.getAttribute(HANDLES);
                    assertEquals(16, handles.size(), "exactly one node added");
                    assertEquals(List.of(70f, 40f), handles.subList(8, 10));
                    assertEquals(List.of(90f, 10f), handles.subList(12, 14), "old last node is still last");
                    assertEquals(Set.of(2), tool.selectedNodes(), "the new node is selected");
                    // repeated inserts continue in order
                    click(view, 75, 60, InputEvent.CTRL_DOWN_MASK);
                    handles = dimension.getAttribute(HANDLES);
                    assertEquals(List.of(70f, 40f), handles.subList(8, 10));
                    assertEquals(List.of(75f, 60f), handles.subList(12, 14));
                    assertEquals(Set.of(3), tool.selectedNodes());
                    // Ctrl+click on a node still toggles it
                    click(view, 10, 10, InputEvent.CTRL_DOWN_MASK);
                    assertEquals(Set.of(3, 0), tool.selectedNodes());
                    assertEquals(20, dimension.getAttribute(HANDLES).size());

                    // nothing selected: append at the end, selection stays empty
                    tool.dispatchKeyEvent(new KeyEvent(view, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_ESCAPE,
                            KeyEvent.CHAR_UNDEFINED));
                    assertEquals(Set.of(), tool.selectedNodes());
                    click(view, 30, 80, InputEvent.CTRL_DOWN_MASK);
                    handles = dimension.getAttribute(HANDLES);
                    assertEquals(List.of(30f, 80f), handles.subList(handles.size() - 4, handles.size() - 2));
                    assertEquals(Set.of(), tool.selectedNodes());

                    // right click no longer adds nodes (kept free for a context menu)
                    int before = dimension.getAttribute(HANDLES).size();
                    tool.tick(20, 90, true, true, 1f);
                    assertEquals(before, dimension.getAttribute(HANDLES).size());
                } finally {
                    tool.setActive(false);
                }
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    private static void tapCtrl(PathTool tool, TestView view, long when, long holdMs) {
        tool.dispatchKeyEvent(new KeyEvent(view, KeyEvent.KEY_PRESSED, when, InputEvent.CTRL_DOWN_MASK,
                KeyEvent.VK_CONTROL, KeyEvent.CHAR_UNDEFINED));
        tool.dispatchKeyEvent(new KeyEvent(view, KeyEvent.KEY_RELEASED, when + holdMs, 0, KeyEvent.VK_CONTROL,
                KeyEvent.CHAR_UNDEFINED));
    }

    @Test
    void doubleTapCtrlTogglesStickyAddMode() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Dimension dimension = dimension();
                dimension.setAttribute(HANDLES, new ArrayList<>(List.of(10f, 10f, 70f, 4f)), true);
                TestView view = new TestView(dimension);
                PathTool tool = synchronousTool();
                tool.setView(view);
                tool.setBrush(brush());
                tool.setActive(true);
                var mapCursor = view.getCursor();
                try {
                    event(view, MouseEvent.MOUSE_MOVED, 60, 60, MouseEvent.NOBUTTON, 0);
                    assertFalse(tool.addModeToggle().isSelected());
                    // slow taps, long holds and interrupted taps do not count
                    tapCtrl(tool, view, 1000, 50);
                    tapCtrl(tool, view, 2000, 50);
                    tapCtrl(tool, view, 3000, 500);
                    tapCtrl(tool, view, 3600, 50);
                    tool.dispatchKeyEvent(new KeyEvent(view, KeyEvent.KEY_PRESSED, 3700, InputEvent.CTRL_DOWN_MASK,
                            KeyEvent.VK_A, 'a'));
                    tapCtrl(tool, view, 3750, 50);
                    assertFalse(tool.addModeToggle().isSelected());

                    // a quick double tap turns add mode on
                    tapCtrl(tool, view, 5000, 60);
                    tapCtrl(tool, view, 5200, 60);
                    assertTrue(tool.addModeToggle().isSelected());
                    var button = findComponent(view, JToggleButton.class, "PathTool.AddButton");
                    assertTrue(button.isSelected(), "viewport button shows the mode");
                    // empty map shows a crosshair; plain clicks add nodes
                    event(view, MouseEvent.MOUSE_MOVED, 60, 60, MouseEvent.NOBUTTON, 0);
                    assertEquals(java.awt.Cursor.CROSSHAIR_CURSOR, view.getCursor().getType());
                    click(view, 60, 60);
                    click(view, 90, 90);
                    assertEquals(List.of(10f, 10f, 70f, 4f, 60f, 60f), dimension.getAttribute(HANDLES).subList(0, 6));
                    assertEquals(12, dimension.getAttribute(HANDLES).size());
                    // clicks on nodes still select in add mode
                    click(view, 10, 10);
                    assertEquals(Set.of(0), tool.selectedNodes());
                    assertEquals(12, dimension.getAttribute(HANDLES).size());

                    // Escape leaves add mode first (selection kept); the button toggles too
                    tool.dispatchKeyEvent(new KeyEvent(view, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_ESCAPE,
                            KeyEvent.CHAR_UNDEFINED));
                    assertFalse(tool.addModeToggle().isSelected());
                    assertEquals(Set.of(0), tool.selectedNodes());
                    event(view, MouseEvent.MOUSE_MOVED, 60, 30, MouseEvent.NOBUTTON, 0);
                    assertEquals(mapCursor, view.getCursor());
                    button.doClick();
                    assertTrue(tool.addModeToggle().isSelected());
                    tapCtrl(tool, view, 9000, 60);
                    tapCtrl(tool, view, 9150, 60);
                    assertFalse(tool.addModeToggle().isSelected(), "double tap toggles off again");

                    // never persisted: off after re-activation
                    button.doClick();
                    tool.setActive(false);
                    tool.setActive(true);
                    assertFalse(tool.addModeToggle().isSelected());
                } finally {
                    tool.setActive(false);
                }
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    private static JButton operationButton(TestView view, String name) {
        return findComponent(view, JButton.class, name);
    }

    @Test
    void nodeAndSegmentMenusPreviewOperationsBeforeApplying() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Dimension dimension = dimension();
                var initial = List.of(20f, 60f, 70f, 4f, 60f, 60f, 70f, 8f, 100f, 60f, 70f, 4f);
                dimension.setAttribute(HANDLES, new ArrayList<>(initial), true);
                TestView view = new TestView(dimension);
                PathTool tool = synchronousTool();
                tool.setView(view);
                tool.setActive(true);
                try {
                    // nothing selected: menu items are disabled
                    assertFalse(findComponent(tool.nodeMenu(), javax.swing.JMenuItem.class, "PathTool.Node.Randomize")
                            .isEnabled());
                    click(view, 20, 60);
                    assertTrue(findComponent(tool.nodeMenu(), javax.swing.JMenuItem.class, "PathTool.Node.Randomize")
                            .isEnabled());
                    assertFalse(
                            findComponent(tool.segmentMenu(), javax.swing.JMenuItem.class, "PathTool.Segment.Subdivide")
                                    .isEnabled(),
                            "one node is no segment");
                    click(view, 60, 60, InputEvent.CTRL_DOWN_MASK);
                    var subdivide = findComponent(tool.segmentMenu(), javax.swing.JMenuItem.class,
                            "PathTool.Segment.Subdivide");
                    assertTrue(subdivide.isEnabled());

                    // Subdivide: preview first, the path is unchanged until Apply
                    subdivide.doClick();
                    var panel = findComponent(view, JPanel.class, "PathTool.OperationPanel");
                    assertNotNull(panel);
                    // top left below the toolbar: WorldPainter draws compass and scale bar bottom
                    // left
                    var editorButton = findComponent(view, JButton.class, "PathTool.EditorMode");
                    assertEquals(8, panel.getX());
                    assertTrue(panel.getY() >= editorButton.getY() + editorButton.getHeight(), "below the toolbar");
                    assertTrue(panel.getY() < 80);
                    assertEquals(4, tool.operationPreview().path().size());
                    assertEquals(initial, dimension.getAttribute(HANDLES));
                    var cuts = findComponent(view, JSpinner.class, "PathTool.Subdivide.Cuts");
                    cuts.setValue(3);
                    assertEquals(6, tool.operationPreview().path().size(), "preview follows the setting");
                    assertEquals(Set.of(1, 2, 3), tool.operationPreview().inserted(), "new nodes are marked");
                    operationButton(view, "PathTool.OperationApply").doClick();
                    var handles = dimension.getAttribute(HANDLES);
                    assertEquals(24, handles.size());
                    assertEquals(List.of(30f, 60f), handles.subList(4, 6));
                    assertEquals(5f, handles.get(7), 1e-4, "radius mixed from 4 and 8");
                    assertEquals(Set.of(0, 1, 2, 3, 4), tool.selectedNodes());
                    assertNull(findComponent(view, JPanel.class, "PathTool.OperationPanel"));
                    assertNull(tool.operationPreview());

                    // Cancel and Escape discard
                    var before = List.copyOf(dimension.getAttribute(HANDLES));
                    findComponent(tool.nodeMenu(), javax.swing.JMenuItem.class, "PathTool.Node.Randomize").doClick();
                    findComponent(view, JSpinner.class, "PathTool.Randomize.Radius").setValue(6.0);
                    assertNotEquals(before.subList(0, 2),
                            List.of(tool.operationPreview().path().get(0).x, tool.operationPreview().path().get(0).y));
                    operationButton(view, "PathTool.OperationCancel").doClick();
                    assertEquals(before, dimension.getAttribute(HANDLES));
                    findComponent(tool.nodeMenu(), javax.swing.JMenuItem.class, "PathTool.Node.Randomize").doClick();
                    assertTrue(tool.dispatchKeyEvent(new KeyEvent(view, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_ESCAPE,
                            KeyEvent.CHAR_UNDEFINED)));
                    assertNull(tool.operationPreview());
                    assertEquals(Set.of(0, 1, 2, 3, 4), tool.selectedNodes(), "selection kept");

                    // Randomize applied with Enter: selected nodes move within range, keep height
                    findComponent(tool.nodeMenu(), javax.swing.JMenuItem.class, "PathTool.Node.Randomize").doClick();
                    findComponent(view, JSpinner.class, "PathTool.Randomize.Radius").setValue(6.0);
                    assertTrue(tool
                            .dispatchKeyEvent(new KeyEvent(view, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_ENTER, '\n')));
                    handles = dimension.getAttribute(HANDLES);
                    for (int i = 0; i < 5; i++) {
                        float dx = handles.get(i * 4) - before.get(i * 4),
                                dy = handles.get(i * 4 + 1) - before.get(i * 4 + 1);
                        assertTrue(Math.hypot(dx, dy) <= 6.8, "node " + i + " moved " + Math.hypot(dx, dy));
                    }
                    assertEquals(before.subList(20, 24), handles.subList(20, 24), "unselected node 5 stays");

                    // clicking the map discards an open operation
                    findComponent(tool.nodeMenu(), javax.swing.JMenuItem.class, "PathTool.Node.Snap").doClick();
                    assertNotNull(tool.operationPreview());
                    click(view, 150, 150);
                    assertNull(tool.operationPreview());
                } finally {
                    tool.setActive(false);
                }
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    @Test
    void randomizeRadiusIsItsOwnDraggableValueOnlyWhileTheOperationIsOpen() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Dimension dimension = dimension();
                var initial = List.of(60f, 60f, 70f, 10f, 140f, 60f, 70f, 10f);
                dimension.setAttribute(HANDLES, new ArrayList<>(initial), true);
                TestView view = new TestView(dimension);
                PathTool tool = synchronousTool();
                tool.setView(view);
                tool.setActive(true);
                int button = InputEvent.BUTTON1_DOWN_MASK;
                try {
                    assertNull(tool.randomizeRadiusSpinner());
                    click(view, 60, 60);
                    findComponent(tool.nodeMenu(), javax.swing.JMenuItem.class, "PathTool.Node.Randomize").doClick();
                    var radius = tool.randomizeRadiusSpinner();
                    assertSame(findComponent(view, JSpinner.class, "PathTool.Randomize.Radius"), radius);
                    assertEquals(4.0, ((Number) radius.getValue()).doubleValue(), "blocks, not x node radius");
                    // the handle sits on the randomize circle (4 blocks < min offset -> 22 px
                    // right)
                    assertEquals(new PathTool.Hit(PathTool.HitKind.OPERATION_RADIUS, 0, 0),
                            tool.hitTest(new Point(60 + 22, 60)));
                    // dragging it changes the randomize radius, never the node radius; the
                    // operation stays open
                    event(view, MouseEvent.MOUSE_PRESSED, 82, 60, MouseEvent.BUTTON1, button);
                    event(view, MouseEvent.MOUSE_DRAGGED, 92, 60, MouseEvent.NOBUTTON, button);
                    event(view, MouseEvent.MOUSE_RELEASED, 92, 60, MouseEvent.BUTTON1, 0);
                    assertNotNull(tool.operationPreview(), "operation still open");
                    assertEquals(14.0, ((Number) radius.getValue()).doubleValue());
                    assertEquals(initial, dimension.getAttribute(HANDLES), "nothing applied yet");
                    // typing a number while dragging sets it exactly
                    var handle = PathEditHandles.radiusHandle(new java.awt.geom.Point2D.Double(60, 60), 14);
                    event(view, MouseEvent.MOUSE_PRESSED, (int) handle.x, (int) handle.y, MouseEvent.BUTTON1, button);
                    type(tool, view, "3");
                    assertEquals("Randomize radius (blocks): 3|", tool.gestureHeader());
                    event(view, MouseEvent.MOUSE_RELEASED, (int) handle.x + 40, (int) handle.y, MouseEvent.BUTTON1, 0);
                    assertEquals(3.0, ((Number) radius.getValue()).doubleValue());

                    operationButton(view, "PathTool.OperationApply").doClick();
                    var handles = dimension.getAttribute(HANDLES);
                    assertTrue(Math.hypot(handles.get(0) - 60, handles.get(1) - 60) <= 3.8);
                    assertEquals(10f, handles.get(3), "node radius untouched");
                    assertEquals(initial.subList(4, 8), handles.subList(4, 8), "unselected node untouched");
                    // the randomize radius only lives as long as the operation
                    assertNull(tool.randomizeRadiusSpinner());
                    findComponent(tool.nodeMenu(), javax.swing.JMenuItem.class, "PathTool.Node.Randomize").doClick();
                    assertEquals(4.0, ((Number) tool.randomizeRadiusSpinner().getValue()).doubleValue(),
                            "a new operation starts fresh");
                    // clicking elsewhere on the map ends it
                    click(view, 150, 150);
                    assertNull(tool.randomizeRadiusSpinner());
                } finally {
                    tool.setActive(false);
                }
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    @Test
    void randomizePreviewDrawsDottedLinesFromOldToNewPositions() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Dimension dimension = dimension();
                dimension.setAttribute(HANDLES, new ArrayList<>(List.of(120f, 120f, 70f, 4f)), true);
                TestView view = new TestView(dimension);
                PathTool tool = synchronousTool();
                tool.setView(view);
                tool.setActive(true);
                try {
                    click(view, 120, 120);
                    findComponent(tool.nodeMenu(), javax.swing.JMenuItem.class, "PathTool.Node.Randomize").doClick();
                    tool.randomizeRadiusSpinner().setValue(60.0);
                    var seed = findComponent(view, JSpinner.class, "PathTool.Randomize.Seed");
                    for (int s = 1; s < 200; s++) {
                        seed.setValue(s);
                        var moved = tool.operationPreview().path().get(0);
                        if (Math.hypot(moved.x - 120, moved.y - 120) >= 30)
                            break;
                    }
                    var moved = tool.operationPreview().path().get(0);
                    assertTrue(Math.hypot(moved.x - 120, moved.y - 120) >= 30, "found a far move");
                    var overlay = (MapToolOverlay) findComponent(view, JButton.class, "PathTool.EditorMode")
                            .getParent();
                    overlay.setSize(300, 300);
                    findComponent(view, JPanel.class, "PathTool.OperationPanel").setVisible(false);
                    Point old = view.worldToView(120, 120);
                    assertTrue(amberPixelsNear(paintOverlay300(overlay), old, 14) > 0, "dotted line near old position");
                    operationButton(view, "PathTool.OperationCancel").doClick();
                    assertEquals(0, amberPixelsNear(paintOverlay300(overlay), old, 14), "gone after cancel");
                } finally {
                    tool.setActive(false);
                }
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    private static BufferedImage paintOverlay300(MapToolOverlay overlay) {
        var image = new BufferedImage(300, 300, BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        overlay.paint(graphics);
        graphics.dispose();
        return image;
    }

    private static int amberPixelsNear(BufferedImage image, Point center, int radius) {
        int count = 0;
        for (int x = center.x - radius; x <= center.x + radius; x++)
            for (int y = center.y - radius; y <= center.y + radius; y++) {
                if (x < 0 || y < 0 || x >= image.getWidth() || y >= image.getHeight() || center.distance(x, y) > radius)
                    continue;
                var color = new java.awt.Color(image.getRGB(x, y), true);
                if (color.getAlpha() > 200 && color.getRed() > 200 && color.getGreen() > 140 && color.getBlue() < 120)
                    count++;
            }
        return count;
    }

    @Test
    void randomizeRadiusHasDraggableMinMaxCirclesThatCannotCross() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Dimension dimension = dimension();
                var initial = List.of(60f, 60f, 70f, 10f, 120f, 60f, 70f, 10f, 160f, 60f, 70f, 10f);
                dimension.setAttribute(HANDLES, new ArrayList<>(initial), true);
                TestView view = new TestView(dimension);
                PathTool tool = synchronousTool();
                tool.setView(view);
                tool.setActive(true);
                int button = InputEvent.BUTTON1_DOWN_MASK;
                try {
                    click(view, 60, 60);
                    click(view, 120, 60, InputEvent.CTRL_DOWN_MASK);
                    findComponent(tool.nodeMenu(), javax.swing.JMenuItem.class, "PathTool.Node.RandomizeRadius")
                            .doClick();
                    var circles = tool.operationCircles();
                    assertEquals(2, circles.size());
                    assertEquals("min", circles.get(0).label());
                    assertEquals(-1, circles.get(0).side(), "min handle on the left");
                    assertEquals("max", circles.get(1).label());
                    assertEquals(1, circles.get(1).side(), "max handle on the right");
                    // defaults around the current radius (10): 5 .. 15
                    assertEquals(5f, circles.get(0).value());
                    assertEquals(15f, circles.get(1).value());
                    for (int i = 0; i < 2; i++) {
                        float w = tool.operationPreview().path().get(i).w;
                        assertTrue(w >= 5 && w <= 15, "preview radius " + w);
                    }
                    assertEquals(10f, tool.operationPreview().path().get(2).w, "unselected node keeps its radius");
                    assertEquals(initial, dimension.getAttribute(HANDLES), "only previewed");

                    // handles: min 22 px left, max 22 px right of node 1 (radii below the offset)
                    assertEquals(new PathTool.Hit(PathTool.HitKind.OPERATION_RADIUS, 0, 0),
                            tool.hitTest(new Point(60 - 22, 60)));
                    assertEquals(new PathTool.Hit(PathTool.HitKind.OPERATION_RADIUS, 0, 1),
                            tool.hitTest(new Point(60 + 22, 60)));
                    // drag max outward by 10 blocks
                    event(view, MouseEvent.MOUSE_PRESSED, 82, 60, MouseEvent.BUTTON1, button);
                    event(view, MouseEvent.MOUSE_DRAGGED, 92, 60, MouseEvent.NOBUTTON, button);
                    event(view, MouseEvent.MOUSE_RELEASED, 92, 60, MouseEvent.BUTTON1, 0);
                    assertEquals(25f, circles.get(1).value());
                    assertNotNull(tool.operationPreview(), "operation still open");
                    // dragging min far outward stops at max: they cannot cross
                    event(view, MouseEvent.MOUSE_PRESSED, 38, 60, MouseEvent.BUTTON1, button);
                    event(view, MouseEvent.MOUSE_DRAGGED, 0, 60, MouseEvent.NOBUTTON, button);
                    event(view, MouseEvent.MOUSE_RELEASED, 0, 60, MouseEvent.BUTTON1, 0);
                    assertEquals(25f, circles.get(0).value());
                    assertEquals(25f, circles.get(1).value());

                    operationButton(view, "PathTool.OperationApply").doClick();
                    var handles = dimension.getAttribute(HANDLES);
                    assertEquals(25f, handles.get(3));
                    assertEquals(25f, handles.get(7));
                    assertEquals(10f, handles.get(11), "unselected");
                    assertEquals(initial.subList(0, 3), handles.subList(0, 3), "positions untouched");
                    assertTrue(tool.operationCircles().isEmpty(), "circles end with the operation");
                } finally {
                    tool.setActive(false);
                }
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    @Test
    void typingIntoTheViewportSetsTheOperationsMainNumber() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Dimension dimension = dimension();
                var initial = List.of(20f, 60f, 70f, 10f, 100f, 60f, 70f, 10f);
                dimension.setAttribute(HANDLES, new ArrayList<>(initial), true);
                TestView view = new TestView(dimension);
                PathTool tool = synchronousTool();
                tool.setView(view);
                tool.setActive(true);
                try {
                    click(view, 20, 60);
                    click(view, 100, 60, InputEvent.CTRL_DOWN_MASK);

                    // Subdivide: type 2, Enter applies
                    findComponent(tool.segmentMenu(), javax.swing.JMenuItem.class, "PathTool.Segment.Subdivide")
                            .doClick();
                    type(tool, view, "2");
                    assertEquals("Number of cuts: 2|", tool.operationTypingHeader());
                    assertEquals(2, findComponent(view, JSpinner.class, "PathTool.Subdivide.Cuts").getValue());
                    assertEquals(4, tool.operationPreview().path().size(), "preview follows the typed number");
                    assertTrue(type(tool, view, KeyEvent.VK_ENTER, '\n'));
                    assertEquals(16, dimension.getAttribute(HANDLES).size(), "two nodes inserted");
                    assertNull(tool.operationPreview());

                    // Randomize radius: min first, Tab to max, Escape restores, second Escape
                    // closes
                    findComponent(tool.nodeMenu(), javax.swing.JMenuItem.class, "PathTool.Node.RandomizeRadius")
                            .doClick();
                    var circles = tool.operationCircles();
                    type(tool, view, "3");
                    assertEquals("Min radius (blocks): 3|   (Tab: Max radius (blocks))", tool.operationTypingHeader());
                    assertEquals(3f, circles.get(0).value());
                    assertTrue(type(tool, view, KeyEvent.VK_TAB, '\t'));
                    float maxBefore = circles.get(1).value();
                    type(tool, view, "20");
                    assertEquals(20f, circles.get(1).value());
                    assertTrue(type(tool, view, KeyEvent.VK_ESCAPE, KeyEvent.CHAR_UNDEFINED));
                    assertEquals(maxBefore, circles.get(1).value(), "Escape restores the field");
                    assertNotNull(tool.operationPreview(), "operation still open");
                    assertTrue(type(tool, view, KeyEvent.VK_ESCAPE, KeyEvent.CHAR_UNDEFINED));
                    assertNull(tool.operationPreview(), "second Escape closes it");

                    // Snap to terrain: search radius circle with a handle, typed as whole blocks
                    findComponent(tool.nodeMenu(), javax.swing.JMenuItem.class, "PathTool.Node.Snap").doClick();
                    assertEquals("search", tool.operationCircles().get(0).label());
                    var search = findComponent(view, JSpinner.class, "PathTool.Snap.Radius");
                    assertEquals(new PathTool.Hit(PathTool.HitKind.OPERATION_RADIUS, 0, 0),
                            tool.hitTest(new Point(20 + 22, 60)), "8 blocks < min offset: handle 22 px right");
                    event(view, MouseEvent.MOUSE_PRESSED, 42, 60, MouseEvent.BUTTON1, InputEvent.BUTTON1_DOWN_MASK);
                    event(view, MouseEvent.MOUSE_DRAGGED, 47, 60, MouseEvent.NOBUTTON, InputEvent.BUTTON1_DOWN_MASK);
                    event(view, MouseEvent.MOUSE_RELEASED, 47, 60, MouseEvent.BUTTON1, 0);
                    assertEquals(13, search.getValue(), "whole blocks for an integer field");
                    type(tool, view, "12.6");
                    assertEquals(13, search.getValue(), "rounded to whole blocks");
                    assertTrue(type(tool, view, KeyEvent.VK_ESCAPE, KeyEvent.CHAR_UNDEFINED));
                    assertTrue(type(tool, view, KeyEvent.VK_ESCAPE, KeyEvent.CHAR_UNDEFINED));

                    // Randomize position: the radius is typeable too
                    findComponent(tool.nodeMenu(), javax.swing.JMenuItem.class, "PathTool.Node.Randomize").doClick();
                    type(tool, view, "7");
                    assertEquals(7.0, ((Number) tool.randomizeRadiusSpinner().getValue()).doubleValue());
                } finally {
                    tool.setActive(false);
                }
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    @Test
    void operationCirclesUseHandleStylingNotPreviewStyling() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Dimension dimension = dimension();
                dimension.setAttribute(HANDLES, new ArrayList<>(List.of(120f, 120f, 70f, 4f)), true);
                TestView view = new TestView(dimension);
                PathTool tool = synchronousTool();
                tool.setView(view);
                tool.setActive(true);
                try {
                    click(view, 120, 120);
                    // flat terrain: snapping moves nothing, so there is no previewed result at all
                    findComponent(tool.nodeMenu(), javax.swing.JMenuItem.class, "PathTool.Node.Snap").doClick();
                    findComponent(view, JSpinner.class, "PathTool.Snap.Radius").setValue(40);
                    var overlay = (MapToolOverlay) findComponent(view, JButton.class, "PathTool.EditorMode")
                            .getParent();
                    overlay.setSize(300, 300);
                    findComponent(view, JPanel.class, "PathTool.OperationPanel").setVisible(false);
                    var image = paintOverlay300(overlay);
                    int cyan = 0, amber = 0; // handle colour (lavender) vs preview amber
                    var handle = PathTool.SELECTED_NODE_FILL;
                    for (int x = 0; x < 300; x++)
                        for (int y = 60; y < 300; y++) {
                            var color = new java.awt.Color(image.getRGB(x, y), true);
                            if (color.getAlpha() < 200)
                                continue;
                            if (Math.abs(color.getRed() - handle.getRed()) < 20
                                    && Math.abs(color.getGreen() - handle.getGreen()) < 20
                                    && Math.abs(color.getBlue() - handle.getBlue()) < 20)
                                cyan++;
                            if (color.getRed() > 200 && color.getGreen() > 140 && color.getBlue() < 120)
                                amber++;
                        }
                    assertTrue(cyan > 100, "handle coloured search circle and handle (" + cyan + " px)");
                    assertEquals(0, amber, "no preview styling without a previewed result");
                } finally {
                    tool.setActive(false);
                }
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    @Test
    void undoRestoredPreviewBitsDoNotRemainAsResidue() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Dimension dimension = dimension();
                TestView view = new TestView(dimension);
                PathTool tool = synchronousTool();
                tool.setView(view);
                tool.setBrush(brush());
                tool.setActive(true);
                try {
                    tool.addNode(20, 20);
                    tool.addNode(60, 20);
                    assertTrue(dimension.getBitLayerValueAt(PreviewOperation.annotationLayer, 40, 20));
                    // what an undo restores: preview bits of an older path, unknown to the tool
                    dimension.setBitLayerValueAt(PreviewOperation.annotationLayer, 100, 100, true);

                    // without the undo notification the diff publish leaves them behind (the bug)
                    tool.addNode(60, 60);
                    assertTrue(dimension.getBitLayerValueAt(PreviewOperation.annotationLayer, 100, 100),
                            "residue survives a plain diff publish");

                    // undo / redo hook: writes nothing itself (keeps WorldPainter's redo intact)
                    // ...
                    tool.afterUndoOrRedo();
                    assertTrue(dimension.getBitLayerValueAt(PreviewOperation.annotationLayer, 100, 100));
                    // ... but the next preview update redraws the layer from scratch
                    tool.addNode(20, 60);
                    assertFalse(dimension.getBitLayerValueAt(PreviewOperation.annotationLayer, 100, 100),
                            "residue cleared");
                    assertTrue(dimension.getBitLayerValueAt(PreviewOperation.annotationLayer, 40, 20),
                            "current preview drawn");

                    // also when the next update is just leaving the tool
                    dimension.setBitLayerValueAt(PreviewOperation.annotationLayer, 100, 100, true);
                    tool.afterUndoOrRedo();
                } finally {
                    tool.setActive(false);
                }
                assertFalse(dimension.getBitLayerValueAt(PreviewOperation.annotationLayer, 100, 100));
                assertFalse(dimension.getBitLayerValueAt(PreviewOperation.annotationLayer, 40, 20));
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    @Test
    void viewportHeaderAlwaysShowsTheEditedValue() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Dimension dimension = dimension();
                dimension.setHeightAt(20, 60, 70f);
                dimension.setHeightAt(100, 60, 70f);
                dimension.setAttribute(HANDLES, new ArrayList<>(List.of(20f, 60f, 75f, 4f, 100f, 60f, 72f, 10f)), true);
                TestView view = new TestView(dimension);
                PathTool tool = synchronousTool();
                tool.setView(view);
                tool.setActive(true);
                try {
                    assertEquals("Radius: (select nodes)", tool.viewportHeader());
                    click(view, 20, 60);
                    assertEquals("Radius: 4", tool.viewportHeader());
                    click(view, 100, 60, InputEvent.CTRL_DOWN_MASK);
                    assertEquals("Radius: 4 - 10", tool.viewportHeader(), "range for differing values");
                    tool.setEditMode(PathTool.EditMode.HEIGHT);
                    assertEquals("Height above ground: +2 - +5", tool.viewportHeader());
                    // typing shows the typed text in the same place
                    type(tool, view, "3");
                    assertEquals("Height above ground: 3|", tool.viewportHeader());
                    assertTrue(type(tool, view, KeyEvent.VK_ENTER, KeyEvent.CHAR_UNDEFINED));
                    assertEquals("Height above ground: +3", tool.viewportHeader());
                    tool.setEditMode(PathTool.EditMode.CURVE);
                    assertEquals("Curve strength: 0.00", tool.viewportHeader());
                    // an open operation shows its values
                    findComponent(tool.nodeMenu(), javax.swing.JMenuItem.class, "PathTool.Node.RandomizeRadius")
                            .doClick();
                    assertEquals("Min radius (blocks): 3.5   Max radius (blocks): 10.5", tool.viewportHeader());
                    findComponent(tool.segmentMenu(), javax.swing.JMenuItem.class, "PathTool.Segment.Subdivide")
                            .doClick();
                    assertEquals("Number of cuts: 1", tool.viewportHeader());
                } finally {
                    tool.setActive(false);
                }
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    @Test
    void onlyDownhillNodeToolPreviewsAndApplies() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Dimension dimension = dimension();
                var initial = List.of(20f, 60f, 80f, 4f, 60f, 60f, 90f, 4f, 100f, 60f, 85f, 4f);
                dimension.setAttribute(HANDLES, new ArrayList<>(initial), true);
                for (int x : new int[]{20, 60, 100})
                    dimension.setHeightAt(x, 60, 200f); // high ground: only the downhill rule applies
                TestView view = new TestView(dimension);
                PathTool tool = synchronousTool();
                tool.setView(view);
                tool.setActive(true);
                try {
                    tool.dispatchKeyEvent(
                            new KeyEvent(view, KeyEvent.KEY_PRESSED, 0, InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_A, 'a'));
                    event(view, MouseEvent.MOUSE_MOVED, 150, 150, MouseEvent.NOBUTTON, 0);
                    tool.dispatchKeyEvent(
                            new KeyEvent(view, KeyEvent.KEY_PRESSED, 0, InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_A, 'a'));
                    assertEquals(Set.of(0, 1, 2), tool.selectedNodes());
                    findComponent(tool.nodeMenu(), javax.swing.JMenuItem.class, "PathTool.Node.OnlyDownhill").doClick();
                    assertEquals("Only downhill: 2 nodes change", tool.viewportHeader());
                    assertEquals(initial, dimension.getAttribute(HANDLES), "only previewed");
                    operationButton(view, "PathTool.OperationApply").doClick();
                    assertEquals(List.of(20f, 60f, 80f, 4f, 60f, 60f, 80f, 4f, 100f, 60f, 80f, 4f),
                            dimension.getAttribute(HANDLES));
                } finally {
                    tool.setActive(false);
                }
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    @Test
    void unravelledPathStripShowsTheEditorsViewIncludingTerrainAndWater() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Dimension dimension = dimension();
                for (int x = 40; x <= 80; x++)
                    dimension.setWaterLevelAt(x, 100, Math.round(dimension.getHeightAt(x, 100)) + 6);
                dimension.setAttribute(HANDLES, new ArrayList<>(List.of(20f, 100f, 72f, 4f, 120f, 100f, 72f, 8f)),
                        true);
                TestView view = new TestView(dimension);
                PathTool tool = synchronousTool();
                tool.setView(view);
                tool.setActive(true);
                try {
                    var overlay = (MapToolOverlay) findComponent(view, JButton.class, "PathTool.EditorMode")
                            .getParent();
                    overlay.setSize(300, 300);
                    paintOverlay300(overlay);
                    var strip = tool.stripBounds(); // grows in the height view
                    // radius view: strip drawn, no water
                    var radius = paintOverlay300(overlay);
                    assertNotEquals(0, radius.getRGB(strip.x + 4, strip.y + 4), "strip background");
                    assertEquals(0, colourPixels(radius, strip, PathTool.PROFILE_WATER), "no water in the radius view");
                    // height view: terrain and the water above it, read from the dimension
                    tool.setEditMode(PathTool.EditMode.HEIGHT);
                    var height = paintOverlay300(overlay);
                    assertTrue(colourPixels(height, strip, PathTool.PROFILE_WATER) > 20, "water drawn");
                    assertTrue(colourPixels(height, strip, PathTool.PROFILE_TERRAIN) > 100, "terrain drawn");
                    // toggled off with its overlay part
                    overlayPart(tool, "PathTool.ShowProfile").doClick();
                    assertEquals(0, paintOverlay300(overlay).getRGB(strip.x + 4, strip.y + 4));
                } finally {
                    tool.setActive(false);
                }
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    /** Pixels close to an (alpha-blended over nothing) overlay colour. */
    private static int colourPixels(BufferedImage image, java.awt.Rectangle area, java.awt.Color colour) {
        int count = 0;
        for (int x = area.x; x < area.x + area.width; x++)
            for (int y = area.y; y < area.y + area.height; y++) {
                var c = new java.awt.Color(image.getRGB(x, y), true);
                if (Math.abs(c.getRed() - colour.getRed()) < 25 && Math.abs(c.getGreen() - colour.getGreen()) < 25
                        && Math.abs(c.getBlue() - colour.getBlue()) < 25 && c.getAlpha() > 150)
                    count++;
            }
        return count;
    }

    @Test
    void operationsWarnAboutAndMergeNodesClumpedOnOneBlock() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Dimension dimension = dimension();
                float ground = dimension.getHeightAt(60, 60);
                dimension.setHeightAt(64, 62, ground - 20); // one pit both nodes snap into
                dimension.setAttribute(HANDLES, new ArrayList<>(List.of(20f, 60f, ground, 4f, 60f, 60f, ground, 4f, 66f,
                        60f, ground, 8f, 110f, 60f, ground, 4f)), true);
                TestView view = new TestView(dimension);
                PathTool tool = synchronousTool();
                tool.setView(view);
                tool.setActive(true);
                try {
                    click(view, 60, 60);
                    click(view, 66, 60, InputEvent.CTRL_DOWN_MASK);
                    findComponent(tool.nodeMenu(), javax.swing.JMenuItem.class, "PathTool.Node.Snap").doClick();
                    assertEquals("Merges nodes on the same block: 2+3", tool.operationMergeNote());
                    assertEquals(8, dimension.getAttribute(HANDLES).size() / 2, "preview only, nothing merged yet");
                    // the warning ring is drawn in the warning colour
                    var ring = new BufferedImage(60, 60, BufferedImage.TYPE_INT_ARGB);
                    PathTool.paintMergeWarning(ring.createGraphics(), new Point(30, 30));
                    assertEquals(PathTool.WARNING_COLOR.getRGB(), ring.getRGB(30 + (20 + 12) / 2, 30));
                    operationButton(view, "PathTool.OperationApply").doClick();
                    var handles = dimension.getAttribute(HANDLES);
                    assertEquals(12, handles.size(), "two nodes merged into one");
                    assertEquals(List.of(64f, 62f), handles.subList(4, 6));
                    assertEquals(6f, handles.get(7), 1e-4, "radius averaged (4, 8)");
                    assertEquals(Set.of(1), tool.selectedNodes());
                    assertNull(tool.operationMergeNote());
                } finally {
                    tool.setActive(false);
                }
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    @Test
    void heightViewDrawsResolvedHeightsTintedBySlope() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Dimension dimension = dimension();
                TestView view = new TestView(dimension);
                PathTool tool = synchronousTool();
                tool.setView(view);
                tool.setActive(true);
                try {
                    var overlay = (MapToolOverlay) findComponent(view, JButton.class, "PathTool.EditorMode")
                            .getParent();
                    overlay.setSize(300, 300);
                    tool.setEditMode(PathTool.EditMode.HEIGHT);
                    paintOverlay300(overlay);
                    var strip = tool.stripBounds(); // grows in the height view
                    // falling path: green only
                    dimension.setAttribute(HANDLES,
                            new ArrayList<>(List.of(20f, 60f, 90f, 4f, 60f, 60f, 80f, 4f, 100f, 60f, 70f, 4f)), true);
                    var falling = paintOverlay300(overlay);
                    assertTrue(colourPixels(falling, strip, PathTool.SLOPE_DOWN_COLOR) > 20, "green");
                    assertEquals(0, colourPixels(falling, strip, PathTool.SLOPE_UP_COLOR), "no red");
                    // rising path: red
                    dimension.setAttribute(HANDLES,
                            new ArrayList<>(List.of(20f, 60f, 70f, 4f, 60f, 60f, 80f, 4f, 100f, 60f, 90f, 4f)), true);
                    assertTrue(colourPixels(paintOverlay300(overlay), strip, PathTool.SLOPE_UP_COLOR) > 20, "red");
                    // the resolved heights are what Apply generates: with snap to terrain the
                    // rising nodes are overridden by the (flat) ground -> no rise left
                    var resolved = new ArrayList<javax.vecmath.Point4f>(
                            PathToolBackend.getPathFromNodes(List.of(new javax.vecmath.Point4f(20, 60, 70, 4),
                                    new javax.vecmath.Point4f(100, 60, 90, 4)), 0f).path);
                    for (var component : tool.pathSettingsPanel().getComponents())
                        if (component instanceof javax.swing.JCheckBox box && "snap to terrain".equals(box.getText()))
                            box.doClick();
                    tool.resolveHeights(resolved, dimension);
                    for (var sample : resolved)
                        assertEquals(dimension.getHeightAt(Math.round(sample.x), Math.round(sample.y)), sample.z);
                    assertEquals(0, colourPixels(paintOverlay300(overlay), strip, PathTool.SLOPE_UP_COLOR),
                            "flat resolved path is not red");
                } finally {
                    tool.setActive(false);
                }
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    @Test
    void onlyDownhillNeverMergesNodesThatAlreadySharedABlock() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Dimension dimension = dimension();
                var initial = List.of(20f, 60f, 90f, 4f, 60f, 60f, 95f, 4f, 60f, 60f, 85f, 4f, 100f, 60f, 99f, 4f);
                dimension.setAttribute(HANDLES, new ArrayList<>(initial), true);
                for (int x : new int[]{20, 60, 100})
                    dimension.setHeightAt(x, 60, 200f); // high ground: only the downhill rule applies
                TestView view = new TestView(dimension);
                PathTool tool = synchronousTool();
                tool.setView(view);
                tool.setActive(true);
                try {
                    event(view, MouseEvent.MOUSE_MOVED, 150, 150, MouseEvent.NOBUTTON, 0);
                    tool.dispatchKeyEvent(
                            new KeyEvent(view, KeyEvent.KEY_PRESSED, 0, InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_A, 'a'));
                    findComponent(tool.nodeMenu(), javax.swing.JMenuItem.class, "PathTool.Node.OnlyDownhill").doClick();
                    assertNull(tool.operationMergeNote(), "height-only: no merge warning");
                    operationButton(view, "PathTool.OperationApply").doClick();
                    var handles = dimension.getAttribute(HANDLES);
                    assertEquals(16, handles.size(), "all four nodes kept");
                    assertEquals(List.of(90f, 90f, 85f, 85f),
                            List.of(handles.get(2), handles.get(6), handles.get(10), handles.get(14)),
                            "only heights changed");
                } finally {
                    tool.setActive(false);
                }
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    @Test
    void stripZoomsByDraggingAndWheelAndResets() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Dimension dimension = dimension();
                dimension.setAttribute(HANDLES, new ArrayList<>(List.of(20f, 60f, 72f, 4f, 120f, 60f, 72f, 4f)), true);
                TestView view = new TestView(dimension);
                PathTool tool = synchronousTool();
                tool.setView(view);
                tool.setBrush(brush());
                tool.setActive(true);
                try {
                    var overlay = (MapToolOverlay) findComponent(view, JButton.class, "PathTool.EditorMode")
                            .getParent();
                    overlay.setSize(300, 300);
                    view.setSize(300, 300);
                    paintOverlay300(overlay); // establishes the strip mapping
                    int stripTop = tool.stripBounds().y;
                    // left half dragged as a rectangle -> zoomed to roughly that half
                    int left = 8 + 24, right = 300 - 8 - 24, middle = (left + right) / 2;
                    overlay.dispatchEvent(new MouseEvent(overlay, MouseEvent.MOUSE_PRESSED, 0,
                            InputEvent.BUTTON1_DOWN_MASK, left, stripTop + 30, 1, false, MouseEvent.BUTTON1));
                    overlay.dispatchEvent(new MouseEvent(overlay, MouseEvent.MOUSE_DRAGGED, 0,
                            InputEvent.BUTTON1_DOWN_MASK, middle, stripTop + 90, 1, false, MouseEvent.NOBUTTON));
                    overlay.dispatchEvent(new MouseEvent(overlay, MouseEvent.MOUSE_RELEASED, 0, 0, middle,
                            stripTop + 90, 1, false, MouseEvent.BUTTON1));
                    double[] zoom = tool.stripZoom();
                    assertEquals(0, zoom[0], 0.01);
                    assertEquals(0.5, zoom[1], 0.02);
                    var reset = findComponent(view, JButton.class, "PathTool.ProfileResetZoom");
                    assertTrue(reset.isEnabled());
                    // wheel in at the right edge of the visible part, then out again
                    paintOverlay300(overlay);
                    overlay.dispatchEvent(new java.awt.event.MouseWheelEvent(overlay, MouseEvent.MOUSE_WHEEL, 0, 0,
                            right, stripTop + 60, 0, false, java.awt.event.MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, -1));
                    zoom = tool.stripZoom();
                    assertEquals(0.5, zoom[1], 0.02, "zooms around the mouse: its position stays put");
                    assertEquals(0.4, zoom[1] - zoom[0], 0.02);
                    for (int notch = 0; notch < 20; notch++)
                        overlay.dispatchEvent(new java.awt.event.MouseWheelEvent(overlay, MouseEvent.MOUSE_WHEEL, 0, 0,
                                right, stripTop + 60, 0, false, java.awt.event.MouseWheelEvent.WHEEL_UNIT_SCROLL, 1,
                                1));
                    zoom = tool.stripZoom();
                    assertEquals(0, zoom[0], 1e-9, "zooming out stops at the whole path");
                    assertEquals(1, zoom[1], 1e-9);
                    // reset
                    overlay.dispatchEvent(new java.awt.event.MouseWheelEvent(overlay, MouseEvent.MOUSE_WHEEL, 0, 0,
                            middle, stripTop + 60, 0, false, java.awt.event.MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, -1));
                    reset.doClick();
                    assertEquals(0, tool.stripZoom()[0]);
                    assertEquals(1, tool.stripZoom()[1]);
                    assertFalse(reset.isEnabled());
                    // the strip takes its clicks: even in add mode nothing is added below it
                    tool.addModeToggle().doClick();
                    assertTrue(overlay.contains(middle, stripTop + 60));
                    int nodes = dimension.getAttribute(HANDLES).size();
                    SwingUtilities.getDeepestComponentAt(view, middle, stripTop + 60);
                    assertSame(overlay, javax.swing.SwingUtilities.getDeepestComponentAt(view, middle, stripTop + 60),
                            "the strip (overlay) is the mouse target, not the map");
                    assertEquals(nodes, dimension.getAttribute(HANDLES).size());
                } finally {
                    tool.setActive(false);
                }
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    private static void dragOnStrip(MapToolOverlay overlay, int x0, int y0, int x1, int y1) {
        overlay.dispatchEvent(new MouseEvent(overlay, MouseEvent.MOUSE_PRESSED, 0, InputEvent.BUTTON1_DOWN_MASK, x0, y0,
                1, false, MouseEvent.BUTTON1));
        overlay.dispatchEvent(new MouseEvent(overlay, MouseEvent.MOUSE_DRAGGED, 0, InputEvent.BUTTON1_DOWN_MASK, x1, y1,
                1, false, MouseEvent.NOBUTTON));
        overlay.dispatchEvent(
                new MouseEvent(overlay, MouseEvent.MOUSE_RELEASED, 0, 0, x1, y1, 1, false, MouseEvent.BUTTON1));
    }

    @Test
    void heightViewBlocksAreSquareAtEveryZoom() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Dimension dimension = dimension();
                dimension.setAttribute(HANDLES, new ArrayList<>(List.of(20f, 60f, 60f, 4f, 120f, 60f, 90f, 4f)), true);
                TestView view = new TestView(dimension);
                PathTool tool = synchronousTool();
                tool.setView(view);
                tool.setActive(true);
                try {
                    var overlay = (MapToolOverlay) findComponent(view, JButton.class, "PathTool.EditorMode")
                            .getParent();
                    overlay.setSize(300, 300);
                    tool.setEditMode(PathTool.EditMode.HEIGHT);
                    paintOverlay300(overlay);
                    int top = tool.stripBounds().y;
                    paintOverlay300(overlay);
                    for (int step = 0; step < 4; step++) {
                        var perBlock = tool.stripPixelsPerBlock();
                        assertEquals(perBlock[0], perBlock[1], 1e-6, "square at step " + step);
                        switch (step) {
                            case 0 -> overlay.dispatchEvent(new java.awt.event.MouseWheelEvent(overlay,
                                    MouseEvent.MOUSE_WHEEL, 0, 0, 120, top + 60, 0, false,
                                    java.awt.event.MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, -1));
                            case 1 -> dragOnStrip(overlay, 60, top + 40, 200, top + 45);
                            case 2 -> dragOnStrip(overlay, 150, top + 30, 152, top + 90);
                            default -> {
                            }
                        }
                        paintOverlay300(overlay);
                    }
                } finally {
                    tool.setActive(false);
                }
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    @Test
    void axisDragsAreLenientAndMiddleMouseDragPansTheStrip() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Dimension dimension = dimension();
                dimension.setAttribute(HANDLES, new ArrayList<>(List.of(20f, 60f, 60f, 4f, 120f, 60f, 90f, 4f)), true);
                TestView view = new TestView(dimension);
                PathTool tool = synchronousTool();
                tool.setView(view);
                tool.setActive(true);
                try {
                    var overlay = (MapToolOverlay) findComponent(view, JButton.class, "PathTool.EditorMode")
                            .getParent();
                    overlay.setSize(300, 300);
                    tool.setEditMode(PathTool.EditMode.HEIGHT);
                    paintOverlay300(overlay);
                    int top = tool.stripBounds().y;
                    var reset = findComponent(view, JButton.class, "PathTool.ProfileResetZoom");
                    // a 30 degree drag still counts as horizontal: no height centre set
                    paintOverlay300(overlay);
                    dragOnStrip(overlay, 60, top + 40, 160, top + 70);
                    assertTrue(tool.stripZoom()[1] - tool.stripZoom()[0] < 1);
                    assertNull(tool.stripZoomCentre(), "horizontal: height position unchanged");
                    reset.doClick();
                    // steep drag: vertical
                    paintOverlay300(overlay);
                    dragOnStrip(overlay, 150, top + 25, 175, top + 95);
                    assertNotNull(tool.stripZoomCentre(), "vertical");
                    reset.doClick();

                    // middle mouse drag pans: along the path and in height
                    paintOverlay300(overlay);
                    overlay.dispatchEvent(new java.awt.event.MouseWheelEvent(overlay, MouseEvent.MOUSE_WHEEL, 0, 0, 150,
                            top + 60, 0, false, java.awt.event.MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, -1));
                    overlay.dispatchEvent(new java.awt.event.MouseWheelEvent(overlay, MouseEvent.MOUSE_WHEEL, 0, 0, 150,
                            top + 60, 0, false, java.awt.event.MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, -1));
                    paintOverlay300(overlay);
                    double from = tool.stripZoom()[0];
                    double centre = tool.stripZoomCentre();
                    overlay.dispatchEvent(new MouseEvent(overlay, MouseEvent.MOUSE_PRESSED, 0,
                            InputEvent.BUTTON2_DOWN_MASK, 150, top + 60, 1, false, MouseEvent.BUTTON2));
                    overlay.dispatchEvent(new MouseEvent(overlay, MouseEvent.MOUSE_DRAGGED, 0,
                            InputEvent.BUTTON2_DOWN_MASK, 120, top + 80, 1, false, MouseEvent.NOBUTTON));
                    overlay.dispatchEvent(new MouseEvent(overlay, MouseEvent.MOUSE_RELEASED, 0, 0, 120, top + 80, 1,
                            false, MouseEvent.BUTTON2));
                    assertTrue(tool.stripZoom()[0] > from, "dragging left shows more of the path's end");
                    assertTrue(tool.stripZoomCentre() > centre, "dragging down shows higher ground");
                    // panning never leaves the path
                    overlay.dispatchEvent(new MouseEvent(overlay, MouseEvent.MOUSE_PRESSED, 0,
                            InputEvent.BUTTON2_DOWN_MASK, 150, top + 60, 1, false, MouseEvent.BUTTON2));
                    overlay.dispatchEvent(new MouseEvent(overlay, MouseEvent.MOUSE_DRAGGED, 0,
                            InputEvent.BUTTON2_DOWN_MASK, -3000, top + 60, 1, false, MouseEvent.NOBUTTON));
                    overlay.dispatchEvent(new MouseEvent(overlay, MouseEvent.MOUSE_RELEASED, 0, 0, -3000, top + 60, 1,
                            false, MouseEvent.BUTTON2));
                    assertEquals(1, tool.stripZoom()[1], 1e-9);
                } finally {
                    tool.setActive(false);
                }
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    @Test
    void clickingANodeInTheStripSelectsJustThatNode() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Dimension dimension = dimension();
                dimension.setAttribute(HANDLES,
                        new ArrayList<>(List.of(20f, 60f, 70f, 4f, 60f, 60f, 70f, 4f, 100f, 60f, 70f, 4f)), true);
                TestView view = new TestView(dimension);
                PathTool tool = synchronousTool();
                tool.setView(view);
                tool.setActive(true);
                try {
                    var overlay = (MapToolOverlay) findComponent(view, JButton.class, "PathTool.EditorMode")
                            .getParent();
                    overlay.setSize(300, 300);
                    click(view, 20, 60);
                    click(view, 100, 60, InputEvent.CTRL_DOWN_MASK);
                    assertEquals(Set.of(0, 2), tool.selectedNodes());
                    paintOverlay300(overlay);
                    Point node2 = tool.stripNodePoint(1);
                    // plain click on node 2 in the strip: only node 2 selected
                    dragOnStrip(overlay, node2.x + 2, node2.y + 1, node2.x + 2, node2.y + 1);
                    assertEquals(Set.of(1), tool.selectedNodes());
                    // a Ctrl+click is still just a click there: no adding
                    Point node3 = tool.stripNodePoint(2);
                    overlay.dispatchEvent(new MouseEvent(overlay, MouseEvent.MOUSE_PRESSED, 0,
                            InputEvent.BUTTON1_DOWN_MASK | InputEvent.CTRL_DOWN_MASK, node3.x, node3.y, 1, false,
                            MouseEvent.BUTTON1));
                    overlay.dispatchEvent(new MouseEvent(overlay, MouseEvent.MOUSE_RELEASED, 0,
                            InputEvent.CTRL_DOWN_MASK, node3.x, node3.y, 1, false, MouseEvent.BUTTON1));
                    assertEquals(Set.of(2), tool.selectedNodes());
                    // clicking empty strip space leaves the selection alone
                    dragOnStrip(overlay, (node2.x + node3.x) / 2, 300 - 20, (node2.x + node3.x) / 2, 300 - 20);
                    assertEquals(Set.of(2), tool.selectedNodes());
                    assertEquals(12, dimension.getAttribute(HANDLES).size(), "no node added or moved");
                } finally {
                    tool.setActive(false);
                }
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    @Test
    void heightViewStripGrowsToFitTheHeightAxisUpToHalfTheViewport() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Dimension dimension = dimension();
                // a steep path: 60 blocks of height over 80 blocks of length
                dimension.setAttribute(HANDLES, new ArrayList<>(List.of(20f, 60f, 40f, 4f, 100f, 60f, 100f, 4f)), true);
                TestView view = new TestView(dimension);
                PathTool tool = synchronousTool();
                tool.setView(view);
                tool.setActive(true);
                try {
                    var overlay = (MapToolOverlay) findComponent(view, JButton.class, "PathTool.EditorMode")
                            .getParent();
                    overlay.setSize(300, 400);
                    var image = new BufferedImage(300, 400, BufferedImage.TYPE_INT_ARGB);
                    overlay.paint(image.createGraphics());
                    assertEquals(PathTool.UNRAVELLED_HEIGHT, tool.stripBounds().height, "radius view: normal height");
                    tool.setEditMode(PathTool.EditMode.HEIGHT);
                    overlay.paint(image.createGraphics());
                    int grown = tool.stripBounds().height;
                    assertTrue(grown > PathTool.UNRAVELLED_HEIGHT, "grows to fit the height axis: " + grown);
                    assertTrue(grown <= 200, "at most half the viewport");
                    assertEquals(400 - 8, tool.stripBounds().y + grown, "still anchored at the bottom");
                    var perBlock = tool.stripPixelsPerBlock();
                    assertEquals(perBlock[0], perBlock[1], 1e-6, "blocks stay square");
                } finally {
                    tool.setActive(false);
                }
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    @Test
    void stripZoomDragsLikePlotlyHorizontalVerticalOrBox() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Dimension dimension = dimension();
                dimension.setAttribute(HANDLES, new ArrayList<>(List.of(20f, 60f, 60f, 4f, 120f, 60f, 90f, 4f)), true);
                TestView view = new TestView(dimension);
                PathTool tool = synchronousTool();
                tool.setView(view);
                tool.setActive(true);
                try {
                    var overlay = (MapToolOverlay) findComponent(view, JButton.class, "PathTool.EditorMode")
                            .getParent();
                    overlay.setSize(300, 300);
                    tool.setEditMode(PathTool.EditMode.HEIGHT);
                    paintOverlay300(overlay);
                    int top = tool.stripBounds().y, left = 32, right = 268, middle = 150;
                    var reset = findComponent(view, JButton.class, "PathTool.ProfileResetZoom");

                    // horizontal drag (moves < 8 px vertically): distance only
                    dragOnStrip(overlay, left, top + 50, middle, top + 55);
                    assertEquals(0.5, tool.stripZoom()[1], 0.02);
                    assertNull(tool.stripZoomCentre(), "height untouched");
                    reset.doClick();

                    // vertical drag: the dragged heights fill the strip; blocks stay square, so the
                    // distance range narrows with it
                    paintOverlay300(overlay);
                    var before = tool.stripPixelsPerBlock();
                    dragOnStrip(overlay, middle, top + 30, middle + 4, top + 80);
                    assertTrue(tool.stripZoom()[1] - tool.stripZoom()[0] < 1, "zoomed in");
                    assertNotNull(tool.stripZoomCentre());
                    paintOverlay300(overlay);
                    assertTrue(tool.stripPixelsPerBlock()[1] > before[1]);
                    reset.doClick();

                    // diagonal: box; blocks stay square, so it zooms just enough for the whole
                    // box to fit, centred on it
                    paintOverlay300(overlay);
                    dragOnStrip(overlay, left, top + 30, middle, top + 80);
                    assertTrue(tool.stripZoom()[0] <= 0.001 && tool.stripZoom()[1] >= 0.49, "box fully visible");
                    assertTrue(tool.stripZoom()[1] - tool.stripZoom()[0] < 1, "zoomed in");
                    assertNotNull(tool.stripZoomCentre());
                    reset.doClick();

                    // tiny drag: a click, no zoom
                    paintOverlay300(overlay);
                    dragOnStrip(overlay, middle, top + 50, middle + 5, top + 54);
                    assertEquals(1, tool.stripZoom()[1]);
                    assertNull(tool.stripZoomCentre());
                } finally {
                    tool.setActive(false);
                }
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    @Test
    void snapToTerrainMovesNodesToTheLowestSpotAndKeepsHeightAboveGround() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Dimension dimension = dimension();
                float ground = dimension.getHeightAt(60, 60);
                dimension.setHeightAt(63, 64, ground - 20); // a pit 5 blocks away
                dimension.setAttribute(HANDLES, new ArrayList<>(List.of(60f, 60f, ground + 2, 4f)), true);
                TestView view = new TestView(dimension);
                PathTool tool = synchronousTool();
                tool.setView(view);
                tool.setActive(true);
                try {
                    click(view, 60, 60);
                    findComponent(tool.nodeMenu(), javax.swing.JMenuItem.class, "PathTool.Node.Snap").doClick();
                    assertEquals(PathOperations.SnapTarget.LOWEST,
                            findComponent(view, javax.swing.JComboBox.class, "PathTool.Snap.Target").getSelectedItem());
                    operationButton(view, "PathTool.OperationApply").doClick();
                    assertEquals(List.of(63f, 64f, ground - 20 + 2, 4f), dimension.getAttribute(HANDLES),
                            "moved into the pit, still 2 blocks above ground");
                } finally {
                    tool.setActive(false);
                }
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    @Test
    void insertedNodesAreDrawnInTheRandomizeStyleNotAsNormalNodes() {
        var inserted = new BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB);
        var normal = new BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB);
        PathTool.paintInsertedNode(inserted.createGraphics(), new Point(20, 20));
        PathTool.paintNode(normal.createGraphics(), new Point(20, 20), true);
        assertEquals(0, inserted.getRGB(20, 20), "hollow: no filled marker");
        assertNotEquals(0, normal.getRGB(20, 20));
        int amber = 0;
        for (int x = 0; x < 40; x++)
            for (int y = 0; y < 40; y++)
                if (inserted.getRGB(x, y) == PathTool.PREVIEW_COLOR.getRGB())
                    amber++;
        assertTrue(amber > 10, "dashed amber ring");
    }

    @Test
    void transformEditorRotatesScalesAndMovesAroundADraggablePivot() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Dimension dimension = dimension();
                dimension.setAttribute(HANDLES,
                        new ArrayList<>(List.of(40f, 60f, 72f, 4f, 60f, 30f, 70f, 4f, 80f, 60f, 70f, 4f)), true);
                TestView view = new TestView(dimension);
                PathTool tool = synchronousTool();
                tool.setView(view);
                tool.setActive(true);
                try {
                    click(view, 40, 60);
                    click(view, 80, 60, InputEvent.CTRL_DOWN_MASK);
                    tool.setEditMode(PathTool.EditMode.TRANSFORM);
                    assertEquals("Transform: 2 nodes", tool.viewportHeader());
                    var overlay = (MapToolOverlay) java.util.Arrays.stream(view.getComponents())
                            .filter(MapToolOverlay.class::isInstance)
                            .findFirst()
                            .orElseThrow();
                    overlay.setSize(200, 200);
                    paintOverlay(overlay); // box and handles paint

                    // dragging a node does nothing here: only the move handle moves (with the
                    // pivot)
                    drag(tool, view.worldToView(40, 60), view.worldToView(50, 70));
                    assertNodes(dimension, 40, 60, 72, 60, 30, 70, 80, 60, 70);
                    assertEquals(Set.of(0, 2), tool.selectedNodes());
                    assertEquals(PathTool.EditMode.TRANSFORM, tool.editMode());
                    var nodeHit = tool.hitTest(view.worldToView(40, 60));
                    assertEquals(java.awt.Cursor.HAND_CURSOR, tool.hoverCursor(nodeHit), "no move cursor on nodes");
                    tool.setEditMode(PathTool.EditMode.RADIUS);
                    assertEquals(java.awt.Cursor.MOVE_CURSOR, tool.hoverCursor(nodeHit), "nodes drag elsewhere");
                    tool.setEditMode(PathTool.EditMode.TRANSFORM);

                    // rotate: the handle above the box, dragged to the right of the pivot (60, 60)
                    var rotate = viewHandle(tool, view, "rotate");
                    assertEquals(PathTool.HitKind.TRANSFORM_ROTATE, tool.hitTest(rotate).kind());
                    drag(tool, rotate, view.worldToView(104, 60));
                    assertNodes(dimension, 60, 40, 72, 60, 30, 70, 60, 80, 70);
                    assertEquals(-90, tool.transformBoxAngle(), 1e-9, "the box turned with the nodes");

                    // pivot: dragged onto the second selected node
                    Point pivot = view.worldToView(60, 60);
                    assertEquals(PathTool.HitKind.TRANSFORM_PIVOT, tool.hitTest(pivot).kind());
                    drag(tool, pivot, view.worldToView(60, 80));
                    assertEquals(new java.awt.geom.Point2D.Double(60, 80), tool.transformPivotWorld());
                    // double click: back to the box centre; drag it out again for the rest
                    Point placed = view.worldToView(60, 80);
                    tool.handleSelectionPress(placed);
                    tool.handleSelectionRelease(placed);
                    assertEquals(new java.awt.geom.Point2D.Double(60, 80), tool.transformPivotWorld(), "one click");
                    tool.handleSelectionPress(placed);
                    tool.handleSelectionRelease(placed);
                    assertEquals(new java.awt.geom.Point2D.Double(60, 60), tool.transformPivotWorld(), "double click");
                    drag(tool, view.worldToView(60, 60), placed);
                    assertEquals(new java.awt.geom.Point2D.Double(60, 80), tool.transformPivotWorld());

                    // scale: the corner arrow halfway towards the pivot
                    var corner = viewHandle(tool, view, "scale");
                    assertEquals(PathTool.HitKind.TRANSFORM_SCALE, tool.hitTest(corner).kind());
                    Point pivotView = view.worldToView(60, 80);
                    // mid drag the box fits the scaled nodes as tightly as at rest (the padding
                    // does not scale with them)
                    Point half = new Point((corner.x + pivotView.x) / 2, (corner.y + pivotView.y) / 2);
                    tool.handleSelectionPress(corner);
                    tool.handleSelectionDrag(half);
                    var during = tool.shownTransformBox(tool.movedPathForTest()).box();
                    tool.handleSelectionRelease(half);
                    var after = tool.shownTransformBox(tool.getCurrentPath()).box();
                    assertEquals(after.getWidth(), during.getWidth(), 1e-6, "no jump on release");
                    assertEquals(after.getHeight(), during.getHeight(), 1e-6);
                    assertNodes(dimension, 60, 60, 72, 60, 30, 70, 60, 80, 70);
                    assertEquals(-90, tool.transformBoxAngle(), 1e-9, "scaling keeps the box angle");

                    // move: the handle left of the box; the placed pivot moves along
                    var move = viewHandle(tool, view, "move");
                    assertEquals(PathTool.HitKind.TRANSFORM_MOVE, tool.hitTest(move).kind());
                    // the pivot travels with the selection while dragging, not only on release
                    tool.handleSelectionPress(move);
                    tool.handleSelectionDrag(new Point(move.x + 10, move.y));
                    assertEquals(new java.awt.geom.Point2D.Double(70, 80), tool.displayedPivot());
                    tool.handleSelectionRelease(new Point(move.x + 10, move.y));
                    assertNodes(dimension, 70, 60, 72, 60, 30, 70, 70, 80, 70);
                    assertEquals(new java.awt.geom.Point2D.Double(70, 80), tool.transformPivotWorld());
                    assertEquals(-90, tool.transformBoxAngle(), 1e-9, "moving keeps the box angle");

                    // typing a number rotates by that many degrees around the pivot
                    type(tool, view, "180");
                    assertEquals("Rotate: 180|", tool.viewportHeader());
                    assertTrue(type(tool, view, KeyEvent.VK_ENTER, KeyEvent.CHAR_UNDEFINED));
                    assertNodes(dimension, 70, 100, 72, 60, 30, 70, 70, 80, 70);
                    assertEquals(90, tool.transformBoxAngle(), 1e-9);

                    // a new selection gets its own centre as the pivot again
                    click(view, 60, 30);
                    assertEquals(new java.awt.geom.Point2D.Double(60, 30), tool.transformPivotWorld());
                    assertEquals(0, tool.transformBoxAngle(), "a new selection gets an axis-aligned box");

                    // a single node only gets the move handle, just below it
                    Point single = view.worldToView(60, 30);
                    var box = PathTransformBox.box(List.of(new java.awt.geom.Point2D.Double(single.x, single.y)),
                            Set.of(0), 0);
                    for (var other : List.of(PathTransformBox.rotateHandle(box), PathTransformBox.corner(box, 2),
                            PathTransformBox.moveHandle(box))) {
                        var hit = tool.hitTest(toPoint(other));
                        assertTrue(hit == null || hit.kind() == PathTool.HitKind.NODE, "only the move handle: " + hit);
                    }
                    var singleMove = viewHandle(tool, view, "move");
                    assertEquals(single.y + PathTransformBox.SINGLE_MOVE_OFFSET, singleMove.y, 1);
                    assertEquals(PathTool.HitKind.TRANSFORM_MOVE, tool.hitTest(singleMove).kind());
                    drag(tool, singleMove, new Point(singleMove.x + 5, singleMove.y));
                    assertNodes(dimension, 70, 100, 72, 65, 30, 70, 70, 80, 70);
                    // no typed rotation for a single node
                    assertFalse(type(tool, view, KeyEvent.VK_5, '5'));
                    assertEquals("Transform: 1 node", tool.viewportHeader());
                    paintOverlay(overlay);
                } finally {
                    tool.setActive(false);
                }
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    @Test
    void transformBoxPivotAndNodesStayTogetherWhenTheMapPansMidTransform() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Dimension dimension = dimension();
                dimension.setAttribute(HANDLES, new ArrayList<>(List.of(40f, 60f, 70f, 4f, 80f, 60f, 70f, 4f)), true);
                TestView view = new TestView(dimension);
                view.setSize(300, 300);
                PathTool tool = synchronousTool();
                tool.setView(view);
                tool.setActive(true);
                try {
                    click(view, 40, 60);
                    click(view, 80, 60, InputEvent.CTRL_DOWN_MASK);
                    tool.setEditMode(PathTool.EditMode.TRANSFORM);
                    // a rotate drag in progress, then the map pans (middle mouse / wheel zoom)
                    var rotate = viewHandle(tool, view, "rotate");
                    tool.handleSelectionPress(rotate);
                    Point mouse = view.worldToView(104, 60);
                    tool.handleSelectionDrag(mouse);
                    view.moveBy(37, 21);
                    // the box is the original box under the transform: it must carry the
                    // original node to where the (moved) node is drawn now
                    Point original = view.worldToView(40, 60);
                    var carried = tool.transformViewNow()
                            .transform(new java.awt.geom.Point2D.Double(original.x, original.y), null);
                    Point drawn = view.worldToView(60, 40); // node 0 turned a quarter clockwise around (60, 60)
                    assertEquals(drawn.x, carried.getX(), 1.01, "box follows the nodes after panning");
                    assertEquals(drawn.y, carried.getY(), 1.01);
                    assertEquals(view.worldToView(60, 60), toPoint(tool.toViewExact(tool.transformPivotWorld())),
                            "pivot stays on its block after panning");
                    tool.handleSelectionRelease(mouse); // the mouse did not move on screen
                    assertNodes(dimension, 60, 40, 70, 60, 80, 70);

                    // typed rotation, then the view zooms
                    type(tool, view, "90");
                    view.setZoom(1);
                    var typed = tool.transformViewNow().transform(toDouble(view.worldToView(60, 40)), null);
                    assertEquals(view.worldToView(40, 60).x, typed.getX(), 1.01, "typed rotation after zooming");
                    assertEquals(view.worldToView(40, 60).y, typed.getY(), 1.01);
                } finally {
                    tool.setActive(false);
                }
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    private static Point toPoint(java.awt.geom.Point2D point) {
        return new Point((int) Math.round(point.getX()), (int) Math.round(point.getY()));
    }

    private static java.awt.geom.Point2D.Double toDouble(Point point) {
        return new java.awt.geom.Point2D.Double(point.x, point.y);
    }

    @Test
    void transformEditorStretchesAlongTheBoxAxes() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Dimension dimension = dimension();
                dimension.setAttribute(HANDLES,
                        new ArrayList<>(List.of(40f, 60f, 70f, 4f, 80f, 60f, 70f, 4f, 60f, 70f, 70f, 4f)), true);
                TestView view = new TestView(dimension);
                view.setSize(300, 300);
                PathTool tool = synchronousTool();
                tool.setView(view);
                tool.setActive(true);
                try {
                    click(view, 40, 60);
                    click(view, 80, 60, InputEvent.CTRL_DOWN_MASK);
                    click(view, 60, 70, InputEvent.CTRL_DOWN_MASK);
                    tool.setEditMode(PathTool.EditMode.TRANSFORM);
                    // pivot: centre of the box around (40..80, 60..70) = (60, 65)
                    var stretchX = viewHandle(tool, view, "scaleX");
                    assertEquals(PathTool.HitKind.TRANSFORM_SCALE, tool.hitTest(stretchX).kind());
                    Point pivot = view.worldToView(60, 65);
                    drag(tool, stretchX, new Point(pivot.x + 2 * (stretchX.x - pivot.x), stretchX.y + 30));
                    assertNodes(dimension, 20, 60, 70, 100, 60, 70, 60, 70, 70); // only x doubled
                    assertEquals("Transform: 3 nodes", tool.viewportHeader());

                    var stretchY = viewHandle(tool, view, "scaleY");
                    assertEquals(PathTool.HitKind.TRANSFORM_SCALE, tool.hitTest(stretchY).kind());
                    drag(tool, stretchY, new Point(stretchY.x - 40, pivot.y + 2 * (stretchY.y - pivot.y)));
                    assertNodes(dimension, 20, 55, 70, 100, 55, 70, 60, 75, 70); // only y doubled
                } finally {
                    tool.setActive(false);
                }
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    /** A transform handle of the current selection, in view coordinates. */
    private static Point viewHandle(PathTool tool, TestView view, String which) {
        var nodes = new ArrayList<java.awt.geom.Point2D.Double>();
        for (var node : tool.getCurrentPath()) {
            Point point = view.worldToView(Math.round(node.x), Math.round(node.y));
            nodes.add(new java.awt.geom.Point2D.Double(point.x, point.y));
        }
        double angle = tool.transformBoxAngle();
        var box = PathTransformBox.box(nodes, tool.selectedNodes(), angle);
        var handle = PathTransformBox.frame(angle).transform(switch (which) {
            case "rotate" -> PathTransformBox.rotateHandle(box);
            case "move" -> tool.selectedNodes().size() < 2
                    ? PathTransformBox.singleMoveHandle(box)
                    : PathTransformBox.moveHandle(box);
            case "scale" -> PathTransformBox.scaleHandle(box, PathTransformBox.SCALE_UNIFORM);
            case "scaleX" -> PathTransformBox.scaleHandle(box, PathTransformBox.SCALE_X);
            case "scaleY" -> PathTransformBox.scaleHandle(box, PathTransformBox.SCALE_Y);
            default -> throw new IllegalArgumentException(which);
        }, null);
        return new Point((int) Math.round(handle.getX()), (int) Math.round(handle.getY()));
    }

    private static void drag(PathTool tool, Point from, Point to) {
        tool.handleSelectionPress(from);
        tool.handleSelectionDrag(new Point((from.x + to.x) / 2, (from.y + to.y) / 2));
        tool.handleSelectionDrag(to);
        tool.handleSelectionRelease(to);
    }

    /** x, y, z per node. */
    private static void assertNodes(Dimension dimension, float... expected) {
        var handles = dimension.getAttribute(HANDLES);
        assertEquals(expected.length / 3 * 4, handles.size(), handles.toString());
        for (int i = 0; i < expected.length / 3; i++) {
            assertEquals(expected[3 * i], handles.get(4 * i), "x of node " + i + " in " + handles);
            assertEquals(expected[3 * i + 1], handles.get(4 * i + 1), "y of node " + i + " in " + handles);
            assertEquals(expected[3 * i + 2], handles.get(4 * i + 2), "z of node " + i + " in " + handles);
        }
    }

    @Test
    void pathMenuAppliesAsRoadOrRiverInAResizableSettingsDialog() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Dimension dimension = dimension();
                TestView view = new TestView(dimension);
                PathTool tool = synchronousTool();
                tool.setView(view);
                tool.setBrush(brush());
                tool.setActive(true);
                try {
                    // the side panel keeps only the essentials; settings moved to the dialog
                    assertNull(findButton(tool.getOptionsPanel(), "PathTool.Apply"));
                    assertNotNull(findButton(tool.getOptionsPanel(), "PathTool.NewPath"));
                    assertNotNull(findButton(tool.getOptionsPanel(), "PathTool.PathSettings"));
                    assertNotNull(findButton(tool.pathSettingsPanel(), "PathTool.Apply"));
                    // toolbar: Node, Segment, Path
                    var segment = findComponent(view, JButton.class, "PathTool.SegmentMenu");
                    var pathButton = findComponent(view, JButton.class, "PathTool.PathMenu");
                    assertNotNull(pathButton);
                    assertTrue(pathButton.getX() > segment.getX());
                    assertFalse(findComponent(tool.pathMenu(), javax.swing.JMenuItem.class, "PathTool.Path.ApplyRoad")
                            .isEnabled(), "needs a path");

                    tool.addNode(20, 20);
                    tool.addNode(60, 20);
                    findComponent(tool.pathMenu(), javax.swing.JMenuItem.class, "PathTool.Path.ApplyRiver").doClick();
                    var dialog = tool.pathSettingsDialog();
                    assertTrue(dialog.isVisible());
                    assertEquals("Apply as river", dialog.getTitle());
                    assertTrue(dialog.isResizable(), "no size restrictions");
                    assertEquals(java.awt.Dialog.ModalityType.MODELESS, dialog.getModalityType(), "map stays usable");
                    assertTrue(checkbox(tool, "Only downhill").isSelected(), "river preset applied");
                    findComponent(tool.pathMenu(), javax.swing.JMenuItem.class, "PathTool.Path.ApplyRoad").doClick();
                    assertEquals("Apply as road", dialog.getTitle());
                    assertFalse(checkbox(tool, "Only downhill").isSelected(), "road preset applied");
                    assertTrue(checkbox(tool, "Set paint").isSelected());

                    // Apply from the dialog builds the path as before
                    dimension.setHeightAt(40, 20, 90f);
                    findButton(tool.pathSettingsPanel(), "PathTool.Apply").doClick();
                    assertNotEquals(90f, dimension.getHeightAt(40, 20), "terrain flattened along the road");

                    // the dialog goes away with the tool
                    tool.setActive(false);
                    assertFalse(dialog.isVisible());
                    tool.setActive(true);
                } finally {
                    tool.setActive(false);
                    if (tool.pathSettingsDialog() != null)
                        tool.pathSettingsDialog().dispose();
                }
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    private static javax.swing.JCheckBox checkbox(PathTool tool, String text) {
        for (var component : tool.pathSettingsPanel().getComponents())
            if (component instanceof javax.swing.JCheckBox box && text.equals(box.getText()))
                return box;
        throw new AssertionError("no checkbox " + text);
    }

    @Test
    void chevronPointsAlongDirection() {
        var right = new BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB);
        var left = new BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB);
        PathTool.paintChevron(right.createGraphics(), new Point(20, 20), 1, 0, false);
        PathTool.paintChevron(left.createGraphics(), new Point(20, 20), -1, 0, false);
        // ">" has its open end (wing tips) on the left, "<" on the right
        assertNotEquals(0, right.getRGB(15, 15));
        assertNotEquals(0, right.getRGB(25, 20));
        assertEquals(0, left.getRGB(15, 15));
        assertNotEquals(0, left.getRGB(25, 15));
        // drawn in the line colour only: no dark outline pixels
        var highlighted = new BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB);
        PathTool.paintChevron(highlighted.createGraphics(), new Point(20, 20), 1, 0, true);
        for (var image : List.of(right, highlighted))
            for (int x = 0; x < 40; x++)
                for (int y = 0; y < 40; y++) {
                    int rgb = image.getRGB(x, y);
                    if (rgb >>> 24 == 0xFF)
                        assertEquals(
                                image == right ? java.awt.Color.WHITE.getRGB() : PathTool.SELECTED_NODE_FILL.getRGB(),
                                rgb, "pixel " + x + "," + y);
                }
    }

    @Test
    void flowArrowsAreToggleableAndFollowNodeOrder() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Dimension dimension = dimension();
                dimension.setAttribute(HANDLES, new ArrayList<>(List.of(20f, 120f, 70f, 4f, 120f, 120f, 70f, 4f)),
                        true);
                TestView view = new TestView(dimension);
                PathTool tool = synchronousTool();
                tool.setView(view);
                tool.setActive(true);
                try {
                    var overlay = (MapToolOverlay) findComponent(view, JButton.class, "PathTool.EditorMode")
                            .getParent();
                    overlay.setSize(200, 200);
                    for (String hide : List.of("PathTool.ShowLine", "PathTool.ShowNodes", "PathTool.ShowLabel",
                            "PathTool.ShowRadius", "PathTool.ShowProfile"))
                        overlayPart(tool, hide).doClick();
                    // low in the view: the animated tool badge starts near the vertical third
                    Point middle = view.worldToView(70, 120);
                    var arrows = paintOverlay(overlay);
                    // flowing from node 1 (x=20) to node 2 (x=120): ">" with wings behind the tip
                    assertNotEquals(0, arrows.getRGB(middle.x - 5, middle.y - 5), "wing of >");
                    assertEquals(0, arrows.getRGB(middle.x + 5, middle.y - 5));

                    overlayPart(tool, "PathTool.ShowDirection").doClick();
                    assertEquals(0, differentPixels(paintOverlay(overlay),
                            new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB), 40, 105, 60, 30));
                    overlayPart(tool, "PathTool.ShowDirection").doClick();

                    tool.reversePath();
                    var reversed = paintOverlay(overlay);
                    assertNotEquals(0, reversed.getRGB(middle.x + 5, middle.y - 5), "wing of <");
                    assertEquals(0, reversed.getRGB(middle.x - 5, middle.y - 5));
                } finally {
                    tool.setActive(false);
                }
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    @Test
    void overlaysPopupStaysOpenWhileTogglingSeveralParts() throws Exception {
        var frameHolder = new JFrame[1];
        var popupHolder = new javax.swing.JPopupMenu[1];
        var toolHolder = new PathTool[1];
        SwingUtilities.invokeAndWait(() -> {
            toolHolder[0] = synchronousTool();
            var frame = new JFrame();
            var anchor = new JButton("anchor");
            frame.add(anchor);
            frame.setSize(300, 300);
            frame.setVisible(true);
            frameHolder[0] = frame;
            popupHolder[0] = toolHolder[0].overlaysMenu();
            popupHolder[0].show(anchor, 0, 0);
        });
        try {
            // closing caused by the click itself (focus changes elsewhere in the suite may
            // close the popup independently; those are not what this test is about)
            var closedByClick = new boolean[1];
            SwingUtilities
                    .invokeAndWait(() -> popupHolder[0].addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
                        @Override
                        public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {
                        }

                        @Override
                        public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {
                            closedByClick[0] = true;
                        }

                        @Override
                        public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {
                        }
                    }));
            for (String name : List.of("PathTool.ShowLabel", "PathTool.ShowRadius", "PathTool.ShowLine")) {
                SwingUtilities.invokeAndWait(() -> {
                    if (!popupHolder[0].isVisible())
                        popupHolder[0].show(frameHolder[0].getContentPane(), 0, 0);
                    closedByClick[0] = false;
                    var box = findComponent(popupHolder[0], JToggleButton.class, name);
                    long now = System.currentTimeMillis();
                    box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_PRESSED, now, InputEvent.BUTTON1_DOWN_MASK,
                            5, 5, 1, false, MouseEvent.BUTTON1));
                    box.dispatchEvent(
                            new MouseEvent(box, MouseEvent.MOUSE_RELEASED, now, 0, 5, 5, 1, false, MouseEvent.BUTTON1));
                    box.dispatchEvent(
                            new MouseEvent(box, MouseEvent.MOUSE_CLICKED, now, 0, 5, 5, 1, false, MouseEvent.BUTTON1));
                    assertFalse(closedByClick[0], "clicking " + name + " must not close the popup");
                });
                SwingUtilities.invokeAndWait(() -> {
                    assertFalse(overlayPart(toolHolder[0], name).isSelected(), name + " toggled off");
                });
            }
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                popupHolder[0].setVisible(false);
                frameHolder[0].dispose();
            });
        }
    }

    @Test
    void overlaySwitchesStartOnAndResetOnEveryActivation() throws Exception {
        var holder = new PathTool[1];
        var dimensionHolder = new Dimension[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                Dimension dimension = dimension();
                dimensionHolder[0] = dimension;
                TestView view = new TestView(dimension);
                PathTool tool = synchronousTool();
                holder[0] = tool;
                tool.setView(view);
                tool.setBrush(brush());
                tool.setActive(true);
                for (var part : tool.overlayParts())
                    assertTrue(part.isSelected(), part.getName());
                assertTrue(tool.overlaysSwitch().isSelected());
                tool.addNode(20, 20);
                tool.overlaysSwitch().doClick();
                for (var part : tool.overlayParts())
                    part.doClick();
                assertFalse(tool.overlaysSwitch().isSelected());
                assertFalse(dimension.getBitLayerValueAt(PreviewOperation.annotationLayer, 20, 20));
                tool.setActive(false);
                tool.setActive(true);
                assertTrue(tool.overlaysSwitch().isSelected());
                for (var part : tool.overlayParts())
                    assertTrue(part.isSelected(), part.getName());
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
        // activation schedules the preview on the event queue; check once it has run
        SwingUtilities.invokeAndWait(() -> {
            try {
                assertTrue(dimensionHolder[0].getBitLayerValueAt(PreviewOperation.annotationLayer, 20, 20),
                        "yellow preview is back after re-activation");
            } finally {
                try {
                    holder[0].setActive(false);
                } catch (java.beans.PropertyVetoException exception) {
                    throw new RuntimeException(exception);
                }
            }
        });
    }

    /** Overlay switches moved from the options panel into the viewport menu. */
    private static JToggleButton overlayPart(PathTool tool, String name) {
        return tool.overlayParts().stream().filter(part -> name.equals(part.getName())).findFirst().orElseThrow();
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
                    // the curve is only recomputed while something draws it: the line or the
                    // flow-direction arrows
                    var line = overlayPart(tool, "PathTool.ShowLine");
                    var direction = overlayPart(tool, "PathTool.ShowDirection");
                    line.doClick();
                    direction.doClick();
                    overlayPart(tool, "PathTool.ShowProfile").doClick(); // the profile strip draws the curve too
                    Object cached = cacheField.get(overlay);
                    dimension.setAttribute(HANDLES, new ArrayList<>(List.of(20f, 10f, 70f, 4f, 70f, 10f, 70f, 4f)),
                            true);
                    paintOverlay(overlay);
                    assertSame(cached, cacheField.get(overlay));
                    direction.doClick();
                    paintOverlay(overlay);
                    assertNotSame(cached, cacheField.get(overlay), "arrows alone need the curve");
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
                    tool.addNode(20, 20);
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
                    findButton(tool.pathSettingsPanel(), "PathTool.Apply").doClick();
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
                    tool.addNode(20, 20);
                    dimension.armSavePoint();
                    tool.addNode(60, 20);
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
    void mapEventsReplaceAndAddSelectionWithoutAppending() throws Exception {
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
                    assertEquals(Set.of(1), tool.selectedNodes());
                    click(view, 30, 30);
                    assertEquals(Set.of(1), tool.selectedNodes());
                    assertEquals(PathTool.EditMode.CURVE, tool.editMode());
                    // node 0 lies under node 1's backward tangent handle, but the node centre is
                    // closer
                    click(view, 10, 10, InputEvent.CTRL_DOWN_MASK);
                    assertEquals(Set.of(0, 1), tool.selectedNodes());
                    // Ctrl+click on a selected node deselects it
                    click(view, 10, 10, InputEvent.CTRL_DOWN_MASK);
                    assertEquals(Set.of(1), tool.selectedNodes());
                    click(view, 10, 10, InputEvent.CTRL_DOWN_MASK);
                    assertEquals(Set.of(0, 1), tool.selectedNodes());
                    // start away from node 0's backward tangent handle (curve mode is still active)
                    event(view, MouseEvent.MOUSE_PRESSED, -25, -5, MouseEvent.BUTTON1, InputEvent.BUTTON1_DOWN_MASK);
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
    void clickingSelectedNodeCyclesEditModesAndModePersistsAcrossSelections() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Dimension dimension = dimension();
                dimension.setAttribute(HANDLES, new ArrayList<>(List.of(10f, 10f, 70f, 4f, 80f, 80f, 70f, 6f)), true);
                TestView view = new TestView(dimension);
                PathTool tool = synchronousTool();
                tool.setView(view);
                tool.setActive(true);
                try {
                    click(view, 10, 10);
                    assertEquals(PathTool.EditMode.RADIUS, tool.editMode());
                    click(view, 10, 10);
                    assertEquals(PathTool.EditMode.CURVE, tool.editMode());
                    click(view, 10, 10);
                    assertEquals(PathTool.EditMode.HEIGHT, tool.editMode());
                    click(view, 10, 10);
                    assertEquals(PathTool.EditMode.TRANSFORM, tool.editMode());
                    click(view, 10, 10);
                    assertEquals(PathTool.EditMode.RADIUS, tool.editMode());
                    click(view, 10, 10);
                    assertEquals(PathTool.EditMode.CURVE, tool.editMode());
                    click(view, 80, 80, InputEvent.CTRL_DOWN_MASK);
                    assertEquals(Set.of(0, 1), tool.selectedNodes());
                    assertEquals(PathTool.EditMode.CURVE, tool.editMode());
                    click(view, 80, 80);
                    assertEquals(Set.of(0, 1), tool.selectedNodes());
                    assertEquals(PathTool.EditMode.HEIGHT, tool.editMode());
                    // the mode survives clearing and replacing the selection
                    tool.dispatchKeyEvent(new KeyEvent(view, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_ESCAPE,
                            KeyEvent.CHAR_UNDEFINED));
                    assertEquals(Set.of(), tool.selectedNodes());
                    assertEquals(PathTool.EditMode.HEIGHT, tool.editMode());
                    click(view, 10, 10);
                    assertEquals(PathTool.EditMode.HEIGHT, tool.editMode());
                    click(view, 10, 10);
                    assertEquals(PathTool.EditMode.TRANSFORM, tool.editMode());
                    click(view, 80, 80);
                    assertEquals(Set.of(1), tool.selectedNodes());
                    assertEquals(PathTool.EditMode.TRANSFORM, tool.editMode());
                } finally {
                    tool.setActive(false);
                }
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    @Test
    void editorButtonShowsModeAndMenuSwitchesIt() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Dimension dimension = dimension();
                dimension.setAttribute(HANDLES, new ArrayList<>(List.of(60f, 60f, 70f, 4f, 90f, 90f, 70f, 6f)), true);
                TestView view = new TestView(dimension);
                PathTool tool = synchronousTool();
                tool.setView(view);
                tool.setActive(true);
                try {
                    var button = (PathTool.DropdownButton) findComponent(view, javax.swing.JButton.class,
                            "PathTool.EditorMode");
                    assertNotNull(button);
                    var label = button.contentLabel();
                    assertTrue(label.getText().contains("Radius"), label.getText());
                    assertNotNull(label.getIcon());
                    // Node / Segment menus come first (top left), then the editor button
                    var nodeMenu = findComponent(view, javax.swing.JButton.class, "PathTool.NodeMenu");
                    var segmentMenu = findComponent(view, javax.swing.JButton.class, "PathTool.SegmentMenu");
                    assertEquals(new Point(8, 8), nodeMenu.getLocation());
                    assertTrue(segmentMenu.getX() > nodeMenu.getX());
                    assertTrue(button.getX() > segmentMenu.getX() + segmentMenu.getWidth());
                    // dropdown arrows are painted, not font glyphs (U+25BE rendered as an empty
                    // box in WorldPainter's font); button text must be displayable in its font
                    assertEquals(-1, label.getFont().canDisplayUpTo(label.getText()), label.getText());
                    var menuButton = findComponent(view, javax.swing.JButton.class, "PathTool.OverlaysMenu");
                    assertSame(PathTool.DROPDOWN_ARROW, menuButton.getIcon());
                    assertTrue(menuButton.getText() == null || menuButton.getText().isEmpty());
                    // text and arrow never overlap, on every dropdown button (they are laid out
                    // side by side instead of relying on look-and-feel margins)
                    for (String name : List.of("PathTool.EditorMode", "PathTool.NodeMenu", "PathTool.SegmentMenu")) {
                        var dropdown = (PathTool.DropdownButton) findComponent(view, javax.swing.JButton.class, name);
                        dropdown.setSize(dropdown.getPreferredSize());
                        dropdown.doLayout();
                        var text = dropdown.contentLabel().getBounds();
                        var arrow = dropdown.arrowLabel().getBounds();
                        assertTrue(text.x + text.width <= arrow.x, name + ": " + text + " vs " + arrow);
                        assertTrue(text.width >= dropdown.contentLabel().getPreferredSize().width,
                                name + " text not truncated");
                        assertTrue(dropdown.getText() == null || dropdown.getText().isEmpty(),
                                name + " paints its text only via the label");
                        assertSame(PathTool.DROPDOWN_ARROW, dropdown.arrowLabel().getIcon());
                    }
                    // the overlay only captures the mouse over the button
                    var overlay = button.getParent();
                    assertTrue(overlay.contains(button.getX() + 2, button.getY() + 2));
                    assertFalse(overlay.contains(60, 60));

                    var menu = tool.editorMenu();
                    assertEquals(PathTool.EditMode.values().length, menu.getComponentCount());
                    var height = findComponent(menu, javax.swing.JRadioButtonMenuItem.class,
                            "PathTool.EditorMode.HEIGHT");
                    assertFalse(height.isSelected());
                    height.doClick();
                    assertEquals(PathTool.EditMode.HEIGHT, tool.editMode());
                    assertTrue(label.getText().contains("Height above ground"), label.getText());
                    assertTrue(findComponent(tool.editorMenu(), javax.swing.JRadioButtonMenuItem.class,
                            "PathTool.EditorMode.HEIGHT").isSelected());

                    // cycling by clicking a selected node updates the button too
                    click(view, 60, 60);
                    click(view, 60, 60);
                    assertEquals(PathTool.EditMode.TRANSFORM, tool.editMode());
                    assertTrue(label.getText().contains("Transform"), label.getText());
                } finally {
                    tool.setActive(false);
                }
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    @Test
    void hoverHighlightsWhatAPressWouldGrabAndSetsCursor() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Dimension dimension = dimension();
                dimension.setAttribute(HANDLES, new ArrayList<>(List.of(50f, 50f, 70f, 10f, 90f, 90f, 70f, 6f)), true);
                TestView view = new TestView(dimension);
                PathTool tool = synchronousTool();
                tool.setView(view);
                tool.setActive(true);
                var mapCursor = view.getCursor();
                try {
                    event(view, MouseEvent.MOUSE_MOVED, 52, 50, MouseEvent.NOBUTTON, 0);
                    assertEquals(new PathTool.Hit(PathTool.HitKind.NODE, 0, 0), tool.hovered());
                    assertEquals(java.awt.Cursor.MOVE_CURSOR, view.getCursor().getType());
                    click(view, 50, 50);
                    // radius 10 < minimum offset: handle 22px right of the node
                    event(view, MouseEvent.MOUSE_MOVED, 72, 50, MouseEvent.NOBUTTON, 0);
                    assertEquals(new PathTool.Hit(PathTool.HitKind.RADIUS, 0, 0), tool.hovered());
                    assertEquals(java.awt.Cursor.HAND_CURSOR, view.getCursor().getType());
                    // the grabbed handle stays highlighted while dragging, even off the handle
                    event(view, MouseEvent.MOUSE_PRESSED, 72, 50, MouseEvent.BUTTON1, InputEvent.BUTTON1_DOWN_MASK);
                    event(view, MouseEvent.MOUSE_DRAGGED, 76, 30, MouseEvent.NOBUTTON, InputEvent.BUTTON1_DOWN_MASK);
                    assertEquals(new PathTool.Hit(PathTool.HitKind.RADIUS, 0, 0), tool.hovered());
                    event(view, MouseEvent.MOUSE_RELEASED, 76, 30, MouseEvent.BUTTON1, 0);
                    event(view, MouseEvent.MOUSE_MOVED, 10, 90, MouseEvent.NOBUTTON, 0);
                    assertNull(tool.hovered());
                    assertEquals(mapCursor, view.getCursor());
                    // height handles get a vertical-resize cursor
                    tool.setEditMode(PathTool.EditMode.HEIGHT);
                    float ground = dimension.getHeightAt(50, 50);
                    var axis = 50 + (int) Math.round(tool.heightAxisOffset());
                    var handle = PathEditHandles.heightHandle(new java.awt.geom.Point2D.Double(axis, 50), 70f - ground);
                    event(view, MouseEvent.MOUSE_MOVED, (int) Math.round(handle.x), (int) Math.round(handle.y),
                            MouseEvent.NOBUTTON, 0);
                    assertEquals(new PathTool.Hit(PathTool.HitKind.HEIGHT, 0, 0), tool.hovered());
                    assertEquals(java.awt.Cursor.N_RESIZE_CURSOR, view.getCursor().getType());
                } finally {
                    tool.setActive(false);
                }
                assertNull(tool.hovered());
                assertEquals(mapCursor, view.getCursor());
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    @Test
    void hoveredHandlesAndNodesAreDrawnDistinctly() {
        var plain = new java.awt.image.BufferedImage(40, 40, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        var hover = new java.awt.image.BufferedImage(40, 40, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        PathTool.paintSquareHandle(plain.createGraphics(), new java.awt.geom.Point2D.Double(20, 20), false);
        PathTool.paintSquareHandle(hover.createGraphics(), new java.awt.geom.Point2D.Double(20, 20), true);
        assertEquals(PathTool.SELECTED_NODE_FILL.getRGB(), plain.getRGB(20, 20));
        assertEquals(java.awt.Color.WHITE.getRGB(), hover.getRGB(20, 20));
        assertEquals(0, plain.getRGB(20 + PathTool.HOVERED_HANDLE_SIZE / 2, 20), "plain handle is smaller");
        assertNotEquals(0, hover.getRGB(20 + PathTool.HOVERED_HANDLE_SIZE / 2, 20));
        var ring = new java.awt.image.BufferedImage(40, 40, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        PathTool.paintNodeHover(ring.createGraphics(), new Point(20, 20));
        assertNotEquals(0, ring.getRGB(20 + 14, 20));
        assertEquals(0, ring.getRGB(20, 20), "glow ring leaves the marker untouched");
    }

    @Test
    void radiusHandleScalesAllSelectedNodesRelatively() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Dimension dimension = dimension();
                dimension.setAttribute(HANDLES, new ArrayList<>(List.of(20f, 20f, 70f, 10f, 80f, 20f, 70f, 5f)), true);
                TestView view = new TestView(dimension);
                PathTool tool = synchronousTool();
                tool.setView(view);
                tool.setActive(true);
                try {
                    click(view, 20, 20);
                    click(view, 80, 20, InputEvent.CTRL_DOWN_MASK);
                    assertEquals(PathTool.EditMode.RADIUS, tool.editMode());
                    // node 0's radius (10px) is below the minimum handle offset, so its handle sits
                    // 22px right
                    event(view, MouseEvent.MOUSE_PRESSED, 42, 20, MouseEvent.BUTTON1, InputEvent.BUTTON1_DOWN_MASK);
                    event(view, MouseEvent.MOUSE_DRAGGED, 64, 20, MouseEvent.NOBUTTON, InputEvent.BUTTON1_DOWN_MASK);
                    event(view, MouseEvent.MOUSE_RELEASED, 64, 20, MouseEvent.BUTTON1, 0);
                    assertEquals(List.of(20f, 20f, 70f, 20f, 80f, 20f, 70f, 10f), dimension.getAttribute(HANDLES));
                    assertEquals(Set.of(0, 1), tool.selectedNodes());
                } finally {
                    tool.setActive(false);
                }
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    @Test
    void tangentHandleDragSetsGlobalCurveStrengthAlongTangentOnly() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Dimension dimension = dimension();
                var initial = List.of(10f, 50f, 70f, 4f, 50f, 50f, 70f, 4f, 90f, 50f, 70f, 4f);
                dimension.setAttribute(HANDLES, new ArrayList<>(initial), true);
                TestView view = new TestView(dimension);
                PathTool tool = synchronousTool();
                tool.setView(view);
                tool.setActive(true);
                try {
                    click(view, 50, 50);
                    click(view, 50, 50);
                    assertEquals(PathTool.EditMode.CURVE, tool.editMode());
                    assertEquals(0f, tool.curveStrength());
                    // forward handle at strength 0 sits 22px along the tangent; half segment = 20px
                    event(view, MouseEvent.MOUSE_PRESSED, 72, 50, MouseEvent.BUTTON1, InputEvent.BUTTON1_DOWN_MASK);
                    event(view, MouseEvent.MOUSE_DRAGGED, 92, 70, MouseEvent.NOBUTTON, InputEvent.BUTTON1_DOWN_MASK);
                    assertEquals(0f, tool.curveStrength(), "only committed on release");
                    event(view, MouseEvent.MOUSE_RELEASED, 92, 70, MouseEvent.BUTTON1, 0);
                    assertEquals(1f, tool.curveStrength(), 1e-6);
                    assertEquals(initial, dimension.getAttribute(HANDLES));
                    assertEquals(Set.of(1), tool.selectedNodes());
                    assertEquals(PathTool.EditMode.CURVE, tool.editMode());
                } finally {
                    tool.setActive(false);
                }
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    @Test
    void heightHandleRaisesSelectedNodesAndMovesKeepHeightAboveGround() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Dimension dimension = dimension();
                dimension.setHeightAt(20, 50, 60f);
                dimension.setHeightAt(60, 50, 64f);
                dimension.setHeightAt(30, 50, 50f);
                dimension.setAttribute(HANDLES, new ArrayList<>(List.of(20f, 50f, 60f, 4f, 60f, 50f, 64f, 4f)), true);
                TestView view = new TestView(dimension);
                PathTool tool = synchronousTool();
                tool.setView(view);
                tool.setActive(true);
                int button = InputEvent.BUTTON1_DOWN_MASK;
                try {
                    click(view, 20, 50);
                    click(view, 60, 50, InputEvent.CTRL_DOWN_MASK);
                    click(view, 20, 50);
                    click(view, 20, 50);
                    assertEquals(PathTool.EditMode.HEIGHT, tool.editMode());
                    assertEquals(Set.of(0, 1), tool.selectedNodes());
                    // the gauge sits beside the node; its on-ground handle is 22px above ground
                    // level. 30px further up = +10 blocks, sideways drift is ignored (vertical
                    // lock)
                    int axis = 20 + (int) Math.round(tool.heightAxisOffset());
                    assertTrue(axis - 20 >= 30, "gauge offset " + (axis - 20));
                    event(view, MouseEvent.MOUSE_PRESSED, axis, 50, MouseEvent.BUTTON1, button);
                    event(view, MouseEvent.MOUSE_DRAGGED, axis + 15, 20, MouseEvent.NOBUTTON, button);
                    event(view, MouseEvent.MOUSE_RELEASED, axis + 15, 20, MouseEvent.BUTTON1, 0);
                    assertEquals(List.of(20f, 50f, 70f, 4f, 60f, 50f, 74f, 4f), dimension.getAttribute(HANDLES));
                    assertEquals(Set.of(0, 1), tool.selectedNodes());

                    // moving node 0 onto lower ground keeps it 10 blocks above ground
                    click(view, 90, 90);
                    event(view, MouseEvent.MOUSE_PRESSED, 20, 50, MouseEvent.BUTTON1, button);
                    event(view, MouseEvent.MOUSE_DRAGGED, 30, 50, MouseEvent.NOBUTTON, button);
                    event(view, MouseEvent.MOUSE_RELEASED, 30, 50, MouseEvent.BUTTON1, 0);
                    assertEquals(List.of(30f, 50f, 60f, 4f, 60f, 50f, 74f, 4f), dimension.getAttribute(HANDLES));
                } finally {
                    tool.setActive(false);
                }
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    @Test
    void shiftMakesDragsPreciseWithoutJumping() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Dimension dimension = dimension();
                dimension.setAttribute(HANDLES,
                        new ArrayList<>(
                                List.of(10f, 50f, 70f, 4f, 50f, 50f, 70f, 4f, 90f, 50f, 70f, 4f, 50f, 90f, 70f, 10f)),
                        true);
                TestView view = new TestView(dimension);
                PathTool tool = synchronousTool();
                tool.setView(view);
                tool.setActive(true);
                int shift = InputEvent.SHIFT_DOWN_MASK, button = InputEvent.BUTTON1_DOWN_MASK;
                try {
                    // node move: 10px normal, then Shift pressed mid-drag: 20px -> 2 blocks
                    event(view, MouseEvent.MOUSE_PRESSED, 50, 50, MouseEvent.BUTTON1, button);
                    event(view, MouseEvent.MOUSE_DRAGGED, 60, 50, MouseEvent.NOBUTTON, button);
                    event(view, MouseEvent.MOUSE_DRAGGED, 80, 50, MouseEvent.NOBUTTON, button | shift);
                    event(view, MouseEvent.MOUSE_RELEASED, 80, 50, MouseEvent.BUTTON1, shift);
                    var handles = dimension.getAttribute(HANDLES);
                    assertEquals(62f, handles.get(4));
                    assertEquals(50f, handles.get(5));

                    // radius handle, Shift held from the press: 22px -> 2.2px, factor 1.1
                    click(view, 50, 90);
                    assertEquals(Set.of(3), tool.selectedNodes());
                    event(view, MouseEvent.MOUSE_PRESSED, 72, 90, MouseEvent.BUTTON1, button | shift);
                    event(view, MouseEvent.MOUSE_DRAGGED, 94, 90, MouseEvent.NOBUTTON, button | shift);
                    event(view, MouseEvent.MOUSE_RELEASED, 94, 90, MouseEvent.BUTTON1, shift);
                    assertEquals(11f, dimension.getAttribute(HANDLES).get(15));

                    // end node tangent handle: 20px -> 2px beyond the offset ->
                    // strength ~0.1 instead of ~0.8
                    click(view, 10, 50);
                    click(view, 10, 50);
                    assertEquals(PathTool.EditMode.CURVE, tool.editMode());
                    var tangentHandle = view.worldToView(10 + 22, 50);
                    event(view, MouseEvent.MOUSE_PRESSED, tangentHandle.x, tangentHandle.y, MouseEvent.BUTTON1,
                            button | shift);
                    event(view, MouseEvent.MOUSE_DRAGGED, tangentHandle.x + 20, tangentHandle.y, MouseEvent.NOBUTTON,
                            button | shift);
                    event(view, MouseEvent.MOUSE_RELEASED, tangentHandle.x + 20, tangentHandle.y, MouseEvent.BUTTON1,
                            shift);
                    assertTrue(tool.curveStrength() > 0f && tool.curveStrength() <= 0.2f,
                            "precise strength was " + tool.curveStrength());

                    // Shift+press on empty map is left to WorldPainter: no box selection
                    var before = tool.selectedNodes();
                    event(view, MouseEvent.MOUSE_PRESSED, 0, 0, MouseEvent.BUTTON1, button | shift);
                    event(view, MouseEvent.MOUSE_DRAGGED, 99, 99, MouseEvent.NOBUTTON, button | shift);
                    event(view, MouseEvent.MOUSE_RELEASED, 99, 99, MouseEvent.BUTTON1, shift);
                    assertEquals(before, tool.selectedNodes());
                } finally {
                    tool.setActive(false);
                }
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    @Test
    void modifierCapturedForMarqueeAndNodeDrags() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Dimension dimension = dimension();
                var initial = List.of(10f, 10f, 70f, 4f, 50f, 10f, 70f, 6f, 90f, 10f, 70f, 8f);
                dimension.setAttribute(HANDLES, new ArrayList<>(initial), true);
                TestView view = new TestView(dimension);
                PathTool tool = synchronousTool();
                tool.setView(view);
                tool.setActive(true);
                try {
                    click(view, 10, 10);
                    event(view, MouseEvent.MOUSE_PRESSED, 35, -10, MouseEvent.BUTTON1,
                            InputEvent.BUTTON1_DOWN_MASK | InputEvent.CTRL_DOWN_MASK);
                    event(view, MouseEvent.MOUSE_DRAGGED, 65, 20, MouseEvent.NOBUTTON, InputEvent.BUTTON1_DOWN_MASK);
                    event(view, MouseEvent.MOUSE_RELEASED, 65, 20, MouseEvent.BUTTON1, 0);
                    assertEquals(Set.of(0, 1), tool.selectedNodes());
                    event(view, MouseEvent.MOUSE_PRESSED, 10, 10, MouseEvent.BUTTON1, InputEvent.BUTTON1_DOWN_MASK);
                    assertEquals(Set.of(0, 1), tool.selectedNodes());
                    event(view, MouseEvent.MOUSE_RELEASED, 10, 10, MouseEvent.BUTTON1, InputEvent.CTRL_DOWN_MASK);
                    // modifier is captured on press: a plain click on a selected node keeps the
                    // group and cycles the edit mode
                    assertEquals(Set.of(0, 1), tool.selectedNodes());
                    assertEquals(PathTool.EditMode.CURVE, tool.editMode());
                    click(view, 70, 60);
                    assertEquals(Set.of(), tool.selectedNodes());
                    assertEquals(PathTool.EditMode.CURVE, tool.editMode());
                    event(view, MouseEvent.MOUSE_PRESSED, 50, 10, MouseEvent.BUTTON1, InputEvent.BUTTON1_DOWN_MASK);
                    event(view, MouseEvent.MOUSE_DRAGGED, 55, 10, MouseEvent.NOBUTTON,
                            InputEvent.BUTTON1_DOWN_MASK | InputEvent.CTRL_DOWN_MASK);
                    event(view, MouseEvent.MOUSE_RELEASED, 55, 10, MouseEvent.BUTTON1, InputEvent.CTRL_DOWN_MASK);
                    assertEquals(Set.of(1), tool.selectedNodes());
                    assertEquals(List.of(10f, 10f, 70f, 4f, 55f, 10f, 70f, 6f, 90f, 10f, 70f, 8f),
                            dimension.getAttribute(HANDLES));
                    event(view, MouseEvent.MOUSE_PRESSED, 90, 10, MouseEvent.BUTTON1,
                            InputEvent.BUTTON1_DOWN_MASK | InputEvent.CTRL_DOWN_MASK);
                    event(view, MouseEvent.MOUSE_DRAGGED, 95, 10, MouseEvent.NOBUTTON, InputEvent.BUTTON1_DOWN_MASK);
                    event(view, MouseEvent.MOUSE_RELEASED, 95, 10, MouseEvent.BUTTON1, 0);
                    assertEquals(Set.of(1, 2), tool.selectedNodes());
                    assertEquals(List.of(10f, 10f, 70f, 4f, 60f, 10f, 70f, 6f, 95f, 10f, 70f, 8f),
                            dimension.getAttribute(HANDLES));
                    event(view, MouseEvent.MOUSE_PRESSED, 75, 30, MouseEvent.BUTTON1, InputEvent.BUTTON1_DOWN_MASK);
                    event(view, MouseEvent.MOUSE_DRAGGED, 105, -10, MouseEvent.NOBUTTON, InputEvent.BUTTON1_DOWN_MASK);
                    event(view, MouseEvent.MOUSE_RELEASED, 105, -10, MouseEvent.BUTTON1, 0);
                    assertEquals(Set.of(2), tool.selectedNodes());
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
                    tool.addNode(10, 10);
                    tool.tick(20, 20, true, false, 1f);
                    tool.tick(30, 30, false, true, 1f);
                    tool.addNode(30, 30);
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
                // nodes start on the ground, so moving them follows the ground exactly
                dimension.setHeightAt(10, 10, 55f);
                dimension.setHeightAt(30, 30, 56f);
                dimension.setHeightAt(20, 10, 81f);
                dimension.setHeightAt(40, 30, 92f);
                TestView view = new TestView(dimension);
                PathTool tool = synchronousTool();
                tool.setView(view);
                tool.setActive(true);
                try {
                    click(view, 10, 10);
                    click(view, 30, 30, InputEvent.CTRL_DOWN_MASK);
                    event(view, MouseEvent.MOUSE_PRESSED, 10, 10, MouseEvent.BUTTON1, InputEvent.BUTTON1_DOWN_MASK);
                    event(view, MouseEvent.MOUSE_DRAGGED, 20, 10, MouseEvent.NOBUTTON, InputEvent.BUTTON1_DOWN_MASK);
                    assertEquals(initial, dimension.getAttribute(HANDLES));
                    event(view, MouseEvent.MOUSE_RELEASED, 20, 10, MouseEvent.BUTTON1, 0);
                    assertEquals(List.of(20f, 10f, 81f, 4f, 40f, 30f, 92f, 6f), dimension.getAttribute(HANDLES));
                    assertEquals(Set.of(0, 1), tool.selectedNodes());
                    boolean snapEnabled = false;
                    for (var component : tool.pathSettingsPanel().getComponents())
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
                    KeyEvent delete = new KeyEvent(view, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_DELETE,
                            KeyEvent.CHAR_UNDEFINED);
                    // No MOUSE_ENTERED: the click itself proves the cursor is over the map.
                    click(view, 30, 30);
                    event(view, MouseEvent.MOUSE_EXITED, -5, 30, MouseEvent.NOBUTTON, 0);
                    assertFalse(tool.dispatchKeyEvent(delete));
                    event(view, MouseEvent.MOUSE_MOVED, 30, 30, MouseEvent.NOBUTTON, 0);
                    // Exiting into a child component (still inside the view's bounds) keeps
                    // shortcuts active.
                    event(view, MouseEvent.MOUSE_EXITED, 30, 30, MouseEvent.NOBUTTON, 0);
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

    private static <T extends java.awt.Component> T findComponent(java.awt.Container container, Class<T> type,
            String name) {
        for (var component : container.getComponents()) {
            if (type.isInstance(component) && name.equals(component.getName()))
                return type.cast(component);
            if (component instanceof java.awt.Container child) {
                T result = findComponent(child, type, name);
                if (result != null)
                    return result;
            }
        }
        return null;
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
        click(view, x, y, 0);
    }

    private static void click(TestView view, int x, int y, int modifiers) {
        event(view, MouseEvent.MOUSE_PRESSED, x, y, MouseEvent.BUTTON1, modifiers | InputEvent.BUTTON1_DOWN_MASK);
        event(view, MouseEvent.MOUSE_RELEASED, x, y, MouseEvent.BUTTON1, modifiers);
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
            // like WorldPainter's editor view: left-drags must not pan the map
            super(false, false);
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
