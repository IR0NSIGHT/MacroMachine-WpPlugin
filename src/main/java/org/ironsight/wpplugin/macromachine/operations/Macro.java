package org.ironsight.wpplugin.macromachine.operations;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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
@JsonIgnoreProperties(ignoreUnknown = true)
public class Macro implements SaveableAction {
    //ordered list of layermappings
    public final UUID[] executionUUIDs;

    public boolean[] getActiveActions() {
        return activeActions.clone();
    }

    private final boolean[] activeActions;
    private final String name;
    private final String description;
    private final UUID uid;

    @Override
    public String getToolTipText() {
        return "" + executionUUIDs.length + " steps\n"+getDescription();
    }
    public Macro(String name, String description, UUID[] ids, UUID id, boolean[] activeActions) {
        this.name = name;
        this.description = description;
        this.uid = id;
        executionUUIDs = ids;
        this.activeActions = activeActions;
    }

    //for json deserialization

    public UUID[] getExecutionUUIDs() {
        return executionUUIDs;
    }

    public Macro withName(String name) {
        return new Macro(name, description, executionUUIDs, uid, activeActions);
    }

    public Macro withDescription(String description) {
        return new Macro(name, description, executionUUIDs, uid, activeActions);
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
        return new Macro(this.name, this.description, this.executionUUIDs.clone(), this.uid, this.activeActions.clone());
    }

    public Macro withUUIDs(UUID[] uuid, boolean[] activeActions) {
        return new Macro(this.name, this.description, uuid, this.uid, activeActions);
    }

    /**
     * with new UUIDS. all are active by default.
     * @param uuid
     * @return
     */
    public Macro withUUIDs(UUID[] uuid) {
        boolean[] activeActions = new boolean[uuid.length];
        Arrays.fill(activeActions, true);
        return new Macro(this.name, this.description, uuid, this.uid, activeActions);
    }

    public Macro withReplacedUUIDs(int[] overwriteIdcs, UUID uid) {
        boolean[] overwrittenActiveState = new boolean[overwriteIdcs.length];
        Arrays.fill(overwrittenActiveState, true);
        return withReplacedUUIDs(overwriteIdcs, uid, overwrittenActiveState);
    }
        public Macro withReplacedUUIDs(int[] overwriteIdcs, UUID uid, boolean[] overwrittenActiveState) {
        UUID[] newIds = executionUUIDs.clone();
        boolean[] newActiveActions = activeActions.clone();

        assert newIds.length == newActiveActions.length;
        int overwriteIdx = 0;
        for (int row : overwriteIdcs) {
            assert row >= 0 : "index ouf of bounds";
            assert row < newIds.length : "index out of bounds";
            newIds[row] = uid;
            newActiveActions[row] = overwrittenActiveState[overwriteIdx++];
        }
        return this.withUUIDs(newIds, newActiveActions);
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

    @Override
    public String getDescription() {
        return description;
    }


    public List<UUID> collectActions(List<UUID> actionList) {
        for (UUID id : this.executionUUIDs) {
            SaveableAction action = MacroContainer.getInstance().queryById(id);
            if (action != null) {//macro
                //macro adds its own steps recursively
                ((Macro) action).collectActions(actionList);
            } else {
                action = MappingActionContainer.getInstance().queryById(id);
                if (action != null)
                    actionList.add(action.getUid());
            }
        }
        return actionList;
    }

    public boolean hasLoop(HashSet<UUID> seen, MacroContainer container) {
        if (seen.contains(this.uid)) return true;
        seen.add(this.uid);
        boolean childLoop = false;
        for (UUID uuid : this.executionUUIDs) {
            Macro macro = container.queryById(uuid);
            if (macro != null && macro.hasLoop((HashSet<UUID>) seen.clone(), container)) return true;
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
}
