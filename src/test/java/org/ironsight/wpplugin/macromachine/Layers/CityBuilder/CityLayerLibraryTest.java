package org.ironsight.wpplugin.macromachine.Layers.CityBuilder;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.pepsoft.worldpainter.objects.WPObject;

class CityLayerLibraryTest
{
    @Test
    void theTutorialLayerIsBundledAndLoadsAsACityLayerWithItsSchematics() throws Exception {
        assertTrue(CityLayerLibrary.hasTutorialLayer());
        CityLayer layer = CityLayerLibrary.readTutorialLayer();
        assertNotNull(layer.getName());
        assertFalse(layer.getObjectList().isEmpty(), "the schematics travel inside the layer file");
        for (WPObject object : layer.getObjectList())
            assertTrue(object.getDimensions().x > 0, object.getName());
        // a new copy each time, so importing it never shares state with an earlier
        // import
        assertNotSame(layer, CityLayerLibrary.readTutorialLayer());
        System.out.println("tutorial layer: " + layer.getName() + ", " + layer.getObjectList().size() + " schematics");
    }

    @Test
    void pressingTutorialLayerAgainSelectsTheOneAlreadyThereEvenAfterRenaming() throws Exception {
        var dimension = org.ironsight.wpplugin.macromachine.operations.ValueProviders.TestDimension.createDimension(
                new org.ironsight.wpplugin.macromachine.operations.ValueProviders.TestDimension.DimensionParams(
                        new java.awt.Rectangle(64, 64), -64, 320, 70, 1, 62,
                        org.pepsoft.worldpainter.DefaultPlugin.JAVA_ANVIL_1_19,
                        org.pepsoft.worldpainter.Terrain.GRASS));
        CityLayer imported = CityLayerLibrary.readTutorialLayer();
        dimension.setCustomLayers(java.util.List.of(imported));

        // no WorldPainter GUI here: anything but the present layer would mean a second
        // import
        assertSame(imported, CityLayerLibrary.addTutorialLayer(dimension));
        imported.setName("My village");
        assertSame(imported, CityLayerLibrary.addTutorialLayer(dimension), "found by its layer id after renaming");
        assertEquals(1, CityLayerLibrary.cityLayers(dimension).size());
    }
}
