package org.demo.wpplugin.operations;

import org.demo.wpplugin.geometry.HeightDimension;
import org.demo.wpplugin.operations.River.RiverHandleInformation;
import org.demo.wpplugin.pathing.Path;
import org.demo.wpplugin.pathing.PointInterpreter;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static org.demo.wpplugin.operations.River.RiverHandleInformation.INHERIT_VALUE;

public class ContinuousCurve {
    private final int length;
    private final ArrayList<float[]> flatCurve;
    private final HashMap<RiverHandleInformation.RiverInformation, Float> maxima = new HashMap<>();
    private final HashMap<RiverHandleInformation.RiverInformation, Float> minima = new HashMap<>();
    private final PointInterpreter.PointType type;

    public ContinuousCurve(ArrayList<float[]> flatCurve, PointInterpreter.PointType type) {
        this.type = type;
        if (flatCurve.isEmpty()) {
            this.length = 0;
        } else {
            this.length = flatCurve.get(0).length;
        }

        for (RiverHandleInformation.RiverInformation info : type.information) {
            float[] curve = flatCurve.get(info.idx + type.posSize);
            assert curve.length == length;
            {
                float max = Float.MIN_VALUE;
                float min = Float.MAX_VALUE;
                for (float f : curve) {
                    max = Math.max(f, max);
                    min = Math.min(f, min);
                }
                maxima.put(info, max);
                minima.put(info, min);
            }
        }

        this.flatCurve = flatCurve;
    }

    /**
     * non flat handle list
     *
     * @param handles list of { x y coords}
     * @return
     */
    private static ArrayList<float[]> onlyNonInterpolateHandles(ArrayList<float[]> handles) {
        ArrayList<float[]> out = new ArrayList<>(handles.size());
        for (float[] point : handles) {
            if (point[0] == INHERIT_VALUE || point[1] == INHERIT_VALUE)
                continue;
            out.add(point.clone());
        }
        return out;
    }

    public static ContinuousCurve fromPath(Path path, HeightDimension dimension) {

        assert path.type != null;
        if (path.amountHandles() < 4)
            return new ContinuousCurve(new ArrayList<>(), path.type);


        //handles exist as flat lists, only true coords are used
        ArrayList<float[]> flatHandles = Path.transposeHandles(onlyNonInterpolateHandles(path.getHandles()));
        ArrayList<float[]> interpolatedCurve = new ArrayList<>(flatHandles.size());

        float[] xsPos = flatHandles.get(0);
        float[] ysPos = flatHandles.get(1);
        assert Arrays.binarySearch(xsPos, INHERIT_VALUE) < 0 : "inherit must not be part of the x position " +
                "interpolation";
        assert Arrays.binarySearch(ysPos, INHERIT_VALUE) < 0 : "inherit must not be part of the y position " +
                "interpolation";

        //calculate the handle offsets using catmull rom and padding
        float[] xsHandleOffsets = positionsToHandleOffsetCatmullRom(xsPos);
        float[] ysHandleOffsets = positionsToHandleOffsetCatmullRom(ysPos);
        assert xsPos.length == ysPos.length && xsPos.length == xsHandleOffsets.length && ysPos.length == ysHandleOffsets.length : "positions and halndPerPositon must all be same length";

        int[] handleToCurveIdx = handleToCurve(flatHandles.get(0), flatHandles.get(1));

        //iterate all handleArrays and calculate a continous curve
        for (int n = 0; n < path.type.size; n++) {
            float[] nthHandles = flatHandles.get(n);
            float[] interpolated = Path.interpolateCatmullRom(nthHandles, handleToCurveIdx);
            interpolatedCurve.add(interpolated);
        }
        return new ContinuousCurve(interpolatedCurve, path.type);
    }

    public static int[] handleToCurve(float[] xsPos, float[] ysPos) {
        float[] xsOff = positionsToHandleOffsetCatmullRom(xsPos);
        float[] ysOff = positionsToHandleOffsetCatmullRom(ysPos);
        //we know the positions of each handle already through magic
        int[] handleToCurveIdx = new int[xsPos.length];
        int[] segmentSizes = Path.estimateSegmentLengths(xsPos, ysPos, xsOff, ysOff);
        for (int i = 0; i < segmentSizes.length; i++)
            handleToCurveIdx[i + 1] = handleToCurveIdx[i] + segmentSizes[0];
        return handleToCurveIdx;
    }

    public static float[] positionsToHandleOffsetCatmullRom(float[] positions) {
        float[] handleOffsets = new float[positions.length];
        //catmull rom from neighbours where possible
        for (int i = 1; i < handleOffsets.length - 1; i++) {
            handleOffsets[i] = (positions[i + 1] - positions[i - 1]) / 4f;    //TODO this might cause overshooting if
            // one of the two neighbours is very far away
        }
        //first and last pos only have 1 neighbour
        handleOffsets[0] = (positions[1] - positions[0]) / 2f;
        int l = handleOffsets.length;
        handleOffsets[l - 1] = (positions[l - 1] - positions[l - 2]) / 2f;
        return handleOffsets;
    }

    public boolean isConnectedToPrevious(int idx) {
        boolean connected = (getPosX(idx) == getPosX(idx - 1) || getPosY(idx) == getPosY(idx - 1));
        return connected;
    }

    /**
     * are all points of the curve connected to a previous neighbour on x or y axis
     * -> no diagonal jumps, no points spaced further than euclid distance 1
     *
     * @return
     */
    public boolean isConnectedCurve() {
        for (int i = 1; i < curveLength(); i++) {
            if (!isConnectedToPrevious(i)) {
                return false;
            }
        }
        return true;
    }

    public float getMax(RiverHandleInformation.RiverInformation information) {
        return maxima.getOrDefault(information, Float.MIN_VALUE);
    }

    public float getMin(RiverHandleInformation.RiverInformation information) {
        return minima.getOrDefault(information, Float.MAX_VALUE);
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

    public float[] getInfo(RiverHandleInformation.RiverInformation information) {
        int idx = this.type.posSize + information.idx;
        assert idx >= 0 && idx < flatCurve.size();
        return flatCurve.get(idx);
    }

    public float getInfo(RiverHandleInformation.RiverInformation information, int curveIdx) {
        int idx = this.type.posSize + information.idx;
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

    public float[] getPositionsX() {
        return flatCurve.get(0).clone();
    }

    public float[] getPositionsY() {
        return flatCurve.get(1).clone();
    }

    public Point[] getPositions2d() {
        Point[] positions = new Point[curveLength()];
        float[] xPos = flatCurve.get(0);
        float[] yPos = flatCurve.get(1);
        if (curveLength() < 2)
            return positions;

        for (int i = 0; i < positions.length; i++) {
            positions[i] = new Point(Math.round(xPos[i]), Math.round(yPos[i]));
        }
        return positions;
    }
}
