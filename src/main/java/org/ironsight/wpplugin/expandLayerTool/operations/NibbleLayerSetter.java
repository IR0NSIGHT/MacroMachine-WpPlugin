package org.ironsight.wpplugin.expandLayerTool.operations;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.layers.Layer;

import java.awt.*;
import java.util.Objects;

public class NibbleLayerSetter implements IPositionValueSetter, IPositionValueGetter {
    private final Layer layer;

    public NibbleLayerSetter(Layer layer) {
        this.layer = layer;
    }

    @Override
    public String toString() {
        return "NibbleLayerSetter{" + "layer=" + layer + '}';
    }

    @Override
    public void setValueAt(Dimension dim, int x, int y, int value) {
        dim.setLayerValueAt(layer, x, y, value);
    }

    @Override
    public int getMinValue() {
        return 0;
    }

    @Override
    public int getMaxValue() {
        return 15;
    }

    @Override
    public String valueToString(int value) {
        return Integer.toString(value);
    }

    @Override
    public boolean isDiscrete() {
        return false;
    }

    @Override
    public void paint(Graphics g, int value, java.awt.Dimension dim) {
        g.setColor(new Color(255 - 255 * value / 15, 255, 255));
        g.fillRect(0, 0, dim.width, dim.height);
    }

    @Override
    public int getValueAt(Dimension dim, int x, int y) {
        return dim.getLayerValueAt(layer, x, y);
    }

    @Override
    public String getName() {
        return "Set layer " + layer.getName();
    }

    @Override
    public String getDescription() {
        return "Set layer " + layer.getName() + " with values 0 to 15, where 0 is absent, 15 is full";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NibbleLayerSetter that = (NibbleLayerSetter) o;
        return Objects.equals(layer, that.layer);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(layer);
    }
}
