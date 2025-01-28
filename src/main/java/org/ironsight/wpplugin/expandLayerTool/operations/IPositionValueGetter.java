package org.ironsight.wpplugin.expandLayerTool.operations;

import org.pepsoft.worldpainter.Dimension;

import java.io.Serializable;

public interface IPositionValueGetter extends IDisplayUnit, Serializable {
    int getValueAt(Dimension dim, int x, int y);

    int getMinValue();
    int getMaxValue();
    String valueToString(int value);
}
