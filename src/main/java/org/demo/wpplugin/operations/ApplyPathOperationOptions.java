package org.demo.wpplugin.operations;

import org.pepsoft.worldpainter.operations.OperationOptions;
import org.pepsoft.worldpainter.selection.CopySelectionOperation;
import org.pepsoft.worldpainter.selection.SelectionOptions;

public class ApplyPathOperationOptions extends SelectionOptions implements OperationOptions<CopySelectionOperation> {
    private int stepsPerGrowth = 0;

    public int getStepsPerGrowth() {
        return stepsPerGrowth;
    }

    public void setStepsPerGrowth(int stepsPerGrowth) {
        this.stepsPerGrowth = stepsPerGrowth;
    }

}
