package org.demo.wpplugin.kernel;

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

    /**
     * run median filter over this area. area has to be x wider in every direction if an x*x kernel is used
     * @param dim
     * @param area
     */
    public static void fixDim(HeightDimension dim, Rectangle area) {
        final float[] heightMapArr = new float[area.width * area.height];
        final float[] fixedHeightMapArr = new float[heightMapArr.length];
        Kernel2dUtility.fillHeightMapFromDimension(dim, area, heightMapArr);
        Kernel kernel = new Fixify(heightMapArr, fixedHeightMapArr, area.width, area.height);
        kernel.execute(heightMapArr.length);
        Kernel2dUtility.writeResultToDimension(dim, area.x, area.y, area.width, area.height, fixedHeightMapArr);
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

        assert fixedHeightMapArr[i] == 0;
        ArrayList<Float> list = new ArrayList<Float>(5);
        list.add(northZ);
        list.add(southZ);
        list.add(eastZ);
        list.add(westZ);
        list.add(selfZ);
        Collections.sort(list);
        fixedHeightMapArr[i] = list.get(2); //median
    }
}
