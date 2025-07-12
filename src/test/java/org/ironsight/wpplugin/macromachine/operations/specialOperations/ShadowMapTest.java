package org.ironsight.wpplugin.macromachine.operations.specialOperations;

import org.ironsight.wpplugin.macromachine.operations.ValueProviders.TerrainHeightIO;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.TileContainer;
import org.junit.jupiter.api.Test;
import org.pepsoft.worldpainter.Dimension;

import java.awt.*;

import static org.ironsight.wpplugin.macromachine.operations.TestData.createDimension;
import static org.junit.jupiter.api.Assertions.*;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE;

class ShadowMapTest {

    @Test
    void calculateShadowMap() {
    }

    @Test
    void calculateShadowFor() {
        {
            int[] terrainHeight = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 5, 0};
            int[] shadowMap = ShadowMap.calculateShadowFor(terrainHeight);
            assertArrayEquals(new int[]{0, 0, 0, 0, 1, 2, 3, 4, 0, 0}, shadowMap);
        }

        {
            int[] terrainHeight = new int[]{0, 0, 0, 0, 7, 2, 0, 5, 5, 0};
            int[] shadowMap = ShadowMap.calculateShadowFor(terrainHeight);
            assertArrayEquals(new int[]{3, 4, 5, 6, 0, 1, 4, 0, 0, 0}, shadowMap);
        }
    }

    @Test
    void testCalculateShadowMap() {
        // 12 x 12 km map
        Dimension dim = createDimension(new Rectangle(0, 0, TILE_SIZE * 100, TILE_SIZE * 100), 62);
        ShadowMap.calculateShadowMap(dim.getExtent(), new TerrainHeightIO(-128, 1000), dim);
    }

    @Test
    void binaryMaskToValue() {
        {
            int[] row = new int[]{0, 7, 0, 0, 0, 1, 0, 2, 0, 0, 0, 0};
            int[] res = ShadowMap.replaceValues(row.clone(), 17, 0, false);
            int[] expected = new int[]{17, 7, 17,17,17,1,17,2,17,17,17,17};
            assertArrayEquals(expected, res);
        }
        {
            int[] row = new int[]{0, 7, 0, 0, 0, 1, 0, 2, 0, 0, 0, 0};
            int[] res = ShadowMap.replaceValues(row.clone(), 17, 0, true);
            int[] expected = new int[]{0,17,0,0,0,17,0,17,0,0,0,0};
            assertArrayEquals(expected, res);
        }
    }

    @Test
    void expandBinaryLinear() {
        { //left to right one peak
            int[] row = new int[]{0, 0, 0, 0, 0, 100, 100, 100, 100, 100};
            int[] res = ShadowMap.expandBinaryLinear(row.clone(), 1, 0, 1);
            int[] expected = new int[]{0, 0, 0, 0, 0, 1, 2, 3, 4, 5};
            assertArrayEquals(expected, res);
        }
        { //left to right a couple peaks
            int[] row = new int[]{0, 0, 0, 0, 0, 100, 0, 100, 20, 30, 0, 0};
            int[] res = ShadowMap.expandBinaryLinear(row.clone(), 1, 0, 1);
            int[] exp = new int[]{0, 0, 0, 0, 0, 1, 0, 1, 2, 3, 0, 0};
            assertArrayEquals(exp, res);
        }
        { //right ot left one peak
            int[] row = new int[]{100,100,100,100, 0, 100,100,100,100,100};
            int[] res = ShadowMap.expandBinaryLinear(row.clone(), 3, row.length - 1, -1);
            int[] expected = new int[]{12,9,6,3, 0, 100,100,100,100,100};
            assertArrayEquals(expected, res);
        }
        { //right to left after left to right
            int[] row = ShadowMap.expandBinaryLinear(new int[]{100, 100, 100, 0, 100 , 0 ,0 ,100 ,100,100,100}, 1, 0,
                    1);
            int[] res = ShadowMap.expandBinaryLinear(row.clone(), 1, row.length - 1, -1);
            int[] expected = new int[]{3,2,1,0,1,0,0,1,2,3,4};
            assertArrayEquals(expected, res);
        }
    }

    @Test
    void expandBinaryMapped() {
        {
            int[] row = new int[]{0, 0, 0, 0, 0, 10, 0, 0, 0, 0, 0, 0};
            int[] map = new int[]{0, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20};
            int[] res = ShadowMap.expandBinaryMapped(row.clone(), map, 0, 1);
            int[] expected = new int[]{0, 0, 0, 0, 0, 20, 18, 16, 14, 12, 10, 8};
            assertArrayEquals(expected, res);
        }
        {
            int[] row = new int[]{0, 0, 0, 0, 0, 10, 0, 10, 0, 0, 0, 0};
            int[] map = new int[]{0, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20};
            int[] res = ShadowMap.expandBinaryMapped(row.clone(), map, 0, 1);
            int[] expected = new int[]{0, 0, 0, 0, 0, 20, 18, 20, 18, 16, 14, 12};
            assertArrayEquals(expected, res);
        }
    }

    @Test
    void expandBinaryMask() {
        TileContainer container = new TileContainer(new Rectangle(0, 0, 1, 1), 0);
        container.setValueAt(50, 60, 10);
        ShadowMap.expandBinaryMask(container, 10);
        container.getTileAt(0, 0).printToStd();
        assertEquals(10, container.getValueAt(50, 60));
        assertEquals(9, container.getValueAt(51, 60));

    }

    @Test
    void expandBinaryLinearColumn() {
        {   //takes existing value, keeps it and writes distance map into the first16bits
            int[] horizDist = new int[]{0xFFFF,0xFFFF,7,0xFFFF,0xFFFF,0xFFFF,3,0xFFFF,0xFFFF,0xFFFF};
            int[] vertiDist = new int[]{0xFFFF,0xFFFF,0,0xFFFF,0xFFFF,0xFFFF,0,0xFFFF,0xFFFF,0xFFFF};

            int[] expHorz = new int[]{0xFFFF,0xFFFF,7,7,7,7,3,3,3,3};
            int[] expVert = new int[]{0xFFFF,0xFFFF,0,1,2,3,0,1,2,3};
            ShadowMap.expandBinaryLinearColumn(horizDist, vertiDist,1, 0, 1);
            assertArrayEquals(expHorz, horizDist);
            assertArrayEquals(expVert, vertiDist);
        }
        {   //takes existing value, keeps it and writes distance map into the first16bits
            int[] horizDist = new int[]{7,0xFFFF,0xFFFF,0xFFFF,3,0xFFFF,0xFFFF,0xFFFF};
            int[] vertiDist = new int[]{0,0xFFFF,0xFFFF,0xFFFF,0,0xFFFF,0xFFFF,0xFFFF};

            int[] expHorz = new int[]{7,7,7,7,3,3,3,3};
            int[] expVert = new int[]{0,1,2,3,0,1,2,3};
            ShadowMap.expandBinaryLinearColumn(horizDist, vertiDist,1, 0, 1);
            assertArrayEquals(expHorz, horizDist);
            assertArrayEquals(expVert, vertiDist);
        }
        {   //walk backwards on data after a first pass
            int[] horizDist = new int[]{7,7,7,7,7,7,7,7,3,3,3,3}; // walks right to left <---|
            int[] vertiDist = new int[]{0,0,0,0,0,1,2,3,0,1,2,3};

            int[] expHorz   = new int[]{7,7,3,3,3,3,3,3,3,3,3,3};
            int[] expVert   = new int[]{0,0,6,5,4,3,2,1,0,1,2,3};
            ShadowMap.expandBinaryLinearColumn(horizDist, vertiDist,1, horizDist.length-1, -1);
            assertArrayEquals(expHorz, horizDist);
            assertArrayEquals(expVert, vertiDist);
        }

    }
}