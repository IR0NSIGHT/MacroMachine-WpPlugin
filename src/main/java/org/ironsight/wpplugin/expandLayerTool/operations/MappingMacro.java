package org.ironsight.wpplugin.expandLayerTool.operations;

import org.ironsight.wpplugin.expandLayerTool.operations.ValueProviders.IDisplayUnit;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.panels.DefaultFilter;

import java.util.UUID;

public class MappingMacro implements IDisplayUnit {
    //ordered list of layermappings
    public final UUID[] mappingUids;

    public MappingMacro(LayerMapping[] layerMappings) {
        mappingUids = new UUID[layerMappings.length];
        for (int i = 0; i < layerMappings.length; i++) {
            mappingUids[i] = layerMappings[i].getUid();
        }
    }

    @Override
    public String getName() {
        return "Mapping Macro";
    }

    @Override
    public String getDescription() {
        return "this macro globally applies multiple actions in fixed order to achieve cool outcomes.";
    }

    public boolean allMappingsReady(LayerMappingContainer container) {
        for (UUID mappingUid : mappingUids) {
            LayerMapping mapping = container.queryMappingById(mappingUid);
            if (mapping == null) {
                return false;
            }
        }
        return true;
    }

    public void apply(Dimension dimension, LayerMappingContainer container) {
        assert allMappingsReady(container);
        DefaultFilter filter = new DefaultFilter(dimension, false, false, -1000, 1000, false, null, null, 0, true);

        for (UUID mappingUid : mappingUids) {
            LayerMapping mapping = container.queryMappingById(mappingUid);
            if (mapping.getMappingPoints().length == 0) continue;
            ApplyAction action = new ApplyAction(filter, mapping);
            action.apply(dimension);
        }
    }
}
