package org.ironsight.wpplugin.macromachine.operations;

import static org.ironsight.wpplugin.macromachine.REST.DTOs.ExecutionStatus.IDLE;
import static org.junit.jupiter.api.Assertions.*;
import static org.pepsoft.util.swing.TiledImageViewer.TILE_SIZE;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.Consumer;
import org.ironsight.wpplugin.macromachine.REST.DTOs.ActionDTO;
import org.ironsight.wpplugin.macromachine.REST.DTOs.MacroDTO;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.AnnotationSetter;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.SelectionIO;
import org.ironsight.wpplugin.macromachine.threeDRendering.TestData;
import org.junit.jupiter.api.Test;
import org.pepsoft.worldpainter.Dimension;

class MacroConcurrentApplicatorTest
{
    public static String loadJson(String resourceName) throws IOException {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName)) {

            if (is == null) {
                throw new IllegalArgumentException("Resource not found: " + resourceName);
            }

            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    public void testRunDeserializedMacro() throws IOException, InterruptedException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();

        var actionDTOs = mapper.readValue(loadJson("GlobalOpsActions.json"), ActionDTO[].class);
        var actions = Arrays.stream(actionDTOs).map(ActionDTO::toAction).toList();

        var macroDTO = mapper.readValue(loadJson("GlobalOpsMacro.json"), MacroDTO.class);
        var macro = macroDTO.toMacro();

        var macroContainer = new MacroContainer("");
        var actionContainer = new MappingActionContainer("");
        macroContainer.addMapping(macro);
        actions.forEach(actionContainer::addMapping);

        assertEquals(1, macroContainer.queryAll().size());
        assertEquals(38, actionContainer.queryAll().size());

        Dimension dim = TestData
                .createDimension(new Rectangle(-2 * TILE_SIZE, -2 * TILE_SIZE, 3 * TILE_SIZE, 3 * TILE_SIZE), 0);
        int[] counter = new int[]{0};
        Consumer<UUID> callbackAfterRun = (uuid1) -> {
            counter[0]++;
        };
        var applicator = new MacroConcurrentApplicator(macroContainer, actionContainer, () -> dim, callbackAfterRun);
        assertEquals(IDLE, applicator.getCurrentState().status());
        applicator.queueMacro(macro.getUid());
        applicator.queueMacro(macro.getUid());
        applicator.queueMacro(macro.getUid());
        assertEquals(3, applicator.getQueue().size());

        applicator.start();
        for (int i = 0; i < 1000; i++) {
            Thread.sleep(1000);
            if (counter[0] == 3)
                break;
        }
        assertEquals(0, applicator.getQueue().size());
        assertEquals(3, counter[0]);

        applicator.shutdown();
    }

    @Test
    public void testRunningMacro() throws InterruptedException {
        var macros = new MacroContainer("");
        var actions = new MappingActionContainer("");
        var uuid = UUID.randomUUID();
        var action = new MappingAction(new SelectionIO(), new AnnotationSetter(), new MappingPoint[0], ActionType.SET,
                "test action", "desc", uuid);
        var macro = new Macro("test macro", "desc", new UUID[]{uuid}, UUID.randomUUID(), new boolean[]{true});
        macros.addMapping(macro);
        actions.addMapping(action);

        Dimension dim = TestData
                .createDimension(new Rectangle(-2 * TILE_SIZE, -2 * TILE_SIZE, 3 * TILE_SIZE, 3 * TILE_SIZE), 0);
        int[] counter = new int[]{0};
        Consumer<UUID> callbackAfterRun = (uuid1) -> {
            counter[0]++;
        };
        var applicator = new MacroConcurrentApplicator(macros, actions, () -> dim, callbackAfterRun);
        assertEquals(IDLE, applicator.getCurrentState().status());
        applicator.queueMacro(macro.getUid());
        applicator.queueMacro(macro.getUid());
        applicator.queueMacro(macro.getUid());
        assertEquals(3, applicator.getQueue().size());

        applicator.start();
        for (int i = 0; i < 1000; i++) {
            Thread.sleep(1000);
            if (counter[0] == 3)
                break;
        }
        assertEquals(0, applicator.getQueue().size());
        assertEquals(3, counter[0]);

        applicator.shutdown();
    }
}
