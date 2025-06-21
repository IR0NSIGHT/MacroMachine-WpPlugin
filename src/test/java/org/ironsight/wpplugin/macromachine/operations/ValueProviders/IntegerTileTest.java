package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE;

class IntegerTileTest {
    @Test
    void construct() {
        IntegerTile tile = new IntegerTile(67);
        assertEquals(67,tile.getMin());
        assertEquals(67, tile.getMax());
    }
    @Test
    void setValueAt() {
        IntegerTile tile = new IntegerTile(7);
        for (int x = 0; x < TILE_SIZE; x++) {
            for (int y = 0; y < TILE_SIZE; y++) {
                assertEquals(7, tile.getValueAt(x, y));
            }
        }
        tile.setValueAt(3, 9, 17);
        assertEquals(17, tile.getValueAt(3, 9));
    }

    @Test
    void calculateMinMax() {
        IntegerTile tile = new IntegerTile(17);
        tile.calculateMinMax();
        assertEquals(17, tile.getMin());
        assertEquals(17, tile.getMax());

        tile.setValueAt(17,123,1234567);
        tile.setValueAt(1,2,-123456789);
        tile.calculateMinMax();
        assertEquals(-123456789, tile.getMin());
        assertEquals(1234567, tile.getMax());
    }

    @Test
    void fillWithValue() {
        IntegerTile tile = new IntegerTile(17);
        tile.fillWith(-1978);
        tile.calculateMinMax();
        assertEquals(-1978, tile.getMin());
        assertEquals(-1978, tile.getMax());
    }
}