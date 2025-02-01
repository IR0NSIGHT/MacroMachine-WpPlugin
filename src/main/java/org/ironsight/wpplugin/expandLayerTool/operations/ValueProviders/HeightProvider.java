package org.ironsight.wpplugin.expandLayerTool.operations.ValueProviders;

import org.pepsoft.worldpainter.Dimension;

import java.awt.*;

public class HeightProvider implements IPositionValueGetter {

    public HeightProvider() {
    }

    @Override
    public int getValueAt(Dimension dim, int x, int y) {
        return Math.round(dim.getHeightAt(x, y));
    }

    @Override
    public int getMinValue() {
        return -64;
    }

    @Override
    public int getMaxValue() {
        return 364; //TODO is the correct?
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

        g.setColor(Color.BLUE);
        g.fillRect(0, 0, dim.width, dim.height);
        g.setColor(Color.gray);
        g.fillRect(0, (int) (dim.height * (1-percent)), dim.width, (int)(dim.height * (percent)));
    }

    @Override
    public String getName() {
        return "Get Height";
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && this.getClass().equals(obj.getClass());
    }

    @Override
    public String getDescription() {
        return "get the height of a position in percent for 0 to 255.";
    }
}
