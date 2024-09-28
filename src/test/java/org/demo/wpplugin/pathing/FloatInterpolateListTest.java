package org.demo.wpplugin.pathing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;


class DoubleInterpolateListTest {

    @Test
    void setValue() {
        FloatInterpolateLinearList l = new FloatInterpolateLinearList();
        assertEquals(0, l.getCurveLength());

        l.setValue(9, 7f);
        assertEquals(1, l.amountHandles());
        assertEquals(10, l.getCurveLength());
        assertEquals(7f,l.getHandleValue(9));
        assertEquals(7f,l.getInterpolatedValue(9));

        l.setValue(25,17f);

        assertEquals(2, l.amountHandles());
        assertEquals(26, l.getCurveLength());
        assertEquals(7f,l.getHandleValue(9));
        assertEquals(17f,l.getHandleValue(25));
        assertEquals(17f,l.getInterpolatedValue(25));



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