package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import org.pepsoft.worldpainter.Dimension;

import java.io.Serializable;
import java.util.Arrays;

public interface IPositionValueSetter extends IDisplayUnit, Serializable, IMappingValue
{
    void setValueAt(Dimension dim, int x, int y, int value);

    boolean isIgnoreValue(int value);

    int[] getAllOutputValues();
    static boolean isLegalOutput(IPositionValueSetter setter, int value) {
        return setter.isIgnoreValue(value) || (setter.getMinValue() <= value && value <= setter.getMaxValue());
    }

    static int getIgnoreValue(IPositionValueSetter setter) {
        if (setter.isIgnoreValue(IGNORE_VALUE))
            return IGNORE_VALUE;
        for (var i : setter.getAllOutputValues())
            if (setter.isIgnoreValue(i))
                return i;
        assert false : "this setter has no ignore value in its set of output values";
        return 0;
    }

    public static final int IGNORE_VALUE = Integer.MAX_VALUE;
}
