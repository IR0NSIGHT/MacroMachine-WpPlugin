package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Tile;

import java.io.Serializable;

public interface IPositionValueGetter extends IDisplayUnit, Serializable, IMappingValue {
    int getValueAt(Dimension dim, int x, int y);
}



