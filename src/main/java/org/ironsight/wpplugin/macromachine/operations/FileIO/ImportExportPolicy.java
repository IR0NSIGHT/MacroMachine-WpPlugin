package org.ironsight.wpplugin.macromachine.operations.FileIO;

import org.ironsight.wpplugin.macromachine.operations.Macro;
import org.ironsight.wpplugin.macromachine.operations.MappingAction;
import org.pepsoft.worldpainter.layers.CustomLayer;
import org.pepsoft.worldpainter.layers.Layer;

public class ImportExportPolicy {
    boolean allowImportExport(Macro macro) {
        return true;
    }

    boolean allowImportExport(MappingAction action) {
        return true;
    }

    boolean allowImportExport(Layer layer) { return layer instanceof CustomLayer;    }
}
