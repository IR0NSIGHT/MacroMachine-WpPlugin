package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonTypeName("int")
@Schema(hidden = true)
public record IntValue(String type, int value) implements IoParameter {
    @JsonCreator
    public IntValue(int value) {
        this("int",value);
    }

    @JsonValue
    public int jsonValue() {
        return value;
    }
}
