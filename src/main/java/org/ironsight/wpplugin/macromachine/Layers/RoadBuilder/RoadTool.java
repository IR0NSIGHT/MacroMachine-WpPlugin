package org.ironsight.wpplugin.macromachine.Layers.RoadBuilder;

import org.ironsight.wpplugin.macromachine.MacroSelectionLayer;
import org.pepsoft.util.swing.TiledImageViewer;
import org.pepsoft.util.undo.BufferKey;
import org.pepsoft.util.undo.UndoListener;
import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.brushes.Brush;
import org.pepsoft.worldpainter.operations.AbstractBrushOperation;
import org.pepsoft.worldpainter.operations.PaintOperation;
import org.pepsoft.worldpainter.painting.Paint;
import org.pepsoft.worldpainter.selection.SelectionChunk;

import javax.swing.*;
import javax.vecmath.*;

import java.awt.*;
import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Math.*;
import static org.ironsight.wpplugin.macromachine.Gui.HelpDialog.getHelpButton;
import static org.ironsight.wpplugin.macromachine.Layers.RoadBuilder.RoadToolBackend.filterStrengthFor;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE_BITS;

public class RoadTool extends AbstractBrushOperation implements PaintOperation, UndoListener {
    private static final String help = """
            Roadtool will connect clicked positions into a path and smoothly blend them into existing terrain, based on the brush you are using.
            Recommend to only use circular brushes.
                        
            Right click: Start new path at this position
            Left click: Advance current path to this position
                        
            """;
    private final JPanel optionsPanel = new JPanel();
    ArrayList<Point4f> path = new ArrayList<>();
    HashMap<Point3i, FloatTile> cachedTiles = new HashMap<>();
    private Point4f lastPosition;
    private Paint paint;
    // only allow downwards movement
    private boolean onlyDown;
    private boolean minPath;
    private boolean fixHeightTo;
    private boolean usePaint;
    private float slopeLimit = 0;
    private JCheckBox onlyDownCheckbox;
    private JCheckBox minCheckbox;
    private JCheckBox fixHeightCheckbox;
    private JCheckBox usePaintCheckbox;
    private JSpinner limitSlopeSpinner;
    private JSpinner transitionMultiSpinner;
    private JPanel brushQuerschnitt;
    private CrossSectionShape brushProfile;
    private float transitionMultiplier = 2;

    public RoadTool() {
        super("Road Tool", "Create smooth roads", "MacroMachine_RoadTool"); //ONE SHOT OP
        init();
    }


    @Override
    protected void activate() throws PropertyVetoException {
        super.activate();
        this.lastPosition = null;
        this.path.clear();
        this.cachedTiles.clear();
    }

    @Override
    protected void deactivate() {
        super.deactivate();
        this.lastPosition = null;
        this.path.clear();
        this.cachedTiles.clear();
    }

    private void init() {
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        optionsPanel.add(getHelpButton("Road Tool", help));

        {
            onlyDownCheckbox = new JCheckBox("onlyDownCheckbox");
            onlyDownCheckbox.setToolTipText("if active, the path will only move horizontally or downwards, but never higher than the last used coordinate. righclick to reset height");
            onlyDownCheckbox.addActionListener(l -> {
                this.onlyDown = onlyDownCheckbox.isSelected();
            });
            this.onlyDown = onlyDownCheckbox.isSelected();
            optionsPanel.add(onlyDownCheckbox);
        }
   /*     {
            minCheckbox = new JCheckBox("min heights");
            minCheckbox.setToolTipText("if active, the path will take the lowest possible heights available. rightclick to reset height");
            minCheckbox.addActionListener(l -> {
                this.minPath = minCheckbox.isSelected();
            });
            this.minPath = minCheckbox.isSelected();
            optionsPanel.add(minCheckbox);
        } */
        {
            usePaintCheckbox = new JCheckBox("usePaintCheckbox");
            usePaintCheckbox.setSelected(true);
            usePaintCheckbox.setToolTipText("if active, the path will paint the current selected layer/terrain where the filter strength is 100% (red area in the cross-section)");
            usePaintCheckbox.addActionListener(l -> {
                this.usePaint = usePaintCheckbox.isSelected();
            });
            this.usePaint = usePaintCheckbox.isSelected();
            optionsPanel.add(usePaintCheckbox);
        }
        {
            fixHeightCheckbox = new JCheckBox("fixHeightCheckbox");
            fixHeightCheckbox.setToolTipText("if active, the path will only move at the specified height. rightclick to reset the height");
            fixHeightCheckbox.addActionListener(l -> {
                this.fixHeightTo = fixHeightCheckbox.isSelected();
            });
            this.fixHeightTo = fixHeightCheckbox.isSelected();
            optionsPanel.add(fixHeightCheckbox);
        }
        {

            limitSlopeSpinner = new JSpinner(new SpinnerNumberModel(0d, 0d, 100d, 1d));
            limitSlopeSpinner.setToolTipText("Limits the allowed slope to x block vertical per 16 blocks horizontal. 0 to disable.");
            limitSlopeSpinner.addChangeListener(l -> {
                slopeLimit = ((Number) limitSlopeSpinner.getValue()).floatValue();
            });
            slopeLimit = ((Number) limitSlopeSpinner.getValue()).floatValue();
            JPanel panel = new JPanel();
            panel.add(new JLabel("Limit slope to x/16 blocks"));
            panel.add(limitSlopeSpinner);
            optionsPanel.add(panel);
        }
        {

            transitionMultiSpinner = new JSpinner(new SpinnerNumberModel(1d, 0d, 100d, .25d));
            transitionMultiSpinner.setToolTipText("How many times bigger the transition is compared to the brush radius. 0 = no transition, 1 = one brush radius width transition");
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
                    Brush brush = getBrush();
                    if (brush == null) return;
                    g.setColor(Color.WHITE);
                    g.fillRect(0, 0, getWidth(), getHeight());

                    g.setColor(Color.BLACK);
                    for (int x = 0; x < getWidth() / 2f; x++) {
                        float strengthAtX = filterStrengthFor(x * transitionMultiplier, getWidth() / 2f, getTransitionMultiplier(), brushProfile);
                        int height = Math.round(getHeight() * strengthAtX);
                        if (height > getHeight() * 0.99f) g.setColor(Color.RED);
                        else g.setColor(Color.BLACK);

                        g.fillRect(x + getWidth() / 2, getHeight() - height, 1, getHeight());
                        g.fillRect(getWidth() - (x + getWidth() / 2), getHeight() - height, 1, getHeight());

                        //g.fillRect(getW x + getWidth()/2, getHeight() - height, 1, getHeight());
                    }
                }
            };
            brushQuerschnitt.setPreferredSize(new java.awt.Dimension(100, 30));
            brushQuerschnitt.setToolTipText("Shows the cross section of the brush strength. Red area = 100% strength");
            optionsPanel.add(brushQuerschnitt);
        }
        {
            JList<CrossSectionShape> profilesList = new JList<>();
            DefaultListModel<CrossSectionShape> model = new DefaultListModel<CrossSectionShape>();
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

            profilesList.setModel(model);
            profilesList.addListSelectionListener(l -> {
                if (!l.getValueIsAdjusting()) this.brushProfile = profilesList.getSelectedValue();
                brushQuerschnitt.repaint();
            });
            profilesList.setSelectedIndex(0);
            optionsPanel.add(new JScrollPane(profilesList));
        }

        updateCheckboxTexts();
    }

    private void updateCheckboxTexts() {
        onlyDownCheckbox.setText("Only downhill");
        fixHeightCheckbox.setText("Fix height to: " + getFixHeight());
        usePaintCheckbox.setText("Use paint");
    }

    @Override
    public JPanel getOptionsPanel() {
        return optionsPanel;
    }

    private float getFixHeight() {
        return lastPosition == null ? 62 : lastPosition.z;
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
        if (dim == null) return;
        getPaint().setBrush(getBrush());

        int pathRadius = getBrush().getEffectiveRadius();
        float centreZ = getDimension().getHeightAt(centreX, centreY);
        Point4f thisPosition = new Point4f(centreX, centreY, centreZ, pathRadius);
        if (inverse) {
            path.clear();
            this.cachedTiles.clear();
        } else {
            if (!dim.isEventsInhibited()) dim.setEventsInhibited(true);
            this.OnSmoothPath(thisPosition); //generate path

            if (dim.isEventsInhibited()) dim.setEventsInhibited(false);
        }

        lastPosition = thisPosition;
        SwingUtilities.invokeLater(this::updateCheckboxTexts);
    }

    private void OnSmoothPath(Point4f thisPosition) {
        Dimension dimension = getDimension();
        if (lastPosition != null) {
            // limit slope if necessary by adjusting thisPos.z
            if (slopeLimit != 0) {
                float distanceBetweenPositions = new Point2f(thisPosition.x, thisPosition.y).distance(new Point2f(lastPosition.x, lastPosition.y));
                float slopeMax = distanceBetweenPositions * slopeLimit / 16f;
                if (thisPosition.z < lastPosition.z) thisPosition.z = Math.max(thisPosition.z, lastPosition.z - slopeMax);
                else if (thisPosition.z > lastPosition.z) thisPosition.z = Math.min(thisPosition.z, lastPosition.z + slopeMax);
            }

            // get new path section and append it
            var newPathSection = RoadToolBackend.plotPathBetween(lastPosition, thisPosition);
            this.path.addAll(newPathSection);

            // mutate path with filters based on user input
            if (minPath) RoadToolBackend.forcePathToMinPos(path, point4f -> dimension.getHeightAt(Math.round(point4f.x), Math.round(point4f.y)));
            if (onlyDown) RoadToolBackend.forcePathOnlyDownhill(path);
            if (fixHeightTo) RoadToolBackend.forcePathToHeight(path, getFixHeight());

            //collect tiles the newly added path section passed through
            Set<Point3i> newPathTiles = RoadToolBackend.collectTilesAroundPath(newPathSection, transitionMultiplier);

            //cache all tiles that might be affected
            for (var tilePos : newPathTiles) {
                var tile = dimension.getTile(tilePos.x, tilePos.y);
                if (tile == null) continue;
                if (cachedTiles.containsKey(tilePos)) continue;
                var clone = RoadToolBackend.cloneHeightMapData(tile);
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
            var outputTiles = newPathTiles.parallelStream()
                    .map(tilePos -> {
                        Point2i tileAreaStart = new Point2i((tilePos.x) << TILE_SIZE_BITS, (tilePos.y) << TILE_SIZE_BITS);
                        Point2i tileAreaEnd = new Point2i(TILE_SIZE + ((tilePos.x) << TILE_SIZE_BITS), TILE_SIZE + ((tilePos.y) << TILE_SIZE_BITS));
                        var paintOutput = new FloatTile(tilePos);
                        paintOutputMap.put(tilePos, paintOutput);
                        return RoadToolBackend.applyToTile(cachedTiles.get(tilePos),
                                paintOutput,
                                tilePos,
                                brushProfile,
                                RoadToolBackend.getSubPathFor(tileAreaStart, tileAreaEnd, path, transitionMultiplier),
                                getTransitionMultiplier());
                    }).toList();
            outputTiles.forEach(floatTile -> {
                if (floatTile == null)
                    return;
                Tile wpTile = dimension.getTileForEditing(floatTile.tilePosX, floatTile.tilePosY);
                if (wpTile == null) return;
                RoadToolBackend.writeHeightMapDataToTile(floatTile, wpTile);
                var paintTile = paintOutputMap.get(new Point3i(wpTile.getX(), wpTile.getY(), 0));
                RoadToolBackend.writePaintDataToDimension(paintTile, dimension, getPaint());
            });

            System.out.printf("Processed %d cachedTiles, %d path points total, current path length %d", totalTiles.get(), totalProcessedPath.get(), path.size());
        }
    }

    /**
     * how much larger the transition is compared to the brush radius 1 = no transition 2 = double radius
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

    private class PathPosition {
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
            return new PathPosition(this.x * t + other.x * t1,
                    this.y * t + other.y * t1,
                    this.pathRadius * t + other.pathRadius * t1,
                    this.transitionRadius * t + other.transitionRadius * t1,
                    this.height * t + other.height * t1);
        }
    }
}
