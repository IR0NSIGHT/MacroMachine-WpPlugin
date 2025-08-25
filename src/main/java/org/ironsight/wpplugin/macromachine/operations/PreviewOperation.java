package org.ironsight.wpplugin.macromachine.operations;

import org.ironsight.wpplugin.macromachine.Gui.GlobalActionPanel;
import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.operations.*;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static org.ironsight.wpplugin.macromachine.threeDRendering.Export3DViewHelper.renderTileToSurfaceObject;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE_BITS;

public class PreviewOperation extends AbstractBrushOperation {

    /**
     * The globally unique ID of the operation. It's up to you what to use here. It is not visible to the user. It can
     * be a FQDN or package and class name, like here, or you could use a UUID. As long as it is globally unique.
     */
    static final String ID = "org.demo.wpplugin.3D_preview_Operation";
    /**
     * Human-readable short name of the operation.
     */
    static final String NAME = "3D Preview";
    /**
     * Human-readable description of the operation. This is used e.g. in the tooltip of the operation selection button.
     */
    static final String DESCRIPTION = "Show terrain, height and waterheight in a 3d preview";
    float[][] height = new float[0][];
    float[][] waterHeight = new float[0][];
    Material[][] terrain = new Material[0][];
    private Rectangle lastExtent = new Rectangle(0, 0, 0, 0);
    private TileChangedListener listener = new TileChangedListener(this);
    private ArrayList<Tile> tilesInExtent = new ArrayList<>();
    private Platform platform;

    public PreviewOperation() {
        super(NAME, DESCRIPTION, ID);
    }

    // HIGHLIGHT AREA
    public static final Layer annotationLayer = new AnnotationLayer(
            "macroMachine_3dpreview_annotationlayer", "3d " +
            "Preview", "Shows " +
            "what the 3d preview is currently " +
            "rendering", Layer.DataSize.BIT, true, 65, '\0');

    /**
     * Perform the operation. For single shot operations this is invoked once per mouse-down. For continuous operations
     * this is invoked once per {@code delay} ms while the mouse button is down, with the first invocation having
     * {@code first} be {@code true} and subsequent invocations having it be {@code false}.
     *
     * @param centreX      The x coordinate where the operation should be applied, in world coordinates.
     * @param centreY      The y coordinate where the operation should be applied, in world coordinates.
     * @param inverse      Whether to perform the "inverse" operation instead of the regular operation, if applicable.
     *                     If the operation has no inverse it should just apply the normal operation.
     * @param first        Whether this is the first tick of a continuous operation. For a one shot operation this will
     *                     always be {@code true}.
     * @param dynamicLevel The dynamic level (from 0.0f to 1.0f inclusive) to apply in addition to the {@code level}
     *                     property, for instance due to a pressure sensitive stylus being used. In other words,
     *                     <strong>not</strong> the total level at which to apply the operation! Operations are free to
     *                     ignore this if it is not applicable. If the operation is being applied through a means which
     *                     doesn't provide a dynamic level (for instance the mouse), this will be <em>exactly</em>
     *                     {@code 1.0f}.
     */
    @Override
    protected void tick(int centreX, int centreY, boolean inverse, boolean first, float dynamicLevel) {
        //  Perform the operation. In addition to the parameters you have the following methods available:
        // * getDimension() - obtain the dimension on which to perform the operation
        // * getLevel() - obtain the current brush intensity setting as a float between 0.0 and 1.0
        // * isAltDown() - whether the Alt key is currently pressed - NOTE: this is already in use to indicate whether
        //                 the operation should be inverted, so should probably not be overloaded
        // * isCtrlDown() - whether any of the Ctrl, Windows or Command keys are currently pressed
        // * isShiftDown() - whether the Shift key is currently pressed
        // In addition you have the following fields in this class:
        // * brush - the currently selected brush
        // * paint - the currently selected paint

        int radius = this.getBrush().getEffectiveRadius();

        float tileRadius = 1f * radius / TILE_SIZE;
        int tileRadiusMax = (int) Math.ceil(tileRadius);
        Dimension dim = getDimension();

        Rectangle selectedSquare = new Rectangle(centreX - radius, centreY - radius, 2 * radius, 2 * radius);

        HashSet<Tile> tiles = new HashSet<>();

        // the tile in which the click position is
        int centreXTile = centreX >> TILE_SIZE_BITS, centreYTile = centreY >> TILE_SIZE_BITS;
        tiles.add(dim.getTile(centreXTile, centreYTile)); // always add the clicked tile itself.
        for (int x = centreXTile - tileRadiusMax; x <= centreXTile + tileRadiusMax; x++) {
            for (int y = centreYTile - tileRadiusMax; y <= centreYTile + tileRadiusMax; y++) {
                if (!dim.getExtent().contains(x, y))
                    continue;

                int tileCenterBlockX = x * TILE_SIZE + TILE_SIZE/2;
                int tileCenterBlockY = y * TILE_SIZE + TILE_SIZE/2;

                if (!selectedSquare.contains(tileCenterBlockX, tileCenterBlockY)) {
                    System.out.printf("reject chunk %d,%d because center %x %d not in extent %s",x,y,
                            tileCenterBlockX, tileCenterBlockY, selectedSquare.toString());
                    continue;
                }

                Tile t = dim.getTile(x, y);
                assert t != null;
                tiles.add(t);
            }
        }

        if (!dim.isEventsInhibited())
            dim.setEventsInhibited(true);
        dim.clearLayerData(annotationLayer);
        for (Tile tile : tiles) {
            tile = dim.getTileForEditing(tile.getX(),tile.getY());
            int width = 5;
            for (int x = 0; x < TILE_SIZE; x++) {
                for (int y = 0; y < TILE_SIZE; y++) {
                    if (x < width || x > TILE_SIZE- width || y < width || y > TILE_SIZE - width)
                        tile.setBitLayerValue(annotationLayer, x, y, true);
                }
            }
        }
        if (dim.isEventsInhibited())
            dim.setEventsInhibited(false);

        GlobalActionPanel.setSurfaceObject(renderTileToSurfaceObject(tiles, dim));
        GlobalActionPanel.flagForChangedSurfaceObject();
    }

    static class TileChangedListener implements Tile.Listener {
        private PreviewOperation op;
        private HashSet<Tile> dirtyTiles = new HashSet<>();
        private boolean changed = false;

        TileChangedListener(PreviewOperation op) {
            this.op = op;
            Timer timer = new Timer(750, e -> triggerTileUpdate());
            timer.setRepeats(true);
            timer.start();
        }

        @Override
        public void heightMapChanged(Tile tile) {
            dirtyTiles.add(tile);
        }

        private void triggerTileUpdate() {
            if (dirtyTiles.isEmpty())
                return;
            for (Tile t : dirtyTiles) {
                // boolean tileChanged = updateArraysFromTile(op.height, op.waterHeight, op.terrain, op
                // .lastExtent, t,
                //         op.platform);
                // if (tileChanged)
                //     changed = true;
            }
        /*    if (changed)
                op.submitArraysToViewer();
                */

            dirtyTiles.clear();
            changed = false;
        }

        @Override
        public void terrainChanged(Tile tile) {
            dirtyTiles.add(tile);
        }

        @Override
        public void waterLevelChanged(Tile tile) {
            dirtyTiles.add(tile);
        }

        @Override
        public void layerDataChanged(Tile tile, Set<Layer> set) {

        }

        @Override
        public void allBitLayerDataChanged(Tile tile) {

        }

        @Override
        public void allNonBitlayerDataChanged(Tile tile) {

        }

        @Override
        public void seedsChanged(Tile tile) {

        }
    }

}