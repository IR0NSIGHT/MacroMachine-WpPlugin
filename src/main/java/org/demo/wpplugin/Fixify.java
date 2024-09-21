package org.demo.wpplugin;

import com.aparapi.Kernel;
import org.demo.wpplugin.geometry.HeightDimension;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;


public class Fixify extends Kernel {

    final float[] heightMapArr;
    final float[] fixedHeightMapArr;
    final int width;
    final int height;

    public Fixify(float[] heightMapArr, float[] fixedHeightMapArr, int width, int height) {
        this.heightMapArr = heightMapArr;
        this.fixedHeightMapArr = fixedHeightMapArr;
        this.width = width;
        this.height = height;

    }

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

    public static void fixDim(HeightDimension dim, Rectangle area) {
        final float[] heightMapArr = new float[area.width * area.height];
        final float[] fixedHeightMapArr = new float[heightMapArr.length];
        fillHeightMapFromDimension(dim, area, heightMapArr);
        Kernel kernel = new Fixify(heightMapArr, fixedHeightMapArr, area.width, area.height);
        kernel.execute(heightMapArr.length);
        writeResultToDimension(dim, area.x, area.y, area.width, area.height, fixedHeightMapArr);
    }

    @Override
    public void run() {
        int i = getGlobalId();
        int y = (i / width);
        int x = (i % width);

        float selfZ = Math.round(heightMapArr[i]);
        float northZ = y < height - 1 ? Math.round(heightMapArr[(y + 1) * width + x]) : selfZ;
        float southZ = y > 0 ? Math.round(heightMapArr[(y - 1) * width + x]) : selfZ;
        float westZ = x > 0 ? Math.round(heightMapArr[y * width + (x - 1)]) : selfZ;
        float eastZ = x < width - 1 ? Math.round(heightMapArr[y * width + (x + 1)]) : selfZ;

        int exposed = 0;
        if (eastZ < selfZ)
            exposed++;
        if (westZ < selfZ)
            exposed++;
        if (southZ < selfZ)
            exposed++;
        if (northZ < selfZ)
            exposed++;

        assert fixedHeightMapArr[i] == 0;
        if (exposed > 2) {
            ArrayList<Float> list = new ArrayList<Float>(4);
            list.add(northZ);
            list.add(southZ);
            list.add(eastZ);
            list.add(westZ);
            list.add(selfZ);
            Collections.sort(list);
            fixedHeightMapArr[i] = list.get(2); //median
        } else {
            fixedHeightMapArr[i] = heightMapArr[i];
        }
        if (fixedHeightMapArr[i] != 7)
            System.out.println();
    }
}

