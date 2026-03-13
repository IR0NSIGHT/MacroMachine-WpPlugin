package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import org.ironsight.wpplugin.macromachine.operations.ProviderType;
import org.pepsoft.worldpainter.Dimension;

import java.awt.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.IntStream;

import static org.pepsoft.worldpainter.Constants.TILE_SIZE_BITS;

public class WaterHeightAbsoluteIO implements IPositionValueGetter, IPositionValueSetter, EditableIO
{
    private final int min, max;
    public static final int IGNORE = Integer.MAX_VALUE;
    private final int[] outputValues;
    private final int[] inputValues;

    public WaterHeightAbsoluteIO(int min, int max) {
        this.min = min;
        this.max = max;
        outputValues = IntStream.range(min - 1, max + 1).toArray();
        outputValues[0] = IGNORE;
        inputValues = IntStream.range(min, max + 1).toArray();
    }

    @Override
    public boolean isIgnoreValue(int value) {
        return value == IGNORE;
    }

    @Override
    public int[] getAllOutputValues() {
        return Arrays.copyOf(outputValues, outputValues.length);
    }

    @Override
    public int[] getAllInputValues() {
        return Arrays.copyOf(inputValues, inputValues.length);
    }

    @Override
    public int getValueAt(Dimension dim, int x, int y) {
        if (!dim.getExtent().contains(x >> TILE_SIZE_BITS, y >> TILE_SIZE_BITS))
            return getMinValue();
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
        return "water level";
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
            return new WaterHeightAbsoluteIO(-64, 319);
        return new WaterHeightAbsoluteIO((Integer) data[0], (Integer) data[1]);
    }

    @Override
    public Object[] getSaveData() {
        return new Object[]{min, max};
    }

    @Override
    public String valueToString(int value) {
        if (value == IGNORE)
            return "Skip";
        return Integer.toString(value);
    }

    @Override
    public boolean isDiscrete() {
        return false;
    }

    @Override
    public int[] getAllPossibleValues() {
        return getAllOutputValues();
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
        if (isIgnoreValue(value)) {
            return;
        }
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
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        WaterHeightAbsoluteIO that = (WaterHeightAbsoluteIO) o;
        return min == that.min && max == that.max;
    }

    @Override
    public int[] getEditableValues() {
        return new int[]{min, max};
    }

    @Override
    public String[] getValueNames() {
        return new String[]{"min", "max"};
    }

    @Override
    public String[] getValueTooltips() {
        return new String[]{"lowest allowed value", "highest allowed value"};
    }

    @Override
    public EditableIO instantiateWithValues(int[] values) {
        return new WaterHeightAbsoluteIO(values[0], values[1]);
    }

    @Override
    public String getToolTipText() {
        return getDescription();
    }
}
