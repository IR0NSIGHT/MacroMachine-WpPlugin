package org.ironsight.wpplugin.macromachine.operations;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Objects;

public class MappingPoint implements Serializable {
    @JsonProperty("x")
    public final int input;
    @JsonProperty("y")
    public final int output;

    public MappingPoint(@JsonProperty("x") int input, @JsonProperty("y") int output) {
        this.input = input;
        this.output = output;
    }

    @Override
    public String toString() {
        return "MappingPoint{" + "input=" + input + ", output=" + output + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        MappingPoint that = (MappingPoint) o;
        return input == that.input && output == that.output;
    }

    @Override
    public int hashCode() {
        return Objects.hash(input, output);
    }
}
