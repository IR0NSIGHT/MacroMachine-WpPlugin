package org.demo.wpplugin.pathing;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.demo.wpplugin.operations.River.RiverHandleInformation.INHERIT_VALUE;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FloatInterpolateListTest {

    void setValue() {
        FloatInterpolateList l = new FloatInterpolateList(10);
        assertEquals(10, l.getSize());

        float[] expected = new float[10];
        Arrays.fill(expected, INHERIT_VALUE);
        assertArrayEquals(expected, l.getInterpolatedList());
        l.setValue(9, 7);
        float[] interpolated = l.getInterpolatedList();
        assertArrayEquals(new float[]{7, 7, 7, 7, 7, 7, 7, 7, 7, 7}, interpolated);


        l.setValue(1, 1);
        interpolated = l.getInterpolatedList();
        assertArrayEquals(new float[]{1, 1, 1, 1, 1, 1, 1, 1, 1, 7}, interpolated);

        l.setValue(1, 9);
        expected = new float[]{9, 9, 9, 9, 9, 9, 9, 9, 9, 7};
        assertArrayEquals(expected, l.getInterpolatedList());

        l.setToInterpolate(1);
        expected = new float[]{7, 7, 7, 7, 7, 7, 7, 7, 7, 7};
        assertArrayEquals(expected, l.getInterpolatedList());
    }

    @Test
    void setToInterpolate() {
    }

    @Test
    void isInterpolate() {
    }

    @Test
    void isValidValue() {
    }

    @Test
    void updateRawValues() {
    }

    @Test
    void getInterpolatedList() {
    }
}