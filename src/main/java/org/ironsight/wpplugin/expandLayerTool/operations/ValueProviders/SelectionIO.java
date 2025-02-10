package org.ironsight.wpplugin.expandLayerTool.operations.ValueProviders;

import org.ironsight.wpplugin.expandLayerTool.operations.ProviderType;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.selection.SelectionBlock;
import org.pepsoft.worldpainter.selection.SelectionChunk;

public class SelectionIO extends BinaryLayerIO {
    public SelectionIO() {
        super(SelectionBlock.INSTANCE);
    }

    @Override
    public int getValueAt(Dimension dim, int x, int y) {
        return dim.getBitLayerValueAt(SelectionBlock.INSTANCE, x, y) ||
                dim.getBitLayerValueAt(SelectionChunk.INSTANCE, x, y) ? 1 : 0;
    }

    @Override
    public IMappingValue instantiateFrom(Object[] data) {
        return new SelectionIO();
    }

    @Override
    public String toString() {
        return "SelectionIO{}";
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.SELECTION;
    }
}
