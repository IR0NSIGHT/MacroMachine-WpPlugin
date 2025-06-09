package org.ironsight.wpplugin.macromachine.operations;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * this class is a collection of MappingActions
 * the actions are ordered and executed in this order
 * a macro can be executed and will apply each of its nested actions to the map
 * macros can container Actions or other Macros (nesting)
 * recursion is technically possible but not allowed because there is no way to detect infinite recursion.
 */
public class Macro implements SaveableAction {
    //ordered list of layermappings
    public UUID[] executionUUIDs;
    private String name;
    private String description;
    private UUID uid;
    @JsonIgnore
    private TileFilter tileFilter = new TileFilter();

    Macro() {
    }

    public Macro(String name, String description, UUID[] ids, UUID id) {
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

    public Macro withName(String name) {
        return new Macro(name, description, executionUUIDs, uid);
    }

    public Macro withDescription(String description) {
        return new Macro(name, description, executionUUIDs, uid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(executionUUIDs), name, description, uid);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Macro that = (Macro) o;
        return Arrays.equals(executionUUIDs, that.executionUUIDs) && Objects.equals(name, that.name) &&
                Objects.equals(description, that.description) && Objects.equals(uid, that.uid);
    }

    @Override
    public Macro clone() {
        return new Macro(this.name, this.description, this.executionUUIDs.clone(), this.uid);
    }

    public Macro withUUIDs(UUID[] uuid) {
        return new Macro(this.name, this.description, uuid, this.uid);
    }

    public Macro withReplacedUUIDs(int[] overwriteIdcs, UUID uid) {
        UUID[] newIds = executionUUIDs.clone();
        for (int row : overwriteIdcs) {
            assert row >= 0 : "index ouf of bounds";
            assert row < newIds.length : "index out of bounds";
            newIds[row] = uid;
        }
        return this.withUUIDs(newIds);
    }

    /**
     *
     * @param macro
     * @param item action or macro to insert
     * @param createNewAction getter to clone actions if necessary
     * @param targetRows    insert item at each of those rows
     * @param outNewSelection output array with indices of row selection. old rows stay selected.
     * @return new macro
     */
    public static Macro insertSaveableActionToList(Macro macro, SaveableAction item,
                                                   Supplier<MappingAction> createNewAction,
                                                   Consumer<MappingAction> updateAction, int[] targetRows,
                                                   ArrayList<Integer> outNewSelection) {
        if (targetRows.length == 0) {
            targetRows = new int[]{macro.getExecutionUUIDs().length - 1};
        }
        //insert any mapping from container at tail of list
        ArrayList<UUID> uids = new ArrayList<>();
        Collections.addAll(uids, macro.executionUUIDs);

        int counter = 0;
        for (int row : targetRows) {
            int idx = row + counter + 1;
            if (item instanceof MappingAction) {
                MappingAction actionClone = createNewAction.get();
                updateAction.accept(actionClone.withValuesFrom((MappingAction) item));
                uids.add(idx, actionClone.getUid());
            } else {
                assert item instanceof Macro;
                uids.add(idx, item.getUid());
            }

            outNewSelection.add(idx);
            counter++;
        }

        UUID[] ids = uids.toArray(new UUID[0]);
        Macro newMacro = macro.withUUIDs(ids);
        return newMacro;
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
            SaveableAction action = MacroContainer.getInstance().queryById(id);
            if (action != null) {//macro
                if (!step.isEmpty()) actionList.add(step);   //add collected stuff until here to actionList
                step = new ArrayList<>();   //init new list

                //macro adds its own steps
                ((Macro) action).collectActions(actionList);
            } else {
                action = MappingActionContainer.getInstance().queryById(id);
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
            Macro macro = MacroContainer.getInstance().queryById(uuid);
            if (macro != null && macro.hasLoop((HashSet<UUID>) seen.clone())) return true;
        }
        return childLoop;
    }

    public boolean containsNoUnknownActions(MappingActionContainer container, MacroContainer macroContainer) {
        for (UUID mappingUid : executionUUIDs) {
            MappingAction mapping = container.queryById(mappingUid);
            Macro nestedMacro = macroContainer.queryById(mappingUid);
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
