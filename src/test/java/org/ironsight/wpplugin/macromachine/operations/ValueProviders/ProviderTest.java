package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import org.junit.jupiter.api.Test;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.layers.Annotations;
import org.pepsoft.worldpainter.layers.Frost;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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
    }
}
