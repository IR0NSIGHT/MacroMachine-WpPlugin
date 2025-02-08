package org.ironsight.wpplugin.expandLayerTool.operations.ValueProviders;

import org.pepsoft.worldpainter.Dimension;

import java.awt.*;

public class IntermediateValueIO implements IPositionValueSetter, IPositionValueGetter {
    private static int value;
    private static int lastX;
    private static int lastY;

    @Override
    public int getValueAt(Dimension dim, int x, int y) {
        if (lastX != x || lastY != y) return 0;
        System.out.printf("%d, %d has %d%n", x, y, value);
        return value;
    }

    @Override
    public void setValueAt(Dimension dim, int x, int y, int value) {
        IntermediateValueIO.value = value;
        lastX = x;
        lastY = y;
    }

    @Override
    public String getName() {
        return "Intermediate Value";
    }

    @Override
    public String getDescription() {
        return "a value to temporarily store an intermediate result. will not exist before or after macro execution, "
                + "only while the macro runs.";
    }

    @Override
    public int getMinValue() {
        return 0;
    }

    @Override
    public int getMaxValue() {
        return 100;
    }

    @Override
    public String valueToString(int value) {
        return Integer.toString(value);
    }

    @Override
    public boolean isDiscrete() {
        return false;
    }

    @Override
    public void paint(Graphics g, int value, java.awt.Dimension dim) {

    }
}
