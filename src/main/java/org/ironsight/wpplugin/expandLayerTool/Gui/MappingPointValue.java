package org.ironsight.wpplugin.expandLayerTool.Gui;

import org.ironsight.wpplugin.expandLayerTool.operations.ValueProviders.IMappingValue;

public class MappingPointValue {
    IMappingValue mappingValue;
    int numericValue;
    public boolean isEditable = false;
    public int mappingPointIndex = -1;

    public MappingPointValue(int numericValue, IMappingValue mappingValue) {
        this.numericValue = numericValue;
        this.mappingValue = mappingValue;
    }

    public MappingPointValue withValue(int numericValue) {
        MappingPointValue mappingPointValue = new MappingPointValue(numericValue, mappingValue);
        mappingPointValue.isEditable = this.isEditable;
        mappingPointValue.mappingPointIndex = this.mappingPointIndex;
        return mappingPointValue;
    }

    @Override
    public String toString() {
        return mappingValue.valueToString(numericValue);
    }
}
