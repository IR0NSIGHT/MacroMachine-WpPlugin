package org.ironsight.wpplugin.macromachine.operations;

import org.pepsoft.worldpainter.Dimension;

public interface ILimitedMapOperation {
    void prepareRightBeforeRun(Dimension dimension, int[] tileX, int[] tileY);
}
