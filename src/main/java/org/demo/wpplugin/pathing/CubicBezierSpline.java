package org.demo.wpplugin.pathing;

import javax.vecmath.Vector2f;
import java.awt.*;

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

    public static float getHalfWay(float A, float C) {
        // (AC)/2+B
        return (C - A) / 2;
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
        float dist = (float)B.distance(C);
        Vector2f handle1p = new Vector2f(
                getHalfWay(A.x, C.x),
                getHalfWay(A.y, C.y));
        handle1p.normalize();
        handle1p.scale(dist/2f);
        handle1p.add(new Vector2f(B.x,B.y));

        Vector2f handle2P = new Vector2f(
                getHalfWay(D.x, B.x),
                getHalfWay(D.y, B.y));
        handle2P.normalize();
        handle2P.scale(dist/2f);
        handle2P.add(new Vector2f(C.x,C.y));

        //estimate length by measuring rough curve with 50 points
        float[] pathX = calculateCubicBezier(B.x, handle1p.x, handle2P.x, C.x, 50);
        float[] pathY = calculateCubicBezier(B.y, handle1p.y, handle2P.y, C.y, 50);

        Point[] path = new Point[pathX.length];
        for (int i = 0; i < pathX.length; i++) {
            path[i] = new Point(Math.round(pathX[i]), Math.round(pathY[i]));
        }
        double length = calculatePathLength(path);

        pathX = calculateCubicBezier(B.x, handle1p.x, handle2P.x, C.x, (int) (length / metersBetweenPoints));
        pathY = calculateCubicBezier(B.y, handle1p.y, handle2P.y, C.y, (int) (length / metersBetweenPoints));

        path = new Point[pathX.length];
        for (int i = 0; i < pathX.length; i++) {
            path[i] = new Point(Math.round(pathX[i]), Math.round(pathY[i]));
        }
        return path;
    }
}
