package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonDeserialize(using = IoParameterDeserializer.class)
@JsonSubTypes({
  @JsonSubTypes.Type(value = IntValue.class, name = "int"),
  @JsonSubTypes.Type(value = FloatValue.class, name = "float"),
  @JsonSubTypes.Type(value = StringValue.class, name = "string"),
  @JsonSubTypes.Type(value = BoolValue.class, name = "bool"),
  @JsonSubTypes.Type(value = IntArrayValue.class, name = "intArray")
})
@Schema(hidden = true)
public sealed interface IoParameter
    permits IntValue, FloatValue, StringValue, BoolValue, IntArrayValue {
  public static Object[] unwrap(IoParameter[] params) {
    Object[] result = new Object[params.length];

    for (int i = 0; i < params.length; i++) {
      IoParameter p = params[i];

      if (p instanceof IntValue v) {
        result[i] = v.value();
      } else if (p instanceof FloatValue v) {
        result[i] = v.value();
      } else if (p instanceof StringValue v) {
        result[i] = v.value();
      } else if (p instanceof BoolValue v) {
        result[i] = v.value();
      } else if (p instanceof IntArrayValue v) {
        result[i] = v.value();
      }
    }

    return result;
  }

  public static IoParameter parseParameter(Object raw, IoParameter expected) {
    if (raw == null) return null;

    if (expected instanceof IntValue) {
      if (raw instanceof Number n) {
        return new IntValue(n.intValue());
      }
    }

    if (expected instanceof FloatValue) {
      if (raw instanceof Number n) {
        return new FloatValue(n.floatValue());
      }
    }

    if (expected instanceof StringValue) {
      if (raw instanceof String s) {
        return new StringValue(s);
      }
    }

    if (expected instanceof BoolValue) {
      if (raw instanceof Boolean b) {
        return new BoolValue(b);
      }
    }

    if (expected instanceof IntArrayValue) {
      if (raw instanceof java.util.List<?> list) {
        int[] arr =
            list.stream()
                .filter(Number.class::isInstance)
                .map(Number.class::cast)
                .mapToInt(Number::intValue)
                .toArray();

        return new IntArrayValue(arr);
      }

      if (raw instanceof int[] arr) {
        return new IntArrayValue(arr);
      }
    }

    return null;
  }
}
