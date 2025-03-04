package org.ironsight.wpplugin.expandLayerTool.operations;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.panels.DefaultFilter;

import java.util.*;
import java.util.stream.Collectors;

import static org.ironsight.wpplugin.expandLayerTool.operations.ApplyAction.applyToDimensionWithFilter;

public class MappingMacro implements SaveableAction {
    //ordered list of layermappings
    public UUID[] mappingUids;
    private String name;
    private String description;
    private UUID uid;

    MappingMacro() {
    }

    public MappingMacro(String name, String description, UUID[] ids, UUID id) {
        this.name = name;
        this.description = description;
        this.uid = id;
        mappingUids = ids;
    }

    //for json deserialization

    public UUID[] getMappingUids() {
        return mappingUids;
    }

    public void setMappingUids(UUID[] mappingUids) {
        this.mappingUids = mappingUids;
    }

    public MappingMacro withName(String name) {
        return new MappingMacro(name, description, mappingUids, uid);
    }

    public MappingMacro withDescription(String description) {
        return new MappingMacro(name, description, mappingUids, uid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(mappingUids), name, description, uid);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MappingMacro that = (MappingMacro) o;
        return Arrays.equals(mappingUids, that.mappingUids) && Objects.equals(name, that.name) &&
                Objects.equals(description, that.description) && Objects.equals(uid, that.uid);
    }

    public MappingMacro withUUIDs(UUID[] uuid) {
        return new MappingMacro(this.name, this.description, uuid, this.uid);
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void apply(Dimension dimension, LayerMappingContainer container) {
        assert allMappingsReady(container) : "Can not apply macro that has invalid actions.";
        DefaultFilter filter = new DefaultFilter(dimension, false, false, -1000, 1000, false, null, null, 0, true);
        Collection<LayerMapping> actions =
                Arrays.stream(mappingUids).map(container::queryById).collect(Collectors.toCollection(ArrayList::new));
        for (LayerMapping lm : actions) {
            System.out.println(lm);
            lm.output.prepareForDimension(dimension);
            lm.input.prepareForDimension(dimension);

            for (int in = lm.input.getMinValue(); in <= lm.input.getMaxValue(); in++) {
                System.out.println(lm.input.valueToString(in) + " => " + lm.output.valueToString(lm.map(in)));
            }
        }
        System.out.println("apply macro " + this.getName() + " to dimension ");
        ArrayList<LayerMapping> mappings = new ArrayList<>(mappingUids.length);
        for (UUID uuid : mappingUids) {
            LayerMapping mapping = container.queryById(uuid);
            if (mapping == null) continue;
            if (mapping.getMappingPoints().length == 0) continue;
            mappings.add(mapping);
            System.out.println("Use action: " + mapping.getName());
        }
        applyToDimensionWithFilter(dimension, filter, pos -> {
            for (LayerMapping mapping : mappings) {
                mapping.applyToPoint(dimension, pos.x, pos.y);
            }
        });

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

    @Override
    public UUID getUid() {
        return uid;
    }

    public void setUid(UUID uid) {
        this.uid = uid;
    }
}
