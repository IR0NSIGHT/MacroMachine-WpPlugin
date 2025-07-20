package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import org.ironsight.wpplugin.macromachine.MacroSelectionLayer;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.layers.Layer;

import java.awt.Rectangle;

import static org.pepsoft.worldpainter.Constants.TILE_SIZE;


public class DebugTileContainer extends TileContainer{
    private final Dimension dimension;
    private final Layer layer;
    private final int[] valueMapping;
    public DebugTileContainer(Dimension dimension, Layer layer, int[] valueMapping, int width, int height, int minX,
                              int minY,
                              int defaultValue) {
        super(width, height, minX, minY, defaultValue);
        this.dimension = dimension;
         this.layer = layer;
         this.valueMapping = valueMapping;
    }

    @Override
    public void fillTile(int tileX, int tileY, int value) {
        super.fillTile(tileX, tileY, value);
        Tile tile = dimension.getTileForEditing(tileX,tileY);
        for (int yInTile = 0; yInTile < TILE_SIZE; yInTile++) {
            for (int xInTile = 0; xInTile < TILE_SIZE; xInTile++) {
                tile.setLayerValue(layer,xInTile,yInTile,valueMapping[value]);
            }
        }
    }

    @Override
    public void setValueAt(int x, int y, int value) {
        super.setValueAt(x, y, value);
        dimension.setLayerValueAt(layer,x,y,valueMapping[value]);
    }
}
