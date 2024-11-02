package org.demo.wpplugin.pathing;

import org.demo.wpplugin.geometry.HeightDimension;
import org.demo.wpplugin.operations.ContinuousCurve;
import org.demo.wpplugin.operations.River.RiverHandleInformation;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;

import static org.demo.wpplugin.operations.River.RiverHandleInformation.INHERIT_VALUE;
import static org.demo.wpplugin.operations.River.RiverHandleInformation.RiverInformation.WATER_Z;
import static org.demo.wpplugin.operations.River.RiverHandleInformation.validateRiver2D;
import static org.demo.wpplugin.pathing.PointUtils.arePositionalsEqual;

public class Path implements Iterable<float[]> {
    public final PointInterpreter.PointType type;
    private final ArrayList<float[]> handles;

    public Path(List<float[]> handles, PointInterpreter.PointType type) {
        this.handles = new ArrayList<>(handles.size());
        for (float[] handle : handles) {
            this.handles.add(handle.clone());
        }
        this.type = type;
        assert invariant() : "path invariant hurt";
    }

    public static Path newFilledPath(int length, PointInterpreter.PointType type) {
        Path p = new Path(Collections.EMPTY_LIST, type);
        for (int i = 0; i < length; i++) {
            float[] newHandle = new float[type.size];
            newHandle[0] = 3 * i;
            newHandle[1] = 4 * i;
            for (int n = 2; n < type.size; n++) {
                newHandle[n] = 27;
            }
            p = p.addPoint(newHandle.clone());
        }
        return p;
    }

    public static float[] interpolateWaterZ(ContinuousCurve curve, HeightDimension dim) {
        float[] out = new float[curve.curveLength()];
        Point first = curve.getPositions2d()[0];
        out[0] = Math.min(curve.getInfo(WATER_Z)[0], dim.getHeight(first.x, first.y));
        Point[] positions = curve.getPositions2d();
        for (int i = 1; i < curve.curveLength(); i++) {
            float curveZ = curve.getInfo(WATER_Z)[i];
            int x = positions[i].x;
            int y = positions[i].y;
            float terrainZ = dim.getHeight(x, y);
            float previousZ = out[i - 1];

            out[i] = Math.min(Math.min(previousZ, terrainZ), curveZ);
        }

        return out;
    }

    public ArrayList<float[]> getHandles() {
        return handles;
    }

    private boolean invariant() {
        boolean okay = true;

        float[] setValues = new float[type.size];
        Arrays.fill(setValues, INHERIT_VALUE);
        for (float[] handle : handles) {
            for (int n = 0; n < type.size; n++) {
                if (Float.isNaN(handle[n]))
                    return false;
                if (setValues[n] == INHERIT_VALUE)
                    setValues[n] = handle[n];
            }

            if (handle.length != type.size) {
                System.err.println("path has a handle with wrong size for type " + type + " expected " + type.size +
                        " but got " + Arrays.toString(handle));
                okay = false;
            }
        }

        if (okay && this.type == PointInterpreter.PointType.RIVER_2D)
            okay = validateRiver2D(handles);

        return okay;
    }

    public Path addPoint(float[] point) {
        Path sum = new Path(this.handles, this.type);
        sum.handles.add(point);
        assert invariant();
        return sum;
    }

    public Path newEmpty() {
        return new Path(Collections.EMPTY_LIST, this.type);
    }

    public Path removePoint(float[] point) {
        Path sum = new Path(this.handles, this.type);
        int idx = sum.indexOfPosition(point);
        sum.handles.remove(idx);
        assert sum.invariant();
        return sum;
    }

    public int indexOfPosition(float[] p) {
        for (int i = 0; i < handles.size(); i++) {
            if (arePositionalsEqual(p, handles.get(i), RiverHandleInformation.PositionSize.SIZE_2_D.value)) return i;
        }
        return -1;
    }

    public Path overwriteHandle(float[] original, float[] newValue) {
        Path sum = new Path(this.handles, this.type);
        int idx = indexOfPosition(original);
        sum.handles.set(idx, newValue);
        assert invariant();
        return sum;
    }

    public float[] getTail() {
        if (amountHandles() == 0) throw new IllegalArgumentException("can not access tail of zero-length path!");
        return handles.get(amountHandles() - 1);
    }

    public int amountHandles() {
        return handles.size();
    }

    public float[] getPreviousPoint(float[] point) throws IllegalAccessException {
        if (amountHandles() < 2)
            throw new IllegalAccessException("can not find previous point on path with less than 2 points.");
        int idx = indexOfPosition(point);
        if (idx == -1) throw new IllegalAccessException("this point is not part of the path.");
        if (idx == 0) return handles.get(1);
        return handles.get(idx - 1);
    }

    public Path insertPointAfter(float[] point, float[] newPosition) {
        Path sum = new Path(this.handles, this.type);
        int idx = indexOfPosition(point);
        if (idx == -1) throw new IllegalArgumentException("can not find point " + Arrays.toString(point) + "in path");
        sum.handles.add(idx + 1, newPosition);
        assert invariant();
        return sum;
    }

    @Override
    public Iterator<float[]> iterator() {
        return handles.iterator();
    }

    @Override
    public void forEach(Consumer<? super float[]> action) {
        Iterable.super.forEach(action);
    }

    @Override
    public Spliterator<float[]> spliterator() {
        return Iterable.super.spliterator();
    }

    public Path setHandleByIdx(float[] handle, int idx) {
        ArrayList<float[]> newHandles = new ArrayList<>(handles);
        newHandles.set(idx, handle);
        Path sum = new Path(newHandles, this.type);
        assert invariant();
        return sum;
    }

    public float[] handleByIndex(int index) throws IndexOutOfBoundsException {
        return handles.get(index);
    }

    public int getClosestHandleIdxTo(float[] coord) throws IllegalAccessException {
        if (amountHandles() == 0) throw new IllegalAccessException("can not find closest handle on zero-handle-path");
        int closest = -1;
        double distMinSquared = Double.MAX_VALUE;
        for (int i = 0; i < handles.size(); i++) {
            float[] p = handleByIndex(i);
            double distanceSq = PointUtils.getPositionalDistance(p, coord,
                    RiverHandleInformation.PositionSize.SIZE_2_D.value);
            if (distanceSq < distMinSquared) {
                distMinSquared = distanceSq;
                closest = i;
            }
        }
        assert invariant();
        return closest;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Path) {
            if (this.type != ((Path) obj).type) return false;
            for (int i = 0; i < handles.size(); i++) {
                float[] own = handleByIndex(i);
                float[] theirs = ((Path) obj).handleByIndex(i);
                for (int n = 0; n < own.length; n++) {
                    if (own[n] != theirs[n]) return false;
                }
            }
            return true;
        } else return false;
    }

    public Path clone() {
        return new Path(this.handles, this.type);
    }

    @Override
    public String toString() {
        return "Path{\n" + "type" + type + "\n" + "handles" + handlesToString() + "\n}";
    }

    public String handlesToString() {
        StringBuilder sb = new StringBuilder("handles:[");
        for (float[] handle : handles) {
            sb.append("\n").append(Arrays.toString(handle));
        }
        sb.append("]");
        return sb.toString();
    }

}
