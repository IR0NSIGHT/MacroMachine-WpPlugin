package org.ironsight.wpplugin.expandLayerTool.operations.ValueProviders;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.selection.SelectionBlock;

import java.awt.*;

public class SelectionSetter extends BinaryLayerIO  {
        public SelectionSetter() {
            super(SelectionBlock.INSTANCE);
        }
}
