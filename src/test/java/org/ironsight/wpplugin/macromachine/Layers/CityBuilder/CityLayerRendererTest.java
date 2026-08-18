package org.ironsight.wpplugin.macromachine.Layers.CityBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CityLayerRendererTest
{
    @Test
    void highlightModeLeavesEmptyCellsUnderlying() {
        CityLayerRenderer renderer = new CityLayerRenderer(new CityLayer("test", "test"));
        renderer.setUseHighlightColors(true);
        renderer.setBaseColor(0xFF0000);

        assertEquals(0x123456, renderer.getPixelColour(0, 0, 0x123456, 0));
    }

    @Test
    void highlightModeInterpolatesEachChannel() {
        CityLayerRenderer renderer = new CityLayerRenderer(new CityLayer("test", "test"));
        renderer.setUseHighlightColors(true);
        renderer.setBaseColor(0xFF0000);

        assertEquals(0x901828, renderer.getPixelColour(0, 0, 0x123456, 8));
        assertEquals(0xFF0000, renderer.getPixelColour(0, 0, 0x123456, 15));
    }

    @Test
    void selectionOverlayOnlyAppliesInsideSelectionBounds() {
        CityLayerRenderer renderer = new CityLayerRenderer(new CityLayer("test", "test"));
        renderer.setUseHighlightColors(true);
        renderer.setBaseColor(0x101010);
        renderer.setIsSelectedPaint(true);
        renderer.setCurrentSelectBBX(new java.awt.Rectangle(2, 3, 4, 5));

        assertEquals(0x101010, renderer.getPixelColour(0, 0, 0x101010, 15));
        assertEquals(0x9F0000, renderer.getPixelColour(2, 3, 0x101010, 15));
    }
}
