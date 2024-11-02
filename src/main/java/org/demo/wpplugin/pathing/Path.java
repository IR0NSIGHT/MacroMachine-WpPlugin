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
        assert invariant() : "path invariant hurt";
    }

    public static Path newFilledPath(int length, PointInterpreter.PointType type) {
        Path p = new Path(Collections.EMPTY_LIST, type);
        for (int i = 0; i < length; i++) {
            float[] newHandle = new float[type.size];
            newHandle[0] = 3 * i;
            newHandle[1] = 4 * i;
            for (int n = 2; n < type.size; n++) {
                newHandle[n] = 27;
            }
            p = p.addPoint(newHandle.clone());
        }
        return p;
    }

    public static float[] interpolateWaterZ(ContinuousCurve curve, HeightDimension dim) {
        float[] out = new float[curve.curveLength()];
        Point first = curve.getPositions2d()[0];
        out[0] = Math.min(curve.getInfo(WATER_Z)[0], dim.getHeight(first.x, first.y));
        Point[] positions = curve.getPositions2d();
        for (int i = 1; i < curve.curveLength(); i++) {
            float curveZ = curve.getInfo(WATER_Z)[i];
            int x = positions[i].x;
            int y = positions[i].y;
            float terrainZ = dim.getHeight(x, y);
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

    public ArrayList<float[]> getHandles() {
        return handles;
    }

    private boolean invariant() {
        boolean okay = true;

        float[] setValues = new float[type.size];
        Arrays.fill(setValues, INHERIT_VALUE);
        for (float[] handle : handles) {
            for (int n = 0; n < type.size; n++) {
                if (Float.isNaN(handle[n]))
                    return false;
                if (setValues[n] == INHERIT_VALUE)
                    setValues[n] = handle[n];
            }

            if (handle.length != type.size) {
                System.err.println("path has a handle with wrong size for type " + type + " expected " + type.size +
                        " but got " + Arrays.toString(handle));
                okay = false;
            }
        }

        if (okay && this.type == PointInterpreter.PointType.RIVER_2D)
            okay = validateRiver2D(handles);

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

    public int[] estimateSegmentLengths(boolean roundToGrid) {
        if (this.amountHandles() < 4) return new int[this.amountHandles()]; //zero filled array

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

}
