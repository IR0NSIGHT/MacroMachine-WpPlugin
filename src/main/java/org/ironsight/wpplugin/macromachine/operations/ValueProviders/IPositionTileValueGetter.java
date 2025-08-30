package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import org.pepsoft.worldpainter.Tile;

import java.io.Serializable;

public interface IPositionTileValueGetter extends IDisplayUnit, Serializable, IMappingValue {
    int getValueAt(Tile tile, int tileX, int tileY);
}
