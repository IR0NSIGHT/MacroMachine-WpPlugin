package org.ironsight.wpplugin.macromachine.operations.specialOperations;

import org.ironsight.wpplugin.macromachine.operations.ValueProviders.TerrainHeightIO;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.TileContainer;
import org.junit.jupiter.api.Test;
import org.pepsoft.worldpainter.Dimension;

import java.awt.*;
import java.util.Arrays;

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
        container.getTileAt(0,0).printToStd();
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
        assertEquals(dist(p2x,p2y,11,29), container.getValueAt(11, 29), "p2 is the closest point to this position");

        // test a point that should have been marked as 31 radius, but wasnt
        assertEquals(31, dist(p2x, p2y, 9, 37), "");
        assertTrue(dist(p1x, p1y, 9, 37) > dist(p2x, p2y, 9, 37), "");
        assertEquals(31, container.getValueAt(9, 37), "its closer to p2, so should take this distance to p1");

    }

    @Test
    public void regTestWrongDistancesUsed() {
        int[] rowY0 = new int[50];

        {   // set point 0,0    calculate the row for it
            Arrays.fill(rowY0, 0xFFFF);
            rowY0[0] = 0;
            int[] rowStep1 = ShadowMap.expandBinaryLinear(rowY0.clone(), 1, 0, 1);
            int[] rowStep2 = ShadowMap.expandBinaryLinear(rowStep1.clone(), 1, rowY0.length - 1, -1);
            assertArrayEquals(new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23,
                            24,
                            25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49},
                    rowStep2);
            rowY0 = rowStep2;
        }

        int[] rowY23 = new int[50];
        {   // set point 37,23  calculate the row for it
            Arrays.fill(rowY23, 0xFFFF);
            rowY23[37] = 0;
            int[] rowStep1 = ShadowMap.expandBinaryLinear(rowY23.clone(), 1, 0, 1);
            int[] rowStep2 = ShadowMap.expandBinaryLinear(rowStep1.clone(), 1, rowY23.length - 1, -1);
            assertArrayEquals(new int[]{37, 36, 35, 34, 33, 32, 31, 30, 29, 28, 27, 26, 25, 24, 23, 22, 21, 20, 19, 18, 17, 16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12},
                    rowStep2);
            rowY23 = rowStep2;
        }

        {  // given row at y=0 and y=23, calculate the column at x= 9 where the point 9,37 lies
            int[] columnX9horidD = new int[40];
            Arrays.fill(columnX9horidD, 0xFFFF);
            columnX9horidD[0] = rowY0[9]; //from point 1  at  0 0
            columnX9horidD[23] = rowY23[9]; // from point 2  at 37,23
            int[] columnX9vertD = ShadowMap.replaceValues(columnX9horidD.clone(),0,0xFFFF,true);

            columnX9horidD[39] = rowY23[9];
            columnX9vertD[39] = 16;

         /*           assertArrayEquals(new int[]{9, 65535, 65535, 65535, 65535, 65535, 65535, 65535, 65535, 65535, 65535, 65535, 65535, 65535, 65535, 65535, 65535, 65535, 65535, 65535, 65535, 65535, 65535,
                    28, 65535, 65535, 65535, 65535, 65535, 65535, 65535, 65535, 65535, 65535, 65535, 65535, 65535, 65535, 65535, 65535},
                    columnX9horidD);
            assertArrayEquals(new int[]{0, 65535, 65535, 65535, 65535, 65535, 65535, 65535, 65535, 65535, 65535, 65535, 65535, 65535, 65535, 65535, 65535, 65535, 65535, 65535, 65535, 65535, 65535,
                    0, 65535, 65535, 65535, 65535, 65535, 65535, 65535, 65535, 65535, 65535, 65535, 65535, 65535, 65535, 65535, 65535}, columnX9vertD);


            //verify distances are still correct:
            assertEquals(9*9+0*0,columnX9horidD[0]*columnX9horidD[0]+columnX9vertD[0]*columnX9vertD[0], "this entry in the array represents the distance to p1 at x=9,y=0");
            assertEquals((37-9)*(37-9)+0*0,columnX9horidD[23]*columnX9horidD[23]+columnX9vertD[23]*columnX9vertD[23], "this entry in the array represents the distance to p2 at x=9,y=0");
*/
            //dist(0,0,columnX9horidD[0],columnX9vertD[0])
            ShadowMap.expandBinaryLinearColumn(columnX9horidD,columnX9vertD,1,0,1);
            ShadowMap.expandBinaryLinearColumn(columnX9horidD,columnX9vertD,1,columnX9horidD.length-1,-1);
            int[] distances = ShadowMap.distanceFrom2Arrays(columnX9horidD,columnX9vertD);
            System.out.println(Arrays.toString(columnX9horidD));
            System.out.println(Arrays.toString(columnX9vertD));

        }
    }

    private int dist(int p1x, int p1y, int p2x, int p2y) {
        int dx = p1x - p2x;
        int dy = p1y - p2y;
        return (int) Math.round(Math.sqrt(dx * dx + dy * dy));
    }

    @Test
    void expandBinaryLinearColumn() {
        {   //takes existing value, keeps it and writes distance map into the first16bits
            int[] horizDist = new int[]{0xFFFF, 0xFFFF, 7, 0xFFFF, 0xFFFF, 0xFFFF, 3, 0xFFFF, 0xFFFF, 0xFFFF};
            int[] vertiDist = new int[]{0xFFFF, 0xFFFF, 0, 0xFFFF, 0xFFFF, 0xFFFF, 0, 0xFFFF, 0xFFFF, 0xFFFF};

            int[] expHorz = new int[]{0xFFFF, 0xFFFF, 7, 7, 7, 7, 3, 3, 3, 3};
            int[] expVert = new int[]{0xFFFF, 0xFFFF, 0, 1, 2, 3, 0, 1, 2, 3};
            ShadowMap.expandBinaryLinearColumn(horizDist, vertiDist, 1, 0, 1);
            assertArrayEquals(expHorz, horizDist);
            assertArrayEquals(expVert, vertiDist);
        }
        {   //takes existing value, keeps it and writes distance map into the first16bits
            int[] horizDist = new int[]{7, 0xFFFF, 0xFFFF, 0xFFFF, 3, 0xFFFF, 0xFFFF, 0xFFFF};
            int[] vertiDist = new int[]{0, 0xFFFF, 0xFFFF, 0xFFFF, 0, 0xFFFF, 0xFFFF, 0xFFFF};

            int[] expHorz = new int[]{7, 7, 7, 7, 3, 3, 3, 3};
            int[] expVert = new int[]{0, 1, 2, 3, 0, 1, 2, 3};
            ShadowMap.expandBinaryLinearColumn(horizDist, vertiDist, 1, 0, 1);
            assertArrayEquals(expHorz, horizDist);
            assertArrayEquals(expVert, vertiDist);
        }
        {   //walk backwards on data after a first pass
            int[] horizDist = new int[]{7, 7, 7, 7, 7, 7, 7, 7, 3, 3, 3, 3}; // walks right to left <---|
            int[] vertiDist = new int[]{0, 0, 0, 0, 0, 1, 2, 3, 0, 1, 2, 3};

            int[] expHorz = new int[]{7, 7, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3};
            int[] expVert = new int[]{0, 0, 6, 5, 4, 3, 2, 1, 0, 1, 2, 3};
            ShadowMap.expandBinaryLinearColumn(horizDist, vertiDist, 1, horizDist.length - 1, -1);
            assertArrayEquals(expHorz, horizDist);
            assertArrayEquals(expVert, vertiDist);
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
        int[] arr = new int[]{0,0,0, 3, 3, 3, 3,0,0,0, 3, 3, 3,0,0,0,0,0};
        int[] edges = ShadowMap.findEdgesOfValues(arr,0);
        assertEquals(edges[0],3);
        assertEquals(edges[1],12);
    }
}