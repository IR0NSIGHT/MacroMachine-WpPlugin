package org.ironsight.wpplugin.rivertool.operations;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GradientTest {

    @Test
    void getValue() {
        Gradient g = new Gradient(new float[]{0,1,2,3}, new float[]{0,1,2,3});
        assertEquals(0, g.getValue(0));
        assertEquals(1, g.getValue(1));
        assertEquals(2, g.getValue(2));
        assertEquals(3, g.getValue(3));

        assertEquals(1, g.getValue(0.75f));
        assertEquals(1, g.getValue(0.25f),"rounds up");

        assertEquals(0, g.getValue(-0.25f),"rounds up");

        assertEquals(2, g.getValue(1.1f),"rounds up");
        assertEquals(2, g.getValue(1.9f),"rounds up");

        assertEquals(3, g.getValue(3.5f),"rounds down if exceeds max value");

    }
}