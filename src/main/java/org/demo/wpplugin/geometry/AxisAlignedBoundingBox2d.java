package org.demo.wpplugin.geometry;

import java.awt.*;
import java.util.Collection;
import java.util.Iterator;

public class AxisAlignedBoundingBox2d implements BoundingBox {
    final Point maxPoint;
    final Point minPoint;
    public final int id;
    /**
     * axis aligned boundingbox. will contain min and maxpoint.
     *
     * @param minPoint
     * @param maxPoint
     */
    public AxisAlignedBoundingBox2d(Point minPoint, Point maxPoint, int id) {
        this.minPoint = minPoint;
        this.maxPoint = maxPoint;
        this.id = id;

        assert maxPoint.x >= minPoint.x;
        assert maxPoint.y >= minPoint.y;
    }

    public AxisAlignedBoundingBox2d(Collection<Point> points, int id) {
        this.id = id;
        int xMin = Integer.MAX_VALUE, xMax = Integer.MIN_VALUE,
                yMin = Integer.MAX_VALUE, yMax = Integer.MIN_VALUE;
        for (Point p : points) {
            xMin = Math.min(xMin, p.x);
            xMax = Math.max(xMax, p.x);
            yMin = Math.min(yMin, p.y);
            yMax = Math.max(yMax, p.y);
        }
        this.minPoint = new Point(xMin, yMin);
        this.maxPoint = new Point(xMax, yMax);
    }

    public static AxisAlignedBoundingBox2d fromPoints(Collection<Point> points) {

        return new AxisAlignedBoundingBox2d(points,0);
    }

    @Override
    public boolean contains(Point p) {
        return minPoint.x <= p.x && p.x <= maxPoint.x && minPoint.y <= p.y && p.y <= maxPoint.y;
    }

    @Override
    public AxisAlignedBoundingBox2d expand(double size) {
        return new AxisAlignedBoundingBox2d(new Point((int) (minPoint.x - size), (int) (minPoint.y - size)),
                new Point((int) Math.ceil(maxPoint.x + size), (int) Math.ceil(maxPoint.y + size)),id);
    }

    public Iterator<Point> areaIterator() {
        return new Iterator<Point>() {
            final int maxX = maxPoint.x;
            final int maxY = maxPoint.y;
            final int minX = minPoint.x;
            final int minY = minPoint.y;
            int x = minPoint.x;
            int y = minPoint.y;

            @Override
            public boolean hasNext() {
                return x <= maxX && y <= maxY;
            }

            @Override
            public Point next() {
                Point p = new Point(x, y);
                if (x >= maxX) {
                    x = minX;
                    y++;
                } else {
                    x++;
                }
                return p;
            }
        };
    }

    @Override
    public String toString() {
        return "AxisAlignedBoundingBox2d{" +
                "width =" + (maxPoint.x - minPoint.x) +
                "height =" + (maxPoint.y - minPoint.y) +
                '}';
    }
}
