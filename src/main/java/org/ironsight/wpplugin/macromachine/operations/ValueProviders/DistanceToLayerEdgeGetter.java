package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import org.ironsight.wpplugin.macromachine.ArrayUtils;
import org.ironsight.wpplugin.macromachine.MacroSelectionLayer;
import org.ironsight.wpplugin.macromachine.operations.ILimitedMapOperation;
import org.ironsight.wpplugin.macromachine.operations.ProviderType;
import org.ironsight.wpplugin.macromachine.operations.specialOperations.ShadowMap;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.layers.Layer;

import java.awt.*;
import java.util.Objects;

public class DistanceToLayerEdgeGetter implements IPositionValueGetter, ILayerGetter, ILimitedMapOperation {
    protected String layerId;
    protected String layerName;
    protected Layer layer = null;
    private TileContainer distanceMap;

    protected DistanceToLayerEdgeGetter(String name, String id) {
        this.layerId = id;
        this.layerName = name;
    }

    public DistanceToLayerEdgeGetter() {
    }

    public DistanceToLayerEdgeGetter(Layer layer) {
        assert layer.dataSize == Layer.DataSize.BIT;
        this.layerName = layer.getName();
        this.layerId = layer.getId();
    }

    @Override
    public void prepareForDimension(Dimension dim) {
        layer = InputOutputProvider.INSTANCE.getLayerById(layerId, f -> {
        });
        if (layer == null)
            throw new IllegalAccessError("Layer not found: " + layerName + "(" + layerId + ")");
        if (layer != null)
            layerName = layer.getName(); //maybe name was updated

    }

    @Override
    public int getMaxValue() {
        return 1000;
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
        if (value == 0) return "inside";
        if (value == getMaxValue()) return "outside";
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
        g.setColor(c);
        g.fillRect(0, 0, dim.width, dim.height);
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.DISTANCE_TO_EDGE;
    }

    /**
     * @param dim
     * @param x
     * @param y
     * @return: 0 = not in layer, 1-max = distance to edge, max+: somewhere inside layer but further away than max
     */
    @Override
    public int getValueAt(Dimension dim, int x, int y) {
        return Math.min(getMaxValue(), distanceMap.getValueAt(x,y));
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
        return getName();
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
        return false;
    }

    @Override
    public Layer getLayer() {
        return layer;
    }

    @Override
    public void prepareRightBeforeRun(Dimension dimension, int[] tileX, int[] tileY) {
        int startX = ArrayUtils.findMin(tileX), endX = ArrayUtils.findMax(tileX);
        int startY = ArrayUtils.findMin(tileY), endY = ArrayUtils.findMax(tileY);
        Rectangle extent = new Rectangle(startX, startY, endX - startX + 1, endY - startY + 1);
        this.distanceMap = ShadowMap.expandBinaryMask(new BinaryLayerIO(layer, false),
                dimension, dimension.getExtent());
    }
}
