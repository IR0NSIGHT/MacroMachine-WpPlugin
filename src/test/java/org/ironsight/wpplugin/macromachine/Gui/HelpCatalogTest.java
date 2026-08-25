package org.ironsight.wpplugin.macromachine.Gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ironsight.wpplugin.macromachine.Layers.CityBuilder.OptionsPanel;
import org.ironsight.wpplugin.macromachine.Layers.PathBuilder.PathTool;
import org.junit.jupiter.api.Test;

public class HelpCatalogTest
{

    @Test
    void catalogHasThreeEntries() {
        assertEquals(3, HelpCatalog.values().length);
    }

    @Test
    void fileNamesEndWithPng() {
        for (HelpCatalog c : HelpCatalog.values()) {
            assertTrue(c.getFileName().endsWith(".png"), c + " filename should end with .png");
            assertFalse(c.getFileName().contains(" "), "filename should not contain spaces: " + c.getFileName());
        }
    }

    @Test
    void titlesAreNonBlank() {
        for (HelpCatalog c : HelpCatalog.values()) {
            assertNotNull(c.getTitle());
            assertFalse(c.getTitle().isBlank());
            assertNotNull(c.getHelpItems());
            assertFalse(c.getHelpItems().isEmpty(), "help items empty for " + c);
        }
    }

    @Test
    void cityHelpStaysInSyncWithOptionsPanel() {
        assertEquals(OptionsPanel.HELP_TITLE, HelpCatalog.CITY.getTitle());
        assertEquals(OptionsPanel.HELP_ITEMS, HelpCatalog.CITY.getHelpItems(),
                "HelpCatalog CITY drifted from OptionsPanel.HELP_ITEMS — update both");
    }

    @Test
    void roadHelpStaysInSyncWithPathTool() {
        assertEquals(PathTool.HELP_TITLE, HelpCatalog.ROAD.getTitle());
        assertEquals(PathTool.HELP_ITEMS, HelpCatalog.ROAD.getHelpItems(),
                "HelpCatalog ROAD drifted from PathTool.HELP_ITEMS — update both");
    }

    @Test
    void threeDDelegatesToRendererPanel() {
        // 3D items are dynamic via reflection; at least delegation should not return
        // null
        assertNotNull(HelpCatalog.THREE_D.getHelpItems());
        assertFalse(HelpCatalog.THREE_D.getHelpItems().isEmpty());
        assertEquals(RendererKeyBindingsPanel.HELP_TITLE, HelpCatalog.THREE_D.getTitle());
    }
}
