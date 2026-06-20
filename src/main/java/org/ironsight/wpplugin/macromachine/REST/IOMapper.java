package org.ironsight.wpplugin.macromachine.REST;

import static org.ironsight.wpplugin.macromachine.operations.ValueProviders.IPositionValueSetter.IGNORE_VALUE;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IMappingValue;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IPositionValueGetter;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IPositionValueSetter;

public class IOMapper
{

    private static final ObjectMapper mapper = new ObjectMapper();

    public static Map<String, Object> toInputOutputJson(IMappingValue io, boolean asInput) {
        Map<String, Object> root = new HashMap<>();

        root.put("displayName", io.getName());
        root.put("description", io.getDescription());
        root.put("min", io.getMinValue());
        root.put("max", io.getMaxValue());

        root.put("discrete", io.isDiscrete());
        root.put("uid", String.valueOf(io.hashCode()));

        // values
        List<Map<String, Object>> valuesList = new ArrayList<>();
        int[] values = asInput
                ? ((IPositionValueGetter) io).getAllInputValues()
                : ((IPositionValueSetter) io).getAllOutputValues();
        for (int v : values) {
            Map<String, Object> val = new HashMap<>();
            val.put("numericValue", v);
            val.put("displayName", io.valueToString(v));
            valuesList.add(val);
        }
        root.put("values", valuesList);

        // parameters (empty for now)
        root.put("parameters", new ArrayList<>());

        // ignore value
        if (io instanceof IPositionValueSetter setter) {
            int ignore = -1;
            for (int v : setter.getAllOutputValues()) {
                if (setter.isIgnoreValue(v)) {
                    ignore = v;
                    break;
                }
            }
            root.put("ignoreValue", ignore);
        } else
            root.put("ignoreValue", IGNORE_VALUE);

        return root;
    }
}
