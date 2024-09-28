package org.demo.wpplugin.pathing;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

public class FloatInterpolateLinearList implements InterpolateList<Float> {
    private final HashMap<Integer, Float> handleByIdx = new HashMap<>();
    private float[] interpolatedValues;
    private int curveLength;
    private int[] handleIdcs;

    public FloatInterpolateLinearList() {
        updateOnChanged();
    }

    @Override
    public int getCurveLength() {
        return curveLength;
    }

    @Override
    public void setValue(int idx, Float value) {
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
    public boolean isValidHandle(int idx, Float value) {
        return true;
    }

    @Override
    public Float getInterpolatedValue(int idx) {
        if (idx < 0 || idx >= getCurveLength()) {
            return null;
        }
        return interpolatedValues[idx];
    }

    @Override
    public Float getHandleValue(int idx) {
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

        float[] interpolatedValues = new float[curveLength];
        Arrays.fill(interpolatedValues, 0);

        float previousValue = 0f;
        int previousIdx = 0;
        for (int i = 0; i < curveLength; i++) {
            if (isInterpolate(i))
                continue;
            int dist = i - previousIdx;
            float valueHandle = handleByIdx.get(i);
            interpolatedValues[i] = valueHandle;
            for (int j = 1; j < dist; j++) {
                float t = j * 1f / dist;
                float interpolated = (1 - t) * previousValue + (t) * valueHandle;
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
