package org.ironsight.wpplugin.macromachine.Gui;

import org.ironsight.wpplugin.macromachine.operations.MappingAction;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.EditableIO;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IPositionValueGetter;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IPositionValueSetter;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.PerlinNoiseIO;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.Arrays;
import java.util.function.Consumer;

public class InputOutputEditor extends LayerMappingPanel {
    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        InputOutputEditor editor = new InputOutputEditor(f -> { System.out.println("update mapping to:" + f); });
        MappingAction lm = MappingAction.getNewEmptyAction().withInput(new PerlinNoiseIO(10,20,123456789, 3));
        editor.setMapping(lm);
        frame.add(new JLabel("HELLO WORLD"), BorderLayout.NORTH);
        frame.add(editor, BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);
    }

    private EditableIO io;
    private Consumer<MappingAction> onChanged;

    public void setIsInput(boolean input) {
        isInput = input;
        updateComponents();
    }

    private boolean isInput = true;
    private KeyValueTableModel tableModel;
    public InputOutputEditor(Consumer<MappingAction> onChanged) {
        this.onChanged = onChanged;
    }

    private void onValueChanged(int[] newValues) {
        if (Arrays.equals(newValues, io.getEditableValues()))
            return;
        if (mapping == null) {
            System.err.println("mapping was null when trying to set values" + newValues);
            assert false;
            return;
        }
        MappingAction updated = isInput ? mapping.withInput((IPositionValueGetter) io.instantiateWithValues(newValues)) :
                mapping.withOutput((IPositionValueSetter) io.instantiateWithValues(newValues));
        onChanged.accept(updated);
        setMapping(updated);
    }

    @Override
    protected void updateComponents() {
        if (mapping == null)
            return;
        System.out.println("update IO editor with values:" + (isInput ? mapping.input : mapping.output));
        if ((isInput && !(mapping.input instanceof EditableIO)) || (!isInput && !(mapping.output instanceof EditableIO))) {
            tableModel.setData(new int[0],new String[0], new String[0]);
        } else {
            io = isInput ? (EditableIO) mapping.input : (EditableIO) mapping.output;
            tableModel.setData(
                    io.getEditableValues(), io.getValueNames(), io.getValueTooltips());
        }
        this.invalidate();
        this.repaint();
    }

    @Override
    protected void initComponents() {
        tableModel =
                new KeyValueTableModel();
        JTable table = new JTable(tableModel) {
            @Override
            public String getToolTipText(java.awt.event.MouseEvent event) {
                Point p = event.getPoint();
                int rowIndex = rowAtPoint(p);
                if (rowIndex >= 0) {
                    return tableModel.getTooltip(rowIndex);
                }
                return super.getToolTipText(event);
            }

            @Override
            public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend) {
                // Only allow selection on the second column
                if (columnIndex == 1) {
                    super.changeSelection(rowIndex, columnIndex, toggle, extend);
                } else {
                    // Optionally, you can select the same row in the second column
                    super.changeSelection(rowIndex, 1, toggle, extend);
                }
            }
        };

        // Add a TableModelListener to detect changes
        tableModel.addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE) {
                if (tableModel.getRowCount() == 0)
                    return;
                int row = e.getFirstRow();
                String key = (String) tableModel.getValueAt(row, 0);
                Integer value = (Integer) tableModel.getValueAt(row, 1);
                System.out.println("Value changed for key: " + key + ", new value: " + value);
                // Call your callback function here
                int[] newValues = new int[tableModel.getRowCount()];
                for (int i = 0; i < newValues.length; i++) {
                    newValues[i] = (int) tableModel.getValueAt(i, 1);
                }
                onValueChanged(newValues);
            }
        });

        // Add the table to a scroll pane
        JScrollPane scrollPane = new JScrollPane(table);
        this.setLayout(new BorderLayout());
        this.add(scrollPane, BorderLayout.CENTER);
    }

    // Custom table model
    static class KeyValueTableModel extends AbstractTableModel {
        private final String[] columnNames = {"Key", "Value"};
        String[] toolTips = new String[0];
        private Object[][] data = new Object[0][];

        public KeyValueTableModel() {

        }

        public String getTooltip(int row) {
            return toolTips[row];
        }

        public void setData(int[] values, String[] keys, String[] toolTips) {
            this.toolTips = toolTips;
            this.data = new Object[values.length][2];
            for (int i = 0; i < values.length; i++) {
                this.data[i][0] = keys[i];
                this.data[i][1] = values[i];
            }
            fireTableDataChanged();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public int getRowCount() {
            return data.length;
        }

        @Override
        public String getColumnName(int col) {
            return columnNames[col];
        }

        @Override
        public Object getValueAt(int row, int col) {
            return data[row][col];
        }

        @Override
        public Class<?> getColumnClass(int col) {
            return getValueAt(0, col).getClass();
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return col == 1; // Only the second column (Value) is editable
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            if (col == 1) {
                data[row][col] = value;
                fireTableCellUpdated(row, col);
            }
        }
    }
}
