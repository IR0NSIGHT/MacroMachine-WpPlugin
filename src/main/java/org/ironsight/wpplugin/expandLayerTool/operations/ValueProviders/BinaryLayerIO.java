package org.ironsight.wpplugin.expandLayerTool.operations.ValueProviders;

import org.ironsight.wpplugin.expandLayerTool.operations.ProviderType;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.selection.SelectionBlock;

import java.awt.*;

public class BinaryLayerIO extends NibbleLayerSetter {
    public BinaryLayerIO(Layer layer) {
        super(layer);
        assert layer.dataSize.equals(Layer.DataSize.BIT);
    }



    public void setValueAt(Dimension dim, int x, int y, int value) {
        if (value == 2) return;
        dim.setBitLayerValueAt(layer, x, y, value == 1);
    }

    public int getMaxValue() {
        return 1;
    }

    public int getMinValue() {
        return 0;
    }

    public String valueToString(int value) {
        if (value == 0) {
            return layerName + " ON (1)";
        } else {
            return layerName + " OFF (0)";
        }
    }

    public boolean isDiscrete() {
        return true;
    }

    public void paint(Graphics g, int value, java.awt.Dimension dim) {
        if (value == 0) return;
        g.setColor(Color.red);
        g.fillRect(0, 0, dim.width, dim.height);
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.BINARY_LAYER;
    }

    public int getValueAt(Dimension dim, int x, int y) {
        return dim.getBitLayerValueAt(SelectionBlock.INSTANCE, x, y) ? 1 : 0;
    }
}
