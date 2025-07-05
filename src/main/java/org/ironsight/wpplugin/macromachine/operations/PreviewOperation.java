package org.ironsight.wpplugin.macromachine.operations;

import org.ironsight.wpplugin.macromachine.Gui.GlobalActionPanel;
import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.operations.AbstractPaintOperation;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Set;

import static org.pepsoft.worldpainter.Constants.TILE_SIZE;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE_BITS;

public class PreviewOperation extends AbstractPaintOperation {
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
        // Using this constructor will create a "single shot" operation. The tick() method below will only be invoked
        // once for every time the user clicks the mouse or presses on the tablet:
        super(NAME, DESCRIPTION, null, new MyRadiusControl(), new MyRadiusControl(), -1, ID);
        // Using this constructor instead will create a continues operation. The tick() method will be invoked once
        // every "delay" ms while the user has the mouse button down or continues pressing on the tablet. The "first"
        // parameter will be true for the first invocation per mouse button press and false for every subsequent
        // invocation:
        // super(NAME, DESCRIPTION, delay, ID);
    }

    void submitArraysToViewer() {
        GlobalActionPanel.getSurfaceObject().setTerrainData(height, terrain, waterHeight);
        SwingUtilities.invokeLater(() -> GlobalActionPanel.getPreviewer()
                .setObject(GlobalActionPanel.getSurfaceObject(), getDimension()));
    }
    private static void updateArraysFromTile(float[][] height, float[][] waterHeight, Material[][] terrain,
                                             Rectangle arrayExtents, Tile t, Platform p) {
        int tileStartX = t.getX() * TILE_SIZE;
        int tileStartY = t.getY() * TILE_SIZE;

        for (int x = 0; x < TILE_SIZE; x++) {
            for (int y = 0; y < TILE_SIZE; y++) {
                int absMapPosX = x + tileStartX;
                int absMapPosY = y + tileStartY;
                if (!arrayExtents.contains(absMapPosX, absMapPosY))
                    continue;
                int idxX = absMapPosX - arrayExtents.x;
                int idxY = absMapPosY - arrayExtents.y;
                int heightValue = t.getIntHeight(x, y);
                height[idxX][idxY] = t.getIntHeight(x, y);
                waterHeight[idxX][idxY] = t.getWaterLevel(x, y);
                terrain[idxX][idxY] = t.getTerrain(x, y)
                        .getMaterial(p, 123456L, absMapPosX, absMapPosY, heightValue, heightValue);
            }
        }
    }


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


        int startX = Math.max(getDimension().getLowestX() * TILE_SIZE, centreX - this.getBrush().getEffectiveRadius());
        int startY = Math.max(getDimension().getLowestY() * TILE_SIZE, centreY - this.getBrush().getEffectiveRadius());
        int endX = Math.min((getDimension().getHighestX() + 1) * TILE_SIZE - 1,
                centreX + this.getBrush().getEffectiveRadius());
        int endY = Math.min((getDimension().getHighestY() + 1) * TILE_SIZE - 1,
                centreY + this.getBrush().getEffectiveRadius());
        int sizeX = Math.max(0, endX - startX);
        int sizeY = Math.max(0, endY - startY);

        lastExtent = new Rectangle(startX, startY, sizeX, sizeY);
        for (Tile t : tilesInExtent)
            t.removeListener(this.listener);
        tilesInExtent.clear();

        for (int x = startX >> TILE_SIZE_BITS; x <= endX >> TILE_SIZE_BITS; x++) {
            for (int y = startY >> TILE_SIZE_BITS; y <= endY >> TILE_SIZE_BITS; y++) {
                Tile t = getDimension().getTile(x, y);
                t.addListener(this.listener);
                tilesInExtent.add(getDimension().getTile(x, y));

            }
        }


        height = new float[sizeX][];
        waterHeight = new float[sizeX][];
        terrain = new Material[sizeX][];
        Dimension dim = getDimension();

        platform = getView().getDimension().getWorld().getPlatform();
        for (int x = 0; x < sizeX; x++) {
            height[x] = new float[sizeY];
            waterHeight[x] = new float[sizeY];
            terrain[x] = new Material[sizeY];
            for (int y = 0; y < sizeY; y++) {
                height[x][y] = dim.getIntHeightAt(x + startX, y + startY);
                waterHeight[x][y] = dim.getWaterLevelAt(x + startX, y + startY);
                terrain[x][y] =
                        dim.getTerrainAt(x + startX, y + startY)
                                .getMaterial(platform, 123456L, x + startX, y + startY, height[x][y],
                                        Math.round(height[x][y]));
            }
        }
        submitArraysToViewer();
    }

    static class TileChangedListener implements Tile.Listener {
        private PreviewOperation op;

        TileChangedListener(PreviewOperation op) {
            this.op = op;
        }

        @Override
        public void heightMapChanged(Tile tile) {
            updateArraysFromTile(op.height, op.waterHeight, op.terrain, op.lastExtent, tile, op.platform);
            op.submitArraysToViewer();
        }

        @Override
        public void terrainChanged(Tile tile) {

        }

        @Override
        public void waterLevelChanged(Tile tile) {

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

    static class MyRadiusControl implements RadiusControl, MapDragControl {
        //dummy class, doesnt do anything. just so no nullpointer is thrown by the operation.q
        int radius = 250;
        boolean mapDrag = false;

        @Override
        public void increaseRadius(int i) {
            radius += i;
        }

        @Override
        public void increaseRadiusByOne() {
            radius++;
        }

        @Override
        public void decreaseRadius(int i) {
            radius -= i;
        }

        @Override
        public void decreaseRadiusByOne() {
            radius--;
        }

        @Override
        public boolean isMapDraggingInhibited() {
            return mapDrag;
        }

        @Override
        public void setMapDraggingInhibited(boolean b) {
            mapDrag = b;
        }
    }
}