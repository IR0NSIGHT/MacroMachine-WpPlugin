package org.ironsight.wpplugin.expandLayerTool.operations;

import org.pepsoft.worldpainter.Dimension;

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
