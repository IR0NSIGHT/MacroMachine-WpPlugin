package org.ironsight.wpplugin.expandLayerTool.operations.ValueProviders;

import org.ironsight.wpplugin.expandLayerTool.operations.ProviderType;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.layers.Annotations;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.layers.LayerManager;
import org.pepsoft.worldpainter.selection.SelectionBlock;
import org.pepsoft.worldpainter.selection.SelectionChunk;

import java.awt.*;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Objects;

public class DistanceToLayerEdgeGetter implements IPositionValueGetter {
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
        if (layerId.equals(SelectionBlock.INSTANCE.getId())) layer = SelectionBlock.INSTANCE;
        else if (layerId.equals(Annotations.INSTANCE.getId())) layer = Annotations.INSTANCE;
        else {
            Collection<Layer> allLayers = new LinkedList<>();
            allLayers.addAll(dim.getCustomLayers());
            allLayers.addAll(LayerManager.getInstance().getLayers());
            allLayers.stream()
                    .filter(f -> f.getId().equals(layerId))
                    .findFirst()
                    .map(l -> this.layer = l)
                    .orElseThrow(IllegalAccessError::new);
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
        return new DistanceToLayerEdgeGetter((String) data[0], (String) data[1]);
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

    @Override
    public int getValueAt(Dimension dim, int x, int y) {
        float dist = dim.getDistanceToEdge(layer, x, y, getMaxValue());
        if (layerId.equals(SelectionBlock.INSTANCE.getId())) {
            float distChunkSel =  dim.getDistanceToEdge(SelectionChunk.INSTANCE, x, y, getMaxValue());
            if (distChunkSel != 0) {
                dist = Math.min(dist,distChunkSel);
            } else if (dist == 0)
                dist = distChunkSel;
        }
        if (x==82 && y == 67)
            System.out.println("DEBUG UWU");
        if (dist == 0)  //position is not part of layer
            return getMaxValue();
        if (dist != 0) System.out.println(x + "," + y + " dist to" + layer.getName() + " =" + dist);
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
}
