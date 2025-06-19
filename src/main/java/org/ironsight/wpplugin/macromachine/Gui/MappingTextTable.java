package org.ironsight.wpplugin.macromachine.Gui;

import org.ironsight.wpplugin.macromachine.operations.MappingAction;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

public class MappingTextTable extends LayerMappingPanel implements IMappingPointSelector {
    MappingActionValueTableModel tableModel;
    TableModelListener listener;
    boolean[] inputSelection = new boolean[0];
    boolean blockSendingSelection = false;
    private Consumer<boolean[]> onSelect = f -> {
    };
    private JTable numberTable;
    private boolean groupValues = true;
    private JCheckBox groupValuesCheckBox;
    private boolean blockTableChanged;
    private TableRowSorter<MappingActionValueTableModel> sorter;

    @Override
    protected void updateComponents() {
        TableRowSorter<?> sorter = (TableRowSorter<?>) numberTable.getRowSorter();
        List<? extends RowSorter.SortKey> sortKeys = sorter.getSortKeys();

        groupValues = groupValuesCheckBox.isSelected();
        blockTableChanged = true;
        tableModel.rebuildDataWithAction(this.mapping);

        // force stop current edit, because the amount of rows changes, and otherwise will cause array index out
        // of bounds
        if (numberTable.isEditing()) {
            TableCellEditor editor = numberTable.getCellEditor();
            if (editor != null) {
                editor.stopCellEditing(); // or editor.cancelCellEditing();
            }
        }
        blockTableChanged = false;

        sorter.setSortKeys(sortKeys);
    }

    private void setRowFilter(boolean groupValues) {
        if (!groupValues) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(new RowFilter<MappingActionValueTableModel, Integer>() {
                @Override
                public boolean include(Entry<? extends MappingActionValueTableModel, ? extends Integer> entry) {
                    return tableModel.isMappingPoint(entry.getIdentifier());
                }
            });
        }
    }

    @Override
    protected void initComponents() {
        groupValues = true;

        this.setLayout(new BorderLayout());
        Border padding = new EmptyBorder(20, 20, 20, 20); // 20px padding on all sides
        Border whiteBorder = new EmptyBorder(5, 5, 5, 5); // 5px white border
        setBorder(BorderFactory.createCompoundBorder(whiteBorder, padding));

        // Add a TableModelListener to get a callback when a cell is edited
        numberTable = new JTable() {
            @Override
            public TableCellEditor getCellEditor(int row, int column) {
                if (!numberTable.isRowSelected(row))    //user clicks into unselected column: clear selection and
                    // only selected the clicked one
                    numberTable.clearSelection();
                numberTable.addRowSelectionInterval(row, row);  //otherwise, just
                return super.getCellEditor(row, column);
            }
        };
        this.tableModel = new MappingActionValueTableModel();
        tableModel.rebuildDataWithAction(mapping);
        numberTable.setModel(tableModel);
        sorter = new TableRowSorter<>(tableModel);
        sorter.setSortsOnUpdates(true);
        numberTable.setRowSorter(sorter);
        setRowFilter(groupValues);

        Font font = new Font("Arial", Font.PLAIN, 24);
        numberTable.setFont(font);
        MappingPointCellRenderer cellRenderer = new MappingPointCellRenderer();
        numberTable.setDefaultRenderer(MappingPointValue.class, cellRenderer);
        numberTable.setRowHeight(cellRenderer.getPreferredHeight());
        numberTable.setDefaultEditor(Object.class, new MappingPointCellEditor());
        numberTable.setSelectionModel(new CustomListSelectionModel());
        JScrollPane scrollPane = new JScrollPane(numberTable);
        this.add(scrollPane, BorderLayout.CENTER);
        JPanel buttons = new JPanel();
        groupValuesCheckBox = new JCheckBox("Only Control Points");
        groupValuesCheckBox.setSelected(groupValues);
        groupValuesCheckBox.addActionListener(f -> {
            this.groupValues = groupValuesCheckBox.isSelected();
            setRowFilter(groupValues);
        });
        buttons.add(groupValuesCheckBox);
        this.add(buttons, BorderLayout.SOUTH);
        listener = e -> {
            // Check if the event is due to a cell update
            if (e.getType() == TableModelEvent.UPDATE && !blockTableChanged) {
                updateMapping(tableModel.getAction());
            }
        };
        tableModel.addTableModelListener(listener);

        numberTable.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        // Add listener to scroll to the selected row
        numberTable.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            int selectedRow = numberTable.getSelectedRow();
            if (selectedRow != -1) {
                numberTable.scrollRectToVisible(numberTable.getCellRect(selectedRow, 0, true));
            }

            boolean[] selection = new boolean[mapping.input.getMaxValue() - mapping.input.getMinValue() + 1];
            for (Integer row : numberTable.getSelectedRows()) {
                MappingPointValue mpv = (MappingPointValue) numberTable.getModel().getValueAt(row, 0);
                // hightlight single selected input
                selection[mpv.numericValue - mapping.input.getMinValue()] = true;
            }
            this.inputSelection = selection;
            if (!blockSendingSelection)
                onSelect.accept(selection);
        });
    }


    @Override
    public void setOnSelect(Consumer<boolean[]> onSelect) {
        this.onSelect = onSelect;
    }

    @Override
    public void setSelectedInputs(boolean[] selectedPointIdx) {
        if (Arrays.equals(this.inputSelection, selectedPointIdx)) {
            return; //nothing to update here
        }
        blockSendingSelection = true;
        this.inputSelection = selectedPointIdx;

        numberTable.clearSelection();

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            MappingPointValue value = (MappingPointValue) tableModel.getValueAt(i, 0);
            if (selectedPointIdx[value.numericValue - mapping.input.getMinValue()]) {
                numberTable.addRowSelectionInterval(i, i);
            }
        }


        repaint();
        blockSendingSelection = false;
    }

    // Custom selection model
    class CustomListSelectionModel extends DefaultListSelectionModel {
        @Override
        public void setSelectionInterval(int index0, int index1) {
            if (numberTable.isEditing()) return;
            super.setSelectionInterval(index0, index1);
        }
    }

}


