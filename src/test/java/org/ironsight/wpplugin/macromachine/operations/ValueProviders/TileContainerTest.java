package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import org.junit.jupiter.api.Test;
import org.pepsoft.worldpainter.Constants;

import static org.junit.jupiter.api.Assertions.*;

class TileContainerTest {

    @Test
    void setValueAt() {
        TileContainer container = new TileContainer(12000 / Constants.TILE_SIZE,17000 / Constants.TILE_SIZE,-1000,
                -5555, 0);
        container.setValueAt(0,0,17);
        assertEquals(17, container.getValueAt(0,0));

        container.setValueAt(19,45,-3);
        assertEquals(-3, container.getValueAt(19,45));
    }
}