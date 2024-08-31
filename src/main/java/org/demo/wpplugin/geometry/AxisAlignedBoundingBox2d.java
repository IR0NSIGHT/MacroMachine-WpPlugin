package org.demo.wpplugin.geometry;

import java.awt.*;
import java.util.Collection;
import java.util.Iterator;

public class AxisAlignedBoundingBox2d implements BoundingBox {
    private final Point minPoint;
    private final Point maxPoint;

    /**
     * axis aligned boundingbox. will contain min and maxpoint.
     *
     * @param minPoint
     * @param maxPoint
     */
    public AxisAlignedBoundingBox2d(Point minPoint, Point maxPoint) {
        this.minPoint = minPoint;
        this.maxPoint = maxPoint;
    }

    public static BoundingBox fromPoints(Collection<Point> points) {
        int xMin = Integer.MAX_VALUE, xMax = Integer.MIN_VALUE,
                yMin = Integer.MAX_VALUE, yMax = Integer.MIN_VALUE;
        for (Point p : points) {
            xMin = Math.min(xMin, p.x);
            xMax = Math.max(xMax, p.x);
            yMin = Math.min(yMin, p.y);
            yMax = Math.max(yMax, p.y);
        }
        return new AxisAlignedBoundingBox2d(new Point(xMin, yMin), new Point(xMax, yMax));
    }

    @Override
    public boolean contains(Point p) {
        return minPoint.x <= p.x && p.x <= maxPoint.x && minPoint.y <= p.y && p.y <= maxPoint.y;
    }

    @Override
    public BoundingBox expand(double size) {
        return new AxisAlignedBoundingBox2d(new Point((int) (minPoint.x - size), (int) (minPoint.y - size)),
                new Point((int) Math.ceil(maxPoint.x + size), (int) Math.ceil(maxPoint.y + size)));
    }

    public Iterator<Point> areaIterator() {
        return new Iterator<Point>() {
            int x = minPoint.x;
            int y = minPoint.y;

            @Override
            public boolean hasNext() {
                return x <= maxPoint.x && y <= maxPoint.y;
            }

            @Override
            public Point next() {
                Point p = new Point(x, y);
                if (x == maxPoint.x) {
                    x = minPoint.x;
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
                "area ="+(maxPoint.x-minPoint.x * maxPoint.y-minPoint.y)+
                '}';
    }
}
