package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import org.ironsight.wpplugin.macromachine.operations.LayerObjectContainer;
import org.ironsight.wpplugin.macromachine.operations.ProviderType;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.selection.SelectionBlock;

import java.awt.*;
import java.util.Objects;

public class BinaryLayerIO implements IPositionValueSetter, IPositionValueGetter, ILayerGetter {
    private String layerName;
    private final String layerId;
    boolean isCustom = false;
    private Layer layer;

    public BinaryLayerIO(Layer layer, boolean isCustom) {
        this.layerId = layer.getId();
        this.layerName = layer.getName();
        this.layer = layer;
        this.isCustom = isCustom;
        assert layer.dataSize.equals(Layer.DataSize.BIT);
    }

    BinaryLayerIO(String name, String id, boolean isCustom) {
        this.layerId = id;
        this.layerName = name;
        this.isCustom = isCustom;
    }

    @Override
    public String getName() {
        return layerName + (isCustom ? " (Custom) layer" : " layer");
    }

    public void setValueAt(Dimension dim, int x, int y, int value) {
        assert layer != null;
        assert value == 0 || value == 1;
        dim.setBitLayerValueAt(layer, x, y, value == 1);
    }

    public int getMaxValue() {
        return 1;
    }

    public int getMinValue() {
        return 0;
    }

    @Override
    public void prepareForDimension(Dimension dim) {
        if (layer == null) {
            LayerObjectContainer.getInstance().setDimension(dim);
            layer = LayerObjectContainer.getInstance().queryLayer(layerId);
        }
        if (layer == null)
            throw new IllegalAccessError("Layer not found: " + layerName + "(" + layerId + ")");
        if (layer != null)
            layerName = layer.getName(); //maybe name was updated
    }

    public String valueToString(int value) {
        if (value == 0) {
            return layerName + " OFF (" + value + ")";
        } else {
            return layerName + " ON (" + value + ")";
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
    public IMappingValue instantiateFrom(Object[] data) {
        Object[] saveData = new Object[]{"Macro Selection", "org.ironsight.wpplugin.macropainter.macroselectionlayer"
                , false};
        for (int i = 0; i < data.length; i++) {
            saveData[i] = data[i];
        }
        return new BinaryLayerIO((String) saveData[0], (String) saveData[1], (Boolean) saveData[2]);
    }

    @Override
    public Object[] getSaveData() {
        return new Object[]{layerName, layerId, isCustom};
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.BINARY_LAYER;
    }

    public int getValueAt(Dimension dim, int x, int y) {
        return dim.getBitLayerValueAt(layer, x, y) ? 1 : 0;
    }

    @Override
    public String getDescription() {
        return "binary (ON or OFF) layer " + layerName;
    }

    @Override
    public int hashCode() {
        return Objects.hash(layerId);
    }

    @Override
    public boolean isVirtual() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BinaryLayerIO that = (BinaryLayerIO) o;
        return Objects.equals(layerId, that.layerId);
    }

    @Override
    public String getLayerName() {
        return layerName;
    }

    @Override
    public String getToolTipText() {
        return getDescription();
    }

    @Override
    public String getLayerId() {
        return layerId;
    }

    @Override
    public boolean isCustomLayer() {
        return isCustom;
    }

    @Override
    public String toString() {
        return layerName;
    }
}
