package org.ironsight.wpplugin.macromachine.Layers.CityBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.ArrayList;
import java.lang.reflect.Field;
import javax.vecmath.Point3i;

import org.junit.jupiter.api.Test;
import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.objects.GenericObject;
import org.pepsoft.worldpainter.objects.MirroredObject;
import org.pepsoft.worldpainter.objects.RotatedObject;
import org.pepsoft.worldpainter.objects.WPObject;

class CityLayerTest
{
    @Test
    void directionConversionsRoundTripAndCycle() {
        for (CityLayer.Direction direction : CityLayer.Direction.values()) {
            assertEquals(direction, CityLayer.Direction.fromCompass(direction.toCompass()));
            assertEquals(direction, direction.nextRotation().nextRotation().nextRotation().nextRotation());
        }

        assertEquals(CityLayer.Direction.NORTH, CityLayer.Direction.fromCompass(45));
        assertEquals(CityLayer.Direction.EAST, CityLayer.Direction.fromCompass(46));
        assertEquals(CityLayer.Direction.EAST, CityLayer.Direction.fromCompass(135));
        assertEquals(CityLayer.Direction.SOUTH, CityLayer.Direction.fromCompass(136));
        assertEquals(CityLayer.Direction.WEST, CityLayer.Direction.fromCompass(226));
        assertEquals(CityLayer.Direction.NORTH, CityLayer.Direction.fromCompass(316));
    }

    @Test
    void objectLookupHandlesNullAndInvalidIndices() {
        CityLayer layer = layerWithObjects();

        assertNull(layer.getObjectForState(null));
        assertNull(layer.getObjectForState(new ObjectState(CityLayer.Direction.NORTH, false, -1, 0, 0)));
        assertNull(layer.getObjectForState(new ObjectState(CityLayer.Direction.NORTH, false, 2, 0, 0)));
        assertDoesNotThrow(() -> layer.setSelected(new ObjectState(CityLayer.Direction.NORTH, false, 2, 0, 0)));
    }

    @Test
    void encodedValuesRoundTripThroughInformationLookup() throws ReflectiveOperationException {
        CityLayer layer = layerWithObjects();
        Field databaseField = CityLayer.class.getDeclaredField("database");
        databaseField.setAccessible(true);
        CityInfoDatabase database = (CityInfoDatabase) databaseField.get(layer);

        for (CityLayer.Direction direction : CityLayer.Direction.values()) {
            for (boolean mirrored : new boolean[]{false, true}) {
                int value = direction.ordinal() << CityLayer.ROTATION_BIT_SHIFT;
                value |= (mirrored ? 1 : 0) << CityLayer.MIRROR_BIT_SHIFT;
                value |= 0 << CityLayer.ID_BIT_SHIFT;
                database.setDataAt(-33, 65, value);

                ObjectState state = layer.getInformationAt(-33, 65);
                assertEquals(direction, state.rotation);
                assertEquals(mirrored, state.mirrored);
                assertEquals(0, state.objectIndex);
                assertEquals(-33, state.xPos);
                assertEquals(65, state.yPos);
            }
        }
    }

    @Test
    void objectLookupAppliesMirrorAndRotation() {
        CityLayer layer = layerWithObjects();
        WPObject original = layer.getObjectList().get(0);

        assertSame(original, layer.getObjectForState(state(0, false)));
        assertEquals(MirroredObject.class, layer.getObjectForState(state(0, true)).getClass());
        assertEquals(RotatedObject.class,
                layer.getObjectForState(new ObjectState(CityLayer.Direction.EAST, false, 0, 0, 0)).getClass());
        assertEquals(RotatedObject.class,
                layer.getObjectForState(new ObjectState(CityLayer.Direction.WEST, true, 0, 0, 0)).getClass());
    }

    @Test
    void schematicImageIncludesPaddingAndUsesTransformedDimensions() {
        CityLayer layer = layerWithObjects();

        var north = layer.getSchematicImage(state(0, false));
        var east = layer.getSchematicImage(new ObjectState(CityLayer.Direction.EAST, false, 0, 0, 0));

        assertNotNull(north);
        assertNotNull(east);
        assertEquals(2 + 6, north.getWidth(null));
        assertEquals(1 + 6, north.getHeight(null));
        assertEquals(1 + 6, east.getWidth(null));
        assertEquals(2 + 6, east.getHeight(null));
    }

    @Test
    void rendererConfigurationIsRetained() {
        CityLayer layer = layerWithObjects();
        layer.setUseHighlightColors(false);
        layer.setIsSelectedPaint(true);

        assertEquals(false, layer.isUseHighlightColors());
        assertNotNull(layer.getRenderer());
    }

    @Test
    void highlightColorsAreEnabledByDefault() {
        CityLayer layer = layerWithObjects();

        assertEquals(true, layer.isUseHighlightColors());
        CityLayerRenderer renderer = (CityLayerRenderer) layer.getRenderer();
        assertEquals(0x123456, renderer.getPixelColour(0, 0, 0x123456, 0));
        assertEquals(0x00FFFF, renderer.getPixelColour(0, 0, 0x123456, 15));
    }

    private static CityLayer layerWithObjects() {
        CityLayer layer = new CityLayer("test", "test");
        ArrayList<WPObject> objects = new ArrayList<>();
        objects.add(new GenericObject("building", 2, 1, 1, new Material[]{Material.STONE, Material.STONE}));
        layer.setObjectList(objects);
        return layer;
    }

    private static ObjectState state(int index, boolean mirrored) {
        return new ObjectState(CityLayer.Direction.NORTH, mirrored, index, 0, 0);
    }
}
