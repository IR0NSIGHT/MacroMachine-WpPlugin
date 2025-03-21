package org.ironsight.wpplugin.expandLayerTool.operations;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.operations.Filter;
import org.pepsoft.worldpainter.panels.DefaultFilter;
import org.pepsoft.worldpainter.selection.SelectionBlock;
import org.pepsoft.worldpainter.selection.SelectionChunk;

import java.awt.*;
import java.util.Iterator;
import java.util.function.Consumer;

import static org.pepsoft.worldpainter.Constants.TILE_SIZE;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE_BITS;

public class ApplyAction {

    public static void applyToDimensionWithFilter(Dimension dim, TileFilter filter, Consumer<Point> applyOnBlock) {
        Iterator<? extends Tile> t = dim.getTiles().iterator();
        while (t.hasNext()) {
            Tile tile = t.next();
            TileFilter.passType pass = filter.testTile(tile);
            if (pass == TileFilter.passType.NO_BLOCKS) continue;

            Point p = new Point(0, 0);
            for (int yInTile = 0; yInTile < TILE_SIZE; yInTile++) {
                for (int xInTile = 0; xInTile < TILE_SIZE; xInTile++) {
                    final int x = xInTile + (tile.getX() << TILE_SIZE_BITS);
                    final int y = yInTile + (tile.getY() << TILE_SIZE_BITS);
                    p.x = x;
                    p.y = y;
                    if (pass == TileFilter.passType.ALL_BLOCKS || 0 != filter.modifyStrength(x, y, 1f)) applyOnBlock.accept(p);
                }
            }
        }
    }
}
