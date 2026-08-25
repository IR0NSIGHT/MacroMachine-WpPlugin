package org.ironsight.wpplugin.macromachine.Gui;

import java.awt.Frame;
import java.util.List;
import java.util.function.Supplier;
import org.ironsight.wpplugin.macromachine.Gui.HelpDialog.HelpItem;

/**
 * Central registry for tool help dialogs that are screenshotted in isolation.
 * Only tool helps (city / road / 3d) are included per requirements.
 * <p>
 * Strings are duplicated here to avoid classloading heavy tool classes
 * (PathTool -> PaintOperation/WPCore) during screenshot generation, which may
 * run with a minimal classpath.
 */
public enum HelpCatalog {
    CITY("city-tool", "City Editor",
            """
                    Use the City Tool to place and edit buildings in a City Layer. Create or import a City Layer, select it and use this tool to place schematics in the map.
                    Select buildings on the map, then use the interactions below to place, move, and edit them.
                    The tool settings show you the current state of your selected building. You can change the building type by selecting a different one from the list.

                    City Layers are not compatible with undo/redo. Do not use undo/redo while editing one.
                    """,
            List.of(new HelpItem("Ctrl + left click", "Place a new building"),
                    new HelpItem("Left click", "Select or deselect the building under the cursor"),
                    new HelpItem("Left-button drag", "Select buildings fully inside the drag rectangle"),
                    new HelpItem("Right click", "Move the selection center to the cursor"),
                    new HelpItem("Shift + mouse wheel", "Change the selected building type"),
                    new HelpItem("Ctrl + A", "Select all buildings"), new HelpItem("Escape", "Clear the selection"),
                    new HelpItem("Ctrl + C", "Copy selected buildings"),
                    new HelpItem("Ctrl + X", "Cut selected buildings"),
                    new HelpItem("Ctrl + V", "Paste buildings at the cursor"),
                    new HelpItem("Q", "Randomize selected buildings using enabled random options"),
                    new HelpItem("W/A/S/D", "Move selected buildings"),
                    new HelpItem("C", "Rotate selected buildings around the selection center"),
                    new HelpItem("X", "Mirror selected buildings"),
                    new HelpItem("Delete", "Delete selected buildings"))), ROAD("road-tool", "Road Tool",
                            """
                                    Use the Road Tool to connect clicked positions into a smooth path that is blended into existing terrain.
                                    The path width follows the brush radius and is previewed on the map before applying. Use presets to start
                                    quickly, then fine-tune blending, slope and transition settings below.
                                    """,
                            List.of(new HelpItem("Right click", "Start new path at cursor"),
                                    new HelpItem("Left click", "Advance current path to cursor"),
                                    new HelpItem("Backspace", "Delete last point on current path"),
                                    new HelpItem("Apply", "Commit previewed path to terrain / paint / water"),
                                    new HelpItem("Brush radius", "Controls path width and transition size"),
                                    new HelpItem("River / Road preset",
                                            "Apply recommended settings for rivers or roads"),
                                    new HelpItem("Only downhill", "Restrict path to horizontal or downhill movement"),
                                    new HelpItem("Snap to terrain", "Force path to follow terrain height"),
                                    new HelpItem("Set paint", "Apply selected paint along the path"),
                                    new HelpItem("Set water / Set terrain",
                                            "Choose how water level and height are edited"),
                                    new HelpItem("Curve strength", "-1 to 2, 0 = straight lines"),
                                    new HelpItem("Limit slope", "Max vertical blocks per 16 horizontal; 0 = unlimited"),
                                    new HelpItem("Transition multiplier / profile",
                                            "Width scale and falloff shape of path edges"))), THREE_D("3d-preview",
                                                    RendererKeyBindingsPanel.HELP_TITLE,
                                                    RendererKeyBindingsPanel.HELP_EXPLANATION,
                                                    RendererKeyBindingsPanel::getHelpItems);

    private final String fileName;
    private final String title;
    private final String explanation;
    private final Supplier<List<HelpItem>> itemsSupplier;

    HelpCatalog(String fileName, String title, String explanation, List<HelpItem> items) {
        this(fileName, title, explanation, () -> items);
    }

    HelpCatalog(String fileName, String title, String explanation, Supplier<List<HelpItem>> itemsSupplier) {
        this.fileName = fileName;
        this.title = title;
        this.explanation = explanation;
        this.itemsSupplier = itemsSupplier;
    }

    public String getFileName() {
        return fileName + ".png";
    }

    public HelpDialog createDialog(Frame owner) {
        return new HelpDialog(owner, title, explanation, itemsSupplier.get());
    }

    public List<HelpItem> getHelpItems() {
        return itemsSupplier.get();
    }

    public String getTitle() {
        return title;
    }
}
