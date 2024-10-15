package org.demo.wpplugin.operations.ApplyPath;

import org.pepsoft.worldpainter.operations.OperationOptions;
import org.pepsoft.worldpainter.selection.CopySelectionOperation;

public class ApplyPathOptions implements OperationOptions<CopySelectionOperation> {
    public double randomFluctuate;
    public double fluctuationSpeed;

    public ApplyPathOptions(double randomFluctuate, double fluctuationSpeed) {
        this.randomFluctuate = randomFluctuate;
        this.fluctuationSpeed = fluctuationSpeed;
    }
}
