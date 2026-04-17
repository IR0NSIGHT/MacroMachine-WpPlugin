package org.ironsight.wpplugin.macromachine.REST;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

public class MMActionBuilder {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static String buildMMActionJson(
            String name,
            String description,
            String uid,
            String actionType,
            Map<String,Object> input,
            Map<String,Object> output,
            List<Integer> inputPoints,
            List<Integer> outputPoints
    ) throws Exception {

        Map<String, Object> action = new HashMap<>();

        action.put("name", name);
        action.put("description", description);
        action.put("uid", uid);
        action.put("actionType", actionType);
        action.put("input", input);
        action.put("output", output);
        action.put("inputPoints", inputPoints);
        action.put("outputPoints", outputPoints);

        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(action);
    }
}