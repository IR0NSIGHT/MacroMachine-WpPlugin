package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class IoParameterTest {

  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  void shouldDeserializeAllIoParameterTypes() throws Exception {

    String json =
        """
            [
              {
                "type": "int",
                "value": 42
              },
              {
                "type": "float",
                "value": 3.14
              },
              {
                "type": "string",
                "value": "hello"
              },
              {
                "type": "bool",
                "value": true
              },
              {
                "type": "intArray",
                "value": [1, 2, 3]
              }
            ]
            """;

    IoParameter[] result = mapper.readValue(json, IoParameter[].class);

    assertNotNull(result);
    assertEquals(5, result.length);

    assertInstanceOf(IntValue.class, result[0]);
    assertEquals(42, ((IntValue) result[0]).value());

    assertInstanceOf(FloatValue.class, result[1]);
    assertEquals(3.14f, ((FloatValue) result[1]).value());

    assertInstanceOf(StringValue.class, result[2]);
    assertEquals("hello", ((StringValue) result[2]).value());

    assertInstanceOf(BoolValue.class, result[3]);
    assertTrue(((BoolValue) result[3]).value());

    assertInstanceOf(IntArrayValue.class, result[4]);
    assertArrayEquals(new int[] {1, 2, 3}, ((IntArrayValue) result[4]).value());
  }
}
