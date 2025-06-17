package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import org.ironsight.wpplugin.macromachine.operations.ProviderType;
import org.pepsoft.worldpainter.Dimension;

import java.awt.*;
import java.util.Objects;

public class WaterHeightAbsoluteIO implements IPositionValueGetter, IPositionValueSetter, EditableIO  {
    private final int min, max;
    public WaterHeightAbsoluteIO(int min, int max) {
        this.min = min;
        this.max = max;
    }
    @Override
    public int getValueAt(Dimension dim, int x, int y) {
        return dim.getWaterLevelAt(x, y);
    }
    @Override
    public String toString() {
        return getName();
    }
    @Override
    public void setValueAt(Dimension dim, int x, int y, int value) {
        dim.setWaterLevelAt(x, y, value);
    }

    @Override
    public String getName() {
        return "water level absolute";
    }

    @Override
    public int hashCode() {
        return Objects.hash(min, max);
    }

    @Override
    public String getDescription() {
        return "absolute water level";
    }

    @Override
    public IMappingValue instantiateFrom(Object[] data) {
        if (data.length == 0)
            return new WaterHeightAbsoluteIO(-64,319);
        return new WaterHeightAbsoluteIO((Integer)data[0],(Integer)data[1]);
    }

    @Override
    public Object[] getSaveData() {
        return new Object[]{min,max};
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
    public boolean isVirtual() {
        return false;
    }

    @Override
    public int getMaxValue() {
        return max;
    }

    @Override
    public int getMinValue() {
        return min;
    }

    @Override
    public void prepareForDimension(Dimension dim) throws IllegalAccessError {

    }

    @Override
    public void paint(Graphics g, int value, java.awt.Dimension dim) {
        float percent = (value - getMinValue() * 1f) / (getMaxValue() - getMinValue());

        g.setColor(Color.GRAY);
        g.fillRect(0, 0, dim.width, dim.height);
        g.setColor(Color.BLUE);
        g.fillRect(0, (int) (dim.height * (1 - percent)), dim.width, (int) (dim.height * (percent)));
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.WATER_HEIGHT;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WaterHeightAbsoluteIO that = (WaterHeightAbsoluteIO) o;
        return min == that.min && max == that.max;
    }

    @Override
    public int[] getEditableValues() {
        return new int[]{min,max};
    }

    @Override
    public String[] getValueNames() {
        return new String[]{"min","max"};
    }

    @Override
    public String[] getValueTooltips() {
        return new String[]{"lowest allowed value","highest allowed value"};
    }

    @Override
    public EditableIO instantiateWithValues(int[] values) {
        return new WaterHeightAbsoluteIO(values[0],values[1]);
    }
    @Override
    public String getToolTipText() {
        return getDescription();
    }
}
