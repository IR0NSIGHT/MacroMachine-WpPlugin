package org.ironsight.wpplugin.expandLayerTool.operations.ValueProviders;

import org.pepsoft.worldpainter.Dimension;

import java.awt.*;

public class TestInputOutput implements IPositionValueSetter, IPositionValueGetter {
    @Override
    public void setValueAt(Dimension dim, int x, int y, int value) {

    }

    @Override
    public int getValueAt(Dimension dim, int x, int y) {
        return 7;
    }

    @Override
    public int getMinValue() {
        return Integer.MIN_VALUE;
    }

    @Override
    public int getMaxValue() {
        return Integer.MAX_VALUE;
    }

    @Override
    public String valueToString(int value) {
        return "testvalue-" + value;
    }

    @Override
    public boolean isDiscrete() {
        return false;
    }

    @Override
    public void paint(Graphics g, int value, java.awt.Dimension dim) {
        g.setColor(Color.RED);
        g.fillRect(0, 0, dim.width, dim.height);
    }

    @Override
    public String getName() {
        return "Test Getter";
    }

    @Override
    public String getDescription() {
        return "test class for getting values";
    }
}
