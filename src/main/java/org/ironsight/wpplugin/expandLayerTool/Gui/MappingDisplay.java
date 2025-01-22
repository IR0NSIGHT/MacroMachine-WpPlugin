package org.ironsight.wpplugin.expandLayerTool.Gui;

import org.ironsight.wpplugin.expandLayerTool.operations.LayerMapping;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class GridPanel extends JPanel {
    private static final int GRID_SIZE = 100;  // Number of cells in both dimensions
    private static final int CELL_SIZE = 10;  // Size of each cell in pixels
    private final List<Line> lines = new ArrayList<>();  // List to store lines

    public GridPanel() {
        this.setPreferredSize(new Dimension(GRID_SIZE * CELL_SIZE, GRID_SIZE * CELL_SIZE));
        setBorder(BorderFactory.createEmptyBorder(100, 100, 100, 100));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Enable anti-aliasing for smoother lines
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw grid
        g2d.setColor(Color.LIGHT_GRAY);

        for (int i = 0; i <= GRID_SIZE; i += 10) {
            // Vertical lines
            g2d.drawLine(i * CELL_SIZE, 0, i * CELL_SIZE, GRID_SIZE * CELL_SIZE);
            // Horizontal lines
            g2d.drawLine(0, i * CELL_SIZE, GRID_SIZE * CELL_SIZE, i * CELL_SIZE);
        }

        // Draw lines from the list
        g2d.setColor(Color.RED);
        g2d.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{5, 5}, 0));
        for (Line line : lines) {
            int x1 = line.x * CELL_SIZE;
            int y1 = (GRID_SIZE - line.y) * CELL_SIZE;  // Flip the y-axis
            int x2 = line.x1 * CELL_SIZE;
            int y2 = (GRID_SIZE - line.y1) * CELL_SIZE; // Flip the y-axis
            g2d.drawLine(x1, y1, x2, y2);

            int radius = 6;  // Circle radius
            g2d.fillOval(x1 - radius / 2, y1 - radius / 2, radius, radius);
            g2d.fillOval(x2 - radius / 2, y2 - radius / 2, radius, radius);
        }

        // Draw circles at points
        g2d.setColor(Color.RED);
    }

    /**
     * Adds a line to be drawn on the grid from (x, y) to (x1, y1).
     *
     * @param x  The x-coordinate of the starting point (0 to 100).
     * @param y  The y-coordinate of the starting point (0 to 100).
     * @param x1 The x-coordinate of the ending point (0 to 100).
     * @param y1 The y-coordinate of the ending point (0 to 100).
     */
    public void drawLine(int x, int y, int x1, int y1) {
        lines.add(new Line(x, y, x1, y1));
        repaint();  // Request a repaint to update the display
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

    public static void main(String[] args) {
        JFrame frame = new JFrame("Grid Panel");

        GridPanel gridPanel = new GridPanel();

        LayerMapping mapper = new LayerMapping(null, null,
                new LayerMapping.MappingPoint[]{
                        new LayerMapping.MappingPoint(20, 10),
                        new LayerMapping.MappingPoint(50, 50),
                        new LayerMapping.MappingPoint(70, 57),
                });
        {
            LayerMapping.MappingPoint a = mapper.getMappingPoints()[0];
            gridPanel.drawLine(0, a.output, a.input, a.output);

            LayerMapping.MappingPoint b = mapper.getMappingPoints()[mapper.getMappingPoints().length - 1];
            gridPanel.drawLine(b.input, b.output, 100, b.output);
        }
        for (int i = 0; i < mapper.getMappingPoints().length - 1; i++) {
            LayerMapping.MappingPoint a = mapper.getMappingPoints()[i];
            LayerMapping.MappingPoint b = mapper.getMappingPoints()[i + 1];

            gridPanel.drawLine(a.input, a.output, b.input, b.output);
        }

        JPanel outerPanel = new JPanel();
        outerPanel.setLayout(new BorderLayout(20, 20));
        outerPanel.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));  // Set outer panel border (50px all
        // around)
        outerPanel.add(gridPanel, BorderLayout.CENTER);

        // Add the outer panel to the frame
        frame.add(outerPanel);


        JPanel all = new JPanel();
        all.add(gridPanel);


        Object[][] data = new Object[mapper.getMappingPoints().length][];
        //table.setPreferredSize(outerPanel.getPreferredSize());
        for (int i = 0; i < mapper.getMappingPoints().length; i++) {
            LayerMapping.MappingPoint a = mapper.getMappingPoints()[i];
            data[i] = new Object[]{a.input, a.output};
        }
        // Column names
        String[] columnNames = {"Input", "Output"};

        // Create a table with the data and column names
        JTable table1 = new JTable(data, columnNames);
        // Place the table inside a JScrollPane (scrollable)
        JScrollPane scrollPane = new JScrollPane(table1);
        all.add(scrollPane);

        frame.add(all);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
