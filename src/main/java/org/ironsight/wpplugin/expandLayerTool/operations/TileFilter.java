package org.ironsight.wpplugin.expandLayerTool.operations;

import org.pepsoft.worldpainter.Terrain;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.layers.Annotations;
import org.pepsoft.worldpainter.operations.Filter;
import org.pepsoft.worldpainter.selection.SelectionBlock;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class TileFilter implements Filter, Serializable {
    private final compareType filterBySelection = compareType.IGNORE;

    private compareType filterByLayer = compareType.IGNORE;
    private final Set<String> layerIds = new HashSet<>();

    private final compareType filterByTerrain = compareType.IGNORE;
    private final Set<Terrain> terrainIds = new HashSet<>();

    private final compareType filterByHeight = compareType.IGNORE;
    private final int minHeight = 0;
    private final int maxHeight = 0;

    public static TileFilter onlyAnnotations() {
        TileFilter filter = new TileFilter();
        filter.filterByLayer = compareType.ONLY_ON;
        filter.layerIds.add(Annotations.INSTANCE.getId());
        return filter;
    }

    private static passType testHeight(Tile tile, int minHeight, int maxHeight, compareType filterByHeight) {
        if (filterByHeight == compareType.IGNORE) return passType.SOME_BLOCKS;
        else if (filterByHeight == compareType.EXCEPT_ON && tile.getMinHeight() > maxHeight ||
                tile.getMaxHeight() < minHeight) return passType.ALL_BLOCKS;
        else if (filterByHeight == compareType.ONLY_ON && tile.getMinHeight() <= minHeight &&
                tile.getMaxHeight() >= maxHeight) return passType.NO_BLOCKS;
        return passType.SOME_BLOCKS;
    }

    private static passType testTerrains(Tile tile, Set<Terrain> terrainIds, compareType filterByTerrain) {
        if (filterByTerrain == compareType.IGNORE) return passType.SOME_BLOCKS;
        else if (filterByTerrain == compareType.EXCEPT_ON &&
                tile.getAllTerrains().stream().noneMatch(terrainIds::contains)) return passType.ALL_BLOCKS;
        else if (filterByTerrain == compareType.ONLY_ON &&
                tile.getAllTerrains().stream().noneMatch(terrainIds::contains)) return passType.NO_BLOCKS;
        return passType.SOME_BLOCKS;
    }

    private static passType testLayers(Tile tile, Set<String> layerIds, compareType filterByLayer) {
        if (filterByLayer == compareType.IGNORE) return passType.SOME_BLOCKS;
        else if (filterByLayer == compareType.EXCEPT_ON &&
                tile.getLayers().stream().noneMatch(l -> layerIds.contains(l.getId()))) return passType.ALL_BLOCKS;
        else if (filterByLayer == compareType.ONLY_ON &&
                tile.getLayers().stream().noneMatch(l -> layerIds.contains(l.getId()))) return passType.NO_BLOCKS;
        return passType.SOME_BLOCKS;
    }

    private static passType testSelection(Tile tile, compareType filterBySelection) {
        switch (filterBySelection) {
            case IGNORE:
                return passType.SOME_BLOCKS;
            case ONLY_ON:
                if (!tile.hasLayer(SelectionBlock.INSTANCE) && !tile.hasLayer(SelectionBlock.INSTANCE))
                    return passType.NO_BLOCKS;
            case EXCEPT_ON:
                if (!tile.hasLayer(SelectionBlock.INSTANCE) && !tile.hasLayer(SelectionBlock.INSTANCE))
                    return passType.ALL_BLOCKS;
        }
        return passType.SOME_BLOCKS;
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

    @Override
    public float modifyStrength(int x, int y, float strength) {
        return 1;
    }

    enum passType {
        ALL_BLOCKS, NO_BLOCKS, SOME_BLOCKS
    }

    enum compareType {
        ONLY_ON, EXCEPT_ON, IGNORE
    }
}
