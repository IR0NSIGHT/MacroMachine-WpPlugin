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
        // a new copy each time, so importing it never shares state with an earlier import
        assertNotSame(layer, CityLayerLibrary.readTutorialLayer());
        System.out.println("tutorial layer: " + layer.getName() + ", " + layer.getObjectList().size() + " schematics");
    }
}
