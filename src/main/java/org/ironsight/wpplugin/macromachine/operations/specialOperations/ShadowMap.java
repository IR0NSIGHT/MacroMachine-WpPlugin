package org.ironsight.wpplugin.macromachine.operations.specialOperations;

import org.ironsight.wpplugin.macromachine.operations.ValueProviders.TerrainHeightIO;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.TileContainer;
import org.pepsoft.worldpainter.Dimension;

import java.awt.*;

import static org.pepsoft.worldpainter.Constants.TILE_SIZE;

public class ShadowMap {
    private TileContainer tileContainer;

    public ShadowMap(Point[] tilePositions) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;

        for (Point tilePos : tilePositions) {
            minX = Math.min(tilePos.x, minX);
            minY = Math.min(tilePos.y, minY);
            maxX = Math.max(tilePos.x, maxX);
            maxY = Math.max(tilePos.y, maxY);
        }
        tileContainer = new TileContainer((maxX - minX), (maxY - minY), TILE_SIZE * (minX),
                TILE_SIZE * (minY), 0);
    }

    public static TileContainer calculateShadowMap(Rectangle extent, TerrainHeightIO heightIO, Dimension dim) {
        int minX = TILE_SIZE * extent.x, minY = TILE_SIZE * extent.y;
        TileContainer shadowmap = new TileContainer(extent.width , extent.height, minX, minY,0);
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
            System.out.println((float)(x - shadowmap.getMinXPos())/ (shadowmap.getMaxXPos() - shadowmap.getMinXPos()));
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
