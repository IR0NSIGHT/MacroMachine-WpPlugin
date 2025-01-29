package org.ironsight.wpplugin.expandLayerTool.operations;

import org.pepsoft.worldpainter.Dimension;

public class SlopeProvider implements IPositionValueGetter {
    /**
     * slope in degrees 0-90
     *
     * @param dim
     * @param x
     * @param y
     * @return
     */
    @Override
    public int getValueAt(Dimension dim, int x, int y) {
        return (int) Math.round(Math.toDegrees(Math.atan(dim.getSlope(x, y))));
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && this.getClass().equals(obj.getClass());
    }

    @Override
    public int getMinValue() {
        return 0;
    }

    @Override
    public int getMaxValue() {
        return 90;
    }

    @Override
    public String valueToString(int value) {
        return value + "°";
    }

    @Override
    public String getName() {
        return "Get Slope";
    }

    @Override
    public String getDescription() {
        return "get the slope of a position in degrees from 0 to 90°";
    }
}
