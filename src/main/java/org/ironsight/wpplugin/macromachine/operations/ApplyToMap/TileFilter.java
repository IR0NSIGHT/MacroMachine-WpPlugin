package org.ironsight.wpplugin.macromachine.operations.ApplyToMap;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Terrain;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.layers.Annotations;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.layers.LayerManager;
import org.pepsoft.worldpainter.selection.SelectionBlock;
import org.pepsoft.worldpainter.selection.SelectionChunk;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * a filter to check if a tile should be further touched or if we can abort early.
 */
public class TileFilter implements Serializable {
    private FilterType filterBySelection = FilterType.IGNORE;
    private Set<String> layerIds = new HashSet<>();
    private FilterType filterByTerrain = FilterType.IGNORE;
    private Set<Terrain> terrainIds = new HashSet<>();
    private FilterType filterByHeight = FilterType.IGNORE;
    private int minHeight = 0;
    private int maxHeight = 0;
    private transient Dimension dimension;
    private transient List<Layer> layers;
    // layer Only-On is "one of these must be present". except-on is "none of these must be present"
    private FilterType filterByLayer = FilterType.IGNORE;

    public TileFilter() {
    }

    private static passType testHeight(Tile tile, int minHeight, int maxHeight, FilterType filterByHeight) {
        if (filterByHeight == FilterType.IGNORE) return passType.SOME_BLOCKS;
        else if (filterByHeight == FilterType.EXCEPT_ON && tile.getMinHeight() > maxHeight ||
                tile.getMaxHeight() < minHeight) return passType.ALL_BLOCKS;
        else if (filterByHeight == FilterType.ONLY_ON && tile.getMinHeight() <= minHeight &&
                tile.getMaxHeight() >= maxHeight) return passType.NO_BLOCKS;
        return passType.SOME_BLOCKS;
    }

    private static passType testTerrains(Tile tile, Set<Terrain> terrainIds, FilterType filterByTerrain) {
        if (filterByTerrain == FilterType.IGNORE) return passType.SOME_BLOCKS;
        else if (filterByTerrain == FilterType.EXCEPT_ON &&
                tile.getAllTerrains().stream().noneMatch(terrainIds::contains)) return passType.ALL_BLOCKS;
        else if (filterByTerrain == FilterType.ONLY_ON &&
                tile.getAllTerrains().stream().noneMatch(terrainIds::contains)) return passType.NO_BLOCKS;
        return passType.SOME_BLOCKS;
    }

    private static passType testLayers(Tile tile, Set<String> layerIds, FilterType filterByLayer) {
        if (filterByLayer == FilterType.IGNORE) return passType.SOME_BLOCKS;
        else if (filterByLayer == FilterType.EXCEPT_ON &&
                tile.getLayers().stream().noneMatch(l -> layerIds.contains(l.getId()))) return passType.ALL_BLOCKS;
        else if (filterByLayer == FilterType.ONLY_ON &&
                tile.getLayers().stream().noneMatch(l -> layerIds.contains(l.getId()))) return passType.NO_BLOCKS;
        return passType.SOME_BLOCKS;
    }

    private static passType testSelection(Tile tile, FilterType filterBySelection) {
        switch (filterBySelection) {
            case IGNORE:
                return passType.SOME_BLOCKS;
            case ONLY_ON:
                if (!tile.hasLayer(SelectionBlock.INSTANCE) && !tile.hasLayer(SelectionChunk.INSTANCE))
                    return passType.NO_BLOCKS;
            case EXCEPT_ON:
                if (!tile.hasLayer(SelectionBlock.INSTANCE) && !tile.hasLayer(SelectionChunk.INSTANCE))
                    return passType.ALL_BLOCKS;
        }
        return passType.SOME_BLOCKS;
    }

    public TileFilter withSelection(FilterType filterBySelection) {
        this.filterBySelection = filterBySelection;
        return this;
    }

    public TileFilter withTerrain(FilterType filterByTerrain, Terrain... terrainIds) {
        this.filterByTerrain = filterByTerrain;
        this.terrainIds = Arrays.stream(terrainIds).collect(HashSet::new, HashSet::add, HashSet::addAll);
        return this;
    }

    public TileFilter withHeight(FilterType filterByHeight, int minHeight, int maxHeight) {
        this.filterByHeight = filterByHeight;
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        return this;
    }

    public TileFilter withLayer(FilterType filterByLayer, String... layerIds) {
        this.filterByLayer = filterByLayer;
        this.layerIds = Arrays.stream(layerIds).collect(HashSet::new, HashSet::add, HashSet::addAll);
        return this;
    }

    public void setDimension(Dimension dimension) {
        this.dimension = dimension;
        List<Layer> allLayers = new ArrayList(LayerManager.getInstance().getLayers());
        allLayers.add(Annotations.INSTANCE);

        this.layers = allLayers.stream().filter(layer -> layerIds.contains(layer.getId())).collect(Collectors.toList());
    }

    public passType testTile(Tile tile) {
        passType selection = testSelection(tile, filterBySelection);
        if (selection == passType.NO_BLOCKS) return passType.NO_BLOCKS;
        passType layers = testLayers(tile, layerIds, filterByLayer);
        if (layers == passType.NO_BLOCKS) return passType.NO_BLOCKS;
        passType terrains = testTerrains(tile, terrainIds, filterByTerrain);
        if (terrains == passType.NO_BLOCKS) return passType.NO_BLOCKS;
        passType height = testHeight(tile, minHeight, maxHeight, filterByHeight);
        if (height == passType.NO_BLOCKS) return passType.NO_BLOCKS;

        if (selection == passType.ALL_BLOCKS && terrains == passType.ALL_BLOCKS && height == passType.ALL_BLOCKS &&
                layers == passType.ALL_BLOCKS) return passType.ALL_BLOCKS;

        return passType.SOME_BLOCKS;
    }

    public boolean passSelection(int x, int y) {
        if (filterBySelection == FilterType.IGNORE) return true;
        else if (filterBySelection == FilterType.ONLY_ON)
            return dimension.getBitLayerValueAt(SelectionBlock.INSTANCE, x, y) ||
                    dimension.getBitLayerValueAt(SelectionChunk.INSTANCE, x, y);
        else if (filterBySelection == FilterType.EXCEPT_ON)
            return !dimension.getBitLayerValueAt(SelectionBlock.INSTANCE, x, y) &&
                    !dimension.getBitLayerValueAt(SelectionChunk.INSTANCE, x, y);
        return false;
    }

    public boolean passLayer(int x, int y) {
        if (filterByLayer == FilterType.IGNORE) return true;
        if (filterByLayer == FilterType.ONLY_ON) {
            for (Layer layer : layers) {
                if (layer.dataSize.equals(Layer.DataSize.BIT) && dimension.getBitLayerValueAt(layer, x, y)) return true;
                else if (dimension.getLayerValueAt(layer, x, y) != 0) return true;
            }
            return false;
        }

        if (filterByLayer == FilterType.EXCEPT_ON) for (Layer layer : layers) {
            if (dimension.getLayerValueAt(layer, x, y) != 0) return false;
        }

        return true;
    }

    public boolean passTerrain(int x, int y) {
        if (filterByTerrain == FilterType.IGNORE) return true;
        Terrain present = dimension.getTerrainAt(x, y);
        if (filterByTerrain == FilterType.ONLY_ON && terrainIds.contains(present)) return true;
        return filterByTerrain != FilterType.EXCEPT_ON || !terrainIds.contains(present);
    }

    public boolean passHeight(int x, int y) {
        if (filterByHeight == FilterType.IGNORE) return true;
        float height = dimension.getHeightAt(x, y);
        if (filterByHeight == FilterType.EXCEPT_ON) return (height > maxHeight || height < minHeight);
        if (filterByHeight == FilterType.ONLY_ON) return (height <= maxHeight && height >= minHeight);
        return true;
    }

    public boolean pass(int x, int y) {
        return passHeight(x, y) && passSelection(x, y) && passLayer(x, y) && passTerrain(x, y);
    }

    public FilterType getFilterBySelection() {
        return filterBySelection;
    }

    public Set<String> getLayerIds() {
        return layerIds;
    }

    public FilterType getFilterByTerrain() {
        return filterByTerrain;
    }

    public Set<Terrain> getTerrainIds() {
        return terrainIds;
    }

    public FilterType getFilterByHeight() {
        return filterByHeight;
    }

    public int getMinHeight() {
        return minHeight;
    }

    public int getMaxHeight() {
        return maxHeight;
    }

    public FilterType getFilterByLayer() {
        return filterByLayer;
    }

    @Override
    public String toString() {
        return "TileFilter{" + "filterBySelection=" + filterBySelection + ", layerIds=" + layerIds +
                ", filterByTerrain=" + filterByTerrain + ", terrainIds=" + terrainIds + ", filterByHeight=" +
                filterByHeight + ", minHeight=" + minHeight + ", maxHeight=" + maxHeight + ", filterByLayer=" +
                filterByLayer + '}';
    }

    enum passType {
        ALL_BLOCKS, NO_BLOCKS, SOME_BLOCKS
    }

    public enum FilterType {
        ONLY_ON, EXCEPT_ON, IGNORE
    }
}
