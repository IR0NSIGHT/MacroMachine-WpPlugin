package org.ironsight.wpplugin.macromachine.Layers.CityBuilder;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

import org.ironsight.wpplugin.macromachine.operations.ValueProviders.TestDimension;
import org.junit.jupiter.api.Test;
import org.pepsoft.minecraft.Material;
import org.pepsoft.util.undo.UndoManager;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.objects.GenericObject;
import org.pepsoft.worldpainter.objects.WPObject;
import org.pepsoft.worldpainter.painting.NibbleLayerPaint;

/**
 * Undo / redo of the City Layer's houses, and that the saved layer stays as the
 * released plugin (0.4.20) reads and writes it.
 */
class CityLayerUndoTest
{
    // ---- backwards compatibility: the saved form must not change ----

    /**
     * The serialized fields and serialVersionUIDs of the released City Layer. A
     * change here breaks worlds and layer files of existing users: never adjust
     * this test to a change, keep new state transient instead.
     */
    @Test
    void theSavedFormIsTheReleasedOne() {
        assertEquals("uid=1 database:CityInfoDatabase lastEdited:long objects:ArrayList useHighlightColors:boolean",
                shape(CityLayer.class));
        assertEquals("uid=1 tileInformation:HashMap", shape(CityInfoDatabase.class));
    }

    private static String shape(Class<?> type) {
        var description = ObjectStreamClass.lookup(type);
        var fields = new TreeMap<String, String>();
        for (var field : description.getFields())
            fields.put(field.getName(), field.getType().getSimpleName());
        var text = new StringBuilder("uid=" + description.getSerialVersionUID());
        fields.forEach((name, fieldType) -> text.append(' ').append(name).append(':').append(fieldType));
        return text.toString();
    }

    /**
     * The bundled layer file was saved by a released plugin, with houses placed: it
     * reads with its houses, and writes and reads back the same.
     */
    @Test
    void aLayerSavedByTheReleaseReadsAndRoundTrips() throws Exception {
        CityLayer layer;
        try (var in = new ObjectInputStream(new GZIPInputStream(new BufferedInputStream(Objects
                .requireNonNull(CityLayer.class.getResourceAsStream(CityLayerLibrary.TUTORIAL_LAYER_RESOURCE)))))) {
            layer = (CityLayer) in.readObject();
        }
        assertEquals(32, layer.getObjectList().size());
        var houses = sorted(layer.getAllObjectStates());
        assertFalse(houses.isEmpty(), "the released file has houses placed");

        var bytes = new ByteArrayOutputStream();
        try (var out = new ObjectOutputStream(bytes)) {
            out.writeObject(layer);
        }
        CityLayer again;
        try (var in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            again = (CityLayer) in.readObject();
        }
        assertEquals(houses, sorted(again.getAllObjectStates()));
        assertEquals(32, again.getObjectList().size());
    }

    /**
     * Undo bookkeeping is not saved: a layer that was edited with undo saves as
     * before.
     */
    @Test
    void aLayerEditedWithUndoSavesItsCurrentHouses() throws Exception {
        Dimension dimension = dimensionWithUndo();
        CityLayer layer = layerWithObjects();
        layer.setDataAt(dimension, 100, 100, house(1, 100, 100));
        dimension.armSavePoint();
        layer.setDataAt(dimension, 130, 100, house(2, 130, 100));

        var bytes = new ByteArrayOutputStream();
        try (var out = new ObjectOutputStream(bytes)) {
            out.writeObject(layer);
        }
        CityLayer again;
        try (var in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            again = (CityLayer) in.readObject();
        }
        assertEquals(sorted(layer.getAllObjectStates()), sorted(again.getAllObjectStates()));
        assertEquals(2, again.getAllObjectStates().size());
        assertNull(again.trackedUndoManager());
    }

    // ---- undo / redo of the layer ----

    @Test
    void undoAndRedoTakeTheHousesAlongWithTheTiles() {
        Dimension dimension = dimensionWithUndo();
        UndoManager manager = CityLayer.undoManagerOf(dimension);
        CityLayer layer = layerWithObjects();

        // place (the layer registers itself with the dimension's undo manager)
        layer.setDataAt(dimension, 100, 100, house(1, 100, 100));
        assertSame(manager, layer.trackedUndoManager());
        dimension.armSavePoint();
        // move it
        layer.removeDataAt(dimension, 100, 100);
        layer.setDataAt(dimension, 140, 100, house(1, 140, 100));
        dimension.armSavePoint();
        assertNull(layer.getInformationAt(100, 100));
        assertEquals(0, layerValue(dimension, layer, 100, 100));
        assertNotEquals(0, layerValue(dimension, layer, 140, 100));

        // undo the move: back where it was, the map too
        assertTrue(manager.undo());
        assertEquals(1, layer.getInformationAt(100, 100).objectIndex);
        assertNull(layer.getInformationAt(140, 100));
        assertNotEquals(0, layerValue(dimension, layer, 100, 100));
        assertEquals(0, layerValue(dimension, layer, 140, 100));
        // undo the placing: gone (before the layer was registered: as it was then)
        assertTrue(manager.undo());
        assertTrue(layer.getAllObjectStates().isEmpty());
        assertEquals(0, layerValue(dimension, layer, 100, 100));
        // redo both
        assertTrue(manager.redo());
        assertEquals(List.of(house(1, 100, 100)), layer.getAllObjectStates());
        assertTrue(manager.redo());
        assertEquals(List.of(house(1, 140, 100)), layer.getAllObjectStates());
        assertNotEquals(0, layerValue(dimension, layer, 140, 100));
    }

    @Test
    void anEditAfterUndoStartsFromTheUndoneHouses() {
        Dimension dimension = dimensionWithUndo();
        UndoManager manager = CityLayer.undoManagerOf(dimension);
        CityLayer layer = layerWithObjects();
        layer.setDataAt(dimension, 100, 100, house(0, 100, 100));
        dimension.armSavePoint();
        layer.setDataAt(dimension, 140, 100, house(1, 140, 100));
        dimension.armSavePoint();
        manager.undo();
        manager.armSavePoint(); // as WorldPainter's Undo does
        layer.setDataAt(dimension, 180, 100, house(2, 180, 100));
        assertEquals(List.of(house(0, 100, 100), house(2, 180, 100)), sorted(layer.getAllObjectStates()));
        // the undone house is not back after another undo / redo round
        manager.undo();
        assertEquals(List.of(house(0, 100, 100)), layer.getAllObjectStates());
        manager.redo();
        assertEquals(List.of(house(0, 100, 100), house(2, 180, 100)), sorted(layer.getAllObjectStates()));
    }

    @Test
    void housesPlacedBeforeUndoWasTrackedStayOnUndo() {
        // a world from before: houses already there when the tool registers the layer
        Dimension dimension = dimensionWithUndo();
        CityLayer layer = layerWithObjects();
        layer.setDataAt(dimension, 100, 100, house(0, 100, 100)); // registers now
        dimension.armSavePoint();
        Dimension other = dimensionWithUndo();
        // moving to another dimension's undo manager keeps the houses
        layer.trackUndo(CityLayer.undoManagerOf(other));
        layer.setDataAt(other, 140, 100, house(1, 140, 100));
        other.armSavePoint();
        CityLayer.undoManagerOf(other).undo();
        assertEquals(List.of(house(0, 100, 100)), layer.getAllObjectStates());
        // undo beyond what this manager saw: nothing more to undo, the old house stays
        assertFalse(CityLayer.undoManagerOf(other).undo());
        assertEquals(List.of(house(0, 100, 100)), layer.getAllObjectStates());
    }

    /**
     * The house list joins the undo history at the City Tool's first edit, so
     * earlier undo steps do not have it. Undoing past that step and then editing
     * once handed back no list (null): every house was gone and the tool threw on
     * each mouse move.
     */
    @Test
    void anEditAfterUndoingPastTheFirstCityEditKeepsTheHouses() {
        // a house from before (placed without undo, e.g. loaded with the world)
        Dimension dimension = TestDimension.createDimension(new TestDimension.DimensionParams(new Rectangle(500, 500),
                -256, 512, 70, 123456789, 62, org.pepsoft.worldpainter.DefaultPlugin.JAVA_ANVIL_1_19,
                org.pepsoft.worldpainter.Terrain.GRASS));
        CityLayer layer = layerWithObjects();
        layer.setDataAt(dimension, 300, 300, house(0, 300, 300));
        UndoManager manager = new UndoManager(50);
        dimension.registerUndoManager(manager);

        // two other WorldPainter edits, then the first City edit
        dimension.setHeightAt(10, 10, 80);
        dimension.armSavePoint();
        dimension.setHeightAt(20, 20, 90);
        dimension.armSavePoint();
        layer.setDataAt(dimension, 100, 100, house(1, 100, 100));
        dimension.armSavePoint();

        // undo the house and one more step (as WorldPainter's Undo: undo, then arm)
        assertTrue(manager.undo());
        manager.armSavePoint();
        assertTrue(manager.undo());
        manager.armSavePoint();
        assertEquals(List.of(house(0, 300, 300)), layer.getAllObjectStates());

        // edit again
        layer.setDataAt(dimension, 140, 100, house(2, 140, 100));
        dimension.armSavePoint();
        assertEquals(List.of(house(2, 140, 100), house(0, 300, 300)), sorted(layer.getAllObjectStates()));

        // undo / redo still work around it, and the old house never goes
        assertTrue(manager.undo());
        assertEquals(List.of(house(0, 300, 300)), layer.getAllObjectStates());
        assertFalse(manager.undo(), "the first height edit went into the starting step");
        assertEquals(List.of(house(0, 300, 300)), layer.getAllObjectStates());
        assertTrue(manager.redo());
        assertEquals(List.of(house(2, 140, 100), house(0, 300, 300)), sorted(layer.getAllObjectStates()));
        assertNotEquals(0, layerValue(dimension, layer, 140, 100));

        // and the next edits go on from there
        layer.removeDataAt(dimension, 140, 100);
        dimension.armSavePoint();
        assertEquals(List.of(house(0, 300, 300)), layer.getAllObjectStates());
        assertTrue(manager.undo());
        assertEquals(List.of(house(2, 140, 100), house(0, 300, 300)), sorted(layer.getAllObjectStates()));
    }

    /** The same through the City Tool, the way the bug showed (on the next mouse move). */
    @Test
    void theToolKeepsWorkingAfterUndoingPastItsFirstEdit() {
        Dimension dimension = dimensionWithUndo();
        UndoManager manager = CityLayer.undoManagerOf(dimension);
        CityLayer layer = layerWithObjects();
        CityEditToolOperation operation = new CityEditToolOperation();
        operation.setView(viewOn(dimension));
        operation.setPaint(new NibbleLayerPaint(layer));
        dimension.setHeightAt(10, 10, 80);
        dimension.armSavePoint();
        dimension.setHeightAt(20, 20, 90);
        dimension.armSavePoint();
        operation.placeAt(100, 100);
        assertEquals(1, layer.getAllObjectStates().size());

        manager.undo();
        manager.armSavePoint();
        manager.undo();
        manager.armSavePoint();
        operation.placeAt(200, 200);
        assertEquals(1, layer.getAllObjectStates().size());
        operation.handleClick(200, 200, false); // hit test, as on a mouse move
        assertEquals(1, operation.selection().size());
    }

    @Test
    void withoutAnUndoManagerTheLayerEditsAsBefore() {
        Dimension dimension = TestDimension.createDimension(new TestDimension.DimensionParams());
        CityLayer layer = layerWithObjects();
        layer.setDataAt(dimension, 100, 100, house(1, 100, 100));
        assertNull(layer.trackedUndoManager());
        assertEquals(List.of(house(1, 100, 100)), layer.getAllObjectStates());
        layer.forgetPlacements();
        assertTrue(layer.getAllObjectStates().isEmpty());
    }

    // ---- the City Tool: one undo step per action ----

    @Test
    void eachToolActionIsOneUndoStep() {
        Dimension dimension = dimensionWithUndo();
        UndoManager manager = CityLayer.undoManagerOf(dimension);
        CityLayer layer = layerWithObjects();
        CityEditToolOperation operation = new CityEditToolOperation();
        operation.setView(viewOn(dimension));
        operation.setPaint(new NibbleLayerPaint(layer));

        operation.addModeToggle().setSelected(true);
        operation.setType(1);
        operation.placeAt(100, 100);
        operation.addModeToggle().setSelected(false);
        operation.handleClick(100, 100, false);
        // two nudges right (D), each its own step
        operation.handleKeyInteraction(KeyEvent.VK_D);
        operation.handleKeyInteraction(KeyEvent.VK_D);
        assertEquals(List.of(house(1, 102, 100)), layer.getAllObjectStates());

        manager.undo();
        assertEquals(List.of(house(1, 101, 100)), layer.getAllObjectStates());
        // the selection follows: the house that is there now is selected
        assertEquals(List.of(), operation.selection(), "the selected house (at 102) is gone");
        manager.undo();
        assertEquals(List.of(house(1, 100, 100)), layer.getAllObjectStates());
        manager.undo();
        assertTrue(layer.getAllObjectStates().isEmpty(), "placing is undone");
        manager.redo();
        manager.redo();
        manager.redo();
        assertEquals(List.of(house(1, 102, 100)), layer.getAllObjectStates());
    }

    @Test
    void deleteAndPasteUndo() {
        Dimension dimension = dimensionWithUndo();
        UndoManager manager = CityLayer.undoManagerOf(dimension);
        CityLayer layer = layerWithObjects();
        CityEditToolOperation operation = new CityEditToolOperation();
        operation.setView(viewOn(dimension));
        operation.setPaint(new NibbleLayerPaint(layer));
        operation.placeAt(100, 100); // Ctrl-click style placing outside add mode
        operation.handleClick(100, 100, false);
        assertEquals(1, operation.selection().size());

        operation.handleKeyInteraction(KeyEvent.VK_DELETE);
        assertTrue(layer.getAllObjectStates().isEmpty());
        manager.undo();
        assertEquals(1, layer.getAllObjectStates().size(), "delete is undone");

        operation.handleClick(100, 100, false);
        operation.handleKeyInteraction(KeyEvent.VK_C, true);
        operation.handleClick(300, 300, false); // the cursor goes there
        operation.handleKeyInteraction(KeyEvent.VK_V, true);
        assertEquals(2, layer.getAllObjectStates().size());
        manager.undo();
        assertEquals(1, layer.getAllObjectStates().size(), "paste is undone");
        assertTrue(operation.selection().isEmpty(), "the pasted (selected) house is gone");
    }

    // ---- changing the schematic list clears the undo history ----

    @Test
    void afterTheSchematicListChangedUndoCannotBringBackHousesAsOtherSchematics() {
        Dimension dimension = dimensionWithUndo();
        UndoManager manager = CityLayer.undoManagerOf(dimension);
        CityLayer layer = layerWithSchematicFiles();
        layer.setDataAt(dimension, 100, 100, house(1, 100, 100));
        dimension.armSavePoint();
        layer.setDataAt(dimension, 140, 100, house(2, 140, 100));
        dimension.armSavePoint();

        // remove schematic 0: 1 and 2 move up, so their houses go
        ArrayList<WPObject> withoutFirst = new ArrayList<>(layer.getObjectList().subList(1, 3));
        assertEquals(List.of(0, 1, 2), layer.positionsLosingHouses(withoutFirst, Set.of()));
        assertEquals(2, layer.housesAt(layer.positionsLosingHouses(withoutFirst, Set.of())));
        layer.setObjectList(withoutFirst, Set.of());
        layer.clearUndoHistory(dimension);
        assertTrue(layer.getAllObjectStates().isEmpty());

        // without the clear, undo would bring them back as the schematics now at 1 and (none) 2
        assertFalse(manager.undo(), "nothing before the change is left to undo");
        assertTrue(layer.getAllObjectStates().isEmpty());

        // the first action after it is its own undo step
        layer.setDataAt(dimension, 180, 100, house(0, 180, 100));
        dimension.armSavePoint();
        assertTrue(manager.undo());
        assertTrue(layer.getAllObjectStates().isEmpty());
        assertTrue(manager.redo());
        assertEquals(List.of(house(0, 180, 100)), layer.getAllObjectStates());
    }

    @Test
    void addingOrReplacingASchematicKeepsEveryHouse() {
        CityLayer layer = layerWithSchematicFiles();
        ArrayList<WPObject> added = new ArrayList<>(layer.getObjectList());
        added.add(schematic("building-3", 3));
        assertEquals(List.of(), layer.positionsLosingHouses(added, Set.of()));
        ArrayList<WPObject> replaced = new ArrayList<>(layer.getObjectList());
        replaced.set(1, schematic("other", 1));
        assertEquals(List.of(1), layer.positionsLosingHouses(replaced, Set.of()));
        assertEquals(List.of(), layer.positionsLosingHouses(replaced, Set.of(1)), "replaced on purpose");
    }

    @Test
    void removingASchematicInTheEditorAsksAndCancelKeepsEverything() {
        Dimension dimension = dimensionWithUndo();
        UndoManager manager = CityLayer.undoManagerOf(dimension);
        CityLayer layer = layerWithSchematicFiles();
        layer.setDataAt(dimension, 100, 100, house(1, 100, 100));
        dimension.armSavePoint();
        CityLayerEditor editor = editorFor(layer, dimension);
        List<Integer> asked = new ArrayList<>();
        editor.removalConfirmation = (e, housesDeleted) -> {
            asked.add(housesDeleted);
            return false;
        };

        schematicList(editor).setSelectedIndex(0);
        editor.removeFiles();
        assertEquals(List.of(1), asked, "warned, with the one house that would go");
        assertEquals(3, schematicList(editor).getModel().getSize(), "Cancel removes nothing");
        editor.commit();
        assertEquals(List.of(house(1, 100, 100)), layer.getAllObjectStates());
        // the history is kept: the placing can still be undone (OK's repaint may add a step first)
        for (int steps = 0; steps < 3 && !layer.getAllObjectStates().isEmpty(); steps++)
            assertTrue(manager.undo(), "the undo history is kept");
        assertTrue(layer.getAllObjectStates().isEmpty());
    }

    @Test
    void removingASchematicInTheEditorClearsTheUndoHistoryOnOk() {
        Dimension dimension = dimensionWithUndo();
        UndoManager manager = CityLayer.undoManagerOf(dimension);
        CityLayer layer = layerWithSchematicFiles();
        layer.setDataAt(dimension, 100, 100, house(0, 100, 100));
        dimension.armSavePoint();
        layer.setDataAt(dimension, 140, 100, house(2, 140, 100));
        dimension.armSavePoint();
        CityLayerEditor editor = editorFor(layer, dimension);
        editor.removalConfirmation = (e, housesDeleted) -> true;

        schematicList(editor).setSelectedIndex(1); // building-1: building-2 moves up
        editor.removeFiles();
        assertEquals(2, schematicList(editor).getModel().getSize());
        assertTrue(manager.undo(), "nothing cleared before OK");
        manager.redo();

        editor.commit();
        assertEquals(List.of(house(0, 100, 100)), layer.getAllObjectStates());
        assertFalse(manager.undo(), "OK cleared the undo history");
        assertEquals(List.of(house(0, 100, 100)), layer.getAllObjectStates());
    }

    @Test
    void anEditorResetForgetsAnAgreedRemoval() {
        Dimension dimension = dimensionWithUndo();
        UndoManager manager = CityLayer.undoManagerOf(dimension);
        CityLayer layer = layerWithSchematicFiles();
        layer.setDataAt(dimension, 100, 100, house(0, 100, 100));
        dimension.armSavePoint();
        CityLayerEditor editor = editorFor(layer, dimension);
        editor.removalConfirmation = (e, housesDeleted) -> true;
        schematicList(editor).setSelectedIndex(2);
        editor.removeFiles();

        editor.reset(); // Reset in the layer settings: back to the layer's schematics
        editor.commit();
        assertEquals(3, layer.getObjectList().size());
        assertTrue(manager.undo(), "the undo history is kept");
    }

    // ---- helpers ----

    private static CityLayerEditor editorFor(CityLayer layer, Dimension dimension) {
        CityLayerEditor editor = new CityLayerEditor();
        editor.setContext(new org.pepsoft.worldpainter.layers.LayerEditor.LayerEditorContext() {
            @Override
            public org.pepsoft.worldpainter.ColourScheme getColourScheme() {
                return null;
            }

            @Override
            public boolean isExtendedBlockIds() {
                return false;
            }

            @Override
            public org.pepsoft.worldpainter.biomeschemes.CustomBiomeManager getCustomBiomeManager() {
                return null;
            }

            @Override
            public List<org.pepsoft.worldpainter.layers.Layer> getAllLayers() {
                return List.of(layer);
            }

            @Override
            public void settingsChanged() {
            }

            @Override
            public Dimension getDimension() {
                return dimension;
            }
        });
        editor.setLayer(layer);
        return editor;
    }

    @SuppressWarnings("unchecked")
    private static javax.swing.JList<WPObject> schematicList(java.awt.Container container) {
        for (java.awt.Component child : container.getComponents()) {
            if (child instanceof javax.swing.JList<?> list)
                return (javax.swing.JList<WPObject>) list;
            if (child instanceof java.awt.Container inner) {
                var found = schematicList(inner);
                if (found != null)
                    return found;
            }
        }
        return null;
    }

    /** Three schematics loaded from files (the layer tells them apart by file). */
    private static CityLayer layerWithSchematicFiles() {
        CityLayer layer = new CityLayer("test", "test");
        ArrayList<WPObject> objects = new ArrayList<>();
        for (int i = 0; i < 3; i++)
            objects.add(schematic("building-" + i, i));
        layer.setObjectList(objects);
        return layer;
    }

    private static WPObject schematic(String name, int size) {
        Material[] blocks = new Material[size + 2];
        Arrays.fill(blocks, Material.STONE);
        WPObject object = new GenericObject(name, size + 2, 1, 1, blocks);
        object.setAttribute(WPObject.ATTRIBUTE_FILE, new File(name + ".schem"));
        return object;
    }

    /**
     * The one test view (WorldPainter allows one per JVM), on {@code dimension} for
     * this test.
     */
    private static CityEditToolOperationTest.TestView viewOn(Dimension dimension) {
        var view = CityEditToolOperationTest.TEST_VIEW;
        borrowedFrom = view.getDimension();
        view.setDimension(dimension);
        return view;
    }

    private static Dimension borrowedFrom;

    @org.junit.jupiter.api.AfterEach
    void giveTheViewBack() {
        if (borrowedFrom != null)
            CityEditToolOperationTest.TEST_VIEW.setDimension(borrowedFrom);
        borrowedFrom = null;
    }

    private static Dimension dimensionWithUndo() {
        Dimension dimension = TestDimension.createDimension(new TestDimension.DimensionParams(new Rectangle(500, 500),
                -256, 512, 70, 123456789, 62, org.pepsoft.worldpainter.DefaultPlugin.JAVA_ANVIL_1_19,
                org.pepsoft.worldpainter.Terrain.GRASS));
        dimension.registerUndoManager(new UndoManager(50));
        return dimension;
    }

    private static int layerValue(Dimension dimension, CityLayer layer, int x, int y) {
        // anywhere on the house's footprint (its anchor may be on an offset)
        int value = 0;
        for (int dx = -4; dx <= 4; dx++)
            for (int dy = -4; dy <= 4; dy++)
                value = Math.max(value, dimension.getLayerValueAt(layer, x + dx, y + dy));
        return value;
    }

    private static ObjectState house(int type, int x, int y) {
        return new ObjectState(CityLayer.Direction.NORTH, false, type, x, y);
    }

    private static List<ObjectState> sorted(Collection<ObjectState> states) {
        var list = new ArrayList<>(states);
        list.sort(Comparator.<ObjectState>comparingInt(state -> state.xPos).thenComparingInt(state -> state.yPos));
        return list;
    }

    private static CityLayer layerWithObjects() {
        CityLayer layer = new CityLayer("test", "test");
        ArrayList<WPObject> objects = new ArrayList<>();
        objects.add(new GenericObject("building-0", 2, 1, 1, new Material[]{Material.STONE, Material.STONE}));
        objects.add(new GenericObject("building-1", 3, 1, 1,
                new Material[]{Material.STONE, Material.STONE, Material.STONE}));
        objects.add(new GenericObject("building-2", 4, 1, 1,
                new Material[]{Material.STONE, Material.STONE, Material.STONE, Material.STONE}));
        layer.setObjectList(objects);
        return layer;
    }
}
