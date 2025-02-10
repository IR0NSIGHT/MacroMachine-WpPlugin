package org.ironsight.wpplugin.expandLayerTool.operations.ValueProviders;

import org.ironsight.wpplugin.expandLayerTool.operations.*;
import org.junit.jupiter.api.Test;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.layers.Annotations;

import java.awt.*;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE;

class IntermediateValueIOTest {
    public static int mod(int a, int b) {
        int result = a % b;
        return (result < 0) ? result + Math.abs(b) : result;
    }

    @Test
    void getValueAt() {
        int EVEN_OUTPUT = 2, UNEVEN_OUTPUT = 3;
        int EVEN_HEIGHT = 20, UNEVEN_HEIGHT = 21;

        Rectangle rect = new Rectangle(-TILE_SIZE, -TILE_SIZE, TILE_SIZE, TILE_SIZE);
        Dimension dim = TestData.createDimension(rect, 50);

        for (int x = rect.x; x < rect.x + rect.width; x++) {
            for (int y = rect.y; y < rect.y + rect.height; y++) {
                dim.setHeightAt(x, y, mod(x, 2) + EVEN_HEIGHT);  //EVEN_HEIGHT or UNEVEN_HEIGHT
            }
        }
        for (int x = rect.x; x < rect.x + rect.width; x++) {
            for (int y = rect.y; y < rect.y + rect.height; y++) {
                assertEquals(mod(x, 2) + EVEN_HEIGHT, dim.getHeightAt(x, y));
            }
        }
        assertEquals(UNEVEN_HEIGHT, dim.getHeightAt(-125, -125));


        LayerMappingContainer container = LayerMappingContainer.INSTANCE;
        IntermediateValueIO intermediateValueIO = new IntermediateValueIO();
        UUID set =
                container.updateMapping(container.addMapping()
                        .withInput(new HeightProvider())
                        .withOutput(intermediateValueIO)
                        .withNewPoints(new MappingPoint[]{new MappingPoint(EVEN_HEIGHT, 4),
                                new MappingPoint(UNEVEN_HEIGHT, 7)})
                        .withName("set intermediate"));
        assertEquals(4, container.queryById(set).map(EVEN_HEIGHT));
        assertEquals(7, container.queryById(set).map(UNEVEN_HEIGHT));

        UUID get =
                container.updateMapping(container.addMapping()
                        .withInput(intermediateValueIO)
                        .withOutput(new AnnotationSetter())
                        .withNewPoints(new MappingPoint[]{new MappingPoint(4, EVEN_OUTPUT),
                                new MappingPoint(7, UNEVEN_OUTPUT)})
                        .withName("get intermediate"));
        assertEquals(EVEN_OUTPUT, container.queryById(get).map(4));
        assertEquals(UNEVEN_OUTPUT, container.queryById(get).map(7));


        for (LayerMapping lm : container.queryAll()) {
            lm.input.prepareForDimension(dim);
            lm.output.prepareForDimension(dim);
        }

        // first apply on uneven
        assertEquals(0, dim.getLayerValueAt(Annotations.INSTANCE, -125, -125));
        assertEquals(1, mod(-125, 2));
        assertEquals(UNEVEN_HEIGHT, dim.getHeightAt(-125, -125));

        container.queryById(set).applyToPoint(dim, -125, -125);
        container.queryById(get).applyToPoint(dim, -125, -125);
        assertEquals(UNEVEN_OUTPUT, dim.getLayerValueAt(Annotations.INSTANCE, -125, -125));

        //second apply
        assertEquals(0, dim.getLayerValueAt(Annotations.INSTANCE, -124, -125));
        assertEquals(EVEN_HEIGHT, dim.getHeightAt(-124, -125));

        container.queryById(set).applyToPoint(dim, -124, -125);
        container.queryById(get).applyToPoint(dim, -124, -125);
        assertEquals(EVEN_OUTPUT, dim.getLayerValueAt(Annotations.INSTANCE, -124, -125));

        MappingMacro mappingMacro = new MappingMacro("test macro", "test intermediate value", new UUID[]{set, get},
                UUID.randomUUID());
        mappingMacro.apply(dim, container);
        // mod(x,2) == 0 -> intermediate 5 -> annotation UNEVEN_OUTPUT
        // mod(x,2) == 1 -> intermediate 7 -> annotation EVEN_OUTPUT

        for (int x = rect.x; x < rect.x + rect.width; x++) {
            for (int y = rect.y; y < rect.y + rect.height; y++) {
                if (mod(x, 2) == 0) assertEquals(EVEN_OUTPUT, dim.getLayerValueAt(Annotations.INSTANCE, x, y));
                else assertEquals(UNEVEN_OUTPUT, dim.getLayerValueAt(Annotations.INSTANCE, x, y));

            }
        }
    }

    @Test
    void setValueAt() {
    }
}