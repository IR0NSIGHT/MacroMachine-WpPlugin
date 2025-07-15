package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import org.ironsight.wpplugin.macromachine.ArrayUtils;
import org.ironsight.wpplugin.macromachine.MacroSelectionLayer;
import org.ironsight.wpplugin.macromachine.operations.ILimitedMapOperation;
import org.ironsight.wpplugin.macromachine.operations.ProviderType;
import org.ironsight.wpplugin.macromachine.operations.TileFilter;
import org.ironsight.wpplugin.macromachine.operations.specialOperations.ShadowMap;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.layers.Layer;

import java.awt.*;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;

import static org.pepsoft.worldpainter.Constants.TILE_SIZE;

public class DistanceToLayerEdgeGetter implements IPositionValueGetter, ILayerGetter, ILimitedMapOperation, EditableIO {
    private final int maxDistance;
    protected String layerId;
    protected String layerName;
    protected Layer layer = null;
    private TileContainer distanceMap;

    protected DistanceToLayerEdgeGetter(String name, String id, int maxDistance) {
        this.layerId = id;
        this.layerName = name;
        this.maxDistance = maxDistance;
    }

    public DistanceToLayerEdgeGetter(Layer layer, int maxDistance) {
        assert layer.dataSize == Layer.DataSize.BIT;
        this.layerName = layer.getName();
        this.layerId = layer.getId();
        this.maxDistance = maxDistance;
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
        return maxDistance;
    }

    @Override
    public int getMinValue() {
        return 0;
    }

    @Override
    public IMappingValue instantiateFrom(Object[] data) {
        try {
            return new DistanceToLayerEdgeGetter((String) data[0], (String) data[1], (Integer)data[2]);
        } catch (Exception ex) {
            return new DistanceToLayerEdgeGetter(MacroSelectionLayer.INSTANCE, 100);

        }
    }

    @Override
    public Object[] getSaveData() {
        return new Object[]{layerName, layerId, (Integer)maxDistance};
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
        return Math.min(getMaxValue(), distanceMap.getValueAt(x, y));
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
        return Objects.hash(getProviderType(), layerId, maxDistance);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DistanceToLayerEdgeGetter that = (DistanceToLayerEdgeGetter) o;
        return Objects.equals(layerId, that.layerId) && this.maxDistance == that.maxDistance;
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
        int[] tileXFiltered = new int[tileX.length], tileYFiltered = new int[tileY.length];
        Iterator<? extends Tile> t = dimension.getTiles().iterator();
        int tileArrIdx = 0;
        for (int i = 0; i < tileX.length; i++) {
            Tile tile = dimension.getTile(tileX[i], tileY[i]);
            if (!tile.hasLayer(layer))
                continue;
            tileXFiltered[tileArrIdx] = tile.getX();
            tileYFiltered[tileArrIdx] = tile.getY();
            tileArrIdx++;
        }
        tileX = Arrays.copyOf(tileXFiltered, tileArrIdx);
        tileY = Arrays.copyOf(tileYFiltered, tileArrIdx);

        int startX = ArrayUtils.findMin(tileX), endX = ArrayUtils.findMax(tileX);
        int startY = ArrayUtils.findMin(tileY), endY = ArrayUtils.findMax(tileY);
        Rectangle dimExtent = dimension.getExtent();
        int expand = (int) Math.ceil(1f * getMaxValue() / TILE_SIZE);
        Rectangle extent = new Rectangle(Math.max(dimExtent.x, startX - expand),
                Math.max(dimExtent.y, startY - expand),
                Math.min(dimExtent.width, endX - startX + 1 + 2 * expand),
                Math.min(dimExtent.height, endY - startY + 1 + 2 * expand));
        this.distanceMap = ShadowMap.expandBinaryMask(new BinaryLayerIO(layer, false),
                dimension, extent);
    }

    @Override
    public int[] getEditableValues() {
        return new int[]{maxDistance};
    }

    @Override
    public String[] getValueNames() {
        return new String[]{"max distance"};
    }

    @Override
    public String[] getValueTooltips() {
        return new String[]{"max distance to calculate. smaller values improve performance"};
    }

    @Override
    public EditableIO instantiateWithValues(int[] values) {
        return new DistanceToLayerEdgeGetter(layerName, layerId, values[0]);
    }
}
