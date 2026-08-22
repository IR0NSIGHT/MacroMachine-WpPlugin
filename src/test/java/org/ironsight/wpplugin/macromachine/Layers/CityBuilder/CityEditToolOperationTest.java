package org.ironsight.wpplugin.macromachine.Layers.CityBuilder;

import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Random;

import org.ironsight.wpplugin.macromachine.operations.ValueProviders.TestDimension;
import org.junit.jupiter.api.Test;
import org.pepsoft.worldpainter.BrushControl;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.MapDragControl;
import org.pepsoft.worldpainter.RadiusControl;
import org.pepsoft.worldpainter.WorldPainterView;
import org.pepsoft.worldpainter.brushes.SymmetricBrush;
import org.pepsoft.worldpainter.objects.GenericObject;
import org.pepsoft.worldpainter.objects.WPObject;
import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.painting.NibbleLayerPaint;

import static org.junit.jupiter.api.Assertions.*;

class CityEditToolOperationTest
{
    @Test
    void directKeyboardFlowMovesAndTransformsRandomisedPlacement() throws Exception {
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
        operation.setPlacementOptions(new CityEditToolOperation.PlacementOptions(true, true, true));
        assertTrue(operation.isRandomRotateCheckBox.isSelected());
        assertTrue(operation.isRandomSelectCheckBox.isSelected());
        assertTrue(operation.isRandomMirroredCheckbox.isSelected());


        operation.placeAt(100, 100);

        assertEquals(new ObjectState(CityLayer.Direction.NORTH, false, 0, 100, 100),
                layer.getInformationAt(100, 100));

        operation.handleKeyInteraction(KeyEvent.VK_W);
        assertNull(layer.getInformationAt(100, 100));
        assertEquals(new ObjectState(CityLayer.Direction.SOUTH, true, 2, 100, 99),
                layer.getInformationAt(100, 99));

        operation.handleKeyInteraction(KeyEvent.VK_A);
        operation.handleKeyInteraction(KeyEvent.VK_S);
        operation.handleKeyInteraction(KeyEvent.VK_D);
        operation.handleKeyInteraction(KeyEvent.VK_C);
        operation.handleKeyInteraction(KeyEvent.VK_X);

        assertEquals(new ObjectState(CityLayer.Direction.WEST, false, 2, 100, 100),
                layer.getInformationAt(100, 100));
        assertNull(layer.getInformationAt(100, 99));
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
