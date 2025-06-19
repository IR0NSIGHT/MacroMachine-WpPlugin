package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import org.pepsoft.worldpainter.Constants;

import java.awt.*;

public class TileContainer {

    private final IntegerTile[][] tiles;
    private final int offsetX;
    private final int offsetY;
    private final int width, height;

    public TileContainer(int width, int height, int minX, int minY, int defaultValue) {
        tiles = new IntegerTile[width][];
        for (int j = 0; j < width; j++) {
            tiles[j] = new IntegerTile[height];
            for (int i = 0; i < height; i++)
                tiles[j][i] = new IntegerTile(defaultValue);
        }
        offsetX = -minX;
        offsetY = -minY;
        this.width = width;
        this.height= height;
    }

    public Rectangle getExtent() {
        return new Rectangle(-offsetX / Constants.TILE_SIZE, -offsetY / Constants.TILE_SIZE, width, height);
    }

    /**
     * @param x     global pos
     * @param y
     * @param value
     */
    void setValueAt(int x, int y, int value) {
        x += offsetX;
        y += offsetY;

        assert x >= 0;
        assert y >= 0;

        int indexX = x >> Constants.TILE_SIZE_BITS;
        int indexY = y >> Constants.TILE_SIZE_BITS;

        IntegerTile tile = this.tiles[indexX][indexY];
        tile.setValueAt(x, y, value);
    }

    /**
     * @param x global pos
     * @param y
     * @return
     */
    int getValueAt(int x, int y) {
        x += offsetX;
        y += offsetY;

        int indexX = x >> Constants.TILE_SIZE_BITS;
        int indexY = y >> Constants.TILE_SIZE_BITS;

        IntegerTile tile = this.tiles[indexX][indexY];
        return tile.getValueAt(x, y);
    }
}
