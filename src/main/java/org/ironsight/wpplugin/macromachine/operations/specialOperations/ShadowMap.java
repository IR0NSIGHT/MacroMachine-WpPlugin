package org.ironsight.wpplugin.macromachine.operations.specialOperations;

import org.ironsight.wpplugin.macromachine.operations.ValueProviders.TerrainHeightIO;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.TileContainer;
import org.pepsoft.worldpainter.Dimension;

import java.awt.*;

import static org.pepsoft.worldpainter.Constants.TILE_SIZE;

public class ShadowMap {
    public static TileContainer calculateShadowMap(Rectangle extent, TerrainHeightIO heightIO, Dimension dim) {
        TileContainer shadowmap = new TileContainer(extent,0);
        shadowmap.addAsValues(heightIO, dim);
        for (int x = shadowmap.getMinXPos(); x < shadowmap.getMaxXPos(); x++) {
            // iterate column from south (-y) to north (+y)
            int[] column = shadowmap.getValueColumn(x);
            int[] shadowColumn = calculateShadowFor(column);
            shadowmap.setValueColumn(shadowColumn, x);
        }
        return shadowmap;
    }

    public static TileContainer expandMask(TileContainer container) {
        for (int y = container.getMaxYPos() - 1; y >= container.getMinYPos(); y--) {
            //left to right, row by row
            int[] row = container.getValueRow(y);

        }

        return container;
    }

    public static int[] calculateShadowFor(int[] heightmapColumn) {
        int[] shadowMap = new int[heightmapColumn.length];
        int shadowHeight = 0;
        for (int i = heightmapColumn.length - 1; i >= 0; i--) {
            int terrainHeight = heightmapColumn[i];
            shadowHeight = Math.max(shadowHeight -1, terrainHeight);
            if (shadowHeight > terrainHeight)
                shadowMap[i] = shadowHeight - terrainHeight;
        }
        return shadowMap;
    }
}
