package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import org.ironsight.wpplugin.macromachine.operations.ProviderType;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.layers.renderers.NibbleLayerRenderer;

import java.awt.*;
import java.util.ArrayList;
import java.util.Objects;

public class NibbleLayerSetter implements IPositionValueSetter, IPositionValueGetter, ILayerGetter {
    private final Color[] COLORS = new Color[]{new Color(0, 0, 0),       // Black
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

    private final static int[] defaultColorHex = new int[16];

    private int[] colorHex;
    protected String layerId;
    protected String layerName;
    protected Layer layer = null;
    private boolean isCustom;

    protected NibbleLayerSetter(String name, String id, boolean isCustom, int[] colorHex) {
        this.layerId = id;
        this.layerName = name;
        this.isCustom = isCustom;
        this.colorHex = colorHex;
        setColorsFromData(colorHex);
    }

    public NibbleLayerSetter(Layer layer, boolean isCustom) {
        this(layer.getName(), layer.getId(), isCustom, defaultColorHex.clone());
        this.layer = layer;
        pullColorsFromLayer();
    }

    private void pullColorsFromLayer() {
        if (layer.getRenderer() == null || !(layer.getRenderer() instanceof NibbleLayerRenderer))
            return;
        NibbleLayerRenderer renderer = (NibbleLayerRenderer) layer.getRenderer();
        for (int value = getMinValue(); value <= getMaxValue(); value++) {
            colorHex[value] = renderer.getPixelColour(0,0,0,value);
        }
        setColorsFromData(colorHex);
    }

    private void setColorsFromData(int[] colorHex) {
        for (int value = getMinValue(); value <= getMaxValue(); value++) {
            COLORS[value] = new Color(colorHex[value]);
        }
    }

    @Override
    public void setValueAt(Dimension dim, int x, int y, int value) {
        dim.setLayerValueAt(layer, x, y, value);
    }

    @Override
    public void prepareForDimension(Dimension dim) {
        if (layer == null) {
            layer = InputOutputProvider.INSTANCE.getLayerById(layerId, f -> {});
        }
        if (layer == null)
            throw new IllegalAccessError("Layer not found: " + layerName + "(" + layerId + ")");
        if (layer != null) {
            layerName = layer.getName(); //maybe name was updated
            pullColorsFromLayer();
        }
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
        Object[] saveData = new Object[]{"Macro Selection", "org.ironsight.wpplugin.macropainter.macroselectionlayer"
                , false, defaultColorHex};
        for (int i = 0; i < data.length; i++) {
            saveData[i] = data[i];
        }
        try {
            int[] colorHex = new int[16]; int i = 0;
            for (Integer hex: ((ArrayList<Integer>) saveData[3]))
                colorHex[i++] = hex;
            return new NibbleLayerSetter((String) saveData[0], (String) saveData[1], (Boolean) saveData[2],
                    colorHex);
        } catch (ClassCastException ex) {
            return new NibbleLayerSetter((String) saveData[0], (String) saveData[1], (Boolean) saveData[2],
                    defaultColorHex);
        }
    }

    @Override
    public Object[] getSaveData() {
        return new Object[]{layerName, layerId, isCustom, colorHex};
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
        return layerName + (isCustom ? " (Custom) layer" : " layer");
    }

    @Override
    public String getDescription() {
        return "layer " + layerName + " with values 0 to 15, where 0 is absent, 15 is full";
    }

    @Override
    public String getToolTipText() {
        return getDescription();
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
        return getName();
    }

    @Override
    public String getLayerName() {
        return layerName;
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
    public Layer getLayer() {
        return layer;
    }
}
