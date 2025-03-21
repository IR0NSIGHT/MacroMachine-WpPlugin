package org.ironsight.wpplugin.expandLayerTool.operations;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.ironsight.wpplugin.expandLayerTool.operations.ValueProviders.IntermediateSelectionIO;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.operations.Filter;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.ironsight.wpplugin.expandLayerTool.operations.ApplyAction.applyToDimensionWithFilter;

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

    public void setTileFilter(TileFilter filter) {
        this.tileFilter = filter;
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
        assert allMappingsReady(actionContainer, macroContainer) : "Can not apply macro that has invalid actions.";

        ArrayList<ArrayList<UUID>> executionSteps = new ArrayList<>(executionUUIDs.length);
        {
            int executionIdx = 0;
            int stepIdx = 0;
            //collect all uuids into steps: macros are one-macro-per-step, actions are many-actions-per-step
            for (UUID uuid : executionUUIDs) {
                LayerMapping mapping = actionContainer.queryById(uuid);
                MappingMacro macro = MappingMacroContainer.getInstance().queryById(uuid);
                if (mapping == null) {
                    // one macro -> one execution
                    ArrayList<UUID> steps = new ArrayList<>(1);
                    steps.add(uuid);
                    executionSteps.add(steps);
                    executionIdx++;
                    stepIdx = 0;
                }
                if (macro == null) {
                    // a mapping -> add to current execution
                    if (stepIdx == 0) {
                        executionSteps.add(new ArrayList<>());
                    }
                    //get current step
                    executionSteps.get(executionIdx).add(uuid);
                    stepIdx++;
                }
            }
        }
        int stepIdx = 0;
        long[] executionTimes = new long[executionSteps.size()];
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
                long timeStart = System.currentTimeMillis();
                //all actions are being applied now, one block at a time
                applyToDimensionWithFilter(dimension, this.tileFilter, pos -> {
                    //FIXME: this will always use the top level macros filter
                    IntermediateSelectionIO.instance.setSelected(true); //by default, each block is selected.
                    // then the macro
                    // can filter out stuff
                    for (LayerMapping mapping : actions) {
                        mapping.applyToPoint(dimension, pos.x, pos.y);
                    }
                });
                long duration = System.currentTimeMillis() - timeStart;
                executionTimes[stepIdx++] = duration;
            }
        }

        // LOG EXECUTION TIMES
        IntStream.range(0, executionSteps.size()).mapToObj(i -> {
            StringBuilder builder = new StringBuilder("Execution step ").append(i);
            builder.append(", duration=").append(executionTimes[i]).append(" ms - ");
            executionSteps.get(i).stream().map(a -> {
                SaveableAction action = LayerMappingContainer.INSTANCE.queryById(a);
                if (action == null) {
                    action = MappingMacroContainer.getInstance().queryById(a);
                }
                if (action == null) {
                    return "NULL ACTION: " + uid;
                } else {
                    return action.getName();
                }
            }).forEach(b -> builder.append(b).append(", "));
            return builder.toString();
        }).forEach(System.out::println);

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

    public boolean allMappingsReady(LayerMappingContainer container, MappingMacroContainer macroContainer) {
        for (UUID mappingUid : executionUUIDs) {
            LayerMapping mapping = container.queryById(mappingUid);
            MappingMacro nestedMacro = macroContainer.queryById(mappingUid);
            if (mapping == null && nestedMacro == null) return false;
            else if (nestedMacro != null && !nestedMacro.allMappingsReady(container, macroContainer)) return false;
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

    private static class EmptyFilter implements Filter {
        public EmptyFilter() {
        }

        @Override
        public float modifyStrength(int x, int y, float strength) {
            return 1;
        }
    }
}
