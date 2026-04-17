package org.ironsight.wpplugin.macromachine.REST;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IMappingValue;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IPositionValueGetter;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IPositionValueSetter;

import java.util.*;

public class IOMapper {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static Map<String, Object> toInputOutputJson(IMappingValue io, boolean asInput) throws Exception {
        Map<String, Object> root = new HashMap<>();

        root.put("displayName", io.getProviderType().toString());
        root.put("description", io.getProviderType().toString());
        root.put("min", io.getMinValue());
        root.put("max", io.getMaxValue());
        root.put("ignoreValue", -1);
        root.put("discrete", io.isDiscrete());
        root.put("uid", String.valueOf(io.hashCode()));

        // values
        List<Map<String, Object>> valuesList = new ArrayList<>();
        int[] values = asInput ? ((IPositionValueGetter)io).getAllInputValues() :  ((IPositionValueSetter)io).getAllOutputValues();
        for (int v : values) {
            Map<String, Object> val = new HashMap<>();
            val.put("numericValue", v);
            val.put("displayName", io.valueToString(v));
            valuesList.add(val);
        }
        root.put("values", valuesList);

        // parameters (empty for now)
        root.put("parameters", new ArrayList<>());

        return root;
    }
}