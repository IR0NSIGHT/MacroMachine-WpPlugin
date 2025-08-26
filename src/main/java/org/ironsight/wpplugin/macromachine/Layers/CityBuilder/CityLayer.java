package org.ironsight.wpplugin.macromachine.Layers.CityBuilder;

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
import org.pepsoft.worldpainter.objects.MirroredObject;
import org.pepsoft.worldpainter.objects.RotatedObject;
import org.pepsoft.worldpainter.objects.WPObject;

import javax.vecmath.Point3i;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.RescaleOp;
import java.io.Serial;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE_BITS;

public class CityLayer extends CustomLayer implements UndoListener {
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

    public CityLayer(String name, String description) {
        super(name, description, DataSize.NIBBLE, 50, Color.cyan);
    }

    public void setDataAt(Dimension dimension, int blockX, int blockY, Direction rotation, boolean mirror, int schematicIdx) {
        database.setDataAt(blockX, blockY, rotation, mirror, schematicIdx);

        WPObject paintedSchem = getObjectForState(new ObjectState(rotation,mirror,schematicIdx));
        if (paintedSchem == null)
            return;
        Point3i dims = paintedSchem.getDimensions();
        for (int tileX =(blockX - dims.x) >> TILE_SIZE_BITS; tileX <= (blockX + dims.x) >> TILE_SIZE_BITS; tileX++) {
            for (int tileY =(blockY - dims.y) >> TILE_SIZE_BITS; tileY <= (blockY + dims.y) >> TILE_SIZE_BITS; tileY++) {
                repaintTile(tileX,tileY, dimension, database);
            }
        }

    }

    public Image getSchematicImage(ObjectState state) {
        WPObject schematic = getObjectForState(state);
        if (schematic == null)
            return null;

        Point3i dims = schematic.getDimensions();
        int space = 3;
        BufferedImage img = new BufferedImage(2 * space + dims.x, 2 * space + dims.y, TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        paintObjectToImage(g2d, space, space, schematic);
        g2d.dispose();
        return img;
    }

    private void paintObjectToImage(Graphics2D graphics2D, int width, int heightPx, WPObject object) {
        int height = object.getDimensions().z;
        for (int x = 0; x < object.getDimensions().x; x++) {
            for (int y = 0; y < object.getDimensions().y; y++) {
                int totalPosY = y;
                for (int z = object.getDimensions().z - 1; z >= 0; z--) {
                    Material mat = object.getMaterial(x, y, z);
                    if (mat != Material.AIR) {
                        double value = (Math.sqrt(1f * z / height)) * 0.5f + 0.5f;
                        Color base = new Color(mat.colour);
                        Color darkenFactor = new Color((int) (Math.max(0,Math.min(255, value * base.getRed()))), (int) (Math.max(0,Math.min(255, value * base.getGreen()))),
                                (int) (Math.max(0,Math.min(255,
                                value * base.getBlue()))));
                        graphics2D.setColor(darkenFactor);
                        graphics2D.fillRect(x + width, heightPx + totalPosY, 1, 1);
                        break;
                    }
                }
            }
        }
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
                        int existing = tile.getLayerValue(this,totalPosX,totalPosY);
                        value = Math.min(15,value + existing);
                        tile.setLayerValue(this, totalPosX, totalPosY, value);
                        break;
                    }
                }
            }
        }
    }

    private void repaintTile(int tileX, int tileY, Dimension dimension, CityInfoDatabase database) {
        // FIXME: all objects must check if they cross tile borders and must be painted into multiple tiles.

        Tile tile = dimension.getTileForEditing(tileX, tileY);
        tile.clearLayerData(this);

        //find all objects that live in tile
        for (int tileXX = tileX - 1; tileXX <= tileX + 1; tileXX++)
            for (int tileYY = tileY - 1; tileYY <= tileY + 1; tileYY++) {
                HashMap<Point, Integer> tileData = database.getTileData(tileXX, tileYY);
                if (tileData == null)
                    continue;
                for (Map.Entry<Point, Integer> buildingInfo : tileData.entrySet()) {
                    int data = buildingInfo.getValue();
                    WPObject object = getObjectForValue(data, DefaultPlugin.JAVA_ANVIL_1_20_5);
                    if (object == null)
                        continue;
                    paintObjectIntoTile(tile, object, buildingInfo.getKey());
                }
            }
    }

    public void removeDataAt(Dimension dimension, int blockX, int blockY) {
        WPObject paintedSchem =  getObjectAt(dimension,blockX,blockY, DefaultPlugin.JAVA_ANVIL_1_20_5);
        if (paintedSchem != null) {
            database.setDataAt(blockX, blockY, Direction.NORTH, false, -1);

            Point3i dims = paintedSchem.getDimensions();
            for (int tileX =(blockX - dims.x) >> TILE_SIZE_BITS; tileX <= (blockX + dims.x) >> TILE_SIZE_BITS; tileX++) {
                for (int tileY =(blockY - dims.y) >> TILE_SIZE_BITS; tileY <= (blockY + dims.y) >> TILE_SIZE_BITS; tileY++) {
                    repaintTile(tileX,tileY, dimension, database);
                }
            }
        }
    }

    public int getItemIndexAt(int blockX, int blockY) {
        return getObjectIdx(database.getDataAt(blockX, blockY));
    }

    public ArrayList<WPObject> getObjectList() {
        return objects;
    }

    public void setObjectList(ArrayList<WPObject> objects) {
        this.objects = objects;
    }

    public boolean isMirrored(int layerValue) {
        return 0 != ((layerValue & MIRROR_BIT_MASK) >> MIRROR_BIT_SHIFT);
    }

    public WPObject getObjectAt(Dimension dim, int blockX, int blockY, Platform platform) {
        int layerValue = database.getDataAt(blockX, blockY);
        if (layerValue < 0)
            return null;
        return getObjectForValue(layerValue, platform);
    }

    public ObjectState getInformationAt(int blockX, int blockY) {
        int data = database.getDataAt(blockX, blockY);
        return new ObjectState(getRotation(data), isMirrored(data), getObjectIdx(data));
    }

    private WPObject getObjectForState(ObjectState state) {
        int objectIdx = state.objectIndex;
        if (objectIdx <0 || objectIdx >= objects.size())
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
        if (objectIdx == -1)
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
        if (layerValue < 0)
            return -1;
        int idBits = (layerValue & ID_BIT_MASK) >> ID_BIT_SHIFT;
        int index = idBits;
        if (objects.size() > index)
            return index;
        return -1;
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

    @Override
    public void savePointArmed() {
        System.out.println("SAVE POINT ARMED");
    }

    @Override
    public void savePointCreated() {
        System.out.println("SAVE POINT CREATED");
    }

    @Override
    public void undoPerformed() {
        System.out.println("UNDO PERFORMED");
    }

    @Override
    public void redoPerformed() {
        System.out.println("REDO PERFORMED");
    }

    @Override
    public void bufferChanged(BufferKey<?> key) {
        System.out.println("BUFFER CHANGED " + key);
    }

    public void registerLayer(UndoManager manager) {
        manager.addListener(this);
    }

    @Override
    public CustomLayer clone() {
        CityLayer clone = new CityLayer(this.getName(), this.getDescription());
        ArrayList<WPObject> cloneList = new ArrayList<>();
        for (WPObject original : this.getObjectList()) {
            cloneList.add(original.clone());
        }
        clone.setObjectList(cloneList);
        assert clone.database.isEmpty();
        return clone;
    }

    enum Direction {
        NORTH(0),
        EAST(1),
        SOUTH(2),
        WEST(3);
        final int rotateSteps;

        Direction(int rotateSteps) {
            this.rotateSteps = rotateSteps;
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

        public CityLayer.Direction nextRotation() {
            return fromCompass(this.rotateSteps * 90 + 90);
        }
    }
}
