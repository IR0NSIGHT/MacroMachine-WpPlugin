package org.ironsight.wpplugin.expandLayerTool.operations;

import org.ironsight.wpplugin.expandLayerTool.operations.ValueProviders.AnnotationSetter;
import org.ironsight.wpplugin.expandLayerTool.operations.ValueProviders.HeightProvider;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ActionJsonWrapperTest {
    @Test
    public void fromMapping() {
        for (ActionType actionType : ActionType.values()) {
            LayerMapping original = new LayerMapping(new HeightProvider(), new AnnotationSetter(),
                    new MappingPoint[]{new MappingPoint(0, 0), new MappingPoint(62, 3), new MappingPoint(100, 7),
                            new MappingPoint(255, 0)}, actionType, "My Test Mapping", "my test description",
                    UUID.randomUUID());
            ActionJsonWrapper wrapper = new ActionJsonWrapper(original);
            LayerMapping restored = LayerMapping.fromJsonWrapper(wrapper);

            assertEquals(original, restored);
        }


    }
}