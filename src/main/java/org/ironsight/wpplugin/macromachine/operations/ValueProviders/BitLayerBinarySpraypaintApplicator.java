package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import org.ironsight.wpplugin.macromachine.operations.LayerObjectContainer;
import org.ironsight.wpplugin.macromachine.operations.ProviderType;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.layers.Layer;

import java.awt.*;
import java.util.Objects;
import java.util.Random;

public class BitLayerBinarySpraypaintApplicator implements IPositionValueSetter, ILayerGetter {
    Random random = new Random();
    private String layerName;
    private String layerId;
    private Layer layer;
    private boolean isCustom = false;

    public BitLayerBinarySpraypaintApplicator() {
        super();
    }

    public BitLayerBinarySpraypaintApplicator(Layer layer, boolean isCustom) {
        this.layer = layer;
        this.layerId = layer.getId();
        this.layerName = layer.getName();
        this.isCustom = isCustom;
    }

    BitLayerBinarySpraypaintApplicator(String layerId, String layerName, boolean isCustom) {
        this.layerId = layerId;
        this.layerName = layerName;
        this.isCustom = isCustom;
    }

    @Override
    public boolean isVirtual() {
        return false;
    }

    /**
     * @param dim
     * @param x
     * @param y
     * @param value 0 to 100 chance
     */
    @Override
    public void setValueAt(Dimension dim, int x, int y, int value) {
        dim.setBitLayerValueAt(layer, x, y, doPaintPos(x, y, value));
    }

    @Override
    public int getMaxValue() {
        return 100;
    }

    @Override
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

    @Override
    public IMappingValue instantiateFrom(Object[] data) {
        Object[] saveData = new Object[]{"Macro Selection", "org.ironsight.wpplugin.macropainter.macroselectionlayer"
                , false};
        for (int i = 0; i < data.length; i++) {
            saveData[i] = data[i];
        }
        return new BitLayerBinarySpraypaintApplicator((String) saveData[0],
                (String) saveData[1],
                (Boolean) saveData[2]);
    }

    @Override
    public Object[] getSaveData() {
        return new Object[]{layerId, layerName, isCustom};
    }

    @Override
    public String valueToString(int value) {
        return value + "%";
    }

    @Override
    public boolean isDiscrete() {
        return false;
    }

    @Override
    public void paint(Graphics g, int value, java.awt.Dimension dim) {
        //value is 0 to 100
        g.setColor(Color.black);
        g.fillRect(0, 0, dim.width, dim.height);
        g.setColor(Color.RED);
        int range = dim.width < 100 ? 10 : 100;
        int cellSize = dim.width / range;
        for (int y = 0; y <= range; y++) {
            for (int x = 0; x <= range; x++) {
                if (doPaintPos(x, y, value)) g.fillRect(x * cellSize, y * cellSize, cellSize, cellSize);
            }
        }
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.BINARY_SPRAYPAINT;
    }

    @Override
    public String getName() {
        return layerName  + (isCustom ? " (Custom) layer" : " layer")+ " spraypaint";
    }

    @Override
    public String getDescription() {
        return "spraypaint binary layer " + layerName + " (ON or OFF) based on input chance 0 to 100%.";
    }

    @Override
    public int hashCode() {
        return Objects.hash(layer);
    }

    private boolean doPaintPos(int x, int y, int value) {
        assert random != null;
        long positionHash = ((long) x * 73856093L) ^ ((long) y * 19349663L);
        random.setSeed(positionHash);
        int randInt = random.nextInt(100);
        boolean set = value > randInt;
        return set;
    }

    @Override
    public String toString() {
        return layerName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BitLayerBinarySpraypaintApplicator that = (BitLayerBinarySpraypaintApplicator) o;
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
    public Layer getLayer() {
        return layer;
    }


}
