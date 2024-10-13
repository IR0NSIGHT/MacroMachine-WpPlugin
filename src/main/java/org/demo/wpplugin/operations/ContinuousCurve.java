package org.demo.wpplugin.operations;

import org.demo.wpplugin.geometry.HeightDimension;
import org.demo.wpplugin.operations.River.RiverHandleInformation;
import org.demo.wpplugin.pathing.Path;

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

    public static ContinuousCurve fromPath(Path path, HeightDimension dimension) {
        ArrayList<float[]> handles = new ArrayList<>(path.amountHandles());
        for (float[] handle : path)
            handles.add(handle.clone());

        //we know the positions of each handle already through magic
        int[] handleToCurveIdx = path.handleToCurveIdx(true);

        //handles exist as flat lists
        ArrayList<float[]> flatHandles = Path.transposeHandles(handles);
        ArrayList<float[]> interpolatedCurve = new ArrayList<>(flatHandles.size());

        //iterate all handleArrays and calculate a continous curve
        for (int n = 0; n < path.type.size; n++) {
            float[] nthHandles = flatHandles.get(n);
            float[] interpolated = Path.interpolateCatmullRom(nthHandles, handleToCurveIdx);
            interpolatedCurve.add(interpolated);
        }
        return new ContinuousCurve(interpolatedCurve);
    }

    private float[] interpolateType(RiverHandleInformation.RiverInformation type, float[] handle,
                                    int[] handleToCurveIdx, HeightDimension dimension) {
        switch (type) {
            case WATER_Z:
                Path.interpolateWaterZ(null, dimension);
            case RIVER_RADIUS:
            case TRANSITION_RADIUS:
            case BEACH_RADIUS:
            case RIVER_DEPTH:
                return Path.interpolateCatmullRom(handle, handleToCurveIdx);
        }
        throw new IllegalArgumentException("unknown river type: " + type);
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
        int idx = 0;
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
