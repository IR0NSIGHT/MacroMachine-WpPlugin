package org.ironsight.wpplugin.macromachine.Layers.PathBuilder;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import javax.vecmath.Point4f;
import org.junit.jupiter.api.Test;

class MonotoneCubicTest
{
    private static float[] samples(float from, float to, int count) {
        float[] at = new float[count];
        for (int i = 0; i < count; i++)
            at[i] = from + (to - from) * i / (count - 1);
        return at;
    }

    @Test
    void passesThroughTheKnotsAndIsConstantOutside() {
        float[] knots = {0, 10, 30}, values = {60, 70, 65};
        float[] out = MonotoneCubic.interpolate(knots, values, new float[]{-5, 0, 10, 30, 40});
        assertArrayEquals(new float[]{60, 60, 70, 65, 65}, out, 1e-4f);
    }

    @Test
    void neverOvershootsAPeakOrUndershootsAValley() {
        float[] knots = {0, 10, 20, 30, 40}, values = {60, 80, 60, 40, 60};
        float[] out = MonotoneCubic.interpolate(knots, values, samples(0, 40, 401));
        for (int i = 0; i < out.length; i++) {
            float s = i / 10f;
            int segment = Math.min(3, (int) (s / 10));
            float low = Math.min(values[segment], values[segment + 1]),
                    high = Math.max(values[segment], values[segment + 1]);
            assertTrue(out[i] >= low - 1e-3f && out[i] <= high + 1e-3f, "at " + s + ": " + out[i]);
        }
    }

    @Test
    void descendingKnotsGiveADescendingCurve() {
        float[] knots = {0, 3, 20, 22, 50}, values = {90, 89, 70, 40, 39};
        float[] out = MonotoneCubic.interpolate(knots, values, samples(0, 50, 501));
        for (int i = 1; i < out.length; i++)
            assertTrue(out[i] <= out[i - 1] + 1e-4f, "rises at " + i);
    }

    @Test
    void smoothThroughAMonotoneKnot() {
        // equal slopes on both sides: the curve is a straight line through the knot
        float[] out = MonotoneCubic.interpolate(new float[]{0, 10, 20}, new float[]{0, 10, 20},
                new float[]{5, 9.9f, 10.1f, 15});
        assertArrayEquals(new float[]{5, 9.9f, 10.1f, 15}, out, 1e-3f);
    }

    @Test
    void pathHeightsStayBetweenTheNodesWhateverTheCurveStrength() {
        var nodes = List.of(new Point4f(0, 0, 60, 4), new Point4f(40, 0, 80, 4), new Point4f(80, 30, 60, 8),
                new Point4f(120, 0, 62, 4));
        for (float strength : new float[]{1f, 2f}) {
            var result = PathToolBackend.getPathFromNodes(nodes, strength);
            float lowest = Float.MAX_VALUE, highest = -Float.MAX_VALUE, thinnest = Float.MAX_VALUE,
                    widest = -Float.MAX_VALUE;
            for (var sample : result.path) {
                lowest = Math.min(lowest, sample.z);
                highest = Math.max(highest, sample.z);
                thinnest = Math.min(thinnest, sample.w);
                widest = Math.max(widest, sample.w);
            }
            assertTrue(highest <= 80 + 1e-3f, "no overshoot above the highest node: " + highest);
            assertTrue(lowest >= 60 - 1e-3f, "no undershoot below the lowest node: " + lowest);
            assertTrue(thinnest >= 4 - 1e-3f && widest <= 8 + 1e-3f, "radius between the nodes'");
            for (var node : nodes) {
                Integer index = result.handlesToPathIndex.get(node);
                if (index != null)
                    assertEquals(node.z, result.path.get(index).z, 1e-3f, "through the node");
            }
        }
    }
}
