package org.ironsight.wpplugin.expandLayerTool.operations;

import org.pepsoft.worldpainter.Dimension;

public interface IPositionValueGetter extends IDisplayUnit {
    int getValueAt(Dimension dim, int x, int y);

    int getMinValue();
    int getMaxValue();
}
