package org.demo.wpplugin.pathing;

import org.demo.wpplugin.ArrayUtility;
import org.demo.wpplugin.CatMullRomInterpolation;
import org.demo.wpplugin.HandleAndIdcs;
import org.demo.wpplugin.operations.ContinuousCurve;
import org.demo.wpplugin.operations.River.RiverHandleInformation;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;

import static org.demo.wpplugin.geometry.HeightDimension.getImmutableDimension62;
import static org.demo.wpplugin.operations.River.RiverHandleInformation.*;
import static org.demo.wpplugin.operations.River.RiverHandleInformation.RiverInformation.*;
import static org.demo.wpplugin.pathing.PointInterpreter.PointType.RIVER_2D;
import static org.demo.wpplugin.pathing.PointUtils.getPoint2D;
import static org.demo.wpplugin.pathing.PointUtils.setPosition2D;
import static org.junit.jupiter.api.Assertions.*;

class PathTest {
    @Test
    void clonePath() {
        Path p = Path.newFilledPath(4, RIVER_2D);
        Path clone = p.clone();
        assertEquals(p, clone);
        assertNotSame(p, clone);

        int idx = p.amountHandles() / 2;
        float[] someHandle = p.handleByIndex(idx).clone();
        p = p.setHandleByIdx(someHandle, idx);
        assertArrayEquals(p.handleByIndex(idx), someHandle);

        assertNotSame(p, clone);
        assertEquals(p, clone);

        float[] newHandle = someHandle.clone();
        newHandle = setValue(newHandle, RIVER_RADIUS, getValue(newHandle, RIVER_RADIUS) + 1);
        assertNotSame(newHandle, someHandle);
        p = p.setHandleByIdx(newHandle, idx);
        assertArrayEquals(p.handleByIndex(idx), newHandle);
        assertNotSame(p, clone);
        assertNotEquals(p, clone);

    }

    @Test
    void addPoint() {
        Path p = new Path(Collections.EMPTY_LIST, PointInterpreter.PointType.POSITION_2D);
        assertEquals(0, p.amountHandles());
        LinkedList<float[]> addedHandles = new LinkedList<>();
        for (int i = 0; i < 50; i++) {
            float[] newHandle = new float[]{3.9f * i, -2.7f * i};
            p = p.addPoint(newHandle.clone());
            addedHandles.add(newHandle);
        }
        assertEquals(50, p.amountHandles());
        for (int i = 0; i < 50; i++) {
            float[] iHandle = addedHandles.get(i);
            float[] path_iHandle = p.handleByIndex(i);
            assertArrayEquals(iHandle, path_iHandle);
        }
    }

    @Test
    void newEmpty() {
        int length = 50;
        Path path = newFilledPath(length);
        assertEquals(length, path.amountHandles());
        Path newEmpty = path.newEmpty();
        assertEquals(0, newEmpty.amountHandles());
        ContinuousCurve curve = ContinuousCurve.fromPath(newEmpty, getImmutableDimension62());
        assertEquals(0, curve.curveLength());
    }

    private Path newFilledPath(int length) {
        return Path.newFilledPath(length, PointInterpreter.PointType.POSITION_2D);
    }

    @Test
    void removePoint() {
        int length = 50;
        Path path = newFilledPath(length);
        assertEquals(length, path.amountHandles());

        LinkedList<float[]> addedHandles = new LinkedList<>();
        for (int i = 0; i < length; i++) {
            addedHandles.add(path.handleByIndex(i));
        }
        assertEquals(addedHandles.size(), path.amountHandles());
        for (int i = 0; i < path.amountHandles(); i++) {
            assertArrayEquals(addedHandles.get(i), path.handleByIndex(i));
        }

        while (!addedHandles.isEmpty()) {
            float[] toBeRemoved = addedHandles.remove(0);
            Path removed = path.removePoint(toBeRemoved);
            for (int i = 0; i < removed.amountHandles(); i++) {
                assertArrayEquals(addedHandles.get(i), removed.handleByIndex(i));
            }
            path = removed;
        }
    }

    @Test
    void movePoint() {
        int length = 50;
        Path path = newFilledPath(length);
        assertEquals(length, path.amountHandles());

        float[] oldHandle = path.handleByIndex(length / 2).clone();
        float[] newHandle = new float[]{123, 456};
        Path moved = path.overwriteHandle(oldHandle, newHandle);

        assertEquals(length, moved.amountHandles(), "new path has a different length after shifting a point");
        assertArrayEquals(newHandle, moved.handleByIndex(length / 2), "the shifted point is not where its supposed " +
                "to" + " be");
    }

    @Test
    void getPreviousPoint() throws IllegalAccessException {
        int length = 50;
        Path path = newFilledPath(length);
        assertEquals(length, path.amountHandles());

        float[] thisPoint = path.handleByIndex(length / 2).clone();
        float[] previousPoint = path.handleByIndex((length / 2) - 1).clone();
        float[] fromPath = path.getPreviousPoint(thisPoint);
        assertArrayEquals(previousPoint, fromPath);
    }

    @Test
    void insertPointAfter() {
        int length = 50;
        Path path = newFilledPath(length);
        assertEquals(length, path.amountHandles());

        float[] thisPoint = path.handleByIndex(length / 2).clone();
        float[] newPoint = new float[path.type.size];
        newPoint[0] = 12345f;
        newPoint[1] = -123456f;
        Path inserted = path.insertPointAfter(thisPoint, newPoint);
        assertEquals(length + 1, inserted.amountHandles());
        assertArrayEquals(newPoint, inserted.handleByIndex(length / 2 + 1), "inserted point not as expected");
        assertArrayEquals(thisPoint, inserted.handleByIndex(length / 2), "this point is not untouched");
    }

    @Test
    void handleByIndex() {
    }

    @Test
    void getClosestHandleTo() {
    }

    @Test
    void indexOf() {
        int length = 10;
        {   //most basic path, position only
            Path p = newFilledPath(length);
            assertEquals(length, p.amountHandles());

            float[] somePoint = p.handleByIndex(length / 2).clone();
            int idxFound = p.indexOfPosition(somePoint);
            assertEquals(length / 2, idxFound);
        }

        {  //make sure meta data is ignored for indexOf
            Path p = Path.newFilledPath(length, RIVER_2D);
            assertEquals(length, p.amountHandles());
            assertSame(p.type, RIVER_2D);

            float[] somePoint = p.handleByIndex(length / 2).clone();
            somePoint = setValue(somePoint, RIVER_RADIUS, 345f);
            assertEquals(345f, getValue(somePoint, RIVER_RADIUS));

            int idxFound = p.indexOfPosition(somePoint);
            assertEquals(length / 2, idxFound);
        }
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
            assertEquals(handlePos.x, curveP.getPosX(curvePointIdx), "this handle is not at the expected x position " +
                    "in the curve");
            assertEquals(37, curveP.getPosY(curvePointIdx), "this handle is not at the expected y position in the " +
                    "curve");

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
    void prepareEmptyHandlesForInterpolation() {
        float[] incompleteHandles;
        float[] completeHandles;
        float[] expectedHandles;

        //only one is set
        incompleteHandles = new float[]{INHERIT_VALUE, INHERIT_VALUE, 7, INHERIT_VALUE, INHERIT_VALUE, INHERIT_VALUE};
        completeHandles = CatMullRomInterpolation.supplementFirstAndLastTwoHandles(incompleteHandles, INHERIT_VALUE,
                156f);
        expectedHandles = new float[]{7, 7, 7, INHERIT_VALUE, 7, 7};
        assertArrayEquals(expectedHandles, completeHandles);

        //two are set
        incompleteHandles = new float[]{5, INHERIT_VALUE, INHERIT_VALUE, INHERIT_VALUE, 7};
        completeHandles = CatMullRomInterpolation.supplementFirstAndLastTwoHandles(incompleteHandles, INHERIT_VALUE,
                156f);
        expectedHandles = new float[]{5, 5, INHERIT_VALUE, 7, 7};
        assertArrayEquals(expectedHandles, completeHandles);

        //two are set but in wierd positions
        incompleteHandles = new float[]{INHERIT_VALUE, 5, 7, INHERIT_VALUE, INHERIT_VALUE};
        completeHandles = CatMullRomInterpolation.supplementFirstAndLastTwoHandles(incompleteHandles, INHERIT_VALUE,
                156f);
        expectedHandles = new float[]{5, 5, 7, 7, 7};
        assertArrayEquals(expectedHandles, completeHandles);

        //three or more are set but the first and last two are missing
        incompleteHandles = new float[]{INHERIT_VALUE, INHERIT_VALUE, 5, INHERIT_VALUE, 6, INHERIT_VALUE, 7,
                INHERIT_VALUE, 8, INHERIT_VALUE, 9, INHERIT_VALUE, 10, INHERIT_VALUE, INHERIT_VALUE};
        completeHandles = CatMullRomInterpolation.supplementFirstAndLastTwoHandles(incompleteHandles, INHERIT_VALUE,
                156f);
        expectedHandles = new float[]{5, 5, 5, INHERIT_VALUE, 6, INHERIT_VALUE, 7, INHERIT_VALUE, 8, INHERIT_VALUE, 9
                , INHERIT_VALUE, 10, 10, 10};
        assertArrayEquals(expectedHandles, completeHandles);


        {        //not a single value is known
            incompleteHandles = new float[]{INHERIT_VALUE, INHERIT_VALUE, INHERIT_VALUE, INHERIT_VALUE, INHERIT_VALUE
                    , INHERIT_VALUE};
            completeHandles = CatMullRomInterpolation.supplementFirstAndLastTwoHandles(incompleteHandles,
                    INHERIT_VALUE, 17.56f);
            expectedHandles = new float[]{17.56f, 17.56f, INHERIT_VALUE, INHERIT_VALUE, 17.56f, 17.56f};
            assertArrayEquals(expectedHandles, completeHandles);
        }

        {    //single set value
            incompleteHandles = new float[]{1, INHERIT_VALUE};
            completeHandles = CatMullRomInterpolation.supplementFirstAndLastTwoHandles(incompleteHandles,
                    INHERIT_VALUE, 17.56f);
            expectedHandles = new float[]{1, 1};
            assertArrayEquals(expectedHandles, completeHandles);
        }
        {    //no set value
            incompleteHandles = new float[]{INHERIT_VALUE};
            completeHandles = CatMullRomInterpolation.supplementFirstAndLastTwoHandles(incompleteHandles,
                    INHERIT_VALUE, 17.56f);
            expectedHandles = new float[]{17.56f};
            assertArrayEquals(expectedHandles, completeHandles, 0.0001f);
        }
    }

    @Test
    void interpolateHandles() {
        float[] incompleteHandles;
        float[] tangents;
        int[] curveByHandleIdx = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        float[] interpolatedCurve;
        float[] expectedCurve;

        //simplest flat
        incompleteHandles = new float[]{1, 1, 1, 1};
        tangents = new float[]{0, 0, 0, 0};
        curveByHandleIdx = new int[]{0, 3, 6, 10};
        assertTrue(CatMullRomInterpolation.canBeInterpolated(incompleteHandles));
        interpolatedCurve = CatMullRomInterpolation.interpolateFromHandles(incompleteHandles, tangents,
                curveByHandleIdx);
        expectedCurve = new float[]{1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};
        assertArrayEquals(expectedCurve, interpolatedCurve, 0.01f);

        //minimum length
        incompleteHandles = new float[]{10, 20};
        tangents = new float[]{1 / 3f, 1 / 3f};
        curveByHandleIdx = new int[]{0, 2};  //3 points: t=0, t=0.5, t=1
        assertTrue(CatMullRomInterpolation.canBeInterpolated(incompleteHandles));
        interpolatedCurve = CatMullRomInterpolation.interpolateFromHandles(incompleteHandles, tangents,
                curveByHandleIdx);
        expectedCurve = new float[]{10, 15f, 20};
        assertArrayEquals(expectedCurve, interpolatedCurve, 0.01f, Arrays.toString(incompleteHandles));

        //equally space means equally spaced points on curve
        incompleteHandles = new float[]{-10, 0, 10, 20, 30, 40};
        tangents = new float[]{1, 1, 1, 1, 1, 1};
        curveByHandleIdx = new int[]{0, 10, 20, 30, 40, 50};
        interpolatedCurve = CatMullRomInterpolation.interpolateFromHandles(incompleteHandles, tangents,
                curveByHandleIdx);
        int i = 0;
        for (int x = -10; x <= 40; x++) {
            assertEquals(x, interpolatedCurve[i++], 0.01f,
                    "idx =" + i + " arr = " + Arrays.toString(incompleteHandles));
        }
        assertEquals(51, interpolatedCurve.length);
    }

    @Test
    void transposeHandles() {
        int n = 2, m = 5;
        ArrayList<float[]> input = new ArrayList<>();
        input.add(new float[]{1, 2, 3, 4, 5});
        input.add(new float[]{6, 7, 8, 9, 10});

        assertEquals(n, input.size());
        assertEquals(m, input.get(0).length);


        ArrayList<float[]> transposed = ArrayUtility.transposeMatrix(input);
        assertEquals(m, transposed.size());
        assertEquals(n, transposed.get(0).length);

        for (int ni = 0; ni < n; ni++) {
            for (int mi = 0; mi < m; mi++) {
                assertEquals(input.get(ni)[mi], transposed.get(mi)[ni], 1e-6);
            }
        }


    }

    @Test
    void interpolateSegment() {
        float[] handles = new float[]{10, 20, 30, 40};
        float[] tangents = new float[]{1, 1, 1, 1};
        int[] handleToCurve = new int[]{0, 10, 20, 30};
        float[] interpolated = CatMullRomInterpolation.interpolateSegment(handles, tangents, handleToCurve, 1);
        float[] expected = new float[]{20, 21, 22, 23, 24, 25, 26, 27, 28, 29};
        assertArrayEquals(expected, interpolated, 0.01f);
    }

    @Test
    void removeInheritValues() {
        float[] handles = new float[]{3.0f, 3.0f, INHERIT_VALUE, INHERIT_VALUE, INHERIT_VALUE, INHERIT_VALUE,
                INHERIT_VALUE, INHERIT_VALUE, INHERIT_VALUE, INHERIT_VALUE, INHERIT_VALUE, 17.0f, 17.0f};
        float[] segmentLengths = new float[handles.length];
        Arrays.fill(segmentLengths, 5);
        int[] handleIdcs = ContinuousCurve.handleToCurve(segmentLengths);

        HandleAndIdcs struct = new HandleAndIdcs(handles, handleIdcs, segmentLengths);
        HandleAndIdcs pure = HandleAndIdcs.removeInheritValues(struct);
        assertArrayEquals(pure.positions, new float[]{3.0f, 3.0f, 17.0f, 17.0f}, 0.01f);
    }

    @Test
    void testSegmentLengthEstimation() {
        {   //flat line on x axis
            float[] xs = new float[]{10, 20, 30, 40, 50, 70};
            float[] ys = new float[]{10, 10, 10, 10, 10, 10};
            float[] xHandleOffset = new float[]{5, 5, 5, 5, 5, 5};
            float[] yHandleOffset = new float[]{0, 0, 0, 0, 0, 0};
            int[] segmentLengths = CatMullRomInterpolation.estimateSegmentLengths(xs, ys, xHandleOffset, yHandleOffset);
            assertEquals(xs.length, segmentLengths.length, "6 points make 6 segments");
            assertArrayEquals(new int[]{10, 10, 10, 10, 20, 1}, segmentLengths, Arrays.toString(segmentLengths));
        }
        {   //simple diagonal line on xy
            float[] xs = new float[]{10, 20, 40};
            float[] ys = new float[]{30, 40, 60};
            float[] yHandleOffset = new float[]{5, 10, 10};
            float[] xHandleOffset = new float[]{5, 10, 10};
            int[] segmentLengths = CatMullRomInterpolation.estimateSegmentLengths(xs, ys, xHandleOffset, yHandleOffset);
            assertEquals(3, segmentLengths.length, "2 points should make 2 segments");
            //ceil sqrt(2)*10, ceil sqrt(2)*20
            assertArrayEquals(new int[]{15, 29, 1}, segmentLengths, Arrays.toString(segmentLengths) + "diagonal jump " +
                    "with" +
                    " 10 " +
                    "distance");
        }
    }

    @Test
    void interpolateCatmullRom() {
        float[] xs = new float[]{10, 20, 30, 40, 50, 70};
        int[] curveIdcs = new int[]{0, 10, 20, 30, 40, 50};
        int[] segmentSizes = new int[]{10, 10, 10, 10, 10, 10};
        float[] curveXs = CatMullRomInterpolation.interpolateCatmullRom(xs, curveIdcs, segmentSizes);
        assertEquals(51, curveXs.length);
        for (int i = 0; i < curveIdcs.length; i++) {
            assertEquals(curveXs[curveIdcs[i]], xs[i], 1e-6, "handle points are missing from the curve or at wrong " +
                    "curve idx");
        }
    }

    @Test
    void flattenNestedList() {
        ArrayList<float[]> nest = new ArrayList<>(5);
        nest.add(new float[]{1, 2, 3, 4});
        nest.add(new float[]{5, 6, 7, 8, 9, 10});
        nest.add(new float[]{});
        nest.add(new float[]{11});

        float[] flat = ArrayUtility.flattenNestedList(nest);
        assertArrayEquals(new float[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11}, flat);
    }
}