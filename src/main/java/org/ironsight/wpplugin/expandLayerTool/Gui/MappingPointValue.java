package org.ironsight.wpplugin.expandLayerTool.Gui;

import org.ironsight.wpplugin.expandLayerTool.operations.ValueProviders.IMappingValue;

import java.util.Objects;

public class MappingPointValue {
    IMappingValue mappingValue;
    int numericValue;
    public boolean isEditable = false;
    public int mappingPointIndex = -1;

    public MappingPointValue(int numericValue, IMappingValue mappingValue) {
        assert (IMappingValue.sanitizeValue(numericValue,mappingValue) == numericValue) : "illegal value for this " +
                "mapping";
        this.numericValue = numericValue;
        this.mappingValue = mappingValue;
    }

    public MappingPointValue(IMappingValue mappingValue, int numericValue, boolean isEditable, int mappingPointIndex) {
        assert (IMappingValue.sanitizeValue(numericValue,mappingValue) == numericValue) : "illegal value for this " +
                "mapping";
        this.mappingValue = mappingValue;
        this.numericValue = numericValue;
        this.isEditable = isEditable;
        this.mappingPointIndex = mappingPointIndex;
    }

    public MappingPointValue withValue(int numericValue) {
        MappingPointValue mappingPointValue = new MappingPointValue(numericValue, mappingValue);
        mappingPointValue.isEditable = this.isEditable;
        mappingPointValue.mappingPointIndex = this.mappingPointIndex;
        return mappingPointValue;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MappingPointValue that = (MappingPointValue) o;
        return numericValue == that.numericValue && Objects.equals(mappingValue, that.mappingValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mappingValue, numericValue);
    }

    @Override
    public String toString() {
        return mappingValue.valueToString(numericValue);
    }
}
