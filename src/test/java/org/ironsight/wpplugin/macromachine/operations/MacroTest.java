package org.ironsight.wpplugin.macromachine.operations;

import org.ironsight.wpplugin.macromachine.operations.ApplyToMap.ApplyAction;
import org.ironsight.wpplugin.macromachine.operations.ApplyToMap.DummyCallback;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.ActionFilterIO;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.AnnotationSetter;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.LayerProvider;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.TerrainHeightIO;
import org.ironsight.wpplugin.macromachine.threeDRendering.TestData;
import org.junit.jupiter.api.Test;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.layers.Annotations;

import java.awt.*;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.pepsoft.util.swing.TiledImageViewer.TILE_SIZE_BITS;

class MacroTest {

    @Test
    void withReplacedUUIDs() {
        Macro initial = new Macro("Test", "descr", new UUID[]{UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID()},
                UUID.randomUUID(), new boolean[]{true, true, true, true});
        UUID replacer = UUID.randomUUID();

        Macro result = initial.withReplacedUUIDs(new int[]{1, 3}, replacer);

        assertEquals(4, result.getExecutionUUIDs().length);
        assertEquals(initial.getExecutionUUIDs()[0], result.getExecutionUUIDs()[0]);
        assertEquals(replacer, result.getExecutionUUIDs()[1]);
        assertEquals(initial.getExecutionUUIDs()[2], result.getExecutionUUIDs()[2]);
        assertEquals(replacer, result.getExecutionUUIDs()[3]);


    }

    @Test
    void applyMacroToDimension() {
        Dimension dim = TestData.createDimension(new Rectangle(-256, -256, 512+256, 512+256), 62);
        MacroContainer macroContainer = new MacroContainer("");
        MappingActionContainer actionContainer = new MappingActionContainer("");
        MappingAction setCyanAction = new MappingAction(new TerrainHeightIO(0, 255), new AnnotationSetter(), new MappingPoint[]{
                new MappingPoint(0, AnnotationSetter.ANNOTATION_ABSENT),
                new MappingPoint(62, AnnotationSetter.ANNOTATION_CYAN),
                new MappingPoint(63, AnnotationSetter.ANNOTATION_ABSENT),
        }, ActionType.SET, "set cyan to 62", "", UUID.randomUUID());
        actionContainer.addMapping(setCyanAction);

        Macro m = new Macro("paint cyan", "test", new UUID[]{setCyanAction.getUid()}, UUID.randomUUID(), new boolean[]{true});
        macroContainer.addMapping(m);

        assertEquals(AnnotationSetter.ANNOTATION_ABSENT, dim.getLayerValueAt(Annotations.INSTANCE, 17, -89));
        assertEquals(AnnotationSetter.ANNOTATION_ABSENT, dim.getLayerValueAt(Annotations.INSTANCE, 0,0));
        assertEquals(AnnotationSetter.ANNOTATION_ABSENT, dim.getLayerValueAt(Annotations.INSTANCE, -256,-256));
        assertEquals(AnnotationSetter.ANNOTATION_ABSENT, dim.getLayerValueAt(Annotations.INSTANCE, 512,512));

        ApplyAction.ApplicationContext context = new ApplyAction.ApplicationContext(
                dim,
                macroContainer,
                actionContainer,
                TestData.getMockLayerProvider(),
                TestData.getMockLayerProvider(),
                new ActionFilterIO()
        );

        Macro.applyMacroToDimension(context, m, new DummyCallback());
        assertEquals(AnnotationSetter.ANNOTATION_CYAN, dim.getLayerValueAt(Annotations.INSTANCE, 17, -89));
        assertEquals(AnnotationSetter.ANNOTATION_CYAN, dim.getLayerValueAt(Annotations.INSTANCE, 0,0));
        assertEquals(AnnotationSetter.ANNOTATION_CYAN, dim.getLayerValueAt(Annotations.INSTANCE, -256,-256));
        assertTrue(dim.isTilePresent(511>>TILE_SIZE_BITS,511>>TILE_SIZE_BITS));
        assertEquals(AnnotationSetter.ANNOTATION_CYAN, dim.getLayerValueAt(Annotations.INSTANCE, 511,511));
    }
}