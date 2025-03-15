package org.ironsight.wpplugin.expandLayerTool.Gui;

import org.ironsight.wpplugin.expandLayerTool.operations.LayerMapping;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class VirtualScrollingTableExample extends JPanel {
    int firstVisibleRow = 0;
    int lastVisibleRow = 100;
    VirtualTableModel model = new VirtualTableModel();

    public void setMappings(ArrayList<LayerMapping> actions) {
        model.setMappings(actions);
    }

    public VirtualScrollingTableExample() {
        // Create a JTable with the custom model
        JTable table = new JTable(model);

        // Add the table to a JScrollPane
        JScrollPane scrollPane = new JScrollPane(table);

        // Add a listener to handle scrolling events
        scrollPane.getVerticalScrollBar().addAdjustmentListener(e -> {
            int rowChange = e.getValue()/table.getRowHeight();
            if (rowChange == 0)
                return;
            System.out.println("scroll adjust by "+ rowChange + " rows");
            firstVisibleRow = Math.max(firstVisibleRow + rowChange, 0);
            lastVisibleRow = Math.max(lastVisibleRow + rowChange, 0);
            model.fetchData(firstVisibleRow, lastVisibleRow);
        });

        table.setDefaultRenderer(MappingPointValue.class, new MappingPointCellRenderer());
        table.setRowHeight(new MappingPointCellRenderer().getPreferredHeight());

        this.setLayout(new BorderLayout());
        this.add(scrollPane, BorderLayout.CENTER);
    }

    public static void main(String[] args) {
        // Create a new JFrame
        JFrame frame = new JFrame("Virtual Scrolling Table Example");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);

        frame.add(new VirtualScrollingTableExample(), BorderLayout.CENTER);


        // Make the frame visible
        frame.setVisible(true);
    }
}