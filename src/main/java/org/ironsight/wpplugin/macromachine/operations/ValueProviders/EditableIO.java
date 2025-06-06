package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import java.util.Map;

public interface EditableIO extends IDisplayUnit {
    int[] getEditableValues();

    String[] getValueNames();
    String[] getValueTooltips();
    void setEditableValues(int[] values);
    boolean sanitizeValue(int value, int index);
}
