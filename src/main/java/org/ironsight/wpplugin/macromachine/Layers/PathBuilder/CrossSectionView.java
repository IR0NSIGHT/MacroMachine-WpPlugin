package org.ironsight.wpplugin.macromachine.Layers.PathBuilder;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * A cut through the terrain: a 1D heightmap, one {@link Column} per horizontal
 * block, drawn to scale ({@link #BLOCK} pixels a block). Each column has its
 * own surface colour; the ground below the surface, the water and the sky are
 * coloured alike for all columns. The highest and lowest height shown are
 * labelled on the left.
 */
public class CrossSectionView extends JPanel
{
    /** Pixels per block, both ways. */
    public static final int BLOCK = 5;
    /** Width of the height labels left of the section. */
    static final int LABEL_WIDTH = 34;
    /** Columns the preferred width has room for; more are cut off at both ends. */
    static final int PREFERRED_COLUMNS = 40;
    /** A column without water. */
    public static final int NO_WATER = Integer.MIN_VALUE;

    /**
     * One block wide slice: the top solid block at {@code terrainHeight} (drawn in
     * {@code surface}), water from above the terrain up to {@code waterHeight}
     * (none if not higher than the terrain, or {@link #NO_WATER}).
     */
    public record Column(int terrainHeight, int waterHeight, Color surface) {
        public boolean hasWater() {
            return waterHeight != NO_WATER && waterHeight > terrainHeight;
        }

        /** The highest block of this column, water included. */
        public int top() {
            return hasWater() ? waterHeight : terrainHeight;
        }
    }

    private List<Column> columns = List.of();
    private Color subsurface = new Color(134, 96, 67), water = new Color(52, 120, 220), sky = new Color(214, 220, 226);

    /** The ground below each column's surface block. */
    public void setSubsurfaceColor(Color subsurface) {
        this.subsurface = subsurface;
        repaint();
    }

    public void setWaterColor(Color water) {
        this.water = water;
        repaint();
    }

    public void setSkyColor(Color sky) {
        this.sky = sky;
        repaint();
    }

    public Color getSubsurfaceColor() {
        return subsurface;
    }

    public Color getWaterColor() {
        return water;
    }

    public Color getSkyColor() {
        return sky;
    }

    public void setColumns(List<Column> columns) {
        this.columns = List.copyOf(columns);
        revalidate();
        repaint();
    }

    public List<Column> getColumns() {
        return columns;
    }

    /** The highest block shown (terrain or water); 0 without columns. */
    public int maxHeight() {
        return columns.stream().mapToInt(Column::top).max().orElse(0);
    }

    /** The lowest terrain block shown; 0 without columns. */
    public int minHeight() {
        return columns.stream().mapToInt(Column::terrainHeight).min().orElse(0);
    }

    /**
     * Rows shown: one block of sky above the highest, one of ground below the
     * lowest.
     */
    private int rows() {
        return columns.isEmpty() ? 0 : maxHeight() - minHeight() + 3;
    }

    @Override
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet())
            return super.getPreferredSize();
        // the container sets the width: columns that do not fit are cut off at
        // both ends, the middle stays shown
        return new Dimension(LABEL_WIDTH + PREFERRED_COLUMNS * BLOCK, Math.max(2 * BLOCK, rows() * BLOCK));
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(LABEL_WIDTH, getPreferredSize().height);
    }

    /** The pixel row of the top of block {@code height}. */
    int rowOf(int height) {
        return (maxHeight() + 1 - height) * BLOCK;
    }

    /**
     * The pixel column of column {@code index}: the section is centred right of the
     * labels.
     */
    int xOf(int index) {
        int area = getWidth() - LABEL_WIDTH;
        return LABEL_WIDTH + (area - columns.size() * BLOCK) / 2 + index * BLOCK;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (columns.isEmpty())
            return;
        var g = (Graphics2D) graphics.create();
        try {
            int height = getHeight();
            g.clipRect(LABEL_WIDTH, 0, getWidth() - LABEL_WIDTH, height);
            for (int i = 0; i < columns.size(); i++) {
                var column = columns.get(i);
                int x = xOf(i);
                int surface = rowOf(column.terrainHeight());
                g.setColor(sky);
                g.fillRect(x, 0, BLOCK, surface);
                if (column.hasWater()) {
                    int waterRow = rowOf(column.waterHeight());
                    g.setColor(water);
                    g.fillRect(x, waterRow, BLOCK, surface - waterRow);
                }
                g.setColor(column.surface());
                g.fillRect(x, surface, BLOCK, BLOCK);
                g.setColor(subsurface);
                g.fillRect(x, surface + BLOCK, BLOCK, height - surface - BLOCK);
            }
            // the highest and lowest height, as lines across and labels left
            g.setClip(null);
            g.setFont(g.getFont().deriveFont(10f));
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int left = xOf(0), right = xOf(columns.size());
            g.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{3, 3}, 0));
            g.setColor(new Color(0, 0, 0, 120));
            int maxRow = rowOf(maxHeight()), minRow = rowOf(minHeight()) + BLOCK - 1;
            g.drawLine(Math.max(LABEL_WIDTH, left), maxRow, right, maxRow);
            g.drawLine(Math.max(LABEL_WIDTH, left), minRow, right, minRow);
            g.setColor(getForeground());
            var metrics = g.getFontMetrics();
            g.drawString(String.valueOf(maxHeight()), 2,
                    Math.max(metrics.getAscent(), maxRow + metrics.getAscent() / 2));
            g.drawString(String.valueOf(minHeight()), 2, Math.min(height - 2, minRow + metrics.getAscent() / 2));
        } finally {
            g.dispose();
        }
    }
}
