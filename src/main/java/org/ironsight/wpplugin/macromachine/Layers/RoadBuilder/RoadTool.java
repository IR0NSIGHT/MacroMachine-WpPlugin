package org.ironsight.wpplugin.macromachine.Layers.RoadBuilder;

import org.pepsoft.util.undo.BufferKey;
import org.pepsoft.util.undo.UndoListener;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.operations.AbstractBrushOperation;
import org.pepsoft.worldpainter.operations.PaintOperation;
import org.pepsoft.worldpainter.painting.Paint;

import javax.swing.*;
import javax.vecmath.Point3i;
import javax.vecmath.Point4f;

import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import static java.lang.Math.*;
import static org.pepsoft.util.swing.TiledImageViewer.TILE_SIZE;
import static org.pepsoft.util.swing.TiledImageViewer.TILE_SIZE_BITS;

public class RoadTool extends AbstractBrushOperation implements PaintOperation, UndoListener {
    private final JPanel optionsPanel = new JPanel();
    ArrayList<Point4f> path = new ArrayList<>();
    private Point4f lastPosition;
    private Paint paint;
    // only allow downwards movement
    private boolean onlyDown;

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

    private void init() {
        JCheckBox onlyDownCheckbox = new JCheckBox("Only downhill");
        onlyDownCheckbox.setToolTipText("if active, the path will only move horizontally or downwards, but never higher than the last used coordinate. righclick to reset height");
        onlyDownCheckbox.addActionListener(l -> {
            this.onlyDown = onlyDownCheckbox.isSelected();
        });
        optionsPanel.add(onlyDownCheckbox);
    }

    @Override
    public JPanel getOptionsPanel() {
        return optionsPanel;
    }

    @Override
    protected void tick(int centreX, int centreY, boolean inverse, boolean first, float dynamicLevel) {
        Dimension dim = getDimension();
        if (dim == null)
            return;


        int radius = getEffectiveRadius();
        float centreZ = getDimension().getHeightAt(centreX, centreY);
        Point4f thisPosition = new Point4f(centreX, centreY, centreZ, radius);


        if (inverse) {
            path.clear();
            lastPosition = thisPosition;
            return;
        }

        if (onlyDown && lastPosition != null) {
            thisPosition.z = Math.min(thisPosition.z, lastPosition.z);
        }

        HashMap<Integer, Float> filter = new HashMap<>();
        for (int r = 0; r <= radius; r++) {
            float filterStrength = getBrush().getStrength(r, 0);
            filter.put(r, filterStrength);
        }

        if (lastPosition != null) {
            if (!dim.isEventsInhibited())
                dim.setEventsInhibited(true);

            float steps = max(abs(thisPosition.x - lastPosition.x), abs(thisPosition.y - lastPosition.y));
            final int MAX_PATH_LENGTH = 10;
            if (path.size() >= MAX_PATH_LENGTH) {
                path = new ArrayList<>(path.subList(path.size() - MAX_PATH_LENGTH, path.size() - 1));
            }
            path.ensureCapacity((int) ceil(path.size() + steps));
            for (int i = 0; i < steps; i++) {
                float t = i / steps;
                Point4f interpolated = interpolate(thisPosition, lastPosition, t);
                path.add(interpolated);
            }

            //collect all tiles that might be affected
            HashSet<Point3i> tiles = new HashSet<>();
            for (var pathPoint : path) {
                int tileRadius = (int) Math.ceil(pathPoint.w / TILE_SIZE);
                int x = Math.round(pathPoint.x) >> TILE_SIZE_BITS;
                int y =  Math.round(pathPoint.y) >> TILE_SIZE_BITS;
                for (int xx = -tileRadius; xx <= tileRadius; xx++)
                    for (int yy = -tileRadius; yy <= tileRadius; yy++) {
                        int tilex = x + xx;
                        int tiley = y + yy;
                        Point3i thisPoint = new Point3i(tilex, tiley , 0);
                        tiles.add(thisPoint);
                    }
            }

            // iterate all tiles and all points inside the tiles
            for (Point3i tilePos : tiles) {
                Tile t = dim.getTileForEditing(tilePos.x, tilePos.y);
                if (t == null) {
                    continue;

                }
                for (int xx = 0; xx < TILE_SIZE; xx++)
                    for (int yy = 0; yy < TILE_SIZE; yy++) {

                        Point3i thisPoint = new Point3i((t.getX() << TILE_SIZE_BITS) + xx, (t.getY() << TILE_SIZE_BITS) + yy, 0);
                        // find closest point on path to this position
                        Point4f closest = null;
                        double distMin = Float.MAX_VALUE;

                        for (var testP : path) {
                            float dX = thisPoint.x - testP.x, dy = thisPoint.y - testP.y;
                            double dist = dX * dX + dy * dy;
                            if (dist < distMin) {
                                closest = testP;
                                distMin = dist;
                            }
                        }
                        distMin = Math.sqrt(distMin);
                        if (closest == null || distMin > closest.w)
                           continue;
                        float thisRadiusAdjusted = (float) distMin / closest.w * thisPosition.w;
                        float filterStrength = filter.getOrDefault((int) Math.round(thisRadiusAdjusted), 0f);
                        float originalHeight = t.getHeight(xx,yy);
                        float mixed = filterStrength * closest.z + (1 - filterStrength) * originalHeight;
                        t.setHeight(xx,yy,mixed);
                    //    if (filterStrength > 0.5f)
                    //        getPaint().applyPixel(dim, thisPoint.x, thisPoint.y);
                    }
            }

        }


        lastPosition = thisPosition;

        if (dim.isEventsInhibited())
            dim.setEventsInhibited(false);

    }

    private Point4f interpolate(Point4f a, Point4f b, float t) {
        Point4f out = new Point4f(a);
        out.interpolate(b, t);
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
