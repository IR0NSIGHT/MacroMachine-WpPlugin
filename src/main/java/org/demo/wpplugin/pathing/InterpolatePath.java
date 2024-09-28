package org.demo.wpplugin.pathing;

import org.demo.wpplugin.operations.River.RiverHandleInformation;

import java.util.HashMap;

public class InterpolatePath {
    private final HashMap<RiverHandleInformation.RiverInformation, InterpolateList> curves = new HashMap<>();

    public InterpolatePath() {
        curves.put(RiverHandleInformation.RiverInformation.BEACH_RADIUS, new FloatInterpolateLinearList());
        curves.put(RiverHandleInformation.RiverInformation.RIVER_RADIUS, new FloatInterpolateLinearList());
        curves.put(RiverHandleInformation.RiverInformation.RIVER_DEPTH, new FloatInterpolateLinearList());
        curves.put(RiverHandleInformation.RiverInformation.TRANSITION_RADIUS, new FloatInterpolateLinearList());
        curves.put(RiverHandleInformation.RiverInformation.WATER_Z, new FloatInterpolateLinearList());

        assert invariant();
    }

    private boolean invariant() {
        return true;
    }

    public InterpolateList getInformation(RiverHandleInformation.RiverInformation information) {
        if (!curves.containsKey(information)) {
            throw new IllegalArgumentException("this path does not contain interpolated for this type of information");
        }
        return curves.get(information);
    }
}
