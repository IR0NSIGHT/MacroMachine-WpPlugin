package org.ironsight.wpplugin.macromachine.Layers.PathBuilder;

import static org.junit.jupiter.api.Assertions.*;

import javax.swing.SwingUtilities;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.ActionFilterIO;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.BitLayerBinarySpraypaintApplicator;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IntermediateValueIO;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.StonePaletteApplicator;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.TerrainProvider;
import org.junit.jupiter.api.Test;
import org.pepsoft.worldpainter.layers.Frost;

class OutputPaintRowTest
{
    @Test
    void offersOnlyOutputsThatWriteIntoTheWorld() {
        assertTrue(OutputPaintRow.paintable(new TerrainProvider()));
        assertFalse(OutputPaintRow.paintable(new ActionFilterIO()), "the action filter only steers actions");
        assertFalse(OutputPaintRow.paintable(new IntermediateValueIO()), "intermediate values only steer actions");
        assertFalse(OutputPaintRow.paintable(new StonePaletteApplicator()), "no stone palette");
    }

    @Test
    void spraypaintOffersItsPercentagesAndKeepsAValueSelected() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var spraypaint = new BitLayerBinarySpraypaintApplicator(Frost.INSTANCE, false);
            // it lists no output values of its own: the dropdown falls back to 0 - 100 %
            assertEquals(101, OutputPaintRow.paintValues(spraypaint).length);
            var row = new OutputPaintRow("Paint riverbed with", "", () -> {
            });
            row.set(new PathTool.OutputPaint(true, spraypaint, 12345)); // a value it does not have
            assertEquals(101, row.valueDropdown().getItemCount());
            assertNotNull(row.valueDropdown().getSelectedItem(), "never an empty selection");
            assertEquals(0, row.get().value());
            row.set(new PathTool.OutputPaint(true, spraypaint, 40));
            assertEquals(40, row.get().value());
        });
    }
}
