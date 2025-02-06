package org.ironsight.wpplugin.expandLayerTool.operations.ValueProviders;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.selection.SelectionBlock;

import java.awt.*;

public class BinaryLayerIO implements IPositionValueSetter, IPositionValueGetter {
    private final Layer layer;

    public BinaryLayerIO(Layer layer) {
        assert layer != null;
        assert layer.dataSize.equals(Layer.DataSize.BIT);
        this.layer = layer;
    }

    public String getName() {
        return layer.getName();
    }

    public String getDescription() {
        return layer.getDescription();
    }

    public void setValueAt(Dimension dim, int x, int y, int value) {
        if (value == 2) return;
        dim.setBitLayerValueAt(layer, x, y, value == 1);
    }

    public int getMinValue() {
        return 0;
    }

    public int getMaxValue() {
        return 1;
    }

    public String valueToString(int value) {
        if (value == 0) {
            return "NOT " + layer.getName();
        } else {
            return layer.getName();
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
    public boolean equals(Object obj) {
        return obj != null && this.getClass().equals(obj.getClass());
    }

    public int getValueAt(Dimension dim, int x, int y) {
        return dim.getBitLayerValueAt(SelectionBlock.INSTANCE, x, y) ? 1 : 0;
    }
}
