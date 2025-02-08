package org.ironsight.wpplugin.expandLayerTool.operations;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.panels.DefaultFilter;
import org.pepsoft.worldpainter.selection.SelectionBlock;
import org.pepsoft.worldpainter.selection.SelectionChunk;

import java.awt.*;
import java.util.Iterator;
import java.util.function.Consumer;

import static org.pepsoft.worldpainter.Constants.TILE_SIZE;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE_BITS;

public class ApplyAction {
    private final DefaultFilter filter;
    private final LayerMapping mapping;

    public ApplyAction(DefaultFilter filter, LayerMapping mapping) {
        this.filter = filter;
        this.mapping = mapping;
    }

    public static void applyToDimensionWithFilter(Dimension dim, DefaultFilter filter, Consumer<Point> applyOnBlock) {
        Iterator<? extends Tile> t = dim.getTiles().iterator();
        while (t.hasNext()) {
            Tile tile = t.next();

            {   // EARLY ABORT IF TILE DOESNT MEET MINIMAL CRITERIA
                if ((filter.isInSelection() && !tile.hasLayer(SelectionBlock.INSTANCE) &&
                        !tile.hasLayer(SelectionChunk.INSTANCE))) continue;

                if (filter.getOnlyOnLayer() != null && !tile.hasLayer(filter.getOnlyOnLayer())) continue;

                // if level above 100   vs tile max is 52 => 100 > 52 => reject
                if (filter.getAboveLevel() > tile.getHighestIntHeight()) continue;

                // if level below 17 vs tile lowest is 18 => 17 < 18 => reject
                if (filter.getBelowLevel() < tile.getLowestIntHeight()) continue;
            }

            Point p = new Point(0, 0);
            for (int xInTile = 0; xInTile < TILE_SIZE; xInTile++) {
                for (int yInTile = 0; yInTile < TILE_SIZE; yInTile++) {
                    final int x = xInTile + (tile.getX() << TILE_SIZE_BITS);
                    final int y = yInTile + (tile.getY() << TILE_SIZE_BITS);
                    p.x = x;
                    p.y = y;
                    if (0 != filter.modifyStrength(x, y, 1f)) applyOnBlock.accept(p);
                }
            }
        }
    }

    public void apply(Dimension dim) {
        assert mapping.getMappingPoints().length != 0;
        applyToDimensionWithFilter(dim, filter, pos -> {
            mapping.applyToPoint(dim, pos.x, pos.y);
        });
    }
}
