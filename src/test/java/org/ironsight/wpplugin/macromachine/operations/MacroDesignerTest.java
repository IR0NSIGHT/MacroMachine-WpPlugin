package org.ironsight.wpplugin.macromachine.operations;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class MacroDesignerTest {
    @Test
    void insertSingleActionIntoEmptyMacro() {
        LayerMappingContainer container = new LayerMappingContainer("./TestMappings.json");

        MappingMacro macro = new MappingMacro("test", "descr", new UUID[0], UUID.randomUUID());

        LayerMapping inputItem = LayerMapping.getNewEmptyAction().withName("hello world");
        assertEquals(0, container.queryAll().size());

        ArrayList<Integer> newSelection = new ArrayList<>();
        MappingMacro inserted = MappingMacro.insertSaveableActionToList(
                macro.clone(),
                inputItem,
                container::addMapping,
                action -> container.updateMapping(action, Assertions::fail),
                new int[0],
                newSelection);
        assertEquals(1, inserted.getExecutionUUIDs().length);
        assertNotEquals(inputItem.getUid(), inserted.getExecutionUUIDs()[0],"macro receives a clone, not the original " +
                "input");
        LayerMapping addedItem = container.queryById(inserted.getExecutionUUIDs()[0]);
        assertTrue(addedItem.equalIgnoreUUID(inputItem),"input and added item have same values, but not UUID");
        assertNotEquals(addedItem.getUid(), inputItem.getUid(),"input and added item are not true equal (different uuid)");
        assertEquals(1, container.queryAll().size(), "inserting cloned the action and updated the container");
    }

    @Test
    void insertSingleActionIntoNonEmptyMacroNoSelection() {
        LayerMappingContainer container = new LayerMappingContainer("./TestMappings.json");
        UUID[] actionIds = new UUID[4];
        for (int i = 0; i < 4; i++) {
            actionIds[i] = UUID.randomUUID();
        }

        MappingMacro macro = new MappingMacro("test", "descr", actionIds, UUID.randomUUID());
        assertEquals(4, macro.getExecutionUUIDs().length);
        assertArrayEquals(actionIds, macro.getExecutionUUIDs());

        LayerMapping inputItem = LayerMapping.getNewEmptyAction().withName("my new item");

        ArrayList<Integer> newSelection = new ArrayList<>();
        MappingMacro inserted = MappingMacro.insertSaveableActionToList(
                macro.clone(),
                inputItem,
                container::addMapping,
                action -> container.updateMapping(action, Assertions::fail),
                new int[0], //tail insert
                newSelection);
        assertEquals(5, inserted.getExecutionUUIDs().length);
        UUID[] expectedIds =  Arrays.copyOf(actionIds, 5);
        expectedIds[4] = container.queryAll().get(0).getUid();

        assertArrayEquals(expectedIds, inserted.getExecutionUUIDs());
    }
}
