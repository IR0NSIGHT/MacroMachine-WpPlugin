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

public class MappingGridPanel extends LayerMappingPanel {
    int widthOutputStrings = 0;
    int fontheight;
    private int shiftX = 50;
    private int pixelSizeX = 500;
    private int pixelSizeY = 500;
    private float GRID_X_SCALE;
    private float GRID_Y_SCALE;
    private int shiftY = 10;

    public MappingGridPanel() {
        setBorder(BorderFactory.createEmptyBorder(100, 100, 100, 100));
        initComponents();
    }

    @Override
    protected void updateComponents() {
        System.out.println(" GRID PANEL UPDATE COMPONENTS");
        SwingUtilities.invokeLater(()->{
            revalidate();
            repaint();
        });
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

        this.setLayout(new BorderLayout());
        this.add(grid, BorderLayout.CENTER);

        setBorder(null); // Remove the border
    }

    private void paintLineInGrid(float x1, float x2, float y1, float y2, Graphics g) {
        Point start, end;
        start = gridToPixel(x1, x2);
        end = gridToPixel(y1, y2);
        g.drawLine(start.x, start.y, end.x, end.y);
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
            g2d.fillOval(x1 - radius / 2, y1 - radius / 2, radius, radius);
        }
    }

    private Point gridToPixel(float gridX, float gridY) {
        int pixelX = Math.round((gridX - mapping.input.getMinValue()) * GRID_X_SCALE) + shiftX;
        int pixelY = Math.round(pixelSizeY - ((gridY - mapping.output.getMinValue()) * GRID_Y_SCALE)) + shiftY;

        return new Point(pixelX, pixelY);
    }

}
