package org.demo.wpplugin.geometry;

import java.awt.*;
import java.util.Collection;

public class AxisAlignedBoundingBox2d implements BoundingBox {
    private final Point minPoint;
    private final Point maxPoint;

    /**
     * axis aligned boundingbox. will contain min and maxpoint.
     * @param minPoint
     * @param maxPoint
     */
    public AxisAlignedBoundingBox2d(Point minPoint, Point maxPoint) {
        this.minPoint = minPoint;
        this.maxPoint = maxPoint;
    }

    @Override
    public boolean contains(Point p) {
        return minPoint.x <= p.x && p.x <= maxPoint.x &&
                minPoint.y <= p.y && p.y <= maxPoint.y;
    }

    @Override
    public BoundingBox expand(double size) {
        return new AxisAlignedBoundingBox2d(new Point((int) (minPoint.x-size), (int) (minPoint.y-size)),
                new Point((int) (maxPoint.x+size), (int) (maxPoint.y+size)));
    }

    @Override
    public BoundingBox fromPoints(Point... points) {
        int xMin = Integer.MAX_VALUE, xMax=Integer.MIN_VALUE, yMin = Integer.MAX_VALUE, yMax = Integer.MIN_VALUE;
        for (Point p : points) {
            xMin = Math.min(xMin, p.x);
            xMax = Math.max(xMax, p.x);
            yMin = Math.min(yMin, p.y);
            yMax = Math.max(yMax, p.y);
        }
        return new AxisAlignedBoundingBox2d(new Point(xMin, yMin), new Point(xMin, yMax));
    }
}
