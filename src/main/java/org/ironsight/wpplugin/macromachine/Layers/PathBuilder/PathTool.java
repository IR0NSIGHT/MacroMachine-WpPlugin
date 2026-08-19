package org.ironsight.wpplugin.macromachine.Layers.PathBuilder;

import static java.lang.Math.PI;
import static org.ironsight.wpplugin.macromachine.Gui.HelpDialog.getHelpButton;
import static org.ironsight.wpplugin.macromachine.Layers.PathBuilder.PathToolBackend.*;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE_BITS;

import java.awt.*;
import java.beans.PropertyVetoException;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.vecmath.Point3i;
import javax.vecmath.Point4f;
import org.ironsight.wpplugin.macromachine.operations.PreviewOperation;
import org.pepsoft.util.AttributeKey;
import org.pepsoft.util.undo.BufferKey;
import org.pepsoft.util.undo.Cloneable;
import org.pepsoft.util.undo.UndoListener;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.brushes.Brush;
import org.pepsoft.worldpainter.operations.AbstractBrushOperation;
import org.pepsoft.worldpainter.operations.PaintOperation;
import org.pepsoft.worldpainter.painting.Paint;

public class PathTool extends AbstractBrushOperation implements PaintOperation, UndoListener
{
    private enum TerrainMode
    {
        DONT_CHANGE("Don't change terrain"),
        FLATTEN("Flatten terrain"),
        CARVE("Carve terrain");

        private final String label;

        TerrainMode(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static final String help = """
            PathTool will connect clicked positions into a path and smoothly blend them into existing terrain, based on the brush you are using.

            Right click: Start new path at this position
            Left click: Advance current path to this position

            """;
    // use flat list of floats to not create any serialization dependecies to custom
    // classes. use like a float buffer
    private static final AttributeKey<ArrayList<Float>> PATHHANDLES_KEY = new AttributeKey<>("PATHTOOL-PATHHANDLES",
            new ArrayList<Float>());
    private final JPanel optionsPanel = new JPanel();
    ArrayList<Point4f> pathHandles = new ArrayList<>();
    HashMap<Point3i, FloatTile> cachedTiles = new HashMap<>();
    private Paint paint;
    // only allow downwards movement
    private boolean onlyDown;
    private boolean snapToTerrain;
    private boolean fixHeightTo;
    private boolean usePaint;
    private boolean setWaterHeight;
    private TerrainMode terrainMode = TerrainMode.FLATTEN;
    private float slopeLimit = 0;
    private float handleStrength = 0f;
    private JCheckBox onlyDownCheckbox;
    private JCheckBox minCheckbox;
    private JCheckBox fixHeightCheckbox;
    private JCheckBox usePaintCheckbox;
    private JCheckBox setWaterHeightCheckbox;
    private JComboBox<TerrainMode> terrainModeDropdown;
    private JSpinner limitSlopeSpinner;
    private JSpinner handleFactorSpinner;
    private JSpinner transitionMultiSpinner;
    private CellPreviewPanel slopePreview;
    private JPanel brushQuerschnitt;
    private JComboBox<CrossSectionShape> profilesDropdown;
    private CrossSectionShape brushProfile;
    private float transitionMultiplier = 2;
    private JButton applyButton;

    public PathTool() {
        super("Road Tool", "Create smooth roads", "MacroMachine_RoadTool"); // ONE SHOT OP
        init();
    }

    @Override
    protected void activate() throws PropertyVetoException {
        super.activate();
        this.pathHandles.clear();
        this.cachedTiles.clear();
    }

    @Override
    protected void deactivate() {
        super.deactivate();
        this.pathHandles.clear();
        this.cachedTiles.clear();
    }

    private void init() {
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        optionsPanel.setBorder(new EmptyBorder(8, 8, 8, 8));
        optionsPanel.add(getHelpButton("Road Tool", help));
        {
            JPanel presetPanel = new JPanel();
            JButton riverPresetButton = new JButton("River preset");
            riverPresetButton.addActionListener(l -> applyPreset(true, true, false, true, TerrainMode.CARVE, 1f, 5f, 0f,
                    null));
            presetPanel.add(riverPresetButton);
            JButton roadPresetButton = new JButton("Road preset");
            roadPresetButton
                    .addActionListener(l -> applyPreset(false, true, true, false, TerrainMode.FLATTEN, .3f, 1f, 4f,
                            "Triangle"));
            presetPanel.add(roadPresetButton);
            optionsPanel.add(presetPanel);
        }
        {
            applyButton = new JButton("Apply");
            applyButton.setName("PathTool.Apply");
            applyButton.setToolTipText("apply the path to the map.");
            applyButton.addActionListener((e) -> {
                var dim = getDimension();
                if (dim == null)
                    return;
                if (!dim.isEventsInhibited())
                    dim.setEventsInhibited(true);

                this.OnSmoothPath(pathHandles, false); // generate path

                if (dim.isEventsInhibited())
                    dim.setEventsInhibited(false);
            });
            optionsPanel.add(applyButton);
        }

        {
            onlyDownCheckbox = new JCheckBox("onlyDownCheckbox");
            onlyDownCheckbox.setToolTipText(
                    "if active, the path will only move horizontally or downwards, but never higher than the last used coordinate. righclick to reset height");
            onlyDownCheckbox.addActionListener(l -> {
                this.onlyDown = onlyDownCheckbox.isSelected();
            });
            this.onlyDown = onlyDownCheckbox.isSelected();
            optionsPanel.add(onlyDownCheckbox);
        }
        {
            minCheckbox = new JCheckBox("snap to terrain");
            minCheckbox.setToolTipText("if active, the path will follow the terrain instead of building 'bridges'.");
            minCheckbox.addActionListener(l -> {
                this.snapToTerrain = minCheckbox.isSelected();
            });
            this.snapToTerrain = minCheckbox.isSelected();
            optionsPanel.add(minCheckbox);
        }
        {
            usePaintCheckbox = new JCheckBox("usePaintCheckbox");
            usePaintCheckbox.setSelected(true);
            usePaintCheckbox.setToolTipText(
                    "if active, the path will paint the current selected layer/terrain where the filter strength is 100% (red area in the cross-section)");
            usePaintCheckbox.addActionListener(l -> {
                this.usePaint = usePaintCheckbox.isSelected();
            });
            this.usePaint = usePaintCheckbox.isSelected();
            optionsPanel.add(usePaintCheckbox);
        }
        {
            setWaterHeightCheckbox = new JCheckBox("Set water height");
            setWaterHeightCheckbox.setToolTipText("Water height will be set to terrain height.");
            setWaterHeightCheckbox.addActionListener(l -> {
                this.setWaterHeight = setWaterHeightCheckbox.isSelected();
            });
            optionsPanel.add(setWaterHeightCheckbox);
        }
        {
            terrainModeDropdown = new JComboBox<>(TerrainMode.values());
            terrainModeDropdown.setSelectedItem(terrainMode);
            terrainModeDropdown.setToolTipText("Controls how terrain height is changed along the path.");
            terrainModeDropdown.addActionListener(l -> {
                this.terrainMode = (TerrainMode) terrainModeDropdown.getSelectedItem();
            });
            optionsPanel.add(terrainModeDropdown);
        }
        {
            fixHeightCheckbox = new JCheckBox("fixHeightCheckbox");
            fixHeightCheckbox.setToolTipText(
                    "if active, the path will only move at the specified height. rightclick to reset the height");
            fixHeightCheckbox.addActionListener(l -> {
                this.fixHeightTo = fixHeightCheckbox.isSelected();
            });
            this.fixHeightTo = fixHeightCheckbox.isSelected();
            // optionsPanel.add(fixHeightCheckbox); // FIXME: disabled because it causes
            // wierd artifacts across tile lines
        }
        {
            handleFactorSpinner = new JSpinner(new SpinnerNumberModel(handleStrength, -1d, 2d, 0.1d));
            handleFactorSpinner.setToolTipText(
                    "Controls how curve the path is. 0 = straight lines, 1 = normal curves, 2 = exaggerated curves, -1 = cursive");
            handleFactorSpinner.addChangeListener(l -> {
                handleStrength = ((Number) handleFactorSpinner.getValue()).floatValue();
            });
            JPanel panel = new JPanel();
            panel.add(new JLabel("Curve strength"));
            panel.add(handleFactorSpinner);
            optionsPanel.add(panel);
        }
        {
            limitSlopeSpinner = new JSpinner(new SpinnerNumberModel(0d, 0d, 100d, 1d));
            limitSlopeSpinner.setToolTipText(
                    "Limits the allowed slope to x block vertical per 16 blocks horizontal. 0 to disable.");
            limitSlopeSpinner.addChangeListener(l -> {
                slopeLimit = ((Number) limitSlopeSpinner.getValue()).floatValue();
                if (slopePreview != null)
                    slopePreview.repaint();
            });
            slopeLimit = ((Number) limitSlopeSpinner.getValue()).floatValue();
            JPanel panel = new JPanel();
            panel.add(new JLabel("Limit slope to x/16 blocks"));
            panel.add(limitSlopeSpinner);
            optionsPanel.add(panel);

            slopePreview = new CellPreviewPanel((x, y) -> {
                if (slopeLimit == 0)
                    return false;
                float centreRow = (slopePreview.getCellRows() - 1) / 2f;
                float centreColumn = (slopePreview.getCellColumns() - 1) / 2f;
                int hypotenuseRow = Math.min(slopePreview.getCellRows() - 1,
                        Math.max(0, Math.round(centreRow + (x - centreColumn) * slopeLimit / 16f)));
                return y == hypotenuseRow;
            });
            slopePreview.setToolTipText("Shows the maximum allowed slope; 0 means unlimited");
            optionsPanel.add(slopePreview);
        }
        {
            transitionMultiSpinner = new JSpinner(new SpinnerNumberModel(1d, 0d, 100d, .25d));
            transitionMultiSpinner.setToolTipText(
                    "How many times bigger the transition is compared to the brush radius. 0 = no transition, 1 = one brush radius width transition");
            transitionMultiSpinner.addChangeListener(l -> {
                transitionMultiplier = ((Number) transitionMultiSpinner.getValue()).floatValue() + 1;
                brushQuerschnitt.repaint();
            });
            transitionMultiSpinner.setValue(1d);
            JPanel panel = new JPanel();
            panel.add(new JLabel("Transition multiplier"));
            panel.add(transitionMultiSpinner);
            optionsPanel.add(panel);
        }
        {
            brushQuerschnitt = new JPanel() {
                @Override
                public void paint(Graphics g) {
                    super.paint(g);
                    if (brushProfile == null)
                        return;
                    g.setColor(Color.WHITE);
                    g.fillRect(0, 0, getWidth(), getHeight());

                    g.setColor(Color.BLACK);
                    for (int x = 0; x < getWidth() / 2f; x++) {
                        float strengthAtX = filterStrengthFor(x * transitionMultiplier, getWidth() / 2f,
                                getTransitionMultiplier(), brushProfile);
                        int height = Math.round(getHeight() * strengthAtX);
                        if (height > getHeight() * 0.99f)
                            g.setColor(Color.RED);
                        else
                            g.setColor(Color.BLACK);

                        g.fillRect(x + getWidth() / 2, getHeight() - height, 1, getHeight());
                        g.fillRect(getWidth() - (x + getWidth() / 2), getHeight() - height, 1, getHeight());

                        // g.fillRect(getW x + getWidth()/2, getHeight() - height, 1, getHeight());
                    }
                }
            };
            brushQuerschnitt.setPreferredSize(new java.awt.Dimension(0, 48));
            brushQuerschnitt.setMinimumSize(new java.awt.Dimension(0, 48));
            brushQuerschnitt.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 48));
            brushQuerschnitt.setToolTipText("Shows the cross section of the brush strength. Red area = 100% strength");
            optionsPanel.add(brushQuerschnitt);
        }
        {
            DefaultComboBoxModel<CrossSectionShape> model = new DefaultComboBoxModel<>();
            model.addElement(new CrossSectionShape("Square Root", "") {
                @Override
                public float getStrengthAt(float t) {
                    return (float) Math.sqrt(t);
                }
            });
            model.addElement(new CrossSectionShape("Logarithmic", "") {
                final float log_2 = (float) Math.log(2);

                @Override
                public float getStrengthAt(float t) {
                    return (float) (Math.log(t + 1) / log_2);
                }
            });
            model.addElement(new CrossSectionShape("Triangle", "") {
                @Override
                public float getStrengthAt(float t) {
                    return t;
                }
            });
            model.addElement(new CrossSectionShape("Sinus", "") {
                @Override
                public float getStrengthAt(float t) {
                    return (float) (Math.sin((t - 0.5f) * PI) / 2f + 0.5f);
                }
            });
            model.addElement(new CrossSectionShape("Quadratic", "") {
                @Override
                public float getStrengthAt(float t) {
                    return t * t;
                }
            });
            model.addElement(new CrossSectionShape("Cubic", "") {
                @Override
                public float getStrengthAt(float t) {
                    return t * t * t;
                }
            });

            profilesDropdown = new JComboBox<>(model);
            profilesDropdown.addActionListener(l -> {
                this.brushProfile = (CrossSectionShape) profilesDropdown.getSelectedItem();
                brushQuerschnitt.repaint();
            });
            profilesDropdown.setSelectedItem(model.getElementAt(3));
            this.brushProfile = (CrossSectionShape) profilesDropdown.getSelectedItem();
            JPanel panel = new JPanel();
            panel.add(new JLabel("Transition profile"));
            panel.add(profilesDropdown);
            optionsPanel.add(panel);
        }

        updateCheckboxTexts();
    }

    private void applyPreset(boolean onlyDown, boolean snapToTerrain, boolean usePaint, boolean setWaterHeight,
            TerrainMode terrainMode, float curveStrength, float transition, float slopeLimit,
            String transitionProfile) {
        this.onlyDown = onlyDown;
        this.snapToTerrain = snapToTerrain;
        this.usePaint = usePaint;
        this.setWaterHeight = setWaterHeight;
        this.terrainMode = terrainMode;
        onlyDownCheckbox.setSelected(onlyDown);
        minCheckbox.setSelected(snapToTerrain);
        usePaintCheckbox.setSelected(usePaint);
        setWaterHeightCheckbox.setSelected(setWaterHeight);
        terrainModeDropdown.setSelectedItem(terrainMode);
        handleFactorSpinner.setValue((double) curveStrength);
        transitionMultiSpinner.setValue((double) transition);
        limitSlopeSpinner.setValue((double) slopeLimit);
        if (transitionProfile != null) {
            for (int i = 0; i < profilesDropdown.getItemCount(); i++) {
                if (transitionProfile.equals(String.valueOf(profilesDropdown.getItemAt(i)))) {
                    profilesDropdown.setSelectedIndex(i);
                    break;
                }
            }
        }
        updateCheckboxTexts();
    }

    private void updateCheckboxTexts() {
        onlyDownCheckbox.setText("Only downhill");
        fixHeightCheckbox.setText("Fix height to: " + getFixHeight());
        usePaintCheckbox.setText("Set paint");
    }

    @Override
    public JPanel getOptionsPanel() {
        return optionsPanel;
    }

    private float getFixHeight() {
        return pathHandles.isEmpty() ? 62 : pathHandles.get(pathHandles.size() - 1).z;
    }

    @Override
    protected void brushChanged(Brush newBrush) {
        super.brushChanged(newBrush);
        brushQuerschnitt.revalidate();
        brushQuerschnitt.repaint();
    }

    @Override
    protected void tick(int centreX, int centreY, boolean inverse, boolean first, float dynamicLevel) {
        Dimension dim = getDimension();
        if (dim == null)
            return;

        { // pull data from attributes
            pathHandles = new ArrayList<>();
            var rawData = dim.getAttribute(PATHHANDLES_KEY);
            assert rawData.size() % 4 == 0;
            for (int i = 0; i < rawData.size(); i += 4) {
                pathHandles
                        .add(new Point4f(rawData.get(i), rawData.get(i + 1), rawData.get(i + 2), rawData.get(i + 3)));
            }
        }

        getPaint().setBrush(getBrush());

        int pathRadius = getBrush().getEffectiveRadius();
        float centreZ = getDimension().getHeightAt(centreX, centreY);
        Point4f thisPosition = new Point4f(centreX, centreY, centreZ, pathRadius);
        if (inverse) {
            pathHandles.clear();
            this.cachedTiles.clear();
        } else {

            pathHandles.add(thisPosition);
            if (!dim.isEventsInhibited())
                dim.setEventsInhibited(true);

            this.OnSmoothPath(pathHandles, true); // generate path

            if (dim.isEventsInhibited())
                dim.setEventsInhibited(false);
        }
        { // Push path handles to dimension attribute as flat float array (list)
            if (pathHandles != null) {
                var flatData = new ArrayList<Float>();
                for (var point : pathHandles) {
                    flatData.add(point.x);
                    flatData.add(point.y);
                    flatData.add(point.z);
                    flatData.add(point.w);
                }
                dim.setAttribute(PATHHANDLES_KEY, flatData, true);
            }
        }

        SwingUtilities.invokeLater(this::updateCheckboxTexts);
    }

    private void OnSmoothPath(List<Point4f> pathHandles, boolean onlyPreview) {
        if (pathHandles.size() < 2)
            return; // nothing to do
        var thisPosition = pathHandles.get(pathHandles.size() - 1);
        var lastPosition = pathHandles.get(pathHandles.size() - 2);

        Dimension dimension = getDimension();
        if (lastPosition != null) {
            // get new path section and append it
            var pathRes = getPathFromHandles(pathHandles, handleStrength);

            // DRAW RESULT ON MAP WITH ALL SEGMENTS
            dimension.clearLayerData(PreviewOperation.annotationLayer);
            for (var p : pathRes.path) {
                dimension.setBitLayerValueAt(PreviewOperation.annotationLayer, Math.round(p.x), Math.round(p.y), true);
            }
            if (onlyPreview)
                return; //early abort, dont apply to map

            if (pathHandles.size() < 4)
                return;

            final var path = pathRes.path;
        //    // cut off last segment that will change on next click anyways
        //    Point4f secondLastHandle = pathHandles.get(pathHandles.size() - 2);
        //    final var path = pathRes.path.subList(0,
        //            pathRes.handlesToPathIndex.getOrDefault(secondLastHandle, pathHandles.size()));
//
        //    // changed segment: thirdlast to secondLast handle
        //    Point4f thirdLastHandle = pathHandles.get(pathHandles.size() - 3);
        //    var newPathSection = path.subList(pathRes.handlesToPathIndex.getOrDefault(thirdLastHandle, 0),
        //            pathRes.handlesToPathIndex.getOrDefault(secondLastHandle, pathHandles.size()));

            // mutate path with filters based on user input
            if (snapToTerrain)
                PathToolBackend.forcePathToMinPos(path,
                        point4f -> dimension.getHeightAt(Math.round(point4f.x), Math.round(point4f.y)));
            if (onlyDown)
                PathToolBackend.forcePathOnlyDownhill(path);
            if (fixHeightTo)
                PathToolBackend.forcePathToHeight(path, getFixHeight());

            if (slopeLimit != 0)
                PathToolBackend.enforceSlopeLimit(path, slopeLimit);

            forceRadiusAtLeast(path, .5f); // always at least 1 thick

            // collect tiles where the newly added path section passed through
            Set<Point3i> newPathTiles = PathToolBackend.collectTilesAroundPath(path, transitionMultiplier);

            // cache all tiles that might be affected
            for (var tilePos : newPathTiles) {
                var tile = dimension.getTile(tilePos.x, tilePos.y);
                if (tile == null)
                    continue;
                if (cachedTiles.containsKey(tilePos))
                    continue;
                var clone = PathToolBackend.cloneHeightMapData(tile);
                cachedTiles.put(tilePos, clone);
            }

            HashMap<Point2i, Point4f> posToData = new HashMap<>();
            path.forEach(pathPoint -> {
                var pos = new Point2i(pathPoint.x, pathPoint.y);
                posToData.put(pos, pathPoint);
            });

            // iterate all cachedTiles and all points inside the cachedTiles
            System.out.println("PROCESSING " + newPathTiles.size() + " Tiles");
            AtomicInteger totalProcessedPath = new AtomicInteger();
            AtomicInteger totalTiles = new AtomicInteger();
            HashMap<Point3i, FloatTile> paintOutputMap = new HashMap<>();
            HashMap<Point3i, FloatTile> waterHeightMap = new HashMap<>();
            var heightInfoTiles = newPathTiles.parallelStream().map(tilePos -> {
                Point2i tileAreaStart = new Point2i((tilePos.x) << TILE_SIZE_BITS, (tilePos.y) << TILE_SIZE_BITS);
                Point2i tileAreaEnd = new Point2i(TILE_SIZE + ((tilePos.x) << TILE_SIZE_BITS),
                        TILE_SIZE + ((tilePos.y) << TILE_SIZE_BITS));
                var paintOutput = new FloatTile(tilePos);
                paintOutputMap.put(tilePos, paintOutput);
                var waterHeightTile = new FloatTile(tilePos);
                waterHeightMap.put(tilePos, waterHeightTile);

                return PathToolBackend.applyToTile(cachedTiles.get(tilePos), paintOutput, waterHeightTile,
                        tilePos, brushProfile,
                        PathToolBackend.getSubPathFor(tileAreaStart, tileAreaEnd, path, transitionMultiplier),
                        getTransitionMultiplier(), terrainMode == TerrainMode.CARVE); //FIXME use actual "dig down" checkbox?
            }).toList();
            heightInfoTiles.forEach(heightInfoTile -> {
                if (heightInfoTile == null)
                    return;
                Tile wpTile = dimension.getTileForEditing(heightInfoTile.tilePosX, heightInfoTile.tilePosY);
                if (wpTile == null)
                    return;
                if (terrainMode != TerrainMode.DONT_CHANGE)
                    PathToolBackend.writeHeightMapDataToTile(heightInfoTile, wpTile);
                if (setWaterHeight) {
                    var waterHeightTile = waterHeightMap.get(new Point3i(wpTile.getX(), wpTile.getY(), 0));
                    PathToolBackend.writeWaterHeightDataToDimension(
                            waterHeightTile, wpTile);
                }
                if (usePaint) {
                    var paintTile = paintOutputMap.get(new Point3i(wpTile.getX(), wpTile.getY(), 0));
                    PathToolBackend.writePaintDataToDimension(paintTile, dimension, getPaint());
                }
            });

            System.out.printf("Processed %d cachedTiles, %d path points total, current path length %d",
                    totalTiles.get(), totalProcessedPath.get(), path.size());
        }
    }

    /**
     * how much larger the transition is compared to the brush radius 1 = no
     * transition 2 = double radius
     *
     * @return
     */
    private float getTransitionMultiplier() {
        return transitionMultiplier;
    }

    @Override
    public Paint getPaint() {
        return paint;
    }

    @Override
    public void setPaint(Paint paint) {
        this.paint = paint;
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

    @Override
    public void bufferChanged(BufferKey<?> key) {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            PathTool pathTool = new PathTool();
            JFrame frame = new JFrame("Road Tool Options");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(pathTool.getOptionsPanel());
            frame.pack();
            frame.setLocationByPlatform(true);
            frame.setVisible(true);
        });
    }

    private static class PathHandlesContainer implements Cloneable, Serializable
    {
        @Serial
        private static final long serialVersionUID = -1L;
        List<Point4f> data;

        public PathHandlesContainer(List<Point4f> data) {
            this.data = data;
        }

        @Override
        public PathHandlesContainer clone() {
            return new PathHandlesContainer(data.stream().map(p -> (Point4f) p.clone()).collect(Collectors.toList()));
        }
    }

    private class PathPosition
    {
        final float x, y, height, pathRadius, transitionRadius;

        private PathPosition(float x, float y, float pathRadius, float transitionRadius, float height) {
            this.x = x;
            this.y = y;
            this.pathRadius = pathRadius;
            this.transitionRadius = transitionRadius;
            this.height = height;
        }

        PathPosition interpolateWith(PathPosition other, float t) {
            float t1 = 1 - t;
            return new PathPosition(this.x * t + other.x * t1, this.y * t + other.y * t1,
                    this.pathRadius * t + other.pathRadius * t1,
                    this.transitionRadius * t + other.transitionRadius * t1, this.height * t + other.height * t1);
        }
    }
}
