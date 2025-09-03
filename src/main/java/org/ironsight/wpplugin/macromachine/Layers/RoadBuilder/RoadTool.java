package org.ironsight.wpplugin.macromachine.Layers.RoadBuilder;

import org.ironsight.wpplugin.macromachine.Gui.GlobalActionPanel;
import org.pepsoft.util.undo.BufferKey;
import org.pepsoft.util.undo.UndoListener;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.brushes.Brush;
import org.pepsoft.worldpainter.operations.AbstractBrushOperation;
import org.pepsoft.worldpainter.operations.PaintOperation;
import org.pepsoft.worldpainter.painting.Paint;

import javax.swing.*;
import javax.vecmath.*;

import java.awt.*;
import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import static java.lang.Math.*;
import static org.ironsight.wpplugin.macromachine.Gui.HelpDialog.getHelpButton;
import static org.pepsoft.util.swing.TiledImageViewer.TILE_SIZE;
import static org.pepsoft.util.swing.TiledImageViewer.TILE_SIZE_BITS;

public class RoadTool extends AbstractBrushOperation implements PaintOperation, UndoListener {
    private final JPanel optionsPanel = new JPanel();

    ArrayList<Point4f> path = new ArrayList<>();
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
    private JPanel brushQuerschnitt;

    public RoadTool() {
        super("Road Tool", "Create smooth roads", "MacroMachine_RoadTool"); //ONE SHOT OP
        init();
    }

    @Override
    protected void activate() throws PropertyVetoException {
        super.activate();
        this.lastPosition = null;
        this.path.clear();
    }

    private static final String help = """
            Roadtool will connect clicked positions into a path and smoothly blend them into existing terrain, based on the brush you are using.
            Recommend to only use circular brushes.
            
            Right click: Start new path at this position
            Left click: Advance current path to this position
            
            """;
    private void init() {
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        optionsPanel.add(getHelpButton("Road Tool",help));

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
            brushQuerschnitt = new JPanel() {
                @Override
                public void paint(Graphics g) {
                    super.paint(g);
                    Brush brush = getBrush();
                    if (brush == null)
                        return;
                    g.setColor(Color.WHITE);
                    g.fillRect(0, 0, getWidth(), getHeight());

                    g.setColor(Color.BLACK);
                    for (int x = 0; x < getWidth(); x++) {
                        float t = (1f * x / getWidth() - 0.5f) * 2;
                        float strengthAtX = brush.getStrength(Math.round(t * brush.getRadius()), 0);
                        int height = Math.round(getHeight() * strengthAtX);
                        if (height > getHeight() * 0.99f)
                            g.setColor(Color.RED);
                        else
                            g.setColor(Color.BLACK);

                        g.fillRect(x, getHeight() - height, 1, getHeight());
                    }
                }
            };
            brushQuerschnitt.setPreferredSize(new java.awt.Dimension(100, 30));
            brushQuerschnitt.setToolTipText("Shows the cross section of the brush strength. Red area = 100% strength");
            optionsPanel.add(brushQuerschnitt);
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
        if (dim == null)
            return;


        int radius = getBrush().getEffectiveRadius();
        float centreZ = getDimension().getHeightAt(centreX, centreY);
        Point4f thisPosition = new Point4f(centreX, centreY, centreZ, radius);

        if (inverse) {
            path.clear();
        } else {
            if (!dim.isEventsInhibited())
                dim.setEventsInhibited(true);
            this.OnSmoothPath(thisPosition); //generate path

            if (dim.isEventsInhibited())
                dim.setEventsInhibited(false);
        }

        lastPosition = thisPosition;
        SwingUtilities.invokeLater(this::updateCheckboxTexts);
    }

    private float[] constructCrossSectionFilter(Brush brush) {
        float[] crossSection = new float[brush.getRadius()];
        for (int r = 0; r < getBrush().getRadius(); r++) {
            float filterStrength = getBrush().getStrength(r, 0);
            crossSection[r] = filterStrength;
        }
        return crossSection;
    }

    private void OnSmoothPath(Point4f thisPosition) {
        if (fixHeightTo)
            thisPosition.z = getFixHeight();

        if (onlyDown && lastPosition != null) {
            thisPosition.z = Math.min(thisPosition.z, lastPosition.z);
        }

        getPaint().setBrush(getBrush());
        Dimension dimension = getDimension();
        if (lastPosition != null) {
            if (slopeLimit != 0) {
                float distanceBetweenPositions = new Point2f(thisPosition.x, thisPosition.y).distance(new Point2f(lastPosition.x, lastPosition.y));
                float slopeMax = distanceBetweenPositions * slopeLimit / 16f;
                if (thisPosition.z < lastPosition.z)
                    thisPosition.z = Math.max(thisPosition.z, lastPosition.z - slopeMax);
                else if (thisPosition.z > lastPosition.z)
                    thisPosition.z = Math.min(thisPosition.z, lastPosition.z + slopeMax);
            }

            int startIdx = path.size();
            float steps = max(abs(thisPosition.x - lastPosition.x), abs(thisPosition.y - lastPosition.y));
            path.ensureCapacity((int) ceil(path.size() + steps));

            for (int i = 0; i < steps; i++) {
                float t = i / steps;
                Point4f interpolated = interpolate(thisPosition, lastPosition, t);
                path.add(interpolated);
            }
            System.out.println("Path =" + path.toString());
            float previousZ = thisPosition.z;
            if (minPath) {
                for (Point4f point : path) {
                    if (minPath) {
                        float height = dimension.getHeightAt(Math.round(point.x), Math.round(point.y));
                        point.z = Math.min(previousZ, Math.min(point.z, height));

                    }
                    previousZ = point.z;
                }
            }

            if (this.usePaint)
                for (Point4f point : path) {
                    getPaint().applyPixel(dimension, Math.round(point.x), Math.round(point.y));
                }

            final float[] filterCrossSection = constructCrossSectionFilter(getBrush());
            //collect all tiles that might be affected
            HashSet<Point3i> tiles = new HashSet<>();
            for (int i = 0; i < path.size(); i++) {
                var pathPoint = path.get(i);
                int tileRadius = (int) Math.ceil(pathPoint.w / TILE_SIZE);
                int x = Math.round(pathPoint.x) >> TILE_SIZE_BITS;
                int y = Math.round(pathPoint.y) >> TILE_SIZE_BITS;
                for (int xx = -tileRadius; xx <= tileRadius; xx++)
                    for (int yy = -tileRadius; yy <= tileRadius; yy++) {
                        int tilex = x + xx;
                        int tiley = y + yy;
                        Point3i thisPoint = new Point3i(tilex, tiley, 0);
                        tiles.add(thisPoint);
                    }
            }

            // iterate all tiles and all points inside the tiles
            tiles.forEach(tilePos -> {
                Tile t = dimension.getTileForEditing(tilePos.x, tilePos.y);
                applyToTile(t, filterCrossSection, path, dimension, startIdx);
            });
        }
    }

    private void applyToTile(Tile t, float[] filterCrossSection, ArrayList<Point4f> path, Dimension dim, int startIdx) {
        if (t == null || path.size() < 2) {
            return;
        }
        synchronized (t) {
            t.inhibitEvents();
            for (int xx = 0; xx < TILE_SIZE; xx++)
                for (int yy = 0; yy < TILE_SIZE; yy++) {
                    Point3i thisPoint = new Point3i((t.getX() << TILE_SIZE_BITS) + xx, (t.getY() << TILE_SIZE_BITS) + yy, 0);
                    Point4f thisPoint4f = new Point4f(thisPoint.x, thisPoint.y, thisPoint.z, 0);
                    // find closest point on path to this position
                    Point4f closest = null;
                    double distMin = Float.MAX_VALUE;
                    for (var testP : path) {
                        double dist = xyDistSq(thisPoint4f, testP);
                        if (dist < distMin) {
                            closest = testP;
                            distMin = dist;
                        }
                    }
                    distMin = Math.sqrt(distMin);
                    if (closest == null || distMin > closest.w)
                        continue;
                    float thisRadiusAdjusted = (float) distMin / closest.w * path.get(0).w;
                    int crossSectionIdx = Math.round(thisRadiusAdjusted);
                    if (crossSectionIdx >= filterCrossSection.length - 1)
                        continue;
                    float filterStrength = filterCrossSection[crossSectionIdx];
                    float originalHeight = t.getHeight(xx, yy);
                    float mixed = filterStrength * closest.z + (1 - filterStrength) * originalHeight;
                    t.setHeight(xx, yy, mixed);
                    if (filterStrength > 0.99f) {
                        getPaint().applyPixel(dim, thisPoint.x, thisPoint.y);
                    }
                }
            t.releaseEvents();
        }
    }

    private double xyDistSq(Point4f a, Point4f b) {
        float dX = a.x - b.x, dy = a.y - b.y;
        double dist = dX * dX + dy * dy;
        return dist;
    }

    private Point4f interpolate(Point4f a, Point4f b, float t) {
        Point4f out = new Point4f(b);
        out.interpolate(a, t);
        return out;
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
}
