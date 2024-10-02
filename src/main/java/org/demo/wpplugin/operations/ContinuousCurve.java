package org.demo.wpplugin.operations;

import org.demo.wpplugin.operations.River.RiverHandleInformation;

import java.awt.*;
import java.util.ArrayList;

public class ContinuousCurve {
    private final int length;
    private final ArrayList<float[]> flatCurve;

    public ContinuousCurve(ArrayList<float[]> flatCurve) {
        if (flatCurve.isEmpty()) {
            this.length = 0;
        } else {
            this.length = flatCurve.get(0).length;

        }

        for (float[] curve : flatCurve) {
            assert curve.length == length;
        }
        this.flatCurve = flatCurve;
    }

    public float[] getInfo(RiverHandleInformation.RiverInformation information) {
        int idx = RiverHandleInformation.PositionSize.SIZE_2_D.value + information.idx;
        assert idx >= 0 && idx < flatCurve.size();
        return flatCurve.get(idx);
    }

    public float getInfo(RiverHandleInformation.RiverInformation information, int curveIdx) {
        int idx = RiverHandleInformation.PositionSize.SIZE_2_D.value + information.idx;
        assert idx >= 0 && idx < flatCurve.size();
        return flatCurve.get(idx)[curveIdx];
    }

    public int curveLength() {
        return length;
    }

    public int getPosX(int curveIdx) {
        int idx = 1;
        assert idx < flatCurve.size();
        return Math.round(flatCurve.get(idx)[curveIdx]);
    }

    public int getPosY(int curveIdx) {
        int idx = 1;
        assert idx < flatCurve.size();
        return Math.round(flatCurve.get(idx)[curveIdx]);
    }

    public Point getPos(int curveIdx) {
        return new Point(getPosX(curveIdx), getPosY(curveIdx));
    }

    public Point[] getPositions2d() {
        Point[] positions = new Point[curveLength()];
        if (curveLength() < 2)
            return positions;

        for (int i = 0; i < positions.length; i++) {
            positions[i] = getPos(i);
        }
        return positions;
    }
}
