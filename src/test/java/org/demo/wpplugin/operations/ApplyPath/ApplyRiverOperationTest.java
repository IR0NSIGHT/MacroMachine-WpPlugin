package org.demo.wpplugin.operations.ApplyPath;

import org.demo.wpplugin.operations.River.RiverHandleInformation;
import org.demo.wpplugin.pathing.Path;
import org.demo.wpplugin.pathing.PointInterpreter;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;

import static org.demo.wpplugin.operations.River.RiverHandleInformation.RiverInformation.*;
import static org.demo.wpplugin.operations.River.RiverHandleInformation.getValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ApplyRiverOperationTest {

    @Test
    void applyRiverPath() {
        Path p = new Path(Collections.EMPTY_LIST, PointInterpreter.PointType.RIVER_2D);
        p = p.addPoint(RiverHandleInformation.riverInformation(10, 10, 5, 6, 7, 30));
        p = p.addPoint(RiverHandleInformation.riverInformation(11, 10));

        p = p.addPoint(RiverHandleInformation.riverInformation(20, 30));
        p = p.addPoint(RiverHandleInformation.riverInformation(21, 30));

        ArrayList<float[]> curve = p.continousCurve();
        for (float[] a : curve) {
            assertEquals(5, getValue(a, RIVER_RADIUS), 0.01f);
            assertEquals(6, getValue(a, RIVER_DEPTH),0.01f);
            assertEquals(7, getValue(a, BEACH_RADIUS),0.01f);
            assertEquals(30, getValue(a, TRANSITION_RADIUS),0.01f);
        }

    }
}