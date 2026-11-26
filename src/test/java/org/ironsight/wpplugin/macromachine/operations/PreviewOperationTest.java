package org.ironsight.wpplugin.macromachine.operations;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.awt.Rectangle;
import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import javax.swing.SwingUtilities;
import org.ironsight.wpplugin.macromachine.Gui.MacroMachineWindow;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.TestDimension;
import org.ironsight.wpplugin.macromachine.threeDRendering.CubeArrayRenderer.RenderOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Timeout;
import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.BrushControl;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.MapDragControl;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.WorldPainterView;
import org.pepsoft.worldpainter.brushes.SymmetricBrush;
import org.pepsoft.worldpainter.objects.GenericObject;
import org.pepsoft.worldpainter.objects.WPObject;

@Timeout(10)
class PreviewOperationTest
{
    final List<Supplier<? extends WPObject>> submitted = new ArrayList<>();
    final List<CompletableFuture<RenderOutcome>> completions = new ArrayList<>();
    final List<CompletableFuture<RenderOutcome>> openings = new ArrayList<>();
    final WPObject object = new GenericObject("preview", 1, 1, 1, new Material[]{Material.STONE});
    String tabletSetting;

    @BeforeEach
    void useMouseInputWithoutNativeTabletDrivers() {
        tabletSetting = System.getProperty("org.pepsoft.worldpainter.disableTabletSupport");
        System.setProperty("org.pepsoft.worldpainter.disableTabletSupport", "true");
    }

    @AfterEach
    void restoreTabletSetting() {
        if (tabletSetting == null)
            System.clearProperty("org.pepsoft.worldpainter.disableTabletSupport");
        else
            System.setProperty("org.pepsoft.worldpainter.disableTabletSupport", tabletSetting);
    }

    CompletableFuture<RenderOutcome> submit(Supplier<? extends WPObject> supplier) {
        assertTrue(SwingUtilities.isEventDispatchThread());
        submitted.add(supplier);
        var result = new CompletableFuture<RenderOutcome>();
        completions.add(result);
        return result;
    }

    CompletableFuture<RenderOutcome> open() {
        assertTrue(SwingUtilities.isEventDispatchThread());
        var result = new CompletableFuture<RenderOutcome>();
        openings.add(result);
        return result;
    }

    @Test
    void brushRenderAndRerenderWorkWithoutMacroMachineDialog() throws Exception {
        Dimension dimension = dimension();
        PreviewOperation[] operation = new PreviewOperation[1];
        SwingUtilities.invokeAndWait(() -> {
            assertNull(MacroMachineWindow.getDialog());
            operation[0] = new PreviewOperation(this::submit, (tiles, dim, full) -> object, this::open);
            operation[0].setView(new TestView(dimension));
            var brush = SymmetricBrush.CONSTANT_CIRCLE.clone();
            brush.setRadius(1);
            operation[0].setBrush(brush);
            operation[0].tick(64, 64, false, true, 1);
            assertEquals(1, submitted.size());
            assertEquals(Set.of(new Point(0, 0)), operation[0].lastTileCoords);
            assertTrue(operation[0].rerenderButton.isEnabled());
            operation[0].rerenderButton.doClick(0);
            assertEquals(2, submitted.size());
            assertNull(MacroMachineWindow.getDialog());
        });
        assertSame(object, submitted.get(0).get());
        assertSame(object, submitted.get(1).get());
    }

    @Test
    void selectionIsClonedOnceAndExportModeIsCaptured() throws Exception {
        Dimension dimension = dimension();
        Tile original = dimension.getTile(0, 0);
        float height = original.getHeight(0, 0);
        var operation = new PreviewOperation(this::submit, (tiles, dim, full) -> {
            assertFalse(SwingUtilities.isEventDispatchThread());
            assertSame(dimension, dim);
            assertTrue(full);
            assertEquals(1, tiles.size());
            Tile snapshot = tiles.iterator().next();
            assertNotSame(original, snapshot);
            assertEquals(height, snapshot.getHeight(0, 0));
            return object;
        }, this::open);
        SwingUtilities.invokeAndWait(() -> {
            operation.requestRender(Set.of(original), dimension, true);
            original.setHeight(0, 0, height + 10);
        });
        assertSame(object, submitted.get(0).get());
    }

    @Test
    void staleCompletionCannotOverwriteLatestStatus() throws Exception {
        Dimension dimension = dimension();
        var operation = new PreviewOperation(this::submit, (tiles, dim, full) -> object, this::open);
        SwingUtilities.invokeAndWait(() -> {
            operation.requestRender(Set.of(dimension.getTile(0, 0)), dimension, false);
            operation.requestRender(Set.of(dimension.getTile(0, 0)), dimension, false);
        });
        completions.get(0).completeExceptionally(new IllegalStateException("stale failure"));
        SwingUtilities.invokeAndWait(() -> assertEquals("Preparing preview...", operation.statusLabel.getText()));
        completions.get(1).complete(RenderOutcome.SUBMITTED);
        SwingUtilities.invokeAndWait(() -> assertEquals("Idle", operation.statusLabel.getText()));
    }

    @Test
    void emptySelectionDoesNotSubmit() throws Exception {
        var operation = new PreviewOperation(this::submit, (tiles, dim, full) -> object, this::open);
        SwingUtilities.invokeAndWait(() -> operation.requestRender(Set.of(), dimension(), false));
        assertEquals(0, submitted.size());
        assertEquals("Idle", operation.statusLabel.getText());
    }

    @Test
    void autoUpdateAndManualRerenderShareSubmissionPath() throws Exception {
        Dimension dimension = dimension();
        var operation = new PreviewOperation(this::submit, (tiles, dim, full) -> object, this::open);
        SwingUtilities.invokeAndWait(() -> {
            operation.lastDim = dimension;
            operation.lastTileCoords = Set.of(new Point(0, 0));
            operation.autoUpdateCheckbox.setSelected(true);
            var listener = new PreviewOperation.TileChangedListener(operation);
            listener.heightMapChanged(dimension.getTile(0, 0));
            assertTrue(listener.debounceTimer.isRunning());
            listener.debounceTimer.stop();
            // Fire the actual debounce action deterministically instead of sleeping for its
            // delay.
            for (var action : listener.debounceTimer.getActionListeners())
                action.actionPerformed(null);
            assertEquals(1, submitted.size());
            operation.rerenderLastSelection(null);
            assertEquals(2, submitted.size());
        });
    }

    @Test
    void activationOpensAsynchronouslyWithoutSelectingTerrainOrOpeningMacroMachine() throws Exception {
        var operation = new PreviewOperation(this::submit, (tiles, dim, full) -> {
            fail("Activation must not export terrain");
            return object;
        }, this::open);
        SwingUtilities.invokeAndWait(() -> {
            var view = new TestView(dimension());
            operation.setView(view);
            setActive(operation, true);
            assertTrue(operation.isActive());
            assertTrue(java.util.Arrays.asList(view.getMouseListeners()).contains(operation));
            assertEquals(1, openings.size());
            assertFalse(openings.get(0).isDone());
            assertEquals("Opening preview...", operation.statusLabel.getText());
            assertNull(operation.lastTileCoords);
            assertNull(operation.lastDim);
            assertFalse(operation.rerenderButton.isEnabled());
            assertEquals(0, submitted.size());
            assertNull(MacroMachineWindow.getDialog());
            setActive(operation, false);
            assertFalse(operation.isActive());
            assertFalse(java.util.Arrays.asList(view.getMouseListeners()).contains(operation));
        });
        openings.get(0).complete(RenderOutcome.SUBMITTED);
        SwingUtilities.invokeAndWait(() -> assertEquals("Idle", operation.statusLabel.getText()));
    }

    @Test
    void reactivationDoesNotSubmitTerrainOrLoseTheRememberedSelection() throws Exception {
        var operation = new PreviewOperation(this::submit, (tiles, dim, full) -> object, this::open);
        var dimension = dimension();
        SwingUtilities.invokeAndWait(() -> {
            operation.setView(new TestView(dimension));
            operation.lastDim = dimension;
            operation.lastTileCoords = Set.of(new Point(0, 0));
            setActive(operation, true);
            setActive(operation, false);
            setActive(operation, true);
            assertEquals(2, openings.size());
            assertEquals(0, submitted.size());
            assertSame(dimension, operation.lastDim);
            assertEquals(Set.of(new Point(0, 0)), operation.lastTileCoords);
            setActive(operation, false);
        });
    }

    @Test
    void staleOpeningFailureCannotOverwriteTheTerrainRequestStatus() throws Exception {
        var dimension = dimension();
        var operation = new PreviewOperation(this::submit, (tiles, dim, full) -> object, this::open);
        SwingUtilities.invokeAndWait(() -> {
            operation.setView(new TestView(dimension));
            setActive(operation, true);
            operation.requestRender(Set.of(dimension.getTile(0, 0)), dimension, false);
            setActive(operation, false);
        });
        openings.get(0).completeExceptionally(new IllegalStateException("stale opening failure"));
        SwingUtilities.invokeAndWait(() -> assertEquals("Preparing preview...", operation.statusLabel.getText()));
        completions.get(0).complete(RenderOutcome.SUBMITTED);
        SwingUtilities.invokeAndWait(() -> assertEquals("Idle", operation.statusLabel.getText()));
    }

    static void setActive(PreviewOperation operation, boolean active) {
        try {
            operation.setActive(active);
        } catch (PropertyVetoException error) {
            throw new AssertionError(error);
        }
    }

    static Dimension dimension() {
        return TestDimension.createDimension(new TestDimension.DimensionParams(new Rectangle(0, 0, 128, 128), 0, 256,
                70, 1234, 62, org.pepsoft.worldpainter.DefaultPlugin.JAVA_ANVIL_1_19,
                org.pepsoft.worldpainter.Terrain.GRASS));
    }

    static class TestView extends WorldPainterView
    {
        Dimension dimension;

        TestView(Dimension dimension) {
            super(false, false);
            this.dimension = dimension;
        }

        @Override
        public Dimension getDimension() {
            return dimension;
        }

        @Override
        public void setDimension(Dimension dimension) {
            this.dimension = dimension;
        }

        @Override
        public void updateStatusBar(int x, int y) {
        }

        @Override
        public boolean isDrawBrush() {
            return false;
        }

        @Override
        public void setDrawBrush(boolean drawBrush) {
        }

        @Override
        public MapDragControl getMapDragControl() {
            return null;
        }

        @Override
        public BrushControl getBrushControl() {
            return null;
        }
    }
}
