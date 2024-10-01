package org.demo.wpplugin.pathing;

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
}