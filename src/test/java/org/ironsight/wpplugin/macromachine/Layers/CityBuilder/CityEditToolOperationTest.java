package org.ironsight.wpplugin.macromachine.Layers.CityBuilder;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Random;

import org.ironsight.wpplugin.macromachine.operations.ValueProviders.TestDimension;
import org.junit.jupiter.api.Test;
import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.brushes.SymmetricBrush;
import org.pepsoft.worldpainter.objects.GenericObject;
import org.pepsoft.worldpainter.objects.WPObject;
import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.painting.NibbleLayerPaint;

import javax.swing.*;

import static org.junit.jupiter.api.Assertions.*;

class CityEditToolOperationTest
{

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
        op.setBrush(SymmetricBrush.CONSTANT_SQUARE);
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
    void randomisationIsAppliedOnlyWhenRequestedAndMovementKeepsObjectType() throws Exception {
        CityLayer layer = layerWithObjects();
        Dimension dimension = TestDimension.createDimension(new TestDimension.DimensionParams(new Rectangle(500, 500),
                -256, 512, 70, 123456789, 62, org.pepsoft.worldpainter.DefaultPlugin.JAVA_ANVIL_1_19,
                org.pepsoft.worldpainter.Terrain.GRASS));
        TestView view = new TestView(dimension);
        CityEditToolOperation operation = new CityEditToolOperation();
        operation.setView(view);
        operation.setBrush(SymmetricBrush.CONSTANT_SQUARE);
        operation.setPaint(new NibbleLayerPaint(layer));
        operation.random = new Random(1234);
        operation.setPlacementOptions(new CityEditToolOperation.PlacementOptions(false, true, false));

        operation.placeAt(100, 100);

        ObjectState placedState = layer.getInformationAt(100, 100);
        assertNotNull(placedState);
        assertEquals(0, placedState.objectIndex);

        operation.onMouseWheel(1);
        ObjectState wheelSelectedState = layer.getInformationAt(100, 100);
        assertNotNull(wheelSelectedState);
        assertEquals(1, wheelSelectedState.objectIndex);

        operation.handleKeyInteraction(KeyEvent.VK_Q);
        ObjectState randomisedState = layer.getInformationAt(100, 100);
        assertNotNull(randomisedState);
        assertEquals(2, randomisedState.objectIndex);

        operation.handleKeyInteraction(KeyEvent.VK_W);
        assertNull(layer.getInformationAt(100, 100));
        ObjectState movedState = layer.getInformationAt(100, 99);
        assertNotNull(movedState);
        assertEquals(randomisedState.objectIndex, movedState.objectIndex);

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
        operation.handleClick(200, 200, false, false);
        operation.handleClick(201, 200, true, false);
        assertNull(layer.getInformationAt(200, 200));
        assertNotNull(layer.getInformationAt(201, 200));

        operation.handleKeyInteraction(KeyEvent.VK_DELETE);
        assertNull(layer.getInformationAt(201, 200));
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
