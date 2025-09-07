package org.ironsight.wpplugin.macromachine.Layers.RoadBuilder;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class CrossSectionShapeTest {

    @Test
    void getStrengthAt() {
        CrossSectionShape shape = new CrossSectionShape("test","my test") {
            @Override
            public float getStrengthAt(float t) {
                return 2*t;
            }
        };

        for (int i = 0; i < 100; i++) {
            float t = 1f*i/100;
            float expected = 2*t;
            assertEquals(expected,shape.getStrengthAt(t));
        }

    }

    @Test
    void asArray() {
        CrossSectionShape shape = new CrossSectionShape("test","my test") {
            @Override
            public float getStrengthAt(float t) {
                return 2*t;
            }
        };

        assertArrayEquals(new float[]{0,2},shape.asArray(2));
        assertArrayEquals(new float[]{0,0.25f,0.5f,0.75f,1,1.25f,1.5f,1.75f, 2},shape.asArray(9), Arrays.toString(shape.asArray(9)));
    }
}