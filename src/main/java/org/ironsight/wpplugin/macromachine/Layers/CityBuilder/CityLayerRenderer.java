package org.ironsight.wpplugin.macromachine.Layers.CityBuilder;

import org.ironsight.wpplugin.macromachine.Layers.PathBuilder.Point2i;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IntegerTile;
import org.pepsoft.worldpainter.layers.renderers.NibbleLayerRenderer;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;

import static org.pepsoft.worldpainter.Constants.TILE_SIZE;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE_BITS;

public class CityLayerRenderer implements NibbleLayerRenderer {

    private HashMap<Point2i, IntegerTile> tiles = new HashMap<>();
    private Rectangle2D selectedBBX = new Rectangle(0, 0, 0, 0);
    private int baseColor = 0x00FF00;
    private boolean isSelectedPaint = false;
    private final CityLayer layer;
    public CityLayerRenderer(CityLayer layer) {
        this.layer = layer;
    }
    public void setIsSelectedPaint(boolean isSelectedPaint) {
        this.isSelectedPaint = isSelectedPaint;
    }
    public IntegerTile getExistingOrNewColorTileFor(int tileX, int tileY) {
        var key = new Point2i(tileX, tileY);
        if (!tiles.containsKey(key)) {
            tiles.put(key, new IntegerTile(0));
        }
        return tiles.get(key);
    }

    public void setCurrentSelectBBX(Rectangle2D bbx) {
        selectedBBX = bbx;
    }

    public void setBaseColor(int rgbHex) {
        baseColor = rgbHex;
    }

    private boolean existsTile(int tileX, int tileY) {
        return tiles.containsKey(new Point2i(tileX,tileY));
    }

    private boolean tileRequiredRepaint(int tileX, int tileY) {
        long lastPaint = tileLastRepaint.getOrDefault(new Point2i(tileX,tileY), 0L);
        return layer.lastEditedAfter(lastPaint);
    }

    private HashMap<Point2i, Long> tileLastRepaint = new HashMap<>();

    /**
     * get color in int RGB format
     *
     * @param x
     * @param y
     * @param underlyingColour
     * @param value
     * @return
     */
    @Override
    public int getPixelColour(int x, int y, int underlyingColour, int value) {
        int baseColor = this.baseColor;
     //  if (value == 0) {
     //      return underlyingColour;
     //  }

        // check if a tile exists for this coord
        int tileX = x >> TILE_SIZE_BITS;
        int tileY = y >> TILE_SIZE_BITS;

        IntegerTile colorTile;
        if (!existsTile(tileX,tileY) || tileRequiredRepaint(tileX,tileY)) { // add new tile, paint immediatly.
            IntegerTile tile = getExistingOrNewColorTileFor(tileX,tileY);
            layer.repaintColorTile(tileX,tileY,tile);
            tileLastRepaint.put(new Point2i(tileX,tileY), System.currentTimeMillis());
            colorTile = tile;
        } else {
            colorTile = getExistingOrNewColorTileFor(tileX,tileY);
        }
        int colorHex = colorTile.getValueAt(x - tileX * TILE_SIZE, y - tileY * TILE_SIZE);
        if (isSelectedPaint && selectedBBX.contains(x, y)) {
            return colorHex | 0x8F0000; // red
        }
        return colorHex;
    }

}
