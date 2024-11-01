package org.demo.wpplugin.operations;

import org.demo.wpplugin.operations.River.RiverHandleInformation;
import org.demo.wpplugin.pathing.PointInterpreter;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class ContinuousCurveTest {
    @Test
    void testContinuousCurve() {
        ArrayList<float[]> flat = new ArrayList<>();
        flat.add(new float[]{1, 1, 2, 2, 3, 3, 4, 4, 5, 5});  //simple staircase
        flat.add(new float[]{1, 2, 2, 3, 3, 4, 4, 5, 5, 6});

        ContinuousCurve c = new ContinuousCurve(flat, PointInterpreter.PointType.POSITION_2D);
        assertEquals(10, c.curveLength());
        assertEquals(new Point(2, 3), c.getPos(3));

        //infos that are not set return default values
        for (RiverHandleInformation.RiverInformation info : RiverHandleInformation.RiverInformation.values()) {
            assertEquals(Float.MIN_VALUE, c.getMax(info));
            assertEquals(Float.MAX_VALUE, c.getMin(info));
        }
    }
}