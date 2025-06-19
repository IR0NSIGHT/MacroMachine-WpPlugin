package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import org.ironsight.wpplugin.macromachine.operations.TestData;
import org.junit.jupiter.api.Test;
import org.pepsoft.worldpainter.Dimension;

import javax.swing.*;
import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.pepsoft.util.swing.TiledImageViewer.TILE_SIZE;

class ActionFilterIOTest {

    @Test
    void setValueAt() {
        Dimension dim = TestData.createDimension(new Rectangle(-2 * TILE_SIZE,
                -2 * TILE_SIZE,
                3 * TILE_SIZE,
                3 * TILE_SIZE), 0);
        ActionFilterIO actionFilter = new ActionFilterIO();
        actionFilter.prepareForDimension(dim);

        assertEquals(ActionFilterIO.PASS_VALUE, actionFilter.getValueAt(dim,7,-177));

        //mutate a value
        actionFilter.setValueAt(dim,7,-177, ActionFilterIO.PASS_VALUE);
        assertEquals(ActionFilterIO.PASS_VALUE, actionFilter.getValueAt(dim,7,-177));

    }
}