package org.ironsight.wpplugin.macromachine.threeDRendering;

import org.pepsoft.minecraft.ChunkFactory;
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.util.SubProgressReceiver;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.World2;
import org.pepsoft.worldpainter.exporting.*;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.platforms.JavaExportSettings;

import java.awt.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.pepsoft.minecraft.ChunkFactory.Stage.DISK_WRITING;
import static org.pepsoft.util.ExceptionUtils.chainContains;

public class PreviewExporter extends JavaWorldExporter {

    protected PreviewExporter(World2 world, WorldExportSettings exportSettings) {
        super(world, exportSettings);
    }

    public HashMap<Point, WorldRegion> export(Dimension dimension) {
        ExportSettings originalSettings = dimension.getExportSettings();

        try {
            ExportSettings minimalExportSettings = new JavaExportSettings(
                    JavaExportSettings.FloatMode.LEAVE_FLOATING,
                    JavaExportSettings.FloatMode.LEAVE_FLOATING,
                    JavaExportSettings.FloatMode.LEAVE_FLOATING,
                    JavaExportSettings.FloatMode.LEAVE_FLOATING,
                    JavaExportSettings.FloatMode.LEAVE_FLOATING,
                    false,
                    false,
                    false,
                    false,
                    false, false,
            false,
                    false
            );

            dimension.setExportSettings(minimalExportSettings);

            Set<Point> tiles = dimension.getWorld().getExportSettings().getTilesToExport();
            HashSet<Point> regions = new HashSet<>();
            int lowestRegionX = Integer.MAX_VALUE, highestRegionX = Integer.MIN_VALUE, lowestRegionZ =
                    Integer.MAX_VALUE, highestRegionZ = Integer.MIN_VALUE;
            for (Point tile : tiles) {
                int regionX = tile.x >> 2;
                int regionZ = tile.y >> 2;
                regions.add(new Point(regionX, regionZ));
                if (regionX < lowestRegionX) {
                    lowestRegionX = regionX;
                }
                if (regionX > highestRegionX) {
                    highestRegionX = regionX;
                }
                if (regionZ < lowestRegionZ) {
                    lowestRegionZ = regionZ;
                }
                if (regionZ > highestRegionZ) {
                    highestRegionZ = regionZ;
                }
            }

            HashMap<Point, WorldRegion> regionItems = new HashMap<>();
            for (Point regionCoords : regions) {
                final int minHeight = dimension.getMinHeight(), maxHeight = dimension.getMaxHeight();
                final Map<Layer, LayerExporter> exporters = getExportersForRegion(dimension, regionCoords);
                final Map<Layer, LayerExporter> ceilingExporters = null;
                final WorldPainterChunkFactory
                        chunkFactory = new WorldPainterChunkFactory(dimension, exporters, platform, maxHeight);
                final WorldPainterChunkFactory ceilingChunkFactory = null;

                WorldRegion worldRegion =
                        new WorldRegion(regionCoords.x, regionCoords.y, minHeight, maxHeight, platform);
                Dimension ceiling = dimension.getWorld().getDimension(new Dimension.Anchor(dimension.getAnchor().dim,
                        dimension.getAnchor().role, true, 0));
                ;
                exportRegion(worldRegion,
                        dimension,
                        ceiling,
                        regionCoords,
                        false,
                        exporters,
                        ceilingExporters,
                        chunkFactory,
                        ceilingChunkFactory,
                        null);


                regionItems.put(regionCoords, worldRegion);
            }

            return regionItems;
        } catch (Throwable t) {
            System.err.println(t);
        } finally {
            dimension.setExportSettings(originalSettings);
        }
        return null;
    }

    @Override
    public ExportResults exportRegion(MinecraftWorld minecraftWorld, Dimension dimension, Dimension ceiling,
                                      Point regionCoords, boolean tileSelection, Map<Layer, LayerExporter> exporters,
                                      Map<Layer, LayerExporter> ceilingExporters, ChunkFactory chunkFactory,
                                      ChunkFactory ceilingChunkFactory, ProgressReceiver progressReceiver) {
        return super.exportRegion(minecraftWorld,
                dimension,
                ceiling,
                regionCoords,
                tileSelection,
                exporters,
                ceilingExporters,
                chunkFactory,
                ceilingChunkFactory,
                progressReceiver);
    }
}
