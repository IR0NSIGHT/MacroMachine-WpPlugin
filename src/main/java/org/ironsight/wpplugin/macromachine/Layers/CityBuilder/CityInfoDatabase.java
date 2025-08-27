package org.ironsight.wpplugin.macromachine.Layers.CityBuilder;

import java.awt.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;

import static org.pepsoft.util.swing.TiledImageViewer.TILE_SIZE_BITS;

/**
 * STARMADE MOD CREATOR: Max1M DATE: 19.08.2025 TIME: 14:52
 */
class CityInfoDatabase implements Serializable {
    public static final int NO_DATA = Integer.MIN_VALUE;
    @Serial
    private static final long serialVersionUID = 1L;
    private HashMap<Point, HashMap<Point, Integer>> tileInformation = new HashMap<>();

    public boolean deleteAllWithValue(int index, int mask) {
        boolean deletedSome = false;
        for (var tileInfo: tileInformation.values()) {
            LinkedList<Point> toDelete = new LinkedList<>();
            for (var entry: tileInfo.entrySet()) {
                if ( (entry.getValue().intValue() & mask) == index) {
                    toDelete.add(entry.getKey());
                }
            }
            deletedSome = deletedSome || !toDelete.isEmpty();
            for (Point p : toDelete)
                tileInfo.remove(p);

        }
        return deletedSome;
    }

    public boolean isEmpty() {
        return tileInformation.isEmpty();
    }

    public int getDataAt(int blockX, int blockY) {
        var tile = tileInformation.getOrDefault(new Point(blockX >> TILE_SIZE_BITS, blockY >> TILE_SIZE_BITS), null);
        if (tile == null)
            return NO_DATA;
        return tile.getOrDefault(new Point(blockX, blockY), NO_DATA);
    }

    public HashMap<Point, Integer> getTileData(int tileX, int tileY) {
        return tileInformation.get(new Point(tileX, tileY));
    }

    public void setDataAt(int blockX, int blockY, int data) {
        Point tileId = new Point(blockX >> TILE_SIZE_BITS, blockY >> TILE_SIZE_BITS);
        var tile = tileInformation.getOrDefault(tileId, new HashMap<Point, Integer>());

        if (data == NO_DATA) {
            tile.remove(new Point(blockX, blockY));
        } else {
            tile.put(new Point(blockX, blockY), data);
        }

        if (tile.isEmpty()) {
            tileInformation.remove(tileId);
        } else {
            tileInformation.put(tileId, tile);
        }
    }
}
