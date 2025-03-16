package org.ironsight.wpplugin.expandLayerTool.operations;

import org.ironsight.wpplugin.expandLayerTool.operations.ValueProviders.IntermediateSelectionIO;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.panels.DefaultFilter;

import java.util.*;
import java.util.stream.Collectors;

import static org.ironsight.wpplugin.expandLayerTool.operations.ApplyAction.applyToDimensionWithFilter;

public class MappingMacro implements SaveableAction {
    //ordered list of layermappings
    public UUID[] executionUUIDs;
    private String name;
    private String description;
    private UUID uid;

    MappingMacro() {
    }

    public MappingMacro(String name, String description, UUID[] ids, UUID id) {
        this.name = name;
        this.description = description;
        this.uid = id;
        executionUUIDs = ids;
    }

    //for json deserialization

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

    public void apply(Dimension dimension, LayerMappingContainer actionContainer,
                      MappingMacroContainer macroContainer) {
        assert allMappingsReady(actionContainer) : "Can not apply macro that has invalid actions.";
        DefaultFilter filter = new DefaultFilter(dimension, false, false, -1000, 1000, false, null, null, 0, true);

        System.out.println("apply macro " + this.getName() + " to dimension ");
        ArrayList<ArrayList<UUID>> executionSteps = new ArrayList<>(executionUUIDs.length);
        int executionIdx = 0;
        int stepIdx = 0;
        //collect all uuids into steps: macros are one-macro-per-step, actions are many-actions-per-step
        for (UUID uuid : executionUUIDs) {
            LayerMapping mapping = actionContainer.queryById(uuid);
            MappingMacro macro = MappingMacroContainer.getInstance().queryById(uuid);
            if (macro == null) {
                ArrayList<UUID> steps = new ArrayList<>(1);
                steps.add(uuid);
                executionSteps.add(steps);
                executionIdx++;
                stepIdx = 0;
            }
            if (mapping == null) {
                if (stepIdx == 0) {
                    executionSteps.add(new ArrayList<>());
                }
                //get current step
                executionSteps.get(executionIdx).add(uuid);
                stepIdx++;
            }
        }
        for (ArrayList<UUID> executionStep : executionSteps) {
            assert !executionStep.isEmpty() : "Invalid execution step: empty";
            MappingMacro macro = macroContainer.queryById(executionStep.get(0));
            if (macro != null) { //apply the whole macro to the whole map
                macro.apply(dimension, actionContainer, macroContainer);
            } else { //apply list of action to map, one block at a time
                //prepare actions
                Collection<LayerMapping> actions = executionStep.stream()
                        .map(actionContainer::queryById)
                        .collect(Collectors.toCollection(ArrayList::new));
                assert actions.stream().noneMatch(Objects::isNull) : "Invalid execution step: contains null action";
                for (LayerMapping lm : actions) {
                    lm.output.prepareForDimension(dimension);
                    lm.input.prepareForDimension(dimension);
                }
                //all actions are being applied now, one block at a time
                applyToDimensionWithFilter(dimension, filter, pos -> {
                    IntermediateSelectionIO.instance.setSelected(true); //by default, each block is selected.
                    // then the macro
                    // can filter out stuff
                    for (LayerMapping mapping : actions) {
                        mapping.applyToPoint(dimension, pos.x, pos.y);
                    }
                });
            }
        }
    }

    public boolean allMappingsReady(LayerMappingContainer container) {
        for (UUID mappingUid : executionUUIDs) {
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
