package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import org.pepsoft.worldpainter.Constants;

import java.util.Arrays;

public class IntegerTile {
    private int min, max;
    public IntegerTile(int defaultValue) {
        fillWith(defaultValue);
    }
    int[] values = new int[Constants.TILE_SIZE * Constants.TILE_SIZE];

    public void fillWith(int value) {
        Arrays.fill(values, value);
        max = value; min = value;
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    public void calculateMinMax(){
        int[] minMax = calculateMinMax(values);
        this.min = minMax[0];
        this.max = minMax[1];
    }

    private static int[] calculateMinMax(int[] values) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("Array must not be null or empty");
        }

        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;

        for (int value : values) {
            if (value < min) {
                min = value;
            }
            if (value > max) {
                max = value;
            }
        }

        return new int[]{min, max};
    }

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
