package org.demo.wpplugin.pathing;

import org.demo.wpplugin.operations.River.RiverHandleInformation;

import java.util.*;
import java.util.function.Consumer;

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
        for (float[] p : curve) {
            if (previous != null && getPositionalDistance(p, previous,
                    RiverHandleInformation.PositionSize.SIZE_2_D.value) > 2) {
                return false;
            }
            previous = p;
        }
        return true;
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

    public ArrayList<float[]> continousCurve() {
        LinkedList<float[]> curvePoints = new LinkedList<>();
        //iterate all handles, calculate coordinates on curve
        for (int i = 0; i < this.amountHandles() - 3; i++) {
            float[][] curveSegment = CubicBezierSpline.getSplinePathFor(
                    this.handleByIndex(i),
                    this.handleByIndex(i + 1),
                    this.handleByIndex(i + 2),
                    this.handleByIndex(i + 3),
                    RiverHandleInformation.PositionSize.SIZE_2_D.value);
            curvePoints.addAll(Arrays.asList(curveSegment));
        }
        //FIXME reactivate    assert curveIsContinous(curvePoints) : "path has gaps inbetween";

        if (curvePoints.isEmpty())
            return new ArrayList<>(0);

        int positionDigits = 2;
        float[] previous = null;
        int size = curvePoints.size();
        for (int i = 0; i < size; i++) {
            //kill all successive points that are the same
            while (i < curvePoints.size() && arePositionalsEqual(curvePoints.get(i), previous, positionDigits))
                curvePoints.remove(i);
            size = curvePoints.size();
            if (i < size)
                previous = curvePoints.get(i);
        }

        assert invariant();
        //    assert curveHasNoClones(curvePoints) : "curve still contains clones";
        //    assert curveIsContinous(curvePoints);
        return new ArrayList<>(curvePoints);
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
