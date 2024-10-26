package org.demo.wpplugin.operations;

import org.demo.wpplugin.geometry.HeightDimension;
import org.demo.wpplugin.operations.River.RiverHandleInformation;
import org.demo.wpplugin.pathing.Path;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;

import static org.demo.wpplugin.pathing.Path.interpolateWaterZ;

public class ContinuousCurve {
    private final int length;
    private final ArrayList<float[]> flatCurve;
    private final HashMap<RiverHandleInformation.RiverInformation, Float> maxima = new HashMap<>();
    private final HashMap<RiverHandleInformation.RiverInformation, Float> minima = new HashMap<>();

    public ContinuousCurve(ArrayList<float[]> flatCurve) {
        if (flatCurve.isEmpty()) {
            this.length = 0;
        } else {
            this.length = flatCurve.get(0).length;
        }

        for (RiverHandleInformation.RiverInformation info :RiverHandleInformation.RiverInformation.values()) {
            float[] curve = flatCurve.get(info.idx + 2 /* position */);
            assert curve.length == length;

            {
                float max = Float.MIN_VALUE;
                float min = Float.MAX_VALUE;
                for (float f: curve) {
                    max = Math.max(f,max);
                    min = Math.min(f,min);
                }
                maxima.put(info, max);
                minima.put(info, min);
            }
        }
        this.flatCurve = flatCurve;
    }

    public float getMax(RiverHandleInformation.RiverInformation information) {
        return maxima.getOrDefault(information,Float.MIN_VALUE);
    }

    public float getMin(RiverHandleInformation.RiverInformation information) {
        return minima.getOrDefault(information,Float.MAX_VALUE);
    }

    public static ContinuousCurve fromPath(Path path, HeightDimension dimension) {
        if (path.amountHandles() < 4)
            return new ContinuousCurve(new ArrayList<>());
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

    public float[] terrainCurve(HeightDimension dim) {
        float[] terrainCurve = new float[this.curveLength()];
        for (int i = 0; i < this.curveLength(); i++) {
            terrainCurve[i] = dim.getHeight(this.getPosX(i), this.getPosY(i));
        }
        return terrainCurve;
    }

    public float[] getWaterCurve(HeightDimension dim) {
        return Path.interpolateWaterZ(this, dim);
    }

    private float[] interpolateType(RiverHandleInformation.RiverInformation type, float[] handle,
                                    int[] handleToCurveIdx, HeightDimension dimension) {
        switch (type) {
            case WATER_Z:
                interpolateWaterZ(null, dimension);
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
        return (int)(flatCurve.get(idx)[curveIdx]);
    }

    public int getPosY(int curveIdx) {
        int idx = 1;
        assert idx < flatCurve.size();
        return (int)(flatCurve.get(idx)[curveIdx]);
    }

    public Point getPos(int curveIdx) {
        return new Point(getPosX(curveIdx), getPosY(curveIdx));
    }

    public Point[] getPositions2d() {
        Point[] positions = new Point[curveLength()];
        float[] xPos = flatCurve.get(0);
        float[] yPos = flatCurve.get(1);
        if (curveLength() < 2)
            return positions;

        for (int i = 0; i < positions.length; i++) {
            positions[i] = new Point((int)xPos[i],(int)yPos[i]);
        }
        return positions;
    }
}
