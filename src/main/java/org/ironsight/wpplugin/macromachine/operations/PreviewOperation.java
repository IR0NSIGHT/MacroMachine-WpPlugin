package org.ironsight.wpplugin.macromachine.operations;

import static org.ironsight.wpplugin.macromachine.threeDRendering.Export3DViewHelper.renderTileToSurfaceObject;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE_BITS;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.ironsight.cubearray.render.CubeSetup;
import org.ironsight.cubearray.render.InstancedCubes;
import org.ironsight.cubearray.render.InstancedCubes.CameraState;
import org.ironsight.cubearray.edit.BlockReplacer;
import org.ironsight.cubearray.schematic.SchemReader;
import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.CoordinateTransform;
import org.pepsoft.worldpainter.objects.MinecraftWorldObject;
import org.joml.Vector3f;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.operations.AbstractBrushOperation;

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

    float[][] height = new float[0][];
    float[][] waterHeight = new float[0][];
    Material[][] terrain = new Material[0][];
    private Rectangle lastExtent = new Rectangle(0, 0, 0, 0);
    private TileChangedListener listener = new TileChangedListener(this);
    private ArrayList<Tile> tilesInExtent = new ArrayList<>();
    private volatile InstancedCubes renderer;
    private Set<Point> lastTileCoords;
    private Dimension lastDim;
    private Platform platform;
    private JButton rerenderButton;
    private JLabel statusLabel;
    private JCheckBox autoUpdateCheckbox;
    private JButton saveScreenshotButton;
    private JButton openScreenshotFolderButton;
    private JButton hideGridButton;
    private JLabel cameraStateLabel;
    private JButton setCameraButton;
    private JSpinner yawSpinner;
    private JSpinner pitchSpinner;
    private JSpinner radiusSpinner;
    private int lastClickX;
    private int lastClickZ;
    private MinecraftWorldObject lastRenderedObject;
    private Set<Point> subscribedTileCoords = new HashSet<>();
    private boolean showGrid = true;
    private InstancedCubes.CameraState prevCameraState;
    private boolean selectCameraPos;
    private float heightAboveGround;
    private final JPanel optionsPanel;

    public PreviewOperation() {
        super(NAME, DESCRIPTION, ID);
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
        cameraStateLabel = new JLabel("Camera: —");
        cameraStateLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        optionsPanel.add(cameraStateLabel);
        optionsPanel.add(Box.createVerticalStrut(4));
        autoUpdateCheckbox = new JCheckBox("Auto Update");
        autoUpdateCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        optionsPanel.add(autoUpdateCheckbox);
        optionsPanel.add(Box.createVerticalStrut(8));
        saveScreenshotButton = new JButton("Save Screenshot");
        saveScreenshotButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        saveScreenshotButton.addActionListener(e -> {
            InstancedCubes r = renderer;
            if (r != null) {
                r.requestScreenshot().thenAcceptAsync(PreviewOperation.this::saveScreenshot);
            }
        });
        optionsPanel.add(saveScreenshotButton);
        optionsPanel.add(Box.createVerticalStrut(4));
        openScreenshotFolderButton = new JButton("Open Screenshot Folder");
        openScreenshotFolderButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        openScreenshotFolderButton.addActionListener(e -> {
            Dimension dim = getDimension();
            if (dim == null) return;
            String worldName = dim.getWorld().getName();
            String safeName = worldName.replaceAll("[\\\\/:*?\"<>|]", "_");
            File screenshotsDir = new File(MacroContainer.getActionsFilePath(),
                    "screenshots" + File.separator + safeName);
            screenshotsDir.mkdirs();
            try {
                Desktop.getDesktop().open(screenshotsDir);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
        optionsPanel.add(openScreenshotFolderButton);
        optionsPanel.add(Box.createVerticalStrut(8));
        hideGridButton = new JButton("Hide Grid");
        hideGridButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        hideGridButton.setEnabled(false);
        hideGridButton.addActionListener(e -> toggleGrid());
        optionsPanel.add(hideGridButton);
        optionsPanel.add(Box.createVerticalStrut(8));
        JPanel cameraPanel = new JPanel(new GridBagLayout());
        cameraPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 2, 2, 2);
        c.gridx = 0;
        c.gridy = 0;
        cameraPanel.add(new JLabel("Yaw"), c);
        c.gridx = 1;
        yawSpinner = new JSpinner(new SpinnerNumberModel(0.0, -360.0, 360.0, 1.0));
        yawSpinner.setPreferredSize(new java.awt.Dimension(80, 24));
        cameraPanel.add(yawSpinner, c);
        c.gridx = 2;
        c.gridy = 0;
        cameraPanel.add(new JLabel("Pitch"), c);
        c.gridx = 3;
        pitchSpinner = new JSpinner(new SpinnerNumberModel(30.0, -90.0, 90.0, 1.0));
        pitchSpinner.setPreferredSize(new java.awt.Dimension(80, 24));
        cameraPanel.add(pitchSpinner, c);
        c.gridx = 4;
        c.gridy = 0;
        cameraPanel.add(new JLabel("Radius"), c);
        c.gridx = 5;
        radiusSpinner = new JSpinner(new SpinnerNumberModel(100.0, 1.0, 10000.0, 10.0));
        radiusSpinner.setPreferredSize(new java.awt.Dimension(80, 24));
        cameraPanel.add(radiusSpinner, c);
        c.gridx = 6;
        setCameraButton = new JButton("Set Camera Pos");
        setCameraButton.addActionListener(e -> {
            selectCameraPos = true;
            statusLabel.setText("Click on map to set camera position");
        });
        cameraPanel.add(setCameraButton, c);
        optionsPanel.add(cameraPanel);
        optionsPanel.add(Box.createVerticalStrut(8));
        JButton saveSchematicButton = new JButton("Save As Schematic");
        saveSchematicButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        saveSchematicButton.addActionListener(e -> saveSchematic());
        optionsPanel.add(saveSchematicButton);

        new Timer(500, e -> {
            InstancedCubes r = renderer;
            if (r != null) {
                var cs = r.getCameraState();
                if (cs != null && lastRenderedObject != null) {
                    double yawDeg = Math.toDegrees(cs.yaw());
                    double pitchDeg = Math.toDegrees(cs.pitch());
                    double radius = cs.radius();
                    yawSpinner.setValue(yawDeg);
                    pitchSpinner.setValue(pitchDeg);
                    radiusSpinner.setValue(radius);
                    javax.vecmath.Point3i wp = sceneToWP(cs.target().x, cs.target().y, cs.target().z);
                    if (!cs.equals(prevCameraState)) {
                        System.out.println("[PreviewOp] camera - scene target (" + cs.target().x + ", " + cs.target().y + ", " + cs.target().z
                            + ") -> WP (" + wp.x + ", " + wp.y + ", " + wp.z + ")");
                        prevCameraState = cs;
                    }
                    cameraStateLabel.setText(String.format("Pos: (%.1f, %.1f, %.1f)  WP: (%d, %d, %d)  Yaw: %.1f°  Pitch: %.1f°  Radius: %.1f",
                            cs.target().x, cs.target().y, cs.target().z,
                            wp.x, wp.y, wp.z, yawDeg, pitchDeg, radius));
                }
            } else {
                cameraStateLabel.setText("Camera: —");
            }
        }).start();
    }

    @Override
    public JPanel getOptionsPanel() {
        return optionsPanel;
    }

    private void rerenderLastSelection(ActionEvent e) {
        if (lastTileCoords == null || lastTileCoords.isEmpty() || lastDim == null) {
            return;
        }
        statusLabel.setText("Loading...");
        HashSet<Tile> tiles = new HashSet<>();
        for (Point coord : lastTileCoords) {
            Tile liveTile = lastDim.getTile(coord.x, coord.y);
            if (liveTile != null) {
                tiles.add(liveTile.transform(CoordinateTransform.NOOP));
            }
        }
        if (tiles.isEmpty()) {
            statusLabel.setText("Idle");
            return;
        }
        statusLabel.setText("Rendering...");
        Runnable task = () -> {
            try {
                var schemObj = renderTileToSurfaceObject(tiles, lastDim);
                lastRenderedObject = schemObj;
                org.pepsoft.util.Box vol = schemObj.getVolume();
                javax.vecmath.Point3i off = schemObj.getOffset();
                System.out.println("[PreviewOp] rerenderLastSelection: vol(x1=" + vol.getX1() + " x2=" + vol.getX2()
                    + " y1=" + vol.getY1() + " y2=" + vol.getY2() + " z1=" + vol.getZ1() + " z2=" + vol.getZ2()
                    + ") off(" + off.x + ", " + off.y + ", " + off.z + ")");
                CubeSetup setup = SchemReader.prepareData(List.of(schemObj), showGrid);
                if (renderer != null) {
                    renderer.replaceData(setup);
                }
                SwingUtilities.invokeLater(() -> statusLabel.setText("Idle"));
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        };
        new Thread(task).start();
    }

    // HIGHLIGHT AREA
    public static final Layer annotationLayer = new AnnotationLayer("macroMachine_3dpreview_annotationlayer",
            "3d " + "Preview", "Shows " + "what the 3d preview is currently " + "rendering", Layer.DataSize.BIT, true,
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
        lastClickX = centreX;
        lastClickZ = centreY;
        System.out.println("[PreviewOp] tick - world pos: (" + centreX + ", " + centreY + ")");

        if (selectCameraPos) {
            selectCameraPos = false;
            InstancedCubes r = renderer;
            if (r != null && lastRenderedObject != null && lastDim != null) {
                CameraState cs = r.getCameraState();
                javax.vecmath.Point3i currentWP = sceneToWP(cs.target().x, cs.target().y, cs.target().z);
                float currentTerrainHeight = lastDim.getHeightAt(currentWP.x, currentWP.z);
                heightAboveGround = currentWP.y - currentTerrainHeight;
                float newTerrainHeight = lastDim.getHeightAt(centreX, centreY);
                int worldY = Math.round(newTerrainHeight + heightAboveGround);
                Vector3f target = wpToScene(centreX, centreY, worldY);
                System.out.println("[PreviewOp] SetCameraPos: WP (" + centreX + ", " + centreY + ", " + worldY
                    + ") -> scene (" + target.x + ", " + target.y + ", " + target.z + ")");
                System.out.println("[PreviewOp] SetCameraPos: prev target (" + cs.target().x + ", " + cs.target().y + ", " + cs.target().z + ")");
                cs.target().x = target.x;
                cs.target().y = target.y;
                cs.target().z = target.z;
                System.out.println("[PreviewOp] SetCameraPos: new target (" + cs.target().x + ", " + cs.target().y + ", " + cs.target().z + ")");
            }
            statusLabel.setText("Idle");
            return;
        }

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

                int tileCenterBlockX = x * TILE_SIZE + TILE_SIZE / 2;
                int tileCenterBlockY = y * TILE_SIZE + TILE_SIZE / 2;

                if (!selectedSquare.contains(tileCenterBlockX, tileCenterBlockY)) {
                    System.out.printf("reject chunk %d,%d because center %x %d not in extent %s", x, y,
                            tileCenterBlockX, tileCenterBlockY, selectedSquare.toString());
                    continue;
                }

                Tile t = dim.getTile(x, y);
                assert t != null;
                tiles.add(t);
            }
        }

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

        statusLabel.setText("Loading...");

        HashSet<Tile> clonedTiles = new HashSet<>();
        for (Tile tile : tiles) {
            clonedTiles.add(tile.transform(CoordinateTransform.NOOP));
        }

        statusLabel.setText("Rendering...");

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

        Runnable task = () -> {
            try {
                var schemObj = renderTileToSurfaceObject(clonedTiles, dim);
                lastRenderedObject = schemObj;
                org.pepsoft.util.Box vol = schemObj.getVolume();
                javax.vecmath.Point3i off = schemObj.getOffset();
                System.out.println("[PreviewOp] tick render: vol(x1=" + vol.getX1() + " x2=" + vol.getX2()
                    + " y1=" + vol.getY1() + " y2=" + vol.getY2() + " z1=" + vol.getZ1() + " z2=" + vol.getZ2()
                    + ") off(" + off.x + ", " + off.y + ", " + off.z + ")");
                CubeSetup setup = SchemReader.prepareData(List.of(schemObj), showGrid);
                if (renderer == null) {
                    renderer = new InstancedCubes(setup);
                    hideGridButton.setEnabled(true);
                    renderer.run();
                    renderer = null;
                    SwingUtilities.invokeLater(() -> hideGridButton.setEnabled(false));
                } else {
                    renderer.replaceData(setup);
                }
                SwingUtilities.invokeLater(() -> statusLabel.setText("Idle"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
        new Thread(task).start();

    }

    private Vector3f wpToScene(int worldX, int worldZ, int worldY) {
        org.pepsoft.util.Box vol = lastRenderedObject.getVolume();
        javax.vecmath.Point3i off = lastRenderedObject.getOffset();
        float sx = worldX - vol.getX1() + off.x;
        float sy = worldY - vol.getZ1() + off.z;
        float sz = worldZ - vol.getY1() + off.y;
        System.out.println("[PreviewOp] wpToScene: vol(x1=" + vol.getX1() + " y1=" + vol.getY1() + " z1=" + vol.getZ1()
            + ") off(" + off.x + ", " + off.y + ", " + off.z
            + ")  WP(" + worldX + ", " + worldZ + ", " + worldY
            + ") -> scene(" + sx + ", " + sy + ", " + sz + ")");
        return new Vector3f(sx, sy, sz);
    }

    private javax.vecmath.Point3i sceneToWP(float sx, float sy, float sz) {
        org.pepsoft.util.Box vol = lastRenderedObject.getVolume();
        javax.vecmath.Point3i off = lastRenderedObject.getOffset();
        int wx = Math.round(sx - off.x + vol.getX1());
        int wz = Math.round(sz - off.y + vol.getY1());
        int wy = Math.round(sy - off.z + vol.getZ1());
        System.out.println("[PreviewOp] sceneToWP: vol(x1=" + vol.getX1() + " y1=" + vol.getY1() + " z1=" + vol.getZ1()
            + ") off(" + off.x + ", " + off.y + ", " + off.z
            + ")  scene(" + sx + ", " + sy + ", " + sz
            + ") -> WP(" + wx + ", " + wz + ", " + wy + ")");
        return new javax.vecmath.Point3i(wx, wz, wy);
    }

    private void toggleGrid() {
        showGrid = !showGrid;
        hideGridButton.setText(showGrid ? "Hide Grid" : "Show Grid");
        InstancedCubes r = renderer;
        if (r == null || lastRenderedObject == null)
            return;
        new Thread(() -> {
            try {
                CubeSetup setup = SchemReader.prepareData(List.of(lastRenderedObject), showGrid);
                r.replaceData(setup);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }).start();
    }

    private void saveSchematic() {
        MinecraftWorldObject obj = lastRenderedObject;
        if (obj == null) return;
        statusLabel.setText("Saving schematic...");
        Runnable task = () -> {
            try {
                javax.vecmath.Point3i dims = obj.getDimensions();
                int w = dims.x;
                int l = dims.y;
                int h = dims.z;
                pitheguy.schemconvert.converter.Schematic.Builder builder =
                        new pitheguy.schemconvert.converter.Schematic.Builder(null, 1343, w, h, l);
                for (int x = 0; x < w; x++) {
                    for (int y = 0; y < l; y++) {
                        for (int z = 0; z < h; z++) {
                            Material mat = obj.getMaterial(x, y, z);
                            if (mat != null) {
                                builder.setBlockAt(x, z, y, mat.toFullString());
                            }
                        }
                    }
                }
                Dimension dim = getDimension();
                String worldName = (dim != null) ? dim.getWorld().getName() : "unknown";
                String safeName = worldName.replaceAll("[\\\\/:*?\"<>|]", "_");
                File schemDir = new File(MacroContainer.getActionsFilePath(),
                        "schematics" + File.separator + safeName);
                schemDir.mkdirs();
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
                File outputFile = new File(schemDir, worldName + "_" + timestamp + ".schem");
                BlockReplacer.write(builder.build(), outputFile);
                SwingUtilities.invokeLater(() -> statusLabel.setText("Schematic saved"));
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        };
        new Thread(task).start();
    }

    private void saveScreenshot(BufferedImage image) {
        try {
            Dimension dim = getDimension();
            if (dim == null) return;
            String worldName = dim.getWorld().getName();
            String safeName = worldName.replaceAll("[\\\\/:*?\"<>|]", "_");
            File screenshotsDir = new File(MacroContainer.getActionsFilePath(),
                    "screenshots" + File.separator + safeName);
            screenshotsDir.mkdirs();
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            File outputFile = new File(screenshotsDir, "screenshot_" + timestamp + ".png");
            ImageIO.write(image, "PNG", outputFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static class TileChangedListener implements Tile.Listener
    {
        private PreviewOperation op;
        private Timer debounceTimer;

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
