package org.ironsight.wpplugin.macromachine.Layers.CityBuilder;

import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.exporting.Fixup;
import org.pepsoft.worldpainter.exporting.IncidentalLayerExporter;
import org.pepsoft.worldpainter.exporting.MinecraftWorld;
import org.pepsoft.worldpainter.exporting.SecondPassLayerExporter;
import org.pepsoft.worldpainter.layers.Bo2Layer;
import org.pepsoft.worldpainter.layers.FloodWithLava;
import org.pepsoft.worldpainter.layers.bo2.Bo2ObjectProvider;
import org.pepsoft.worldpainter.layers.exporters.ExporterSettings;
import org.pepsoft.worldpainter.layers.exporters.WPObjectExporter;
import org.pepsoft.worldpainter.objects.WPObject;

import javax.vecmath.Point3i;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.pepsoft.minecraft.Material.AIR;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE_BITS;
import static org.pepsoft.worldpainter.objects.WPObject.*;

public class CityLayerExporter extends WPObjectExporter<CityLayer>
        implements SecondPassLayerExporter, IncidentalLayerExporter {
    private final Random applyRandom = new Random();

    public CityLayerExporter(Dimension dimension, Platform platform,
                             ExporterSettings settings, CityLayer layer) {
        super(dimension, platform, settings, layer);
    }

    @Override
    public Fixup apply(Point3i location, int intensity, Rectangle exportedArea, MinecraftWorld minecraftWorld) {
        WPObject object = layer.getObjectAt(dimension, location.x, location.y, platform);
        if (object == null) // layer says we dont want to place a building here.
            return null;
        // final long seed = dimension.getSeed() + location.x + location.y * 4099L + location.z * 65537L + layer.hashCode();
        //applyRandom.setSeed(seed);
        // final int variation = object.getAttribute(ATTRIBUTE_Y_VARIATION);
        final int height = ((object.getAttribute(ATTRIBUTE_HEIGHT_MODE) == HEIGHT_MODE_TERRAIN) ? location.z : 0)
                + object.getAttribute(ATTRIBUTE_VERTICAL_OFFSET);
        // + ((variation > 0) ? (applyRandom.nextInt(variation + 1) - ((variation + 1) / 2)) : 0); // Bias odd variation downwards
        renderObject(minecraftWorld, dimension, platform, object, location.x, location.y, height);
        return null;
    }

    @Override
    public Set<Stage> getStages() {
        return Set.of(Stage.ADD_FEATURES);
    }

    @Override
    public List<Fixup> carve(Rectangle area, Rectangle exportedArea, MinecraftWorld minecraftWorld) {
        // Nothing to do.
        return List.of();
    }
    /**
     * Determines whether an object's attributes allow it to be placed at a
     * certain location, and if so where along the vertical axis.
     *
     * @return An indication of where along the vertical axis the object may
     *     be placed, which may be {@link Placement#NONE} if it may not be
     *     placed at all.
     */
    private Placement getPlacement(final MinecraftWorld minecraftWorld, final Dimension dimension, final int x, final int y, final int z, final WPObject object) {
        final boolean spawnUnderWater = object.getAttribute(ATTRIBUTE_SPAWN_IN_WATER), spawnUnderLava = object.getAttribute(ATTRIBUTE_SPAWN_IN_LAVA);
        final boolean spawnOnWater = object.getAttribute(ATTRIBUTE_SPAWN_ON_WATER), spawnOnLava = object.getAttribute(ATTRIBUTE_SPAWN_ON_LAVA);
        final boolean flooded;
        if (object.getAttribute(ATTRIBUTE_HEIGHT_MODE) == HEIGHT_MODE_TERRAIN) {
            flooded = dimension.getWaterLevelAt(x, y) >= z;
        } else {
            flooded = (z > dimension.getIntHeightAt(x, y)) && (dimension.getWaterLevelAt(x, y) >= z);
        }
        if (flooded && (spawnUnderWater || spawnUnderLava || spawnOnWater || spawnOnLava)) {
            boolean lava = dimension.getBitLayerValueAt(FloodWithLava.INSTANCE, x, y);
            if (lava ? (spawnUnderLava && spawnOnLava) : (spawnUnderWater && spawnOnWater)) {
                return Placement.ON_LAND ;
            } else if (lava ? spawnUnderLava : spawnUnderWater) {
                return Placement.ON_LAND;
            } else if (lava ? spawnOnLava : spawnOnWater) {
                return Placement.FLOATING;
            }
        } else if (! flooded) {
            Material materialUnderCoords = (z > minecraftWorld.getMinHeight()) ? minecraftWorld.getMaterialAt(x, y, z - 1) : AIR;
            if (object.getAttribute(ATTRIBUTE_SPAWN_ON_LAND) && (! materialUnderCoords.veryInsubstantial)) {
                return Placement.ON_LAND;
            } else if ((! object.getAttribute(ATTRIBUTE_NEEDS_FOUNDATION)) && materialUnderCoords.veryInsubstantial) {
                return Placement.ON_LAND;
            }
        }
        return Placement.NONE;
    }
    @Override
    public List<Fixup> addFeatures(Rectangle area, Rectangle exportedArea, MinecraftWorld minecraftWorld) {
        for (int chunkX = area.x; chunkX < area.x + area.width; chunkX += 16) {
            for (int chunkY = area.y; chunkY < area.y + area.height; chunkY += 16) {
                if (!dimension.isTilePresent(chunkX >> TILE_SIZE_BITS, chunkY >> TILE_SIZE_BITS)) {
                    continue;
                }
                for (int xx = chunkX; xx < chunkX + 16; xx++) {
                    for (int yy = chunkY; yy < chunkY + 16; yy++) {
                        WPObject object = getLayer().getObjectAt(dimension,xx,yy, platform);
                        if (object == null)
                            continue;

                        final int height = ((object.getAttribute(ATTRIBUTE_HEIGHT_MODE) == HEIGHT_MODE_TERRAIN) ? (dimension.getIntHeightAt(xx, yy) + 1) : 0)
                                + object.getAttribute(ATTRIBUTE_VERTICAL_OFFSET);

                        final Placement placement = getPlacement(minecraftWorld, dimension, xx,yy, height, object);

                        final int z = (placement == Placement.ON_LAND) ? height : dimension.getWaterLevelAt(xx,yy) + 1;
                        renderObject(minecraftWorld,dimension,platform,object,xx,yy,z);
                    }
                }
            }
        }
        return List.of();
    }
}
