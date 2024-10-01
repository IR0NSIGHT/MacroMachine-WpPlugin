package org.demo.wpplugin.pathing;

import java.util.Arrays;

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
        float min = Math.min(Math.min(startPoint, endPoint), Math.min(handle0, handle1));
        float max = Math.max(Math.max(startPoint, endPoint), Math.max(handle0, handle1));

        float[] points = new float[numPoints];
        float tDelta = 1f / (numPoints - 1);
        for (int i = 0; i < numPoints; i++) {
            float t = tDelta * i;
            float tValue = calcuateCubicBezier(startPoint, handle0, handle1, endPoint, t);
            points[i] = tValue;
            assert (tValue - min > -0.01f && max - tValue > -0.01f);
        }

        assert numPoints == 0 || Math.round(points[0]) == Math.round(startPoint);
        assert numPoints <= 1 || Math.round(points[points.length - 1]) == Math.round(endPoint);
        return points;
    }

    public static float calcuateCubicBezier(float startPoint, float handle0, float handle1, float endPoint, float t) {
        double tSq = t * t, tCub = t * t * t;
        double x = Math.pow(1 - t, 3) * startPoint
                + 3 * Math.pow(1 - t, 2) * t * handle0
                + 3 * (1 - t) * tSq * handle1
                + tCub * endPoint;
        return (float) x;
    }

    /**
     * get spline connecting B and C. A and D are the points before and after BC in the path
     *
     * @param beforeStart
     * @param startPoint
     * @param endPoint
     * @param afterEnd
     */
    public static float[][] getSplinePathFor(float[] beforeStart, float[] startPoint, float[] endPoint,
                                             float[] afterEnd, int positionDigits) {
        assert beforeStart.length == startPoint.length;
        assert startPoint.length == endPoint.length;
        assert endPoint.length == afterEnd.length;
        assert beforeStart.length >= positionDigits;

        int vectorSize = beforeStart.length;

        float distance = PointUtils.getPositionalDistance(startPoint, endPoint, positionDigits);

        //prepare output array of n-vectors
        int pointsInCurve = (int) (estimateCurveSize(beforeStart, startPoint, endPoint, afterEnd, positionDigits) * 2);
        float[][] path = new float[pointsInCurve][];
        for (int i = 0; i < path.length; i++) {
            path[i] = new float[vectorSize];    //new n-vector
        }

        float[] handle1p = getBezierHandle(beforeStart, startPoint, endPoint, positionDigits, distance);
        float[] handle2P = getBezierHandle(afterEnd, endPoint, startPoint, positionDigits, distance);

        for (int n = 0; n < vectorSize; n++) {
            float[] nThPositionCurve = calculateCubicBezier(startPoint[n],
                    n < 2 ? handle1p[n] : startPoint[n],
                    n < 2 ? handle2P[n] : endPoint[n], endPoint[n], pointsInCurve);
            for (int i = 0; i < nThPositionCurve.length; i++) {
                path[i][n] = nThPositionCurve[i];
                if (Float.isNaN(nThPositionCurve[i]))
                    System.out.println("NaN");
                assert !Float.isNaN(nThPositionCurve[i]) : "position is Nan: " + Arrays.toString(path[i]);
            }
        }

        return path;
    }

    public static float estimateCurveSize(float[] pointA, float[] pointB, float[] pointC, float[] pointD,
                                          int positionDigits) {
        float chord = PointUtils.getPositionalDistance(pointD, pointA, positionDigits);
        float cont_net = PointUtils.getPositionalDistance(pointA, pointB, positionDigits) +
                PointUtils.getPositionalDistance(pointC, pointB, positionDigits) +
                PointUtils.getPositionalDistance(pointC, pointD, positionDigits);
        float app_arc_length = (cont_net + chord) / 2f;
        return app_arc_length;
    }

    private static float[] getBezierHandle(float[] pointA, float[] pointB, float[] pointC, int positionDigits,
                                           float distance) {
        float[] out = new float[pointA.length];
        for (int n = 0; n < pointA.length; n++) {
            out[n] = (pointC[n] - pointA[n]) / 2f;
        }

        //normalize and scale positions based on distance, so the catmull rom spline doesnt act crazy
        //equal to handle.normalize.scale(distance/2)
        float length = PointUtils.getPositionalLength(out, positionDigits);
        if (length == 0)
            length = distance;
        for (int i = 0; i < positionDigits; i++) {
            out[i] = out[i] / length * distance / 2f;
        }

        for (int n = 0; n < pointA.length; n++) {
            out[n] += pointB[n];
        }
        return out;
    }

}
