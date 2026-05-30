package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class IoParameterDeserializer extends JsonDeserializer<IoParameter> {
  @Override
  public IoParameter deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    JsonToken t = p.currentToken();

    if (t == JsonToken.VALUE_NUMBER_INT || t == JsonToken.VALUE_NUMBER_FLOAT) {
      if (t == JsonToken.VALUE_NUMBER_FLOAT) {
        return new FloatValue(p.getFloatValue());
      } else {
        return new IntValue(p.getIntValue());
      }
    }

    if (t == JsonToken.VALUE_STRING) {
      return new StringValue(p.getText());
    }

    if (t == JsonToken.VALUE_TRUE || t == JsonToken.VALUE_FALSE) {
      return new BoolValue(p.getBooleanValue());
    }

    if (t == JsonToken.START_ARRAY) {
      List<Integer> tmp = new ArrayList<>();
      while (p.nextToken() != JsonToken.END_ARRAY) {
        if (p.currentToken().isNumeric()) {
          tmp.add(p.getIntValue());
        }
      }
      int[] arr = tmp.stream().mapToInt(Integer::intValue).toArray();
      return new IntArrayValue(arr);
    }

    if (t == JsonToken.START_OBJECT) {
      ObjectMapper mapper = (ObjectMapper) p.getCodec();
      JsonNode node = mapper.readTree(p);
      if (node.has("type") && node.has("value")) {
        String type = node.get("type").asText();
        JsonNode val = node.get("value");
        switch (type) {
          case "int":
            return new IntValue(val.asInt());
          case "float":
            return new FloatValue((float) val.asDouble());
          case "string":
            return new StringValue(val.asText());
          case "bool":
            return new BoolValue(val.asBoolean());
          case "intArray":
            {
              if (val.isArray()) {
                List<Integer> tmp = new ArrayList<>();
                for (JsonNode e : val) if (e.isNumber()) tmp.add(e.intValue());
                int[] arr = tmp.stream().mapToInt(Integer::intValue).toArray();
                return new IntArrayValue(arr);
              }
              break;
            }
        }
      }
    }

    return null;
  }
}
