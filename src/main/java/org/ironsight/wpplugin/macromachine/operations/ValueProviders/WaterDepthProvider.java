package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import org.ironsight.wpplugin.macromachine.operations.ProviderType;
import org.pepsoft.worldpainter.Dimension;

import java.awt.*;
import java.util.Collection;

import static org.pepsoft.worldpainter.Constants.TILE_SIZE_BITS;

public class WaterDepthProvider implements IPositionValueSetter, IPositionValueGetter {
    private final static Color LAND_GREEN = new Color(43, 157, 0);
    private final static Color SHORE_BLUE = new Color(159, 181, 255);
    private final static Color DEEP_BLUE = new Color(0, 46, 171);

    @Override
    public int getValueAt(Dimension dim, int x, int y) {
        if (!dim.getExtent().contains(x >> TILE_SIZE_BITS, y >> TILE_SIZE_BITS))
            return getMinValue();
        int value = Math.min(getMaxValue(), Math.max(0, getWaterDepthRaw(dim, x, y)));
        return value;
    }

    private int getWaterDepthRaw(Dimension dim, int x, int y) {
        return Math.round(dim.getWaterLevelAt(x, y) - dim.getHeightAt(x, y));
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && this.getClass().equals(obj.getClass());
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public void setValueAt(Dimension dim, int x, int y, int value) {
        int targetDepth = value;
        int currentDepth = getWaterDepthRaw(dim, x, y);
        int diff = currentDepth - targetDepth;
        dim.setHeightAt(x, y, Math.round(dim.getHeightAt(x, y) + diff));
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
        if (value == 0)
            return "Land (0)";
        return String.format("Water (%d)", value);
    }

    @Override
    public boolean isDiscrete() {
        return false;
    }

    @Override
    public void paint(Graphics g, int value, java.awt.Dimension dim) {
        g.setColor(LAND_GREEN);
        g.fillRect(0, 0, dim.width, dim.height);

        if (value == getMinValue())
            return;
        float percent = (value - getMinValue() * 1f) / (getMaxValue() - getMinValue());
        int r = (int) (DEEP_BLUE.getRed() * percent + (1 - percent) * SHORE_BLUE.getRed());
        int gg = (int) (DEEP_BLUE.getGreen() * percent + (1 - percent) * SHORE_BLUE.getGreen());
        int b = (int) (DEEP_BLUE.getBlue() * percent + (1 - percent) * SHORE_BLUE.getBlue());
        g.setColor(new Color(r, gg, b));
        g.fillRect(0, 0, dim.width, (int) (dim.height));
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
