package org.demo.wpplugin.pathing;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class Path implements Iterable<Point> {
    private final ArrayList<Point> handles;

    public Path() {
        handles = new ArrayList<>(0);
    }

    public Path(List<Point> handles) {
        this.handles = new ArrayList<>(handles.size());
        this.handles.addAll(handles);
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
        return handles.get(amountHandles()-1);
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

    @Override
    public Spliterator<Point> spliterator() {
        return Iterable.super.spliterator();
    }

    public ArrayList<Point> continousCurve(Predicate<Point> pointOnMap) {
        LinkedList<Point> curvePoints = new LinkedList<>();
        //iterate all handles, calculate coordinates on curve
        for (int i = 0; i < this.amountHandles() - 3; i++) {
            curvePoints.addAll(CubicBezierSpline.getSplinePathFor(
                    this.handleByIndex(i),
                    this.handleByIndex(i + 1),
                    this.handleByIndex(i + 2),
                    this.handleByIndex(i + 3),
                    .5f));
        }

        assert curveIsContinous(curvePoints) : "path has gaps inbetween";

        if (curvePoints.size() == 0)
            return new ArrayList<>(0);

        Point previous = null;
        int size = curvePoints.size();
        for (int i = 0; i < size; i++) {
            //KILL ALL POINTS THAT ARE OUTSIDE THE map
            while (i < curvePoints.size() && !pointOnMap.test(curvePoints.get(i)))
                curvePoints.remove(i);
            //kill all successive points that are the same
            while (i < curvePoints.size() && curvePoints.get(i).equals(previous))
                curvePoints.remove(i);
            size = curvePoints.size();
            if (i < size)
                previous = curvePoints.get(i);
        }

        assert curveHasNoClones(curvePoints) : "curve still contains clones";

        return new ArrayList<>(curvePoints);
    }

    public Point handleByIndex(int index) throws IndexOutOfBoundsException {
        return handles.get(index);
    }

    private boolean curveIsContinous(List<Point> curve) {
        Point previous = null;
        for (Point p : curve) {
            if (previous != null && p.distanceSq(previous) > 2) {
                return false;
            }
            previous = p;
        }
        return true;
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
