package org.ironsight.wpplugin.expandLayerTool.operations.ValueProviders;

import org.ironsight.wpplugin.expandLayerTool.operations.ProviderType;
import org.pepsoft.worldpainter.Dimension;

import java.awt.*;

public class IntermediateValueIO implements IPositionValueSetter, IPositionValueGetter {
    private static int value;
    private static int lastX;
    private static int lastY;

    @Override
    public boolean equals(Object obj) {
        return obj != null && this.getClass().equals(obj.getClass());
    }

    @Override
    public int getValueAt(Dimension dim, int x, int y) {
        if (lastX != x || lastY != y) return 0;
        return value;
    }

    @Override
    public void setValueAt(Dimension dim, int x, int y, int value) {
        IntermediateValueIO.value = value;
        lastX = x;
        lastY = y;
    }

    @Override
    public void prepareForDimension(Dimension dim) {

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
    public int getMaxValue() {
        return 100;
    }

    @Override
    public int getMinValue() {
        return 0;
    }

    @Override
    public IMappingValue instantiateFrom(Object[] data) {
        return new IntermediateValueIO();
    }

    @Override
    public Object[] getSaveData() {
        return new Object[0];
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

    @Override
    public ProviderType getProviderType() {
        return ProviderType.INTERMEDIATE;
    }
}
