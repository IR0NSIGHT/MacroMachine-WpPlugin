package org.demo.wpplugin.operations.River;

import org.demo.wpplugin.pathing.Interpolatable;

public class RiverHandleInformation implements Interpolatable {
    public final int riverRadius;
    public final float riverDepth;
    public final int beachRadius;
    public final int transitionRadius;

    public RiverHandleInformation(int riverRadius, float riverDepth, int beachRadius, int transitionRadius) {
        this.riverRadius = riverRadius;
        this.riverDepth = riverDepth;
        this.beachRadius = beachRadius;
        this.transitionRadius = transitionRadius;
    }

    public static float[] riverInformation(int x, int y, float riverRadius, float riverDepth, int beachRadius,
                                           int transitionRadius) {
        return new float[]{x, y, riverRadius, riverDepth, beachRadius, transitionRadius};
    }

    public static float[] riverInformation(int x, int y) {
        return new float[]{x, y, 10, 3, 5, 25};
    }

    @Override
    public Interpolatable interpolateWith(float t, Interpolatable other) {
        if (other instanceof RiverHandleInformation) {
            return new RiverHandleInformation(
                    Math.round((t - 1) * riverRadius + t * ((RiverHandleInformation) other).riverRadius),
                    (t - 1) * riverDepth + t * ((RiverHandleInformation) other).riverDepth,
                    Math.round((t - 1) * beachRadius + t * ((RiverHandleInformation) other).beachRadius),
                    Math.round((t - 1) * transitionRadius + t * ((RiverHandleInformation) other).transitionRadius));
        }

        throw new IllegalArgumentException("can not interpolate with this" + this + " other type" + other);
    }

    public enum RiverInformation {
        RIVER_RADIUS(0),
        RIVER_DEPTH(1),
        BEACH_RADIUS(2),
        TRANSITION_RADIUS(3);
        public final int idx;

        RiverInformation(int idx) {
            this.idx = idx;
        }
    }
    public enum PositionSize {
        SIZE_1_D(1),
        SIZE_2_D(2),
        SIZE_3_D(3);
        public final int value;

        PositionSize(int idx) {
            this.value = idx;
        }
    }
}
