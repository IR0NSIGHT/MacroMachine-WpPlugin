package org.ironsight.wpplugin.rivertool.operations;

import org.ironsight.wpplugin.rivertool.ArrayUtility;
import org.ironsight.wpplugin.rivertool.geometry.HeightDimension;
import org.ironsight.wpplugin.rivertool.operations.River.RiverHandleInformation;
import org.ironsight.wpplugin.rivertool.pathing.Path;
import org.ironsight.wpplugin.rivertool.pathing.PointInterpreter;
import org.ironsight.wpplugin.rivertool.pathing.PointUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.ironsight.wpplugin.rivertool.pathing.PointInterpreter.PointType.RIVER_2D;
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
    void linearInterpolate() {
        float[] a = new float[]{1, 2, 3, 4};
        float[] b = new float[]{2, 4, 6, 8};

        assertArrayEquals(new float[]{1, 2, 3, 4}, ContinuousCurve.linearInterpolate(a, b, 0), 0.01f, "t zero returns" +
                " a");
        assertArrayEquals(new float[]{2, 4, 6, 8}, ContinuousCurve.linearInterpolate(a, b, 1), 0.01f, "t one returns " +
                "b");
        assertArrayEquals(new float[]{1.5f, 3, 4.5f, 6}, ContinuousCurve.linearInterpolate(a, b, 0.5f), 0.01f, "t 0.5" +
                " returns middle");

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
        p = p.setHandleByIdx(RiverHandleInformation.setValue(p.handleByIndex(0),
                RiverHandleInformation.RiverInformation.RIVER_RADIUS, startWidthRiver), 0);
        Assertions.assertEquals(startWidthRiver, RiverHandleInformation.getValue(p.handleByIndex(0),
                RiverHandleInformation.RiverInformation.RIVER_RADIUS));

        p = p.setHandleByIdx(RiverHandleInformation.setValue(p.handleByIndex(1),
                RiverHandleInformation.RiverInformation.RIVER_RADIUS, startWidthRiver), 1);
        Assertions.assertEquals(startWidthRiver, RiverHandleInformation.getValue(p.handleByIndex(1),
                RiverHandleInformation.RiverInformation.RIVER_RADIUS));


        //set river final width values
        p = p.setHandleByIdx(RiverHandleInformation.setValue(p.handleByIndex(p.amountHandles() - 1),
                RiverHandleInformation.RiverInformation.RIVER_RADIUS, 17), p.amountHandles() - 1);
        Assertions.assertEquals(17, RiverHandleInformation.getValue(p.handleByIndex(p.amountHandles() - 1),
                RiverHandleInformation.RiverInformation.RIVER_RADIUS));
        p = p.setHandleByIdx(RiverHandleInformation.setValue(p.handleByIndex(p.amountHandles() - 2),
                RiverHandleInformation.RiverInformation.RIVER_RADIUS, 17), p.amountHandles() - 2);
        Assertions.assertEquals(17, RiverHandleInformation.getValue(p.handleByIndex(p.amountHandles() - 2),
                RiverHandleInformation.RiverInformation.RIVER_RADIUS));

        //set river radius to inherit on all but first two and last two values
        for (int i = 2; i < p.amountHandles() - 2; i++) {
            p = p.setHandleByIdx(RiverHandleInformation.setValue(p.handleByIndex(i),
                    RiverHandleInformation.RiverInformation.RIVER_RADIUS, RiverHandleInformation.INHERIT_VALUE), i);
            Assertions.assertEquals(RiverHandleInformation.INHERIT_VALUE,
                    RiverHandleInformation.getValue(p.handleByIndex(i),
                            RiverHandleInformation.RiverInformation.RIVER_RADIUS));
        }

        //path moves along x, with y = 37 for all points
        //set position for all points
        for (int i = 0; i < p.amountHandles(); i++) {
            float[] newHandle = PointUtils.setPosition2D(p.handleByIndex(i), new float[]{i * 10, 37});
            p = p.setHandleByIdx(newHandle, i);

            Assertions.assertEquals(new Point(i * 10, 37), PointUtils.getPoint2D(p.handleByIndex(i)));
        }

        ContinuousCurve curve;
        ContinuousCurve curveP = ContinuousCurve.fromPath(p, HeightDimension.getImmutableDimension62());
        //are all curvepoints y=37?
        for (int i = 0; i < curveP.curveLength(); i++) {
            assertEquals(37, curveP.getPosY(i), 0.01f, "this point on the curve is supposed to be at y=37");
        }

        //test if all handles (except zero and last handle) are at the right index
        ArrayList<float[]> flatHandles = new ArrayList<>();
        flatHandles = ArrayUtility.transposeMatrix(p.getHandles());
        int[] handleToCurve = ContinuousCurve.handleToCurve(flatHandles.get(0), flatHandles.get(1));

        /* //FIXME
        for (int i = 0; i < p.amountHandles(); i++) {
            float[] handle = p.handleByIndex(i);
            Point handlePos = PointUtils.getPoint2D(handle);
            int curvePointIdx = handleToCurve[i];
            //iterate from first handle where curve starts to last handle where curve ends
            assertEquals(handlePos.x, curveP.getPosX(curvePointIdx),
                    "this handle is not at the expected x position " + "in the curve");
            assertEquals(37, curveP.getPosY(curvePointIdx),
                    "this handle is not at the expected y position in the " + "curve");

        }
        */

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

                curve = ContinuousCurve.fromPath(p, HeightDimension.getImmutableDimension62());
                for (int i = 0; i < curve.curveLength(); i++) {

                    assertEquals(5, curve.getInfo(RiverHandleInformation.RiverInformation.RIVER_RADIUS, i), 0.01f);
                    assertEquals(6, curve.getInfo(RiverHandleInformation.RiverInformation.RIVER_DEPTH, i), 0.01f);
                    assertEquals(7, curve.getInfo(RiverHandleInformation.RiverInformation.BEACH_RADIUS, i), 0.01f);
                    assertEquals(30, curve.getInfo(RiverHandleInformation.RiverInformation.TRANSITION_RADIUS, i),
                            0.01f);
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

                curve = ContinuousCurve.fromPath(p, HeightDimension.getImmutableDimension62());
                for (int i = 0; i < curve.curveLength(); i++) {

                    assertEquals(5, curve.getInfo(RiverHandleInformation.RiverInformation.RIVER_RADIUS, i), 0.01f);
                    assertEquals(6, curve.getInfo(RiverHandleInformation.RiverInformation.RIVER_DEPTH, i), 0.01f);
                    assertEquals(7, curve.getInfo(RiverHandleInformation.RiverInformation.BEACH_RADIUS, i), 0.01f);
                    assertEquals(30, curve.getInfo(RiverHandleInformation.RiverInformation.TRANSITION_RADIUS, i),
                            0.01f);
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

                curve = ContinuousCurve.fromPath(p, HeightDimension.getImmutableDimension62());
                for (int i = 0; i < curve.curveLength(); i++) {

                    assertEquals(5, curve.getInfo(RiverHandleInformation.RiverInformation.RIVER_RADIUS, i), 0.01f);
                    assertEquals(6, curve.getInfo(RiverHandleInformation.RiverInformation.RIVER_DEPTH, i), 0.01f);
                    assertEquals(7, curve.getInfo(RiverHandleInformation.RiverInformation.BEACH_RADIUS, i), 0.01f);
                    assertEquals(30, curve.getInfo(RiverHandleInformation.RiverInformation.TRANSITION_RADIUS, i),
                            0.01f);
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

    @Test
    void makeContinuous() {
        {
            ArrayList<float[]> flatHandles = new ArrayList<>();
            //x positions
            flatHandles.add(new float[]{6, 10});
            //y positions
            flatHandles.add(new float[]{1, 2});
            flatHandles.add(new float[]{0, 10});

            ArrayList<float[]> flatHandlesConnected = ContinuousCurve.makeContinuous(flatHandles);
            assertArrayEquals(new float[]{6, 7, 7, 8, 9, 10}, flatHandlesConnected.get(0), 0.01f);
            assertArrayEquals(new float[]{1, 1f, 2f, 2f, 2, 2}, flatHandlesConnected.get(1), 0.01f);
            assertArrayEquals(new float[]{0, 2.5f, 3.75f, 5, 7.5f, 10}, flatHandlesConnected.get(1), 0.01f);
        }
    }

    @Test
    void roundHandles() {
        {
            ArrayList<float[]> handles = new ArrayList<>();
            handles.add(new float[]{0, 0, 15});
            handles.add(new float[]{0, 1, 16});
            handles.add(new float[]{1, 1, 17});
            handles.add(new float[]{2, 1, 18});
            handles.add(new float[]{2, 2, 19});

            ArrayList<float[]> rounded = ArrayUtility.transposeMatrix(ContinuousCurve.roundHandles(handles));
            assertArrayEquals(new float[]{0, 0, 1, 2, 2}, rounded.get(0));
            assertArrayEquals(new float[]{0, 1, 1, 1, 2}, rounded.get(1));
            assertArrayEquals(new float[]{15, 16, 17, 18, 19}, rounded.get(2));
        }

        {
            ArrayList<float[]> handles = new ArrayList<>();
            handles.add(new float[]{0.1f, 0, 15});
            handles.add(new float[]{0, 0.3f, 16});

            handles.add(new float[]{0, 1, 16});
            handles.add(new float[]{1.2f, 1.1f, 17});
            handles.add(new float[]{1.2f, 1.1f, 16});

            handles.add(new float[]{2, 1, 18});
            handles.add(new float[]{2, 2, 19});

            ArrayList<float[]> rounded = ArrayUtility.transposeMatrix(ContinuousCurve.roundHandles(handles));
            assertArrayEquals(new float[]{0, 0, 1, 2, 2}, rounded.get(0));
            assertArrayEquals(new float[]{0, 1, 1, 1, 2}, rounded.get(1));
            assertArrayEquals(new float[]{15, 16, 17, 18, 19}, rounded.get(2));
        }
    }

    @Test
    void connectDiagonals() {
        {
            ArrayList<float[]> handles = new ArrayList<>();
            handles.add(new float[]{0, 0, 15});
            handles.add(new float[]{1, 1, 17});
            handles.add(new float[]{2, 2, 19});

            ArrayList<float[]> rounded = ArrayUtility.transposeMatrix(ContinuousCurve.connectDiagonals(handles));
            assertArrayEquals(new float[]{0, 0, 1, 1, 2}, rounded.get(0));
            assertArrayEquals(new float[]{0, 1, 1, 2, 2}, rounded.get(1));
            assertArrayEquals(new float[]{15, 16, 17, 18, 19}, rounded.get(2));
        }

    }
}