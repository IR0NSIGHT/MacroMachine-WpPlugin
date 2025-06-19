package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import org.ironsight.wpplugin.macromachine.operations.MappingAction;
import org.ironsight.wpplugin.macromachine.operations.MappingActionContainer;
import org.ironsight.wpplugin.macromachine.operations.MappingPoint;
import org.ironsight.wpplugin.macromachine.operations.TestData;
import org.junit.jupiter.api.Test;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.layers.Annotations;

import java.awt.*;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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


        MappingActionContainer container = MappingActionContainer.getInstance();
        IntermediateValueIO intermediateValueIO = new IntermediateValueIO(0,100,"");

        UUID set = container.addMapping().getUid();
        container.updateMapping(container.queryById(set)
                .withInput(new TerrainHeightIO(-64,319))
                .withOutput(intermediateValueIO)
                .withNewPoints(new MappingPoint[]{new MappingPoint(EVEN_HEIGHT, 4), new MappingPoint(UNEVEN_HEIGHT, 7)})
                .withName("set intermediate"), f -> {
        });
        assertEquals(4, container.queryById(set).map(EVEN_HEIGHT));
        assertEquals(7, container.queryById(set).map(UNEVEN_HEIGHT));

        UUID get = container.addMapping().getUid();
        container.updateMapping(container.queryById(get)
                .withInput(intermediateValueIO)
                .withOutput(new AnnotationSetter())
                .withNewPoints(new MappingPoint[]{new MappingPoint(4, EVEN_OUTPUT), new MappingPoint(7, UNEVEN_OUTPUT)})
                .withName("get intermediate"), f -> {
        });
        assertEquals(EVEN_OUTPUT, container.queryById(get).map(4));
        assertEquals(UNEVEN_OUTPUT, container.queryById(get).map(7));

        ActionFilterIO.instance.prepareForDimension(dim);
        for (MappingAction lm : container.queryAll()) {
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
    }

    @Test
    void notEditable() {
        // currently, the plugin can not handle intermediatevalue havimg multiple instances with different values
        // therefore, we can not allow the user to edit the min, max values of this IO type.
        IntermediateValueIO io = new IntermediateValueIO(12,13,"uwu");
        assertFalse(io instanceof EditableIO);
    }
    @Test
    void setValueAt() {
    }
}