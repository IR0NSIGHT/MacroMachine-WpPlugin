package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import org.ironsight.wpplugin.macromachine.operations.ProviderType;
import org.pepsoft.worldpainter.Dimension;

import java.awt.*;

public class WaterHeightAbsoluteIO extends TerrainHeightIO {

    @Override
    public int getValueAt(Dimension dim, int x, int y) {
        return dim.getWaterLevelAt(x, y);
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
    public String getDescription() {
        return "absolute water level";
    }

    @Override
    public IMappingValue instantiateFrom(Object[] data) {
        return new WaterHeightAbsoluteIO();
    }

    @Override
    public String valueToString(int value) {
        return Integer.toString(value);
    }
    @Override
    public boolean isVirtual() {
        return false;
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
    public boolean equals(Object obj) {
        return obj != null && this.getClass() == obj.getClass();
    }
}
