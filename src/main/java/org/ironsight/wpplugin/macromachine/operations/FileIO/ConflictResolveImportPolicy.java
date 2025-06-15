package org.ironsight.wpplugin.macromachine.operations.FileIO;

import org.ironsight.wpplugin.macromachine.operations.*;

import javax.swing.*;
import java.awt.*;

public class ConflictResolveImportPolicy extends ImportExportPolicy {
    private MacroContainer macroContainer;
    private MappingActionContainer actionContainer;
    private Window parent;
    private boolean allowAll = false;
    private boolean skipAll = false;

    public ConflictResolveImportPolicy(MacroContainer macros, MappingActionContainer actions, Window parent) {
        this.macroContainer = macros;
        this.actionContainer = actions;
        this.parent = parent;
    }

    @Override
    boolean allowImportExport(Macro macro) {
        return doDialogFor(macro, macroContainer);
    }

    private boolean doDialogFor(SaveableAction action, AbstractOperationContainer container) {
        if (skipAll)
            return false;
        if (allowAll)
            return true;
        if (container.queryContains(action.getUid())) {
            SaveableAction original = container.queryById(action.getUid());
            if (original.equals(action))
                return false; // nothing to do, just skip
            FileConflictResolverDialog diag;
            if (parent instanceof JDialog)
                diag = new FileConflictResolverDialog((JDialog) parent, action, original);
            else if (parent instanceof JFrame)
                diag = new FileConflictResolverDialog((JFrame) parent, action, original);
            else
                diag = new FileConflictResolverDialog((JFrame) null, action, original);
            diag.setVisible(true);
            if (diag.isRemember() && diag.isOverwrite())
                allowAll = true;
            if (diag.isRemember() && !diag.isOverwrite())
                skipAll = true;
            return diag.isOverwrite();

        } else {
            return true;
        }
    }

    @Override
    boolean allowImportExport(MappingAction action) {
        return doDialogFor(action, actionContainer);
    }
}
