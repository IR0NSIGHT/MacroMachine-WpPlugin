package org.ironsight.wpplugin.macromachine.REST;

import static org.ironsight.wpplugin.macromachine.REST.IOMapper.toInputOutputJson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.ironsight.wpplugin.macromachine.operations.MappingAction;
import org.ironsight.wpplugin.macromachine.operations.MappingPoint;

public class MMActionBuilder {

  private static final ObjectMapper mapper = new ObjectMapper();

  public static String buildMMActionJson(MappingAction action) throws JsonProcessingException {
    return buildMMActionJson(
        action.getName(),
        action.getDescription(),
        action.getUid().toString(),
        action.getActionType().displayName,
        toInputOutputJson(action.getInput(), true),
        toInputOutputJson(action.getOutput(), false),
        Arrays.stream(action.getInput().getAllInputValues()).boxed().toList(),
        Arrays.stream(action.getInput().getAllInputValues()).map(action::map).boxed().toList(),
        action.getMappingPoints());
  }

  public String toJson(MappingPoint[] points) throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    return mapper.writeValueAsString(points);
  }

  private static String buildMMActionJson(
      String name,
      String description,
      String uid,
      String actionType,
      Map<String, Object> input,
      Map<String, Object> output,
      List<Integer> inputPoints,
      List<Integer> outputPoints,
      MappingPoint[] mappingPoints)
      throws JsonProcessingException {

    Map<String, Object> action = new HashMap<>();

    action.put("name", name);
    action.put("description", description);
    action.put("uid", uid);
    action.put("actionType", actionType);
    action.put("input", input);
    action.put("output", output);
    action.put("mappedInputs", inputPoints);
    action.put("mappedOutputs", outputPoints);
    action.put("mappingPoints", mappingPoints);
    return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(action);
  }
}
