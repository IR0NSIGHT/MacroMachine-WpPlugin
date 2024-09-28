package org.demo.wpplugin.pathing;

import javax.vecmath.Point2f;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

public class PointFInterpolateLinearList implements InterpolateList<Point2f> {

    private final HashMap<Integer, Point2f> handleByIdx = new HashMap<>();
    private Point2f[] interpolatedValues;
    private int curveLength;
    private int[] handleIdcs;

    public PointFInterpolateLinearList() {
        updateOnChanged();
    }

    @Override
    public int getCurveLength() {
        return curveLength;
    }

    @Override
    public void setValue(int idx, Point2f value) {
        if (!isValidHandle(idx, value)) {
            throw new IllegalArgumentException("this value is not legal for this state");
        }
        handleByIdx.put(idx, value);
        updateOnChanged();
    }

    @Override
    public void setToInterpolate(int idx) {
        handleByIdx.remove(idx);
        updateOnChanged();
    }

    @Override
    public boolean isInterpolate(int idx) {
        return !handleByIdx.containsKey(idx);
    }

    @Override
    public boolean isValidHandle(int idx, Point2f value) {
        return true;
    }

    @Override
    public Point2f getInterpolatedValue(int idx) {
        if (idx < 0 || idx >= getCurveLength()) {
            return null;
        }
        return interpolatedValues[idx];
    }

    @Override
    public Point2f getHandleValue(int idx) {
        return handleByIdx.get(idx);
    }

    @Override
    public int amountHandles() {
        return handleByIdx.size();
    }

    @Override
    public int[] handleIdcs() {

        return handleIdcs;
    }

    private void updateCurveLength() {
        if (handleByIdx.isEmpty())
            curveLength = 0;
        else
            curveLength = 1 + Collections.max(handleByIdx.keySet()); //max idx + 1
    }

    private void updateOnChanged() {
        updateCurveLength();
        updateHandleIdcs();

        updateInterpolateValues();
    }

    private void updateInterpolateValues() {
        int curveLength = getCurveLength();

        Point2f[] interpolatedValues = new Point2f[curveLength];
        Arrays.fill(interpolatedValues, 0);

        Point2f previousValue = new Point2f(0, 0);
        int previousIdx = 0;
        for (int i = 0; i < curveLength; i++) {
            if (isInterpolate(i))
                continue;
            int dist = i - previousIdx;
            Point2f valueHandle = handleByIdx.get(i);
            interpolatedValues[i] = valueHandle;
            for (int j = 1; j < dist; j++) {
                float t = j * 1f / dist;

                Point2f interpolated = new Point2f(
                        (1 - t) * previousValue.x + (t) * valueHandle.x,
                        (1 - t) * previousValue.y + (t) * valueHandle.y);
                interpolatedValues[previousIdx + j] = interpolated;
            }

            previousValue = valueHandle;
            previousIdx = i;
        }

        this.interpolatedValues = interpolatedValues;
    }

    private void updateHandleIdcs() {
        int[] handleIdcs = new int[handleByIdx.size()];
        int i = 0;
        for (Integer idx : handleByIdx.keySet()) {
            handleIdcs[i++] = idx;
        }
        Arrays.sort(handleIdcs);
        this.handleIdcs = handleIdcs;
    }
}


