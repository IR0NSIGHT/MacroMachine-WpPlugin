package org.demo.wpplugin.pathing;

import org.demo.wpplugin.geometry.AxisAlignedBoundingBox2d;
import org.demo.wpplugin.geometry.BoundingBox;

import javax.vecmath.Vector2f;
import java.awt.*;
import java.util.*;

import static org.demo.wpplugin.pathing.PointUtils.*;

public class CubicBezierSpline {
    /**
     * calculate a cubic bezier curve connection start and endpoint with 2 control handles.
     * returns a path on the curve with @numPoints length. includes start and endpoint (?)
     * @param startPoint
     * @param handle0
     * @param handle1
     * @param endPoint
     * @param numPoints
     * @return
     */
    public static Collection<Point> calculateCubicBezier(Point startPoint, Point handle0, Point handle1, Point endPoint, int numPoints) {
        Collection<Point> points = new ArrayList<>();
        for (int i = 0; i <= numPoints; i++) {
            double t = (double) i / numPoints;
            double x = Math.pow(1 - t, 3) * startPoint.x
                    + 3 * Math.pow(1 - t, 2) * t * handle0.x
                    + 3 * (1 - t) * Math.pow(t, 2) * handle1.x
                    + Math.pow(t, 3) * endPoint.x;
            double y = Math.pow(1 - t, 3) * startPoint.y
                    + 3 * Math.pow(1 - t, 2) * t * handle0.y
                    + 3 * (1 - t) * Math.pow(t, 2) * handle1.y
                    + Math.pow(t, 3) * endPoint.y;
            points.add(new Point((int) Math.round(x), (int) Math.round(y)));
        }
        return points;
    }

    public static Point getCubicBezierHandles(Point A, Point B, Point C) {
        float factor = (float) B.distance(C) / 2f;
        final Vector2f av,bv,cv,dv;
        av = new Vector2f(A.x,A.y);
        bv = new Vector2f(B.x,B.y);
        cv = new Vector2f(C.x,C.y);

        // (AC)/2+B
        Vector2f handle1 = new Vector2f(cv);
        handle1.sub(av);
        handle1.normalize();
        handle1.scale(factor);
        handle1.add(bv);

        return new Point((int) handle1.x, (int) handle1.y);
    }

    public static BoundingBox boundingBoxCurveSegment(Point controlA, Point controlB, Point controlC, Point controlD) {
        Point handle1p = getCubicBezierHandles(controlA,controlB,controlC);
        Point handle2P =getCubicBezierHandles(controlD,controlC,controlB);
        return AxisAlignedBoundingBox2d.fromPoints(Arrays.asList(controlB, handle1p, handle2P, controlD));
    }

    /**
     * get spline connecting B and C. A and D are the points before and after BC in the path
     *
     * @param A
     * @param B
     * @param C
     * @param D
     */
    public static Collection<Point> getSplinePathFor(Point A, Point B, Point C, Point D, float metersBetweenPoints) {

        Point handle1p = getCubicBezierHandles(A,B,C);
        Point handle2P =getCubicBezierHandles(D,C,B);

        //estimate length by measuring rough curve with 50 points
        Collection<Point> path = calculateCubicBezier(B, handle1p, handle2P, C, 50);
        double length = calculatePathLength(path.toArray(new Point[0]));

        path = calculateCubicBezier(B, handle1p, handle2P, C, (int) (length / metersBetweenPoints));

        return path;
    }
}
