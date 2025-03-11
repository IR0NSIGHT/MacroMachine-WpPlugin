package org.ironsight.wpplugin.expandLayerTool.Gui;

import org.ironsight.wpplugin.expandLayerTool.operations.LayerMapping;
import org.ironsight.wpplugin.expandLayerTool.operations.MappingPoint;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.vecmath.Point2d;
import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;

public class MappingTextTable extends LayerMappingPanel implements IMappingPointSelector {
    int selectedPointIdx;
    DefaultTableModel tableModel;
    TableModelListener listener;

    private Consumer<boolean[]> onSelect = f -> {
    };
    private JTable numberTable;
    private boolean groupValues = false;
    private JCheckBox groupValuesCheckBox;

    private void initTableModel() {
        System.out.println("REBUILD TABLE MODEL");
        if (mapping == null)
            return;
        MappingPointValue[][] data;
        Object[] columnNames;
        HashMap<Integer, Integer> mappingPointByInput = new HashMap<>();
        {
            int index = 0;
            for (MappingPoint p : mapping.getMappingPoints()) {
                mappingPointByInput.put(p.input, index++);
            }
        }

        if (!groupValues) {
            boolean inputEditable = !mapping.input.isDiscrete();
            data = new MappingPointValue[mapping.input.getMaxValue() - mapping.input.getMinValue() + 1][];
            columnNames = new String[]{mapping.input.getName(), mapping.output.getName()};
            for (int i = mapping.input.getMinValue(); i <= mapping.input.getMaxValue(); i++) {
                data[i - mapping.input.getMinValue()] = new MappingPointValue[]{new MappingPointValue(i, mapping.input),
                        new MappingPointValue(mapping.map(i), mapping.output)};
                if (mappingPointByInput.containsKey(i)) {
                    int controlPointIndex = mappingPointByInput.get(i);
                    data[i - mapping.input.getMinValue()][0].isEditable = inputEditable;
                    data[i - mapping.input.getMinValue()][0].mappingPointIndex = controlPointIndex;
                    data[i - mapping.input.getMinValue()][1].isEditable = true;
                    data[i - mapping.input.getMinValue()][1].mappingPointIndex = controlPointIndex;
                }
            }
        } else {
            columnNames = new String[]{"from " + mapping.input.getName(),
                    " to " + mapping.input.getName(),
                    mapping.output.getName()};
            List<Point2d> ranges = LayerMapping.calculateRanges(mapping);
            int ii = 0;
            data = new MappingPointValue[ranges.size()][];
            for (Point2d range : ranges) {
                int start = (int) range.x;
                int end = (int) range.y;
                data[ii++] = new MappingPointValue[]{new MappingPointValue(start, mapping.input),
                        new MappingPointValue(end, mapping.input),
                        new MappingPointValue(mapping.map(start), mapping.output)};

            }
        }

        this.tableModel = new DefaultTableModel(data, columnNames);
        this.tableModel.addTableModelListener(this.listener);
        numberTable.setModel(tableModel);
    }
    private boolean blockTableChanged;
    @Override
    protected void updateComponents() {
        groupValues = groupValuesCheckBox.isSelected();
        blockTableChanged = true;
        if (groupValues) {
            List<Point2d> ranges = LayerMapping.calculateRanges(mapping);
            if (numberTable.getModel().getRowCount() != ranges.size()) {
                initTableModel();   //rebuild
            }
            int ii = 0;
            for (Point2d range : ranges) {
                int start = (int) range.x;
                int end = (int) range.y;
                tableModel.setValueAt(new MappingPointValue(start, mapping.input), ii, 0);
                tableModel.setValueAt(new MappingPointValue(end, mapping.input), ii, 1);
                tableModel.setValueAt(new MappingPointValue(mapping.map(start), mapping.output), ii, 2);
            }
        } else {

            int range = mapping.input.getMaxValue() - mapping.input.getMinValue() + 1;
            if (numberTable.getModel().getRowCount() != range) {
                initTableModel();   //rebuild
            }
            HashMap<Integer, Integer> mappingPointByInput = new HashMap<>();
            {
                int i = 0;
                for (MappingPoint p : mapping.getMappingPoints()) {
                    mappingPointByInput.put(p.input, i++);
                }
            }
            for (int i = 0; i < numberTable.getRowCount(); i++) {
                MappingPointValue mpv = (MappingPointValue) numberTable.getModel().getValueAt(i, 0);
                int numeric = i+mapping.input.getMinValue();
                boolean editable = mappingPointByInput.containsKey(numeric);
                int controlPointiD = mappingPointByInput.getOrDefault(numeric,-1);
                MappingPointValue inputV = new MappingPointValue(mapping.input,numeric, editable, controlPointiD);
                numberTable.getModel().setValueAt(inputV, i, 0);
                if (groupValues) {
                    initTableModel();
                }
                MappingPointValue outputV = new MappingPointValue(mapping.output, mapping.map(inputV.numericValue),
                        editable, controlPointiD);
                numberTable.getModel().setValueAt(outputV, i, 1);
            }
        }
        blockTableChanged = false;
    }

    @Override
    protected void initComponents() {
        this.setLayout(new BorderLayout());
        Border padding = new EmptyBorder(20, 20, 20, 20); // 20px padding on all sides
        Border whiteBorder = new EmptyBorder(5, 5, 5, 5); // 5px white border
        setBorder(BorderFactory.createCompoundBorder(whiteBorder, padding));

        // Add a TableModelListener to get a callback when a cell is edited
        numberTable = new JTable() {
            @Override
            public boolean isCellEditable(int row, int column) {
                MappingPointValue value = (MappingPointValue) numberTable.getModel().getValueAt(row, column);
                //test if this value is a controlpoint
                if (!groupValues) return value.isEditable;
                return false; // All cells are non-editable
            }
        };

        Font font = new Font("Arial", Font.PLAIN, 24);
        numberTable.setFont(font);
        numberTable.setDefaultRenderer(Object.class, new MappingPointCellRenderer());
        numberTable.setDefaultEditor(Object.class, new MappingPointCellEditor());

        JScrollPane scrollPane = new JScrollPane(numberTable);
        this.add(scrollPane, BorderLayout.CENTER);
        JPanel buttons = new JPanel();
        groupValuesCheckBox = new JCheckBox("Group Values");
        groupValuesCheckBox.addActionListener(f -> {
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
                if (e.getType() == TableModelEvent.UPDATE && !blockTableChanged) {
                    int row = e.getFirstRow(); // Get the row of the edited cell
                    int column = e.getColumn(); // Get the column of the edited cell
                    Object newValue = tableModel.getValueAt(row, column); // Get the new value
                    SwingUtilities.invokeLater(() -> parseAndSetValue(newValue, row, column));
                }
            }
        };
        numberTable.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        // Add listener to scroll to the selected row
        numberTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = numberTable.getSelectedRow();
                if (selectedRow != -1) {
                    numberTable.scrollRectToVisible(numberTable.getCellRect(selectedRow, 0, true));
                }
            }
        });
        numberTable.getSelectionModel().addListSelectionListener(event -> {
            // Ignore the event if it's being adjusted (e.g., dragging selection)
            if (!event.getValueIsAdjusting()) {
                int selectedRow = numberTable.getSelectedRow();
                boolean[] selection = new boolean[mapping.input.getMaxValue() - mapping.input.getMinValue() + 1];
                for (Integer row : numberTable.getSelectedRows()) {
                    MappingPointValue mpv = (MappingPointValue) numberTable.getModel().getValueAt(row, 0);
                    selection[mpv.numericValue - mapping.input.getMinValue()] = true;
                }
                onSelect.accept(selection);
            }
        });

        initTableModel();
    }

    protected boolean parseAndSetValue(Object newValue, int row, int column) {
        assert newValue instanceof MappingPointValue;
        MappingPointValue mpv = (MappingPointValue) newValue;
        MappingPoint[] points = mapping.getMappingPoints().clone();
        //FIXME allow editing
        if (column == 0) {
            points[mpv.mappingPointIndex] = new MappingPoint(mpv.numericValue, points[mpv.mappingPointIndex].output);
        } else {
            points[mpv.mappingPointIndex] = new MappingPoint(points[mpv.mappingPointIndex].input, mpv.numericValue);
        }
        updateMapping(mapping.withNewPoints(points));
        return true;
    }

    @Override
    public void setOnSelect(Consumer<boolean[]> onSelect) {
        this.onSelect = onSelect;
    }

    @Override
    public void setSelectedInputs(boolean[] selectedPointIdx) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            MappingPointValue value = (MappingPointValue) tableModel.getValueAt(i, 0);
            if (selectedPointIdx[value.numericValue - mapping.input.getMinValue()]) {
                numberTable.setRowSelectionInterval(i, i);
                return;
            }
        }
    }
}


