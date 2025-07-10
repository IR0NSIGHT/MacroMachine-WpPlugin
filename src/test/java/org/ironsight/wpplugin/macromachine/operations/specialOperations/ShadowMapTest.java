package org.ironsight.wpplugin.macromachine.operations.specialOperations;

import org.ironsight.wpplugin.macromachine.operations.ValueProviders.TerrainHeightIO;
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
            assertArrayEquals(    new int[]{3, 4, 5, 6, 0, 1, 4, 0, 0, 0}, shadowMap);
        }
    }

    @Test
    void testCalculateShadowMap() {
        // 12 x 12 km map
        Dimension dim = createDimension(new Rectangle(0,0,TILE_SIZE * 100, TILE_SIZE * 100),62);
        ShadowMap.calculateShadowMap(dim.getExtent(), new TerrainHeightIO(-128,1000), dim);
    }
}