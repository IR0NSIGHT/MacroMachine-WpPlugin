package org.demo.wpplugin.pathing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;


class DoubleInterpolateListTest {

    @Test
    void setValue() {
        FloatInterpolateLinearList l = new FloatInterpolateLinearList();
        assertEquals(0, l.getCurveLength());
        assertArrayEquals(new int[]{}, l.handleIdcs());


        //add new handle
        l.setValue(9, 7f);
        assertEquals(1, l.amountHandles());
        assertEquals(10, l.getCurveLength());
        assertEquals(7f, l.getHandleValue(9));

        assertEquals(7f, l.getInterpolatedValue(9));
        assertArrayEquals(new int[]{9}, l.handleIdcs());

        //add new handle
        l.setValue(25, 17f);
        assertEquals(2, l.amountHandles());
        assertEquals(26, l.getCurveLength());
        assertEquals(7f, l.getHandleValue(9));
        assertEquals(17f, l.getHandleValue(25));

        assertEquals(17f, l.getInterpolatedValue(25));
        assertArrayEquals(new int[]{9, 25}, l.handleIdcs());


        //add new handle
        l.setValue(0, 10f);
        assertEquals(3, l.amountHandles());
        assertEquals(26, l.getCurveLength());
        assertEquals(10f, l.getHandleValue(0));
        assertEquals(7f, l.getHandleValue(9));
        assertEquals(17f, l.getHandleValue(25));

        assertEquals(10f, l.getInterpolatedValue(0));

        assertArrayEquals(new int[]{0, 9, 25}, l.handleIdcs());


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