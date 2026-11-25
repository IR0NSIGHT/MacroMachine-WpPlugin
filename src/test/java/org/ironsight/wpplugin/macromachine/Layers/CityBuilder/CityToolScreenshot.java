package org.ironsight.wpplugin.macromachine.Layers.CityBuilder;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;

import org.ironsight.wpplugin.macromachine.Gui.OverlayStyle;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.TestDimension;
import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.painting.NibbleLayerPaint;

/**
 * Development harness: the City Tool's HUD on an empty view with the bundled
 * test schematics, a few houses placed and two selected; writes PNGs of the
 * view and of the open menus. Usage: {@code CityToolScreenshot <output dir>}.
 */
public final class CityToolScreenshot
{
    public static void main(String[] args) throws Exception {
        File out = new File(args.length > 0 ? args[0] : "target/city-tool-screens");
        out.mkdirs();
        Dimension dimension = TestDimension.createDimension(new TestDimension.DimensionParams(new Rectangle(1024, 768),
                -256, 512, 70, 1, 62, DefaultPlugin.JAVA_ANVIL_1_19, Terrain.GRASS));
        CityLayer layer = new CityLayer("Ukrainian village", "test");
        var schematics = new java.util.ArrayList<org.pepsoft.worldpainter.objects.WPObject>();
        var provider = new DefaultCustomObjectProvider();
        var folder = new File(CityToolScreenshot.class.getResource("/CityBuilder/Houses").toURI());
        var files = folder.listFiles((dir, name) -> name.endsWith(".schem"));
        java.util.Arrays.sort(files);
        for (File file : files)
            schematics.add(provider.loadObject(file));
        layer.setObjectList(schematics);
        CityLayer other = new CityLayer("Harbour", "test");
        dimension.setCustomLayers(List.of(layer, other));

        var view = new View(dimension);
        var operation = new CityEditToolOperation();
        SwingUtilities.invokeAndWait(() -> {
            var frame = new JFrame("City Tool");
            view.setBackground(new Color(92, 128, 70));
            view.setOpaque(true);
            view.setLayout(null);
            view.setPreferredSize(new java.awt.Dimension(1100, 720));
            frame.setContentPane(view);
            frame.pack();
            frame.setLocation(100, 40);
            frame.setVisible(true);
            operation.setView(view);
            operation.setPaint(new NibbleLayerPaint(layer));
            try {
                operation.setActive(true);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            int[][] houses = {{300, 300, 0}, {360, 300, 1}, {420, 310, 2}, {330, 380, 3}, {600, 420, 4}};
            for (int[] house : houses)
                layer.setDataAt(dimension, house[0], house[1],
                        new ObjectState(CityLayer.Direction.NORTH, false, house[2], house[0], house[1]));
            operation.handleClick(300, 300, false, false);
            operation.handleClick(360, 300, false, true);
            operation.handleClick(330, 380, false, true);
        });
        Thread.sleep(1500); // the tool name label animates into the corner
        save(view, new File(out, "city-hud.png"));
        for (String menu : List.of("schematics", "layer", "overlays")) {
            var shown = new JPopupMenu[1];
            SwingUtilities.invokeAndWait(() -> {
                shown[0] = OverlayStyle.popup(menu.equals("schematics")
                        ? operation.schematicsMenu()
                        : menu.equals("layer") ? operation.layerMenu() : operation.overlaysMenu());
                shown[0].show(view, 40, 50);
            });
            Thread.sleep(500);
            save(shown[0], new File(out, "city-" + menu + "-menu.png"));
            SwingUtilities.invokeAndWait(() -> shown[0].setVisible(false));
        }
        SwingUtilities.invokeAndWait(operation::openTypeGrid);
        Thread.sleep(300);
        save(view, new File(out, "city-type-grid.png"));
        System.exit(0);
    }

    private static void save(JComponent component, File file) throws Exception {
        var image = new BufferedImage[1];
        SwingUtilities.invokeAndWait(() -> {
            image[0] = new BufferedImage(component.getWidth(), component.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image[0].createGraphics();
            component.paint(g);
            g.dispose();
        });
        ImageIO.write(image[0], "png", file);
    }

    private static class View extends WorldPainterView
    {
        private Dimension dimension;

        View(Dimension dimension) {
            this.dimension = dimension;
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
