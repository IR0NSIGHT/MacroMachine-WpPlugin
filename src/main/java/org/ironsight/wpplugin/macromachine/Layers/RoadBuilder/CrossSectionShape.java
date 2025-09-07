package org.ironsight.wpplugin.macromachine.Layers.RoadBuilder;

/**
 * implementation of a 1d function f(t) to construct a filter strength for t=distance to something must be continuous, can NOT be discrete
 */
public abstract class CrossSectionShape {
    private final String name;
    private final String description;

    public CrossSectionShape(String name, String description) {
        this.name = name;
        this.description = description;
    }

    String getName() {
        return name;
    }

    String getDescription() {
        return description;
    }

    /**
     * @param t in range [0,1]
     * @return strength at t
     */
    public float getStrengthAt(float t) {
        return t;
    }

    /**
     * @param n
     * @return array with values of function where arr[0] = f(0) and arr[arr.length-1] = f(1)
     */
    public final float[] asArray(int n) {
        float[] values = new float[n];
        for (int i = 0; i < n; i++) {
            float t = 1f * i / (n - 1); // fraction to produce [0,n] as range
            values[i] = getStrengthAt(t);
        }
        return values;
    }

    @Override
    public String toString() {
        return getName();
    }
}
