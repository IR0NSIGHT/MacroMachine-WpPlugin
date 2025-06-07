package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

public interface EditableIO extends IDisplayUnit {
    int[] getEditableValues();

    String[] getValueNames();
    String[] getValueTooltips();
    EditableIO instantiateWithValues(int[] values);
    boolean sanitizeValue(int value, int index);
}
