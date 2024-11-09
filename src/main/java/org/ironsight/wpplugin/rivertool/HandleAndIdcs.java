package org.ironsight.wpplugin.rivertool;

import org.ironsight.wpplugin.rivertool.operations.River.RiverHandleInformation;

public class HandleAndIdcs {
    public final float[] positions;
    public final int[] idcs;
    public final float[] segmentLengths;

    public HandleAndIdcs(float[] positions, int[] idcs, float[] segmentLengths) {
        if (positions.length != idcs.length || idcs.length != segmentLengths.length)
            throw new IllegalArgumentException("all arrays must be of same length.");
        this.positions = positions;
        this.idcs = idcs;
        this.segmentLengths = segmentLengths;
    }

    public static HandleAndIdcs removeInheritValues(HandleAndIdcs struct) {
        boolean[] removeMarkers = new boolean[struct.positions.length];

        for (int i = 0; i < struct.positions.length; i++) {
            if (struct.positions[i] == RiverHandleInformation.INHERIT_VALUE) {
                removeMarkers[i] = true;
            }
        }

        float[] newPositions = ArrayUtility.removePositions(struct.positions, removeMarkers);
        int[] newHandleToCurve = ArrayUtility.removePositions(struct.idcs, removeMarkers);
        float[] newSegmentLengths = ArrayUtility.sumIndices(struct.segmentLengths, removeMarkers);
        HandleAndIdcs out = new HandleAndIdcs(newPositions, newHandleToCurve, newSegmentLengths);

        //postcondition
        assert !ArrayUtility.linearSearch(out.positions, RiverHandleInformation.INHERIT_VALUE) : "array still contains INHERIT values";
        assert out.positions.length <= struct.positions.length;
        assert !ArrayUtility.linearSearch(out.segmentLengths, 0) : "segments can never be zero length";
        return out;
    }
}
