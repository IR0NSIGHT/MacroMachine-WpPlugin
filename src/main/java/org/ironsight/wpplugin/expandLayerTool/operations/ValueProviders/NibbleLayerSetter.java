package org.ironsight.wpplugin.expandLayerTool.operations.ValueProviders;

import org.ironsight.wpplugin.expandLayerTool.operations.ProviderType;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.layers.LayerManager;

import java.awt.*;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Objects;

public class NibbleLayerSetter implements IPositionValueSetter, IPositionValueGetter, ILayerGetter {
    private final static Color[] COLORS = new Color[]{new Color(0, 0, 0),       // Black
            new Color(0, 0, 0),       // Black
            new Color(0, 16, 0),      // Very dark green
            new Color(0, 32, 0),      // Dark green
            new Color(0, 48, 0),      // Darker green
            new Color(0, 64, 0),      // Medium-dark green
            new Color(0, 80, 0),      // Medium green
            new Color(0, 96, 0),      // Slightly lighter green
            new Color(0, 112, 0),     // Light green
            new Color(0, 128, 0),     // Lighter green
            new Color(0, 144, 0),     // Bright green
            new Color(0, 160, 0),     // Brighter green
            new Color(0, 176, 0),     // Vibrant green
            new Color(0, 192, 0),     // Very vibrant green
            new Color(0, 208, 0),     // Almost neon green
            new Color(0, 224, 0),     // Neon green
            new Color(0, 255, 0)      // Pure green
    };
    protected String layerId;
    protected String layerName;
    protected Layer layer = null;

    protected NibbleLayerSetter(String name, String id) {
        this.layerId = id;
        this.layerName = name;
    }

    public NibbleLayerSetter() {
    }

    public NibbleLayerSetter(Layer layer) {
        this.layerName = layer.getName();
        this.layerId = layer.getId();
    }

    @Override
    public void setValueAt(Dimension dim, int x, int y, int value) {
        dim.setLayerValueAt(layer, x, y, value);
    }

    @Override
    public void prepareForDimension(Dimension dim) {
        Collection<Layer> allLayers = new LinkedList<>();
        allLayers.addAll(dim.getCustomLayers());
        allLayers.addAll(LayerManager.getInstance().getLayers());
        allLayers.stream()
                .filter(f -> f.getId().equals(layerId))
                .findFirst()
                .map(l -> this.layer = l)
                .orElseThrow(() -> new IllegalAccessError("Layer not found: " + layerName + "(" + layerId + ")"));
    }

    @Override
    public int getMaxValue() {
        return 15;
    }

    @Override
    public int getMinValue() {
        return 0;
    }

    @Override
    public IMappingValue instantiateFrom(Object[] data) {
        return new NibbleLayerSetter((String) data[0], (String) data[1]);
    }

    @Override
    public Object[] getSaveData() {
        return new Object[]{layerName, layerId};
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
        g.setColor(COLORS[value]);
        g.fillRect(0, 0, dim.width, dim.height);
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.NIBBLE_LAYER;
    }

    @Override
    public int getValueAt(Dimension dim, int x, int y) {
        return dim.getLayerValueAt(layer, x, y);
    }

    @Override
    public String getName() {
        return layerName;
    }

    @Override
    public String getDescription() {
        return "layer " + layerName + " with values 0 to 15, where 0 is absent, 15 is full";
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(layerId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NibbleLayerSetter that = (NibbleLayerSetter) o;
        return Objects.equals(layerId, that.layerId);
    }

    @Override
    public boolean isVirtual() {
        return false;
    }

    @Override
    public String toString() {
        return "NibbleLayerSetter{" + "layer=" + layerName + ", layerId=" + layerId + '}';
    }

    @Override
    public String getLayerName() {
        return layerName;
    }

    @Override
    public String getLayerId() {
        return layerId;
    }
}
