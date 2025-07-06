package org.ironsight.wpplugin.macromachine.operations;

import org.ironsight.wpplugin.macromachine.Gui.GlobalActionPanel;
import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.brushes.Brush;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.operations.AbstractPaintOperation;
import org.pepsoft.worldpainter.operations.BrushOperation;
import org.pepsoft.worldpainter.operations.MouseOrTabletOperation;
import org.pepsoft.worldpainter.operations.RadiusOperation;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static org.pepsoft.worldpainter.Constants.TILE_SIZE;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE_BITS;

public class PreviewOperation extends RadiusOperation {

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
        super(NAME, DESCRIPTION, new MyShittyView(), ID); // ONE SHOT OP
    }

    private static WorldPainterView getWpView() {
        App app = App.getInstanceIfExists();
        if (app == null) return null;
        try {
            Field field = App.class.getDeclaredField("view");
            field.setAccessible(true);
            return (WorldPainterView) field.get(app);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static boolean updateArraysFromTile(float[][] height, float[][] waterHeight, Material[][] terrain,
                                                Rectangle arrayExtents, Tile t, Platform p) {
        boolean changed = false;
        int tileStartX = t.getX() * TILE_SIZE;
        int tileStartY = t.getY() * TILE_SIZE;
        int startX = Math.max(0, arrayExtents.x - tileStartX);
        int startY = Math.max(0, arrayExtents.y - tileStartY);
        for (int x = startX; x < TILE_SIZE; x++) {
            for (int y = startY; y < TILE_SIZE; y++) {
                changed = true;
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
        return changed;
    }

    void submitArraysToViewer() {
        GlobalActionPanel.getSurfaceObject().setTerrainData(height, terrain, waterHeight);
        GlobalActionPanel.flagForChangedSurfaceObject();
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

        int radius = this.getBrush().getEffectiveRadius();
        int startX = Math.max(getDimension().getLowestX() * TILE_SIZE, centreX - radius);
        int startY = Math.max(getDimension().getLowestY() * TILE_SIZE, centreY - radius);
        int endX = Math.min((getDimension().getHighestX() + 1) * TILE_SIZE - 1,
                centreX + radius);
        int endY = Math.min((getDimension().getHighestY() + 1) * TILE_SIZE - 1,
                centreY + radius);
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

    static class MyShittyView extends WorldPainterView {

        Dimension dim;
        private boolean drawBrush = false;

        @Override
        public Dimension getDimension() {
            return dim;
        }

        @Override
        public void setDimension(Dimension dimension) {
            this.dim = dimension;
        }

        @Override
        public void updateStatusBar(int x, int y) {

        }

        @Override
        public boolean isDrawBrush() {
            return drawBrush;
        }

        @Override
        public void setDrawBrush(boolean drawBrush) {
            this.drawBrush = drawBrush;
        }

        @Override
        public MapDragControl getMapDragControl() {
            return new MyRadiusControl();
        }

        @Override
        public RadiusControl getRadiusControl() {
            return new MyRadiusControl();
        }
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
                boolean tileChanged = updateArraysFromTile(op.height, op.waterHeight, op.terrain, op.lastExtent, t,
                        op.platform);
                if (tileChanged)
                    changed = true;
            }
            if (changed)
                op.submitArraysToViewer();
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