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
}
