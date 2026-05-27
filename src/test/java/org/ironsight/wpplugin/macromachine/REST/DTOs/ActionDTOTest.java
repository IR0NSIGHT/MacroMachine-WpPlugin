package org.ironsight.wpplugin.macromachine.REST.DTOs;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import javax.swing.*;
import org.ironsight.wpplugin.macromachine.operations.ActionType;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.NibbleLayerSetter;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.TerrainHeightIO;
import org.junit.jupiter.api.Test;
import org.pepsoft.worldpainter.layers.PineForest;

class ActionDTOTest {
  @Test
  void fromToAction() {
    var input = new NibbleLayerSetter(PineForest.INSTANCE, false);
    ;
    var output = new TerrainHeightIO(-75, 1234);
    var dto =
        new ActionDTO(
            InputOutputDTO.fromInputGetter(input),
            InputOutputDTO.fromOutputSetter(output),
            ActionType.SET,
            "test action name",
            "test descriptions",
            UUID.randomUUID(),
            // mapping points
            new int[] {0},
            new int[] {52},
            // mapped values
            new int[] {
              52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52,
            },
            new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15});

    var action = dto.toAction();
    var dto2 = new ActionDTO(action);

    assertEquals(dto, dto2);
  }
}
