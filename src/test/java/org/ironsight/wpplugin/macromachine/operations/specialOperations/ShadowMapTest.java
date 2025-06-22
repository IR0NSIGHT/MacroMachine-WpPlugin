package org.ironsight.wpplugin.macromachine.operations.specialOperations;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ShadowMapTest {

    @Test
    void calculateShadowMap() {
    }

    @Test
    void calculateShadowFor() {
        {
            int[] terrainHeight = new int[]{0, 0, 0, 5, 0, 0, 0, 0, 0, 0};
            int[] shadowMap = ShadowMap.calculateShadowFor(terrainHeight);
            assertArrayEquals(new int[]{0, 0, 0, 0, 4, 3, 2, 1, 0, 0}, shadowMap);
        }

        {
            int[] terrainHeight = new int[]{0, 2, 0, 5, 5, 0, 0, 0, 0, 0};
            int[] shadowMap = ShadowMap.calculateShadowFor(terrainHeight);
            assertArrayEquals(    new int[]{0, 0, 1, 0, 0, 4, 3, 2, 1, 0}, shadowMap);
        }
    }
}