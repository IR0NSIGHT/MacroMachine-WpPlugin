package org.ironsight.wpplugin.expandLayerTool.operations.ValueProviders;

import org.ironsight.wpplugin.expandLayerTool.operations.ProviderType;
import org.pepsoft.worldpainter.Dimension;

import java.awt.*;

public class TestInputOutput implements IPositionValueSetter, IPositionValueGetter {
    @Override
    public void setValueAt(Dimension dim, int x, int y, int value) {

    }

    @Override
    public void prepareForDimension(Dimension dim) {

    }

    @Override
    public int getValueAt(Dimension dim, int x, int y) {
        return 7;
    }

    @Override
    public int getMaxValue() {
        return 1000;
    }

    @Override
    public int getMinValue() {
        return -5;
    }

    @Override
    public IMappingValue instantiateFrom(Object[] data) {
        return new TestInputOutput();
    }

    @Override
    public Object[] getSaveData() {
        return new Object[0];
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
    public ProviderType getProviderType() {
        return ProviderType.TEST;
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
