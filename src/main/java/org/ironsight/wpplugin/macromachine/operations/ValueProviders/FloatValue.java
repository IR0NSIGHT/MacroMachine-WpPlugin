package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonTypeName("float")
@Schema(hidden = true)
public record FloatValue(String type, float value) implements IoParameter {
    @JsonCreator
    public FloatValue(float value) {
        this("float",value);
    }

    // keep object form with fields `type` and `value` for compatibility
}
