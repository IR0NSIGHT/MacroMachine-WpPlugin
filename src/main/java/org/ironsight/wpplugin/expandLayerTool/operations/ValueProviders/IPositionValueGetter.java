package org.ironsight.wpplugin.expandLayerTool.operations.ValueProviders;

import org.pepsoft.worldpainter.Dimension;

import java.io.Serializable;

public interface IPositionValueGetter extends IDisplayUnit, Serializable, IMappingValue {
    int getValueAt(Dimension dim, int x, int y);
}
