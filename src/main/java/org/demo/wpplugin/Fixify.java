package org.demo.wpplugin;

import com.aparapi.Kernel;
import org.demo.wpplugin.geometry.HeightDimension;

public class Fixify {
    public static void fixDim(HeightDimension dim, int startX, int startY, int width, int height) {
        final float[] heightMapArr = new float[width * height];
        final float[] fixedHeightMapArr = new float[heightMapArr.length];
        {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int posX = startX + x;
                    int posY = startY + y;
                    int idx = y * width + x;
                    float heightHere = dim.getHeight(posX, posY);

                    assert heightMapArr[idx] == 0; //never used before
                    heightMapArr[idx] = heightHere;

                    assert idx % width + startX == posX;
                    assert idx / width + startY == posY;
                }
            }
        }

        {
            Kernel kernel = new Kernel() {
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
                        fixedHeightMapArr[i] = (eastZ+southZ+westZ+northZ)/4f;
                    } else {
                        fixedHeightMapArr[i] = heightMapArr[i];
                    }
                }
            };

            kernel.execute(heightMapArr.length);
        }

        {
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    float heightHere = fixedHeightMapArr[y * width + x];
                    dim.setHeight(x + startX, y + startY, heightHere);
                }
            }
        }
    }
}
