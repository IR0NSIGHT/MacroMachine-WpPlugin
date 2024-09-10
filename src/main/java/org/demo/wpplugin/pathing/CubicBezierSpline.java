package org.demo.wpplugin.pathing;

import org.demo.wpplugin.geometry.AxisAlignedBoundingBox2d;
import org.demo.wpplugin.geometry.BoundingBox;

import java.awt.*;
import java.util.Arrays;

import static org.demo.wpplugin.pathing.PointUtils.calculatePathLength;

public class CubicBezierSpline {
    /**
     * calculate a cubic bezier curve connection start and endpoint with 2 control handles.
     * returns a path on the curve with @numPoints length. includes start and endpoint (?)
     *
     * @param startPoint
     * @param handle0
     * @param handle1
     * @param endPoint
     * @param numPoints
     * @return
     */
    public static float[] calculateCubicBezier(float startPoint, float handle0, float handle1, float endPoint,
                                               int numPoints) {
        float[] points = new float[numPoints];
        for (int i = 0; i < numPoints; i++) {
            double t = (double) i / numPoints;
            double tSq = t * t, tCub = tSq * t;
            double x = Math.pow(1 - t, 3) * startPoint
                    + 3 * Math.pow(1 - t, 2) * t * handle0
                    + 3 * (1 - t) * tSq * handle1
                    + tCub * endPoint;
            points[i] = (float) x;
        }
        return points;
    }

    public static float getCubicBezierHandles(float A, float B, float C) {
        // (AC)/2+B
        return B + (C - A) / 2;
    }

    public static BoundingBox boundingBoxCurveSegment(Point controlA, Point controlB, Point controlC, Point controlD) {
        Point handle1p = new Point(
                Math.round(getCubicBezierHandles(controlA.x, controlB.x, controlC.x)),
                Math.round(getCubicBezierHandles(controlA.y, controlB.y, controlC.y)));
        Point handle2P = new Point(
                Math.round(getCubicBezierHandles(controlD.x, controlC.x, controlB.x)),
                Math.round(getCubicBezierHandles(controlD.y, controlC.y, controlB.y)));
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
    public static Point[] getSplinePathFor(Point A, Point B, Point C, Point D, float metersBetweenPoints) {

        Point handle1p = new Point(
                Math.round(getCubicBezierHandles(A.x, B.x, C.x)),
                Math.round(getCubicBezierHandles(A.y, B.y, C.y)));
        Point handle2P = new Point(
                Math.round(getCubicBezierHandles(D.x, C.x, B.x)),
                Math.round(getCubicBezierHandles(D.y, C.y, B.y)));

        //estimate length by measuring rough curve with 50 points
        float[] pathX = calculateCubicBezier(B.x, handle1p.x, handle2P.x, C.x, 50);
        float[] pathY = calculateCubicBezier(B.y, handle1p.y, handle2P.y, C.y, 50);

        Point[] path = new Point[pathX.length];
        for (int i = 0; i < pathX.length; i++) {
            path[i] = new Point(Math.round(pathX[i]),Math.round(pathY[i]));
        }
        double length = calculatePathLength(path);

        pathX = calculateCubicBezier(B.x, handle1p.x, handle2P.x, C.x, (int) (length / metersBetweenPoints));
        pathY = calculateCubicBezier(B.y, handle1p.y, handle2P.y, C.y, (int) (length / metersBetweenPoints));

        path = new Point[pathX.length];
        for (int i = 0; i < pathX.length; i++) {
            path[i] = new Point(Math.round(pathX[i]),Math.round(pathY[i]));
        }
        return path;
    }
}
