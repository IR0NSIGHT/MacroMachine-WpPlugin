package org.ironsight.wpplugin.macromachine.operations;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * this class is a collection of MappingActions the action are ordered and executed in this order a macro can be
 * executed and will apply each of its nested action to the map macros can container Actions or other Macros (nesting)
 * recursion is technically possible but not allowed because there is no way to detect infinite recursion.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Macro implements SaveableAction {
    //ordered list of layermappings
    public final UUID[] executionUUIDs;
    private final boolean[] activeActions;
    private final String name;
    private final String description;
    private final UUID uid;
    public Macro(String name, String description, UUID[] ids, UUID id, boolean[] activeActions) {
        this.name = name;
        this.description = description;
        this.uid = id;
        executionUUIDs = ids;
        this.activeActions = activeActions;
    }

    /**
     * @param macro
     * @param item            action or macro to insert
     * @param createNewAction getter to clone action if necessary
     * @param targetRows      insert item at each of those rows
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
        ArrayList<Boolean> activeUids = new ArrayList<>();
        for (boolean b: macro.activeActions)
            activeUids.add(b);

        int counter = 0;
        for (int row : targetRows) {
            int idx = row + counter + 1;
            if (item instanceof MappingAction) {
                MappingAction actionClone = createNewAction.get();
                updateAction.accept(actionClone.withValuesFrom((MappingAction) item));
                uids.add(idx, actionClone.getUid());
                activeUids.add(idx, true);
            } else {
                assert item instanceof Macro;
                uids.add(idx, item.getUid());
                activeUids.add(idx, true);
            }

            outNewSelection.add(idx);
            counter++;
        }

        UUID[] ids = uids.toArray(new UUID[0]);
        boolean[] active = new boolean[activeUids.size()];
        for (int i = 0; i < activeUids.size(); i++)
            active[i] = activeUids.get(i);
        Macro newMacro = macro.withUUIDs(ids, active);
        return newMacro;
    }

    public boolean[] getActiveActions() {
        return activeActions.clone();
    }

    @Override
    public boolean isActive() {
        return isActive;
    }

    @Override
    public void setActive(boolean active) {
        isActive = active;
    }

    //only for gui purposes, not part of the actual data. only use this flag if you set it yourself
    private boolean isActive;

    @Override
    public String getToolTipText() {
        return "" + executionUUIDs.length + " steps\n" + getDescription();
    }

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
                Objects.equals(description, that.description) && Objects.equals(uid, that.uid) && Arrays.equals(activeActions, that.activeActions);
    }

    @Override
    public Macro clone() {
        return new Macro(this.name,
                this.description,
                this.executionUUIDs.clone(),
                this.uid,
                this.activeActions.clone());
    }

    public Macro withUUIDs(UUID[] uuid, boolean[] activeActions) {
        return new Macro(this.name, this.description, uuid, this.uid, activeActions);
    }

    public Macro withUUID(UUID selfId) {
        return new Macro(name,description, executionUUIDs.clone(), selfId,activeActions.clone());
    }
    public static boolean[] deleteAt(boolean[] arr, int idx) {
        if (arr == null || idx < 0 || idx >= arr.length) {
            throw new IllegalArgumentException("Invalid index or array is null");
        }

        boolean[] newArray = new boolean[arr.length - 1];
        System.arraycopy(arr, 0, newArray, 0, idx);
        System.arraycopy(arr, idx + 1, newArray, idx, arr.length - idx - 1);

        return newArray;
    }

    public static UUID[] deleteAt(UUID[] arr, int idx) {
        if (arr == null || idx < 0 || idx >= arr.length) {
            throw new IllegalArgumentException("Invalid index or array is null");
        }

        UUID[] newArray = new UUID[arr.length - 1];
        System.arraycopy(arr, 0, newArray, 0, idx);
        System.arraycopy(arr, idx + 1, newArray, idx, arr.length - idx - 1);

        return newArray;
    }

    public Macro withRemovedItem(int itemIdx) {
        return new Macro(this.name, this.description, deleteAt(getExecutionUUIDs(), itemIdx), this.uid,
                deleteAt(getActiveActions(), itemIdx));
    }

    public Macro withShiftedItem(int oldIdx, int newIdx) {
        UUID[] newIds = getExecutionUUIDs().clone();
        boolean[] active = getActiveActions().clone();
        newIds[newIdx] = getExecutionUUIDs()[oldIdx];
        newIds[oldIdx] = getExecutionUUIDs()[newIdx];
        active[newIdx] = getActiveActions()[oldIdx];
        active[oldIdx] = getActiveActions()[newIdx];
        return new Macro(this.name, this.description,newIds, this.uid,
                active);
    }

    /**
     * with new UUIDS. all are active by default.
     *
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

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }


    public List<UUID> collectActions(List<UUID> actionList, MacroContainer macroContainer, MappingActionContainer actionContainer) {
        int idx = 0;
        for (UUID id : this.executionUUIDs) {
            SaveableAction action =macroContainer.queryById(id);
            if (!this.activeActions[idx++])
                continue;
            if (action != null) {//macro
                //macro adds its own steps recursively
                ((Macro) action).collectActions(actionList,macroContainer,actionContainer);
            } else {
                action = actionContainer.queryById(id);
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
