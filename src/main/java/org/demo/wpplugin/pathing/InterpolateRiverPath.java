package org.demo.wpplugin.pathing;

import org.demo.wpplugin.operations.River.RiverHandleInformation;

import java.util.HashMap;

public class InterpolateRiverPath {
    private final HashMap<RiverHandleInformation.RiverInformation, InterpolateList> curves = new HashMap<>();

    private final PointFInterpolateLinearList positions = new PointFInterpolateLinearList();
    private final FloatInterpolateLinearList beachRadius = new FloatInterpolateLinearList();
    private final FloatInterpolateLinearList riverRadius = new FloatInterpolateLinearList();
    private final FloatInterpolateLinearList riverDepth = new FloatInterpolateLinearList();
    private final FloatInterpolateLinearList transitionRadius = new FloatInterpolateLinearList();
    private final FloatInterpolateLinearList waterZ = new FloatInterpolateLinearList();

    public InterpolateRiverPath() {
        assert invariant();
    }

    private boolean invariant() {
        return true;
    }

    public PointFInterpolateLinearList getPositions() {
        return positions;
    }

    public FloatInterpolateLinearList getBeachRadius() {
        return beachRadius;
    }

    public FloatInterpolateLinearList getRiverRadius() {
        return riverRadius;
    }

    public FloatInterpolateLinearList getRiverDepth() {
        return riverDepth;
    }

    public FloatInterpolateLinearList getTransitionRadius() {
        return transitionRadius;
    }

    public FloatInterpolateLinearList getWaterZ() {
        return waterZ;
    }
}
