package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import com.kenperlin.ImprovedNoise;
import org.junit.jupiter.api.Test;
import org.pepsoft.util.PerlinNoise;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class PerlinNoiseIOTest {

    @Test
    void PerlinGenerator() {
        // test for myself to gather understanding on what values the ImprovedNoise perlin generator produces
        float amplitude = 1;
        ImprovedNoise gen = new ImprovedNoise(1234567);
        //period is 256
        assertEquals(gen.noise(7, 0, 0), gen.noise(256 + 7, 0, 0));
        assertEquals(gen.noise(0, 0, 0), gen.noise(1, 0, 0));
        assertEquals(gen.noise(-1, 0, 0), gen.noise(1, 0, 0));

        for (int x = 0; x < 1000; x++) {
            double random = Math.random();
            assertEquals(gen.noise(random, 0, 0), gen.noise(256 + random, 0, 0), 0.00001);
        }

        float min = Float.MAX_VALUE;
        float max = Float.MIN_VALUE;
        for (float x = -2550; x < 2550; x += 11.11f) {
            for (float y = -2550; y <= 2560; y += 13.13f) {
                float value = (float) gen.noise(x, y, 0);
                max = Math.max(max, value);
                assertTrue(value <= 1, "value=" + value + " at" + Arrays.toString(new float[]{x, y}));
                min = Math.min(min, value);
                assertTrue(-1 <= value, "value=" + value + " at" + Arrays.toString(new float[]{x, y}));
            }
        }
    }

    @Test
    void serialize() {
        PerlinNoiseIO io = new PerlinNoiseIO(1, 2, 3, 4);
        Object[] data = io.getSaveData();
        IMappingValue instantiated = io.instantiateFrom(data);
        assertEquals(io, instantiated);
    }

    @Test
    void instantiateFromEditableValues() {
        PerlinNoiseIO io = new PerlinNoiseIO(1, 2, 3, 4);
        IMappingValue instantiated = io.instantiateWithValues(io.getEditableValues());
        assertEquals(io, instantiated);
    }

    @Test
    void getValueAt() {
        // unit test to ensure the perlin IO produces values across the whole intervall of [0, amplitude] and doesnt
        // just cluster around the median
        float scale = 27f;
        int amplitude = 12;
        int[] histogram = new int[amplitude + 1];
        PerlinNoiseIO io = new PerlinNoiseIO(scale, amplitude, 12134586, 5);

        for (int x = -1000; x < 1000; x++) {
            for (int y = -1000; y <= 2000; y++) {
                int value = io.getValueAt(null, x, y);
                histogram[value]++;
                assertTrue(value <= amplitude, "value=" + value + " at" + Arrays.toString(new int[]{x, y}));
                assertTrue(0 <= value, "value=" + value + " at" + Arrays.toString(new int[]{x, y}));
            }
        }
        //full range from zero to 255 is actually hit
        for (int hits : histogram) {
            assertNotEquals(0, hits);
        }
    }

    @Test
    void userSetOctaves() {
        PerlinNoiseIO io = new PerlinNoiseIO(12, 13, 14, 3);
        int[] values = io.getEditableValues();
        assertArrayEquals(new int[]{12, 13, 3, 14}, values);
        PerlinNoiseIO newIo = io.instantiateWithValues(new int[]{12, 13, 7, 14});
        assertArrayEquals(new int[]{12, 13, 7, 14}, newIo.getEditableValues());
        assertNotEquals(io,newIo);
    }


}