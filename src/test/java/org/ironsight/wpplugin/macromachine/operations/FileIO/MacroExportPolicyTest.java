package org.ironsight.wpplugin.macromachine.operations.FileIO;

import org.ironsight.wpplugin.macromachine.operations.Macro;
import org.ironsight.wpplugin.macromachine.operations.MacroContainer;
import org.ironsight.wpplugin.macromachine.operations.MappingAction;
import org.ironsight.wpplugin.macromachine.operations.MappingActionContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.ironsight.wpplugin.macromachine.operations.FileIO.ContainerIOTest.*;
import static org.junit.jupiter.api.Assertions.*;

class MacroExportPolicyTest {
    private MappingActionContainer actionContainer;
    private MacroContainer macroContainer;

    @BeforeEach
    public void setUp() {
        actionContainer = new MappingActionContainer("./ioTestAction.json");
        macroContainer = new MacroContainer("./ioTestMacro.json");

        assertEquals(0, macroContainer.queryAll().size());
        assertEquals(0, actionContainer.queryAll().size());

        ContainerIOTest.fillWithData(actionContainer, macroContainer);
        assertEquals(3, macroContainer.queryAll().size(),"expect macros: empty, simple, complex");
        assertEquals(2, actionContainer.queryAll().size()); // 2 actions
    }
    @Test
    void testEmptyMacroExportPolicy() {
        Macro emtpyMacro = macroContainer.queryById(emptyMacroUID);
        assertNotNull(emtpyMacro);
        assertEquals(0, emtpyMacro.getExecutionUUIDs().length);
        MacroExportPolicy macroExportPolicy = new MacroExportPolicy(emtpyMacro, macroContainer);

        for (Macro m : macroContainer.queryAll()) {
            if (m.getUid().equals(emptyMacroUID))
                assertTrue( macroExportPolicy.allowImportExport(m));
            else
                assertFalse(macroExportPolicy.allowImportExport(m));
        }

        for (MappingAction a: actionContainer.queryAll())
            assertFalse(macroExportPolicy.allowImportExport(a));
    }

    @Test
    void testSimpleMacroExportPolicy() {
        Macro simpleMacro = macroContainer.queryById(simpleMacroUID);
        assertNotNull(simpleMacro);
        assertEquals(2, simpleMacro.getExecutionUUIDs().length);
        MacroExportPolicy macroExportPolicy = new MacroExportPolicy(simpleMacro, macroContainer);

        for (Macro m : macroContainer.queryAll()) {
            if (m.getUid().equals(simpleMacroUID))
                assertTrue( macroExportPolicy.allowImportExport(m));
            else
                assertFalse(macroExportPolicy.allowImportExport(m));
        }
        for (MappingAction a: actionContainer.queryAll())
            if (a.getUid().equals(applyGrassAction01) || a.getUid().equals(applyGrassAction02))
                assertTrue(macroExportPolicy.allowImportExport(a));
            else
                assertFalse(macroExportPolicy.allowImportExport(a));
    }

    @Test
    void testComplexMacroWithNestingExportPolicy() {
        Macro complexMacro = macroContainer.queryById(complexMacroUID);
        assertNotNull(complexMacro);
        assertEquals(2, complexMacro.getExecutionUUIDs().length, "has two nested macros");
        MacroExportPolicy macroExportPolicy = new MacroExportPolicy(complexMacro, macroContainer);

        for (Macro m : macroContainer.queryAll()) {
            if (m.getUid().equals(complexMacroUID) || m.getUid().equals(simpleMacroUID) || m.getUid().equals(emptyMacroUID))
                assertTrue( macroExportPolicy.allowImportExport(m));
            else
                assertFalse(macroExportPolicy.allowImportExport(m));
        }

        for (MappingAction a: actionContainer.queryAll())
            if (a.getUid().equals(applyGrassAction01) || a.getUid().equals(applyGrassAction02))
                assertTrue(macroExportPolicy.allowImportExport(a));
            else
                assertFalse(macroExportPolicy.allowImportExport(a));
    }
}