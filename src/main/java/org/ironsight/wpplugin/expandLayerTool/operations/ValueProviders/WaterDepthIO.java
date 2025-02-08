package org.ironsight.wpplugin.expandLayerTool.operations.ValueProviders;

import org.pepsoft.worldpainter.Dimension;

import java.awt.*;

public class WaterDepthIO implements IPositionValueSetter, IPositionValueGetter {
    @Override
    public int getValueAt(Dimension dim, int x, int y) {
        return Math.round(dim.getHeightAt(x, y) - dim.getWaterLevelAt(x, y));
    }

    @Override
    public void setValueAt(Dimension dim, int x, int y, int value) {
        dim.setHeightAt(x, y, Math.round(dim.getHeightAt(x, y) - value));
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
}
