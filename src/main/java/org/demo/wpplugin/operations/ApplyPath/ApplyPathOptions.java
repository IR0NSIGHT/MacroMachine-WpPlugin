package org.demo.wpplugin.operations.ApplyPath;

import org.pepsoft.worldpainter.operations.OperationOptions;
import org.pepsoft.worldpainter.selection.CopySelectionOperation;

public class ApplyPathOptions implements OperationOptions<CopySelectionOperation> {
    private int finalWidth = 3;
    private double randomFluctuate = 0f;
    private double fluctuationSpeed = 1f;
    private int startWidth = 3;

    public ApplyPathOptions(int finalWidth, double randomFluctuate, double fluctuationSpeed, int startWidth) {
        this.finalWidth = finalWidth;
        this.randomFluctuate = randomFluctuate;
        this.fluctuationSpeed = fluctuationSpeed;
        this.startWidth = startWidth;
    }

    public double getRandomFluctuate() {
        return randomFluctuate;
    }

    public void setRandomFluctuate(double randomFluctuate) {
        this.randomFluctuate = randomFluctuate;
    }

    public int getStartWidth() {
        return startWidth;
    }

    public void setStartWidth(int startWidth) {
        this.startWidth = startWidth;
    }

    public int getFinalWidth() {
        return finalWidth;
    }

    public void setFinalWidth(int finalWidth) {
        this.finalWidth = finalWidth;
    }

    public double getFluctuationSpeed() {
        return fluctuationSpeed;
    }

    public void setFluctuationSpeed(double fluctuationSpeed) {
        this.fluctuationSpeed = fluctuationSpeed;
    }
}
