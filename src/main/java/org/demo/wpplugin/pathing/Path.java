package org.demo.wpplugin.pathing;

import org.demo.wpplugin.geometry.HeightDimension;
import org.demo.wpplugin.operations.ContinuousCurve;
import org.demo.wpplugin.operations.River.RiverHandleInformation;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;

import static org.demo.wpplugin.operations.River.RiverHandleInformation.*;
import static org.demo.wpplugin.operations.River.RiverHandleInformation.RiverInformation.WATER_Z;
import static org.demo.wpplugin.pathing.CubicBezierSpline.calcuateCubicBezier;
import static org.demo.wpplugin.pathing.PointUtils.*;

public class Path implements Iterable<float[]> {
    public final PointInterpreter.PointType type;
    private final ArrayList<float[]> handles;
    private final float[] defaultInterpolationValues;

    public Path(List<float[]> handles, PointInterpreter.PointType type) {
        this.handles = new ArrayList<>(handles.size());
        for (float[] handle : handles) {
            this.handles.add(handle.clone());
        }
        this.type = type;
        defaultInterpolationValues = new float[type.size];
        Arrays.fill(defaultInterpolationValues, 5f);
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
     * @param handles
     * @param roundToGrid
     * @return
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
            else if (arePositionalsEqual(point, previousPoint, positionDigits)) {
                continue;
            } else {
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
        ArrayList pointResult = point2DfromNVectorArr(result);
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
     * @param handles
     * @param emptyMarker use this value to decide if a value in handle is "empty"
     * @return new array with set handles
     * @requires at least one handle value is set
     * @ensures first two and last two values are set, therefor all others can be interpolated
     */
    public static float[] supplementFirstAndLastTwoHandles(float[] handles, float emptyMarker, float defaultValue) {
        handles = handles.clone();
        if (handles.length > 0 && handles.length < 4) {
            throw new IllegalArgumentException("zero or at least 4 handles are required for interpolation");
        }

        //count how many handles are not empty, remember first and last handle
        int setHandles = 0;
        float firstHandle = emptyMarker;
        float lastHandle = emptyMarker;
        for (float handle : handles) {
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
        handles[0] = handles[0] == emptyMarker ? firstHandle : handles[0];
        handles[1] = handles[1] == emptyMarker ? firstHandle : handles[1];
        int idx = handles.length - 1;
        handles[idx] = handles[idx] == emptyMarker ? lastHandle : handles[idx];
        idx = handles.length - 2;
        handles[idx] = handles[idx] == emptyMarker ? lastHandle : handles[idx];

        assert canBeInterpolated(handles);
        return handles;
    }

    public static HandleAndIdcs removeInheritValues(float[] flatHandles, int[] handleToCurve) {
        if (flatHandles.length != handleToCurve.length)
            throw new IllegalArgumentException("must be same size");

        float[] pureFlatHandles = new float[flatHandles.length];
        int[] pureHandleToCurve = new int[flatHandles.length];
        int pureIdx = 0;
        for (int i = 0; i < flatHandles.length - 1; i++) {
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
        assert pureFlatHandles.length == pureHandleToCurve.length;
        assert pureFlatHandles.length <= handleToCurve.length;

        return new HandleAndIdcs(pureFlatHandles, pureHandleToCurve);
    }

    /**
     * takes a flat array of handles and interpolates a curve from them.
     * the spacing between the handles on Y axis is determined by handleToCurve
     * any INHERIT values in the flatarray are just ignored and overwritten by interpolation
     *
     * @param flatHandles
     * @param handleToCurve
     * @return
     */
    public static float[] doInterpolation(float[] flatHandles, int[] handleToCurve) {
        assert flatHandles.length == handleToCurve.length : "both input array must be of same size.";
        assert Arrays.binarySearch(flatHandles, INHERIT_VALUE) < 0 : "handle array can not contain INHERIT values!";

        //each curve segment
        for (int i = 0; i < handleToCurve.length - 3; i++) {

        }
        return null;
    }

    /**
     * fill take a handle array with unknown values and return one where all values are know/interpolated
     *
     * @param handles
     * @param curveIdxByHandle array where arr[handleIdx] = startIdx on curve
     * @return
     * @requires handles array needs to be interpolatable -> first two and last two values MUST be set
     */
    public static float[] interpolateHandles(float[] handles, int[] curveIdxByHandle) {
        assert handles.length == curveIdxByHandle.length : "both arrays must be the same length as the represent the "
                + "same curveHandles";
        assert canBeInterpolated(handles);
        //collect a map of all handles that are NOT interpolated and carry values
        int amountHandlesWithValues = 0;
        for (int i = 0; i < handles.length; i++)
            if (handles[i] != INHERIT_VALUE) amountHandlesWithValues++;

        int[] setValueIdcs = new int[amountHandlesWithValues];
        {
            int setValueIdx = 0;
            for (int i = 0; i < handles.length; i++)
                if (handles[i] != INHERIT_VALUE) setValueIdcs[setValueIdx++] = i;
        }

        float[] outHandles = handles.clone();

        for (int i = 0; i < setValueIdcs.length - 3; i++) {
            //interpolate all unknown handles within the segment ranging from B to C
            int handleIdxA = setValueIdcs[i];
            int handleIdxB = setValueIdcs[i + 1];
            int handleIdxC = setValueIdcs[i + 2];
            int handleIdxD = setValueIdcs[i + 3];

            float vA, vB, vC, vD;
            vA = handles[handleIdxA];
            vB = handles[handleIdxB];
            vC = handles[handleIdxC];
            vD = handles[handleIdxD];

            int length = curveIdxByHandle[handleIdxC] - curveIdxByHandle[handleIdxB];
            float start, end, handle0, handle1;
            start = vB;
            end = vC;
            handle0 = length / 2f * (vC - vA) / (curveIdxByHandle[handleIdxC] - curveIdxByHandle[handleIdxA]) / 2f + vB;
            handle1 = length / 2f * (vB - vD) / (curveIdxByHandle[handleIdxB] - curveIdxByHandle[handleIdxD]) / 2f + vC;

            //find all handles that are between b and c and are interpolated
            for (int unknownHandleIdx = handleIdxB + 1; unknownHandleIdx < handleIdxC; unknownHandleIdx++) {
                int handlePositionInSegment = (curveIdxByHandle[unknownHandleIdx] - curveIdxByHandle[handleIdxB]);
                float t = handlePositionInSegment / (length * 1f);
                float interpolatedV = calcuateCubicBezier(start, handle0, handle1, end, t);
                outHandles[unknownHandleIdx] = interpolatedV;
            }
        }
        return outHandles;
    }

    public static boolean canBeInterpolated(float[] handles) {
        return handles.length >= 4 && handles[0] != INHERIT_VALUE && handles[1] != INHERIT_VALUE && handles[handles.length - 1] != INHERIT_VALUE && handles[handles.length - 2] != INHERIT_VALUE;
    }

    /**
     * will turn a n x m list into an m x n list
     * deep-clones inputs
     *
     * @param input
     * @return
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
     * @param flatHandles
     * @param handleToCurve
     * @param i
     * @return
     */
    public static float[] interpolateSegment(float[] flatHandles, int[] handleToCurve, int i) {
        assert i >= 0;
        assert i < flatHandles.length;
        assert i < handleToCurve.length;

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

        //FIXME how to handle if user inputs illegal values?
       /* if (handles.size() != 0)
            for (int n = 0; n < type.size; n++) {
                if (setValues[n] == INHERIT_VALUE) {
                    okay = false;
                    break;
                }
            }

        */
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

    public Path movePoint(float[] point, float[] newPosition) {
        Path sum = new Path(this.handles, this.type);
        int idx = indexOfPosition(point);
        float[] copy = point.clone();
        //use meta info from point and position of newPosition
        if (RiverHandleInformation.PositionSize.SIZE_2_D.value >= 0)
            System.arraycopy(newPosition, 0, copy, 0, RiverHandleInformation.PositionSize.SIZE_2_D.value);
        sum.handles.set(idx, newPosition);
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
        if (idx == -1) throw new IllegalArgumentException("can not find point " + point + "in path");
        sum.handles.add(idx + 1, newPosition);
        assert invariant();
        return sum;
    }

    public boolean isHandle(float[] point) {
        return indexOfPosition(point) != -1;
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

    public ContinuousCurve continousCurve() {
        return continousCurve(true);
    }

    public ContinuousCurve continousCurve(boolean roundToGrid) {
        if (this.handles.isEmpty())
            return new ContinuousCurve(new ArrayList<float[]>());

        ArrayList<float[]> handles = new ArrayList<>(this.handles.size());
        for (float[] handle : this.handles)
            handles.add(handle.clone());

        //we know the positions of each handle already through magic
        int[] handleToCurveIdx = this.handleToCurveIdx(roundToGrid);

        //handles exist as flat lists
        ArrayList<float[]> flatHandles = transposeHandles(handles);
        ArrayList<float[]> flatHandlesInterpolated = new ArrayList<>(flatHandles.size());

        //iterate all handleArrays and calculate a continous curve
        for (int n = 0; n < type.size; n++) {
            float[] nthHandles = flatHandles.get(n);
            nthHandles = supplementFirstAndLastTwoHandles(nthHandles, INHERIT_VALUE, defaultInterpolationValues[n]);
            HandleAndIdcs ready = removeInheritValues(nthHandles, handleToCurveIdx.clone());
            float[] interpolated = interpolateHandles(ready.handles, ready.idcs);
            assert interpolated.length == handleToCurveIdx[handleToCurveIdx.length - 1] : "interpolated values " +
                    "array is not as long as the whole curve";
            flatHandlesInterpolated.add(interpolated);
        }
        return new ContinuousCurve(flatHandlesInterpolated);
    }

    public int[] handleToCurveIdx(boolean roundToGrid) {
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
        return handleIdcs;
    }

    public float[] handleByIndex(int index) throws IndexOutOfBoundsException {
        return handles.get(index);
    }

    private boolean curveHasNoClones(List<float[]> curve) {
        float[] previous = null;
        for (float[] p : curve) {
            if (p.equals(previous)) {
                return false;
            }
            previous = p;
        }
        return true;
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
