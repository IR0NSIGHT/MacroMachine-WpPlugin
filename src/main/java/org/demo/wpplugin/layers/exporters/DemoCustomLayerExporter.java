package org.demo.wpplugin.layers.exporters;

import org.demo.wpplugin.layers.BezierPathLayer;
import org.demo.wpplugin.layers.DemoLayer;
import org.pepsoft.minecraft.Chunk;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.exporting.*;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.layers.exporters.ExporterSettings;

import javax.vecmath.Point3i;
import java.awt.*;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.pepsoft.worldpainter.exporting.SecondPassLayerExporter.Stage.ADD_FEATURES;
import static org.pepsoft.worldpainter.exporting.SecondPassLayerExporter.Stage.CARVE;

/**
 * The exporter is responsible for applying the layer to the map when the world is being Exported by WorldPainter.
 *
 * <p>It must implement at least <em>one</em> of {@link FirstPassLayerExporter} or {@link SecondPassLayerExporter}.
 * Usually it only needs to implement one, although a complicated layer could conceivably need to perform operations at
 * both stages.
 *
 * <p>If can also optionally implement {@link IncidentalLayerExporter}, which is used to apply the layer incidentally
 * at one block location instead of an entire chunk or region. This is currently only used to apply layers to Custom
 * Cave Layer floors. If support for that is not necessary it can be left out.
 *
 * <p>The {@link Layer} by default finds the exporter by looking for a class in the {@code exporters} subpackage and
 * with the word {@code Exporter} appended to its own class name. In that case the exporter must have a default (public, no
 * arguments) constructor. You could also override the {@link Layer#getRenderer()} method if you want to do something
 * more complicated.
 */
public class DemoCustomLayerExporter extends AbstractLayerExporter<BezierPathLayer> implements FirstPassLayerExporter, SecondPassLayerExporter, IncidentalLayerExporter {
    public DemoCustomLayerExporter(Dimension dimension, Platform platform, ExporterSettings settings, BezierPathLayer layer) {
        super(dimension, platform, settings, layer);
    }

    // TODO: add explanation of ExporterSettings

    // FirstPassLayerExporter

    /**
     * This method will be invoked once for every chunk as it is being created during an Export. It should read the
     * layer values for its own layer from the {@link Tile} and then edit the {@link Chunk} as required. For
     * {@code Tile}s that don't contain this layer this method will not be executed at all.
     *
     * <p>For most layers you will only need to implement this <em>or</em> {@link SecondPassLayerExporter}, not both,
     * although that is possible for complicated situation.
     *
     * <p>Note that the operation must be deterministic. I.e. it must produce the same results for the same world seed
     * and coordinates, and the results must align with those that the exporter would create for the same world seed in
     * neighbouring chunks.
     *
     * @param tile The {@code Tile} that the current chunk belongs to. Get the layer values from this object.
     * @param chunk The {@code Chunk} that is currently being created. Apply your changes to this object.
     */
    @Override
    public void render(Tile tile, Chunk chunk) {
        // TODO finish settings support with custom settings and settings editor

        // Create a local seed to ensure the results are deterministic, yet vary by world seed and by chunk
        final long seed = (dimension.getSeed() << 8) ^ ((long) chunk.getxPos() << 4) ^ chunk.getzPos();
        final int xOffset = (chunk.getxPos() & 7) << 4, zOffset = (chunk.getzPos() & 7) << 4;
        for (int xInChunk = 0; xInChunk < 16; xInChunk++) {
            for (int zInChunk = 0; zInChunk < 16; zInChunk++) {
                final int xInTile = xOffset + xInChunk, zInTile = zOffset + zInChunk;
                final int layerValue = tile.getLayerValue(DemoLayer.INSTANCE, xInTile, zInTile);
                // TODO: modify the Chunk as required according to the layer value
            }
        }
    }

    // SecondPassLayerExporter

    /**
     * The second export pass is further divided into stages. This method indicates which stages are implemented by this
     * exporter. The stages are:
     *
     * <table>
     *     <tr>
     *         <th>Stage</th><th>Description</th>
     *     </tr>
     *     <tr>
     *         <td>CARVE</td><td>This stage is for carving out features; in general the exporter should only remove
     *         blocks in this stage.</td>
     *     </tr>
     *     <tr>
     *         <td>ADD_FEATURES</td><td>This stage is for adding features; in general the exporter should only place
     *         blocks in this stage. This stage is only executed after the {@code CARVE} stage has been executed for
     *         <em>all</em> second pass layers.</td>
     *     </tr>
     * </table>
     *
     * The division in these two stages exists for purposes such as making cave layers interact better and not interfere
     * with each other. It is not a hard rule that a layer may not place blocks during {@code CARVE} or remove them
     * during {@code ADD_FEATURES}.
     */
    public Set<Stage> getStages() {
        return EnumSet.of(CARVE, ADD_FEATURES);
    }

    /**
     * This method will be invoked once for every region as it is being created during an Export, after the first pass
     * has been completed and all the chunks for the region created. It should read the layer values for its own layer
     * from the {@link Dimension} and then edit the {@link MinecraftWorld} as required. For regions that don't contain
     * this layer this method will not be executed at all.
     *
     * <p>In this method the exporter should generally only remove blocks, although that is not a hard requirement. If
     * the exporter does not return {@code CARVE} from {@link #getStages()} it does not have to implement this method.
     *
     * <p>For most layers you will only need to implement this <em>or</em> {@link FirstPassLayerExporter}, not both,
     * although that is possible for complicated situation.
     *
     * <p>Note that the operation must be deterministic. I.e. it must produce the same results for the same world seed
     * and coordinates, and the results must align with those that the exporter would create for the same world seed in
     * neighbouring regions.

     * @param area The area that the exporter should actually process. This may be larger than the {@code exportedArea}.
     *             At the time of this writing this is area is actually one chunk larger on each side. The purpose of
     *             this is to allow the exporter to easily replicate cross border effects, for example structures that
     *             would have been created on the other side of the region border by a parallel exporter but protrude
     *             into this region. But look at the {@link Fixup} mechanism below for an alternative method to achieve
     *             that.
     * @param exportedArea The area that will actually be saved to disk. At the time of this writing this always
     *                     corresponds to one Minecraft region (512x512 area).
     * @param minecraftWorld The region that is currently being created. Apply your changes to this object.
     * @return An optional list of {@link Fixup}s to apply in a later stage. This can be used e.g. to create structures
     * that straddle region borders. The {@code Fixup}s will be invoked once the second pass is complete for this region
     * and all surrounding regions.
     */
    @Override
    public List<Fixup> carve(Rectangle area, Rectangle exportedArea, MinecraftWorld minecraftWorld) {
        // Create a local seed to ensure the results are deterministic, yet vary by world seed and by region
        final long seed = (dimension.getSeed() << 8) ^ ((long) exportedArea.x << 4) ^ exportedArea.y;
        for (int x = area.x; x < (area.x + area.width); x++) {
            for (int y = area.y; y < (area.y + area.height); y++) {
                final int layerValue = dimension.getLayerValueAt(DemoLayer.INSTANCE, x, y);
                // TODO: modify the MinecraftWorld as required according to the layer value
            }
        }
        return null; // Or return Fixups for creating structures that cross region borders, if that is hard to do in one go
    }

    /**
     * This method will be invoked once for every region as it is being created during an Export, after the first pass
     * and the {@code CARVE} stage of the second pass has been completedd. It should read the layer values for its own
     * layer from the {@link Dimension} and then edit the {@link MinecraftWorld} as required. For regions that don't
     * contain this layer this method will not be executed at all.
     *
     * <p>In this method the exporter should generally only place blocks, although that is not a hard requirement. If
     * the exporter does not return {@code ADD_FEATURES} from {@link #getStages()} it does not have to implement this
     * method.
     *
     * <p>For most layers you will only need to implement this <em>or</em> {@link FirstPassLayerExporter}, not both,
     * although that is possible for complicated situation.
     *
     * <p>Note that the operation must be deterministic. I.e. it must produce the same results for the same world seed
     * and coordinates, and the results must align with those that the exporter would create for the same world seed in
     * neighbouring regions.

     * @param area The area that the exporter should actually process. This may be larger than the {@code exportedArea}.
     *             At the time of this writing this is area is actually one chunk larger on each side. The purpose of
     *             this is to allow the exporter to easily replicate cross border effects, for example structures that
     *             would have been created on the other side of the region border by a parallel exporter but protrude
     *             into this region. But look at the {@link Fixup} mechanism below for an alternative method to achieve
     *             that.
     * @param exportedArea The area that will actually be saved to disk. At the time of this writing this always
     *                     corresponds to one Minecraft region (512x512 area).
     * @param minecraftWorld The region that is currently being created. Apply your changes to this object.
     * @return An optional list of {@link Fixup}s to apply in a later stage. This can be used e.g. to create structures
     * that straddle region borders. The {@code Fixup}s will be invoked once the second pass is complete for this region
     * and all surrounding regions.
     */
    @Override
    public List<Fixup> addFeatures(Rectangle area, Rectangle exportedArea, MinecraftWorld minecraftWorld) {
        // Create a local seed to ensure the results are deterministic, yet vary by world seed and by region
        final long seed = (dimension.getSeed() << 8) ^ ((long) exportedArea.x << 4) ^ exportedArea.y;
        for (int x = area.x; x < (area.x + area.width); x++) {
            for (int y = area.y; y < (area.y + area.height); y++) {
                final int layerValue = dimension.getLayerValueAt(DemoLayer.INSTANCE, x, y);
                // TODO: modify the MinecraftWorld as required according to the layer value
            }
        }
        return null; // Or return Fixups for creating structures that cross region borders, if that is hard to do in one go
    }

    // IncidentalLayerExporter

    /**
     * Apply the layer to one specific block.
     *
     * <p>This is currently only used to apply layers to Custom Cave Layer floors. If support for that is not necessary
     * this can be left out.
     *
     * <p>Note that the operation must be deterministic. I.e. it must produce the same results for the same world seed
     * and coordinates.
     *
     * @param location       The 3D block coordinates (in the WorldPainter coordinate system, i.e. z is up) where the
     *                       layer should be applied.
     * @param intensity      The layer value or "intensity" with which the layer should be applied.
     * @param exportedArea   The area that will actually be saved to disk. At the time of this writing this always
     *                       corresponds to one Minecraft region (512x512 area).
     * @param minecraftWorld The region that is currently being created. Apply your changes to this object.
     * @return An optional {@link Fixup} to apply in a later stage. This can be used e.g. to create structures that
     * straddle region borders. The {@code Fixup} will be invoked once the second pass is complete for this region and
     * all surrounding regions.
     */
    @Override
    public Fixup apply(Point3i location, int intensity, Rectangle exportedArea, MinecraftWorld minecraftWorld) {
        // Create a local seed to ensure the results are deterministic, yet vary by world seed and by location
        final long seed = (dimension.getSeed() << 12) ^ ((long) location.x << 8) ^ ((long) location.y << 4) ^ location.z;
        // TODO: modify the MinecraftWorld as required according to the intensity
        return null; // Or return a Fixup for creating structures that cross region borders, if that is hard to do in
        // one go
    }
}