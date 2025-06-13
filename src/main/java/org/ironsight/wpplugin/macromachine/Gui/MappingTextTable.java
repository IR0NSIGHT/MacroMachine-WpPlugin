package org.ironsight.wpplugin.macromachine.Gui;

import org.ironsight.wpplugin.macromachine.operations.MappingAction;
import org.ironsight.wpplugin.macromachine.operations.MappingPoint;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IMappingValue;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;
import java.awt.*;
import java.util.*;
import java.util.function.Consumer;

public class MappingTextTable extends LayerMappingPanel implements IMappingPointSelector {
    MappingActionValueTableModel tableModel;
    TableModelListener listener;
    boolean[] inputSelection = new boolean[0];
    boolean blockSendingSelection = false;
    private Consumer<boolean[]> onSelect = f -> {
    };
    private JTable numberTable;
    private boolean groupValues = false;
    private JCheckBox groupValuesCheckBox;
    private boolean blockTableChanged;
    private Object beforeChange;
    private int eventRow;
    private int eventColumn;

    @Override
    protected void updateComponents() {
        groupValues = groupValuesCheckBox.isSelected();
        blockTableChanged = true;
        tableModel.rebuildDataWithAction(this.mapping);
        tableModel.setOnlyControlPointMode(groupValues);
        blockTableChanged = false;
        numberTable.revalidate();
        numberTable.repaint();
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
        tableModel.setOnlyControlPointMode(groupValues);
        numberTable.setModel(tableModel);


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
        groupValuesCheckBox.addActionListener(f -> {
            if (groupValues != groupValuesCheckBox.isSelected()) {
                groupValues = groupValuesCheckBox.isSelected();
                updateComponents();
            }
        });
        buttons.add(groupValuesCheckBox);
        this.add(buttons, BorderLayout.SOUTH);
        listener = e -> {
            // Check if the event is due to a cell update
            if (e.getType() == TableModelEvent.UPDATE && !blockTableChanged) {
                updateMapping(tableModel.action);
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

    class MappingActionValueTableModel implements TableModel {
        private static final int INPUT_COLUMN_IDX = 0;
        private static final int OUTPUT_COLUMN_IDX = 1;
        LinkedList<TableModelListener> listeners = new LinkedList<>();
        private MappingAction action;
        private boolean onlyControlPoints;
        private MappingPointValue[] inputs, output;
        private boolean[] isMappingPoint;
        private int[] rowToMappingPointIdx;
        private int[] mappingPointToRowIdx;

        public void rebuildDataWithAction(MappingAction action) {
            if (action == null)
                return;
            if (action.equals(this.action))
                return;
            this.action = action;
            rebuildData();
        }

        public void setOnlyControlPointMode(boolean flag) {
            this.onlyControlPoints = flag;
        }

        private void rebuildData() {
            int rowAmount = IMappingValue.range(action.getInput());
            inputs = new MappingPointValue[rowAmount];
            output = new MappingPointValue[rowAmount];
            isMappingPoint = new boolean[rowAmount];
            rowToMappingPointIdx = new int[rowAmount];

            // construct map inputValue -> mappingPointIndex
            mappingPointToRowIdx = new int[action.getMappingPoints().length];
            Arrays.fill(rowToMappingPointIdx, -1);
            for (int mpIndex = 0; mpIndex < action.getMappingPoints().length; mpIndex++) {
                MappingPoint mp = action.getMappingPoints()[mpIndex];
                rowToMappingPointIdx[mp.input - action.getInput().getMinValue()] = mpIndex;
                isMappingPoint[mp.input - action.getInput().getMinValue()] = true;
                mappingPointToRowIdx[mpIndex] = mp.input - action.getInput().getMinValue();
            }

            int rowIndex = 0;
            for (int inputValue = action.getInput().getMinValue();
                 inputValue <= action.getInput().getMaxValue(); inputValue++) {
                inputs[rowIndex] = new MappingPointValue(
                        action.getInput(),
                        inputValue,
                        isMappingPoint[inputValue],
                        rowToMappingPointIdx[inputValue]
                );
                output[rowIndex] = new MappingPointValue(
                        action.output,
                        action.map(inputValue),
                        isMappingPoint[inputValue],
                        rowToMappingPointIdx[inputValue]
                );
                rowIndex++;
            }
            for (TableModelListener l: listeners) {
                l.tableChanged(new TableModelEvent(this, TableModelEvent.HEADER_ROW));
            }
        }

        @Override
        public int getRowCount() {
            if (action == null)
                return 0;
            return onlyControlPoints ? action.getMappingPoints().length : inputs.length;
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int columnIndex) {
            if (this.action == null)
                return "NULL ACTION";
            if (columnIndex == INPUT_COLUMN_IDX)
                return action.getInput().getName();
            else if (columnIndex == OUTPUT_COLUMN_IDX)
                return action.output.getName();
            else
                throw new RuntimeException();
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return MappingPointValue.class;
        }

        private boolean isControlPoint(int rowIndex) {
            return onlyControlPoints || this.isMappingPoint[rowIndex];
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return isControlPoint(rowIndex);
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            int numericInput = onlyControlPoints ? mappingPointToRowIdx[rowIndex] : rowIndex;
            assert numericInput != -1;
            if (columnIndex == INPUT_COLUMN_IDX)
                return inputs[numericInput];
            if (columnIndex == OUTPUT_COLUMN_IDX)
                return output[numericInput];
            assert false;
            return null;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (!isCellEditable(rowIndex, columnIndex))
                return;
            int numericInput = onlyControlPoints ? mappingPointToRowIdx[rowIndex] : rowIndex;
            int mappingPointIdx = onlyControlPoints ? rowIndex : rowToMappingPointIdx[rowIndex];
            MappingPoint p = action.getMappingPoints()[mappingPointIdx];
            MappingPoint[] newPoints = action.getMappingPoints();
            assert numericInput == p.input;
            if (columnIndex == OUTPUT_COLUMN_IDX) {
                newPoints[mappingPointIdx] = new MappingPoint(p.input, ((MappingPointValue) aValue).numericValue);
            } else if (columnIndex == INPUT_COLUMN_IDX) {
                newPoints[mappingPointIdx] = new MappingPoint(((MappingPointValue) aValue).numericValue, p.output);
            } else {
                throw new RuntimeException("invalid column index");
            }

            MappingAction newAction = action.withNewPoints(newPoints);
            if (newAction.equals(this.action))
                return;
            rebuildDataWithAction(action.withNewPoints(newPoints));
            fireOnTableChanged(rowIndex, columnIndex);
        }

        @Override
        public void addTableModelListener(TableModelListener l) {
            listeners.add(l);
        }

        private void fireOnTableChanged(int row, int column) {
            for (TableModelListener l : listeners)
                l.tableChanged(new TableModelEvent(this, row, row, column));
        }

        @Override
        public void removeTableModelListener(TableModelListener l) {
            listeners.remove(l);
        }
    }

}


