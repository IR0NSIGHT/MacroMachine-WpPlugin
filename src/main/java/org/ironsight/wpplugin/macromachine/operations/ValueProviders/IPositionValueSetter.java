package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import org.pepsoft.worldpainter.Dimension;

import java.io.Serializable;

public interface IPositionValueSetter extends IDisplayUnit, Serializable, IMappingValue
{
    void setValueAt(Dimension dim, int x, int y, int value);

    boolean isIgnoreValue(int value);

    int[] getAllOutputValues();
    static boolean isLegalOutput(IPositionValueSetter setter, int value) {
        return setter.isIgnoreValue(value) || (setter.getMinValue() <= value && value <= setter.getMaxValue());
    }

    public static final int IGNORE_VALUE = Integer.MAX_VALUE;
}
