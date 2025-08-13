package org.ironsight.wpplugin.macromachine.operations.specialOperations;

import org.ironsight.wpplugin.macromachine.operations.ValueProviders.BinaryLayerIO;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.TerrainHeightIO;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.TileContainer;
import org.pepsoft.worldpainter.Dimension;

import java.awt.*;
import java.util.Arrays;

public class ShadowMap {
    public static TileContainer calculateShadowMap(Rectangle extent, TerrainHeightIO heightIO, Dimension dim) {
        TileContainer shadowmap = new TileContainer(extent, 0);
        shadowmap.addAsValues(heightIO, dim, false);
        for (int x = shadowmap.getMinXPos(); x < shadowmap.getMaxXPos(); x++) {
            // iterate column from south (-y) to north (+y)
            int[] column = shadowmap.getValueColumn(x);
            int[] shadowColumn = calculateShadowFor(column);
            shadowmap.setValueColumn(shadowColumn, x);
        }
        return shadowmap;
    }

    public static TileContainer expandBinaryMask(BinaryLayerIO input, Dimension dimension, Rectangle extent,
                                                 boolean invert) {
        TileContainer container = new TileContainer(extent, 0);
        container.addAsValues(input, dimension, invert);
        return expandBinaryMask(container, 1);
    }

    public static TileContainer expandBinaryMask(TileContainer container,
                                                 int incrementPerStep) {
        for (int y = container.getMinYPos(); y < container.getMaxYPos(); y++) {
            //row by row
            int[] row = container.getValueRow(y);
            row = replaceValues(row, 0xFFFF, 0, false); // 1 -> 1, 0 -> 0xFFFF
            row = replaceValues(row, 0, 0xFFFF, true); // 0xFFFF -> 0xFFFF, else -> 0
            row = expandBinaryLinear(row, incrementPerStep, 0, 1);
            row = expandBinaryLinear(row, incrementPerStep, row.length - 1, -1);
            container.setValueRow(y, row);
        }

        for (int x = container.getMinXPos(); x < container.getMaxXPos(); x++) {
            //row by row
            int[] columnXDist = container.getValueColumn(x); // consists of set values 0 .. x and unset values 0xFFFF
            int[] distances = expandBinaryLinearColumn(columnXDist);
            container.setValueColumn(distances, x);
        }
        return container;
    }

    /**
     * replaces all originals with replacement
     *
     * @param row
     * @param replacement
     * @param invert      set if value[i] != orginal
     * @return
     */
    public static int[] replaceValues(int[] row, int replacement, int original, boolean invert) {
        for (int i = 0; i < row.length; i++) {
            if (!invert && row[i] == original || invert && row[i] != original)
                row[i] = replacement;
        }
        return row;
    }

    public static int[] distanceFrom2Arrays(int[] xDistance, int[] yDistance) {
        int[] dist = new int[xDistance.length];
        for (int i = 0; i < dist.length; i++) {
            dist[i] = (int)Math.round(Math.sqrt(xDistance[i]*xDistance[i]+yDistance[i]*yDistance[i]));
        }
        return dist;
    }

    public static int[] expandBinaryLinear(int[] row, int decrement, int start, int dir) {
        int current = Integer.MAX_VALUE - decrement;
        for (int i = start; i < row.length && i >= 0; i += dir) {
            current = Math.min(row[i], current + decrement);
            if (row[i] != 0 || current != 0) {
                row[i] = Math.max(0, current);
            } else {
                current = 0;
            }
        }
        return row;
    }

    public static int[] expandBinaryLinearColumn(int[] horizontalDist) {
        // a list of all reference points, arr[i] = index in horizontalDist
        int[] refPointIndices = new int[horizontalDist.length];
        {
            int refPointIdx = 0;
            for (int i = 0; i < horizontalDist.length && i >= 0; i ++) {
                if (horizontalDist[i] != 0xFFFF) {
                    //point is set
                    refPointIndices[refPointIdx++] = i;
                }
            }
            refPointIndices = Arrays.copyOf(refPointIndices,refPointIdx);
        }

        // iterate all positions in array, find closest point using refPointIndices and set distance to this point
        int[] dist = new int[horizontalDist.length];
        for (int i = 0; i < dist.length; i ++) {
            int distSq = Integer.MAX_VALUE;
            for (int j = 0; j < refPointIndices.length; j++) {
                int distVert = i-refPointIndices[j];
                int distHoriz = horizontalDist[refPointIndices[j]];
                int distSqRefPoint = distVert*distVert + distHoriz*distHoriz;
                if (distSq > distSqRefPoint)
                    distSq = distSqRefPoint;
            }
            dist[i] = (int)Math.round(Math.sqrt(distSq));
        }
        return dist;
    }

    /**
     * find the first and last index to NOT be marker
     * @param arr
     * @return
     */
    public static int[] findEdgesOfValues(int[] arr, int marker) {
        int first = -1;
        int last = -1;
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] != marker && first == -1) {
                first = i;
                break;
            }
        }
        for (int i = arr.length -1; i >= 0; i--) {
            if (arr[i] != marker && last == -1) {
                last = i;
                break;
            }
        }
        return new int[]{first,last};
    }

    public static int[] expandBinaryMapped(int[] row, int[] map, int start, int dir) {
        int current = 0;
        for (int i = start; i < row.length && i >= 0; i += dir) {
            current = Math.max(row[i], current - 1);
            if (row[i] != 0 || current != 0) {
                row[i] = Math.max(0, map[current]);
            } else {
                current = 0;
            }
        }
        return row;
    }

    public static int[] dualValueToDistance(int[] row) {
        for (int i = 0; i < row.length; i++) {
            int x = row[i] & 0xFFFF;
            int y = row[i] >> 16;
            int dist = x * x + y * y;
            row[i] = dist;
        }
        return row;
    }

    public static int[] calculateShadowFor(int[] heightmapColumn) {
        int[] shadowMap = new int[heightmapColumn.length];
        int shadowHeight = 0;
        for (int i = heightmapColumn.length - 1; i >= 0; i--) {
            int terrainHeight = heightmapColumn[i];
            shadowHeight = Math.max(shadowHeight - 1, terrainHeight);
            if (shadowHeight > terrainHeight)
                shadowMap[i] = shadowHeight - terrainHeight;
        }
        return shadowMap;
    }
}
