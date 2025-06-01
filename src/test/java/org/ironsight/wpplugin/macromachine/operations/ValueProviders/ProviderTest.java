package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import org.junit.jupiter.api.Test;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.layers.Annotations;
import org.pepsoft.worldpainter.layers.Frost;

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
    void AnnotationProviderSetGetValue() {
        AnnotationSetter h = new AnnotationSetter();
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
}
