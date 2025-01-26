package org.ironsight.wpplugin.expandLayerTool.operations;

import org.pepsoft.worldpainter.Dimension;

public interface IPositionValueSetter extends IDisplayUnit {
    void setValueAt(Dimension dim, int x, int y, int value);
    int getMinValue();
    int getMaxValue();
    String valueToString(int value);
}
