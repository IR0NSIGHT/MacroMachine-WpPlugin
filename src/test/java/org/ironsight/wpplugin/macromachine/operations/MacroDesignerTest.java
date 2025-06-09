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
        MappingActionContainer container = new MappingActionContainer("./TestMappings.json");

        Macro macro = new Macro("test", "descr", new UUID[0], UUID.randomUUID());

        MappingAction inputItem = MappingAction.getNewEmptyAction().withName("hello world");
        assertEquals(0, container.queryAll().size());

        ArrayList<Integer> newSelection = new ArrayList<>();
        Macro inserted = Macro.insertSaveableActionToList(
                macro.clone(),
                inputItem,
                container::addMapping,
                action -> container.updateMapping(action, Assertions::fail),
                new int[0],
                newSelection);
        assertEquals(1, inserted.getExecutionUUIDs().length);
        assertNotEquals(inputItem.getUid(), inserted.getExecutionUUIDs()[0],"macro receives a clone, not the original " +
                "input");
        MappingAction addedItem = container.queryById(inserted.getExecutionUUIDs()[0]);
        assertTrue(addedItem.equalIgnoreUUID(inputItem),"input and added item have same values, but not UUID");
        assertNotEquals(addedItem.getUid(), inputItem.getUid(),"input and added item are not true equal (different uuid)");
        assertEquals(1, container.queryAll().size(), "inserting cloned the action and updated the container");

        ArrayList<Integer> expectedSelection = new ArrayList<>();
        expectedSelection.add(0);
        assertIterableEquals(expectedSelection, newSelection, "new items are selected");
    }

    @Test
    void insertSingleActionIntoNonEmptyMacroNoSelection() {
        MappingActionContainer container = new MappingActionContainer("./TestMappings.json");
        UUID[] actionIds = new UUID[4];
        for (int i = 0; i < 4; i++) {
            actionIds[i] = UUID.randomUUID();
        }

        Macro macro = new Macro("test", "descr", actionIds, UUID.randomUUID());
        assertEquals(4, macro.getExecutionUUIDs().length);
        assertArrayEquals(actionIds, macro.getExecutionUUIDs());

        MappingAction inputItem = MappingAction.getNewEmptyAction().withName("my new item");

        ArrayList<Integer> newSelection = new ArrayList<>();
        Macro inserted = Macro.insertSaveableActionToList(
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
        MappingActionContainer container = new MappingActionContainer("./TestMappings.json");
        UUID[] actionIds = new UUID[4];
        for (int i = 0; i < 4; i++) {
            actionIds[i] = UUID.randomUUID();
        }

        Macro macro = new Macro("test", "descr", actionIds, UUID.randomUUID());
        assertEquals(4, macro.getExecutionUUIDs().length);
        assertArrayEquals(actionIds, macro.getExecutionUUIDs());

        MappingAction inputItem = MappingAction.getNewEmptyAction().withName("my new item");

        ArrayList<Integer> newSelection = new ArrayList<>();
        Macro inserted = Macro.insertSaveableActionToList(
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
    void insertManyActionsIntoMacro() {
        MappingActionContainer container = new MappingActionContainer("./TestMappings.json");
        UUID[] actionIds = new UUID[4];
        for (int i = 0; i < 4; i++) {
            actionIds[i] = UUID.randomUUID();
        }

        Macro macro = new Macro("test", "descr", actionIds, UUID.randomUUID());
        assertEquals(4, macro.getExecutionUUIDs().length);
        assertArrayEquals(actionIds, macro.getExecutionUUIDs());

        MappingAction inputItem = MappingAction.getNewEmptyAction().withName("my new item");

        ArrayList<Integer> newSelection = new ArrayList<>();
        ArrayList<UUID> addedIdsInOrder = new ArrayList<>();
        Macro inserted = Macro.insertSaveableActionToList(
                macro.clone(),
                inputItem,
                () -> { MappingAction lm = container.addMapping(); addedIdsInOrder.add(lm.getUid()); return lm; },
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

    @Test
    void insertNestedMacroIntoMacro() {
        UUID[] actionIds = new UUID[4];
        for (int i = 0; i < 4; i++) {
            actionIds[i] = UUID.randomUUID();
        }

        Macro macro = new Macro("test", "descr", actionIds, UUID.randomUUID());
        assertEquals(4, macro.getExecutionUUIDs().length);
        assertArrayEquals(actionIds, macro.getExecutionUUIDs());

        Macro inputItem = new Macro("myNewMacro", "descr", new UUID[0], UUID.randomUUID());

        ArrayList<Integer> newSelection = new ArrayList<>();
        Macro inserted = Macro.insertSaveableActionToList(
                macro.clone(),
                inputItem,
                () -> { Assertions.fail("not supposed to be called"); return null; },
                action -> {
                    Assertions.fail("not supposed to be called");
                },
                new int[]{2,3}, //insert after index=2 in list => at index 3
                newSelection);
        assertEquals(6, inserted.getExecutionUUIDs().length);

        //ids: 0,1,2, insert, 3, insert
        assertEquals(actionIds[0], inserted.getExecutionUUIDs()[0]);
        assertEquals(actionIds[1], inserted.getExecutionUUIDs()[1]);
        assertEquals(actionIds[2], inserted.getExecutionUUIDs()[2]);
        assertEquals(inputItem.getUid(), inserted.getExecutionUUIDs()[3], "macro is linked, not cloned so the UID is " +
                "known");
        assertEquals(actionIds[3], inserted.getExecutionUUIDs()[4]);
        assertEquals(inputItem.getUid(), inserted.getExecutionUUIDs()[3]);


        ArrayList<Integer> expectedSelection = new ArrayList<>();
        expectedSelection.add(3);
        expectedSelection.add(5);

        assertIterableEquals(expectedSelection, newSelection, "new items are selected");

    }
}
