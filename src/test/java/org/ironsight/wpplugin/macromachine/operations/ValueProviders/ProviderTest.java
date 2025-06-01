package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import org.junit.jupiter.api.Test;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Terrain;
import org.pepsoft.worldpainter.layers.Annotations;
import org.pepsoft.worldpainter.layers.Frost;
import org.pepsoft.worldpainter.layers.PineForest;
import org.pepsoft.worldpainter.selection.SelectionBlock;
import org.pepsoft.worldpainter.selection.SelectionChunk;

import static org.junit.jupiter.api.Assertions.*;

public class ProviderTest {

    @Test
    void HeightProviderGetSetValue() {
        HeightProvider h = new HeightProvider();
        Dimension dim = TestDimension.createDimension(new TestDimension.DimensionParams());
        h.prepareForDimension(dim);

        dim.setHeightAt(14,15,77);
        assertEquals(77, h.getValueAt(dim,14,15));
        dim.setHeightAt(14,15,-5);
        assertEquals(-5, h.getValueAt(dim,14,15));

        h.setValueAt(dim, 17,18,19);
        assertEquals(19, dim.getHeightAt(17,18));
        h.setValueAt(dim, 17,18,21);
        assertEquals(21, dim.getHeightAt(17,18));
    }

    @Test
    void WaterLevelProviderGetSetValue() {
        WaterHeightAbsoluteIO h = new WaterHeightAbsoluteIO();
        Dimension dim = TestDimension.createDimension(new TestDimension.DimensionParams());
        h.prepareForDimension(dim);

        dim.setWaterLevelAt(14,15,77);
        assertEquals(77, h.getValueAt(dim,14,15));
        dim.setWaterLevelAt(14,15,-5);
        assertEquals(-5, h.getValueAt(dim,14,15));

        h.setValueAt(dim, 17,18,19);
        assertEquals(19, dim.getWaterLevelAt(17,18));
        h.setValueAt(dim, 17,18,21);
        assertEquals(21, dim.getWaterLevelAt(17,18));
    }

    @Test
    void WaterDepthProviderSetGetValue() {
        WaterDepthProvider h = new WaterDepthProvider();
        Dimension dim = TestDimension.createDimension(new TestDimension.DimensionParams());
        h.prepareForDimension(dim);

        //land below water
        dim.setWaterLevelAt(14,15,77);
        dim.setHeightAt(14,15,77-3);
        assertEquals(3, h.getValueAt(dim,14,15));

        //land above water
        dim.setWaterLevelAt(14,15,77);
        dim.setHeightAt(14,15,77+3);
        assertEquals(0, h.getValueAt(dim,14,15));

        //land equals water
        dim.setWaterLevelAt(14,15,77);
        dim.setHeightAt(14,15,77);
        assertEquals(0, h.getValueAt(dim,14,15));

        //set values
        dim.setHeightAt(17,18,77);
        dim.setWaterLevelAt(17,18,77);
        h.setValueAt(dim, 17,18,3);
        assertEquals(77 , dim.getWaterLevelAt(17,18), "water depth should not touch waterlevel");
        assertEquals(77-3, dim.getHeightAt(17,18), "water depth should change terrain height to achieve waterdepth");

    }

    @Test
    void AnnotationProviderGetSetValue() {
        AnnotationSetter h = new AnnotationSetter();
        Dimension dim = TestDimension.createDimension(new TestDimension.DimensionParams());
        h.prepareForDimension(dim);

        dim.setLayerValueAt(Annotations.INSTANCE,14,15, 5);
        assertEquals(5, h.getValueAt(dim,14,15));
        dim.setLayerValueAt(Annotations.INSTANCE,14,15, 7);
        assertEquals(7, h.getValueAt(dim,14,15));

        h.setValueAt(dim, 17,18,6);
        assertEquals(6, dim.getLayerValueAt(Annotations.INSTANCE, 17,18));
        h.setValueAt(dim, 17,18,3);
        assertEquals(3, dim.getLayerValueAt(Annotations.INSTANCE, 17,18));
    }

    @Test
    void AlwaysProviderGetValue() {
        AlwaysIO h = new AlwaysIO();
        Dimension dim = TestDimension.createDimension(new TestDimension.DimensionParams());
        h.prepareForDimension(dim);

        assertEquals(0, h.getValueAt(dim,14,15),"always IO always returns value zero, nothing else");
    }

    @Test
    void BinaryLayerProviderGetSetValue() {
        BinaryLayerIO h = new BinaryLayerIO(Frost.INSTANCE);
        Dimension dim = TestDimension.createDimension(new TestDimension.DimensionParams());
        h.prepareForDimension(dim);

        dim.setBitLayerValueAt(Frost.INSTANCE,17,18,false);
        assertFalse(dim.getBitLayerValueAt(Frost.INSTANCE, 17, 18));
        assertEquals(0, h.getValueAt(dim,17,18),"no frost");
        dim.setBitLayerValueAt(Frost.INSTANCE,17,18,true);
        assertEquals(1, h.getValueAt(dim,17,18),"has frost");

        assertFalse(dim.getBitLayerValueAt(Frost.INSTANCE, 21, 22));
        h.setValueAt(dim,21,22,1);
        assertTrue(dim.getBitLayerValueAt(Frost.INSTANCE, 21, 22));
        h.setValueAt(dim,21,22,0);
        assertFalse(dim.getBitLayerValueAt(Frost.INSTANCE, 21, 22));

    }

    @Test
    void BitLayerSpraypaintProviderGetSetValue() {
        IPositionValueSetter h = new BitLayerBinarySpraypaintApplicator(Frost.INSTANCE);
        Dimension dim = TestDimension.createDimension(new TestDimension.DimensionParams());
        h.prepareForDimension(dim);

        int spraypaintChancePercent = 77;

        int hits = 0;
        for (int y = 0; y < 100; y ++) {
            for (int x = 0; x < 100; x++) {
                assertFalse(dim.getBitLayerValueAt(Frost.INSTANCE, x, y));
                h.setValueAt(dim, x, y, spraypaintChancePercent);
                if (dim.getBitLayerValueAt(Frost.INSTANCE, x, y))
                    hits++;
            }
        }
        assertTrue(100*spraypaintChancePercent*0.99 <= hits, "hits less often than 99% requested chance");
        assertTrue(100*spraypaintChancePercent*1.01 >= hits, "hits more often than 101% requested chance");

        //check that random decision is stable for coordinates
        h.setValueAt(dim, 17,18, spraypaintChancePercent);
        boolean value = dim.getLayerValueAt(Frost.INSTANCE,17,18) == 0;
        for (int i = 0; i < 100; i++) {
            h.setValueAt(dim, 17,18, spraypaintChancePercent);
            assertEquals(value,dim.getLayerValueAt(Frost.INSTANCE,17,18) == 0, "spraypainting again caused change of " +
                    "value at i="+i);
        }

    }

    @Test
    void BlockFacingProviderGetValue() {
        IPositionValueGetter h = new BlockFacingDirectionIO();
        Dimension dim = TestDimension.createDimension(new TestDimension.DimensionParams());
        h.prepareForDimension(dim);

        {        //create world with slope 45° all facing -x: west
            for (int y = 0; y < 100; y++) {
                for (int x = 0; x < 100; x++) {
                    int height = y;
                    dim.setHeightAt(x, y, height);
                }
            }

            int compassDirAngle = h.getValueAt(dim, 17, 18);
            assertEquals(0, compassDirAngle);
        }
        {        //create world with slope 45° all facing -x: west
            for (int y = 0; y < 100; y++) {
                for (int x = 0; x < 100; x++) {
                    int height = -y;
                    dim.setHeightAt(x, y, height);
                }
            }

            int compassDirAngle = h.getValueAt(dim, 17, 18);
            assertEquals(180, compassDirAngle);
        }

        {        //create world with slope 45° all facing -x: west
            int hits = 0;
            for (int y = 0; y < 100; y++) {
                for (int x = 0; x < 100; x++) {
                    int height = x;
                    dim.setHeightAt(x, y, height);
                }
            }

            int compassDirAngle = h.getValueAt(dim, 17, 18);
            assertEquals(270, compassDirAngle);
        }

        {        //create world with slope 45° all facing -x: west
            int hits = 0;
            for (int y = 0; y < 100; y++) {
                for (int x = 0; x < 100; x++) {
                    int height = -x;
                    dim.setHeightAt(x, y, height);
                }
            }

            int compassDirAngle = h.getValueAt(dim, 17, 18);
            assertEquals(90, compassDirAngle);
        }
    }

    @Test
    void intermediateValueTest() {
        IntermediateValueIO io = new IntermediateValueIO();
        Dimension dim = TestDimension.createDimension(new TestDimension.DimensionParams());
        io.prepareForDimension(dim);

        assertEquals(0,io.getValueAt(dim,18,19),"initial value is zero");
        io.setValueAt(dim,18,19,27);
        assertEquals(27, io.getValueAt(dim,18,19),"io remembers last set value");
        io.setValueAt(dim,18,19,-43);
        assertEquals(-43, io.getValueAt(dim,18,19),"io can modifiy previously set values");
        assertEquals(0,io.getValueAt(dim,105,107),"other position has initial value, not previous value");
        assertEquals(27, io.getValueAt(dim,18,19),"value was erased when querying other position");
    }

    @Test
    void NibbleLayerTest() {
        NibbleLayerSetter io = new NibbleLayerSetter(PineForest.INSTANCE);
        Dimension dim = TestDimension.createDimension(new TestDimension.DimensionParams());
        io.prepareForDimension(dim);

        dim.setLayerValueAt(PineForest.INSTANCE,18,19,5);
        assertEquals(5,io.getValueAt(dim,18,19),"read from dim");
        dim.setLayerValueAt(PineForest.INSTANCE,18,19,7);
        assertEquals(7,io.getValueAt(dim,18,19),"read from dim after modification");

        io.setValueAt(dim,18,19,3);
        assertEquals(3,dim.getLayerValueAt(PineForest.INSTANCE,18,19));
        io.setValueAt(dim,18,19,1);
        assertEquals(1,dim.getLayerValueAt(PineForest.INSTANCE,18,19));
    }

    @Test
    void SelectionTest() {
        SelectionIO io = new SelectionIO();
        Dimension dim = TestDimension.createDimension(new TestDimension.DimensionParams());
        io.prepareForDimension(dim);

        //select block
        dim.setBitLayerValueAt(SelectionBlock.INSTANCE,18,19,true);
        assertEquals(1,io.getValueAt(dim,18,19),"read from dim");
        dim.setBitLayerValueAt(SelectionBlock.INSTANCE,18,19,false);
        assertEquals(0,io.getValueAt(dim,18,19),"read from dim");

        dim.setBitLayerValueAt(SelectionChunk.INSTANCE,18,19,true);
        assertEquals(1,io.getValueAt(dim,18,19),"read from dim");
        dim.setBitLayerValueAt(SelectionChunk.INSTANCE,18,19,false);
        assertEquals(0,io.getValueAt(dim,18,19),"read from dim");

        io.setValueAt(dim,18,19,1);
        assertEquals(true, dim.getBitLayerValueAt(SelectionBlock.INSTANCE,18,19),"io wrties to block select layer");

        dim.setBitLayerValueAt(SelectionChunk.INSTANCE,18,19,true);
        io.setValueAt(dim,18,19,0);
        assertEquals(false, dim.getBitLayerValueAt(SelectionBlock.INSTANCE,18,19),"io sets block and chunk select to " +
                "zero");
        assertEquals(false, dim.getBitLayerValueAt(SelectionChunk.INSTANCE,18,19),"io sets block and chunk select to " +
                "zero");
    }

    @Test
    void SlopeTest() {
        SlopeProvider io = new SlopeProvider();
        Dimension dim = TestDimension.createDimension(new TestDimension.DimensionParams());
        io.prepareForDimension(dim);

        assertEquals(0, io.getValueAt(dim, 17, 18));

        {        //create world with slope 45° in y dir
            for (int y = 0; y < 100; y++) {
                for (int x = 0; x < 100; x++) {
                    int height = y;
                    dim.setHeightAt(x, y, height);
                }
            }
            assertEquals(45, io.getValueAt(dim, 17, 18));
        }
        {        //create world with slope 2up, 1 over
            for (int y = 0; y < 100; y++) {
                for (int x = 0; x < 100; x++) {
                    int height = y*2;
                    dim.setHeightAt(x, y, height);
                }
            }
            assertEquals(63, io.getValueAt(dim, 17, 18));
        }

        {        //create world with slope 45° in x dir
            for (int y = 0; y < 100; y++) {
                for (int x = 0; x < 100; x++) {
                    int height = x;
                    dim.setHeightAt(x, y, height);
                }
            }
            assertEquals(45, io.getValueAt(dim, 17, 18));
        }
        {        //create world with slope 45° in x dir
            for (int y = 0; y < 100; y++) {
                for (int x = 0; x < 100; x++) {
                    int height = -x;
                    dim.setHeightAt(x, y, height);
                }
            }
            assertEquals(45, io.getValueAt(dim, 17, 18));
        }
        {        //create world with slope 45° in x dir
            for (int y = 0; y < 100; y++) {
                for (int x = 0; x < 100; x++) {
                    int height = -y;
                    dim.setHeightAt(x, y, height);
                }
            }
            assertEquals(45, io.getValueAt(dim, 17, 18));
        }
    }

    @Test
    void TestTerrainProvider() {
        TerrainProvider io = new TerrainProvider();
        Dimension dim = TestDimension.createDimension(new TestDimension.DimensionParams());
        io.prepareForDimension(dim);

        dim.setTerrainAt(17,18, Terrain.BEACHES);
        assertEquals(Terrain.BEACHES.ordinal(), io.getValueAt(dim,17,18));
        dim.setTerrainAt(17,18, Terrain.STONE);
        assertEquals(Terrain.STONE.ordinal(), io.getValueAt(dim,17,18));

        io.setValueAt(dim, 19,20, Terrain.DIORITE.ordinal());
        assertEquals(Terrain.DIORITE, dim.getTerrainAt(19,20));
        io.setValueAt(dim,19,20, Terrain.OBSIDIAN.ordinal());
        assertEquals(Terrain.OBSIDIAN, dim.getTerrainAt(19,20));
    }

}
