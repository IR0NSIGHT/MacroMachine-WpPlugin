package org.ironsight.wpplugin.expandLayerTool.Gui;

import org.ironsight.wpplugin.expandLayerTool.operations.MappingPoint;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.function.Consumer;

public class MappingTextTable extends LayerMappingPanel implements IMappingPointSelector {
    int selectedPointIdx;
    DefaultTableModel tableModel;
    TableModelListener listener;

    private Consumer<Integer> onSelect = f -> {
    };
    private JTable numberTable;
    private boolean groupValues = true;

    @Override
    protected void updateComponents() {
        MappingPointValue[][] data;
        Object[] columnNames = new String[]{mapping.input.getName(), mapping.output.getName()};
        if (!groupValues) {
            data = new MappingPointValue[mapping.input.getMaxValue() - mapping.input.getMinValue()][];

            for (int i = mapping.input.getMinValue(); i < mapping.input.getMaxValue(); i++) {
                data[i - mapping.input.getMinValue()] = new MappingPointValue[]{new MappingPointValue(i, mapping.input),
                        new MappingPointValue(mapping.map(i), mapping.output)};
            }
        } else {
            data = new MappingPointValue[mapping.getMappingPoints().length * 2 + 2][];
            int i = 0;
            data[i++] = new MappingPointValue[]{new MappingPointValue(mapping.input.getMinValue(), mapping.input),
                    new MappingPointValue(mapping.map(mapping.input.getMinValue()), mapping.output)};

            for (MappingPoint p : mapping.getMappingPoints()) {
                data[i++] = new MappingPointValue[]{new MappingPointValue(p.input, mapping.input),
                        new MappingPointValue(p.output, mapping.output)};
                data[i++] = new MappingPointValue[]{new MappingPointValue(p.input+1, mapping.input),
                        new MappingPointValue(mapping.map(p.input+1), mapping.output)};
            }
            data[i++] = new MappingPointValue[]{new MappingPointValue(mapping.input.getMaxValue(), mapping.input),
                    new MappingPointValue(mapping.map(mapping.input.getMaxValue()), mapping.output)};
        }


        selectedPointIdx = Math.min(selectedPointIdx, mapping.getMappingPoints().length - 1);

        this.tableModel = new DefaultTableModel(data, columnNames);
        this.tableModel.addTableModelListener(this.listener);
        numberTable.setModel(tableModel);

        Font font = new Font("Arial", Font.PLAIN, 24);
        numberTable.setFont(font);
        FontMetrics fontMetrics = numberTable.getFontMetrics(font);

        // Place the selection cursor in the i-th row
        if (selectedPointIdx >= 0 && selectedPointIdx < numberTable.getRowCount()) {
            numberTable.setRowSelectionInterval(selectedPointIdx, selectedPointIdx);
            numberTable.scrollRectToVisible(numberTable.getCellRect(selectedPointIdx, 0, true));
        }

        numberTable.setDefaultRenderer(Object.class, new MappingPointCellRenderer());
        numberTable.setDefaultEditor(Object.class, new MappingPointCellEditor());
        numberTable.setCellSelectionEnabled(true);

    }

    @Override
    protected void initComponents() {
        Border padding = new EmptyBorder(20, 20, 20, 20); // 20px padding on all sides
        Border whiteBorder = new EmptyBorder(5, 5, 5, 5); // 5px white border
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

    protected boolean parseAndSetValue(Object newValue, int row, int column) {
        assert newValue instanceof MappingPointValue;
        MappingPoint[] points = mapping.getMappingPoints().clone();
        if (column == 0) {
            points[row] = new MappingPoint(((MappingPointValue) newValue).numericValue, points[row].output);
        } else {
            points[row] = new MappingPoint(points[row].input, ((MappingPointValue) newValue).numericValue);
        }
        updateMapping(mapping.withNewPoints(points));
        return true;
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


