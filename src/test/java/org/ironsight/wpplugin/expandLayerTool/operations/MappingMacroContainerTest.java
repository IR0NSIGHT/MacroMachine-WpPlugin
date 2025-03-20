package org.ironsight.wpplugin.expandLayerTool.operations;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MappingMacroContainerTest {

    @Test
    public void saveLoad() {
        MappingMacroContainer container = new MappingMacroContainer();
        MappingMacro saved = container.addMapping().withName("my first " + "macro").withDescription("this is a test " +
                "description").withUUIDs(new UUID[]{UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID()});
        container.updateMapping(saved, f -> {});
        assertEquals(saved, container.queryById(saved.getUid()));

        container.setFilePath(System.getProperty("user.dir") + "/test_saves.txt");
        container.writeToFile();
        container.readFromFile();

        MappingMacroContainer newContainer = new MappingMacroContainer();
        newContainer.setFilePath(container.getFilePath());
        newContainer.readFromFile();
        MappingMacro loaded = newContainer.queryById(saved.getUid());
        assertEquals(saved, loaded);
    }
}