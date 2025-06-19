package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE;

class FlagTileTest {

    @Test
    void setValueAt() {
        IntegerTile tile = new IntegerTile(7);
        for (int x = 0; x < TILE_SIZE; x++) {
            for (int y = 0; y < TILE_SIZE; y++) {
                assertEquals(7,tile.getValueAt(x,y));
            }
        }
        tile.setValueAt(3,9,17);
        assertEquals(17, tile.getValueAt(3,9));
    }
}