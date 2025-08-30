package org.ironsight.wpplugin.macromachine.threeDRendering;

import org.pepsoft.minecraft.Chunk;
import org.pepsoft.minecraft.Material;
import org.pepsoft.util.Box;
import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.exporting.MinecraftWorld;
import org.pepsoft.worldpainter.exporting.VirtualChunk;
import org.pepsoft.worldpainter.exporting.WorldExportSettings;
import org.pepsoft.worldpainter.exporting.WorldRegion;
import org.pepsoft.worldpainter.layers.exporters.ExporterSettings;
import org.pepsoft.worldpainter.objects.MinecraftWorldObject;

import javax.vecmath.Point3i;

import java.awt.*;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.singleton;
import static org.ironsight.wpplugin.macromachine.threeDRendering.TestData.*;
import static org.pepsoft.worldpainter.Constants.DIM_NORMAL;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE;
import static org.pepsoft.worldpainter.Dimension.Anchor.NORMAL_DETAIL;
import static org.pepsoft.worldpainter.Dimension.Role.DETAIL;
import static org.pepsoft.worldpainter.exporting.WorldRegion.CHUNKS_PER_SIDE;

public class Export3DViewHelper {

    public static MinecraftWorldObject renderTileToSurfaceObject(Set<Tile> tiles, Dimension dimension) {
        WorldExportSettings settings = new WorldExportSettings();

        settings.setTilesToExport(tiles
                .stream()
                .map(t -> new Point(t.getX(), t.getY()))
                .collect(Collectors.toSet())
        );

        HashSet<WorldExportSettings.Step> skipSteps = new HashSet<>();
        skipSteps.add(WorldExportSettings.Step.CAVES);
        skipSteps.add(WorldExportSettings.Step.LEAVES);
        skipSteps.add(WorldExportSettings.Step.LIGHTING);
        skipSteps.add(WorldExportSettings.Step.RESOURCES);
        settings.setStepsToSkip(skipSteps);

        settings.setDimensionsToExport(singleton(DIM_NORMAL));

        dimension.getWorld().setExportSettings(settings);
        dimension.getWorld().setCreateGoodiesChest(false);


        final Dimension.Anchor anchor = dimension.getAnchor();
        World2 world = dimension.getWorld();
        world.setSpawnPoint(new Point(0, 0));
        world.setSpawnPointDimension((anchor.role == DETAIL) ? null : anchor);

        PreviewExporter exporter = new PreviewExporter(dimension.getWorld(), dimension.getWorld().getExportSettings());
        Map<Point, WorldRegion> worldRegionList = exporter.export(dimension);

        int tileMinX = Integer.MAX_VALUE, tileMinY = Integer.MAX_VALUE, tileMaxX = Integer.MIN_VALUE, tileMaxY =
                Integer.MIN_VALUE;
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

        Box displayObjectBBX = new Box(tileMinX * TILE_SIZE,
                (1 + tileMaxX) * TILE_SIZE
                , tileMinY * TILE_SIZE,
                (1 + tileMaxY) * TILE_SIZE,
                tileMinHeight,
                tileMaxHeight);

        Point3i tileOffset =
                new Point3i(-displayObjectBBX.getWidth() / 2, -displayObjectBBX.getLength()/ 2,
                        -tileMinHeight);//-displayObjectBBX.getWidth() /
        // 2,
        // -displayObjectBBX
        // .getLength() /
        // 2,                -displayObjectBBX.getHeight() / 2);

        final MinecraftWorldObject
                minecraftWorldObject = new MinecraftWorldObject("Preview",
                displayObjectBBX,
                previewHeight,
                waterHeight,
                null,
                tileOffset);

        System.out.println("display " + displayObjectBBX);

        // chunk size = 16x16
        // tile size = 128 x 128
        // region size = 512 x 512 (x 256H)

        int blockMinX = Integer.MAX_VALUE, blockMinY = Integer.MAX_VALUE, blockMaxX = Integer.MIN_VALUE, blockMaxY =
                Integer.MIN_VALUE;
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
                      /*  if (!minecraftWorldObject.getVolume().contains(blockPosX,blockPosZ, tileMaxHeight)) {
                            System.out.println("reject");
                            continue;
                        }*/
                        System.out.println("accept");
                        blockMaxX = Math.max(blockMaxX, blockPosX);
                        blockMinX = Math.min(blockMinX, blockPosX);

                        blockMaxY = Math.max(blockMaxY, blockPosZ);
                        blockMinY = Math.min(blockMinY, blockPosZ);

                        //minecraftWorldObject.addChunk(chunk);
                        // copy values over from chunk to worldobject. necessary manually because chunk region offset
                        // is lost when using addChunk.
                        int dz = minecraftWorldObject.getVolume().getZ1();
                        for (int x = 0; x < 16; ++x) {
                            for (int z = 0; z < 16; ++z) {
                                for (int y = Math.min(chunk.getHighestNonAirBlock(x, z),
                                        dz + minecraftWorldObject.getVolume().getHeight() - 1); y >= dz; --y) {
                                    int xx = blockPosX + x, zz = blockPosZ + z, yy = y;
                                    Material mat = chunk.getMaterial(x, y, z);
                                    minecraftWorldObject.setMaterialAt(xx, zz, yy,
                                            mat);
                                }
                            }
                        }
                    }
                }
            }
        }
        System.out.println("block extents are:" + new Box(blockMinX, blockMaxX + 16, blockMinY, blockMaxY + 16, 0, 0));

        //FIXME set offset in minecraftWorldObject so tile is centered.

        return minecraftWorldObject;
    }

    private class ChunkWrapper {

    }
}
