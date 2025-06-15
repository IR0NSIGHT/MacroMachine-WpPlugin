package org.ironsight.wpplugin.macromachine.operations.FileIO;

import org.ironsight.wpplugin.macromachine.operations.Macro;
import org.ironsight.wpplugin.macromachine.operations.MappingAction;

public class ImportExportPolicy {
    boolean allowImportExport(Macro macro) {
        return true;
    }

    boolean allowImportExport(MappingAction action) {
        return true;
    }
}
