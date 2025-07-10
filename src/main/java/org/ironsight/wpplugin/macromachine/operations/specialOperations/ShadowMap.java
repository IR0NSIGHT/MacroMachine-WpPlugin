package org.ironsight.wpplugin.macromachine.operations.specialOperations;

import org.ironsight.wpplugin.macromachine.operations.ValueProviders.TerrainHeightIO;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.TileContainer;
import org.pepsoft.worldpainter.Dimension;

import java.awt.*;

import static org.pepsoft.worldpainter.Constants.TILE_SIZE;

public class ShadowMap {
    public static TileContainer calculateShadowMap(Rectangle extent, TerrainHeightIO heightIO, Dimension dim) {
        TileContainer shadowmap = new TileContainer(extent,0);
        for (int x = shadowmap.getMinXPos(); x < shadowmap.getMaxXPos(); x++) {
            // iterate column from south (-y) to north (+y)
            int[] column = new int[shadowmap.getMaxYPos() - shadowmap.getMinYPos()];
            {
                int i = 0;
                for (int y = shadowmap.getMaxYPos() - 1; y >= shadowmap.getMinYPos(); y--) {
                    column[i++] = heightIO.getValueAt(dim,x,y);
                }
            }
            int[] shadowColumn = calculateShadowFor(column);
            {
                int i = 0;
                for (int y = shadowmap.getMaxYPos() - 1; y >= shadowmap.getMinYPos(); y--) {
                    shadowmap.setValueAt(x,y,shadowColumn[i++]);
                }
            }
        }
        return shadowmap;
    }

    public static int[] calculateShadowFor(int[] heightmapColumn) {
        int[] shadowMap = new int[heightmapColumn.length];
        int shadowHeight = 0;
        int idx = 0;
        for (int terrainHeight: heightmapColumn) {
            shadowHeight = Math.max(shadowHeight -1, terrainHeight);
            if (shadowHeight > terrainHeight)
                shadowMap[idx] = shadowHeight - terrainHeight;
            idx++;
        }
        return shadowMap;
    }
}
