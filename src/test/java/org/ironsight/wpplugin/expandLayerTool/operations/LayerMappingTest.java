package org.ironsight.wpplugin.expandLayerTool.operations;

import org.junit.jupiter.api.Test;
import org.pepsoft.worldpainter.layers.Annotations;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LayerMappingTest {

    @Test
    void map() {
        /*
        {   // LINEAR WITH 3 POINTS
            LayerMapping linear = new LayerMapping(null, null,
                    new LayerMapping.MappingPoint[]{new LayerMapping.MappingPoint(10, 100),
                            new LayerMapping.MappingPoint(50 + 10, 100 + 500), new LayerMapping.MappingPoint(110,
                            1100)});

            assertEquals(100, linear.map(10));
            assertEquals(600, linear.map(60));
            assertEquals(1100, linear.map(110));

            assertEquals(100 + 500, linear.map(50 + 10));
            assertEquals(100 + 300, linear.map(30 + 10));
        }

        {   // STATIC ONE POINT
            LayerMapping mapper = new LayerMapping(null, null,
                    new LayerMapping.MappingPoint[]{new LayerMapping.MappingPoint(57, 89)});

            for (int i = -1000; i < 1000; i += 7) {
                assertEquals(89, mapper.map(i));
            }
        } */

        {   // 2 POINT LINEAR AT FIRST THAN PLATEAU
            LayerMapping mapper = new LayerMapping(new LayerMapping.HeightProvider(),
                    new LayerMapping.NibbleLayerSetter(Annotations.INSTANCE),
                    new LayerMapping.MappingPoint[]{new LayerMapping.MappingPoint(50, 0),
                            new LayerMapping.MappingPoint(150, 10),}, LayerMapping.ActionType.SET, "", "",-1);

            for (int i = 0; i < 50; i++) { //plateau before first point
                assertEquals(0, mapper.map(i), i + "->" + mapper.map(i));
            }
            for (int i = 50; i < 150; i++) { //linear between points
                assertEquals(Math.round(((float) i - 50) / 10), mapper.map(i), i + "->" + mapper.map(i));
            }
            for (int i = 150; i < 300; i++) { //plateau after second point
                assertEquals(10, mapper.map(i));
            }
        }
    }

    @Test
    void applyToPoint() {
    }

    @Test
    void reverseMap() {
    }
}