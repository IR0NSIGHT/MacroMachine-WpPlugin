package org.demo.wpplugin.operations;

import org.pepsoft.worldpainter.operations.OperationOptions;
import org.pepsoft.worldpainter.selection.CopySelectionOperation;
import org.pepsoft.worldpainter.selection.SelectionOptions;

public class ApplyPathOperationOptions extends SelectionOptions implements OperationOptions<CopySelectionOperation> {
    public float getGrowthPerStep() {
        return growthPerStep;
    }

    public void setGrowthPerStep(float growthPerStep) {
        this.growthPerStep = growthPerStep;
    }

    private float growthPerStep = 0.0f;

}
