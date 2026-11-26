package org.ironsight.wpplugin.macromachine.Layers.CityBuilder;

import java.awt.Color;
import java.awt.image.BufferedImage;
import javax.vecmath.Point3i;

import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.objects.WPObject;

/**
 * Pictures of a schematic, one pixel per block: the top view the map and the
 * City Tool show, shaded like WorldPainter's height map (light from the top
 * left: slopes facing it lighter, those facing away darker, so roofs and walls
 * read in 3D), and the front and side views of the schematic picker.
 */
final class SchematicViews
{
    /**
     * How much one block of height difference to a neighbour lightens or darkens.
     */
    static final double SLOPE_SHADE = 0.09;
    static final double MIN_LIGHT = 0.5, MAX_LIGHT = 1.45;

    private SchematicViews() {
    }

    /**
     * The top view as ARGB colours, {@code [x][y]}; 0 where no block is seen from
     * above (transparent).
     */
    static int[][] top(WPObject object) {
        Point3i size = object.getDimensions();
        int[][] heights = new int[size.x][size.y];
        Material[][] materials = new Material[size.x][size.y];
        for (int x = 0; x < size.x; x++)
            for (int y = 0; y < size.y; y++) {
                heights[x][y] = -1;
                for (int z = size.z - 1; z >= 0; z--) {
                    Material material = object.getMaterial(x, y, z);
                    if (material != null && material != Material.AIR) {
                        heights[x][y] = z + 1;
                        materials[x][y] = material;
                        break;
                    }
                }
            }
        int[][] colours = new int[size.x][size.y];
        for (int x = 0; x < size.x; x++)
            for (int y = 0; y < size.y; y++) {
                if (heights[x][y] < 0)
                    continue;
                // light from the top left (north west), like WorldPainter's hill shading
                int west = x > 0 ? Math.max(0, heights[x - 1][y]) : 0;
                int north = y > 0 ? Math.max(0, heights[x][y - 1]) : 0;
                double slope = (heights[x][y] - west) + (heights[x][y] - north);
                double light = Math.max(MIN_LIGHT, Math.min(MAX_LIGHT, 1 + slope * SLOPE_SHADE));
                // higher blocks a little lighter overall
                light *= 0.8 + 0.2 * Math.sqrt((double) heights[x][y] / Math.max(1, size.z));
                colours[x][y] = shade(materials[x][y].colour, light);
            }
        return colours;
    }

    /**
     * The front view: looking at the schematic's front (north) side, x to the
     * right, height up; nearer blocks lighter.
     */
    static BufferedImage front(WPObject object) {
        Point3i size = object.getDimensions();
        var image = new BufferedImage(Math.max(1, size.x), Math.max(1, size.z), BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < size.x; x++)
            for (int z = 0; z < size.z; z++)
                for (int y = 0; y < size.y; y++) {
                    Material material = object.getMaterial(x, y, z);
                    if (material != null && material != Material.AIR) {
                        image.setRGB(x, size.z - 1 - z, shade(material.colour, depthLight(y, size.y)));
                        break;
                    }
                }
        return image;
    }

    /**
     * The side view: looking at the schematic's east side, its front (north) to the
     * left, height up; nearer blocks lighter.
     */
    static BufferedImage side(WPObject object) {
        Point3i size = object.getDimensions();
        var image = new BufferedImage(Math.max(1, size.y), Math.max(1, size.z), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < size.y; y++)
            for (int z = 0; z < size.z; z++)
                for (int x = size.x - 1; x >= 0; x--) {
                    Material material = object.getMaterial(x, y, z);
                    if (material != null && material != Material.AIR) {
                        image.setRGB(y, size.z - 1 - z, shade(material.colour, depthLight(size.x - 1 - x, size.x)));
                        break;
                    }
                }
        return image;
    }

    /**
     * Light of a block {@code depth} blocks behind the nearest face of a
     * {@code size} deep schematic.
     */
    private static double depthLight(int depth, int size) {
        return 1.15 - 0.55 * depth / Math.max(1.0, size - 1);
    }

    /**
     * {@code rgb} lightened or darkened by {@code light} (1: unchanged), opaque.
     */
    static int shade(int rgb, double light) {
        var base = new Color(rgb);
        int red = (int) Math.max(0, Math.min(255, Math.round(base.getRed() * light)));
        int green = (int) Math.max(0, Math.min(255, Math.round(base.getGreen() * light)));
        int blue = (int) Math.max(0, Math.min(255, Math.round(base.getBlue() * light)));
        return 0xff000000 | red << 16 | green << 8 | blue;
    }
}
