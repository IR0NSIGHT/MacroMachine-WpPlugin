package org.ironsight.wpplugin.macromachine.threeDRendering;

import static java.util.Collections.singleton;
import static org.ironsight.wpplugin.macromachine.threeDRendering.TestData.*;
import static org.pepsoft.worldpainter.Constants.DIM_NORMAL;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE;
import static org.pepsoft.worldpainter.Dimension.Role.DETAIL;
import static org.pepsoft.worldpainter.exporting.WorldRegion.CHUNKS_PER_SIDE;

import java.awt.*;
import java.util.*;
import java.util.stream.Collectors;
import javax.vecmath.Point3i;
import org.pepsoft.minecraft.Chunk;
import org.pepsoft.minecraft.Material;
import org.pepsoft.util.Box;
import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.exporting.WorldExportSettings;
import org.pepsoft.worldpainter.layers.exporters.ExporterSettings;
import org.pepsoft.worldpainter.exporting.WorldRegion;
import org.pepsoft.worldpainter.layers.CustomLayer;
import org.pepsoft.worldpainter.objects.MinecraftWorldObject;

public class Export3DViewHelper
{

    public static MinecraftWorldObject renderTileToSurfaceObject(Set<Tile> tiles, Dimension liveDimension, boolean useFullExport) {
        Dimension dimension = createDimensionFromTiles(tiles, liveDimension);
        World2 world = dimension.getWorld();
        Dimension.Anchor anchor = dimension.getAnchor();

        WorldExportSettings settings = new WorldExportSettings();
        settings.setTilesToExport(tiles.stream().map(t -> new Point(t.getX(), t.getY())).collect(Collectors.toSet()));

        if (!useFullExport) {
            HashSet<WorldExportSettings.Step> skipSteps = new HashSet<>();
            skipSteps.add(WorldExportSettings.Step.CAVES);
            skipSteps.add(WorldExportSettings.Step.LEAVES);
            skipSteps.add(WorldExportSettings.Step.LIGHTING);
            skipSteps.add(WorldExportSettings.Step.RESOURCES);
            settings.setStepsToSkip(skipSteps);
        }

        settings.setDimensionsToExport(singleton(DIM_NORMAL));
        world.setExportSettings(settings);
        world.setCreateGoodiesChest(false);
        world.setSpawnPoint(new Point(0, 0));
        world.setSpawnPointDimension((anchor.role == DETAIL) ? null : anchor);

        PreviewExporter exporter = new PreviewExporter(world, world.getExportSettings());
        exporter.setUseFullExport(useFullExport);
        Map<Point, WorldRegion> worldRegionList = exporter.export(dimension);

        int tileMinX = Integer.MAX_VALUE, tileMinY = Integer.MAX_VALUE, tileMaxX = Integer.MIN_VALUE,
                tileMaxY = Integer.MIN_VALUE;
        int tileMaxHeight = Integer.MIN_VALUE, tileMinHeight = Integer.MAX_VALUE;
        for (Tile tile : tiles) {
            tileMaxX = Math.max(tileMaxX, tile.getX());
            tileMinX = Math.min(tileMinX, tile.getX());

            tileMaxY = Math.max(tileMaxY, tile.getY());
            tileMinY = Math.min(tileMinY, tile.getY());

            tileMaxHeight = Math.max(tileMaxHeight, tile.getHighestIntHeight());
            tileMinHeight = Math.min(tileMinHeight, tile.getLowestIntHeight());
        }

        int previewHeight = tileMaxHeight;
        int waterHeight = 62;

        // find highest block pos in exported chunks
        for (Point regionPos : worldRegionList.keySet()) {
            WorldRegion region = worldRegionList.get(regionPos);
            for (int chunkX = 0; chunkX <= CHUNKS_PER_SIDE; chunkX++) {
                for (int chunkY = 0; chunkY <= CHUNKS_PER_SIDE; chunkY++) {
                    Chunk chunk = region.getChunk(chunkX + (int) regionPos.getX() * CHUNKS_PER_SIDE,
                            chunkY + (int) regionPos.getY() * CHUNKS_PER_SIDE);
                    if (chunk != null)
                        tileMaxHeight = Math.max(tileMaxHeight, chunk.getHighestNonAirBlock());
                }
            }
        }

        Box displayObjectBBX = new Box(tileMinX * TILE_SIZE, (1 + tileMaxX) * TILE_SIZE, tileMinY * TILE_SIZE,
                (1 + tileMaxY) * TILE_SIZE, tileMinHeight, tileMaxHeight);

        Point3i tileOffset = new Point3i(-displayObjectBBX.getWidth() / 2, -displayObjectBBX.getLength() / 2,
                -tileMinHeight);

        final MinecraftWorldObject minecraftWorldObject = new MinecraftWorldObject("Preview", displayObjectBBX,
                previewHeight, waterHeight, null, tileOffset);

        System.out.println("display " + displayObjectBBX);

        int blockMinX = Integer.MAX_VALUE, blockMinY = Integer.MAX_VALUE, blockMaxX = Integer.MIN_VALUE,
                blockMaxY = Integer.MIN_VALUE;
        for (Point regionPos : worldRegionList.keySet()) {
            WorldRegion region = worldRegionList.get(regionPos);
            System.out.println(region);
            for (int chunkX = 0; chunkX <= CHUNKS_PER_SIDE; chunkX++) {
                for (int chunkY = 0; chunkY <= CHUNKS_PER_SIDE; chunkY++) {
                    int blockPosX = ((int) regionPos.getX() * CHUNKS_PER_SIDE + chunkX) * 16;
                    int blockPosZ = ((int) regionPos.getY() * CHUNKS_PER_SIDE + chunkY) * 16;
                    Chunk chunk = region.getChunk(chunkX + (int) regionPos.getX() * CHUNKS_PER_SIDE,
                            chunkY + (int) regionPos.getY() * CHUNKS_PER_SIDE);
                    if (chunk != null) {
                        System.out.println("accept");
                        blockMaxX = Math.max(blockMaxX, blockPosX);
                        blockMinX = Math.min(blockMinX, blockPosX);

                        blockMaxY = Math.max(blockMaxY, blockPosZ);
                        blockMinY = Math.min(blockMinY, blockPosZ);

                        int dz = minecraftWorldObject.getVolume().getZ1();
                        for (int x = 0; x < 16; ++x) {
                            for (int z = 0; z < 16; ++z) {
                                for (int y = Math.min(chunk.getHighestNonAirBlock(x, z),
                                        dz + minecraftWorldObject.getVolume().getHeight() - 1); y >= dz; --y) {
                                    int xx = blockPosX + x, zz = blockPosZ + z, yy = y;
                                    Material mat = chunk.getMaterial(x, y, z);
                                    if (mat != Material.HARDENED_CLAY) {
                                        minecraftWorldObject.setMaterialAt(xx, zz, yy, mat);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        System.out.println("block extents are:" + new Box(blockMinX, blockMaxX + 16, blockMinY, blockMaxY + 16, 0, 0));

        return minecraftWorldObject;
    }

    private static Dimension createDimensionFromTiles(Set<Tile> clonedTiles, Dimension originalDim) {
        World2 originalWorld = originalDim.getWorld();
        int minHeight = originalDim.getMinHeight();
        int maxHeight = originalDim.getMaxHeight();

        World2 standaloneWorld = new World2(originalWorld.getPlatform(), minHeight, maxHeight);
        Dimension standaloneDim = new Dimension(standaloneWorld,
                originalDim.getName(), originalDim.getMinecraftSeed(),
                originalDim.getTileFactory(), originalDim.getAnchor());

        for (Tile tile : clonedTiles) {
            standaloneDim.addTile(tile);
        }

        standaloneDim.setCustomLayers(new ArrayList<>(originalDim.getCustomLayers()));

        for (CustomLayer layer : originalDim.getCustomLayers()) {
            ExporterSettings settings = originalDim.getLayerSettings(layer);
            if (settings != null) {
                standaloneDim.setLayerSettings(layer, settings);
            }
        }

        standaloneDim.setSubsurfaceMaterial(originalDim.getSubsurfaceMaterial());
        standaloneDim.setTopLayerMinDepth(originalDim.getTopLayerMinDepth());
        standaloneDim.setTopLayerVariation(originalDim.getTopLayerVariation());
        standaloneDim.setTopLayerAnchor(originalDim.getTopLayerAnchor());
        standaloneDim.setSubsurfaceLayerAnchor(originalDim.getSubsurfaceLayerAnchor());

        return standaloneDim;
    }

    private class ChunkWrapper
    {
    }
}
