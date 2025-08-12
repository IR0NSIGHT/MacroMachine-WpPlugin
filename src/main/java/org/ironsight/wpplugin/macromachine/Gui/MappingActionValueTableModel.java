package org.ironsight.wpplugin.macromachine.Gui;

import org.ironsight.wpplugin.macromachine.operations.MappingAction;
import org.ironsight.wpplugin.macromachine.operations.MappingPoint;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IMappingValue;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

class MappingActionValueTableModel implements TableModel {
    private static final int INPUT_COLUMN_IDX = 0;
    private static final int OUTPUT_COLUMN_IDX = 1;
    private final LinkedList<TableModelListener> listeners = new LinkedList<>();
    private MappingAction action;

    private MappingPointValue[] inputs = new MappingPointValue[0], output = new MappingPointValue[0];
    private boolean[] isMappingPoint = new boolean[0];
    private int[] rowToMappingPointIdx = new int[0];

    public boolean isMappingPoint(int rowIdx) {
        return isMappingPoint[rowIdx];
    }
    public MappingAction constructMapping() {
        ArrayList<MappingPoint> mappingPoints = new ArrayList<>();
          for (int rowIdx = 0; rowIdx < inputs.length; rowIdx++) {
              if (isMappingPoint(rowIdx))
                  mappingPoints.add(new MappingPoint(inputs[rowIdx].numericValue,output[rowIdx].numericValue));
          }
        if (this.action == null)
            return null;
        return action.withNewPoints(mappingPoints.toArray(new MappingPoint[0]));
    }
    public void rebuildModelFromAction(MappingAction action) {
        if (action == null)
            return;
        if (action.equals(this.action))
            return;
        boolean headerRowChanged = this.action == null ||
                !(this.action.input.equals(action.input) && this.action.output.equals(action.getOutput()));
        this.action = action;
        int oldLength = inputs.length;
        int newLength = IMappingValue.range(action.getInput());
        if (newLength < oldLength)
            fireEvent(new TableModelEvent(this, newLength, oldLength - 1, TableModelEvent.DELETE));

        rebuildData();

        if (headerRowChanged)
            fireEvent(new TableModelEvent(this, TableModelEvent.HEADER_ROW));

        if (oldLength != 0 && newLength != 0)
            fireEvent(new TableModelEvent(this, 0, Math.min(oldLength - 1, newLength - 1), TableModelEvent.UPDATE));
        if (oldLength < newLength)
            fireEvent(new TableModelEvent(this, oldLength, newLength - 1, TableModelEvent.INSERT));
    }


    public void setIsMappingPoint(int[] rows, boolean isMappingPoint) {
        if (rows.length == 0)
            return;
        for (int row : rows) {
            this.isMappingPoint[row] = isMappingPoint;
        }
        this.rowToMappingPointIdx = reconstructRowToMappingPointIdx(this.isMappingPoint);
        fireEvent(new TableModelEvent(this, rows[0],rows[rows.length-1], TableModelEvent.UPDATE));
    }

    public void insertMappingPointNear(int rowIndex) {
        for (int i = rowIndex; i < inputs.length; i++) {
            int mappingPointIndex = rowToMappingPointIdx[i];
            if (mappingPointIndex != -1)
                continue; // already a mapping point, attempt next one
            MappingPoint[] mps = Arrays.copyOf(action.getMappingPoints(),action.getMappingPoints().length+1);
            //insert in back
            mps[mps.length-1] = new MappingPoint(inputs[i].numericValue, action.map(inputs[i].numericValue));
            rebuildModelFromAction(this.getAction().withNewPoints(mps));
            break;
        }
    }

    public void deleteMappingPointAt(int[] rowIndex) {
        ArrayList<MappingPoint> mps = new ArrayList<>(Arrays.asList(action.getMappingPoints()));

        for (int i = rowIndex.length -1 ; i >= 0 ; i--) {
            int row = rowIndex[i];
            int mappingPointIndex = rowToMappingPointIdx[row];
            if (mappingPointIndex == -1)
                continue; // already a mapping point, attempt next one
            mps.remove(mappingPointIndex);
        }
        rebuildModelFromAction(this.getAction().withNewPoints(mps.toArray(new MappingPoint[0])));
    }

    public void fireEvent(TableModelEvent event) {
        for (TableModelListener l : listeners) {
            try {
                l.tableChanged(event);
            } catch (ArrayIndexOutOfBoundsException ignored) {
                ; //idk java swing sometimes doesnt like row converstion index to view. dont care
            }
        }
    }

    private MappingAction getAction() {
        return this.action;
    }

    private int[] reconstructRowToMappingPointIdx(boolean[] isMappingPoint) {
        int [] rowToMappingPointIdx = new int[isMappingPoint.length];

        // construct map inputValue -> mappingPointIndex
        Arrays.fill(rowToMappingPointIdx, -1);
        int mpIndex = 0;
        for (int row = 0; row < isMappingPoint.length; row++) {
            if (!isMappingPoint[row])
                continue;
            rowToMappingPointIdx[row] = mpIndex;
            mpIndex++;
        }
        return rowToMappingPointIdx;
    }

    /**
     * rebuild internal arrays from this.action
     */
    private void rebuildData() {
        int rowAmount = IMappingValue.range(action.getInput());
        inputs = new MappingPointValue[rowAmount];
        output = new MappingPointValue[rowAmount];
        isMappingPoint = new boolean[rowAmount];
        rowToMappingPointIdx = new int[rowAmount];

        // construct map inputValue -> mappingPointIndex
        Arrays.fill(rowToMappingPointIdx, -1);
        for (int mpIndex = 0; mpIndex < action.getMappingPoints().length; mpIndex++) {
            MappingPoint mp = action.getMappingPoints()[mpIndex];
            rowToMappingPointIdx[mp.input - action.getInput().getMinValue()] = mpIndex;
            isMappingPoint[mp.input - action.getInput().getMinValue()] = true;
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
        return inputs.length;
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
        return this.isMappingPoint[rowIndex];
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        if (columnIndex == INPUT_COLUMN_IDX && action.getInput().isDiscrete())
            return false; // discrete inputs already have 1 mapping point per value, there is no point in changing it.
        return isControlPoint(rowIndex);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (columnIndex == INPUT_COLUMN_IDX)
            return inputs[rowIndex];
        if (columnIndex == OUTPUT_COLUMN_IDX)
            return output[rowIndex];
        assert false;
        return null;
    }

    public void setValuesAt(MappingPointValue aValue, int[] rowIndices, int columnIndex) {
        if (rowIndices.length == 0)
            return;
        for (int rowIndex : rowIndices) {
            if (!isCellEditable(rowIndex, columnIndex))
                continue;
            assert aValue instanceof MappingPointValue;
            if (columnIndex == INPUT_COLUMN_IDX) { // input changes, mapping point is moved
                int inputNumeric =((MappingPointValue) aValue).numericValue;
                int inputTargetRow = inputNumeric - action.getInput().getMinValue();
                inputs[inputTargetRow] = (MappingPointValue) aValue;
                // old value at rowIndex remains untouched, but is not a mapping point anymore.
                isMappingPoint[rowIndex] = false;
                isMappingPoint[inputTargetRow] = true;
                //switch outputs
                MappingPointValue oldValue = output[rowIndex];
                output[rowIndex] = output[inputTargetRow];
                output[inputTargetRow] = oldValue;
            }
            else
                output[rowIndex] = (MappingPointValue)aValue;
        }
        rebuildModelFromAction(constructMapping());
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (!isCellEditable(rowIndex, columnIndex))
            return;
        assert aValue instanceof MappingPointValue;
        if (columnIndex == INPUT_COLUMN_IDX) { // input changes, mapping point is moved
            int inputNumeric =((MappingPointValue) aValue).numericValue;
            int inputTargetRow = inputNumeric - action.getInput().getMinValue();
            inputs[inputTargetRow] = (MappingPointValue) aValue;
            // old value at rowIndex remains untouched, but is not a mapping point anymore.
            isMappingPoint[rowIndex] = false;
            isMappingPoint[inputTargetRow] = true;
        }
        else
            output[rowIndex] = (MappingPointValue)aValue;
        fireEvent(new TableModelEvent(this, rowIndex, rowIndex, TableModelEvent.UPDATE));
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
