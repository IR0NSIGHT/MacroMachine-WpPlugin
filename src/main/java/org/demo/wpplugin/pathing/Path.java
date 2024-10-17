package org.demo.wpplugin.pathing;

import org.demo.wpplugin.geometry.HeightDimension;
import org.demo.wpplugin.operations.ContinuousCurve;
import org.demo.wpplugin.operations.River.RiverHandleInformation;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;

import static org.demo.wpplugin.operations.River.RiverHandleInformation.INHERIT_VALUE;
import static org.demo.wpplugin.operations.River.RiverHandleInformation.RiverInformation.WATER_Z;
import static org.demo.wpplugin.operations.River.RiverHandleInformation.validateRiver2D;
import static org.demo.wpplugin.pathing.CubicBezierSpline.calcuateCubicBezier;
import static org.demo.wpplugin.pathing.PointUtils.*;

public class Path implements Iterable<float[]> {
    public final PointInterpreter.PointType type;
    private final ArrayList<float[]> handles;

    public Path(List<float[]> handles, PointInterpreter.PointType type) {
        this.handles = new ArrayList<>(handles.size());
        for (float[] handle : handles) {
            this.handles.add(handle.clone());
        }
        this.type = type;
        assert invariant();
    }

    public static float[] interpolateWaterZ(ContinuousCurve curve, HeightDimension dim) {
        float[] out = new float[curve.curveLength()];
        Point first = curve.getPositions2d()[0];
        out[0] = Math.min(curve.getInfo(WATER_Z)[0], dim.getHeight(first.x, first.y));
        for (int i = 1; i < curve.curveLength(); i++) {
            float curveZ = curve.getInfo(WATER_Z)[i];
            float terrainZ = dim.getHeight(curve.getPositions2d()[i].x, curve.getPositions2d()[i].y);
            float previousZ = out[i - 1];

            out[i] = Math.min(Math.min(previousZ, terrainZ), curveZ);
        }

        return out;
    }

    public static boolean curveIsContinous(ArrayList<float[]> curve) {
        Point previous = null;
        float root2 = (float) Math.sqrt(2) + 0.001f; //allow delta
        for (Point p : point2DfromNVectorArr(curve)) {
            if (previous != null) {
                float dist = (float) p.distance(previous);
                if (p.equals(previous)) return false; //point has clone
                if (dist >= root2) {
                    return false; //point is not connected
                }
            }
            previous = p;
        }
        return true;
    }

    /**
     * every item in the list is an array of handles.
     * the first two items are interpreted as x and y coordinates
     *
     * @param handles     list of handles to interpolate curve from.
     * @param roundToGrid make sure each point on the curve only maps to one point on the grid
     * @return list of points building a continous curve
     */
    private static ArrayList<float[]> continousCurveFromHandles(ArrayList<float[]> handles, boolean roundToGrid) {
        LinkedList<float[]> curvePoints = new LinkedList<>();

        //iterate all handles, calculate coordinates on curve
        for (int i = 0; i < handles.size() - 3; i++) {
            float[] handleA, handleB, handleC, handleD;
            handleA = handles.get(i);
            handleB = handles.get(i + 1);
            handleC = handles.get(i + 2);
            handleD = handles.get(i + 3);
            float[][] curveSegment = CubicBezierSpline.getSplinePathFor(handleA, handleB, handleC, handleD,
                    RiverHandleInformation.PositionSize.SIZE_2_D.value);
            //curvepoints contain [x,y,t]
            curvePoints.addAll(Arrays.asList(curveSegment));
        }

        if (curvePoints.isEmpty()) return new ArrayList<>(0);

        int positionDigits = PointInterpreter.PointType.POSITION_2D.size;   //FIXME thats the wrong constant
        assert curvePoints.size() > 2;

        ArrayList<float[]> result = new ArrayList<>(curvePoints.size());
        float[] previousPoint = curvePoints.get(0);
        if (roundToGrid) result.add(previousPoint);
        for (float[] point : curvePoints) {
            assert point != null : "whats going on?";
            if (!roundToGrid) result.add(point);
            else if (!arePositionalsEqual(point, previousPoint, positionDigits)) {
                previousPoint = point;
                result.add(previousPoint);
                assert getPositionalDistance(point, previousPoint,
                        RiverHandleInformation.PositionSize.SIZE_2_D.value) <= Math.sqrt(2) + 0.01f : "distance " +
                        "between curvepoints is to large:" + getPositionalDistance(point, previousPoint,
                        RiverHandleInformation.PositionSize.SIZE_2_D.value);
            }

        }
        result.trimToSize();
        //assert curveIsContinous(result);
        ArrayList<Point> pointResult = point2DfromNVectorArr(result);
        for (int i = 1; i < handles.size() - 1; i++) {
            Point handlePoint = getPoint2D(handles.get(i));
            assert pointResult.contains(handlePoint) : "handle not in curve" + handlePoint;
        }
        return result;
    }

    /**
     * will prepare handles array so that it can be interpolated with catmull rom
     * adds first two and last two handels if not present, based on existing handles
     * if no handles exist, uses default value
     *
     * @param inHandles input flat list of handles. each idx = 1 handle
     * @param emptyMarker use this value to decide if a value in handle is "empty"
     * @return new array with set handles
     * @requires at least one handle value is set
     * @ensures first two and last two values are set, therefor all others can be interpolated, out.length = in.length
     */
    public static float[] supplementFirstAndLastTwoHandles(float[] inHandles, float emptyMarker, float defaultValue) {
        float[] outHandles = inHandles.clone();
        if (outHandles.length > 0 && outHandles.length < 4) {
            throw new IllegalArgumentException("zero or at least 4 handles are required for interpolation");
        }

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
        //we set the first two and last to values if they arent set already
        outHandles[0] = outHandles[0] == emptyMarker ? firstHandle : outHandles[0];
        outHandles[1] = outHandles[1] == emptyMarker ? firstHandle : outHandles[1];
        int idx = outHandles.length - 1;
        outHandles[idx] = outHandles[idx] == emptyMarker ? lastHandle : outHandles[idx];
        idx = outHandles.length - 2;
        outHandles[idx] = outHandles[idx] == emptyMarker ? lastHandle : outHandles[idx];

        assert outHandles.length == inHandles.length;
        return outHandles;
    }

    public static HandleAndIdcs removeInheritValues(float[] flatHandles, int[] handleToCurve) {
        if (flatHandles.length != handleToCurve.length) throw new IllegalArgumentException("must be same size");

        float[] pureFlatHandles = new float[flatHandles.length];
        int[] pureHandleToCurve = new int[flatHandles.length];
        int pureIdx = 0;
        for (int i = 0; i < flatHandles.length; i++) {
            if (flatHandles[i] != INHERIT_VALUE) {
                pureFlatHandles[pureIdx] = flatHandles[i];
                pureHandleToCurve[pureIdx] = handleToCurve[i];
                pureIdx++;
            }
        }
        pureHandleToCurve = Arrays.copyOf(pureHandleToCurve, pureIdx);
        pureFlatHandles = Arrays.copyOf(pureFlatHandles, pureIdx);

        //postcondition
        assert Arrays.binarySearch(pureFlatHandles, INHERIT_VALUE) < 0 : "array still contains INHERIT values";
        assert pureFlatHandles.length <= handleToCurve.length;

        return new HandleAndIdcs(pureFlatHandles, pureHandleToCurve);
    }

    /**
     * will take a handle array and index information
     * will construct a interpolated curve matching both
     *
     * @param handles flat list of handle values, each idx = 1 handle
     * @param curveIdxByHandle array where arr[handleIdx] = startIdx on curve, describes where each handle is on the curve
     * @return interpolated curve that fills unknown positions between the handles using catmull rom
     * @requires handles must not contain INHERIT values
     */
    public static float[] interpolateFromHandles(float[] handles, int[] curveIdxByHandle) {
        assert handles.length == curveIdxByHandle.length : "both arrays must be the same length as the represent the "
                + "same curveHandles";
        //    assert curveIdxByHandle[0] == 0 : "curveIdxByHandle must represent the complete curve.";
        assert curveIdxByHandle[1] == 0 : "first curve idx must be zero so the first index can be ignored";
        if (!canBeInterpolated(handles)) {
            throw new IllegalArgumentException("handles are not interpolatable");
        }

        int totalCurveLength = curveIdxByHandle[curveIdxByHandle.length - 2] + 1;   //last used handle index

        float[] outHandles = new float[totalCurveLength];
        Arrays.fill(outHandles, INHERIT_VALUE);
        for (int i = 0; i < handles.length - 3; i++) {
            float[] segment = interpolateSegment(handles, curveIdxByHandle, i);
            int segmentStartIdx = curveIdxByHandle[i + 1];
            for (int j = 0; j < segment.length; j++) {
                assert outHandles[segmentStartIdx + j] == INHERIT_VALUE;
                outHandles[segmentStartIdx + j] = segment[j];
                assert outHandles[segmentStartIdx + j] != INHERIT_VALUE;
                assert outHandles[curveIdxByHandle[i + 1]] == handles[i + 1];
            }
        }
        //copy last used handle to outarray
        outHandles[outHandles.length - 1] = handles[curveIdxByHandle.length - 2];

        return outHandles;
    }

    public static boolean canBeInterpolated(float[] handles) {
        float[] copy = handles.clone();
        Arrays.sort(copy);
        int inheritIdx = Arrays.binarySearch(copy, INHERIT_VALUE);
        return handles.length >= 4 && inheritIdx < 0; //does not contain inherit value
    }

    /**
     * will turn a n x m list into an m x n list
     * deep-clones inputs
     *
     * @param input matrix N x M
     * @return matrix M x N
     */
    public static ArrayList<float[]> transposeHandles(ArrayList<float[]> input) {
        int nLength = input.get(0).length;
        int mLenght = input.size();
        ArrayList<float[]> output = new ArrayList<>(nLength);

        for (int n = 0; n < nLength; n++) {
            float[] nThList = new float[mLenght];
            output.add(nThList);
            for (int m = 0; m < mLenght; m++) {
                float handle = input.get(m)[n];
                nThList[m] = handle;
            }
        }
        return output;
    }

    /**
     * interpolates  a segment of the given handle list using catmull rom.
     * segment starts at flatHandles[i] .. flatHandles[i+3]
     *
     * @param flatHandles list of flat handle values. each idx = 1 handle
     * @param handleToCurve positions of handles on the curve
     * @param i index of segment start. can be 0 to length-3
     * @return interpolated segment between handle[i+1] and handle[i+2]
     */
    public static float[] interpolateSegment(float[] flatHandles, int[] handleToCurve, int i) {
        assert i >= 0;
        assert i < flatHandles.length - 3;
        assert flatHandles.length == handleToCurve.length;

        //interpolate all unknown handles within the segment ranging from B to C
        int IdxA = handleToCurve[i];
        int IdxB = handleToCurve[i + 1];
        int IdxC = handleToCurve[i + 2];
        int IdxD = handleToCurve[i + 3];

        float vA, vB, vC, vD;
        vA = flatHandles[i];
        vB = flatHandles[i + 1];
        vC = flatHandles[i + 2];
        vD = flatHandles[i + 3];

        int length = IdxC - IdxB;

        float start, end, handle0, handle1;
        start = vB;
        end = vC;
        float distSegment = IdxC - IdxB;
        float distCANormalized = (IdxC - IdxA) / distSegment;
        float distDBNormalized = (IdxD - IdxB) / distSegment;
        float diffCA = vC - vA;
        float diffDB = vD - vB;
        float tangentCA = diffCA / distCANormalized;
        float tangentDB = diffDB / distDBNormalized;
        handle0 = tangentCA / 3f + vB;    //start point + 1/3 tangent in startpoint
        handle1 = vC - tangentDB / 3f;

        float[] interpolated = new float[length];

        //find all handles that are between b and c and are interpolated
        for (int j = 0; j < length; j++) {
            float t = j * 1f / (length);
            float interpolatedV = calcuateCubicBezier(start, handle0, handle1, end, t);
            interpolated[j] = interpolatedV;
        }
        return interpolated;
    }

    public static float[] interpolateCatmullRom(float[] nthHandles, int[] handleToCurveIdx) {
        nthHandles = supplementFirstAndLastTwoHandles(nthHandles, INHERIT_VALUE, 0);
        HandleAndIdcs ready = removeInheritValues(nthHandles, handleToCurveIdx.clone());
        float[] interpolated = interpolateFromHandles(ready.handles, ready.idcs);
        assert interpolated.length == 1 + handleToCurveIdx[handleToCurveIdx.length - 2] : "interpolated values " +
                "array is not as long as the whole curve";
        return interpolated;
    }

    private boolean invariant() {
        boolean okay = true;

        float[] setValues = new float[type.size];
        Arrays.fill(setValues, INHERIT_VALUE);
        for (float[] handle : handles) {
            for (int n = 0; n < type.size; n++) {
                if (Float.isNaN(handle[n])) return false;
                if (setValues[n] == INHERIT_VALUE) setValues[n] = handle[n];
            }

            if (handle.length != type.size) {
                System.err.println("path has a handle with wrong size for type " + type + " expected " + type.size +
                        " but got " + Arrays.toString(handle));
                okay = false;
            }
        }

        if (this.type == PointInterpreter.PointType.RIVER_2D) okay = okay && validateRiver2D(handles);

        return okay;
    }

    public Path addPoint(float[] point) {
        Path sum = new Path(this.handles, this.type);
        sum.handles.add(point);
        assert invariant();
        return sum;
    }

    public Path newEmpty() {
        return new Path(Collections.EMPTY_LIST, this.type);
    }

    public Path removePoint(float[] point) {
        Path sum = new Path(this.handles, this.type);
        int idx = sum.indexOfPosition(point);
        sum.handles.remove(idx);
        assert sum.invariant();
        return sum;
    }

    public int indexOfPosition(float[] p) {
        for (int i = 0; i < handles.size(); i++) {
            if (arePositionalsEqual(p, handles.get(i), RiverHandleInformation.PositionSize.SIZE_2_D.value)) return i;
        }
        return -1;
    }

    public Path overwriteHandle(float[] original, float[] newValue) {
        Path sum = new Path(this.handles, this.type);
        int idx = indexOfPosition(original);
        sum.handles.set(idx, newValue);
        assert invariant();
        return sum;
    }

    public float[] getTail() {
        if (amountHandles() == 0) throw new IllegalArgumentException("can not access tail of zero-length path!");
        return handles.get(amountHandles() - 1);
    }

    public int amountHandles() {
        return handles.size();
    }

    public float[] getPreviousPoint(float[] point) throws IllegalAccessException {
        if (amountHandles() < 2)
            throw new IllegalAccessException("can not find previous point on path with less than 2 points.");
        int idx = indexOfPosition(point);
        if (idx == -1) throw new IllegalAccessException("this point is not part of the path.");
        if (idx == 0) return handles.get(1);
        return handles.get(idx - 1);
    }

    public Path insertPointAfter(float[] point, float[] newPosition) {
        Path sum = new Path(this.handles, this.type);
        int idx = indexOfPosition(point);
        if (idx == -1) throw new IllegalArgumentException("can not find point " + Arrays.toString(point) + "in path");
        sum.handles.add(idx + 1, newPosition);
        assert invariant();
        return sum;
    }

    @Override
    public Iterator<float[]> iterator() {
        return handles.iterator();
    }

    @Override
    public void forEach(Consumer<? super float[]> action) {
        Iterable.super.forEach(action);
    }

    @Override
    public Spliterator<float[]> spliterator() {
        return Iterable.super.spliterator();
    }

    public Path setHandleByIdx(float[] handle, int idx) {
        ArrayList<float[]> newHandles = new ArrayList<>(handles);
        newHandles.set(idx, handle);
        Path sum = new Path(newHandles, this.type);
        assert invariant();
        return sum;
    }

    public int[] handleToCurveIdx(boolean roundToGrid) {
        if (this.amountHandles() < 4)
            return new int[this.amountHandles()]; //zero filled array

        ArrayList<float[]> curve = continousCurveFromHandles(toPosition2DArray(this.handles), roundToGrid);
        int[] handleIdcs = new int[amountHandles()];
        int handleIdx = 1;
        for (int i = 0; i < curve.size(); i++) {
            if (arePositionalsEqual(handleByIndex(handleIdx), curve.get(i),
                    RiverHandleInformation.PositionSize.SIZE_2_D.value)) {
                handleIdcs[handleIdx] = i;
                handleIdx++;
            }
        }
        handleIdx--;
        for (int i = handleIdx; i < handleIdcs.length; i++) {
            handleIdcs[i] = handleIdcs[handleIdx];
        }
        //manually set the first and last handle index, so that the zero and last (virtual) segements are the same
        // length as the true first and last segments
        handleIdcs[0] = handleIdcs[1] - handleIdcs[2];
        int lastIdx = handleIdcs.length - 1;
        handleIdcs[lastIdx] = handleIdcs[lastIdx - 1] + (handleIdcs[lastIdx - 1] - handleIdcs[lastIdx - 2]);
        return handleIdcs;
    }

    public float[] handleByIndex(int index) throws IndexOutOfBoundsException {
        return handles.get(index);
    }

    public int getClosestHandleIdxTo(float[] coord) throws IllegalAccessException {
        if (amountHandles() == 0) throw new IllegalAccessException("can not find closest handle on zero-handle-path");
        int closest = -1;
        double distMinSquared = Double.MAX_VALUE;
        for (int i = 0; i < handles.size(); i++) {
            float[] p = handleByIndex(i);
            double distanceSq = PointUtils.getPositionalDistance(p, coord,
                    RiverHandleInformation.PositionSize.SIZE_2_D.value);
            if (distanceSq < distMinSquared) {
                distMinSquared = distanceSq;
                closest = i;
            }
        }
        assert invariant();
        return closest;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Path) {
            if (this.type != ((Path) obj).type) return false;
            for (int i = 0; i < handles.size(); i++) {
                float[] own = handleByIndex(i);
                float[] theirs = ((Path) obj).handleByIndex(i);
                for (int n = 0; n < own.length; n++) {
                    if (own[n] != theirs[n]) return false;
                }
            }
            return true;
        } else return false;
    }

    public Path clone() {
        return new Path(this.handles, this.type);
    }

    @Override
    public String toString() {
        return "Path{\n" + "type" + type + "\n" + "handles" + handlesToString() + "\n}";
    }

    public String handlesToString() {
        StringBuilder sb = new StringBuilder("handles:[");
        for (float[] handle : handles) {
            sb.append("\n").append(Arrays.toString(handle));
        }
        sb.append("]");
        return sb.toString();
    }

    public static class HandleAndIdcs {
        public final float[] handles;
        public final int[] idcs;

        public HandleAndIdcs(float[] handles, int[] idcs) {
            this.handles = handles;
            this.idcs = idcs;
        }
    }
}
