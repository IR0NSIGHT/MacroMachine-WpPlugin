package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Arrays;

@JsonTypeName("intArray")
@Schema(hidden = true)
public record IntArrayValue(String type, int[] value) implements IoParameter {
  @JsonCreator
  public IntArrayValue(int[] value) {
    this("intArray",value);
  }

  // keep object form with fields `type` and `value` for compatibility
  @Override
  public String toString() {
    return "IntArrayValue{" + "value=" + Arrays.toString(value) + '}';
  }
}
