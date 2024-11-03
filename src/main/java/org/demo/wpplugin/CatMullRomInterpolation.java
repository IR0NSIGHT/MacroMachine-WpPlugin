package org.demo.wpplugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Predicate;

import static org.demo.wpplugin.ArrayUtility.isValidArray;
import static org.demo.wpplugin.operations.ContinuousCurve.positionsToHandleOffsetCatmullRom;
import static org.demo.wpplugin.operations.River.RiverHandleInformation.INHERIT_VALUE;
import static org.demo.wpplugin.pathing.CubicBezierSpline.calcuateCubicBezier;
import static org.demo.wpplugin.pathing.CubicBezierSpline.estimateCurveSize;

public class CatMullRomInterpolation {
    public static float[] interpolateCatmullRom(float[] positions, int[] handleToCurveIdx, int[] segmentLengths) {
        if (positions.length < 2)
            return positions.clone();

        Predicate<float[]> arrayContainsValuesThatAreNotInheritVal = xs -> {
            ArrayList<Float> sorted = new ArrayList<>();
            for (float f : xs)
                sorted.add(f);
            sorted.removeIf(f -> f == INHERIT_VALUE);
            if (sorted.isEmpty())
                System.out.println("whoopsie :(");
            return !sorted.isEmpty();
        };

        final float[] handlesWithSomeValues = supplementFirstAndLastTwoHandles(positions, INHERIT_VALUE, 5);
        assert arrayContainsValuesThatAreNotInheritVal.test(handlesWithSomeValues) : "handles MUST have at least one " +
                "value to be interpolable" + Arrays.toString(handlesWithSomeValues);

        //FIXME use float lengths everywhere
        float[] segmentLengthsFloat = new float[segmentLengths.length];
        for (int i = 0; i < segmentLengths.length; i++)
            segmentLengthsFloat[i] = segmentLengths[i];
        HandleAndIdcs in = new HandleAndIdcs(handlesWithSomeValues, handleToCurveIdx.clone(), segmentLengthsFloat);
        HandleAndIdcs ready = HandleAndIdcs.removeInheritValues(in);

        float[] tangents = tangentsFromPositions(ready.positions, ready.segmentLengths);
        assert isValidArray(tangents) : "some illegal values are in this float array: "+ Arrays.toString(tangents);

        float[] interpolatedPositions = interpolateFromHandles(ready.positions, tangents, ready.idcs);
        assert isValidArray(interpolatedPositions) : "some illegal values are in this float array: "+ Arrays.toString(tangents);

        assert interpolatedPositions.length == 1 + handleToCurveIdx[handleToCurveIdx.length - 1] : "interpolated " +
                "values " +
                "array is not as long as the whole curve";
        return interpolatedPositions;
    }

    public static float[] tangentsFromPositions(float[] positions, float[] segmentLengths) {
        float[] tangents = positionsToHandleOffsetCatmullRom(positions);
        //TODO tangents should be twice as big?
        for (int i = 0; i < tangents.length; i++)   //scale offsets to tangents
            tangents[i] /= segmentLengths[i] / 2f;
        return tangents;
    }

    /**
     * will prepare handles array so that it can be interpolated with catmull rom
     * adds first two and last two handels if not present, based on existing handles
     * if no handles exist, uses default value
     *
     * @param inHandles   input flat list of handles. each idx = 1 handle
     * @param emptyMarker use this value to decide if a value in handle is "empty"
     * @return new array with set handles
     * @requires at least one handle value is set
     * @ensures first two and last two values are set, therefor all others can be interpolated, out.length = in.length
     */
    public static float[] supplementFirstAndLastTwoHandles(float[] inHandles, float emptyMarker, float defaultValue) {
        float[] outHandles = inHandles.clone();

        //count how many handles are not empty, remember first and last handle
        int setHandles = 0;
        float firstHandle = emptyMarker;
        float lastHandle = emptyMarker;
        for (float handle : outHandles) {
            if (handle != emptyMarker) {
                setHandles++;
                if (firstHandle == emptyMarker) firstHandle = handle;
                else lastHandle = handle;
            }
        }
        switch (setHandles) {
            case 0:
                lastHandle = firstHandle = defaultValue;
                break;
            case 1:
                lastHandle = firstHandle;
                break;
            case 2:
            default: {
                break;
            }
        }
        if (outHandles.length != 0)
            outHandles[0] = outHandles[0] == emptyMarker ? firstHandle : outHandles[0];

        if (outHandles.length > 1) {
            //we set the first two and last to values if they arent set already
            outHandles[1] = outHandles[1] == emptyMarker ? firstHandle : outHandles[1];
            int idx = outHandles.length - 1;
            outHandles[idx] = outHandles[idx] == emptyMarker ? lastHandle : outHandles[idx];
            idx = outHandles.length - 2;
            outHandles[idx] = outHandles[idx] == emptyMarker ? lastHandle : outHandles[idx];
        }

        assert outHandles.length == inHandles.length;
        return outHandles;
    }

    public static int[] estimateSegmentLengths(float[] xsPos, float[] ysPos, float[] xsHandlesOffset,
                                               float[] ysHandlesOffset) {
        ArrayList<Integer> segmentLengthsList = new ArrayList<>();
        for (int i = 0; i < xsPos.length - 1; i++) {
            float[] pos0, handle0, handle1, pos1;
            pos0 = new float[]{xsPos[i], ysPos[i]};
            handle0 = new float[]{pos0[0] + xsHandlesOffset[i], pos0[1] + ysHandlesOffset[i]};

            pos1 = new float[]{xsPos[i + 1], ysPos[i + 1]};
            handle1 = new float[]{pos1[0] - xsHandlesOffset[i + 1], pos1[1] - ysHandlesOffset[i + 1]};

            int sizeOfSegment = (int) Math.ceil(estimateCurveSize(
                    pos0, handle0, handle1, pos1,
                    2
            ));
            segmentLengthsList.add(sizeOfSegment);
        }
        segmentLengthsList.add(1);  //last point is single length segment
        return ArrayUtility.toIntArray(segmentLengthsList);
    }

    /**
     * will take a handle array and index information
     * will construct a interpolated curve matching both
     *
     * @param positions        flat list of handle values, each idx = 1 handle
     * @param curveIdxByHandle array where arr[handleIdx] = startIdx on curve, describes where each handle is on the
     *                         curve
     * @return interpolated curve that fills unknown positions between the handles using catmull rom
     * @requires handles must not contain INHERIT values
     */
    public static float[] interpolateFromHandles(float[] positions, float[] tangents, int[] curveIdxByHandle) {
        assert positions.length == curveIdxByHandle.length && tangents.length == positions.length : "both arrays" +
                " must be the same length as the represent the "
                + "same curveHandles";

        //    assert curveIdxByHandle[0] == 0 : "curveIdxByHandle must represent the complete curve.";
        if (!canBeInterpolated(positions)) {
            throw new IllegalArgumentException("handles are not interpolatable");
        }

        ArrayList<float[]> curveSegments = new ArrayList<>(positions.length);
        //interpolate segment by segment
        for (int i = 0; i < positions.length - 1; i++) {
            float[] segment = interpolateSegment(positions, tangents, curveIdxByHandle, i);
            curveSegments.add(segment);
        }
        //add last handle that was excluded in the last segment
        curveSegments.add(new float[]{positions[curveIdxByHandle.length - 1]});
        float[] outCurvePositions = ArrayUtility.flattenNestedList(curveSegments);
        for (int i = 0; i < curveIdxByHandle.length; i++) {
            int curveIdx = curveIdxByHandle[i];
            int handleIdx = i;
            assert Math.abs(outCurvePositions[curveIdx] - positions[handleIdx]) < 0.001f : "a handle position is not " +
                    "in the curve where it should be.";
        }
        return outCurvePositions;
    }

    public static boolean canBeInterpolated(float[] handles) {
        float[] copy = handles.clone();
        Arrays.sort(copy);
        int inheritIdx = Arrays.binarySearch(copy, INHERIT_VALUE);
        return handles.length >= 2 && inheritIdx < 0; //does not contain inherit value
    }

    /**
     * interpolates  a segment of the given handle list using catmull rom.
     * segment starts at flatHandles[i] .. flatHandles[i+1]
     *
     * @param positions     list of flat handle values. each idx = 1 handle
     * @param handleToCurve positions of handles on the curve
     * @param i             index of segment start. can be 0 to length-3
     * @return interpolated segment between handle[i+1] and handle[i+2]
     */
    public static float[] interpolateSegment(float[] positions, float[] tangents, int[] handleToCurve, int i) {
        assert i >= 0;
        assert i < positions.length - 1;
        assert positions.length == handleToCurve.length && positions.length == tangents.length : "all arrays must be " +
                "the same length ";

        //interpolate all unknown handles within the segment ranging from B to C
        int startIdx = handleToCurve[i];
        int endIdx = handleToCurve[i + 1];

        int length = endIdx - startIdx;
        float tangentMulti = length;
        float start, end, handle0, handle1;
        start = positions[i];
        end = positions[i + 1];
        handle0 = (tangents[i] * tangentMulti) / 3f + start;    //start point + 1/3 tangent in startpoint
        handle1 = end - (tangents[i + 1] * tangentMulti) / 3f;

        float[] interpolated = new float[length];

        //find all handles that are between b and c and are interpolated
        for (int j = 0; j < length; j++) {
            float t = j * 1f / (length);
            float interpolatedV = calcuateCubicBezier(start, handle0, handle1, end, t);
            interpolated[j] = interpolatedV;
        }
        return interpolated;
    }

}
