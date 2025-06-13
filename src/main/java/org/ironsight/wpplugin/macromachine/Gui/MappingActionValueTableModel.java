package org.ironsight.wpplugin.macromachine.Gui;

import org.ironsight.wpplugin.macromachine.operations.MappingAction;
import org.ironsight.wpplugin.macromachine.operations.MappingPoint;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IMappingValue;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.util.Arrays;
import java.util.LinkedList;

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
        boolean inputOutputChanged = this.action == null ||
                !(this.action.input.equals(action.input) && this.action.output.equals(action.getOutput()));
        this.action = action;
        rebuildData();
        if (inputOutputChanged)
            for (TableModelListener l : listeners) {
                l.tableChanged(new TableModelEvent(this, TableModelEvent.HEADER_ROW));
            }
    }

    public MappingAction getAction() {
        return this.action;
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
            inputs[rowIndex] = new MappingPointValue(inputValue, action.getInput());
            output[rowIndex] = new MappingPointValue(action.map(inputValue), action.output);
            rowIndex++;
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
