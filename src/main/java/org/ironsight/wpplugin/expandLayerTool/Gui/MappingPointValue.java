package org.ironsight.wpplugin.expandLayerTool.Gui;

import org.ironsight.wpplugin.expandLayerTool.operations.ValueProviders.IMappingValue;

public class MappingPointValue {
    IMappingValue mappingValue;
    int numericValue;

    public MappingPointValue(int numericValue, IMappingValue mappingValue) {
        this.numericValue = numericValue;
        this.mappingValue = mappingValue;
    }

    @Override
    public String toString() {
        return mappingValue.valueToString(numericValue);
    }
}
