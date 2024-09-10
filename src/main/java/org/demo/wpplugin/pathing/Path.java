package org.demo.wpplugin.pathing;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;

import static org.demo.wpplugin.pathing.CubicBezierSpline.arePositionalsEqual;

public class Path implements Iterable<Point> {
    private final ArrayList<Point> handles;

    public Path() {
        handles = new ArrayList<>(0);
    }

    public Path(List<Point> handles) {
        this.handles = new ArrayList<>(handles.size());
        this.handles.addAll(handles);
    }

    public static boolean curveIsContinous(List<Point> curve) {
        Point previous = null;
        for (Point p : curve) {
            if (previous != null && p.distanceSq(previous) > 2) {
                return false;
            }
            previous = p;
        }
        return true;
    }

    public Path addPoint(Point point) {
        Path sum = new Path(this.handles);
        sum.handles.add(point);
        return sum;
    }

    public Path removePoint(Point point) {
        Path sum = new Path(this.handles);
        sum.handles.remove(point);
        return sum;
    }

    public Path movePoint(Point point, Point newPosition) {
        Path sum = new Path(this.handles);
        int idx = sum.handles.lastIndexOf(point);
        sum.handles.set(idx, newPosition);
        return sum;
    }

    public Point getTail() {
        if (amountHandles() == 0)
            throw new IllegalArgumentException("can not access tail of zero-length path!");
        return handles.get(amountHandles() - 1);
    }

    public Point getPreviousPoint(Point point) throws IllegalAccessException {
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

    public Path insertPointAfter(Point point, Point newPosition) {
        Path sum = new Path(this.handles);
        int idx = sum.handles.lastIndexOf(point);
        sum.handles.add(idx + 1, newPosition);
        return sum;
    }

    public boolean isHandle(Point point) {
        return handles.contains(point);
    }

    @Override
    public Iterator<Point> iterator() {
        return handles.iterator();
    }

    @Override
    public void forEach(Consumer<? super Point> action) {
        Iterable.super.forEach(action);
    }

    public Point byIndex(int index) {
        return handles.get(index);
    }

    public int indexOf(Point p) {
        for (int i = 0; i < handles.size(); i++) {
            if (p.equals(handles.get(i)))
                return i;
        }
        return -1;
    }

    @Override
    public Spliterator<Point> spliterator() {
        return Iterable.super.spliterator();
    }

    public ArrayList<float[]> continousCurve() {
        LinkedList<float[]> curvePoints = new LinkedList<>();
        //iterate all handles, calculate coordinates on curve
        for (int i = 0; i < this.amountHandles() - 3; i++) {
            float[][] curveSegment = CubicBezierSpline.getSplinePathFor(
                    new float[]{this.handleByIndex(i).x, this.handleByIndex(i).y},
                    new float[]{this.handleByIndex(i + 1).x, this.handleByIndex(i + 1).y},
                    new float[]{this.handleByIndex(i + 2).x, this.handleByIndex(i + 2).y},
                    new float[]{this.handleByIndex(i + 3).x, this.handleByIndex(i + 3).y},
                    2);
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

    public Point handleByIndex(int index) throws IndexOutOfBoundsException {
        return handles.get(index);
    }

    private boolean curveHasNoClones(List<Point> curve) {
        Point previous = null;
        for (Point p : curve) {
            if (p.equals(previous)) {
                return false;
            }
            previous = p;
        }
        return true;
    }

    public Point getClosestHandleTo(Point coord) throws IllegalAccessException {
        if (amountHandles() == 0)
            throw new IllegalAccessException("can not find closest handle on zero-handle-path");
        Point closest = null;
        double distMinSquared = Double.MAX_VALUE;
        for (Point p : this) {
            double distanceSq = p.distanceSq(coord);
            if (distanceSq < distMinSquared) {
                distMinSquared = distanceSq;
                closest = p;
            }
        }
        return closest;
    }
}
