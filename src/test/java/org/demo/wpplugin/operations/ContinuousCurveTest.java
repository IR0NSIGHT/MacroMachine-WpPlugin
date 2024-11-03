package org.demo.wpplugin.operations;

import org.demo.wpplugin.ArrayUtility;
import org.demo.wpplugin.operations.River.RiverHandleInformation;
import org.demo.wpplugin.pathing.Path;
import org.demo.wpplugin.pathing.PointInterpreter;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.demo.wpplugin.geometry.HeightDimension.getImmutableDimension62;
import static org.demo.wpplugin.operations.River.RiverHandleInformation.*;
import static org.demo.wpplugin.operations.River.RiverHandleInformation.RiverInformation.*;
import static org.demo.wpplugin.pathing.PointInterpreter.PointType.RIVER_2D;
import static org.demo.wpplugin.pathing.PointUtils.getPoint2D;
import static org.demo.wpplugin.pathing.PointUtils.setPosition2D;
import static org.junit.jupiter.api.Assertions.*;

class ContinuousCurveTest {
    @Test
    void instantiateContinousCurve() {
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

    @Test
    void isConnectedCurve() {
        ArrayList<float[]> flat = new ArrayList<>();
        flat.add(new float[]{1, 1, 2, 2, 3, 3, 4, 4, 5, 5});  //simple staircase
        flat.add(new float[]{1, 2, 2, 3, 3, 4, 4, 5, 5, 6});

        ContinuousCurve c = new ContinuousCurve(flat, PointInterpreter.PointType.POSITION_2D);

        assertTrue(c.isConnectedCurve());
    }

    @Test
    void isNotConnectedCurve() {
        ArrayList<float[]> flat = new ArrayList<>();
        flat.add(new float[]{1, 2, 3, 4, 5,});  //simple staircase
        flat.add(new float[]{1, 2, 3, 4, 5,});

        ContinuousCurve c = new ContinuousCurve(flat, PointInterpreter.PointType.POSITION_2D);

        assertFalse(c.isConnectedCurve());
    }

    @Test
    void continuousCurve() {
        Path p = Path.newFilledPath(10 + 2 + 1, RIVER_2D);

        //set start river width values
        float startWidthRiver = 3;
        p = p.setHandleByIdx(setValue(p.handleByIndex(0), RIVER_RADIUS, startWidthRiver), 0);
        assertEquals(startWidthRiver, getValue(p.handleByIndex(0), RIVER_RADIUS));

        p = p.setHandleByIdx(setValue(p.handleByIndex(1), RIVER_RADIUS, startWidthRiver), 1);
        assertEquals(startWidthRiver, getValue(p.handleByIndex(1), RIVER_RADIUS));


        //set river final width values
        p = p.setHandleByIdx(setValue(p.handleByIndex(p.amountHandles() - 1), RIVER_RADIUS, 17), p.amountHandles() - 1);
        assertEquals(17, getValue(p.handleByIndex(p.amountHandles() - 1), RIVER_RADIUS));
        p = p.setHandleByIdx(setValue(p.handleByIndex(p.amountHandles() - 2), RIVER_RADIUS, 17), p.amountHandles() - 2);
        assertEquals(17, getValue(p.handleByIndex(p.amountHandles() - 2), RIVER_RADIUS));

        //set river radius to inherit on all but first two and last two values
        for (int i = 2; i < p.amountHandles() - 2; i++) {
            p = p.setHandleByIdx(setValue(p.handleByIndex(i), RIVER_RADIUS, INHERIT_VALUE), i);
            assertEquals(INHERIT_VALUE, getValue(p.handleByIndex(i), RIVER_RADIUS));
        }

        //path moves along x, with y = 37 for all points
        //set position for all points
        for (int i = 0; i < p.amountHandles(); i++) {
            float[] newHandle = setPosition2D(p.handleByIndex(i), new float[]{i * 10, 37});
            p = p.setHandleByIdx(newHandle, i);

            assertEquals(new Point(i * 10, 37), getPoint2D(p.handleByIndex(i)));
        }

        ContinuousCurve curve;
        ContinuousCurve curveP = ContinuousCurve.fromPath(p, getImmutableDimension62());
        //are all curvepoints y=37?
        for (int i = 0; i < curveP.curveLength(); i++) {
            assertEquals(37, curveP.getPosY(i), 0.01f, "this point on the curve is supposed to be at y=37");
        }

        //test if all handles (except zero and last handle) are at the right index
        ArrayList<float[]> flatHandles = new ArrayList<>();
        flatHandles = ArrayUtility.transposeMatrix(p.getHandles());
        int[] handleToCurve = ContinuousCurve.handleToCurve(flatHandles.get(0), flatHandles.get(1));


        for (int i = 0; i < p.amountHandles(); i++) {
            float[] handle = p.handleByIndex(i);
            Point handlePos = getPoint2D(handle);
            int curvePointIdx = handleToCurve[i];
            //iterate from first handle where curve starts to last handle where curve ends
            assertEquals(handlePos.x, curveP.getPosX(curvePointIdx), "this handle is not at the expected x position " + "in the curve");
            assertEquals(37, curveP.getPosY(curvePointIdx), "this handle is not at the expected y position in the " + "curve");

        }

        {
            //Test if interpolating of INHERT handles works for edge cases
            //allow only setting one or more values and propagate to the other handles
            {
                //only first value is set
                p = new Path(Collections.EMPTY_LIST, PointInterpreter.PointType.RIVER_2D);
                p = p.addPoint(RiverHandleInformation.riverInformation(10, 10, 5, 6, 7, 30, 10));
                p = p.addPoint(RiverHandleInformation.riverInformation(11, 10));

                p = p.addPoint(RiverHandleInformation.riverInformation(20, 30));
                p = p.addPoint(RiverHandleInformation.riverInformation(21, 30));

                curve = ContinuousCurve.fromPath(p, getImmutableDimension62());
                for (int i = 0; i < curve.curveLength(); i++) {

                    assertEquals(5, curve.getInfo(RIVER_RADIUS, i), 0.01f);
                    assertEquals(6, curve.getInfo(RIVER_DEPTH, i), 0.01f);
                    assertEquals(7, curve.getInfo(BEACH_RADIUS, i), 0.01f);
                    assertEquals(30, curve.getInfo(TRANSITION_RADIUS, i), 0.01f);
                }
            }

            {
                //only last value is set
                ArrayList<float[]> handles = new ArrayList<>();

                handles.add(RiverHandleInformation.riverInformation(10, 10));
                handles.add(RiverHandleInformation.riverInformation(11, 10));

                handles.add(RiverHandleInformation.riverInformation(20, 30));
                handles.add(RiverHandleInformation.riverInformation(21, 30, 5, 6, 7, 30, 10));

                p = new Path(handles, PointInterpreter.PointType.RIVER_2D);

                curve = ContinuousCurve.fromPath(p, getImmutableDimension62());
                for (int i = 0; i < curve.curveLength(); i++) {

                    assertEquals(5, curve.getInfo(RIVER_RADIUS, i), 0.01f);
                    assertEquals(6, curve.getInfo(RIVER_DEPTH, i), 0.01f);
                    assertEquals(7, curve.getInfo(BEACH_RADIUS, i), 0.01f);
                    assertEquals(30, curve.getInfo(TRANSITION_RADIUS, i), 0.01f);
                }
            }

            {
                //only one value anywhere is set
                ArrayList<float[]> handles = new ArrayList<>();

                handles.add(RiverHandleInformation.riverInformation(10, 10));
                handles.add(RiverHandleInformation.riverInformation(11, 10));
                handles.add(RiverHandleInformation.riverInformation(100, 200));
                handles.add(RiverHandleInformation.riverInformation(20, 30, 5, 6, 7, 30, 10));
                handles.add(RiverHandleInformation.riverInformation(21, 30));

                p = new Path(handles, PointInterpreter.PointType.RIVER_2D);

                curve = ContinuousCurve.fromPath(p, getImmutableDimension62());
                for (int i = 0; i < curve.curveLength(); i++) {

                    assertEquals(5, curve.getInfo(RIVER_RADIUS, i), 0.01f);
                    assertEquals(6, curve.getInfo(RIVER_DEPTH, i), 0.01f);
                    assertEquals(7, curve.getInfo(BEACH_RADIUS, i), 0.01f);
                    assertEquals(30, curve.getInfo(TRANSITION_RADIUS, i), 0.01f);
                }
            }
        }
    }

    @Test
    void positionsToHandleOffsetCatmullRom() {
        float[] xsPos = new float[]{10, 20, 30, 100};
        float[] xHandleOff = ContinuousCurve.positionsToHandleOffsetCatmullRom(xsPos);

        assertArrayEquals(new float[]{5, 5, 20, 35}, xHandleOff, "handles:" + Arrays.toString(xHandleOff));
    }
}