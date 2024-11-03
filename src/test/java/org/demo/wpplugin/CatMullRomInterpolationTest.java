package org.demo.wpplugin;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.demo.wpplugin.operations.River.RiverHandleInformation.INHERIT_VALUE;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatMullRomInterpolationTest {
    @Test
    void interpolateCatmullRom() {
        float[] xs = new float[]{10, 20, 30, 40, 50, 70};
        int[] curveIdcs = new int[]{0, 10, 20, 30, 40, 50};
        int[] segmentSizes = new int[]{10, 10, 10, 10, 10, 10};
        float[] curveXs = CatMullRomInterpolation.interpolateCatmullRom(xs, curveIdcs, segmentSizes);
        assertEquals(51, curveXs.length);
        for (int i = 0; i < curveIdcs.length; i++) {
            assertEquals(curveXs[curveIdcs[i]], xs[i], 1e-6, "handle points are missing from the curve or at wrong " + "curve idx");
        }
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
            assertArrayEquals(new int[]{15, 29, 1}, segmentLengths, Arrays.toString(segmentLengths) + "diagonal jump " + "with" + " 10 " + "distance");
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
    void interpolateSegment() {
        float[] handles = new float[]{10, 20, 30, 40};
        float[] tangents = new float[]{1, 1, 1, 1};
        int[] handleToCurve = new int[]{0, 10, 20, 30};
        float[] interpolated = CatMullRomInterpolation.interpolateSegment(handles, tangents, handleToCurve, 1);
        float[] expected = new float[]{20, 21, 22, 23, 24, 25, 26, 27, 28, 29};
        assertArrayEquals(expected, interpolated, 0.01f);
    }


    @Test
    void tangentsFromPositions() {
    }
}