package org.ironsight.wpplugin.expandLayerTool.operations.ValueProviders;

import org.ironsight.wpplugin.expandLayerTool.operations.ProviderType;
import org.pepsoft.worldpainter.Dimension;

import java.awt.*;

public class IntermediateSelectionIO implements IPositionValueSetter, IPositionValueGetter {
    public static IntermediateSelectionIO instance = new IntermediateSelectionIO();
    private static int value;
    private static int lastX;
    private static int lastY;

    private IntermediateSelectionIO() {
    }

    @Override
    public int hashCode() {
        return getProviderType().hashCode();
    }

    public boolean isSelected() {
        return value != 0;
    }
    @Override
    public boolean isVirtual() {
        return false;
    }
    public void setSelected(boolean selected) {
        if (selected) {
            value = 1;
        } else value = 0;
    }

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
        IntermediateSelectionIO.value = value;
        lastX = x;
        lastY = y;
    }

    @Override
    public void prepareForDimension(Dimension dim) {

    }

    @Override
    public String getName() {
        return "Action Filter";
    }

    @Override
    public String getDescription() {
        return "only blocks that are selected will be used in following actions.";
    }

    @Override
    public int getMaxValue() {
        return 1;
    }

    @Override
    public int getMinValue() {
        return 0;
    }

    @Override
    public IMappingValue instantiateFrom(Object[] data) {
        return instance;
    }

    @Override
    public Object[] getSaveData() {
        return new Object[0];
    }

    @Override
    public String valueToString(int value) {
        return value == 1 ? "SELECTED (1)" : "NOT SELECTED (0)";
    }

    @Override
    public boolean isDiscrete() {
        return true;
    }

    @Override
    public void paint(Graphics g, int value, java.awt.Dimension dim) {
        g.setColor(value == 1 ? Color.RED : Color.BLACK);
        g.fillRect(0, 0, dim.width, dim.height);
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.INTERMEDIATE_SELECTION;
    }
}
