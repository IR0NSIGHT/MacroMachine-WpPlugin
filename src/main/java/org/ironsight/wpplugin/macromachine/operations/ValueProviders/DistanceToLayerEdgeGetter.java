package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import org.ironsight.wpplugin.macromachine.operations.LayerObjectContainer;
import org.ironsight.wpplugin.macromachine.operations.ProviderType;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.layers.Layer;

import java.awt.*;
import java.util.Objects;

public class DistanceToLayerEdgeGetter implements IPositionValueGetter, ILayerGetter {
    protected String layerId;
    protected String layerName;
    protected Layer layer = null;

    protected DistanceToLayerEdgeGetter(String name, String id) {
        this.layerId = id;
        this.layerName = name;
    }

    public DistanceToLayerEdgeGetter() {
    }

    public DistanceToLayerEdgeGetter(Layer layer) {
        this.layerName = layer.getName();
        this.layerId = layer.getId();
    }

    @Override
    public void prepareForDimension(Dimension dim) {
        LayerObjectContainer.getInstance().setDimension(dim);
        layer = LayerObjectContainer.getInstance().queryLayer(layerId);
        if (layer == null)
            throw new IllegalAccessError("Layer not found: " + layerName + "(" + layerId + ")");
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
        return new DistanceToLayerEdgeGetter((String) data[0], (String) data[1]);
    }

    @Override
    public Object[] getSaveData() {
        return new Object[]{layerName, layerId};
    }

    @Override
    public String valueToString(int value) {
        if (value == 0) return "outside";
        if (value == getMaxValue()) return "inside";
        return value + "m from edge";
    }

    @Override
    public boolean isDiscrete() {
        return false;
    }

    @Override
    public void paint(Graphics g, int value, java.awt.Dimension dim) {
        float point = ((1f * getMaxValue() - value) / getMaxValue());
        Color c = new Color(0, point, 0, 1);
        System.out.println("point " + point + " => " + c);
        g.setColor(c);
        g.fillRect(0, 0, dim.width, dim.height);
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.DISTANCE_TO_EDGE;
    }

    /**
     *
     * @param dim
     * @param x
     * @param y
     * @return: 0 = not in layer, 1-max = distance to edge, max+: somewhere inside layer but further away than max
     */
    @Override
    public int getValueAt(Dimension dim, int x, int y) {
        float dist = dim.getDistanceToEdge(layer, x, y, getMaxValue());
        return Math.round(dist);
    }

    @Override
    public String getName() {
        return "distance to " + layerName;
    }

    @Override
    public String getDescription() {
        return "distance to edge of " + layerName + " with distance 0 to " + getMaxValue() +
                " where 0 is right at the " +
                "edge. possibly very slow operation depending on how many blocks it has to check.";
    }

    @Override
    public int hashCode() {
        return Objects.hash(getProviderType(), layerId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DistanceToLayerEdgeGetter that = (DistanceToLayerEdgeGetter) o;
        return Objects.equals(layerId, that.layerId);
    }

    @Override
    public boolean isVirtual() {
        return false;
    }

    @Override
    public String toString() {
        return "DistanceToEdge{" + "layer=" + layerName + '}';
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
