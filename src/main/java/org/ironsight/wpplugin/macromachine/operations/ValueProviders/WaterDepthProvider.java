package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import org.ironsight.wpplugin.macromachine.operations.ProviderType;
import org.pepsoft.worldpainter.Dimension;

import java.awt.*;

public class WaterDepthProvider implements IPositionValueSetter, IPositionValueGetter {
    @Override
    public int getValueAt(Dimension dim, int x, int y) {
        int value = Math.min(100, Math.max(0, Math.round(dim.getWaterLevelAt(x, y) - dim.getHeightAt(x, y))));
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && this.getClass().equals(obj.getClass());
    }

    @Override
    public void setValueAt(Dimension dim, int x, int y, int value) {
        dim.setHeightAt(x, y, Math.round(dim.getHeightAt(x, y) - value));
    }

    @Override
    public void prepareForDimension(Dimension dim) {

    }

    @Override
    public String getName() {
        return "Water Depth";
    }

    @Override
    public String getDescription() {
        return "height of terrain below water level. only changes terrain, not water!";
    }

    @Override
    public int getMinValue() {
        return 0;
    }

    @Override
    public IMappingValue instantiateFrom(Object[] data) {
        return new WaterDepthProvider();
    }

    @Override
    public Object[] getSaveData() {
        return new Object[0];
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
        g.setColor(Color.GRAY);
        g.fillRect(0, 0, dim.width, dim.height);


        float percent = (value - getMinValue() * 1f) / (getMaxValue() - getMinValue());
        g.setColor(Color.BLUE);
        g.fillRect(0, 0, dim.width, (int) (dim.height * (percent)));
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.WATER_DEPTH;
    }

    @Override
    public boolean isVirtual() {
        return false;
    }

    @Override
    public String getToolTipText() {
        return getDescription();
    }

    @Override
    public int hashCode() {
        return getProviderType().hashCode();
    }
}
