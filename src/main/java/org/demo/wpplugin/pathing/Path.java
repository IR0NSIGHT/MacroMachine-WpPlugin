package org.demo.wpplugin.pathing;

import org.demo.wpplugin.operations.River.RiverHandleInformation;

import java.util.*;
import java.util.function.Consumer;

import static org.demo.wpplugin.pathing.CubicBezierSpline.arePositionalsEqual;
import static org.demo.wpplugin.pathing.CubicBezierSpline.getPositionalDistance;

public class Path implements Iterable<float[]> {
    private final ArrayList<float[]> handles;

    public Path() {
        handles = new ArrayList<>(0);
    }

    public Path(List<float[]> handles) {
        this.handles = new ArrayList<>(handles.size());
        this.handles.addAll(handles);
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

    public Path addPoint(float[] point) {
        Path sum = new Path(this.handles);
        sum.handles.add(point);
        return sum;
    }

    public Path removePoint(float[] point) {
        Path sum = new Path(this.handles);
        sum.handles.remove(point);
        return sum;
    }

    public Path movePoint(float[] point, float[] newPosition) {
        Path sum = new Path(this.handles);
        int idx = indexOf(point);
        sum.handles.set(idx, newPosition);
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
        int idx = handles.indexOf(point);
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
        Path sum = new Path(this.handles);
        int idx = sum.handles.lastIndexOf(point);
        sum.handles.add(idx + 1, newPosition);
        return sum;
    }

    public boolean isHandle(float[] point) {
        return indexOf(point) != -1;
    }

    @Override
    public Iterator<float[]> iterator() {
        return handles.iterator();
    }

    @Override
    public void forEach(Consumer<? super float[]> action) {
        Iterable.super.forEach(action);
    }

    public int indexOf(float[] p) {
        for (int i = 0; i < handles.size(); i++) {
            if (p.equals(handles.get(i)))
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

    public float[] getClosestHandleTo(float[] coord) throws IllegalAccessException {
        if (amountHandles() == 0)
            throw new IllegalAccessException("can not find closest handle on zero-handle-path");
        float[] closest = null;
        double distMinSquared = Double.MAX_VALUE;
        for (float[] p : this) {
            double distanceSq = CubicBezierSpline.getPositionalDistance(p, coord,
                    RiverHandleInformation.PositionSize.SIZE_2_D.value);
            if (distanceSq < distMinSquared) {
                distMinSquared = distanceSq;
                closest = p;
            }
        }
        return closest;
    }
}
