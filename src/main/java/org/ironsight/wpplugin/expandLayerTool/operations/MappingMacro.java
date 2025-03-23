package org.ironsight.wpplugin.expandLayerTool.operations;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.*;

public class MappingMacro implements SaveableAction {
    //ordered list of layermappings
    public UUID[] executionUUIDs;
    private String name;
    private String description;
    private UUID uid;
    @JsonIgnore
    private TileFilter tileFilter = new TileFilter();

    MappingMacro() {
    }

    public MappingMacro(String name, String description, UUID[] ids, UUID id) {
        this.name = name;
        this.description = description;
        this.uid = id;
        executionUUIDs = ids;
    }

    public TileFilter getTileFilter() {
        return tileFilter;
    }

    //for json deserialization

    public void setTileFilter(TileFilter filter) {
        this.tileFilter = filter;
    }

    public UUID[] getExecutionUUIDs() {
        return executionUUIDs;
    }

    public void setExecutionUUIDs(UUID[] executionUUIDs) {
        this.executionUUIDs = executionUUIDs;
    }

    public MappingMacro withName(String name) {
        return new MappingMacro(name, description, executionUUIDs, uid);
    }

    public MappingMacro withDescription(String description) {
        return new MappingMacro(name, description, executionUUIDs, uid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(executionUUIDs), name, description, uid);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MappingMacro that = (MappingMacro) o;
        return Arrays.equals(executionUUIDs, that.executionUUIDs) && Objects.equals(name, that.name) &&
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

    public List<List<UUID>> collectActions(List<List<UUID>> actionList) {
        List<UUID> step = new ArrayList<>();
        for (UUID id : this.executionUUIDs) {
            SaveableAction action = MappingMacroContainer.getInstance().queryById(id);
            if (action != null) {//macro
                if (!step.isEmpty()) actionList.add(step);   //add collected stuff until here to actionList
                step = new ArrayList<>();   //init new list

                //macro adds its own steps
                ((MappingMacro) action).collectActions(actionList);
            } else {
                action = LayerMappingContainer.INSTANCE.queryById(id);
                if (action != null) step.add(id);
            }
        }
        if (!step.isEmpty()) actionList.add(step);

        return actionList;
    }

    public boolean hasLoop(HashSet<UUID> seen) {
        if (seen.contains(this.uid)) return true;
        seen.add(this.uid);
        boolean childLoop = false;
        for (UUID uuid : this.executionUUIDs) {
            MappingMacro macro = MappingMacroContainer.getInstance().queryById(uuid);
            if (macro != null && macro.hasLoop((HashSet<UUID>) seen.clone())) return true;
        }
        return childLoop;
    }

    public boolean containsNoUnknownActions(LayerMappingContainer container, MappingMacroContainer macroContainer) {
        for (UUID mappingUid : executionUUIDs) {
            LayerMapping mapping = container.queryById(mappingUid);
            MappingMacro nestedMacro = macroContainer.queryById(mappingUid);
            if (mapping == null && nestedMacro == null) return false;
            else if (nestedMacro != null && !nestedMacro.containsNoUnknownActions(container, macroContainer))
                return false;
            else continue;
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
