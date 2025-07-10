package org.ironsight.wpplugin.macromachine.Gui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class MappingTextTable extends JPanel {
    private final MappingActionValueTableModel tableModel;
    private final JTable numberTable;
    private boolean isFilterForMappingPoints = true;
    private JCheckBox groupValuesCheckBox;
    private TableRowSorter<MappingActionValueTableModel> sorter;
    private int[] selectedRow = new int[0];

    public MappingTextTable(MappingActionValueTableModel model, ListSelectionModel selectionModel) {
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

        numberTable.setModel(model);
        numberTable.setSelectionModel(selectionModel);
        this.tableModel = model;
        initComponents();
        addListeners(model, selectionModel);
    }

    protected void updateComponents() {
        TableRowSorter<?> sorter = (TableRowSorter<?>) numberTable.getRowSorter();
        List<? extends RowSorter.SortKey> sortKeys = sorter.getSortKeys();
// force stop current edit, because the amount of rows changes, and otherwise will cause array index out
        // of bounds
        if (numberTable.isEditing()) {
            TableCellEditor editor = numberTable.getCellEditor();
            if (editor != null) {
                editor.stopCellEditing(); // or editor.cancelCellEditing();
            }
        }

        sorter.setSortKeys(sortKeys);
        sorter.sort();

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

    private void addListeners(MappingActionValueTableModel model, ListSelectionModel selectionModel) {
        // Add listener to scroll to the selected row
        selectionModel.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            int selectedRow = numberTable.getSelectedRow();
            if (selectedRow != -1) {
                numberTable.scrollRectToVisible(numberTable.getCellRect(selectedRow, 0, true));
            }
        });

        model.addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE) {
                updateComponents();
            }
        });


// Save selected row table
        numberTable.getSelectionModel().addListSelectionListener(e -> {
            selectedRow = numberTable.getSelectedRows();
        });

// Restore selected raw table after a value was changed in the table
        model.addTableModelListener(e -> SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    for (int selectedRow: selectedRow) {
                        numberTable.addRowSelectionInterval(selectedRow, selectedRow);
                    }
                } catch (IllegalArgumentException ignored) {
                    // view row amount might be MUCH lower than expected. just ignore it
                }
            }
        }));
    }

    protected void initComponents() {
        isFilterForMappingPoints = true;

        this.setLayout(new BorderLayout());
        Border padding = new EmptyBorder(20, 20, 20, 20); // 20px padding on all sides
        Border whiteBorder = new EmptyBorder(5, 5, 5, 5); // 5px white border
        setBorder(BorderFactory.createCompoundBorder(whiteBorder, padding));

        // Add a TableModelListener to get a callback when a cell is edited
        numberTable.setModel(tableModel);
        sorter = new TableRowSorter<>(tableModel);
        sorter.setSortsOnUpdates(true);
        numberTable.setRowSorter(sorter);
        setRowFilter(isFilterForMappingPoints);

        Font font = new Font("Arial", Font.PLAIN, 24);
        numberTable.setFont(font);
        MappingPointCellRenderer cellRenderer = new MappingPointCellRenderer();
        numberTable.setDefaultRenderer(MappingPointValue.class, cellRenderer);
        numberTable.setRowHeight(cellRenderer.getPreferredHeight());
        numberTable.setDefaultEditor(Object.class, new MappingPointCellEditor());
        JScrollPane scrollPane = new JScrollPane(numberTable);
        this.add(scrollPane, BorderLayout.CENTER);
        JPanel buttons = new JPanel();
        groupValuesCheckBox = new JCheckBox("Only Control Points");
        groupValuesCheckBox.setSelected(isFilterForMappingPoints);
        groupValuesCheckBox.addActionListener(f -> {
            this.isFilterForMappingPoints = groupValuesCheckBox.isSelected();
            setRowFilter(isFilterForMappingPoints);
        });
        buttons.add(groupValuesCheckBox);

        {
            JButton addMappingPointButton = new JButton("add control point");
            addMappingPointButton.addActionListener(l -> {
                if (numberTable.getSelectedRow() != -1)
                    tableModel.insertMappingPointNear(numberTable.convertRowIndexToModel(numberTable.getSelectedRow()));
                else if (numberTable.getRowCount() != 0) {
                    tableModel.insertMappingPointNear(0);
                }
            });
            buttons.add(addMappingPointButton);
        }
        {
            JButton button = new JButton("remove control point");
            button.addActionListener(l -> {
                if (numberTable.getSelectedRow() != -1) {
                    int[] selectedRows = numberTable.getSelectedRows();
                    for (int i = 0; i < selectedRows.length; i++) {
                        selectedRows[i] = numberTable.convertRowIndexToModel(selectedRows[i]);
                    }
                    tableModel.deleteMappingPointAt(selectedRows);
                }

            });
            buttons.add(button);
        }

        this.add(buttons, BorderLayout.SOUTH);


    }
}


