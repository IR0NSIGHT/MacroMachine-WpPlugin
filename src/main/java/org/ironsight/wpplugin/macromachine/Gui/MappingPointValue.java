package org.ironsight.wpplugin.macromachine.Gui;

import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IMappingValue;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IPositionValueSetter;

import java.util.Objects;

public class MappingPointValue implements Comparable<MappingPointValue>
{
    public IMappingValue mappingValue;
    public int numericValue;

    public MappingPointValue(int numericValue, IMappingValue mappingValue) { // TODO make constructors forgetter OR
                                                                             // setter to diffferentiate context?
        this.numericValue = numericValue;
        this.mappingValue = mappingValue;
    }

    public MappingPointValue withValue(int numericValue) {
        MappingPointValue mappingPointValue = new MappingPointValue(numericValue, mappingValue);
        return mappingPointValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
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

    @Override
    public int compareTo(MappingPointValue o) {
        if (mappingValue instanceof IPositionValueSetter setter) {
            boolean thisIgnore = setter.isIgnoreValue(numericValue);
            boolean otherIgnore = setter.isIgnoreValue(o.numericValue);

            if (thisIgnore && !otherIgnore)
                return -1; // this first
            if (!thisIgnore && otherIgnore)
                return 1; // other first
            if (thisIgnore && otherIgnore)
                return 0; // both ignore
        }
        if (mappingValue.isDiscrete())
            return mappingValue.valueToString(numericValue)
                    .compareToIgnoreCase(o.mappingValue.valueToString(o.numericValue));
        return Integer.compare(numericValue, o.numericValue);
    }
}
