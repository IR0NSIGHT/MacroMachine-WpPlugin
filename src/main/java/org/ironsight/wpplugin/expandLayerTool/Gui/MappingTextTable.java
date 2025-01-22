package org.ironsight.wpplugin.expandLayerTool.Gui;

import org.ironsight.wpplugin.expandLayerTool.operations.LayerMapping;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.function.Consumer;

public class MappingTextTable extends JPanel {
    public void setMapping(LayerMapping mapping) {
        this.mapping = mapping;
    }

    private LayerMapping mapping;
    private final Consumer<LayerMapping> onUpdate;
    private JTable numberTable;
    DefaultTableModel tableModel;
    TableModelListener listener;

    public MappingTextTable(LayerMapping mapping, Consumer<LayerMapping> onUpdate) {
        this.mapping = mapping;
        this.onUpdate = onUpdate;
        init();
        updateComponents();
    }

    private void init() {
        Border padding = new EmptyBorder(20, 20, 20, 20); // 20px padding on all sides
        Border whiteBorder = new LineBorder(Color.WHITE, 5); // 5px white border
        setBorder(BorderFactory.createCompoundBorder(whiteBorder, padding));

        // Add a TableModelListener to get a callback when a cell is edited
        numberTable = new JTable();
        JScrollPane scrollPane = new JScrollPane(numberTable);
        this.add(scrollPane, BorderLayout.CENTER);

        listener = new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                // Check if the event is due to a cell update
                if (e.getType() == TableModelEvent.UPDATE) {
                    int row = e.getFirstRow(); // Get the row of the edited cell
                    int column = e.getColumn(); // Get the column of the edited cell
                    Object newValue = tableModel.getValueAt(row, column); // Get the new value
                    if (!(newValue instanceof String))
                        return;
                    try {
                        int newValueInt = Integer.parseInt((String) newValue);
                        LayerMapping.MappingPoint[] points = mapping.getMappingPoints();
                        if (column == 0) {
                            points[row] = new LayerMapping.MappingPoint(newValueInt, points[row].output);
                        } else {
                            points[row] = new LayerMapping.MappingPoint(points[row].input, newValueInt);
                        }
                        mapping = new LayerMapping(null, null, points);
                        onUpdate.accept(mapping);
                    } catch (NumberFormatException exception) {
                        exception.printStackTrace();
                    }
                    updateComponents();
                }
            }
        };
    }

    private void updateComponents() {
        Object[][] data = new Object[mapping.getMappingPoints().length][];
        Object[] columnNames = new String[]{"input", "output"};
        //table.setPreferredSize(outerPanel.getPreferredSize());
        for (int i = 0; i < mapping.getMappingPoints().length; i++) {
            LayerMapping.MappingPoint a = mapping.getMappingPoints()[i];
            data[i] = new Object[]{a.input, a.output};
        }

        this.tableModel = new DefaultTableModel(data, columnNames);
        this.tableModel.addTableModelListener(this.listener);
        // Set the model to the JTable
        numberTable.setModel(tableModel);
        numberTable.revalidate();
        numberTable.repaint();
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("TEST PANEL");

        LayerMapping mapper = new LayerMapping(null, null,
                new LayerMapping.MappingPoint[]{
                        new LayerMapping.MappingPoint(20, 10),
                        new LayerMapping.MappingPoint(50, 50),
                        new LayerMapping.MappingPoint(70, 57),
                });

        MappingTextTable table = new MappingTextTable(mapper, f -> {
        });

        // Add the outer panel to the frame
        frame.add(table);

        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
