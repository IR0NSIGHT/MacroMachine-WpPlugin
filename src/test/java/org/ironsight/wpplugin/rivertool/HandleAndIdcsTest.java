package org.ironsight.wpplugin.rivertool;

import org.ironsight.wpplugin.rivertool.operations.ContinuousCurve;
import org.ironsight.wpplugin.rivertool.operations.River.RiverHandleInformation;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class HandleAndIdcsTest {
    @Test
    void removeInheritValues() {
        float[] handles = new float[]{3.0f, 3.0f, RiverHandleInformation.INHERIT_VALUE, RiverHandleInformation.INHERIT_VALUE, RiverHandleInformation.INHERIT_VALUE, RiverHandleInformation.INHERIT_VALUE, RiverHandleInformation.INHERIT_VALUE, RiverHandleInformation.INHERIT_VALUE, RiverHandleInformation.INHERIT_VALUE, RiverHandleInformation.INHERIT_VALUE, RiverHandleInformation.INHERIT_VALUE, 17.0f, 17.0f};
        float[] segmentLengths = new float[handles.length];
        Arrays.fill(segmentLengths, 5);
        int[] handleIdcs = ContinuousCurve.handleToCurve(segmentLengths);

        HandleAndIdcs struct = new HandleAndIdcs(handles, handleIdcs, segmentLengths);
        HandleAndIdcs pure = HandleAndIdcs.removeInheritValues(struct);
        assertArrayEquals(pure.positions, new float[]{3.0f, 3.0f, 17.0f, 17.0f}, 0.01f);
    }



}