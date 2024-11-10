package org.ironsight.wpplugin.rivertool;

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
            ArrayList<float[]> handles = new ArrayList<>();
            handles.add(new float[]{1, 5});
            handles.add(new float[]{11, 25});
            Subdivide div = new Subdivide() {
                @Override
                public float[] subdividePoints(float[] a, float[] b) {
                    return new float[]{(a[0] + b[0]) / 2f, (a[1] + b[1]) / 2};
                }
            };
            {
                ArrayList<float[]> flats = Subdivide.subdivide(handles, 0,  div);
                assertEquals(2, flats.size());
                assertArrayEquals(new float[]{1, 5}, flats.get(0), Arrays.toString(flats.get(0)));
                assertArrayEquals(new float[]{11, 25}, flats.get(1), Arrays.toString(flats.get(1)));
            }
            {
                ArrayList<float[]> flats = Subdivide.subdivide(handles, 1, div);
                assertEquals(3, flats.size());
                assertArrayEquals(new float[]{1, 5}, flats.get(0), Arrays.toString(flats.get(0)));
                assertArrayEquals(new float[]{6, 15}, flats.get(1), Arrays.toString(flats.get(1)));
                assertArrayEquals(new float[]{11, 25}, flats.get(2), Arrays.toString(flats.get(2)));
            }
            {
                ArrayList<float[]> flats = Subdivide.subdivide(handles, 2, div);
                assertEquals(5, flats.size());
                assertArrayEquals(new float[]{1, 5}, flats.get(0), Arrays.toString(flats.get(0)));
                assertArrayEquals(new float[]{3.5f, 10}, flats.get(1), Arrays.toString(flats.get(1)));
                assertArrayEquals(new float[]{6, 15}, flats.get(2), Arrays.toString(flats.get(2)));
                assertArrayEquals(new float[]{8.5f, 20}, flats.get(3), Arrays.toString(flats.get(3)));
                assertArrayEquals(new float[]{11, 25}, flats.get(4), Arrays.toString(flats.get(4)));
            }
        }
    }
}