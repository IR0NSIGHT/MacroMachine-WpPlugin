package org.demo.wpplugin.pathing;

import java.awt.*;
import java.util.ArrayList;

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
    public static float[][] getSplinePathFor(float[] A, float[] B, float[] C, float[] D, int positionDigits) {
        assert A.length == B.length;
        assert B.length == C.length;
        assert C.length == D.length;
        assert A.length >= positionDigits;

        int vectorSize = A.length;

        float distance = getPositionalDistance(B, C, positionDigits);

        //prepare output array of n-vectors
        //FIXME use estimate of curvelength
        int pointsInCurve = (int)(estimateCurveSize(A,B,C,D, positionDigits)*2);
        float[][] path = new float[pointsInCurve][];
        for (int i = 0; i < pointsInCurve; i++) {
            path[i] = new float[vectorSize];    //new n-vector
        }

        float[] handle1p = getBezierHandle(A, B, C, positionDigits, distance);
        float[] handle2P = getBezierHandle(D, C, B, positionDigits, distance);

        for (int n = 0; n < vectorSize; n++) {
            float[] nThPositionCurve = calculateCubicBezier(B[n], handle1p[n], handle2P[n], C[n], pointsInCurve);
            for (int i = 0; i < pointsInCurve; i++) {
                path[i][n] = nThPositionCurve[i];
            }
        }

        return path;
    }

    private static float getPositionalLength(float[] pointA, int positionDigits) {
        //euclidian distance of B and C positions
        float dist = 0;
        for (int i = 0; i < positionDigits; i++) {
            float distI = pointA[i];
            dist += distI * distI;
        }
        dist = (float) Math.sqrt(dist);
        return dist;
    }

    private static float estimateCurveSize(float[] pointA, float[] pointB, float[] pointC, float[] pointD, int positionDigits) {
        float chord = getPositionalDistance(pointD, pointA, positionDigits);
        float cont_net = getPositionalDistance(pointA, pointB, positionDigits)+
                getPositionalDistance(pointC, pointB, positionDigits)+
                getPositionalDistance(pointC, pointD, positionDigits);
        float app_arc_length = (cont_net+chord)/2f;
        return app_arc_length;
    }


    public static float getPositionalDistance(float[] pointA, float[] pointB, int positionDigits) {
        //euclidian distance of B and C positions
        float dist = 0;
        for (int i = 0; i < positionDigits; i++) {
            float distI = pointA[i] - pointB[i];
            dist += distI * distI;
        }
        dist = (float) Math.sqrt(dist);
        return dist;
    }

    public static boolean arePositionalsEqual(float[] pointA, float[] pointB, int positionDigits) {
        return point2dFromN_Vector(pointA).equals(point2dFromN_Vector(pointB));
    }

    private static float[] getBezierHandle(float[] pointA, float[] pointB, float[] pointC, int positionDigits,
                                           float distance) {
        float[] out = new float[pointA.length];
        for (int n = 0; n < pointA.length; n++) {
            out[n] = (pointC[n] - pointA[n]) / 2f;
        }

        //normalize and scale positions based on distance, so the catmull rom spline doesnt act crazy
        //equal to handle.normalize.scale(distance/2)
        float length = getPositionalLength(out, positionDigits);
        for (int i = 0; i < positionDigits; i++) {
            out[i] = out[i] / length * distance / 2f;
        }

        for (int n = 0; n < pointA.length; n++) {
            out[n] += pointB[n];
        }
        return out;
    }

    public static Point point2dFromN_Vector(float[] nVector) {
        if (nVector == null)
            return null;
        return new Point(Math.round(nVector[0]), Math.round(nVector[1]));
    }

    public static ArrayList<Point> point2DfromNVectorArr(ArrayList<float[]> points ) {
        ArrayList<Point> out = new ArrayList<>(points.size());
        for (int i = 0; i < points.size(); i++) {
            out.add(point2dFromN_Vector(points.get(i)));
        }
        return out;
    }
}
