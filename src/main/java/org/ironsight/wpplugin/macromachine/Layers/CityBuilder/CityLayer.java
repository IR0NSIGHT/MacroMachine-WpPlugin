package org.ironsight.wpplugin.macromachine.Layers.CityBuilder;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static org.ironsight.wpplugin.macromachine.Layers.CityBuilder.CityInfoDatabase.NO_DATA;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE_BITS;
import static org.pepsoft.worldpainter.objects.WPObject.ATTRIBUTE_FILE;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.vecmath.Point3i;

import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IntegerTile;
import org.pepsoft.minecraft.Material;
import org.pepsoft.util.undo.BufferKey;
import org.pepsoft.util.undo.UndoListener;
import org.pepsoft.util.undo.UndoManager;
import org.pepsoft.worldpainter.DefaultPlugin;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.exporting.LayerExporter;
import org.pepsoft.worldpainter.layers.CustomLayer;
import org.pepsoft.worldpainter.layers.exporters.ExporterSettings;
import org.pepsoft.worldpainter.layers.renderers.LayerRenderer;
import org.pepsoft.worldpainter.objects.MirroredObject;
import org.pepsoft.worldpainter.objects.RotatedObject;
import org.pepsoft.worldpainter.objects.WPObject;

public class CityLayer extends CustomLayer implements UndoListener
{
    public static final int MIRROR_BIT_MASK = 0b1;
    public static final int MIRROR_BIT_SHIFT = 0;
    public static final int ID_BIT_SHIFT = 4;
    public static final int ID_BIT_MASK = 0xFFFFFFF0;
    public static final int ROTATION_BIT_MASK = 0b0110;
    public static final int ROTATION_BIT_SHIFT = 1;
    @Serial
    private static final long serialVersionUID = 1L;
    private ArrayList<WPObject> objects = new ArrayList<>();
    private CityInfoDatabase database = new CityInfoDatabase();
    private boolean useHighlightColors = true;
    private transient CityLayerRenderer renderer = new CityLayerRenderer(this);
    /*
     * Undo: the house list is a buffer of the undo manager of the dimension the
     * layer is edited in (WorldPainter's own mechanism: it keeps a copy per undo
     * step and hands the right one back on undo / redo). All runtime only, so the
     * saved layer stays exactly as before.
     */
    /** The undo manager the house list is registered with (null: none). */
    private transient UndoManager undoManager;
    /** The house list's key in {@link #undoManager}. */
    private transient BufferKey<CityInfoDatabase> undoKey;
    /** The house list as it was when registered: undo beyond that returns to it. */
    private transient CityInfoDatabase undoBaseline;
    /** Told after undo / redo restored the house list (the City Tool). */
    private transient Runnable afterUndoRestored;
    public boolean isUseHighlightColors() {
        return useHighlightColors;
    }
    public void setUseHighlightColors(boolean useHighlightColors) {
        this.useHighlightColors = useHighlightColors;
        if (renderer != null) {
            renderer.setUseHighlightColors(useHighlightColors);
        }
    }
    public CityLayer(String name, String description) {
        super(name, description, DataSize.NIBBLE, 50, Color.cyan);
    }

    public void setDataAt(Dimension dimension, int blockX, int blockY, ObjectState state) {
        if (state == null || state.objectIndex < 0 || state.objectIndex >= objects.size())
            return;

        trackUndo(undoManagerOf(dimension));
        editableDatabase().setDataAt(blockX, blockY,
                getValueForState(state.rotation, state.mirrored, state.objectIndex));
        resetLastEdited();
        WPObject paintedSchem = getObjectForState(state);
        if (paintedSchem == null)
            return;
        Point3i dims = paintedSchem.getDimensions();
        for (int tileX = (blockX - dims.x) >> TILE_SIZE_BITS; tileX <= (blockX + dims.x) >> TILE_SIZE_BITS; tileX++) {
            for (int tileY = (blockY - dims.y) >> TILE_SIZE_BITS; tileY <= (blockY
                    + dims.y) >> TILE_SIZE_BITS; tileY++) {
                repaintWorldpainterTile(tileX, tileY, dimension, database);
            }
        }
    }

    @Override
    public LayerRenderer getRenderer() {
        if (renderer == null) {
            renderer = new CityLayerRenderer(this);
        }
        if (this.getPaint() instanceof Color color) {
            renderer.setBaseColor(color.getRGB());
        }
        renderer.setUseHighlightColors(this.useHighlightColors);
        return renderer;
    }

    public Image getSchematicImage(ObjectState state) {
        WPObject schematic = getObjectForState(state);
        if (schematic == null)
            return null;

        Point3i dims = schematic.getDimensions();
        int space = 3;
        BufferedImage img = new BufferedImage(2 * space + dims.x, 2 * space + dims.y, TYPE_INT_ARGB);
        paintObjectToImage(img, space, space, schematic);
        return img;
    }

    /**
     * The house's shaded top view (see {@link SchematicViews#top}) into the map's
     * colour tile.
     */
    private void paintObjectToColorTile(IntegerTile colorTile, WPObject object, int objectTilePosX,
            int objectTilePosY) {
        int[][] colours = SchematicViews.top(object);
        int offsetX = object.getOffset().x;
        int offsetY = object.getOffset().y;
        for (int x = 0; x < colours.length; x++)
            for (int y = 0; y < colours[x].length; y++) {
                if (colours[x][y] == 0)
                    continue;
                int pixelPosInTileX = objectTilePosX + x + offsetX;
                int pixelPosInTileY = objectTilePosY + y + offsetY;
                if (pixelPosInTileX < 0 || pixelPosInTileY < 0 || pixelPosInTileX >= TILE_SIZE
                        || pixelPosInTileY >= TILE_SIZE)
                    continue; // outside the colour tile
                colorTile.setValueAt(pixelPosInTileX, pixelPosInTileY, colours[x][y]);
            }
    }

    /**
     * The house's shaded top view into an image, {@code left} / {@code top} pixels
     * in.
     */
    private void paintObjectToImage(BufferedImage image, int left, int top, WPObject object) {
        int[][] colours = SchematicViews.top(object);
        for (int x = 0; x < colours.length; x++)
            for (int y = 0; y < colours[x].length; y++)
                if (colours[x][y] != 0)
                    image.setRGB(left + x, top + y, colours[x][y]);
    }

    private void paintObjectIntoTile(Tile tile, WPObject object, Point objectGlobalPos) {
        int tileXX = tile.getX();
        int tileYY = tile.getY();

        int blockX = objectGlobalPos.x;
        int blockY = objectGlobalPos.y;
        int offsetX = object.getOffset().x;
        int offsetY = object.getOffset().y;
        int height = object.getDimensions().z;
        for (int x = 0; x < object.getDimensions().x; x++) {
            for (int y = 0; y < object.getDimensions().y; y++) {
                int totalPosX = blockX + x + offsetX, totalPosY = blockY + y + offsetY;
                if (totalPosX >> TILE_SIZE_BITS != tileXX || totalPosY >> TILE_SIZE_BITS != tileYY)
                    continue;
                totalPosX -= tileXX << TILE_SIZE_BITS;
                totalPosY -= tileYY << TILE_SIZE_BITS;
                for (int z = object.getDimensions().z - 1; z >= 0; z--) {
                    if (object.getMaterial(x, y, z) != Material.AIR) {
                        int value = Math.max(1, 16 * z / height);
                        int existing = tile.getLayerValue(this, totalPosX, totalPosY);
                        value = Math.min(15, value + existing);
                        tile.setLayerValue(this, totalPosX, totalPosY, value);
                        break;
                    }
                }
            }
        }
    }
    private long lastEdited = 0;
    public boolean lastEditedAfter(long compareTimeMillis) {
        return lastEdited > compareTimeMillis;
    }
    private void resetLastEdited() {
        lastEdited = System.currentTimeMillis();
    }
    void repaintColorTile(int tileX, int tileY, IntegerTile tile) {
        tile.fillWith(0);
        // find all objects that live in tile
        for (int tileXX = tileX - 1; tileXX <= tileX + 1; tileXX++) // iterate neighbouring tiles, one left,r,u,d
            for (int tileYY = tileY - 1; tileYY <= tileY + 1; tileYY++) {
                HashMap<Point, Integer> tileData = database.getTileData(tileXX, tileYY); // gets data which objects live
                                                                                         // in this tile
                if (tileData == null)
                    continue;
                for (var pos : tileData.keySet()) { // iterate all objects in the tile
                    ObjectState state = getInformationAt(pos.x, pos.y);
                    WPObject object = getObjectForState(state);
                    if (object == null)
                        continue;

                    // paint object into reference tile
                    paintObjectToColorTile(tile, object, state.xPos - tileX * TILE_SIZE,
                            state.yPos - tileY * TILE_SIZE);
                }
            }
    }

    private void repaintWorldpainterTile(int tileX, int tileY, Dimension dimension, CityInfoDatabase database) {
        Tile tile = dimension.getTileForEditing(tileX, tileY);
        if (tile == null)
            return;
        tile.clearLayerData(this);

        // find all objects that live in tile
        for (int tileXX = tileX - 1; tileXX <= tileX + 1; tileXX++) // iterate neighbouring tiles, one left,r,u,d
            for (int tileYY = tileY - 1; tileYY <= tileY + 1; tileYY++) {
                HashMap<Point, Integer> tileData = database.getTileData(tileXX, tileYY); // gets data which objects live
                                                                                         // in this tile
                if (tileData == null)
                    continue;
                for (var pos : tileData.keySet()) { // iterate all objects in the tile
                    ObjectState state = getInformationAt(pos.x, pos.y);
                    WPObject object = getObjectForState(state);
                    if (object == null)
                        continue;

                    // paint object into reference tile
                    paintObjectIntoTile(tile, object, pos);
                }
            }
    }

    public void removeDataAt(Dimension dimension, int blockX, int blockY) {
        WPObject paintedSchem = getObjectForState(getInformationAt(blockX, blockY));
        if (paintedSchem != null) {
            trackUndo(undoManagerOf(dimension));
            editableDatabase().setDataAt(blockX, blockY, NO_DATA);
            resetLastEdited();
            Point3i dims = paintedSchem.getDimensions();
            for (int tileX = (blockX - dims.x) >> TILE_SIZE_BITS; tileX <= (blockX
                    + dims.x) >> TILE_SIZE_BITS; tileX++) {
                for (int tileY = (blockY - dims.y) >> TILE_SIZE_BITS; tileY <= (blockY
                        + dims.y) >> TILE_SIZE_BITS; tileY++) {
                    repaintWorldpainterTile(tileX, tileY, dimension, database);
                }
            }
        }
    }

    /**
     * Forgets where houses stand, keeping the schematics: for a layer coming into
     * another world (from a layer file), whose house positions belong to the world
     * it was saved in. Without this the City Tool would show houses that are not on
     * this world's map.
     */
    public void forgetPlacements() {
        editableDatabase().clear();
        resetLastEdited();
    }

    public ArrayList<WPObject> getObjectList() {
        return objects;
    }

    public void setObjectList(ArrayList<WPObject> newObjects) {
        setObjectList(newObjects, Set.of());
    }

    void setObjectList(ArrayList<WPObject> newObjects, Set<Integer> preservedIndices) {
        for (int i : preservedIndices)
            if (i < getObjectList().size() && i < newObjects.size())
                copyObjectSettings(getObjectList().get(i), newObjects.get(i));

        for (int schematicIdx : positionsLosingHouses(newObjects, preservedIndices))
            editableDatabase().deleteAllWithValue(getValueForState(Direction.NORTH, false, schematicIdx), ID_BIT_MASK);

        this.objects = newObjects;
        resetLastEdited();
    }

    /**
     * The positions in the current schematic list whose houses
     * {@link #setObjectList(ArrayList, Set)} deletes for {@code newObjects}: houses
     * keep their schematic by its position, so a position that is gone or now
     * holds another file loses its houses. Removing a schematic shifts all below
     * it, so their houses go too.
     */
    // todo: map old to new positions (removing 5 of 10 need not clear 6..9)
    List<Integer> positionsLosingHouses(List<WPObject> newObjects, Set<Integer> preservedIndices) {
        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i < getObjectList().size(); i++) {
            WPObject newObj = i < newObjects.size() ? newObjects.get(i) : null;
            if (newObj == null) { // the position is gone
                positions.add(i);
                continue;
            }
            if (preservedIndices.contains(i)) // replaced on purpose: houses take the new schematic
                continue;
            File oldFile = getObjectList().get(i).getAttribute(ATTRIBUTE_FILE);
            File newFile = newObj.getAttribute(ATTRIBUTE_FILE);
            if (oldFile == null || !oldFile.equals(newFile)) // another file at this position
                positions.add(i);
        }
        return positions;
    }

    /** How many houses stand on the map with a schematic at one of {@code positions}. */
    int housesAt(Collection<Integer> positions) {
        int count = 0;
        for (ObjectState state : getAllObjectStates())
            if (positions.contains(state.objectIndex))
                count++;
        return count;
    }

    static void copyObjectSettings(WPObject source, WPObject target) {
        File targetFile = target.getAttribute(ATTRIBUTE_FILE);
        Map<String, Serializable> sourceAttributes = source.getAttributes();
        target.setName(source.getName());
        target.setAttributes(sourceAttributes == null ? null : new HashMap<>(sourceAttributes));
        target.setAttribute(ATTRIBUTE_FILE, targetFile);
    }

    public boolean isMirrored(int layerValue) {
        return 0 != ((layerValue & MIRROR_BIT_MASK) >> MIRROR_BIT_SHIFT);
    }

    public ObjectState getInformationAt(int blockX, int blockY) {
        int data = database.getDataAt(blockX, blockY);
        if (data == NO_DATA)
            return null;
        return new ObjectState(getRotation(data), isMirrored(data), getObjectIdx(data), blockX, blockY);
    }

    public ArrayList<ObjectState> getAllObjectStates() {
        ArrayList<ObjectState> states = new ArrayList<>();
        for (Point position : database.getAllData().keySet()) {
            ObjectState state = getInformationAt(position.x, position.y);
            if (state != null)
                states.add(state);
        }
        return states;
    }

    /**
     * returns an object that matches the type of scheamtic, with rotation and
     * mirror
     *
     * @param state
     * @return
     */
    public WPObject getObjectForState(ObjectState state) {
        if (state == null)
            return null;
        int objectIdx = state.objectIndex;
        if (objectIdx < 0 || objectIdx >= objects.size())
            return null;
        WPObject object = objects.get(objectIdx);

        boolean mirror = state.mirrored;
        if (mirror)
            object = new MirroredObject(object, true, DefaultPlugin.JAVA_ANVIL_1_20_5);

        Direction rotation = state.rotation;
        if (rotation != Direction.NORTH) {
            object = new RotatedObject(object, rotation.rotateSteps, DefaultPlugin.JAVA_ANVIL_1_20_5);
        }

        return object;
    }

    private WPObject getObjectForValue(int layerValue, Platform platform) {
        int objectIdx = getObjectIdx(layerValue);
        if (objectIdx == NO_DATA)
            return null;

        WPObject object = objects.get(objectIdx);

        boolean mirror = isMirrored(layerValue);
        if (mirror)
            object = new MirroredObject(object, true, platform);

        Direction rotation = getRotation(layerValue);
        if (rotation != Direction.NORTH) {
            object = new RotatedObject(object, rotation.rotateSteps, platform);
        }

        return object;
    }

    private int getObjectIdx(int layerValue) {
        if (layerValue == NO_DATA)
            return NO_DATA;
        int idBits = (layerValue & ID_BIT_MASK) >> ID_BIT_SHIFT;
        int index = idBits;
        if (objects.size() > index)
            return index;
        return NO_DATA;
    }

    private int getValueForState(Direction rotation, boolean isMirror, int schematicIdx) {
        return rotation.ordinal() << CityLayer.ROTATION_BIT_SHIFT | (isMirror ? 1 : 0) << CityLayer.MIRROR_BIT_SHIFT
                | schematicIdx << CityLayer.ID_BIT_SHIFT;
    }

    private Direction getRotation(int layerValue) {
        int idBits = ((layerValue & ROTATION_BIT_MASK) >> ROTATION_BIT_SHIFT);
        assert idBits >= 0 && idBits <= 3 : String.format("bits were not in range 0..3: %d", idBits);
        Direction rot = Direction.values()[idBits];
        assert rot.rotateSteps == idBits;
        return rot;
    }

    @Override
    public Class<? extends LayerExporter> getExporterType() {
        return CityLayerExporter.class;
    }

    @Override
    public LayerExporter getExporter(Dimension dimension, Platform platform, ExporterSettings settings) {
        return new CityLayerExporter(dimension, platform, settings, this);
    }

    public void repaintAll(Dimension dim) {
        if (dim == null)
            return;
        try {
            if (!dim.isEventsInhibited()) {
                dim.setEventsInhibited(true);
            }
            for (Tile t : dim.getTiles()) {
                repaintWorldpainterTile(t.getX(), t.getY(), dim, database);
            }
        } catch (Exception exception) {

        } finally {
            if (dim.isEventsInhibited()) {
                dim.setEventsInhibited(false);
            }
        }
    }

    @Override
    public void savePointArmed() {
    }

    @Override
    public void savePointCreated() {
    }

    @Override
    public void undoPerformed() {
    }

    @Override
    public void redoPerformed() {
    }

    /**
     * Undo or redo changed the house list: take the manager's copy for this step
     * (the tiles' layer data is restored by WorldPainter itself).
     */
    @Override
    public void bufferChanged(BufferKey<?> key) {
        if (undoManager == null || key != undoKey)
            return;
        CityInfoDatabase restored = undoManager.getBuffer(undoKey);
        // undone to before the list was registered: it was as registered then
        database = restored != null ? restored : undoBaseline;
        resetLastEdited();
        if (afterUndoRestored != null)
            afterUndoRestored.run();
    }

    /**
     * {@code action} runs after undo / redo restored the house list (the undo
     * manager tells its general listeners before it restores buffers).
     */
    public void setAfterUndoRestored(Runnable action) {
        afterUndoRestored = action;
    }

    /**
     * Registers the house list with {@code manager} (the undo manager of the
     * dimension being edited), so undo / redo take it along with the tiles. Moving
     * to another manager (another world or dimension) leaves the old one. A null
     * manager (none, e.g. in tests) changes nothing.
     */
    public void trackUndo(UndoManager manager) {
        if (manager == null || manager == undoManager)
            return;
        if (undoManager != null)
            undoManager.removeBuffer(undoKey);
        undoManager = manager;
        undoKey = new BufferKey<>() {
            @Override
            public String toString() {
                return "CityLayer houses of " + getName();
            }
        };
        undoBaseline = database;
        manager.addBuffer(undoKey, database, this);
        // the next change starts a new undo step: the registered list stays as it was
        manager.armSavePoint();
    }

    /**
     * Clears the undo history of {@code dimension} (and of the undo manager the
     * house list is registered with, if another one): after the schematic list
     * changed, the house lists of earlier undo steps name schematics by positions
     * that now hold others or none, so undo would bring houses back as the wrong
     * schematic or as invisible ones. The current state stays; the next change
     * starts a new undo step.
     */
    public void clearUndoHistory(Dimension dimension) {
        UndoManager dimensionManager = undoManagerOf(dimension);
        if (dimension != null)
            dimension.clearUndo();
        if (undoManager != null && undoManager != dimensionManager)
            undoManager.clear();
        // clear() disarms the save point: without this the next action would
        // change the remaining step in place and could not be undone
        if (dimensionManager != null)
            dimensionManager.armSavePoint();
        if (undoManager != null && undoManager != dimensionManager)
            undoManager.armSavePoint();
        undoBaseline = database;
    }

    /** The undo manager the house list is registered with (null: none). */
    UndoManager trackedUndoManager() {
        return undoManager;
    }

    /**
     * The house list to change: with undo, the manager's copy for the current step
     * (it keeps the previous one), else the list itself.
     */
    private CityInfoDatabase editableDatabase() {
        if (undoManager == null)
            return database;
        CityInfoDatabase editable = undoManager.getBufferForEditing(undoKey);
        if (editable == null) {
            // The list joined the history at the first tracked edit (addBuffer puts
            // it in the newest step only). Undone past that step and edited again,
            // the step that had it is gone and the manager has no list to copy: it
            // hands back null. Here the houses are as they were before tracking
            // (bufferChanged showed undoBaseline): add a copy of that to the current
            // step. A copy, so undoBaseline stays what earlier steps fall back to.
            editable = undoBaseline != null ? undoBaseline.clone() : new CityInfoDatabase();
            undoManager.addBuffer(undoKey, editable, this);
        }
        database = editable;
        return database;
    }

    /**
     * The undo manager of {@code dimension} (WorldPainter keeps it private): null
     * if it has none or cannot be reached.
     */
    public static UndoManager undoManagerOf(Dimension dimension) {
        if (dimension == null)
            return null;
        try {
            Field field = Dimension.class.getDeclaredField("undoManager");
            field.setAccessible(true);
            return (UndoManager) field.get(dimension);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return null;
        }
    }

    @Override
    public CityLayer clone() {
        CityLayer clone = new CityLayer(this.getName(), this.getDescription());
        ArrayList<WPObject> cloneList = new ArrayList<>();
        for (WPObject original : this.getObjectList()) {
            cloneList.add(original.clone());
        }
        clone.setObjectList(cloneList);
        assert clone.database.isEmpty();
        return clone;
    }

    public enum Direction {
        NORTH(0), EAST(1), SOUTH(2), WEST(3);
        final int rotateSteps;

        Direction(int rotateSteps) {
            this.rotateSteps = rotateSteps;
        }

        public int toCompass() {
            return rotateSteps * 90;
        }

        public static Direction fromCompass(int degrees) {
            if (degrees <= 45)
                return NORTH;
            else if (degrees <= 45 + 90) {
                return EAST;
            } else if (degrees <= 45 + 180) {
                return SOUTH;
            } else if (degrees <= 45 + 270) {
                return WEST;
            } else {
                return NORTH;
            }
        }

        /**
         * A quarter turn clockwise on screen: the next step of WorldPainter's
         * RotatedObject, which turns clockwise.
         */
        public CityLayer.Direction nextRotation() {
            return fromCompass(this.rotateSteps * 90 + 90);
        }

        /** A quarter turn counter-clockwise on screen. */
        public CityLayer.Direction counterClockwise() {
            return fromCompass((this.rotateSteps * 90 + 270) % 360);
        }
    }
}
