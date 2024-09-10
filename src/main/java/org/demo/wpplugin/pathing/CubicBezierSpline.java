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
        assert A.length <= positionDigits;

        int vectorSize = A.length;

        float distance = getPositionalDistance(B, C, positionDigits);

        //prepare output array of n-vectors
        int pointsInCurve = (int)(Math.pow(distance,1.5));
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

        //    pathX = calculateCubicBezier(B.x, handle1p.x, handle2P.x, C.x, (int) (length / metersBetweenPoints));
        //    pathY = calculateCubicBezier(B.y, handle1p.y, handle2P.y, C.y, (int) (length / metersBetweenPoints));

        //    path = new Point[pathX.length];
        //    for (int i = 0; i < pathX.length; i++) {
        //        path[i] = new Point(Math.round(pathX[i]), Math.round(pathY[i]));
        //    }
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

    private static float getPositionalDistance(float[] pointA, float[] pointB, int positionDigits) {
        //euclidian distance of B and C positions
        float dist = 0;
        for (int i = 0; i < positionDigits; i++) {
            float distI = pointA[i] - pointB[i];
            dist += distI * distI;
        }
        dist = (float) Math.sqrt(dist);
        return dist;
    }

    private static float[] getBezierHandle(float[] pointA, float[] pointB, float[] pointC, int positionDigits,
                                           float distance) {
        float[] out = new float[pointA.length];
        for (int n = 0; n < pointA.length; n++) {
            out[n] =  (pointC[n] - pointA[n]) / 2f;
        }

        //normalize and scale positions based on distance, so the catmull rom spline doesnt act crazy
        //equal to handle.normalize.scale(distance/2)
        float length = getPositionalLength(out, positionDigits);
        for (int i = 0; i < positionDigits; i++) {
            out[i] = out[i] / length * distance/2f;
        }

        for (int n = 0; n < pointA.length; n++) {
            out[n] += pointB[n];
        }
        return out;
    }
}
