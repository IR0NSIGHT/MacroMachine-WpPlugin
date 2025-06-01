package org.ironsight.wpplugin.macromachine.operations;

import org.checkerframework.checker.units.qual.A;
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

        ArrayList<Integer> expectedSelection = new ArrayList<>();
        expectedSelection.add(0);
        assertIterableEquals(expectedSelection, newSelection, "new items are selected");
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
        expectedIds[4] = container.queryAll().get(0).getUid();  //inserted at tail

        assertArrayEquals(expectedIds, inserted.getExecutionUUIDs());

        ArrayList<Integer> expectedSelection = new ArrayList<>();
        expectedSelection.add(4);
        assertIterableEquals(expectedSelection, newSelection, "new items are selected");
    }

    @Test
    void insertSingleActionIntoNonEmptyMacroMiddleSelection() {
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
                new int[]{2}, //insert after index=2 in list => at index 3
                newSelection);
        assertEquals(5, inserted.getExecutionUUIDs().length);
        UUID[] expectedIds =  Arrays.copyOf(actionIds, 5);
        //ids 0,1,2 stay same
        expectedIds[3] = container.queryAll().get(0).getUid();  //inserted after idx 2
        expectedIds[4] = actionIds[3];

        assertArrayEquals(expectedIds, inserted.getExecutionUUIDs());
    }

    @Test
    void insertSingleActionIntoNonEmptyMacroMultiSelection() {
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
        ArrayList<UUID> addedIdsInOrder = new ArrayList<>();
        MappingMacro inserted = MappingMacro.insertSaveableActionToList(
                macro.clone(),
                inputItem,
                () -> { LayerMapping lm = container.addMapping(); addedIdsInOrder.add(lm.getUid()); return lm; },
                action -> container.updateMapping(action, Assertions::fail),
                new int[]{2,3}, //insert after index=2 in list => at index 3
                newSelection);
        assertEquals(6, inserted.getExecutionUUIDs().length);

        //ids: 0,1,2, insert, 3, insert
        assertEquals(actionIds[0], inserted.getExecutionUUIDs()[0]);
        assertEquals(actionIds[1], inserted.getExecutionUUIDs()[1]);
        assertEquals(actionIds[2], inserted.getExecutionUUIDs()[2]);
        assertEquals(addedIdsInOrder.get(0), inserted.getExecutionUUIDs()[3]);
        assertEquals(actionIds[3], inserted.getExecutionUUIDs()[4]);
        assertEquals(addedIdsInOrder.get(1), inserted.getExecutionUUIDs()[5]);

        ArrayList<Integer> expectedSelection = new ArrayList<>();
        expectedSelection.add(3);
        expectedSelection.add(5);

        assertIterableEquals(expectedSelection, newSelection, "new items are selected");

    }
}
