package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import java.io.Serializable;
import org.pepsoft.worldpainter.Dimension;

public interface IPositionValueSetter extends IDisplayUnit, Serializable, IMappingValue
{
    void setValueAt(Dimension dim, int x, int y, int value);

    boolean isIgnoreValue(int value);

    int[] getAllOutputValues();

    static boolean isLegalOutput(IPositionValueSetter setter, int value) {
        return setter.isIgnoreValue(value) || (setter.getMinValue() <= value && value <= setter.getMaxValue());
    }

    public static final int IGNORE_VALUE = Integer.MAX_VALUE;

    static int getIgnoreValue(IPositionValueSetter setter) {
        if (setter.isIgnoreValue(IGNORE_VALUE))
            return IGNORE_VALUE;
        for (int v : setter.getAllOutputValues()) {
            if (setter.isIgnoreValue(v)) {
                return v;
            }
        }
        assert false : "this setter has no known ignore value??";
        return IGNORE_VALUE;
    }
}
