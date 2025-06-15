package org.ironsight.wpplugin.macromachine.operations;

import org.ironsight.wpplugin.macromachine.operations.FileIO.ActionJsonWrapper;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.AnnotationSetter;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.TerrainHeightIO;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ActionJsonWrapperTest {
    @Test
    public void fromMapping() {
        for (ActionType actionType : ActionType.values()) {
            MappingAction original = new MappingAction(new TerrainHeightIO(-64,319), new AnnotationSetter(),
                    new MappingPoint[]{new MappingPoint(0, 0), new MappingPoint(62, 3), new MappingPoint(100, 7),
                            new MappingPoint(255, 0)}, actionType, "My Test Mapping", "my test description",
                    UUID.randomUUID());
            ActionJsonWrapper wrapper = new ActionJsonWrapper(original);
            MappingAction restored = MappingAction.fromJsonWrapper(wrapper);

            assertEquals(original, restored);
        }


    }
}