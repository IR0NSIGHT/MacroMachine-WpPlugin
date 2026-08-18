package org.ironsight.wpplugin.macromachine.Layers.CityBuilder;

import org.ironsight.wpplugin.macromachine.Layers.PathBuilder.Point2i;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IntegerTile;
import org.pepsoft.worldpainter.layers.renderers.NibbleLayerRenderer;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;

import static org.pepsoft.worldpainter.Constants.TILE_SIZE;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE_BITS;

public class CityLayerRenderer implements NibbleLayerRenderer
{

    private HashMap<Point2i, IntegerTile> tiles = new HashMap<>();
    private Rectangle2D selectedBBX = new Rectangle(0, 0, 0, 0);
    private int baseColor = 0x00FF00;
    private boolean isSelectedPaint = false;
    private boolean useHighlightColors = false;
    private final CityLayer layer;
    public CityLayerRenderer(CityLayer layer) {
        this.layer = layer;
    }
    public void setIsSelectedPaint(boolean isSelectedPaint) {
        this.isSelectedPaint = isSelectedPaint;
    }
    public void setUseHighlightColors(boolean useHighlightColors) {
        this.useHighlightColors = useHighlightColors;
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
        return tiles.containsKey(new Point2i(tileX, tileY));
    }

    private boolean tileRequiredRepaint(int tileX, int tileY) {
        long lastPaint = tileLastRepaint.getOrDefault(new Point2i(tileX, tileY), 0L);
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

        int resultColor;
        if (useHighlightColors) {
            if (value == 0) {
                return underlyingColour;
            }
            float strength = value / 15.0f;
            int baseR = (baseColor >> 16) & 0xFF;
            int baseG = (baseColor >> 8) & 0xFF;
            int baseB = baseColor & 0xFF;
            int underR = (underlyingColour >> 16) & 0xFF;
            int underG = (underlyingColour >> 8) & 0xFF;
            int underB = underlyingColour & 0xFF;
            int r = (int) (underR * (1 - strength) + baseR * strength);
            int g = (int) (underG * (1 - strength) + baseG * strength);
            int b = (int) (underB * (1 - strength) + baseB * strength);
            resultColor = (r << 16) | (g << 8) | b;
        } else {
            int tileX = x >> TILE_SIZE_BITS;
            int tileY = y >> TILE_SIZE_BITS;

            IntegerTile colorTile;
            if (!existsTile(tileX, tileY) || tileRequiredRepaint(tileX, tileY)) {
                IntegerTile tile = getExistingOrNewColorTileFor(tileX, tileY);
                layer.repaintColorTile(tileX, tileY, tile);
                tileLastRepaint.put(new Point2i(tileX, tileY), System.currentTimeMillis());
                colorTile = tile;
            } else {
                colorTile = getExistingOrNewColorTileFor(tileX, tileY);
            }
            resultColor = colorTile.getValueAt(x - tileX * TILE_SIZE, y - tileY * TILE_SIZE);
        }

        if (isSelectedPaint && selectedBBX.contains(x, y)) {
            int r = Math.min(255, ((resultColor >> 16) & 0xFF) + 0x8F);
            int g = Math.max(0, ((resultColor >> 8) & 0xFF) - 0x8F);
            int b = Math.max(0, (resultColor & 0xFF) - 0x8F);
            return (r << 16) | (g << 8) | b;
        }
        return resultColor;
    }

}
