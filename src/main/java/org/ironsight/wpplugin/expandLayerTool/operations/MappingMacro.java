package org.ironsight.wpplugin.expandLayerTool.operations;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.panels.DefaultFilter;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

import static org.ironsight.wpplugin.expandLayerTool.operations.ApplyAction.applyToDimensionWithFilter;

public class MappingMacro implements SaveableAction {
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

    public MappingMacro withName(String name) {
        return new MappingMacro(name, description, mappingUids, id);
    }

    public MappingMacro withDescription(String description) {
        return new MappingMacro(name, description, mappingUids, id);
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
        return new MappingMacro(this.name, this.description, uuid, this.id);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public boolean allMappingsReady(LayerMappingContainer container) {
        for (UUID mappingUid : mappingUids) {
            LayerMapping mapping = container.queryById(mappingUid);
            if (mapping == null) {
                return false;
            }
        }
        return true;
    }

    public void apply(Dimension dimension, LayerMappingContainer container) {
        assert allMappingsReady(container);
        DefaultFilter filter = new DefaultFilter(dimension, false, false, -1000, 1000, false, null, null, 0, true);

        applyToDimensionWithFilter(dimension, filter, pos -> {
            for (UUID mappingUid : mappingUids) {
                LayerMapping mapping = container.queryById(mappingUid);
                if (mapping.getMappingPoints().length == 0) continue;
                mapping.applyToPoint(dimension, pos.x, pos.y);
            }
        });

    }

    @Override
    public UUID getUid() {
        return id;
    }
}
