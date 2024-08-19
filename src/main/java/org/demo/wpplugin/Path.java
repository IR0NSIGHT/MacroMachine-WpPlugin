package org.demo.wpplugin;

import org.checkerframework.checker.units.qual.A;
import org.demo.wpplugin.layers.PathPreviewLayer;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

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

    public Path insertPointAfter(Point point, Point newPosition) {
        Path sum = new Path(this.handles);
        int idx = sum.handles.lastIndexOf(point);
        sum.handles.add(idx + 1, newPosition);
        return sum;
    }

    public Point handleByIndex(int index) throws IndexOutOfBoundsException {
        return handles.get(index);
    }

    public int amountHandles() {
        return handles.size();
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

    public ArrayList<Point> continousCurve() {
        LinkedList<Point> curvePoints = new LinkedList<>();
        //iterate all handles, calculate coordinates on curve
        for (int i = 0; i < this.amountHandles() - 3; i++) {
            curvePoints.addAll(CubicBezierSpline.getSplinePathFor(
                    this.handleByIndex(i),
                    this.handleByIndex(i + 1),
                    this.handleByIndex(i + 2),
                    this.handleByIndex(i + 3),
                    1));
        }
        return new ArrayList<>(curvePoints);
    }

    public Point getClosestHandleTo(Point coord) throws IllegalAccessException {
        if (amountHandles() == 0)
            throw new IllegalAccessException("can not find closest handle on zero-handle-path");
        Point closest = null;
        double distMinSquared = Double.MAX_VALUE;
        for (Point p: this) {
            double distanceSq = p.distanceSq(coord);
            if (distanceSq < distMinSquared) {
                distMinSquared = distanceSq;
                closest = p;
            }
        }
        return closest;
    }
}
