package org.ironsight.wpplugin.macromachine.operations;

import jakarta.annotation.Nullable;
import org.ironsight.wpplugin.macromachine.threeDRendering.CubeArrayRenderer;
import org.ironsight.wpplugin.macromachine.threeDRendering.CubeArrayRenderer.RenderOutcome;
import org.ironsight.wpplugin.macromachine.threeDRendering.Export3DViewHelper;
import org.pepsoft.worldpainter.CoordinateTransform;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.operations.AbstractBrushOperation;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyVetoException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import org.pepsoft.worldpainter.objects.WPObject;

import static org.pepsoft.worldpainter.Constants.TILE_SIZE;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE_BITS;

public class PreviewOperation extends AbstractBrushOperation
{

    /**
     * The globally unique ID of the operation. It's up to you what to use here. It
     * is not visible to the user. It can be a FQDN or package and class name, like
     * here, or you could use a UUID. As long as it is globally unique.
     */
    static final String ID = "org.demo.wpplugin.3D_preview_Operation";

    /** Human-readable short name of the operation. */
    static final String NAME = "3D Preview";

    /**
     * Human-readable description of the operation. This is used e.g. in the tooltip
     * of the operation selection button.
     */
    static final String DESCRIPTION = "Show terrain, height and waterheight in a 3d preview";

    private TileChangedListener listener = new TileChangedListener(this);
    Set<Point> lastTileCoords;
    Dimension lastDim;
    JButton rerenderButton;
    JLabel statusLabel;
    JCheckBox autoUpdateCheckbox;
    private Set<Point> subscribedTileCoords = new HashSet<>();
    private boolean useFullExport;
    private final JPanel optionsPanel;
    final Function<Supplier<? extends WPObject>, CompletableFuture<RenderOutcome>> renderer;
    final SelectionExporter exporter;
    final Supplier<CompletableFuture<RenderOutcome>> opener;
    long latestRequest;

    public PreviewOperation() {
        this(PreviewOperation::renderSelection, Export3DViewHelper::renderTileToSurfaceObject,
                PreviewOperation::openRenderer);
    }

    PreviewOperation(Function<Supplier<? extends WPObject>, CompletableFuture<RenderOutcome>> renderer,
            SelectionExporter exporter, Supplier<CompletableFuture<RenderOutcome>> opener) {
        super(NAME, DESCRIPTION, ID);
        this.renderer = renderer;
        this.exporter = exporter;
        this.opener = opener;
        optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        JLabel title = new JLabel(NAME);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        optionsPanel.add(title);
        JTextArea desc = new JTextArea(DESCRIPTION);
        desc.setEditable(false);
        desc.setWrapStyleWord(true);
        desc.setLineWrap(true);
        desc.setAlignmentX(Component.LEFT_ALIGNMENT);
        optionsPanel.add(desc);
        optionsPanel.add(Box.createVerticalStrut(8));
        rerenderButton = new JButton("Rerender");
        rerenderButton.setEnabled(false);
        rerenderButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        rerenderButton.addActionListener(this::rerenderLastSelection);
        optionsPanel.add(rerenderButton);
        optionsPanel.add(Box.createVerticalStrut(4));
        statusLabel = new JLabel("Idle");
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        optionsPanel.add(statusLabel);
        optionsPanel.add(Box.createVerticalStrut(2));
        optionsPanel.add(Box.createVerticalStrut(4));
        autoUpdateCheckbox = new JCheckBox("Auto Update");
        autoUpdateCheckbox
                .setToolTipText("Automatically update the 3d Preview, every time you edit the map. Can be slow.");
        autoUpdateCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        optionsPanel.add(autoUpdateCheckbox);
        optionsPanel.add(Box.createVerticalStrut(8));

        /*
         * JToggleButton fullExportToggle = new JToggleButton("Full Export", false);
         * fullExportToggle.
         * setToolTipText("Use full export settings (slower, produces a larger schematic)"
         * ); fullExportToggle.setAlignmentX(Component.LEFT_ALIGNMENT);
         * fullExportToggle.addActionListener(e -> useFullExport =
         * fullExportToggle.isSelected()); optionsPanel.add(fullExportToggle);
         */
    }

    @Override
    public JPanel getOptionsPanel() {
        return optionsPanel;
    }

    @Override
    protected void activate() throws PropertyVetoException {
        super.activate();
        long request = ++latestRequest;
        statusLabel.setText("Opening preview...");
        statusLabel.setToolTipText(null);
        try {
            opener.get()
                    .whenComplete((outcome, error) -> SwingUtilities.invokeLater(() -> finishRender(request, error)));
        } catch (Exception error) {
            finishRender(request, error);
        }
    }

    void rerenderLastSelection(@Nullable ActionEvent e) {
        if (lastTileCoords == null || lastTileCoords.isEmpty() || lastDim == null) {
            return;
        }
        HashSet<Tile> tiles = new HashSet<>();
        for (Point coord : lastTileCoords) {
            Tile liveTile = lastDim.getTile(coord.x, coord.y);
            if (liveTile != null) {
                tiles.add(liveTile);
            }
        }
        if (tiles.isEmpty()) {
            statusLabel.setText("Idle");
            return;
        }
        requestRender(tiles, lastDim, useFullExport);
    }

    void requestRender(Set<Tile> referenceTiles, Dimension referenceDimension, boolean fullExport) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> requestRender(referenceTiles, referenceDimension, fullExport));
            return;
        }
        long request = ++latestRequest;
        Set<Tile> clonedTiles = new HashSet<>();
        for (Tile tile : referenceTiles) {
            if (tile != null)
                clonedTiles.add(tile.transform(CoordinateTransform.NOOP));
        }
        if (clonedTiles.isEmpty()) {
            statusLabel.setText("Idle");
            return;
        }
        statusLabel.setText("Preparing preview...");
        statusLabel.setToolTipText(null);
        renderer.apply(() -> exporter.export(clonedTiles, referenceDimension, fullExport))
                .whenComplete((outcome, error) -> SwingUtilities.invokeLater(() -> finishRender(request, error)));
    }

    void finishRender(long request, Throwable error) {
        if (request != latestRequest)
            return;
        statusLabel.setText(error == null ? "Idle" : "Preview failed");
        if (error != null) {
            statusLabel.setToolTipText(error.getMessage());
            System.getLogger(PreviewOperation.class.getName())
                    .log(System.Logger.Level.ERROR, "3D preview failed", error);
        }
    }

    static CompletableFuture<RenderOutcome> renderSelection(Supplier<? extends WPObject> selection) {
        return CubeArrayRenderer.getInstance().render(selection);
    }

    static CompletableFuture<RenderOutcome> openRenderer() {
        return CubeArrayRenderer.getInstance().openPreview();
    }

    @FunctionalInterface
    interface SelectionExporter
    {
        WPObject export(Set<Tile> tiles, Dimension dimension, boolean fullExport);
    }

    // HIGHLIGHT AREA
    public static final Layer annotationLayer = new AnnotationLayer("macroMachine_3dpreview_annotationlayer",
            "3d " + "Preview", "Shows " + "what the 3d preview is currently " + "rendering", Layer.DataSize.BIT, false,
            65, '\0');

    /**
     * Perform the operation. For single shot operations this is invoked once per
     * mouse-down. For continuous operations this is invoked once per {@code delay}
     * ms while the mouse button is down, with the first invocation having
     * {@code first} be {@code true} and subsequent invocations having it be
     * {@code false}.
     *
     * @param centreX
     *            The x coordinate where the operation should be applied, in world
     *            coordinates.
     * @param centreY
     *            The y coordinate where the operation should be applied, in world
     *            coordinates.
     * @param inverse
     *            Whether to perform the "inverse" operation instead of the regular
     *            operation, if applicable. If the operation has no inverse it
     *            should just apply the normal operation.
     * @param first
     *            Whether this is the first tick of a continuous operation. For a
     *            one shot operation this will always be {@code true}.
     * @param dynamicLevel
     *            The dynamic level (from 0.0f to 1.0f inclusive) to apply in
     *            addition to the {@code level} property, for instance due to a
     *            pressure sensitive stylus being used. In other words,
     *            <strong>not</strong> the total level at which to apply the
     *            operation! Operations are free to ignore this if it is not
     *            applicable. If the operation is being applied through a means
     *            which doesn't provide a dynamic level (for instance the mouse),
     *            this will be <em>exactly</em> {@code 1.0f}.
     */
    @Override
    protected void tick(int centreX, int centreY, boolean inverse, boolean first, float dynamicLevel) {
        System.out.println("[PreviewOp] tick - world pos: (" + centreX + ", " + centreY + ")");
        if (inverse)
            return;

        int radius = this.getBrush().getEffectiveRadius();

        float tileRadius = 1f * radius / TILE_SIZE;
        int tileRadiusMax = (int) Math.ceil(tileRadius);
        Dimension dim = getDimension();
        if (dim == null)
            return;

        Rectangle selectedSquare = new Rectangle(centreX - radius, centreY - radius, 2 * radius, 2 * radius);

        HashSet<Tile> tiles = new HashSet<>();

        // the tile in which the click position is
        int centreXTile = centreX >> TILE_SIZE_BITS, centreYTile = centreY >> TILE_SIZE_BITS;
        Tile centreTile = dim.getTile(centreXTile, centreYTile);
        if (centreTile != null)
            tiles.add(centreTile);
        for (int x = centreXTile - tileRadiusMax; x <= centreXTile + tileRadiusMax; x++) {
            for (int y = centreYTile - tileRadiusMax; y <= centreYTile + tileRadiusMax; y++) {
                if (!dim.getExtent().contains(x, y))
                    continue;

                int tileCenterBlockX = x * TILE_SIZE + TILE_SIZE / 2;
                int tileCenterBlockY = y * TILE_SIZE + TILE_SIZE / 2;

                if (!selectedSquare.contains(tileCenterBlockX, tileCenterBlockY)) {
                    System.out.printf("reject chunk %d,%d because center %x %d not in extent %s", x, y,
                            tileCenterBlockX, tileCenterBlockY, selectedSquare.toString());
                    continue;
                }

                Tile t = dim.getTile(x, y);
                if (t != null)
                    tiles.add(t);
            }
        }

        if (tiles.isEmpty())
            return;
        lastTileCoords = new HashSet<>();
        for (Tile tile : tiles) {
            lastTileCoords.add(new Point(tile.getX(), tile.getY()));
        }
        lastDim = dim;
        rerenderButton.setEnabled(true);

        Set<Point> newCoords = new HashSet<>(lastTileCoords);
        for (Point coord : subscribedTileCoords) {
            if (!newCoords.contains(coord)) {
                Tile oldTile = dim.getTile(coord.x, coord.y);
                if (oldTile != null) {
                    oldTile.removeListener(listener);
                }
            }
        }
        for (Point coord : newCoords) {
            if (!subscribedTileCoords.contains(coord)) {
                Tile newTile = dim.getTile(coord.x, coord.y);
                if (newTile != null) {
                    newTile.addListener(listener);
                }
            }
        }
        subscribedTileCoords = newCoords;

        if (!dim.isEventsInhibited())
            dim.setEventsInhibited(true);
        dim.clearLayerData(annotationLayer);
        for (Tile tile : tiles) {
            tile = dim.getTileForEditing(tile.getX(), tile.getY());
            int width = 5;
            for (int x = 0; x < TILE_SIZE; x++) {
                for (int y = 0; y < TILE_SIZE; y++) {
                    if (x < width || x > TILE_SIZE - width || y < width || y > TILE_SIZE - width)
                        tile.setBitLayerValue(annotationLayer, x, y, true);
                }
            }
        }
        if (dim.isEventsInhibited())
            dim.setEventsInhibited(false);

        requestRender(tiles, dim, useFullExport);
    }

    static class TileChangedListener implements Tile.Listener
    {
        private PreviewOperation op;
        Timer debounceTimer;

        TileChangedListener(PreviewOperation op) {
            this.op = op;
            debounceTimer = new Timer(500, e -> {
                if (op.autoUpdateCheckbox.isSelected() && op.lastTileCoords != null && !op.lastTileCoords.isEmpty()) {
                    op.rerenderLastSelection(null);
                }
            });
            debounceTimer.setRepeats(false);
        }

        private void onTileChanged(Tile tile) {
            if (!SwingUtilities.isEventDispatchThread()) {
                SwingUtilities.invokeLater(() -> onTileChanged(tile));
                return;
            }
            if (op.lastTileCoords == null || op.lastTileCoords.isEmpty())
                return;
            Point tileCoord = new Point(tile.getX(), tile.getY());
            if (op.lastTileCoords.contains(tileCoord) && op.autoUpdateCheckbox.isSelected()) {
                debounceTimer.restart();
            }
        }

        @Override
        public void heightMapChanged(Tile tile) {
            onTileChanged(tile);
        }

        @Override
        public void terrainChanged(Tile tile) {
            onTileChanged(tile);
        }

        @Override
        public void waterLevelChanged(Tile tile) {
            onTileChanged(tile);
        }

        @Override
        public void layerDataChanged(Tile tile, Set<Layer> set) {
            onTileChanged(tile);
        }

        @Override
        public void allBitLayerDataChanged(Tile tile) {
            onTileChanged(tile);
        }

        @Override
        public void allNonBitlayerDataChanged(Tile tile) {
            onTileChanged(tile);
        }

        @Override
        public void seedsChanged(Tile tile) {
            onTileChanged(tile);
        }
    }
}
