package org.ironsight.wpplugin.macromachine.Layers.PathBuilder;

import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import org.ironsight.wpplugin.macromachine.Gui.MapToolOverlay;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.TestDimension;
import org.pepsoft.util.AttributeKey;
import org.pepsoft.worldpainter.Dimension;

/**
 * Isolated run of the Path Tool overlay outside WorldPainter: a synthetic world
 * with hills, a valley and a lake, a river path through it, shown in the height
 * editor so the unravelled height profile (terrain, water, path) is visible.
 * Run its main; pass a PNG path as the first argument to also save a
 * screenshot.
 */
public class PathProfileDemo
{
    private static final AttributeKey<ArrayList<Float>> HANDLES = new AttributeKey<>("PATHTOOL-PATHHANDLES",
            new ArrayList<>());

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Dimension dimension = TestDimension
                        .createDimension(new TestDimension.DimensionParams(new Rectangle(0, 0, 640, 384), -64, 320, 70,
                                42, 62, org.pepsoft.worldpainter.DefaultPlugin.JAVA_ANVIL_1_19,
                                org.pepsoft.worldpainter.Terrain.GRASS));
                for (int x = 0; x < 640; x++)
                    for (int y = 0; y < 384; y++) {
                        double hills = 72 + 14 * Math.sin(x / 45.0) + 6 * Math.cos(y / 37.0);
                        double valley = 10 * Math.exp(-Math.pow((y - 0.35 * x - 60) / 40.0, 2));
                        double lake = Math.hypot(x - 330, y - 190);
                        float height = (float) (hills - valley - (lake < 55 ? 9 * (1 - lake / 55) : 0));
                        dimension.setHeightAt(x, y, height);
                        if (lake < 48)
                            dimension.setWaterLevelAt(x, y, 66);
                    }
                int[][] positions = {{50, 70}, {150, 120}, {250, 150}, {330, 190}, {420, 215}, {520, 260}, {600, 300}};
                float[] radii = {5, 6, 7, 9, 8, 7, 10};
                var handles = new ArrayList<Float>();
                for (int i = 0; i < positions.length; i++) {
                    int x = positions[i][0], y = positions[i][1];
                    handles.addAll(List.of((float) x, (float) y,
                            (float) Math.round(dimension.getHeightAt(x, y)) + (i % 2 == 0 ? 2 : -1), radii[i]));
                }
                dimension.setAttribute(HANDLES, handles, true);

                var view = new PathToolSelectionTest.TestView(dimension);
                view.setSize(900, 620);
                var tool = new PathTool(
                        () -> new PathPreviewWorker(Runnable::run, Runnable::run, PathPreviewMask::calculate));
                tool.setView(view);
                tool.setActive(true);
                // select nodes 3..5 (Ctrl+A needs the cursor over the map, then plain
                // selection)
                view.dispatchEvent(new MouseEvent(view, MouseEvent.MOUSE_MOVED, 0, 0, 330, 190, 0, false));
                for (int i : new int[]{2, 3, 4}) {
                    int x = positions[i][0], y = positions[i][1];
                    int modifiers = i == 2 ? 0 : InputEvent.CTRL_DOWN_MASK;
                    view.dispatchEvent(new MouseEvent(view, MouseEvent.MOUSE_PRESSED, 0,
                            modifiers | InputEvent.BUTTON1_DOWN_MASK, x, y, 1, false, MouseEvent.BUTTON1));
                    view.dispatchEvent(new MouseEvent(view, MouseEvent.MOUSE_RELEASED, 0, modifiers, x, y, 1, false,
                            MouseEvent.BUTTON1));
                }
                tool.setEditMode(PathTool.EditMode.HEIGHT);

                var frame = new JFrame("Path Tool - height profile (isolated)");
                frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                frame.getContentPane().setBackground(new java.awt.Color(60, 90, 60));
                frame.getContentPane().setLayout(null);
                frame.getContentPane().add(view);
                frame.getContentPane().setPreferredSize(new java.awt.Dimension(900, 620));
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);

                if (args.length > 0 && args[0].equals("bench")) {
                    // repaint cost of the overlay (map overlay + strip) in the height view
                    MapToolOverlay overlay = null;
                    for (var component : view.getComponents())
                        if (component instanceof MapToolOverlay found)
                            overlay = found;
                    overlay.setSize(900, 620);
                    var image = new BufferedImage(900, 620, BufferedImage.TYPE_INT_ARGB);
                    for (String phase : new String[]{"zoomed out", "strip only", "no strip", "all off", "zoomed in"}) {
                        for (var part : tool.overlayParts())
                            part.setSelected(phase.equals("strip only")
                                    ? part.getName().equals("PathTool.ShowProfile")
                                    : !(phase.equals("no strip") && part.getName().equals("PathTool.ShowProfile")));
                        tool.overlaysSwitch().setSelected(!phase.equals("all off"));
                        if (phase.equals("zoomed in"))
                            for (int notch = 0; notch < 10; notch++)
                                overlay.dispatchEvent(new java.awt.event.MouseWheelEvent(overlay,
                                        java.awt.event.MouseEvent.MOUSE_WHEEL, 0, 0, 330, 560, 0, false,
                                        java.awt.event.MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, -1));
                        for (int warm = 0; warm < 30; warm++)
                            overlay.paint(image.createGraphics());
                        long started = System.nanoTime();
                        int runs = 200;
                        for (int run = 0; run < runs; run++) {
                            var g = image.createGraphics();
                            overlay.paint(g);
                            g.dispose();
                        }
                        System.out.printf("BENCH %s: %.2f ms per repaint%n", phase,
                                (System.nanoTime() - started) / 1e6 / runs);
                        try { // the overlay's own drawing only, without its child components
                            var own = overlay.getClass().getDeclaredMethod("paintComponent", java.awt.Graphics.class);
                            own.setAccessible(true);
                            long ownStarted = System.nanoTime();
                            for (int run = 0; run < runs; run++) {
                                var g = image.createGraphics();
                                own.invoke(overlay, g);
                                g.dispose();
                            }
                            System.out.printf("BENCH %s (own drawing only): %.2f ms%n", phase,
                                    (System.nanoTime() - ownStarted) / 1e6 / runs);
                        } catch (ReflectiveOperationException exception) {
                            throw new RuntimeException(exception);
                        }
                    }
                    frame.dispose();
                    return;
                }
                if (args.length > 0) {
                    MapToolOverlay overlay = null;
                    for (var component : view.getComponents())
                        if (component instanceof MapToolOverlay found)
                            overlay = found;
                    overlay.setSize(900, 620);
                    if (args.length > 1 && args[1].startsWith("zoomed")) {
                        overlay.paint(new BufferedImage(900, 620, BufferedImage.TYPE_INT_ARGB).createGraphics());
                        int notches = args[1].contains(":")
                                ? Integer.parseInt(args[1].substring(args[1].indexOf(':') + 1))
                                : 4;
                        for (int notch = 0; notch < notches; notch++) // real wheel events, zooming in at the lake
                            overlay.dispatchEvent(new java.awt.event.MouseWheelEvent(overlay,
                                    java.awt.event.MouseEvent.MOUSE_WHEEL, 0, 0, 330, 560, 0, false,
                                    java.awt.event.MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, -1));
                    }
                    boolean transform = args.length > 1 && args[1].startsWith("transform");
                    if (transform) { // Transform editor; "transform:rotate" mid rotate drag
                        tool.setEditMode(PathTool.EditMode.TRANSFORM);
                        if (args[1].endsWith("rotate")) {
                            var nodes = new java.util.ArrayList<java.awt.geom.Point2D.Double>();
                            for (var node : tool.getCurrentPath()) {
                                var point = view.worldToView(Math.round(node.x), Math.round(node.y));
                                nodes.add(new java.awt.geom.Point2D.Double(point.x, point.y));
                            }
                            var box = PathTransformBox.box(nodes, tool.selectedNodes());
                            var handle = PathTransformBox.rotateHandle(box);
                            var start = new java.awt.Point((int) handle.x, (int) handle.y);
                            tool.handleSelectionPress(start);
                            tool.handleSelectionDrag(new java.awt.Point(start.x + 60, start.y + 20));
                        }
                    }
                    if (args.length > 1 && args[1].startsWith("band:")) { // a drag in progress
                        overlay.paint(new BufferedImage(900, 620, BufferedImage.TYPE_INT_ARGB).createGraphics());
                        boolean horizontal = args[1].endsWith("h");
                        overlay.dispatchEvent(new java.awt.event.MouseEvent(overlay,
                                java.awt.event.MouseEvent.MOUSE_PRESSED, 0, InputEvent.BUTTON1_DOWN_MASK, 250, 530, 1,
                                false, java.awt.event.MouseEvent.BUTTON1));
                        overlay.dispatchEvent(
                                new java.awt.event.MouseEvent(overlay, java.awt.event.MouseEvent.MOUSE_DRAGGED, 0,
                                        InputEvent.BUTTON1_DOWN_MASK, horizontal ? 480 : 268, horizontal ? 548 : 585, 1,
                                        false, java.awt.event.MouseEvent.NOBUTTON));
                    }
                    var image = new BufferedImage(900, 620, BufferedImage.TYPE_INT_ARGB);
                    var g = image.createGraphics();
                    g.setColor(new java.awt.Color(60, 90, 60));
                    g.fillRect(0, 0, 900, 620);
                    overlay.paint(g);
                    g.dispose();
                    // the strip only
                    ImageIO.write(transform
                            ? image
                            : image.getSubimage(0, tool.stripBounds().y - 8, 900, tool.stripBounds().height + 16),
                            "png", new File(args[0]));
                }
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }
}
