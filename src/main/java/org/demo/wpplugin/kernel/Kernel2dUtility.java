package org.demo.wpplugin.kernel;

import org.demo.wpplugin.geometry.HeightDimension;

import java.awt.*;

public class Kernel2dUtility {
    public static void fillHeightMapFromDimension(HeightDimension dim, Rectangle area,
                                                  float[] heightMapArr) {
        for (int y = 0; y < area.height; y++) {
            for (int x = 0; x < area.width; x++) {
                int posX = area.x + x;
                int posY = area.y + y;
                int idx = y * area.width + x;
                float heightHere = dim.getHeight(posX, posY);

                assert heightMapArr[idx] == 0; //never used before
                heightMapArr[idx] = heightHere;

                assert idx % area.width + area.x == posX;
                assert idx / area.width + area.y == posY;
            }
        }
    }

    public static void writeResultToDimension(HeightDimension dim, int startX, int startY, int width, int height,
                                              float[] fixedHeightMapArr) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                float heightHere = fixedHeightMapArr[y * width + x];
                dim.setHeight(x + startX, y + startY, heightHere);
            }
        }
    }
}
