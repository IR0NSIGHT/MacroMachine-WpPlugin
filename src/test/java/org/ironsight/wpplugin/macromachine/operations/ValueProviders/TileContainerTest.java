package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import org.junit.jupiter.api.Test;
import org.pepsoft.worldpainter.Constants;

import static org.junit.jupiter.api.Assertions.*;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE;

class TileContainerTest {

    @Test
    void setValueAt() {
        TileContainer container = new TileContainer(12000 / TILE_SIZE,17000 / TILE_SIZE,-1000,
                -5555, 0);
        container.setValueAt(0,0,17);
        assertEquals(17, container.getValueAt(0,0));

        container.setValueAt(19,45,-3);
        assertEquals(-3, container.getValueAt(19,45));

        // attempt to set a value outside the extent
        container.setValueAt(10000000,10000000,42069);
        assertEquals(Integer.MAX_VALUE, container.getValueAt(10000000,10000000));
    }

    @Test
    void getMinMaxXY() {
        int width = 93;
        int height = 132;
        TileContainer container = new TileContainer(width, height,-1000,
                -5555, 0);
        assertEquals(-1000,container.getMinXPos());
        assertEquals(-5555, container.getMinYPos());
        assertEquals(-1000 + 93 * TILE_SIZE, container.getMaxXPos());
        assertEquals(-5555 + 132 * TILE_SIZE, container.getMaxYPos());
    }
}