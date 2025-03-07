package org.ironsight.wpplugin.expandLayerTool.Gui;

import org.ironsight.wpplugin.expandLayerTool.operations.MappingPoint;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
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
        groupValues = groupValuesCheckBox.isSelected();

        MappingPointValue[][] data;
        Object[] columnNames = new String[]{mapping.input.getName(), mapping.output.getName()};
        if (!groupValues) {
            data = new MappingPointValue[mapping.input.getMaxValue() - mapping.input.getMinValue()][];

            for (int i = mapping.input.getMinValue(); i < mapping.input.getMaxValue(); i++) {
                data[i - mapping.input.getMinValue()] = new MappingPointValue[]{new MappingPointValue(i, mapping.input),
                        new MappingPointValue(mapping.map(i), mapping.output)};
            }
        } else {
            HashSet<Integer> changeValues = new HashSet<>();
            int previousOutput = Integer.MAX_VALUE;
            for (int i = mapping.input.getMinValue(); i <= mapping.input.getMaxValue(); i++) {
                if (mapping.map(i) != previousOutput) {
                    previousOutput = mapping.map(i);
                    changeValues.add(mapping.sanitizeInput(i-1));
                    changeValues.add(mapping.sanitizeInput(i));
                }
            }
            changeValues.add(mapping.input.getMinValue());
            changeValues.add(mapping.input.getMaxValue());

            data = new MappingPointValue[changeValues.size()][];
            int ii = 0;
            for (int i = mapping.input.getMinValue(); i <= mapping.input.getMaxValue(); i++) {
                if (changeValues.contains(i)) {
                    data[ii++] = new MappingPointValue[]{new MappingPointValue(i, mapping.input),
                            new MappingPointValue(mapping.map(i), mapping.output)};
                }
            }
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
       // numberTable.setCellSelectionEnabled(false);
    }
    private JCheckBox groupValuesCheckBox;
    @Override
    protected void initComponents() {
        this.setLayout(new BorderLayout());
        Border padding = new EmptyBorder(20, 20, 20, 20); // 20px padding on all sides
        Border whiteBorder = new EmptyBorder(5, 5, 5, 5); // 5px white border
        setBorder(BorderFactory.createCompoundBorder(whiteBorder, padding));

        // Add a TableModelListener to get a callback when a cell is edited
        numberTable = new JTable(){
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // All cells are non-editable
            }
        };

        JScrollPane scrollPane = new JScrollPane(numberTable);
        this.add(scrollPane, BorderLayout.CENTER);
        JPanel buttons = new JPanel();
        groupValuesCheckBox = new JCheckBox("Group Values");
        groupValuesCheckBox.addActionListener(f  -> {
            if (groupValues != groupValuesCheckBox.isSelected()) {
                groupValues = groupValuesCheckBox.isSelected();
                updateComponents();
            }
        });
        buttons.add(groupValuesCheckBox);
        this.add(buttons, BorderLayout.SOUTH);
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
        numberTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // Add listener to scroll to the selected row
        numberTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = numberTable.getSelectedRow();
                if (selectedRow != -1) {
                    numberTable.scrollRectToVisible(numberTable.getCellRect(selectedRow, 0, true));
                }
            }
        });
    /*    numberTable.getSelectionModel().addListSelectionListener(event -> {
            // Ignore the event if it's being adjusted (e.g., dragging selection)
            if (!event.getValueIsAdjusting()) {
                int selectedRow = numberTable.getSelectedRow();
                if (selectedRow != -1) {
                    // Callback when a row is clicked
                    onSelect.accept(selectedRow);
                }
            }
        });

     */
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
        System.out.println("table select input value =" + selectedPointIdx);
        if (selectedPointIdx != this.selectedPointIdx) {
            this.selectedPointIdx = selectedPointIdx;
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                MappingPointValue value = (MappingPointValue) tableModel.getValueAt(i,0);
                if (value.numericValue == selectedPointIdx) {
                    numberTable.setRowSelectionInterval(i,i);
                    return;
                }
            }
        }
    }
}


