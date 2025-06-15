package org.ironsight.wpplugin.macromachine.operations.FileIO;

import org.ironsight.wpplugin.macromachine.operations.Macro;
import org.ironsight.wpplugin.macromachine.operations.MacroContainer;
import org.ironsight.wpplugin.macromachine.operations.MappingAction;

import java.util.HashSet;
import java.util.UUID;

/**
 * this policy will filter out everything but a given macro and all its dependencies, including nested macros.
 */
public class MacroExportPolicy extends ImportExportPolicy {
    private HashSet<UUID> requiredIds = new HashSet<>();
    public MacroExportPolicy(Macro macro, MacroContainer container) {
        addMacroAndChildren(macro, container);
    }

    private void addMacroAndChildren(Macro macro,MacroContainer container) {
        requiredIds.add(macro.getUid());
        for (UUID childId: macro.getExecutionUUIDs()) {
            if (container.queryContains(childId))
                //macro
                addMacroAndChildren(container.queryById(childId),container);
            else {
                //action
                requiredIds.add(childId);
            }
        }

    }

    @Override
    boolean allowImportExport(Macro macro) {
        return requiredIds.contains(macro.getUid());
    }

    @Override
    boolean allowImportExport(MappingAction action) {
        return requiredIds.contains(action.getUid());
    }
}
