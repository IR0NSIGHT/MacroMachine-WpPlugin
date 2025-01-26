package org.ironsight.wpplugin.expandLayerTool.operations;

import java.util.HashMap;

public class LayerMappingContainer {
    public static LayerMappingContainer INSTANCE = new LayerMappingContainer();
    private HashMap<String, LayerMapping> mappings = new HashMap<>();

    public void putMapping(LayerMapping mapping, String name) {
        mappings.put(name,mapping);
    }

    public LayerMapping getMapping(String name) {
        return mappings.get(name);
    }
}

