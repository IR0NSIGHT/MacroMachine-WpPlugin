package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import org.pepsoft.worldpainter.Constants;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Tile;

import java.awt.*;

import static org.pepsoft.worldpainter.Constants.TILE_SIZE;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE_BITS;

public class TileContainer {

    private final IntegerTile[][] tiles;
    private final int offsetX;
    private final int offsetY;
    private final int width, height;

    public TileContainer(Rectangle extent, int defaultValue) {
        this(extent.width , extent.height, TILE_SIZE * extent.x, TILE_SIZE * extent.y,defaultValue);
    }

    public void addAsValues(IPositionTileValueGetter getter, Dimension dim, boolean invert) {
        dim.getTiles().parallelStream().forEach(t -> {
            int yOff = t.getY() * TILE_SIZE;
            int xOff = t.getX() * TILE_SIZE;
            if (!existsTile(t.getX(),t.getY()))
                return;
            IntegerTile intTile = getTileAt(xOff,yOff);
            for (int yPos = 0; yPos < TILE_SIZE; yPos ++) {
                for (int xPos = 0; xPos < TILE_SIZE; xPos ++) {
                    int value = getter.getValueAt(t, xPos, yPos);
                    if (invert)
                        value = getter.getMaxValue() - value;
                    intTile.setValueAt(  offsetX + xPos + xOff,yPos + yOff + offsetY, value);
                }
            }
        });
    }

    /**
     * get row at position y as array
     * @param yPos
     * @return
     */
    public int[] getValueRow(int yPos) {
        int[] row = new int[getMaxXPos()-getMinXPos()];
        int start = getMinXPos();
        for (int x = start; x < getMaxXPos(); x++) {
            row[x - start] = getValueAt(x,yPos);
        }
        return row;
    }

    public void setValueRow(int yPos, int[] row) {
        int start = getMinXPos();
        for (int x = start; x < getMaxXPos(); x++) {
            setValueAt(x,yPos,row[x-start]);
        }
    }

    public void fillTile(int tileX, int tileY, int value) {
        getTileAt(tileX << TILE_SIZE_BITS, tileY << TILE_SIZE_BITS).fillWith(value);
    }

    /**
     * get column at position x as array
     * @param xPos
     * @return
     */
    public int[] getValueColumn(int xPos){
        int[] row = new int[getMaxYPos()-getMinYPos()];
        int start = getMinYPos();
        for (int y = start; y < getMaxYPos(); y++) {
            row[y - start] = getValueAt(xPos,y);
        }
        return row;
    }

    public void setValueColumn(int[] column, int xPos) {
        int start = getMinYPos();
        for (int y = start; y < getMaxYPos(); y++) {
            setValueAt(xPos,y,column[y - start]);
        }
    }

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
        this.height = height;
        assert invariant();
    }

    private boolean invariant() {
        for (int j = 0; j < width; j++) {
            for (int i = 0; i < height; i++)
                if (tiles[j][i] == null)
                    return false;
        }
        return true;
    }

    public void fillWithValue(int value) {
        assert invariant();
        for (int j = 0; j < width; j++) {
            for (int i = 0; i < height; i++)
                tiles[j][i].fillWith(value);
        }
        assert invariant();
    }

    public boolean existsTile(int tileX, int tileY) {
        int x = tileX * TILE_SIZE;
        int y = tileY * TILE_SIZE;
        x += offsetX;
        y += offsetY;

        int indexX = x >> Constants.TILE_SIZE_BITS;
        int indexY = y >> Constants.TILE_SIZE_BITS;
        if (indexX < 0 || indexY < 0)
            return false;
        if (indexX >= tiles.length)
            return false;
        if (indexY >= tiles[indexX].length)
            return false;
        if (tiles[indexX][indexY] == null)
            return false;
        return true;
    }

    public int getMinXPos() {
        return -offsetX;
    }

    public int getMaxXPos() {
        return -offsetX + width * TILE_SIZE;
    }

    public int getMaxYPos() {
        return -offsetY + height * TILE_SIZE;
    }

    public int getMinYPos() {
        return -offsetY;
    }


    private IntegerTile getTileAt(int pointX, int pointY) {
        pointX += offsetX;
        pointY += offsetY;

        assert pointX >= 0;
        assert pointY >= 0;

        int indexX = pointX >> Constants.TILE_SIZE_BITS;
        int indexY = pointY >> Constants.TILE_SIZE_BITS;
        assert indexX < tiles.length : "" + indexX;
        assert indexY < tiles[indexX].length : "" + indexY;
        return tiles[indexX][indexY];
    }

    public int getMinValueAtTile(int tileX, int tileY) {
        return getTileAt(tileX << TILE_SIZE_BITS, tileY << TILE_SIZE_BITS).getMin();
    }

    public int getMaxValueAtTile(int tileX, int tileY) {
        IntegerTile tile = getTileAt(tileX << TILE_SIZE_BITS, tileY << TILE_SIZE_BITS);
        return tile.getMax();
    }

    public void calculateMinMax(int x, int y) {
        getTileAt(x, y).calculateMinMax();
    }

    public Rectangle getExtent() {
        return new Rectangle(-offsetX / TILE_SIZE, -offsetY / TILE_SIZE, width, height);
    }

    int getMaxAt(int x, int y) {
        return getTileAt(x, y).getMax();
    }

    int getMinAt(int x, int y) {
        return getTileAt(x, y).getMin();
    }


    /**
     * @param x     global pos
     * @param y
     * @param value
     */
    public void setValueAt(int x, int y, int value) {
        if (!existsTile(x >> TILE_SIZE_BITS, y >> TILE_SIZE_BITS))
            return;
        getTileAt(x, y).setValueAt(x + offsetX, y + offsetY, value);
    }

    /**
     * @param x global pos
     * @param y
     * @return
     */
    public int getValueAt(int x, int y) {
        if (!existsTile(x >> TILE_SIZE_BITS, y >> TILE_SIZE_BITS))
            return Integer.MAX_VALUE;
        return getTileAt(x, y).getValueAt(x + offsetX, y + offsetY);
    }
}
