package org.demo.wpplugin.pathing;

import java.util.Arrays;

import static org.demo.wpplugin.operations.River.RiverHandleInformation.INHERIT_VALUE;

public class FloatInterpolateList {
    private final float[] rawValues;
    private final boolean[] interpolate;


    public FloatInterpolateList(int size) {
        rawValues = new float[size];
        interpolate = new boolean[size];
        Arrays.fill(interpolate, true);
    }

    public int getSize() {
        return rawValues.length;
    }

    public void setValue(int idx, float value) {
        if (idx < 0 || idx >= rawValues.length) {
            throw new ArrayIndexOutOfBoundsException(idx);
        }
        if (!isValidValue(idx, value)) {
            throw new IllegalArgumentException("this value is not legal for this state");
        }
        rawValues[idx] = value;
        interpolate[idx] = false;
    }

    public void setToInterpolate(int idx) {
        if (idx < 0 || idx >= rawValues.length) {
            throw new ArrayIndexOutOfBoundsException(idx);
        }
        interpolate[idx] = true;
    }

    public boolean isInterpolate(int idx) {
        if (idx < 0 || idx >= rawValues.length) {
            throw new ArrayIndexOutOfBoundsException(idx);
        }
        return interpolate[idx];
    }

    protected boolean isValidValue(int idx, float value) {
        return true;
    }

    private float[] updateRawValues() {
        float[] interpolatedValues = new float[rawValues.length];
        Arrays.fill(interpolatedValues, 0);

        float previousValue = 0;
        int previousIdx = 0;
        for (int i = 0; i < rawValues.length; i++) {
            if (isInterpolate(i))
                continue;
            interpolatedValues[i] = rawValues[i];
            int dist = i-previousIdx;
            float valueHandle = rawValues[i];
            for (int j = 1; j < dist; j++) {
                float t = j*1f/dist;
                float interpolated = (1-t)*previousValue + (t)*valueHandle;
                interpolatedValues[previousIdx + j] = interpolated;
            }

            previousValue = rawValues[i];
            previousIdx = i;
        }

        return interpolatedValues;
    }

    public float[] getInterpolatedList() {
        return updateRawValues();
    }


}
