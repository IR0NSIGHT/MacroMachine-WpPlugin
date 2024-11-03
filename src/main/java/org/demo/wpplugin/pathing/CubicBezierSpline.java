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
     *
     * @param pos0 pos 0
     * @param handle0 handle 0
     * @param handle1 handle 1
     * @param pos1 pos 1
     * @param positionDigits
     * @return
     */
    public static float estimateCurveSize(float[] pos0, float[] handle0, float[] handle1, float[] pos1,
                                          int positionDigits) {

        float[] xPoints = calculateCubicBezier(pos0[0],handle0[0],handle1[0], pos1[0],30+1);
        float[] yPoints =calculateCubicBezier(pos0[1],handle0[1],handle1[1], pos1[1],30+1);
        double totalLength = 0;
        for (int i = 0; i < xPoints.length-1; i++) {
            float xDiff = xPoints[i] - xPoints[i + 1];
            xDiff *= xDiff;
            float yDiff = yPoints[i] - yPoints[i + 1];
            yDiff *= yDiff;
            double distanceBetweenNeighbours = Math.sqrt(xDiff + yDiff);
            totalLength += distanceBetweenNeighbours;
        }

        /*
        float chord = PointUtils.getPositionalDistance(pos1, pos0, positionDigits);
        float cont_net = PointUtils.getPositionalDistance(pos0, handle0, positionDigits) +
                PointUtils.getPositionalDistance(handle1, handle0, positionDigits) +
                PointUtils.getPositionalDistance(handle1, pos1, positionDigits);
        */
        return (float)totalLength;
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
