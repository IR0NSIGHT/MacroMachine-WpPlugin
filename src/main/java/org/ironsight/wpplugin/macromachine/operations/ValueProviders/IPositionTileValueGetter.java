package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import java.io.Serializable;
import org.pepsoft.worldpainter.Tile;

public interface IPositionTileValueGetter extends IDisplayUnit, Serializable, IMappingValue
{
    int getValueAt(Tile tile, int tileX, int tileY);

    int[] getAllInputValues();
}
