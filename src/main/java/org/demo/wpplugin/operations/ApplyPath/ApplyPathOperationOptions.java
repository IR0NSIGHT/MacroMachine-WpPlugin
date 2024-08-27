package org.demo.wpplugin.operations.ApplyPath;

import org.pepsoft.worldpainter.operations.OperationOptions;
import org.pepsoft.worldpainter.selection.CopySelectionOperation;

public class ApplyPathOperationOptions implements OperationOptions<CopySelectionOperation> {
    public ApplyPathOperationOptions(int finalWidth, double randomFluctuate, double fluctuationSpeed, int startWidth) {
        this.finalWidth = finalWidth;
        this.randomFluctuate = randomFluctuate;
        this.fluctuationSpeed = fluctuationSpeed;
        this.startWidth = startWidth;
    }

    private int finalWidth = 0;

    private double randomFluctuate = 0f;
    private double fluctuationSpeed = 1f;
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

    private int startWidth = 1;

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
