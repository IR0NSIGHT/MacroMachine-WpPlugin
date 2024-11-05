package org.demo.wpplugin;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SubdivideTest {

    @Test
    void subdivide() {

        {   //just two points, interpolate in middle one or more times
            float[] xs = new float[]{1, 11};
            float[] ys = new float[]{5, 25};
            Subdivide div = new Subdivide() {
                @Override
                public float[] subdividePoints(float x1, float x2, float y1, float y2) {
                    return new float[]{(x1 + x2) / 2, (y1 + y2) / 2};
                }
            };
            {
                ArrayList<float[]> flats = Subdivide.subdivide(xs, ys, 0, 0, div);
                assertEquals(2, flats.size());
                assertArrayEquals(new float[]{1, 11}, flats.get(0), Arrays.toString(flats.get(0)));
                assertArrayEquals(new float[]{5, 25}, flats.get(1), Arrays.toString(flats.get(1)));
            }
            {
                ArrayList<float[]> flats = Subdivide.subdivide(xs, ys, 0, 1, div);
                assertEquals(2, flats.size());
                assertArrayEquals(new float[]{1, 6, 11}, flats.get(0), Arrays.toString(flats.get(0)));
                assertArrayEquals(new float[]{5, 15, 25}, flats.get(1), Arrays.toString(flats.get(1)));
            }
            {
                ArrayList<float[]> flats = Subdivide.subdivide(xs, ys, 0, 2, div);
                assertEquals(2, flats.size());
                assertArrayEquals(new float[]{1, 3.5f, 6, 8.5f, 11}, flats.get(0), Arrays.toString(flats.get(0)));
                assertArrayEquals(new float[]{5, 10, 15, 20, 25}, flats.get(1), Arrays.toString(flats.get(1)));
            }
        }

        {   //just two points, interpolate in middle one or more times
            float[] xs = new float[]{4, 5, 6, 1, 11, 7, 8, 9};
            float[] ys = new float[]{1, 2, 3, 5, 25, 4, 5, 6};
            Subdivide div = new Subdivide() {
                @Override
                public float[] subdividePoints(float x1, float x2, float y1, float y2) {
                    return new float[]{(x1 + x2) / 2, (y1 + y2) / 2};
                }
            };
            {
                ArrayList<float[]> flats = Subdivide.subdivide(xs, ys, 3, 0, div);
                assertEquals(2, flats.size());
                assertArrayEquals(xs, flats.get(0), Arrays.toString(flats.get(0)));
                assertArrayEquals(ys, flats.get(1), Arrays.toString(flats.get(1)));
            }
            {
                ArrayList<float[]> flats = Subdivide.subdivide(xs, ys, 3, 1, div);
                assertEquals(2, flats.size());
                assertArrayEquals(new float[]{4, 5, 6, 1, 6, 11, 7, 8, 9}, flats.get(0), Arrays.toString(flats.get(0)));
                assertArrayEquals(new float[]{1, 2, 3, 5, 15, 25, 4, 5, 6}, flats.get(1), Arrays.toString(flats.get(1)));
            }
            {
                ArrayList<float[]> flats = Subdivide.subdivide(xs, ys, 3, 2, div);
                assertEquals(2, flats.size());
                assertArrayEquals(new float[]{4, 5, 6, 1, 3.5f, 6, 8.5f, 11, 7, 8, 9}, flats.get(0), Arrays.toString(flats.get(0)));
                assertArrayEquals(new float[]{1, 2, 3, 5, 10, 15, 20, 25, 4, 5, 6}, flats.get(1), Arrays.toString(flats.get(1)));
            }
        }
    }
}