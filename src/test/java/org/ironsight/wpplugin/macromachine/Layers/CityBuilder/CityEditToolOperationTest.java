package org.ironsight.wpplugin.macromachine.Layers.CityBuilder;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.Random;

import org.ironsight.wpplugin.macromachine.operations.ValueProviders.TestDimension;
import org.junit.jupiter.api.Test;
import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.brushes.SymmetricBrush;
import org.pepsoft.worldpainter.objects.GenericObject;
import org.pepsoft.worldpainter.objects.WPObject;
import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.painting.LayerPaint;
import org.pepsoft.worldpainter.painting.NibbleLayerPaint;

import javax.swing.*;

import static org.junit.jupiter.api.Assertions.*;

class CityEditToolOperationTest
{
    private static final Dimension TEST_DIMENSION = TestDimension
            .createDimension(new TestDimension.DimensionParams(new Rectangle(500, 500), -256, 512, 70, 123456789, 62,
                    org.pepsoft.worldpainter.DefaultPlugin.JAVA_ANVIL_1_19, org.pepsoft.worldpainter.Terrain.GRASS));
    private static final TestView TEST_VIEW = new TestView(TEST_DIMENSION);

    static ArrayList<WPObject> loadDevelopmentSchematics() throws IOException {
        var resource = CityEditToolOperation.class.getResource("/CityBuilder/Houses");
        if (resource == null)
            throw new IOException("Development schematic resources were not found: /CityBuilder/Houses");

        try {
            Path directory = Paths.get(resource.toURI());
            try (var paths = Files.list(directory)) {
                ArrayList<WPObject> schematics = new ArrayList<>();
                var provider = new DefaultCustomObjectProvider();
                paths.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".schem"))
                        .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                        .forEach(path -> {
                            try {
                                schematics.add(provider.loadObject(path.toFile()));
                            } catch (IOException exception) {
                                throw new DevelopmentSchematicLoadException(path, exception);
                            }
                        });
                if (schematics.isEmpty())
                    throw new IOException("No development schematics found in /CityBuilder/Houses");
                return schematics;
            } catch (DevelopmentSchematicLoadException exception) {
                throw exception.getIOException();
            }
        } catch (URISyntaxException exception) {
            throw new IOException("Could not resolve development schematic resources", exception);
        }
    }

    private static class DevelopmentSchematicLoadException extends RuntimeException
    {
        private final IOException exception;

        DevelopmentSchematicLoadException(Path path, IOException exception) {
            super("Could not load development schematic: " + path, exception);
            this.exception = exception;
        }

        IOException getIOException() {
            return exception;
        }
    }

    public static void main(String[] args) throws IOException {
        // set up layer
        CityLayer layer = new CityLayer("test-city-layer", "this is a description");
        layer.setObjectList(loadDevelopmentSchematics());

        // set up operation
        var op = new CityEditToolOperation();
        op.setPaint(new NibbleLayerPaint(layer));

        JDialog dialog = new JDialog((Frame) null, "CityLayer Options");
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.add(op.getOptionsPanel());
        dialog.setResizable(true);
        dialog.setSize(220, 250);
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    @Test
    void developmentSchematicsLoadOnlyTopLevelFixtures() throws Exception {
        var schematics = loadDevelopmentSchematics();

        assertEquals(8, schematics.size());
        assertEquals("Ektelion - Ukrainian house 1-converted.schem",
                schematics.get(0).getAttribute(WPObject.ATTRIBUTE_FILE).getName());
        assertEquals("Ektelion - Ukrainian house 9-converted.schem",
                schematics.get(7).getAttribute(WPObject.ATTRIBUTE_FILE).getName());
        schematics.forEach(schematic -> assertTrue(schematic.getDimensions().x > 0));
    }

    @Test
    void copyCutAndPastePreserveSelectionLayout() {
        CityLayer layer = layerWithObjects();
        CityEditToolOperation operation = new CityEditToolOperation();
        operation.setView(TEST_VIEW);
        operation.setPaint(new NibbleLayerPaint(layer));

        ObjectState first = new ObjectState(CityLayer.Direction.NORTH, false, 0, 100, 102);
        ObjectState second = new ObjectState(CityLayer.Direction.SOUTH, true, 1, 110, 104);
        layer.setDataAt(TEST_DIMENSION, first.xPos, first.yPos, first);
        layer.setDataAt(TEST_DIMENSION, second.xPos, second.yPos, second);

        operation.handleClick(first.xPos, first.yPos, false, false);
        operation.handleClick(second.xPos, second.yPos, false, true);
        operation.handleKeyInteraction(KeyEvent.VK_C, true);

        assertEquals(first, layer.getInformationAt(first.xPos, first.yPos));
        assertEquals(second, layer.getInformationAt(second.xPos, second.yPos));

        operation.setCursorWorldPosition(200, 200);
        operation.handleKeyInteraction(KeyEvent.VK_V, true);

        assertEquals(first.objectIndex, layer.getInformationAt(190, 198).objectIndex);
        assertEquals(first.rotation, layer.getInformationAt(190, 198).rotation);
        assertEquals(first.mirrored, layer.getInformationAt(190, 198).mirrored);
        assertEquals(second.objectIndex, layer.getInformationAt(200, 200).objectIndex);
        assertEquals(second.rotation, layer.getInformationAt(200, 200).rotation);
        assertEquals(second.mirrored, layer.getInformationAt(200, 200).mirrored);

        operation.handleKeyInteraction(KeyEvent.VK_X, true);
        assertNull(layer.getInformationAt(190, 198));
        assertNull(layer.getInformationAt(200, 200));

        operation.setCursorWorldPosition(300, 300);
        operation.handleKeyInteraction(KeyEvent.VK_V, true);
        assertEquals(first.objectIndex, layer.getInformationAt(290, 298).objectIndex);
        assertEquals(second.objectIndex, layer.getInformationAt(300, 300).objectIndex);
    }

    @Test
    void pastingAnEmptyClipboardDoesNothing() {
        CityLayer layer = layerWithObjects();
        CityEditToolOperation operation = new CityEditToolOperation();
        operation.setView(TEST_VIEW);
        operation.setPaint(new NibbleLayerPaint(layer));
        operation.setCursorWorldPosition(200, 200);

        operation.handleKeyInteraction(KeyEvent.VK_V, true);

        assertNull(layer.getInformationAt(200, 200));
    }

    @Test
    void movingSelectionPlacesItsCenterAtTheCursor() {
        CityLayer layer = layerWithObjects();
        CityEditToolOperation operation = new CityEditToolOperation();
        operation.setView(TEST_VIEW);
        operation.setPaint(new NibbleLayerPaint(layer));

        ObjectState first = new ObjectState(CityLayer.Direction.NORTH, false, 0, 100, 102);
        ObjectState second = new ObjectState(CityLayer.Direction.SOUTH, true, 1, 110, 104);
        layer.setDataAt(TEST_DIMENSION, first.xPos, first.yPos, first);
        layer.setDataAt(TEST_DIMENSION, second.xPos, second.yPos, second);

        operation.handleClick(first.xPos, first.yPos, false, false);
        operation.handleClick(second.xPos, second.yPos, false, true);
        operation.handleClick(200, 200, true, false);

        ObjectState movedFirst = layer.getInformationAt(195, 199);
        ObjectState movedSecond = layer.getInformationAt(205, 201);
        assertNotNull(movedFirst);
        assertNotNull(movedSecond);
        assertEquals(first.objectIndex, movedFirst.objectIndex);
        assertEquals(first.rotation, movedFirst.rotation);
        assertEquals(first.mirrored, movedFirst.mirrored);
        assertEquals(second.objectIndex, movedSecond.objectIndex);
        assertEquals(second.rotation, movedSecond.rotation);
        assertEquals(second.mirrored, movedSecond.mirrored);
    }

    @Test
    void rotatingSelectionRotatesPositionsAroundItsCenter() {
        CityLayer layer = layerWithObjects();
        CityEditToolOperation operation = new CityEditToolOperation();
        operation.setView(TEST_VIEW);
        operation.setPaint(new NibbleLayerPaint(layer));

        ObjectState first = new ObjectState(CityLayer.Direction.NORTH, false, 0, 100, 102);
        ObjectState second = new ObjectState(CityLayer.Direction.SOUTH, true, 1, 110, 104);
        layer.setDataAt(TEST_DIMENSION, first.xPos, first.yPos, first);
        layer.setDataAt(TEST_DIMENSION, second.xPos, second.yPos, second);

        operation.handleClick(first.xPos, first.yPos, false, false);
        operation.handleClick(second.xPos, second.yPos, false, true);
        operation.handleKeyInteraction(KeyEvent.VK_C);

        ObjectState rotatedFirst = layer.getInformationAt(104, 108);
        ObjectState rotatedSecond = layer.getInformationAt(106, 98);
        assertNotNull(rotatedFirst);
        assertNotNull(rotatedSecond);
        assertEquals(CityLayer.Direction.WEST, rotatedFirst.rotation);
        assertEquals(CityLayer.Direction.EAST, rotatedSecond.rotation);
        assertFalse(rotatedFirst.mirrored);
        assertTrue(rotatedSecond.mirrored);
        assertEquals(first.objectIndex, rotatedFirst.objectIndex);
        assertEquals(second.objectIndex, rotatedSecond.objectIndex);
    }

    @Test
    void randomisationIsAppliedOnlyWhenRequestedAndMovementKeepsObjectType() throws Exception {
        CityLayer layer = layerWithObjects();
        Dimension dimension = TEST_DIMENSION;
        TestView view = TEST_VIEW;
        CityEditToolOperation operation = new CityEditToolOperation();
        operation.setView(view);
        operation.setPaint(new NibbleLayerPaint(layer));
        JPanel mapView = new JPanel();
        JPanel mapChild = new JPanel();
        JPanel otherView = new JPanel();
        mapView.add(mapChild);
        assertFalse(CityEditToolOperation.isMapComponent(null, mapChild));
        assertTrue(CityEditToolOperation.isMapComponent(mapView, mapView));
        assertTrue(CityEditToolOperation.isMapComponent(mapView, mapChild));
        assertFalse(CityEditToolOperation.isMapComponent(mapView, otherView));
        operation.random = new Random(1234);

        operation.placeAt(100, 100);

        ObjectState placedState = layer.getInformationAt(100, 100);
        assertNotNull(placedState);
        assertEquals(0, placedState.objectIndex);

        operation.onMouseWheel(1);
        ObjectState wheelSelectedState = layer.getInformationAt(100, 100);
        assertNotNull(wheelSelectedState);
        assertEquals(1, wheelSelectedState.objectIndex);

        operation.setPlacementOptions(new CityEditToolOperation.PlacementOptions(false, true, false));

        operation.handleKeyInteraction(KeyEvent.VK_Q);
        ObjectState randomisedState = layer.getInformationAt(100, 100);
        assertNotNull(randomisedState);
        assertEquals(2, randomisedState.objectIndex);

        operation.handleKeyInteraction(KeyEvent.VK_W);
        assertNull(layer.getInformationAt(100, 100));
        ObjectState movedState = layer.getInformationAt(100, 99);
        assertNotNull(movedState);
        assertEquals(randomisedState.objectIndex, movedState.objectIndex);

        operation.handleClick(104, 99, false, false);
        operation.handleKeyInteraction(KeyEvent.VK_W);
        assertNotNull(layer.getInformationAt(100, 99));

        operation.handleClick(103, 99, false, false);
        operation.handleKeyInteraction(KeyEvent.VK_S);
        assertNotNull(layer.getInformationAt(100, 100));
        operation.handleKeyInteraction(KeyEvent.VK_W);

        operation.handleClick(100, 99, false, false);
        operation.handleClick(400, 400, false, false);
        assertNotNull(layer.getInformationAt(100, 99));

        operation.handleKeyInteraction(KeyEvent.VK_W);
        operation.handleKeyInteraction(KeyEvent.VK_C);
        operation.handleKeyInteraction(KeyEvent.VK_X);
        operation.handleKeyInteraction(KeyEvent.VK_Q);
        assertNull(layer.getInformationAt(Integer.MAX_VALUE, Integer.MAX_VALUE));
        assertNull(layer.getInformationAt(Integer.MAX_VALUE, Integer.MAX_VALUE - 1));

        operation.handleClick(200, 200, false, true);
        assertNotNull(layer.getInformationAt(200, 200));
        operation.handleClick(201, 200, true, false);
        assertNull(layer.getInformationAt(200, 200));
        assertNotNull(layer.getInformationAt(201, 200));

        operation.handleKeyInteraction(KeyEvent.VK_DELETE);
        assertNull(layer.getInformationAt(201, 200));

        ObjectState first = new ObjectState(CityLayer.Direction.NORTH, false, 0, 300, 300);
        ObjectState second = new ObjectState(CityLayer.Direction.EAST, true, 1, 310, 300);
        layer.setDataAt(dimension, first.xPos, first.yPos, first);
        layer.setDataAt(dimension, second.xPos, second.yPos, second);

        operation.handleClick(300, 300, false, false);
        operation.handleClick(310, 300, false, true);
        operation.handleClick(300, 300, false, true);
        operation.handleClick(300, 300, false, true);

        operation.handleKeyInteraction(KeyEvent.VK_W);
        ObjectState movedFirst = layer.getInformationAt(300, 299);
        ObjectState movedSecond = layer.getInformationAt(310, 299);
        assertNotNull(movedFirst);
        assertNotNull(movedSecond);
        assertEquals(first.objectIndex, movedFirst.objectIndex);
        assertEquals(second.objectIndex, movedSecond.objectIndex);
        assertEquals(first.rotation, movedFirst.rotation);
        assertEquals(second.rotation, movedSecond.rotation);
        assertEquals(first.mirrored, movedFirst.mirrored);
        assertEquals(second.mirrored, movedSecond.mirrored);

        operation.handleKeyInteraction(KeyEvent.VK_C);
        operation.handleKeyInteraction(KeyEvent.VK_X);
        ObjectState rotatedFirst = layer.getInformationAt(305, 304);
        ObjectState rotatedSecond = layer.getInformationAt(305, 294);
        assertEquals(CityLayer.Direction.WEST, rotatedFirst.rotation);
        assertEquals(CityLayer.Direction.NORTH, rotatedSecond.rotation);
        assertTrue(rotatedFirst.mirrored);
        assertFalse(rotatedSecond.mirrored);

        operation.onMouseWheel(-1);
        assertEquals(0, layer.getInformationAt(305, 304).objectIndex);
        assertEquals(0, layer.getInformationAt(305, 294).objectIndex);

        operation.handleKeyInteraction(KeyEvent.VK_DELETE);
        assertNull(layer.getInformationAt(305, 304));
        assertNull(layer.getInformationAt(305, 294));

        ObjectState boxed = new ObjectState(CityLayer.Direction.NORTH, false, 0, 320, 320);
        ObjectState outside = new ObjectState(CityLayer.Direction.NORTH, false, 1, 330, 320);
        layer.setDataAt(dimension, boxed.xPos, boxed.yPos, boxed);
        layer.setDataAt(dimension, outside.xPos, outside.yPos, outside);
        operation.selectWithinBox(layer, new Rectangle(319, 319, 4, 4));
        operation.handleKeyInteraction(KeyEvent.VK_W);

        assertNotNull(layer.getInformationAt(320, 319));
        assertNotNull(layer.getInformationAt(330, 320));
        assertNull(layer.getInformationAt(320, 320));

        operation.handleKeyInteraction(KeyEvent.VK_A, true);
        operation.handleKeyInteraction(KeyEvent.VK_W);

        assertNotNull(layer.getInformationAt(320, 318));
        assertNotNull(layer.getInformationAt(330, 319));
        operation.handleKeyInteraction(KeyEvent.VK_ESCAPE);
        operation.handleKeyInteraction(KeyEvent.VK_W);
        assertNotNull(layer.getInformationAt(320, 318));
        assertNotNull(layer.getInformationAt(330, 319));
    }

    @Test
    void transformsMoveTurnAndSpreadHousePositions() {
        ObjectState house = new ObjectState(CityLayer.Direction.NORTH, true, 1, 110, 100);

        assertEquals(new ObjectState(CityLayer.Direction.NORTH, true, 1, 113, 96),
                CityEditToolOperation.moveBy(3, -4).apply(house));
        // a quarter turn counter-clockwise on screen: east of the pivot becomes north
        ObjectState turned = CityEditToolOperation.rotateAround(new Point(100, 100), 1).apply(house);
        assertEquals(100, turned.xPos);
        assertEquals(90, turned.yPos);
        assertEquals(CityLayer.Direction.WEST, turned.rotation);
        assertTrue(turned.mirrored);
        assertEquals(house, CityEditToolOperation.rotateAround(new Point(100, 100), 4).apply(house));
        assertEquals(CityEditToolOperation.rotateAround(new Point(100, 100), 3).apply(house),
                CityEditToolOperation.rotateAround(new Point(100, 100), -1).apply(house));

        ObjectState spread = CityEditToolOperation.scaleAround(new Point2D.Double(100, 100), 2, 0.5)
                .apply(new ObjectState(CityLayer.Direction.SOUTH, false, 2, 110, 120));
        assertEquals(new ObjectState(CityLayer.Direction.SOUTH, false, 2, 120, 110), spread);
    }

    @Test
    void transformingTheSelectionRefusesToStackHouses() {
        CityLayer layer = layerWithObjects();
        CityEditToolOperation operation = new CityEditToolOperation();
        operation.setView(TEST_VIEW);
        operation.setPaint(new NibbleLayerPaint(layer));
        ObjectState first = new ObjectState(CityLayer.Direction.NORTH, false, 0, 100, 100);
        ObjectState second = new ObjectState(CityLayer.Direction.NORTH, false, 1, 110, 100);
        layer.setDataAt(TEST_DIMENSION, first.xPos, first.yPos, first);
        layer.setDataAt(TEST_DIMENSION, second.xPos, second.yPos, second);
        operation.handleClick(100, 100, false, false);
        operation.handleClick(110, 100, false, true);

        assertFalse(
                operation.transformSelection(CityEditToolOperation.scaleAround(new Point2D.Double(105, 100), 0.01, 1)));
        assertEquals(first, layer.getInformationAt(100, 100));
        assertEquals(second, layer.getInformationAt(110, 100));

        assertTrue(operation.transformSelection(CityEditToolOperation.scaleAround(new Point2D.Double(100, 100), 2, 1)));
        assertNull(layer.getInformationAt(110, 100));
        assertEquals(1, layer.getInformationAt(120, 100).objectIndex);
        assertEquals(0, layer.getInformationAt(100, 100).objectIndex);
        assertEquals(2, operation.selection().size());
    }

    @Test
    void clickSelectsOnlyTheHouseAndCtrlClickToggles() {
        CityLayer layer = layerWithObjects();
        CityEditToolOperation operation = new CityEditToolOperation();
        operation.setView(TEST_VIEW);
        operation.setPaint(new NibbleLayerPaint(layer));
        ObjectState first = new ObjectState(CityLayer.Direction.NORTH, false, 0, 100, 140);
        ObjectState second = new ObjectState(CityLayer.Direction.NORTH, false, 1, 110, 140);
        layer.setDataAt(TEST_DIMENSION, first.xPos, first.yPos, first);
        layer.setDataAt(TEST_DIMENSION, second.xPos, second.yPos, second);

        operation.handleClick(100, 140, false, false);
        operation.handleClick(110, 140, false, false);
        assertEquals(List.of(second), operation.selection());
        operation.handleClick(100, 140, false, true);
        assertEquals(2, operation.selection().size());
        operation.handleClick(110, 140, false, true);
        assertEquals(List.of(first), operation.selection());
        // a plain click on empty map clears the selection; in add mode it places a
        // house
        operation.handleClick(150, 150, false, false);
        assertTrue(operation.selection().isEmpty());
        assertNull(layer.getInformationAt(150, 150));
        operation.addModeToggle().setSelected(true);
        operation.handleClick(150, 150, false, false);
        assertNotNull(layer.getInformationAt(150, 150));
    }

    @Test
    void layerMenuListsTheDimensionsCityLayersAlphabeticallyAndSelectsThem() {
        CityLayer bravo = layerWithObjects();
        bravo.setName("bravo");
        CityLayer alpha = layerWithObjects();
        alpha.setName("Alpha");
        CityLayer charlie = layerWithObjects();
        charlie.setName("Charlie");
        var previous = TEST_DIMENSION.getCustomLayers();
        TEST_DIMENSION.setCustomLayers(List.of(charlie, bravo, alpha));
        try {
            assertEquals(List.of(alpha, bravo, charlie), CityLayerLibrary.cityLayers(TEST_DIMENSION));

            CityEditToolOperation operation = new CityEditToolOperation();
            operation.setView(TEST_VIEW);
            var names = new ArrayList<String>();
            for (var child : operation.layerMenu().getComponents())
                if (child instanceof JMenuItem item && item.getName() != null)
                    names.add(item.getName());
            assertEquals(List.of("CityTool.TutorialLayer", "CityTool.ImportLayer", "CityTool.Layer.Alpha",
                    "CityTool.Layer.bravo", "CityTool.Layer.Charlie", "CityTool.Help"), names);

            clickMenuItem(operation.layerMenu(), "CityTool.Layer.bravo");
            assertSame(bravo, ((LayerPaint) operation.getPaint()).getLayer());
            var current = new ArrayList<String>();
            for (var child : operation.layerMenu().getComponents())
                if (child instanceof JRadioButtonMenuItem item && item.isSelected()
                        && item.getName().startsWith("CityTool.Layer."))
                    current.add(item.getName());
            assertEquals(List.of("CityTool.Layer.bravo"), current);
        } finally {
            TEST_DIMENSION.setCustomLayers(previous);
        }
    }

    @Test
    void typeGridSetsTypeAndSchematicsMenuMirrorsAndSwitchesRandomOptions() {
        CityLayer layer = layerWithObjects();
        CityEditToolOperation operation = new CityEditToolOperation();
        operation.setView(TEST_VIEW);
        operation.setPaint(new NibbleLayerPaint(layer));
        ObjectState house = new ObjectState(CityLayer.Direction.NORTH, false, 0, 100, 180);
        layer.setDataAt(TEST_DIMENSION, house.xPos, house.yPos, house);
        operation.handleClick(100, 180, false, false);

        operation.openTypeGrid();
        operation.pickTypeInGrid(2);
        assertEquals(2, layer.getInformationAt(100, 180).objectIndex);
        assertFalse(operation.typeGridOpen());
        // bright mode is kept for the next time the picker opens
        operation.openTypeGrid();
        assertTrue(operation.typeGridOpen());
        assertFalse(operation.typeGridBrightToggle().isSelected());
        operation.typeGridBrightToggle().doClick();
        operation.handleKeyInteraction(KeyEvent.VK_ESCAPE);
        assertFalse(operation.typeGridOpen());
        operation.openTypeGrid();
        assertTrue(operation.typeGridBrightToggle().isSelected());
        operation.handleKeyInteraction(KeyEvent.VK_ESCAPE);
        for (var child : operation.schematicsMenu().getComponents())
            if (child instanceof JMenuItem item && item.getName() != null)
                assertFalse(item.getName().startsWith("CityTool.Type."), "the type is picked in the grid");

        clickMenuItem(operation.addMenu(), "CityTool.RandomMirroring");
        assertEquals(new CityEditToolOperation.PlacementOptions(false, false, true), operation.placementOptions());
        clickMenuItem(operation.addMenu(), "CityTool.RandomMirroring");
        assertEquals(new CityEditToolOperation.PlacementOptions(false, false, false), operation.placementOptions());

        clickMenuItem(operation.schematicsMenu(), "CityTool.Mirror");
        assertTrue(layer.getInformationAt(100, 180).mirrored);
    }

    @Test
    void groupRotationTurnsPositionsByAnyAngleAndHousesByTheNearestQuarterTurn() {
        ObjectState east = new ObjectState(CityLayer.Direction.NORTH, false, 0, 110, 100);
        ObjectState south = new ObjectState(CityLayer.Direction.WEST, true, 1, 100, 110);
        var pivot = new Point2D.Double(100, 100);

        // 30 degrees counter-clockwise on screen: positions turn, houses keep facing
        // north
        ObjectState turned = CityEditToolOperation.rotateAround(pivot, 30).apply(east);
        assertEquals(109, turned.xPos);
        assertEquals(95, turned.yPos);
        assertEquals(CityLayer.Direction.NORTH, turned.rotation);
        // 60 degrees: past 45, each house takes a quarter turn
        turned = CityEditToolOperation.rotateAround(pivot, 60).apply(south);
        assertEquals(109, turned.xPos);
        assertEquals(105, turned.yPos);
        assertEquals(CityLayer.Direction.SOUTH, turned.rotation);
        assertTrue(turned.mirrored);
        // 90 degrees matches the quarter turn of the C key
        assertEquals(CityEditToolOperation.rotateAround(new Point(100, 100), 1).apply(east),
                CityEditToolOperation.rotateAround(pivot, 90).apply(east));
        assertEquals(CityEditToolOperation.rotateAround(new Point(100, 100), -1).apply(south),
                CityEditToolOperation.rotateAround(pivot, -90).apply(south));
    }

    @Test
    void overlaySwitchesDriveTheLayersHighlightColors() {
        CityLayer layer = layerWithObjects();
        layer.setUseHighlightColors(false);
        CityEditToolOperation operation = new CityEditToolOperation();
        operation.setView(TEST_VIEW);
        operation.setPaint(new NibbleLayerPaint(layer));
        var highlight = operation.overlayParts().get(0);
        var outlines = operation.overlayParts().get(1);
        assertEquals("CityTool.Overlays.HighlightColors", highlight.getName());
        assertEquals("CityTool.Overlays.Outlines", outlines.getName());
        assertEquals("CityTool.Overlays.Facing", operation.overlayParts().get(2).getName());
        assertFalse(highlight.isSelected(), "the switch shows the layer's setting");

        highlight.setSelected(true);
        assertTrue(layer.isUseHighlightColors());
        operation.overlaysSwitch().setSelected(false);
        assertFalse(layer.isUseHighlightColors(), "all overlays off: the schematics' blocks show");
        operation.overlaysSwitch().setSelected(true);
        assertTrue(layer.isUseHighlightColors());
        outlines.setSelected(false);
        assertTrue(layer.isUseHighlightColors());
    }

    @Test
    void housesTurnTheSameWayAsTheirPositions() {
        // a two block house: stone in the west, dirt in the east
        CityLayer layer = new CityLayer("turn", "test");
        var objects = new ArrayList<WPObject>();
        objects.add(new GenericObject("house", 2, 1, 1, new Material[]{Material.STONE, Material.DIRT}));
        layer.setObjectList(objects);
        ObjectState house = new ObjectState(CityLayer.Direction.NORTH, false, 0, 10, 0);

        // a quarter turn counter-clockwise on screen: east of the pivot moves north ...
        ObjectState turned = CityEditToolOperation.rotateStateAround(house, new Point(0, 0));
        assertEquals(new Point(0, -10), new Point(turned.xPos, turned.yPos));
        // ... and the house's east end turns to the north as well
        WPObject object = layer.getObjectForState(turned);
        assertEquals(1, object.getDimensions().x);
        assertEquals(2, object.getDimensions().y);
        assertEquals(Material.DIRT, object.getMaterial(0, 0, 0));
        assertEquals(Material.STONE, object.getMaterial(0, 1, 0));
        // a group turned by 90 degrees agrees with the quarter turn
        assertEquals(turned, CityEditToolOperation.rotateAround(new Point2D.Double(0, 0), 90).apply(house));
    }

    @Test
    void theTransformBoxKeepsTheAngleTheGroupWasTurnedBy() {
        CityLayer layer = layerWithObjects();
        CityEditToolOperation operation = new CityEditToolOperation();
        operation.setView(TEST_VIEW);
        operation.setPaint(new NibbleLayerPaint(layer));
        layer.setDataAt(TEST_DIMENSION, 100, 220, new ObjectState(CityLayer.Direction.NORTH, false, 0, 100, 220));
        layer.setDataAt(TEST_DIMENSION, 120, 220, new ObjectState(CityLayer.Direction.NORTH, false, 1, 120, 220));
        operation.handleClick(100, 220, false, false);
        operation.handleClick(120, 220, false, true);
        assertEquals(0, operation.boxAngle());

        operation.handleKeyInteraction(KeyEvent.VK_C);
        assertEquals(90, operation.boxAngle());
        // moving keeps the angle, a new selection starts upright
        operation.handleKeyInteraction(KeyEvent.VK_D);
        assertEquals(90, operation.boxAngle());
        operation.handleKeyInteraction(KeyEvent.VK_ESCAPE);
        operation.handleKeyInteraction(KeyEvent.VK_A, true);
        assertEquals(0, operation.boxAngle());
    }

    @Test
    void aClickOnAPickerTileCountsEvenWhenTheMouseMovesAPixel() {
        CityLayer layer = layerWithObjects();
        CityEditToolOperation operation = new CityEditToolOperation();
        operation.setView(TEST_VIEW);
        operation.setPaint(new NibbleLayerPaint(layer));
        operation.openTypeGrid();
        var grid = operation.typeGridComponent();
        grid.setSize(1000, 800);
        // the first tile sits top left below the header; press on it, release two
        // pixels away
        int x = grid.getWidth() / 2, y = 120;
        Point press = null;
        for (int px = 0; px < grid.getWidth() && press == null; px += 4)
            if (grid.getComponentAt(px, y) == grid)
                press = new Point(px + 60, y);
        long when = System.currentTimeMillis();
        grid.dispatchEvent(new java.awt.event.MouseEvent(grid, java.awt.event.MouseEvent.MOUSE_PRESSED, when,
                java.awt.event.InputEvent.BUTTON1_DOWN_MASK, press.x, press.y, 1, false,
                java.awt.event.MouseEvent.BUTTON1));
        grid.dispatchEvent(new java.awt.event.MouseEvent(grid, java.awt.event.MouseEvent.MOUSE_RELEASED, when + 80, 0,
                press.x + 2, press.y + 1, 1, false, java.awt.event.MouseEvent.BUTTON1));
        assertFalse(operation.typeGridOpen(), "the tile was picked and the picker closed");
    }

    @Test
    void addRandomPlacesARandomPickOfTheTickedSchematics() {
        CityLayer layer = layerWithObjects();
        CityEditToolOperation operation = new CityEditToolOperation();
        operation.setView(TEST_VIEW);
        operation.setPaint(new NibbleLayerPaint(layer));
        operation.random = new Random(42);

        clickMenuItem(operation.addMenu(), "CityTool.Add.Random");
        assertTrue(operation.addRandom());
        assertTrue(operation.addModeToggle().isSelected(), "choosing Add random turns add mode on");
        assertTrue(operation.pickingRandomPool());

        // nothing ticked: nothing is placed
        operation.placeAt(100, 260);
        assertNull(layer.getInformationAt(100, 260));

        operation.toggleRandomPool(1);
        operation.toggleRandomPool(2);
        operation.toggleRandomPool(0);
        operation.toggleRandomPool(0); // unticked again
        assertEquals(Set.of(1, 2), operation.randomPool());
        var placed = new java.util.HashSet<Integer>();
        for (int i = 0; i < 20; i++) {
            operation.placeAt(100 + 10 * i, 260);
            placed.add(layer.getInformationAt(100 + 10 * i, 260).objectIndex);
        }
        assertEquals(Set.of(1, 2), placed, "only the ticked schematics, both of them");

        // plain Add places the current type again; no checkboxes outside add mode
        clickMenuItem(operation.addMenu(), "CityTool.Add.Plain");
        assertFalse(operation.addRandom());
        assertFalse(operation.pickingRandomPool());
        operation.placeAt(100, 290);
        assertEquals(0, layer.getInformationAt(100, 290).objectIndex);
        operation.setAddRandom(true);
        operation.addModeToggle().setSelected(false);
        assertFalse(operation.pickingRandomPool(), "the checkboxes only matter in add mode");
    }

    @Test
    void inAddRandomAClickOnAPickerTileTicksIt() {
        CityLayer layer = layerWithObjects();
        CityEditToolOperation operation = new CityEditToolOperation();
        operation.setView(TEST_VIEW);
        operation.setPaint(new NibbleLayerPaint(layer));
        operation.setAddRandom(true);
        operation.openTypeGrid();
        var grid = operation.typeGridComponent();
        grid.setSize(1000, 800);
        // 1000 px wide: five tiles of 150 px with 12 px gaps, centred: the first starts
        // at x 101,
        // y 84; its checkbox is 6 px in, 22 px wide
        pressAndRelease(grid, new Point(118, 101), new Point(119, 101));
        assertTrue(operation.typeGridOpen(), "ticking keeps the picker open");
        assertEquals(Set.of(0), operation.randomPool());
        // a click on the picture counts as a click on the checkbox: unticks, picker
        // stays open
        pressAndRelease(grid, new Point(176, 150), new Point(176, 150));
        assertTrue(operation.typeGridOpen());
        assertEquals(Set.of(), operation.randomPool());
        // outside add mode a click on a tile picks it again
        operation.addModeToggle().setSelected(false);
        pressAndRelease(grid, new Point(176, 150), new Point(176, 150));
        assertFalse(operation.typeGridOpen());
    }

    private static Point pressAndRelease(javax.swing.JComponent grid, Point press, Point release) {
        long when = System.currentTimeMillis();
        grid.dispatchEvent(new java.awt.event.MouseEvent(grid, java.awt.event.MouseEvent.MOUSE_PRESSED, when,
                java.awt.event.InputEvent.BUTTON1_DOWN_MASK, press.x, press.y, 1, false,
                java.awt.event.MouseEvent.BUTTON1));
        grid.dispatchEvent(new java.awt.event.MouseEvent(grid, java.awt.event.MouseEvent.MOUSE_RELEASED, when + 50, 0,
                release.x, release.y, 1, false, java.awt.event.MouseEvent.BUTTON1));
        return press;
    }

    @Test
    void pickerTileNamesBreakIntoLinesInsteadOfBeingCutOff() {
        var metrics = new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB)
                .createGraphics()
                .getFontMetrics(new Font(Font.DIALOG, Font.BOLD, 13));
        String name = "Ektelion - Ukrainian house 1-converted";
        var lines = CityEditToolOperation.wrapForTest(name, metrics, 134);
        assertTrue(lines.size() > 1);
        assertEquals(name.replace(" ", ""), String.join("", lines).replace(" ", ""), "nothing left out");
        for (String line : lines)
            assertTrue(metrics.stringWidth(line) <= 134, line);
        // a word longer than a line is broken too
        var long_ = CityEditToolOperation.wrapForTest("Supercalifragilisticexpialidocious-house-without-spaces",
                metrics, 134);
        assertTrue(long_.size() > 1);
        assertEquals("Supercalifragilisticexpialidocious-house-without-spaces", String.join("", long_));
    }

    @Test
    void randomizePanelsPickOnlyAllowedFinalStates() {
        CityLayer layer = layerWithObjects();
        CityEditToolOperation operation = new CityEditToolOperation();
        operation.setView(TEST_VIEW);
        operation.setPaint(new NibbleLayerPaint(layer));
        for (int i = 0; i < 12; i++)
            layer.setDataAt(TEST_DIMENSION, 100 + 10 * i, 320,
                    new ObjectState(CityLayer.Direction.NORTH, false, 0, 100 + 10 * i, 320));
        operation.handleKeyInteraction(KeyEvent.VK_A, true);
        assertEquals(12, operation.selection().size());

        // rotation: only 90 and 270 degrees allowed
        clickMenuItem(operation.schematicsMenu(), "CityTool.RandomizeRotation");
        var panel = operation.operationPanel();
        assertNotNull(panel);
        var description = (javax.swing.JLabel) OperationPanelLookup.find(panel, "CityTool.OperationDescription");
        assertTrue(description.getText().contains("ticked rotations"), "the panel says what Apply does");
        clickNamed(panel, "CityTool.Randomize.Allowed.0"); // untick 0 degrees
        clickNamed(panel, "CityTool.Randomize.Allowed.2"); // untick 180 degrees
        clickNamed(panel, "CityTool.OperationApply");
        assertNull(operation.operationPanel());
        var rotations = new java.util.HashSet<CityLayer.Direction>();
        for (ObjectState house : operation.selection())
            rotations.add(house.rotation);
        assertEquals(Set.of(CityLayer.Direction.EAST, CityLayer.Direction.WEST), rotations);

        // type: only schematic 2; nothing ticked disables Apply
        clickMenuItem(operation.schematicsMenu(), "CityTool.RandomizeType");
        panel = operation.operationPanel();
        clickNamed(panel, "CityTool.Randomize.DeselectAll");
        for (int i = 0; i < 3; i++)
            assertFalse(((javax.swing.JCheckBox) OperationPanelLookup.find(panel, "CityTool.Randomize.Allowed." + i))
                    .isSelected());
        assertFalse(OperationPanelLookup.find(panel, "CityTool.OperationApply").isEnabled());
        clickNamed(panel, "CityTool.Randomize.SelectAll");
        assertTrue(OperationPanelLookup.find(panel, "CityTool.OperationApply").isEnabled());
        clickNamed(panel, "CityTool.Randomize.DeselectAll");
        clickNamed(panel, "CityTool.Randomize.Allowed.2");
        clickNamed(panel, "CityTool.OperationApply");
        for (ObjectState house : operation.selection())
            assertEquals(2, house.objectIndex);

        // mirroring: Cancel changes nothing
        clickMenuItem(operation.schematicsMenu(), "CityTool.RandomizeMirroring");
        clickNamed(operation.operationPanel(), "CityTool.Randomize.Allowed.0"); // only mirrored
        clickNamed(operation.operationPanel(), "CityTool.OperationCancel");
        for (ObjectState house : operation.selection())
            assertFalse(house.mirrored);
    }

    private static final class OperationPanelLookup
    {
        static java.awt.Component find(java.awt.Container container, String name) {
            return org.ironsight.wpplugin.macromachine.Gui.OperationPanel.find(container, name);
        }
    }

    private static void clickNamed(java.awt.Container container, String name) {
        var component = OperationPanelLookup.find(container, name);
        assertNotNull(component, name);
        ((javax.swing.AbstractButton) component).doClick();
    }

    @Test
    void theFacingArrowPointsAlongTheHousesForwardAxis() {
        var bounds = new java.awt.geom.Rectangle2D.Double(0, 0, 40, 40);
        // a ">" outside the front edge: its tip 8 px out from the edge's middle
        assertEquals(new Point2D.Double(20, -8),
                tip(CityEditToolOperation.facingArrow(bounds, CityLayer.Direction.NORTH)));
        assertEquals(new Point2D.Double(48, 20),
                tip(CityEditToolOperation.facingArrow(bounds, CityLayer.Direction.EAST)));
        assertEquals(new Point2D.Double(20, 48),
                tip(CityEditToolOperation.facingArrow(bounds, CityLayer.Direction.SOUTH)));
        assertEquals(new Point2D.Double(-8, 20),
                tip(CityEditToolOperation.facingArrow(bounds, CityLayer.Direction.WEST)));
        // its arms start on the edge itself (attached to the box)
        var arms = points(CityEditToolOperation.facingArrow(bounds, CityLayer.Direction.EAST));
        assertEquals(40, arms.get(0).x);
        assertEquals(40, arms.get(2).x);
        assertNull(CityEditToolOperation.facingArrow(new java.awt.geom.Rectangle2D.Double(0, 0, 4, 30),
                CityLayer.Direction.NORTH), "too small for an arrow");
    }

    /** The chevron's tip: its middle point. */
    private static Point2D.Double tip(java.awt.geom.Path2D.Double arrow) {
        return points(arrow).get(1);
    }

    private static List<Point2D.Double> points(java.awt.geom.Path2D.Double arrow) {
        var points = new ArrayList<Point2D.Double>();
        var coordinates = new double[6];
        for (var it = arrow.getPathIterator(null); !it.isDone(); it.next()) {
            it.currentSegment(coordinates);
            points.add(new Point2D.Double(coordinates[0], coordinates[1]));
        }
        return points;
    }

    private static void clickMenuItem(JPopupMenu menu, String name) {
        for (var child : menu.getComponents())
            if (child instanceof JMenuItem item && name.equals(item.getName())) {
                item.doClick();
                return;
            }
        fail("no menu item " + name);
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

    private static class TestView extends WorldPainterView
    {
        private Dimension dimension;

        TestView(Dimension dimension) {
            this.dimension = dimension;
        }

        @Override
        public Dimension getDimension() {
            return dimension;
        }

        @Override
        public void setDimension(Dimension dimension) {
            this.dimension = dimension;
        }

        @Override
        public void updateStatusBar(int x, int y) {
        }

        @Override
        public boolean isDrawBrush() {
            return false;
        }

        @Override
        public void setDrawBrush(boolean drawBrush) {
        }

        @Override
        public MapDragControl getMapDragControl() {
            return null;
        }

        @Override
        public BrushControl getBrushControl() {
            return new BrushControl() {
                @Override
                public int getRadius() {
                    return 0;
                }

                @Override
                public void increaseRadius(int amount) {
                }

                @Override
                public void increaseRadiusByOne() {
                }

                @Override
                public void decreaseRadius(int amount) {
                }

                @Override
                public void decreaseRadiusByOne() {
                }

                @Override
                public void setRadius(int radius) {
                }

                @Override
                public int getRotation() {
                    return 0;
                }

                @Override
                public void setRotation(int rotation) {
                }
            };
        }

        @Override
        public RadiusControl getRadiusControl() {
            return new RadiusControl() {
                @Override
                public void increaseRadius(int amount) {
                }

                @Override
                public void increaseRadiusByOne() {
                }

                @Override
                public void decreaseRadius(int amount) {
                }

                @Override
                public void decreaseRadiusByOne() {
                }
            };
        }
    }
}
