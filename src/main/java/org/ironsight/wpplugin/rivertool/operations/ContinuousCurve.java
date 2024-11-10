package org.ironsight.wpplugin.rivertool.operations;

import org.ironsight.wpplugin.rivertool.ArrayUtility;
import org.ironsight.wpplugin.rivertool.CatMullRomInterpolation;
import org.ironsight.wpplugin.rivertool.geometry.HeightDimension;
import org.ironsight.wpplugin.rivertool.operations.River.RiverHandleInformation;
import org.ironsight.wpplugin.rivertool.pathing.MapPointAction;
import org.ironsight.wpplugin.rivertool.pathing.Path;
import org.ironsight.wpplugin.rivertool.pathing.PointInterpreter;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

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

    public static ContinuousCurve fromPath(Path path, HeightDimension dimension) {
        assert path.type != null;

        if (path.amountHandles() == 0)
            return new ContinuousCurve(new ArrayList<>(), path.type);

        //handles exist as flat lists, only true coords are used
        ArrayList<float[]> flatHandles = ArrayUtility.transposeMatrix(onlyNonInterpolateHandles(path.getHandles()));
        ArrayList<float[]> interpolatedCurve = new ArrayList<>(flatHandles.size());

        float[] xsPos = flatHandles.get(0);
        float[] ysPos = flatHandles.get(1);
        assert Arrays.binarySearch(xsPos, RiverHandleInformation.INHERIT_VALUE) < 0 : "inherit must not be part of " +
                "the x position " +
                "interpolation";
        assert Arrays.binarySearch(ysPos, RiverHandleInformation.INHERIT_VALUE) < 0 : "inherit must not be part of " +
                "the y position " +
                "interpolation";

        //calculate the handle offsets using catmull rom and padding
        float[] xsHandleOffsets = positionsToHandleOffsetCatmullRom(xsPos);
        float[] ysHandleOffsets = positionsToHandleOffsetCatmullRom(ysPos);
        assert xsPos.length == ysPos.length && xsPos.length == xsHandleOffsets.length && ysPos.length == ysHandleOffsets.length : "positions and halndPerPositon must all be same length";

        int[] segmentLengths = CatMullRomInterpolation.estimateSegmentLengths(flatHandles.get(0), flatHandles.get(1),
                xsHandleOffsets, ysHandleOffsets);
        int[] handleToCurveIdx = handleToCurve(segmentLengths);

        //iterate all handleArrays and calculate a continous curve
        for (int n = 0; n < path.type.size; n++) {
            float[] nthHandles = flatHandles.get(n);
            float[] interpolated = CatMullRomInterpolation.interpolateCatmullRom(nthHandles, handleToCurveIdx,
                    segmentLengths);
            interpolatedCurve.add(interpolated);
        }

        ArrayList<float[]> continuousFlats = makeContinuous(interpolatedCurve);

        return new ContinuousCurve(continuousFlats, path.type);
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
            if (point[0] == RiverHandleInformation.INHERIT_VALUE || point[1] == RiverHandleInformation.INHERIT_VALUE)
                continue;
            out.add(point.clone());
        }
        return out;
    }

    public static float[] positionsToHandleOffsetCatmullRom(float[] positions) {
        float[] handleOffsets = new float[positions.length];

        //catmull rom from neighbours where possible
        for (int i = 1; i < handleOffsets.length - 1; i++) {
            handleOffsets[i] = (positions[i + 1] - positions[i - 1]) / 4f;    //TODO this might cause overshooting if
            // one of the two neighbours is very far away
        }

        //first and last pos only have 1 neighbour
        if (handleOffsets.length > 2) {
            handleOffsets[0] = (positions[1] - positions[0]) / 2f;
            int l = handleOffsets.length;
            handleOffsets[l - 1] = (positions[l - 1] - positions[l - 2]) / 2f;
        } else {
            //zero or one position
            Arrays.fill(handleOffsets, 0);
        }
        return handleOffsets;
    }

    public static int[] handleToCurve(int[] segmentSizes) {
        int[] handleToCurveIdx = new int[segmentSizes.length];
        for (int i = 0; i < segmentSizes.length - 1; i++)
            handleToCurveIdx[i + 1] = handleToCurveIdx[i] + segmentSizes[i];

        for (int i = 1; i < handleToCurveIdx.length; i++)
            assert handleToCurveIdx[i] > handleToCurveIdx[i - 1] : "not strictly monotone";
        return handleToCurveIdx;
    }

    public static ArrayList<float[]> makeContinuous(ArrayList<float[]> flatCurveWithHoles) {
        ArrayList<float[]> positions = ArrayUtility.transposeMatrix(flatCurveWithHoles);
        positions = roundHandles(positions);
        ArrayList<float[]> continuousPositions = new ArrayList<>();
        continuousPositions.add(positions.get(0));
        for (int i = 1; i < positions.size(); i++) {
            float[] previous = positions.get(i-1);
            float[] thisPos = positions.get(i);
            float xDiff = Math.abs(thisPos[0] - previous[0]);
            float yDiff = Math.abs(thisPos[1] - previous[1]);
            float range = Math.max(xDiff, yDiff);

            //fill holes:
            for (int step = 1; step < range; step++) {
                float t = step / range;
                float[] newPoint = linearInterpolate(previous, thisPos, t);
                newPoint[0] = Math.round(newPoint[0]);
                newPoint[1] = Math.round(newPoint[1]);
                continuousPositions.add(newPoint);  //contains previous up to one before thisPos
            }
            continuousPositions.add(thisPos);

        }
        continuousPositions.add(positions.get(positions.size() - 1));

        continuousPositions = connectDiagonals(continuousPositions);

        assert continuousPositions.size() >= positions.size();

        return ArrayUtility.transposeMatrix(continuousPositions);
    }

    public static ArrayList<float[]> roundHandles(ArrayList<float[]> handles) {
        MapPointAction a = new MapPointAction() {
            @Override
            public float[] map(float[] point, int index) {
                point[0] = Math.round(point[0]);
                point[1] = Math.round(point[1]);
                return point;
            }
        };

        Path p = new Path(handles, PointInterpreter.PointType.RIVER_2D);
        Path rounded = p.mapPoints(a);
        ArrayList<float[]> roundedHandles = new ArrayList<>();
        roundedHandles.add(p.handleByIndex(0));
        for (float[] handle : rounded) {
            float[] previous = roundedHandles.get(roundedHandles.size() - 1);
            if (handle[0] == previous[0] && handle[1] == previous[1])
                continue;
            roundedHandles.add(handle);
        }
        return roundedHandles;
    }

    public static float[] linearInterpolate(float[] a, float[] b, float t) {
        float[] out = new float[a.length];
        for (int n = 0; n < out.length; n++) {
            out[n] = (1 - t) * a[n] + t * b[n];
        }
        return out;
    }

    public static ArrayList<float[]> connectDiagonals(ArrayList<float[]> handles) {
        ArrayList<float[]> outHandles = new ArrayList<>();
        outHandles.add(handles.get(0));
        for (int i = 1; i < handles.size(); i++) {
            float[] existing = handles.get(i - 1);
            float[] next = handles.get(i);
            if (existing[0] - next[0] != 0 && existing[1] - next[1] != 0) {
                float[] diag = linearInterpolate(existing,next,0.5f);
                //mix x coord so they are connected
                diag[0] = existing[0];
                diag[1] = next[1];
                outHandles.add(diag);
            }
            outHandles.add(next);
        }
        return outHandles;
        }

    public static int[] handleToCurve(Path p) {
        ArrayList<float[]> handles = p.getHandles();
        ArrayList<float[]> flatHandles = ArrayUtility.transposeMatrix(handles);
        return handleToCurve(flatHandles.get(0), flatHandles.get(1));
    }

    public static int[] handleToCurve(float[] xsPos, float[] ysPos) {
        float[] xsOff = positionsToHandleOffsetCatmullRom(xsPos);
        float[] ysOff = positionsToHandleOffsetCatmullRom(ysPos);
        //we know the positions of each handle already through magic
        int[] segmentSizes = CatMullRomInterpolation.estimateSegmentLengths(xsPos, ysPos, xsOff, ysOff);
        return handleToCurve(segmentSizes);
    }

    public static int[] handleToCurve(float[] segmentSizes) {
        float[] handleToCurveIdx = new float[segmentSizes.length];
        for (int i = 0; i < segmentSizes.length - 1; i++)
            handleToCurveIdx[i + 1] = handleToCurveIdx[i] + segmentSizes[i];

        int[] handlesIdcsOut = new int[handleToCurveIdx.length];
        for (int i = 0; i < handleToCurveIdx.length; i++)
            handlesIdcsOut[i] = Math.round(handleToCurveIdx[i]);


        for (int i = 1; i < handlesIdcsOut.length; i++)
            assert handlesIdcsOut[i] > handlesIdcsOut[i - 1] :
                    "not strictly monotone: " + Arrays.toString(handlesIdcsOut);

        return handlesIdcsOut;
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

    public int curveLength() {
        return length;
    }

    public boolean isConnectedToPrevious(int idx) {
        boolean connected = (getPosX(idx) == getPosX(idx - 1) || getPosY(idx) == getPosY(idx - 1));
        return connected;
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
