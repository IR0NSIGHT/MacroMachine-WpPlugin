package org.ironsight.wpplugin.macromachine.Layers.CityBuilder;

import java.awt.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;

import static org.pepsoft.util.swing.TiledImageViewer.TILE_SIZE_BITS;

/**
 * STARMADE MOD CREATOR: Max1M DATE: 19.08.2025 TIME: 14:52
 */
class CityInfoDatabase implements Serializable {
    private HashMap<Point, HashMap<Point, Integer>> tileInformation = new HashMap<>();

    public boolean isEmpty() {
        return tileInformation.isEmpty();
    }
    public int getDataAt(int blockX, int blockY) {
        var tile = tileInformation.getOrDefault(new Point(blockX >> TILE_SIZE_BITS, blockY >> TILE_SIZE_BITS), null);
        if (tile == null)
            return -1;
        return tile.getOrDefault(new Point(blockX, blockY), -1);
    }

    public HashMap<Point, Integer> getTileData(int tileX, int tileY) {
        return tileInformation.get(new Point(tileX,tileY));
    }

    public void setDataAt(int blockX, int blockY, CityLayer.Direction rotation, boolean mirror, int schematicIdx) {
        Point tileId = new Point(blockX >> TILE_SIZE_BITS, blockY >> TILE_SIZE_BITS);
        var tile = tileInformation.getOrDefault(tileId, new HashMap<Point, Integer>());

        if (schematicIdx < 0) {
            tile.remove(new Point(blockX, blockY));
        } else {
            int data = rotation.ordinal() << CityLayer.ROTATION_BIT_SHIFT | (mirror ? 1 : 0) << CityLayer.MIRROR_BIT_SHIFT | schematicIdx << CityLayer.ID_BIT_SHIFT;
            tile.put(new Point(blockX, blockY), data);
        }

        if (tile.isEmpty()) {
            tileInformation.remove(tileId);
        } else {
            tileInformation.put(tileId, tile);
        }
    }

    @Serial
    private static final long serialVersionUID = 1L;
}
