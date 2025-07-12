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
        shadowmap.addAsValues(heightIO, dim);
        for (int x = shadowmap.getMinXPos(); x < shadowmap.getMaxXPos(); x++) {
            // iterate column from south (-y) to north (+y)
            int[] column = shadowmap.getValueColumn(x);
            int[] shadowColumn = calculateShadowFor(column);
            shadowmap.setValueColumn(shadowColumn, x);
        }
        return shadowmap;
    }

    public static TileContainer expandBinaryMask(BinaryLayerIO input, Dimension dimension, Rectangle extent) {
        TileContainer container = new TileContainer(extent, 0);
        container.addAsValues(input, dimension);
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
            int[] columnYDist = replaceValues(columnXDist.clone(), 0, 0xFFFF, true); // everything not 0xFFFF set zero

            //add first and last ref points to ends of arrays
            int[] firstLast = findEdgesOfValues(columnXDist,0xFFFF);
            int firstIdx = firstLast[0];
            int lastIdx = firstLast[1];

            expandBinaryLinearColumn(columnXDist, columnYDist, incrementPerStep, firstIdx, -1);
            expandBinaryLinearColumn(columnXDist, columnYDist, incrementPerStep, lastIdx, 1);

            expandBinaryLinearColumn(columnXDist, columnYDist, incrementPerStep, 0, 1);
            expandBinaryLinearColumn(columnXDist, columnYDist, incrementPerStep, columnXDist.length - 1, -1);
            int[] distances = distanceFrom2Arrays(columnXDist, columnYDist);
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

    public static int[] expandBinaryLinearColumn(int[] horizontalDist, int[] verticalDistance, int step, int start,
                                                 int dir) {
        assert horizontalDist.length == verticalDistance.length;
        int refPointX = horizontalDist[start];
        int refPointY = verticalDistance[start];
        int distToRef = 0;
        for (int i = start; i < horizontalDist.length && i >= 0; i += dir) {
            int distNewSq =
                    refPointX < 0xFFFF && refPointY < 0xFFFF ?
                            refPointX * refPointX + (refPointY + distToRef) * (refPointY + distToRef) : Integer.MAX_VALUE;
            int distOldSq = (horizontalDist[i] < 0xFFFF && verticalDistance[i] < 0xFFFF) ?
                    horizontalDist[i] * horizontalDist[i] + verticalDistance[i] * verticalDistance[i] : Integer.MAX_VALUE;
            if (distOldSq < distNewSq) { // found a
                // point that
                // is closer than the current one we are referencing
                refPointX = horizontalDist[i];
                refPointY = verticalDistance[i];
                distToRef = 0;
            }
            if (refPointX < 0xFFFF && refPointY < 0xFFFF) {
                horizontalDist[i] = refPointX;
                verticalDistance[i] = refPointY + distToRef; //we only work on y axis
                distToRef += step;
            }
        }
        return horizontalDist;
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
