package org.demo.wpplugin.operations;

import org.pepsoft.worldpainter.operations.OperationOptions;
import org.pepsoft.worldpainter.selection.CopySelectionOperation;
import org.pepsoft.worldpainter.selection.SelectionOptions;

public class ApplyPathOperationOptions implements OperationOptions<CopySelectionOperation> {
    private int stepsPerGrowth = 0;

    private double randomFluctuate = 0f;

    public double getRandomFluctuate() {
        return randomFluctuate;
    }

    public void setRandomFluctuate(double randomFluctuate) {
        this.randomFluctuate = randomFluctuate;
    }

    public int getBaseRadius() {
        return baseRadius;
    }

    public void setBaseRadius(int baseRadius) {
        this.baseRadius = baseRadius;
    }

    private int baseRadius = 1;

    public int getStepsPerGrowth() {
        return stepsPerGrowth;
    }

    public void setStepsPerGrowth(int stepsPerGrowth) {
        this.stepsPerGrowth = stepsPerGrowth;
    }

}
