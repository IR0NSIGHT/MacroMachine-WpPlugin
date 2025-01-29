package org.ironsight.wpplugin.expandLayerTool.operations;

import org.pepsoft.worldpainter.Dimension;

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
        return 0;
    }

    @Override
    public int getMaxValue() {
        return 15;
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
    public String getName() {
        return "Test Getter";
    }

    @Override
    public String getDescription() {
        return "test class for getting values";
    }
}
