package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonTypeName("bool")
@Schema(hidden = true)
public record BoolValue(String type, boolean value) implements IoParameter {
    @JsonCreator
    public BoolValue(boolean value){
        this("bool",value);
    }

    // keep object form with fields `type` and `value` for compatibility
}
