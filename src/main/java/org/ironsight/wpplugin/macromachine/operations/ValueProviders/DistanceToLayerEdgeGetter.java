package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import org.ironsight.wpplugin.macromachine.ArrayUtils;
import org.ironsight.wpplugin.macromachine.MacroSelectionLayer;
import org.ironsight.wpplugin.macromachine.operations.ILimitedMapOperation;
import org.ironsight.wpplugin.macromachine.operations.ProviderType;
import org.ironsight.wpplugin.macromachine.operations.specialOperations.ShadowMap;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.layers.Layer;

import java.awt.*;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;

import static org.pepsoft.worldpainter.Constants.TILE_SIZE;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE_BITS;

public class DistanceToLayerEdgeGetter implements IPositionValueGetter, ILimitedMapOperation, EditableIO {
    private final boolean searchInwards;
    private final int maxDistance;
    protected String layerId;
    protected String layerName;
    protected Layer layer = null;
    private TileContainer distanceMap;

    protected DistanceToLayerEdgeGetter(boolean searchInwards, String name, String id, int maxDistance) {
        this.searchInwards = searchInwards;
        this.layerId = id;
        this.layerName = name;
        this.maxDistance = maxDistance;
    }

    public DistanceToLayerEdgeGetter(boolean searchInwards, Layer layer, int maxDistance) {
        this.searchInwards = searchInwards;
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
            return new DistanceToLayerEdgeGetter(((Integer) data[3]) == 1,
                    (String) data[0],
                    (String) data[1],
                    (Integer) data[2]);
        } catch (Exception ex) {
            return new DistanceToLayerEdgeGetter(searchInwards, MacroSelectionLayer.INSTANCE, 100);
        }
    }

    @Override
    public Object[] getSaveData() {
        return new Object[]{layerName, layerId, (Integer) maxDistance, searchInwards ? 1 : 0};
    }

    @Override
    public String valueToString(int value) {
        if (value == 0) return searchInwards ? "outside" : "inside";
        if (value == getMaxValue()) return searchInwards ? "inside" : "outside";
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
        if (distanceMap == null || !distanceMap.existsTile(x >> TILE_SIZE_BITS, y >> TILE_SIZE_BITS))
            return getMaxValue(); //outside the distance map
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
        return Arrays.equals(getSaveData(), that.getSaveData()) ;
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
    public String getToolTipText() {
        return getDescription();
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

        Rectangle extent;
        if (tileX.length == 0) {
            extent = new Rectangle(0, 0, 0, 0);
        } else {
            int startX = ArrayUtils.findMin(tileX), endX = ArrayUtils.findMax(tileX);
            int startY = ArrayUtils.findMin(tileY), endY = ArrayUtils.findMax(tileY);
            Rectangle dimExtent = dimension.getExtent();
            int expand = (int) Math.ceil(1f * getMaxValue() / TILE_SIZE);
            extent = new Rectangle(Math.max(dimExtent.x, startX - expand),
                    Math.max(dimExtent.y, startY - expand),
                    Math.min(dimExtent.width, endX - startX + 1 + 2 * expand),
                    Math.min(dimExtent.height, endY - startY + 1 + 2 * expand));
        }
        this.distanceMap = ShadowMap.expandBinaryMask(new BinaryLayerIO(layer, false),
                dimension, extent, searchInwards);
    }

    @Override
    public void releaseRightAfterRun() {
        this.distanceMap = null;
    }

    @Override
    public int[] getEditableValues() {
        return new int[]{searchInwards ? 1 : 0, maxDistance};
    }

    @Override
    public String[] getValueNames() {
        return new String[]{"search inwards", "max distance"};
    }

    @Override
    public String[] getValueTooltips() {
        return new String[]{"1 = search inwards of macroselection, 0 = search outwards of macroselection",
                "max distance to calculate. smaller values improve performance"};
    }

    @Override
    public EditableIO instantiateWithValues(int[] values) {
        return new DistanceToLayerEdgeGetter(values[0] == 1, layerName, layerId, values[1]);
    }
}
