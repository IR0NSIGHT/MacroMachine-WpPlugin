package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class IoParameterTest {

  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  void serializeToPrimitive() throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    mapper.findAndRegisterModules();

    var ioParams = new ArrayList<IoParameter>();
    ioParams.add(new IntValue(-17));
    ioParams.add(new StringValue("hello world"));
    ioParams.add(new BoolValue(false));
    ioParams.add(new IntArrayValue(new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9}));
    ioParams.add(new FloatValue(3.141592654f));

    String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(ioParams);
    assertEquals("[ -17, \"hello world\", false, [ 1, 2, 3, 4, 5, 6, 7, 8, 9 ], 3.1415927 ]", json);
  }

  @Test
  void deserializeFromPrimitive() throws JsonProcessingException {
    var json = "[ -17, \"hello world\", false, [ 1, 2, 3, 4, 5, 6, 7, 8, 9 ], 3.1415927 ]";
    ObjectMapper mapper = new ObjectMapper();
    mapper.findAndRegisterModules();
    List<IoParameter> parameters =
        mapper.readValue(json, new TypeReference<List<IoParameter>>() {});

    assertEquals(new IntValue(-17), parameters.get(0));
    assertEquals(new StringValue("hello world"), parameters.get(1));
    assertEquals(new BoolValue(false), parameters.get(2));
    assertEquals(new IntArrayValue(new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9}), parameters.get(3));
    assertEquals(new FloatValue(3.141592654f), parameters.get(4));
  }
}
