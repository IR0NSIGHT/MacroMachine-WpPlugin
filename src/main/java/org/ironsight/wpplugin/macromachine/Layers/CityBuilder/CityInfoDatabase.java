package org.ironsight.wpplugin.macromachine.Layers.CityBuilder;

import static org.pepsoft.util.swing.TiledImageViewer.TILE_SIZE_BITS;

import java.awt.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;

/** STARMADE MOD CREATOR: Max1M DATE: 19.08.2025 TIME: 14:52 */
/*
 * Undo: WorldPainter's undo manager copies the list for each undo step through
 * clone(). The interface adds nothing to the saved form (serialVersionUID and
 * the fields stay as released).
 */
public class CityInfoDatabase implements Serializable, org.pepsoft.util.undo.Cloneable<CityInfoDatabase>
{
    public static final int NO_DATA = Integer.MIN_VALUE;
    @Serial
    private static final long serialVersionUID = 1L;
    private HashMap<Point, HashMap<Point, Integer>> tileInformation = new HashMap<>();

    public boolean deleteAllWithValue(int index, int mask) {
        boolean deletedSome = false;
        for (var tileInfo : tileInformation.values()) {
            LinkedList<Point> toDelete = new LinkedList<>();
            for (var entry : tileInfo.entrySet()) {
                if ((entry.getValue().intValue() & mask) == index) {
                    toDelete.add(entry.getKey());
                }
            }
            deletedSome = deletedSome || !toDelete.isEmpty();
            for (Point p : toDelete)
                tileInfo.remove(p);
        }
        return deletedSome;
    }

    /** A deep copy: changing it leaves this list as it is (an undo step's copy). */
    @Override
    public CityInfoDatabase clone() {
        CityInfoDatabase copy = new CityInfoDatabase();
        for (var tile : tileInformation.entrySet()) {
            HashMap<Point, Integer> houses = new HashMap<>();
            for (var house : tile.getValue().entrySet())
                houses.put(new Point(house.getKey()), house.getValue());
            copy.tileInformation.put(new Point(tile.getKey()), houses);
        }
        return copy;
    }

    /** Forgets every placement. */
    public void clear() {
        tileInformation.clear();
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
        var data = tileInformation.get(new Point(tileX, tileY));
        return data != null ? new HashMap<>(data) : null;
    }

    HashMap<Point, Integer> getAllData() {
        HashMap<Point, Integer> data = new HashMap<>();
        for (HashMap<Point, Integer> tile : tileInformation.values()) {
            for (var entry : tile.entrySet())
                data.put(new Point(entry.getKey()), entry.getValue());
        }
        return data;
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
