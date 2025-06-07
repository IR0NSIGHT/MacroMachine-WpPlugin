package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import com.kenperlin.ImprovedNoise;
import org.junit.jupiter.api.Test;
import org.pepsoft.util.PerlinNoise;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class PerlinNoiseIOTest {

    @Test
    void PerlinGenerator() {
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
    void getValueAt() {
        float scale = 2000f;
        int amplitude = 100;
        int[] histogram = new int[amplitude + 1];
        PerlinNoiseIO io = new PerlinNoiseIO(scale, amplitude, 123456, 1);
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
    /*
    @Test
    void getValueAtBinary() {
        float scale = 250f;
        int amplitude = 10;
        int[] histogram = new int[amplitude+1];
        PerlinNoiseIO io = new PerlinNoiseIO(scale, amplitude, 123456,1);
        int total = 0;
        for (int x = -1000; x < 1000; x++) {
            for (int y = -1000; y <= 2000; y++) {
                int value = io.getValueAt(null, x, y);
                histogram[value]++;
                assertTrue(value <= amplitude, "value=" + value + " at" + Arrays.toString(new int[]{x, y}));
                assertTrue(0 <= value, "value=" + value + " at" + Arrays.toString(new int[]{x, y}));
                total++;
            }
        }
        int i = 0;
        //full range from zero to 255 is actually hit
        for (int hits: histogram) {
            float percent = hits/(float)total;
            int repetitions = (int)Math.ceil(percent*10)*5;
           // assertNotEquals(0,hits);
            System.out.println(i++ +":" + "#".repeat(repetitions));
        }

    }

     */
}