package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonTypeName("string")
@Schema(hidden = true)
public record StringValue(String type, String value) implements IoParameter {
    @JsonCreator
    public StringValue(String value) {
        this("string", value);
    }

    @JsonValue
    public String jsonValue() {
        return value;
    }

}
