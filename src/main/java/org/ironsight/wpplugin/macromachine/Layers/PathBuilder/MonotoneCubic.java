package org.ironsight.wpplugin.macromachine.Layers.PathBuilder;

/**
 * Monotone cubic interpolation (Fritsch–Carlson / PCHIP): a smooth curve
 * through the knots that never leaves the range of the two knots around it, so
 * it has no over- or undershoot, and runs only downhill where the knots do.
 * Each tangent depends only on its neighbours (local: moving a knot changes its
 * four surrounding segments). Setup O(n), each value O(1) when evaluated in
 * order.
 */
final class MonotoneCubic
{
    private MonotoneCubic() {
    }

    /**
     * The curve through {@code values} at the increasing {@code knots}, evaluated
     * at the increasing {@code at}; constant before the first and after the last
     * knot. Knots closer than {@code 1e-6} to the previous one are skipped.
     */
    static float[] interpolate(float[] knots, float[] values, float[] at) {
        // drop coincident knots (no length to interpolate over)
        int n = 0;
        double[] x = new double[knots.length], y = new double[knots.length];
        for (int i = 0; i < knots.length; i++) {
            if (n > 0 && knots[i] - x[n - 1] < 1e-6)
                continue;
            x[n] = knots[i];
            y[n] = values[i];
            n++;
        }
        float[] out = new float[at.length];
        if (n == 0)
            return out;
        if (n == 1) {
            java.util.Arrays.fill(out, (float) y[0]);
            return out;
        }
        double[] slope = new double[n - 1], width = new double[n - 1];
        for (int k = 0; k < n - 1; k++) {
            width[k] = x[k + 1] - x[k];
            slope[k] = (y[k + 1] - y[k]) / width[k];
        }
        // tangents: zero at a peak, a valley or a flat neighbour; else the weighted
        // harmonic mean of both slopes (Fritsch–Butland), which keeps every segment
        // monotone; the ends take their segment's slope
        double[] tangent = new double[n];
        tangent[0] = slope[0];
        tangent[n - 1] = slope[n - 2];
        for (int k = 1; k < n - 1; k++) {
            double before = slope[k - 1], after = slope[k];
            if (before * after <= 0) {
                tangent[k] = 0;
            } else {
                double w1 = 2 * width[k] + width[k - 1], w2 = width[k] + 2 * width[k - 1];
                tangent[k] = (w1 + w2) / (w1 / before + w2 / after);
            }
        }
        int segment = 0;
        for (int i = 0; i < at.length; i++) {
            double s = at[i];
            if (s <= x[0]) {
                out[i] = (float) y[0];
                continue;
            }
            if (s >= x[n - 1]) {
                out[i] = (float) y[n - 1];
                continue;
            }
            if (s < x[segment])
                segment = 0; // not in order: search again from the start
            while (s > x[segment + 1])
                segment++;
            double h = width[segment], t = (s - x[segment]) / h, t2 = t * t, t3 = t2 * t;
            out[i] = (float) ((2 * t3 - 3 * t2 + 1) * y[segment] + (t3 - 2 * t2 + t) * h * tangent[segment]
                    + (-2 * t3 + 3 * t2) * y[segment + 1] + (t3 - t2) * h * tangent[segment + 1]);
        }
        return out;
    }
}
