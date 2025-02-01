package org.ironsight.wpplugin.expandLayerTool.operations;

import org.pepsoft.worldpainter.Dimension;

import java.io.Serializable;

public interface IPositionValueSetter extends IDisplayUnit, Serializable, IMappingValue {
    void setValueAt(Dimension dim, int x, int y, int value);

}
