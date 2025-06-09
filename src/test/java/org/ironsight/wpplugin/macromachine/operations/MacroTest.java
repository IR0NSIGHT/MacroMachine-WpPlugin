package org.ironsight.wpplugin.macromachine.operations;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MacroTest {

    @Test
    void withReplacedUUIDs() {
        Macro initial = new Macro("Test","descr", new UUID[]{ UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(),UUID.randomUUID()},
                UUID.randomUUID());
        UUID replacer = UUID.randomUUID();

        Macro result = initial.withReplacedUUIDs(new int[]{1,3}, replacer);

        assertEquals(4,result.getExecutionUUIDs().length);
        assertEquals(initial.getExecutionUUIDs()[0],result.getExecutionUUIDs()[0]);
        assertEquals(replacer,result.getExecutionUUIDs()[1]);
        assertEquals(initial.getExecutionUUIDs()[2],result.getExecutionUUIDs()[2]);
        assertEquals(replacer,result.getExecutionUUIDs()[3]);


    }
}