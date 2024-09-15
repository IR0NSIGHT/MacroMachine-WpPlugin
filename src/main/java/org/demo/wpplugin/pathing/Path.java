package org.demo.wpplugin.pathing;

import org.demo.wpplugin.operations.River.RiverHandleInformation;

import java.util.*;
import java.util.function.Consumer;

import static org.demo.wpplugin.pathing.CubicBezierSpline.calcuateCubicBezier;
import static org.demo.wpplugin.pathing.PointUtils.arePositionalsEqual;
import static org.demo.wpplugin.pathing.PointUtils.getPositionalDistance;

public class Path implements Iterable<float[]> {
    public final PointInterpreter.PointType type;
    private final ArrayList<float[]> handles;

    private Path() {
        this.type = PointInterpreter.PointType.POSITION_2D;
        handles = new ArrayList<>(0);
    }

    public Path(List<float[]> handles, PointInterpreter.PointType type) {
        this.handles = new ArrayList<>(handles.size());
        this.handles.addAll(handles);
        this.type = type;
    }

    public static boolean curveIsContinous(List<float[]> curve) {
        float[] previous = null;
        float root2 = (float)Math.sqrt(2) + 0.001f; //allow delta
        for (float[] p : curve) {
            if (previous != null) {
                float dist = getPositionalDistance(p, previous,
                        RiverHandleInformation.PositionSize.SIZE_2_D.value);
                if (arePositionalsEqual(p,previous,RiverHandleInformation.PositionSize.SIZE_2_D.value))
                    return false; //point has clone
                if (dist >= root2){
                    return false; //point is not connected
                }
            }
            previous = p;
        }
        return true;
    }

    public static float[] interpolateHandles(float[] handles, int[] curveIdxByHandle) {
        float[] outHandles = handles.clone();
        //collect a map of all handles that are NOT interpolated and carry values
        int amountHandlesWithValues = 0;
        for (int i = 0; i < handles.length; i++)
            if (handles[i] != RiverHandleInformation.INHERIT_VALUE)
                amountHandlesWithValues++;

        int[] setValueIdcs = new int[amountHandlesWithValues];
        {
            int setValueIdx = 0;
            for (int i = 0; i < handles.length; i++)
                if (handles[i] != RiverHandleInformation.INHERIT_VALUE)
                    setValueIdcs[setValueIdx++] = i;
        }

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

    public static ArrayList<float[]> continousCurveFromHandles(ArrayList<float[]> handles) {
        LinkedList<float[]> curvePoints = new LinkedList<>();

        //iterate all handles, calculate coordinates on curve
        for (int i = 0; i < handles.size() - 3; i++) {
            float[][] curveSegment = CubicBezierSpline.getSplinePathFor(
                    handles.get(i),
                    handles.get(i + 1),
                    handles.get(i + 2),
                    handles.get(i + 3),
                    RiverHandleInformation.PositionSize.SIZE_2_D.value);
            curvePoints.addAll(Arrays.asList(curveSegment));
        }
        //FIXME reactivate    assert curveIsContinous(curvePoints) : "path has gaps inbetween";

        if (curvePoints.isEmpty())
            return new ArrayList<>(0);

        int positionDigits = PointInterpreter.PointType.POSITION_2D.size;   //FIXME thats the wrong constant
        assert curvePoints.size() > 2;

        ArrayList<float[]> result = new ArrayList<>(curvePoints.size());
        float[] previousPoint = curvePoints.get(0);
        result.add(previousPoint);
        for (float[] point: curvePoints) {
            assert point != null : "whats going on?";
            if (arePositionalsEqual(point, previousPoint, positionDigits)) {
                continue;
            } else {
                previousPoint = point;
                result.add(previousPoint);
            }
        }
        result.trimToSize();
        assert curveIsContinous(result);
        return result;
    }

    private boolean invariant() {
        boolean okay = true;
        for (float[] handle : handles) {
            if (handle.length != type.size) {
                System.err.println("path has a handle with wrong size for type " + type +
                        " expected " + type.size +
                        " but got " + Arrays.toString(handle));
                okay = false;
            }
        }
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
        sum.handles.remove(point);
        assert invariant();
        return sum;
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
        if (amountHandles() == 0)
            throw new IllegalArgumentException("can not access tail of zero-length path!");
        return handles.get(amountHandles() - 1);
    }

    public float[] getPreviousPoint(float[] point) throws IllegalAccessException {
        if (amountHandles() < 2)
            throw new IllegalAccessException("can not find previous point on path with less than 2 points.");
        int idx = indexOfPosition(point);
        if (idx == -1)
            throw new IllegalAccessException("this point is not part of the path.");
        if (idx == 0)
            return handles.get(1);
        return handles.get(idx - 1);
    }

    public int amountHandles() {
        return handles.size();
    }

    public Path insertPointAfter(float[] point, float[] newPosition) {
        Path sum = new Path(this.handles, this.type);
        int idx = indexOfPosition(point);
        if (idx == -1)
            throw new IllegalArgumentException("can not find point " + point + "in path");
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

    public int indexOfPosition(float[] p) {
        for (int i = 0; i < handles.size(); i++) {
            if (arePositionalsEqual(p, handles.get(i), RiverHandleInformation.PositionSize.SIZE_2_D.value))
                return i;
        }
        return -1;
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

    public ArrayList<float[]> continousCurve() {
        ArrayList<float[]> handles = new ArrayList<>(this.handles);
        int[] handleToCurveIdx = this.handleToCurveIdx();
        //fill handles that are marked as "to be interpolated"
        for (int n = 2; n < this.type.size; n++) {
            float[] informationArr = new float[handles.size()];
            for (int i = 0; i < handles.size(); i++) {
                informationArr[i] = handles.get(i)[n];
            }
            informationArr = interpolateHandles(informationArr, handleToCurveIdx );
            for (int i = 0; i < handles.size(); i++) {
                handles.get(i)[n] = informationArr[i];
            }
        }
        return continousCurveFromHandles(handles);
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

    public int[] handleToCurveIdx() {
        ArrayList<float[]> curve = continousCurveFromHandles(this.handles);
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

    public int getClosestHandleIdxTo(float[] coord) throws IllegalAccessException {
        if (amountHandles() == 0)
            throw new IllegalAccessException("can not find closest handle on zero-handle-path");
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
}
