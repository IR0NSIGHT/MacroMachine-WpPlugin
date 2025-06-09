package org.ironsight.wpplugin.macromachine.operations;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MacroContainerTest {

    @Test
    public void saveLoad() {
        MacroContainer container = new MacroContainer("./saveLoad_Test_Macros.json");
        MacroContainer.SetInstance(container);
        Macro saved = container.addMapping().withName("my first " + "macro").withDescription("this is a test " +
                "description").withUUIDs(new UUID[]{UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID()});
        container.updateMapping(saved, f -> {});
        assertEquals(saved, container.queryById(saved.getUid()));

        container.setFilePath(System.getProperty("user.dir") + "/test_saves.txt");
        container.writeToFile();
        container.readFromFile();

        MacroContainer newContainer = new MacroContainer("./saveLoad_Test_Macros_2.json");
        newContainer.setFilePath(container.getFilePath());
        newContainer.readFromFile();
        Macro loaded = newContainer.queryById(saved.getUid());
        assertEquals(saved, loaded);
    }
}