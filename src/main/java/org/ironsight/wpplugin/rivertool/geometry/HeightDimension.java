package org.ironsight.wpplugin.rivertool.geometry;

import java.awt.*;
import java.util.HashMap;

public interface HeightDimension {
    static HeightDimension getImmutableDimension62() {
        return new HeightDimension() {

            @Override
            public float getHeight(int x, int y) {
                return 62;
            }

            @Override
            public void setHeight(int x, int y, float z) {

            }
        };
    }

    static HeightDimension getEmptyMutableDimension() {
        HashMap<Point, Float> heights = new HashMap<>();
        return new HeightDimension() {

            @Override
            public float getHeight(int x, int y) {
                return heights.getOrDefault(new Point(x, y), 0f);
            }

            @Override
            public void setHeight(int x, int y, float z) {
                heights.put(new Point(x, y), z);
            }
        };
    }

    float getHeight(int x, int y);

    void setHeight(int x, int y, float z);
}

