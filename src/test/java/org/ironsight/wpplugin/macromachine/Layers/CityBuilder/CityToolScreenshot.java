package org.ironsight.wpplugin.macromachine.Layers.CityBuilder;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;

import org.ironsight.wpplugin.macromachine.Gui.OverlayStyle;
import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.biomeschemes.CustomBiomeManager;
import org.pepsoft.worldpainter.painting.NibbleLayerPaint;

/**
 * Development harness: the City Tool's HUD on WorldPainter's own map view of a
 * default (Perlin noise) world, with the bundled test schematics, a few houses
 * placed in the middle of the view and three selected; writes PNGs of the view
 * and of the open menus. Usage: {@code CityToolScreenshot <output dir>}.
 */
public final class CityToolScreenshot
{
    /** WorldPainter's New World defaults (Minecraft 1.18+ heights). */
    private static final int MIN_HEIGHT = -64, MAX_HEIGHT = 320, BASE_HEIGHT = 58, WATER_LEVEL = 62;
    /** The houses: x, z offsets from the village centre, schematic index. */
    private static final int[][] HOUSES = {{-60, -40, 0}, {0, -40, 1}, {60, -30, 2}, {-30, 40, 3}, {50, 50, 4}};
    /** The view's zoom (WorldPainter's zoom steps: 1 is 2:1, 2 is 4:1). */
    private static final int ZOOM = 2;

    public static void main(String[] args) throws Exception {
        File out = new File(args.length > 0 ? args[0] : "target/city-tool-screens");
        out.mkdirs();
        // WorldPainter's map view and layer manager read these singletons
        if (Configuration.getInstance() == null)
            Configuration.setInstance(new Configuration());
        if (org.pepsoft.worldpainter.plugins.WPPluginManager.getInstance() == null)
            org.pepsoft.worldpainter.plugins.WPPluginManager.initialise(java.util.UUID.randomUUID(), null);
        Dimension dimension = null;
        Point centre = null;
        // the first seed whose default terrain is dry land under every house
        for (long seed = 1; centre == null; seed++) {
            dimension = noiseWorld(seed);
            centre = dryVillageCentre(dimension);
        }
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

        final Dimension world = dimension;
        final Point village = centre;
        var view = new WorldPainter(world, ColourScheme.DEFAULT, new CustomBiomeManager());
        var operation = new CityEditToolOperation();
        SwingUtilities.invokeAndWait(() -> {
            var frame = new JFrame("City Tool");
            view.setLayout(null);
            view.setDrawBrush(false);
            view.setPreferredSize(new java.awt.Dimension(1100, 720));
            frame.setContentPane(view);
            frame.pack();
            frame.setLocation(100, 40);
            frame.setVisible(true);
            view.setZoom(ZOOM);
            view.moveTo(village);
            operation.setView(view);
            operation.setPaint(new NibbleLayerPaint(layer));
            try {
                operation.setActive(true);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            for (int[] house : HOUSES) {
                int x = village.x + house[0], y = village.y + house[1];
                layer.setDataAt(world, x, y, new ObjectState(CityLayer.Direction.NORTH, false, house[2], x, y));
            }
            operation.handleClick(village.x + HOUSES[0][0], village.y + HOUSES[0][1], false);
            operation.handleClick(village.x + HOUSES[1][0], village.y + HOUSES[1][1], true);
            operation.handleClick(village.x + HOUSES[3][0], village.y + HOUSES[3][1], true);
        });
        Thread.sleep(15000); // the map tiles render in the background; the tool name label animates
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
        SwingUtilities.invokeAndWait(() -> operation.openRandomize(CityEditToolOperation.Randomized.ROTATION));
        Thread.sleep(300);
        save(view, new File(out, "city-randomize-rotation.png"));
        SwingUtilities.invokeAndWait(() -> operation.openRandomize(CityEditToolOperation.Randomized.TYPE));
        Thread.sleep(300);
        save(view, new File(out, "city-randomize-type.png"));
        SwingUtilities.invokeAndWait(operation::closeOperation);
        SwingUtilities.invokeAndWait(operation::openTypeGrid);
        Thread.sleep(300);
        save(view, new File(out, "city-type-grid.png"));
        SwingUtilities.invokeAndWait(() -> ((AbstractButton) org.ironsight.wpplugin.macromachine.Gui.OperationPanel
                .find(operation.typeGridComponent(), "CityTool.TypeGrid.Filters")).doClick());
        Thread.sleep(300);
        save(view, new File(out, "city-type-grid-filters.png"));
        SwingUtilities.invokeAndWait(() -> ((AbstractButton) org.ironsight.wpplugin.macromachine.Gui.OperationPanel
                .find(operation.typeGridComponent(), "CityTool.TypeGrid.Filters")).doClick());
        SwingUtilities.invokeAndWait(() -> operation.typeGridBrightToggle().doClick());
        Thread.sleep(300);
        save(view, new File(out, "city-type-grid-bright.png"));
        SwingUtilities.invokeAndWait(() -> operation.handleKeyInteraction(java.awt.event.KeyEvent.VK_ESCAPE));
        Thread.sleep(300);
        save(view, new File(out, "city-hud-bright.png"));
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

    /**
     * A world as WorldPainter's New World makes it by default: Perlin noise
     * grassland around the base height with water below the water level, 1024 x
     * 1024 blocks.
     */
    private static Dimension noiseWorld(long seed) {
        TileFactory tileFactory = TileFactoryFactory.createNoiseTileFactory(seed, Terrain.GRASS, MIN_HEIGHT,
                MAX_HEIGHT, BASE_HEIGHT, WATER_LEVEL, false, true, 20f, 1.0);
        World2 world = new World2(DefaultPlugin.JAVA_ANVIL_1_19, MIN_HEIGHT, MAX_HEIGHT);
        Dimension dimension = new Dimension(world, "Surface", seed, tileFactory, Dimension.Anchor.NORMAL_DETAIL);
        int tiles = 1024 / Constants.TILE_SIZE;
        for (int tileX = 0; tileX < tiles; tileX++)
            for (int tileY = 0; tileY < tiles; tileY++)
                dimension.addTile(tileFactory.createTile(tileX, tileY));
        return dimension;
    }

    /**
     * A village centre near the middle of the world with dry land (a few blocks
     * above the water) around every house; null if this world has none.
     */
    private static Point dryVillageCentre(Dimension dimension) {
        for (int radius = 0; radius <= 320; radius += 16)
            for (int dx = -radius; dx <= radius; dx += 16)
                for (int dy = -radius; dy <= radius; dy += 16) {
                    if (Math.max(Math.abs(dx), Math.abs(dy)) != radius)
                        continue;
                    Point centre = new Point(512 + dx, 512 + dy);
                    if (isDry(dimension, centre))
                        return centre;
                }
        return null;
    }

    private static boolean isDry(Dimension dimension, Point centre) {
        for (int[] house : HOUSES)
            for (int ox = -12; ox <= 12; ox += 4)
                for (int oy = -12; oy <= 12; oy += 4)
                    if (dimension.getHeightAt(centre.x + house[0] + ox, centre.y + house[1] + oy) < WATER_LEVEL + 3)
                        return false;
        return true;
    }
}
