package org.demo.wpplugin.pathing;

public interface InterpolateList<T> {
    int getCurveLength();

    void setValue(int idx, T value);

    void setToInterpolate(int idx);

    boolean isInterpolate(int idx);

    boolean isValidHandle(int idx, T value);

    T getInterpolatedValue(int idx);
    T getHandleValue(int idx);

    int amountHandles();
}
