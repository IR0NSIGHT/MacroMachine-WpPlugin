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

    // ---- helpers ----

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
