package org.ironsight.wpplugin.expandLayerTool.operations;

import org.ironsight.wpplugin.expandLayerTool.operations.ValueProviders.IDisplayUnit;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.panels.DefaultFilter;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

public class MappingMacro implements IDisplayUnit {
    //ordered list of layermappings
    public final UUID[] mappingUids;
    private final String name;
    private final String description;
    private final UUID id;

    public MappingMacro(String name, String description, UUID[] ids, UUID id) {
        this.name = name;
        this.description = description;
        this.id = id;
        mappingUids = ids;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MappingMacro that = (MappingMacro) o;
        return Arrays.equals(mappingUids, that.mappingUids) && Objects.equals(name, that.name) &&
                Objects.equals(description, that.description) && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(mappingUids), name, description, id);
    }

    public MappingMacro withUUIDs(UUID[] uuid) {
        MappingMacro mappingMacro = new MappingMacro(this.name, this.description, uuid, this.id);
        return mappingMacro;
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
