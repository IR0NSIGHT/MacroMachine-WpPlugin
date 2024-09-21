package org.demo.wpplugin.operations.ApplyPath;

import org.pepsoft.worldpainter.operations.OperationOptions;
import org.pepsoft.worldpainter.selection.CopySelectionOperation;

public class ApplyPathOptions implements OperationOptions<CopySelectionOperation> {
    private double randomFluctuate = 0f;
    private double fluctuationSpeed = 1f;

    public ApplyPathOptions(int finalWidth, double randomFluctuate, double fluctuationSpeed, int startWidth) {
        this.randomFluctuate = randomFluctuate;
        this.fluctuationSpeed = fluctuationSpeed;
    }

    public double getRandomFluctuate() {
        return randomFluctuate;
    }

    public void setRandomFluctuate(double randomFluctuate) {
        this.randomFluctuate = randomFluctuate;
    }

    public double getFluctuationSpeed() {
        return fluctuationSpeed;
    }

    public void setFluctuationSpeed(double fluctuationSpeed) {
        this.fluctuationSpeed = fluctuationSpeed;
    }
}
