package org.demo.wpplugin.pathing;

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
            float t = (float) i / numPoints;
            points[i] = calcuateCubicBezier(startPoint, handle0, handle1, endPoint, t);
        }
        return points;
    }

    public static float calcuateCubicBezier(float startPoint, float handle0, float handle1, float endPoint, float t) {
        double tSq = t * t, tCub = tSq * t;
        double x = Math.pow(1 - t, 3) * startPoint
                + 3 * Math.pow(1 - t, 2) * t * handle0
                + 3 * (1 - t) * tSq * handle1
                + tCub * endPoint;
        return (float) x;
    }

    public static float getHalfWay(float A, float C) {
        // (AC)/2
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

        float distance = PointUtils.getPositionalDistance(B, C, positionDigits);

        //prepare output array of n-vectors
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

    public static float estimateCurveSize(float[] pointA, float[] pointB, float[] pointC, float[] pointD, int positionDigits) {
        float chord = PointUtils.getPositionalDistance(pointD, pointA, positionDigits);
        float cont_net = PointUtils.getPositionalDistance(pointA, pointB, positionDigits)+
                PointUtils.getPositionalDistance(pointC, pointB, positionDigits)+
                PointUtils.getPositionalDistance(pointC, pointD, positionDigits);
        float app_arc_length = (cont_net+chord)/2f;
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
        for (int i = 0; i < positionDigits; i++) {
            out[i] = out[i] / length * distance / 2f;
        }

        for (int n = 0; n < pointA.length; n++) {
            out[n] += pointB[n];
        }
        return out;
    }

}
