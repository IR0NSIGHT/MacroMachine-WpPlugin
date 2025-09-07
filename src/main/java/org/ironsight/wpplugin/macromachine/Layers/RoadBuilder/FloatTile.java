package org.ironsight.wpplugin.macromachine.Layers.RoadBuilder;


import javax.vecmath.Point3i;
import java.util.Arrays;

import static org.pepsoft.worldpainter.Constants.TILE_SIZE;

public class FloatTile {
    public int tilePosX;
    public int tilePosY;
    private float min, max;
    public FloatTile(int defaultValue) {
        fillWith(defaultValue);
    }

    public FloatTile(Point3i tilePos) {
        tilePosX = tilePos.x;
        tilePosY = tilePos.y;
        fillWith(0);
    }
    float[] values = new float[TILE_SIZE * TILE_SIZE];

    public void fillWith(float value) {
        Arrays.fill(values, value);
        max = value; min = value;
    }

    public float getMin() {
        return min;
    }

    public float getMax() {
        return max;
    }

    public void calculateMinMax(){
        float[] minMax = calculateMinMax(values);
        this.min = minMax[0];
        this.max = minMax[1];
    }

    private static float[] calculateMinMax(float[] values) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("Array must not be null or empty");
        }

        float min = Float.MAX_VALUE;
        float max = Float.NEGATIVE_INFINITY;

        for (float value : values) {
            if (value < min) {
                min = value;
            }
            if (value > max) {
                max = value;
            }
        }

        return new float[]{min, max};
    }

    /**
     *
     * @param x global pos
     * @param y
     * @param value
     */
    public void setValueAt(int x, int y, float value) {
        int index = (y % TILE_SIZE) * TILE_SIZE + (x % TILE_SIZE);
        values[index] = value;
    }

    /**
     *
     * @param x global pos
     * @param y
     * @return
     */
    public float getValueAt(int x, int y) {
        int index = (y % TILE_SIZE) * TILE_SIZE + (x % TILE_SIZE);
        return values[index];
    }
}
