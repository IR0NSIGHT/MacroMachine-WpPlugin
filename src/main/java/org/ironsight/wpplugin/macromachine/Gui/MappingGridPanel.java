package org.ironsight.wpplugin.macromachine.Gui;

import org.checkerframework.checker.units.qual.C;
import org.ironsight.wpplugin.macromachine.operations.MappingPoint;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Consumer;
import java.util.function.Function;

import static javax.swing.BorderFactory.createLineBorder;

public class MappingGridPanel extends LayerMappingPanel implements IMappingPointSelector {
    private final int pointHitBoxRadius = 20;
    int widthOutputStrings = 0;
    int fontheight;
    private int shiftX = 50;
    private int pixelSizeX = 500;
    private int pixelSizeY = 500;
    private float GRID_X_SCALE;
    private float GRID_Y_SCALE;
    private boolean drag;
    private boolean[] selectedInputs;
    private Consumer<boolean[]> onSelect = f -> {
    };
    private boolean showCursor = false;
    private int mousePosX, mousePosY;
    private boolean mouseMoved = false;
    private int shiftY = 10;

    public MappingGridPanel() {

        //    this.setPreferredSize(new Dimension(pixelSizeX, pixelSizeY));
        setBorder(BorderFactory.createEmptyBorder(100, 100, 100, 100));
        initComponents();
    }

    @Override
    protected void updateComponents() {
        if (selectedInputs == null ||
                this.mapping.input.getMaxValue() - this.mapping.input.getMinValue() + 1 != selectedInputs.length) {
            resetSelection();
        }
        System.out.println(" GRID PANEL UPDATE COMPONENTS");
        SwingUtilities.invokeLater(()->{
            revalidate();
            repaint();
        });
    }

    private void setInputSelection(MappingPoint point, boolean selected) {
        this.selectedInputs[point.input - mapping.input.getMinValue()] = selected;
        onSelect.accept(selectedInputs);
        lastClickedPoint = point;
    }

    private boolean isInputSelected(int input) {
        return selectedInputs[input - mapping.input.getMinValue()];
    }

    private void resetSelection() {
        selectedInputs = new boolean[mapping.input.getMaxValue() - mapping.input.getMinValue() + 1];
    }

    @Override
    protected void initComponents() {
        JPanel grid = new JPanel() {
            @Override
            public void paint(Graphics g) {
                super.paint(g);
                paintGrid(g);
            }
        };

        // Add a MouseListener to detect clicks inside the panel
        MappingGridPanel panel = this;
        grid.addMouseListener(new MouseAdapter() {
            int gridXDragStart, gridYDragStart;
            int pixelXDragStart, pixelYDragStart;

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    Point grid = pixelToGrid(e.getX(), e.getY());
                    gridXDragStart = grid.x;
                    gridYDragStart = grid.y;
                    pixelXDragStart = e.getX();
                    pixelYDragStart = e.getY();
                    //update selected

                    MappingPoint last = lastClickedPoint;
                    boolean hit = selectPointNear(pixelXDragStart, pixelYDragStart, pointHitBoxRadius);
                    if (last != lastClickedPoint)
                        return; // do not drag a point that was just selected.
                    drag = true;
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                super.mouseReleased(e);
                drag = false;
                // Get the pixel coordinates of the click
                int pixelX = e.getX();
                int pixelY = e.getY();
                Point pressed = pixelToGrid(pixelX, pixelY);

                // Convert the pixel coordinates to grid coordinates
                int gridX = pressed.x;
                int gridY = pressed.y;

                int MINIMAL_DRAG_DISTANCE = 30;
                boolean dragged = distanceBetween(pixelX, pixelY, pixelXDragStart, pixelYDragStart) > MINIMAL_DRAG_DISTANCE;
                if (e.getButton() == MouseEvent.BUTTON1) {
                    if (!dragged) {    // INSERT POINT
                        // Call the callback with the grid coordinates
                        boolean hit = selectPointNear(pixelX, pixelY, pointHitBoxRadius);
                        if (!hit) { //selected point didnt change,
                            // INSERT NEW POINT
                            MappingPoint[] newPoints = Arrays.copyOf(panel.mapping.getMappingPoints(),
                                    panel.mapping.getMappingPoints().length + 1);
                            MappingPoint p = new MappingPoint(mapping.sanitizeInput(gridX), mapping.sanitizeOutput(gridY));
                            newPoints[newPoints.length - 1] = p;

                            updateMapping(mapping.withNewPoints(newPoints));
                            resetSelection();
                            setInputSelection(p, true); //select the new point
                        } else {
                            //implicitly set selected point, but dont do anything with it
                        }
                    }
                } else if (e.getButton() == MouseEvent.BUTTON3) {   // RIGHT CLICK
                    // DELETE SELECTED POINT
                    if (lastClickedPoint == null)
                        return;

                    //test if rightclick occured far away from current selected point
                    Point p = gridToPixel(lastClickedPoint.input,lastClickedPoint.output);
                    p.x -= pixelX; p.y -= pixelY;
                    double distPixel =Math.sqrt(p.x*p.x+p.y*p.y);
                    if (distPixel > pointHitBoxRadius)
                        return; //abort, click is to far away from selction

                    //update selected
                    boolean hit = selectPointNear(pixelX, pixelY, pointHitBoxRadius);

                    if (panel.mapping.getMappingPoints().length <= 1) return;   //dont allow to delete the last
                    // control point
                    MappingPoint[] newPoints = Arrays.stream(panel.mapping.getMappingPoints())
                            .filter(f -> !isInputSelected(f.input))
                            .toArray(MappingPoint[]::new);
                    Arrays.fill(selectedInputs, false);
                    updateMapping(mapping.withNewPoints(newPoints));
                }
            }
        });

        grid.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                // Get the pixel coordinates of the click
                Point pressed = pixelToGrid(e.getX(), e.getY());
                int gridX = pressed.x;
                int gridY = pressed.y;
                mousePosX = e.getX();
                mousePosY = e.getY();
                mouseMoved = true;

                if (drag) {
                    // move selected control points to current mouse position
                    MappingPoint[] points = mapping.getMappingPoints().clone();
                    boolean changed = false;
                    boolean anotherPointPresent = Arrays.stream(points).anyMatch(p -> p.input == gridX);
                    if (anotherPointPresent) {
                        return; //do not overwrite existing points
                    }
                    for (int i = 0; i < points.length; i++) {
                        mapping.getMappingPoints()[i] = points[i];

                        if (isInputSelected(points[i].input)) {
                            setInputSelection(points[i], false);  //deselect old
                            points[i] = new MappingPoint(mapping.sanitizeInput(gridX), mapping.sanitizeOutput(gridY));
                            setInputSelection(points[i], true);   //reselect updated value
                            changed = true;
                        }
                    }
                    if (changed) updateMapping(mapping.withNewPoints(points));
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                // Optional: Handle mouse movement when not dragging
                mousePosX = e.getX();
                mousePosY = e.getY();
                Point gridPos = pixelToGrid(mousePosX, mousePosY);
                if (showCursor && mapping.sanitizeInput(gridPos.x) == gridPos.x &&
                        mapping.sanitizeOutput(gridPos.y) == gridPos.y) {
                    grid.repaint();
                }
                mouseMoved = true;
            }
        });

        this.setLayout(new BorderLayout());
        this.add(grid, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new BorderLayout());
        JCheckBox cursorCheckbox = new JCheckBox("cursor");
        cursorCheckbox.setSelected(showCursor);
        //buttons.add(cursorCheckbox, BorderLayout.WEST);
        cursorCheckbox.addActionListener(e -> {
            if (showCursor != cursorCheckbox.isSelected()) {
                showCursor = cursorCheckbox.isSelected();
                grid.repaint();
            }
        });
        this.add(buttons, BorderLayout.SOUTH);
        this.setPreferredSize(new Dimension(pixelSizeX, pixelSizeY));
        //keep this, otherwise the size is fucked up
        setBorder(null); // Remove the border
    }

    private void paintLineInGrid(float x1, float x2, float y1, float y2, Graphics g) {
        Point start, end;
        start = gridToPixel(x1, x2);
        end = gridToPixel(y1, y2);
        g.drawLine(start.x, start.y, end.x, end.y);
    }

    protected void paintCursor(Graphics g) {
        if (!mouseMoved) return;
        if (!showCursor) return;
        g.setColor(Color.gray);
        ((Graphics2D) g).setStroke(new BasicStroke(2,
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND,
                0,
                new float[]{4, 10},
                0));
        // find closest grid pos to mouse
        Point gridPos = pixelToGrid(mousePosX, mousePosY);
        if (mapping.sanitizeInput(gridPos.x) != gridPos.x || mapping.sanitizeOutput(gridPos.y) != gridPos.y) return;
        paintLineInGrid(gridPos.x, mapping.output.getMinValue(), gridPos.x, mapping.output.getMaxValue(), g);
        paintLineInGrid(mapping.input.getMinValue(), gridPos.y, mapping.input.getMaxValue(), gridPos.y, g);
        Point pixelPos = gridToPixel(gridPos.x, gridPos.y);

        //circle
        ((Graphics2D) g).setStroke(new BasicStroke(2));
        int radius = 30;
        g.drawOval(pixelPos.x - radius / 2, pixelPos.y - radius / 2, radius, radius);

        //dot on mapped value
        int mapped = mapping.map(gridPos.x);
        g.setColor(Color.RED);
        radius = 8;
        pixelPos = gridToPixel(gridPos.x, mapped);
        ((Graphics2D) g).setStroke(new BasicStroke(2));

        g.drawRect(pixelPos.x - radius / 2, pixelPos.y - radius / 2, radius, radius);
        resetSelection();
    }

    protected void paintCurveLines(Graphics2D g) {
        g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{4, 4}, 0));
        g.setColor(Color.red);
        if (mapping.output.isDiscrete()) {
            if (mapping.getMappingPoints().length > 0) {
                MappingPoint p = mapping.getMappingPoints()[0];
                paintLineInGrid(mapping.input.getMinValue(), p.output, p.input, p.output, g);

                p = mapping.getMappingPoints()[mapping.getMappingPoints().length - 1];
                paintLineInGrid(p.input, p.output, mapping.input.getMaxValue(), p.output, g);
            }
            for (int i = 1; i < mapping.getMappingPoints().length; i++) {
                MappingPoint a2 = mapping.getMappingPoints()[i];
                MappingPoint a1 = mapping.getMappingPoints()[i - 1];
                paintLineInGrid(a1.input + 0.5f, a2.output, a2.input, a2.output, g);   //left right
                //     paintLineInGrid(a1.input + 0.5f, a1.output, a1.input + 0.5f, a2.output, g);   //up down
            }
        } else {
            {
                if (mapping.getMappingPoints().length != 0) {
                    MappingPoint a = mapping.getMappingPoints()[0];
                    paintLineInGrid(mapping.input.getMinValue(), a.output, a.input, a.output, g);

                    MappingPoint b = mapping.getMappingPoints()[mapping.getMappingPoints().length - 1];
                    paintLineInGrid(b.input, b.output, mapping.input.getMaxValue(), b.output, g);
                }
            }
            for (int i = 0; i < mapping.getMappingPoints().length - 1; i++) {
                MappingPoint a = mapping.getMappingPoints()[i];
                MappingPoint b = mapping.getMappingPoints()[i + 1];

                paintLineInGrid(a.input, a.output, b.input, b.output, g);
            }
        }
        for (int i = mapping.input.getMinValue(); i <= mapping.input.getMaxValue(); i++) {
            if (isInputSelected(i)) {
                Point gridPos = gridToPixel(i, mapping.map(i));
                int size = 4;
                g.drawRect(gridPos.x - size / 2, gridPos.y - size / 2, size, size);
            }
        }
    }

    private void paintGrid(Graphics g) {
        if (this.mapping == null) return;
        super.paintComponent(g);
        int padding = fontheight * 2;
        shiftX = widthOutputStrings + padding;  //add some empty space ot the right
        // large?
        shiftY = fontheight;
        pixelSizeY = (int) Math.ceil(getHeight() - shiftY - fontheight * 1.5f - padding);
        pixelSizeX = getWidth() - shiftX - padding; //right padding
        this.GRID_X_SCALE = pixelSizeX / ((float) mapping.input.getMaxValue() - mapping.input.getMinValue());
        this.GRID_Y_SCALE = pixelSizeY / ((float) mapping.output.getMaxValue() - mapping.output.getMinValue());

        Graphics2D g2d = (Graphics2D) g;

        // Enable anti-aliasing for smoother lines
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw grid
        g2d.setColor(Color.LIGHT_GRAY);


        int rangeX = mapping.input.getMaxValue() - mapping.input.getMinValue();
        int rangeY = mapping.output.getMaxValue() - mapping.output.getMinValue();


        int stepX = 1;
        if (rangeX > 20) stepX = 10;
        if (rangeX > 100) stepX = 50;

        // x axis grid
        fontheight = g2d.getFontMetrics().getHeight();
        for (int i = stepX; i <= mapping.input.getMaxValue(); i += stepX) {
            Point start = gridToPixel(i, mapping.output.getMinValue());
            Point end = gridToPixel(i, mapping.output.getMaxValue());
            g2d.drawLine(start.x, start.y, end.x, end.y);
            String s = mapping.input.valueToString(i);
            int width = g2d.getFontMetrics().stringWidth(s);
            g2d.drawString(s, start.x - 0.3f * width, start.y + fontheight * 1.5f);
        }
        for (int i = 0; i >= mapping.input.getMinValue(); i -= stepX) {
            Point start = gridToPixel(i, mapping.output.getMinValue());
            Point end = gridToPixel(i, mapping.output.getMaxValue());
            g2d.drawLine(start.x, start.y, end.x, end.y);
            String s = mapping.input.valueToString(i);
            int width = g2d.getFontMetrics().stringWidth(s);
            g2d.drawString(s, start.x - 0.5f * width, start.y + fontheight * 1.5f);
        }

        int stepY = 1;
        if (rangeY > 20) stepY = 10;
        if (rangeY > 100) stepY = 50;

        // y axis grid
        for (int i = stepY; i <= mapping.output.getMaxValue(); i += stepY) {
            Point start = gridToPixel(mapping.input.getMinValue(), i);
            Point end = gridToPixel(mapping.input.getMaxValue(), i);
            g2d.drawLine(start.x, start.y, end.x, end.y);
            String s = mapping.output.valueToString(i);
            int width = g2d.getFontMetrics().stringWidth(s);
            widthOutputStrings = Math.max(widthOutputStrings, width + fontheight);

            g2d.drawString(s, start.x - (width + fontheight), start.y + 0.5f * fontheight);
        }
        for (int i = 0; i >= mapping.output.getMinValue(); i -= stepY) {
            Point start = gridToPixel(mapping.input.getMinValue(), i);
            Point end = gridToPixel(mapping.input.getMaxValue(), i);
            g2d.drawLine(start.x, start.y, end.x, end.y);
            String s = mapping.output.valueToString(i);
            int width = g2d.getFontMetrics().stringWidth(s);
            widthOutputStrings = Math.max(widthOutputStrings, width + fontheight);
            g2d.drawString(s, start.x - (width + fontheight), start.y + 0.5f * fontheight);
        }

        {
            g2d.setFont(new Font(g2d.getFont().getName(), g2d.getFont().getStyle(), g2d.getFont().getSize() * 3));
            int fontheight = g2d.getFontMetrics().getHeight();

            Point center = gridToPixel((mapping.input.getMaxValue() + mapping.input.getMinValue()) / 2f,
                    (mapping.output.getMinValue() + mapping.output.getMaxValue()) / 2f);
            Point zero = gridToPixel(mapping.input.getMinValue(), mapping.output.getMinValue());
            Point extent = gridToPixel(mapping.input.getMaxValue(), mapping.output.getMaxValue());
            {
                g2d.drawRect(zero.x, zero.y, extent.x - zero.x, extent.y - zero.y);
            }
            {
                String s = mapping.input.getName();
                int width = g2d.getFontMetrics().stringWidth(s);
                g2d.drawString(s, center.x - 0.5f * width, zero.y - fontheight * 0.2f);
            }

            {
                String s = mapping.output.getName();
                int width = g2d.getFontMetrics().stringWidth(s);

                g2d.translate(zero.x, center.y);
                g2d.drawRect(center.x, center.y, 10, 10);
                g2d.rotate(-Math.toRadians(90));
                g2d.drawString(s, -width / 2f, 1.2f * fontheight);
                g2d.rotate(Math.toRadians(90));
                g2d.translate(-zero.x, -center.y);
            }
        }

        paintMappingPoints(g2d);
        paintCurveLines((Graphics2D) g);
        paintCursor(g);
        mouseMoved = false;
    }

    private void paintMappingPoints(Graphics2D g2d) {
        // Draw mapping points
        g2d.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{4, 4}, 0));
        g2d.setColor(Color.red);
        for (MappingPoint p : mapping.getMappingPoints()) {
            int radius = 10;  // Circle radius
            Point start = gridToPixel(p.input, p.output);

            int x1 = start.x;
            int y1 = start.y;

            if (isInputSelected(p.input)) {
                int size = radius * 4;
                g2d.drawRect(x1 - size / 2, y1 - size / 2, size, size);
            }
            g2d.fillOval(x1 - radius / 2, y1 - radius / 2, radius, radius);
        }
    }

    private Point pixelToGrid(int pixelX, int pixelY) {
        // Convert the pixel coordinates to grid coordinates
        int gridXPressed = Math.round((pixelX - shiftX) / GRID_X_SCALE) + mapping.input.getMinValue();
        int gridYPressed =
                Math.round((pixelSizeY - (pixelY - shiftY)) / GRID_Y_SCALE) + mapping.output.getMinValue(); //
        // Flip
        // the Y-axis
        return new Point(gridXPressed, gridYPressed);
    }

    // Callback method to handle the click event (pass the grid coordinates)
    private boolean selectPointNear(int pixelX, int pixelY, int maxDist) {
        ArrayList<MappingPoint> points = new ArrayList<>();
        for (MappingPoint p : mapping.getMappingPoints()) {
            points.add(p);
        }
        final Function<MappingPoint, Double> distance = p -> {
            Point p1 = gridToPixel(p.input,p.output);

            double dX = p1.x - pixelX;
            double dY = p1.y - pixelY;
            return Math.sqrt(dX * dX + dY * dY);
        };
        points.sort(new Comparator<MappingPoint>() {

            @Override
            public int compare(MappingPoint o1, MappingPoint o2) {
                double dist1 = distance.apply(o1);
                double dist2 = distance.apply(o2);
                return Double.compare(dist1, dist2);
            }
        });
        MappingPoint closest = points.get(0);

        resetSelection();
        if (distance.apply(closest) > maxDist) return false;

        setInputSelection(closest, true);
        this.repaint();
        return true;
    }

    private MappingPoint lastClickedPoint;
    private float distanceBetween(int x1, int y1, int x2, int y2) {
        float dX = x1 - x2;
        float dY = y1 - y2;
        return (float) Math.sqrt(dX * dX + dY * dY);
    }

    private Point gridToPixel(float gridX, float gridY) {
        int pixelX = Math.round((gridX - mapping.input.getMinValue()) * GRID_X_SCALE) + shiftX;
        int pixelY = Math.round(pixelSizeY - ((gridY - mapping.output.getMinValue()) * GRID_Y_SCALE)) + shiftY;

        return new Point(pixelX, pixelY);
    }

    @Override
    public void setOnSelect(Consumer<boolean[]> onSelect) {
        this.onSelect = onSelect;
    }

    @Override
    public void setSelectedInputs(boolean[] selectedPointIdx) {
        if (mapping == null) return;
        int l = mapping.getMappingPoints().length;
        Arrays.fill(selectedInputs, false);
        selectedInputs = selectedPointIdx;
        this.repaint();
    }

    // Helper class to represent a line
    private static class Line {
        int x, y, x1, y1;

        Line(int x, int y, int x1, int y1) {
            this.x = x;
            this.y = y;
            this.x1 = x1;
            this.y1 = y1;
        }
    }
}
