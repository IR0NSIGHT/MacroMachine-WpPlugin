package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

public interface EditableIO extends IDisplayUnit {
    static float clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    int[] getEditableValues();

    String[] getValueNames();
    String[] getValueTooltips();
    EditableIO instantiateWithValues(int[] values);
}
