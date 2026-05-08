package org.ironsight.wpplugin.macromachine.REST.DTOs;

import org.ironsight.wpplugin.macromachine.operations.ActionType;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.AlwaysIO;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.TerrainProvider;
import org.junit.jupiter.api.Test;

import javax.swing.*;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ActionDTOTest
{
    @Test
    void fromToAction() {
        var input = new AlwaysIO();
        var output = new TerrainProvider();
        var dto = new ActionDTO(InputOutputDTO.fromInputGetter(input), InputOutputDTO.fromOutputSetter(output),
                ActionType.SET, "test action name", "test descriptions", UUID.randomUUID(), new int[]{0}, new int[]{52},
                new int[]{52}, new int[]{0});

        var action = dto.toAction();
        var dto2 = new ActionDTO(action);

        assertEquals(dto, dto2);
    }
}
