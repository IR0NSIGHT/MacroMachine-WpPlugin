package org.ironsight.wpplugin.macromachine.operations.specialOperations;

import org.ironsight.wpplugin.macromachine.MacroSelectionLayer;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.BinaryLayerIO;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IPositionTileValueGetter;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.TerrainHeightIO;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.TileContainer;
import org.junit.jupiter.api.Test;
import org.pepsoft.worldpainter.Dimension;

import java.awt.*;

import static org.ironsight.wpplugin.macromachine.threeDRendering.TestData.createDimension;
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
        int size = 3;
        Dimension dim = createDimension(new Rectangle(TILE_SIZE *-3 * size, TILE_SIZE *-4 * size, TILE_SIZE * 10 * size, TILE_SIZE * 10 * size), 62);
        dim.setHeightAt(-17, 29, 90);
        var shadowMap = ShadowMap.calculateShadowMap(dim.getExtent(), new TerrainHeightIO(-128, 1000), dim);
        assertEquals(0, shadowMap.getValueAt(-17, 29));
        assertEquals(90-62-1, shadowMap.getValueAt(-17, 28)); // north = shadow dir = y- axis in WP
    }

    @Test
    void binaryMaskToValue() {
        {
            int[] row = new int[]{0, 7, 0, 0, 0, 1, 0, 2, 0, 0, 0, 0};
            int[] res = ShadowMap.replaceValues(row.clone(), 17, 0, false);
            int[] expected = new int[]{17, 7, 17, 17, 17, 1, 17, 2, 17, 17, 17, 17};
            assertArrayEquals(expected, res);
        }
        {
            int[] row = new int[]{0, 7, 0, 0, 0, 1, 0, 2, 0, 0, 0, 0};
            int[] res = ShadowMap.replaceValues(row.clone(), 17, 0, true);
            int[] expected = new int[]{0, 17, 0, 0, 0, 17, 0, 17, 0, 0, 0, 0};
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
            int[] row = new int[]{100, 100, 100, 100, 0, 100, 100, 100, 100, 100};
            int[] res = ShadowMap.expandBinaryLinear(row.clone(), 3, row.length - 1, -1);
            int[] expected = new int[]{12, 9, 6, 3, 0, 100, 100, 100, 100, 100};
            assertArrayEquals(expected, res);
        }
        { //right to left after left to right
            int[] row = ShadowMap.expandBinaryLinear(new int[]{100, 100, 100, 0, 100, 0, 0, 100, 100, 100, 100}, 1, 0,
                    1);
            int[] res = ShadowMap.expandBinaryLinear(row.clone(), 1, row.length - 1, -1);
            int[] expected = new int[]{3, 2, 1, 0, 1, 0, 0, 1, 2, 3, 4};
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
        int p1x = 0, p1y = 0, p2x = 37, p2y = 23;
        container.setValueAt(p1x, p1y, 1); //binary
        container.setValueAt(p2x, p2y, 1); //binary

        ShadowMap.expandBinaryMask(container, 1);
        assertEquals(0, container.getValueAt(p1x, p1y), "distance to mask must be zero for set points");
        assertEquals(0, container.getValueAt(p2x, p2y), "distance to mask must be zero for set points");
        //test points moving away from the common center

        assertEquals(10, container.getValueAt(p2x + 10, p2y), "p2x,p2y is the closest point");
        assertEquals(10, container.getValueAt(p2x, p2y + 10), "p2x,p2y is the closest point");
        assertEquals(14, container.getValueAt(p2x - 10, p2y + 10), "p2x,p2y is the closest point");

        //test points inbetween both maskes points
        assertEquals(10, container.getValueAt(p1x + 10, p1y), "p1x,p1x is the closest point");
        assertEquals(10, container.getValueAt(p1x, p1y + 10), "p1x,p1x is the closest point");
        assertEquals(14, container.getValueAt(p1x + 10, p1x + 10), "p1x,p1x is the closest point");

        // test random points i pulled from the WP map when encountering unexpected behaviour
        assertEquals(dist(p2x, p2y, 11, 29), container.getValueAt(11, 29), "p2 is the closest point to this position");

        // test a point that should have been marked as 31 radius, but wasnt
        assertEquals(31, dist(p2x, p2y, 9, 37), "");
        assertTrue(dist(p1x, p1y, 9, 37) > dist(p2x, p2y, 9, 37), "");
        assertEquals(31, container.getValueAt(9, 37), "its closer to p2, so should take this distance to p1");

    }

    private int dist(int p1x, int p1y, int p2x, int p2y) {
        int dx = p1x - p2x;
        int dy = p1y - p2y;
        return (int) Math.round(Math.sqrt(dx * dx + dy * dy));
    }

    private int distInt(int x, int y) {
        return (int) Math.round(Math.sqrt(x * x + y * y));
    }

    @Test
    void expandBinaryLinearColumn() {
        {   //takes existing value, keeps it and writes distance map into the first16bits
            int[] horizDist = new int[]{0xFFFF, 0xFFFF, 7, 0xFFFF, 0xFFFF, 0xFFFF, 3, 0xFFFF, 0xFFFF, 0xFFFF};

            int[] expHorz = new int[]{distInt(2, 7), distInt(5, 3), distInt(4, 3), distInt(3, 3), distInt(2, 3), distInt(1, 3), 3, distInt(1, 3), distInt(2, 3), distInt(3, 3)};
            int[] distances = ShadowMap.expandBinaryLinearColumn(horizDist);
            assertArrayEquals(expHorz, distances);
        }
    }

    @Test
    void distanceFrom2Arrays() {
        int[] expHorz = new int[]{7, 7, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3};
        int[] expVert = new int[]{0, 0, 6, 5, 4, 3, 2, 1, 0, 1, 2, 3};
        int[] dist = new int[]{7, 7, 7, 6, 5, 4, 4, 3, 3, 3, 4, 4};
        int[] dist2 = ShadowMap.distanceFrom2Arrays(expHorz, expVert);
        assertArrayEquals(dist, dist2);
    }

    @Test
    void findEdgesOfValues() {
        int[] arr = new int[]{0, 0, 0, 3, 3, 3, 3, 0, 0, 0, 3, 3, 3, 0, 0, 0, 0, 0};
        int[] edges = ShadowMap.findEdgesOfValues(arr, 0);
        assertEquals(edges[0], 3);
        assertEquals(edges[1], 12);
    }

    @Test
    void testExpandBinaryMask() {
        int sizeTiles = 20;
        Dimension dim = createDimension(new Rectangle(TILE_SIZE *-1 * sizeTiles, TILE_SIZE *-1 * sizeTiles, TILE_SIZE * sizeTiles, TILE_SIZE * sizeTiles), 62);
        BinaryLayerIO input = new BinaryLayerIO(MacroSelectionLayer.INSTANCE, false);
        input.setValueAt(dim,-17,29,1);
        var expandedMap = ShadowMap.expandBinaryMask(input,dim,dim.getExtent(),false);

        assertEquals(0, expandedMap.getValueAt(-17, 29));
        assertEquals(1, expandedMap.getValueAt(-17, 28));
        assertEquals(2, expandedMap.getValueAt(-17, 27));

    }
}