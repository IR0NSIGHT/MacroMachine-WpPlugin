package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Arrays;
import java.util.Objects;

@JsonTypeName("intArray")
@Schema(hidden = true)
public record IntArrayValue(String type, int[] value) implements IoParameter {
  @JsonCreator
  public IntArrayValue(int[] value) {
    this("intArray", value);
  }

  @JsonValue
  public Object getValue() {
    return value();
  }

  // keep object form with fields `type` and `value` for compatibility
  @Override
  public String toString() {
    return "IntArrayValue{" + "value=" + Arrays.toString(value) + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    IntArrayValue that = (IntArrayValue) o;
    return Objects.equals(type, that.type) && Arrays.equals(value(), that.value());
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(type);
    result = 31 * result + Arrays.hashCode(value());
    return result;
  }
}
