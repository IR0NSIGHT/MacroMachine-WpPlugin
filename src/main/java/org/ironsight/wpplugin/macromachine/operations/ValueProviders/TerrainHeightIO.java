package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import org.ironsight.wpplugin.macromachine.operations.ProviderType;
import org.pepsoft.worldpainter.Dimension;

import java.awt.*;

public class TerrainHeightIO implements IPositionValueGetter, IPositionValueSetter, EditableIO {
    private final int minHeight;
    private final int maxHeight;

    public TerrainHeightIO(int minHeight, int maxHeight) {
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
    }

    @Override
    public String toString() {
        return getName();
    }
    @Override
    public String getToolTipText() {
        return getDescription();
    }
    @Override
    public int getValueAt(Dimension dim, int x, int y) {
        return (int)EditableIO.clamp(Math.round(dim.getHeightAt(x, y)),getMinValue(),getMaxValue());
    }

    @Override
    public String getName() {
        return "Height";
    }

    @Override
    public int hashCode() {
        return getProviderType().hashCode();
    }

    @Override
    public boolean isVirtual() {
        return false;
    }

    @Override
    public String getDescription() {
        return "get the height of a position in percent for 0 to 255.";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TerrainHeightIO that = (TerrainHeightIO) o;
        return minHeight == that.minHeight && maxHeight == that.maxHeight;
    }

    @Override
    public void setValueAt(Dimension dim, int x, int y, int value) {
        dim.setHeightAt(x, y, value);
    }


    @Override
    public int getMinValue() {
        return minHeight;
    }


    @Override
    public IMappingValue instantiateFrom(Object[] data) {
        if (data.length == 0)
            return new TerrainHeightIO(-64,319);
        int minHeight = (int)data[0];
        int maxHeight = (int)data[1];
        return new TerrainHeightIO(minHeight,maxHeight);
    }


    @Override
    public Object[] getSaveData() {
        return new Object[]{(Integer)minHeight,(Integer)maxHeight};
    }


    @Override
    public int getMaxValue() {
        return maxHeight;
    }

    @Override
    public String valueToString(int value) {
        return value + "H";
    }

    @Override
    public boolean isDiscrete() {
        return false;
    }

    @Override
    public void paint(Graphics g, int value, java.awt.Dimension dim) {
        float percent = (value - getMinValue() * 1f) / (getMaxValue() - getMinValue());

        g.setColor(new Color(131, 154, 255));
        g.fillRect(0, 0, dim.width, dim.height);
        g.setColor(new Color(0, 142, 7));
        g.fillRect(0, (int) (dim.height * (1 - percent)), dim.width, (int) (dim.height * (percent)));
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.HEIGHT;
    }


    @Override
    public void prepareForDimension(Dimension dim) {

    }

    @Override
    public int[] getEditableValues() {
        return new int[]{minHeight,maxHeight};
    }

    @Override
    public String[] getValueNames() {
        return new String[]{"min","max"};
    }

    @Override
    public String[] getValueTooltips() {
        return new String[]{"lowest value allowed","highest value allowed"};
    }

    @Override
    public EditableIO instantiateWithValues(int[] values) {
        return new TerrainHeightIO(values[0],values[1]);
    }


}
