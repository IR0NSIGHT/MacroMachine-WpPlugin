package org.ironsight.wpplugin.rivertool.pathing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CubicBezierSplineTest {

    @Test
    void calcuateCubicBezier() {
        //calculate a straight linear curve
        float start = 20;
        float end = 30;

        float tangent = (end-start);

        float handle0 = start + (tangent / 3f);
        float handle1 = end - (tangent / 3f);

        for (float t = 0; t < 1f; t += 0.05f) {
            float interpolated = CubicBezierSpline.calcuateCubicBezier(start, handle0, handle1, end, t);
            assertEquals( interpolated, (t*end)+(1-t)*start,0.001f);
        }
    }

    @Test
    void estimateCurveSize() {
        {
            float[] start = new float[]{10, 20};
            float[] end = new float[]{20, 20};
            float[] handle0 = new float[]{13.333f, 20};
            float[] handle1 = new float[]{16.666f, 20}; //straight diagonal line
            float length = CubicBezierSpline.estimateCurveSize(start, handle0, handle1, end, 2);
            assertEquals(length, 10f, 0.01f);
        }

        {
            float[] start = new float[]{10, 20};
            float[] end = new float[]{20, 30};
            float[] handle0 = new float[]{13.333f, 23.333f};
            float[] handle1 = new float[]{16.666f, 26.666f}; //straight diagonal line
            float length = CubicBezierSpline.estimateCurveSize(start, handle0, handle1, end, 2);
            assertEquals(length, Math.sqrt(2) * 10f, 0.01f);
        }
    }
}