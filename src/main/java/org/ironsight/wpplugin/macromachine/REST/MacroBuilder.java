package org.ironsight.wpplugin.macromachine.REST;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.ironsight.wpplugin.macromachine.operations.Macro;

public class MacroBuilder {

  private static final ObjectMapper mapper = new ObjectMapper();

  public static String buildMacroJson(Macro macro) throws Exception {

    Map<String, Object> result = new HashMap<>();

    result.put("name", macro.getName());
    result.put("description", macro.getDescription());
    result.put("uid", macro.getUid().toString());

    // UUID[] → List<String>
    result.put(
        "executionUUIDs", Arrays.stream(macro.getExecutionUUIDs()).map(UUID::toString).toList());

    // boolean[]
    List<Boolean> activeActions = new ArrayList<>();
    for (boolean b : macro.getActiveActions()) {
      activeActions.add(b);
    }
    result.put("activeActions", activeActions);

    return mapper.writeValueAsString(result);
  }
}
