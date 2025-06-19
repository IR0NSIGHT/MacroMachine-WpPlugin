package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import org.pepsoft.worldpainter.Constants;

import java.util.Arrays;

public class IntegerTile {
    public IntegerTile(int defaultValue) {
        Arrays.fill(values, defaultValue);
    }
    int[] values = new int[Constants.TILE_SIZE * Constants.TILE_SIZE];

    /**
     *
     * @param x global pos
     * @param y
     * @param value
     */
    void setValueAt(int x, int y, int value) {
        int index = (y % Constants.TILE_SIZE) * Constants.TILE_SIZE + (x % Constants.TILE_SIZE);
        values[index] = value;
    }

    /**
     *
     * @param x global pos
     * @param y
     * @return
     */
    int getValueAt(int x, int y) {
        int index = (y % Constants.TILE_SIZE) * Constants.TILE_SIZE + (x % Constants.TILE_SIZE);
        return values[index];
    }
}
