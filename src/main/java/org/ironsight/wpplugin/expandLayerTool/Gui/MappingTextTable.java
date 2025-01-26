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

public class MappingTextTable extends JPanel implements IMappingEditor {
    int selectedPointIdx;
    DefaultTableModel tableModel;
    TableModelListener listener;
    private Consumer<LayerMapping> onUpdate = f -> {
    };

    private Consumer<Integer> onSelect = f -> {
    };
    private LayerMapping mapping;
    private JTable numberTable;

    public MappingTextTable(LayerMapping mapping) {
        this.mapping = mapping;

        init();
        updateComponents();
    }

    private void init() {
        Border padding = new EmptyBorder(20, 20, 20, 20); // 20px padding on all sides
        Border whiteBorder = new EmptyBorder( 5,5,5,5); // 5px white border
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
                    parseAndSetValue(newValue, row, column);
                }
            }
        };

        numberTable.getSelectionModel().addListSelectionListener(event -> {
            // Ignore the event if it's being adjusted (e.g., dragging selection)
            if (!event.getValueIsAdjusting()) {
                int selectedRow = numberTable.getSelectedRow();
                if (selectedRow != -1) {
                    // Callback when a row is clicked
                    onSelect.accept(selectedRow);
                }
            }
        });
    }

    private void updateComponents() {
        Object[][] data = new Object[mapping.getMappingPoints().length][];
        Object[] columnNames = new String[]{mapping.input.getName(), mapping.output.getName()};
        for (int i = 0; i < mapping.getMappingPoints().length; i++) {
            LayerMapping.MappingPoint a = mapping.getMappingPoints()[i];
            data[i] = new Object[]{mapping.input.valueToString(a.input),mapping.output.valueToString(a.output)};
        }

        this.tableModel = new DefaultTableModel(data, columnNames);
        this.tableModel.addTableModelListener(this.listener);
        numberTable.setModel(tableModel);

        Font font = new Font("Arial", Font.PLAIN, 24);
        numberTable.setFont(font);
        FontMetrics fontMetrics = numberTable.getFontMetrics(font);
        int rowHeight = fontMetrics.getHeight() + 10;
        numberTable.setRowHeight(rowHeight);

        // Place the selection cursor in the i-th row
        if (selectedPointIdx >= 0 && selectedPointIdx < numberTable.getRowCount()) {
            numberTable.setRowSelectionInterval(selectedPointIdx, selectedPointIdx);
            numberTable.scrollRectToVisible(numberTable.getCellRect(selectedPointIdx, 0, true));
        }

        numberTable.revalidate();
        numberTable.repaint();
    }

    protected boolean parseAndSetValue(Object newValue, int row, int column) {
        int newValueInt;
        if (newValue instanceof Integer) newValueInt = (Integer) newValue;
        else if (newValue instanceof String) try {
            newValueInt = Integer.parseInt((String) newValue);
        } catch (NumberFormatException exception) {
            exception.printStackTrace();
            return false;
        }
        else return false;

        LayerMapping.MappingPoint[] points = mapping.getMappingPoints();

        if (column == 0) {
            if (points[row].input == newValueInt) return false;
            points[row] = new LayerMapping.MappingPoint(newValueInt, points[row].output);
        } else {
            if (points[row].output == newValueInt) return false;
            points[row] = new LayerMapping.MappingPoint(points[row].input, newValueInt);
        }
        mapping = mapping.withNewPoints(points);
        onUpdate.accept(mapping);

        updateComponents();
        return true;
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("TEST PANEL");

        LayerMapping mapper = new LayerMapping(null, null,
                new LayerMapping.MappingPoint[]{new LayerMapping.MappingPoint(20, 10),
                        new LayerMapping.MappingPoint(50, 50), new LayerMapping.MappingPoint(70, 57),});

        MappingTextTable table = new MappingTextTable(mapper);

        // Add the outer panel to the frame
        frame.add(table);

        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    public void setMapping(LayerMapping mapping) {
        this.mapping = mapping;
        updateComponents();
    }

    @Override
    public void setOnUpdate(Consumer<LayerMapping> onUpdate) {
        this.onUpdate = onUpdate;
    }

    @Override
    public void setOnSelect(Consumer<Integer> onSelect) {
        this.onSelect = onSelect;
    }

    @Override
    public void setSelected(Integer selectedPointIdx) {
        if (selectedPointIdx != this.selectedPointIdx) {
            this.selectedPointIdx = selectedPointIdx;
            updateComponents();
        }
    }
}
