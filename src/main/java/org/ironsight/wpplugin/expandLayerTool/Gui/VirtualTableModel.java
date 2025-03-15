package org.ironsight.wpplugin.expandLayerTool.Gui;

import org.ironsight.wpplugin.expandLayerTool.operations.LayerMapping;
import org.ironsight.wpplugin.expandLayerTool.operations.UniqueList;
import org.ironsight.wpplugin.expandLayerTool.operations.ValueProviders.IDisplayUnit;
import org.ironsight.wpplugin.expandLayerTool.operations.ValueProviders.IPositionValueGetter;
import org.ironsight.wpplugin.expandLayerTool.operations.ValueProviders.IPositionValueSetter;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import java.util.*;

class VirtualTableModel extends AbstractTableModel {
    private final Map<Integer, Object[]> cache = new HashMap<>();
    private int rowCount = 1000000; // Example large row count
    private ArrayList<LayerMapping> mappings = new ArrayList<>();

    private String[] columnNames = new String[]{"A", "B", "C", "D", "E", "F"};

    protected void updateComponents() {
        System.out.println("virtual table modle update structure");

        UniqueList<IPositionValueGetter> inputs = new UniqueList<>();
        UniqueList<IPositionValueSetter> outputs = new UniqueList<>();
        for (LayerMapping action : mappings) {
            inputs.add(action.input);
            outputs.add(action.output);
        }

        DefaultTableModel model = new DefaultTableModel();
        model.setColumnCount(inputs.getList().size() + outputs.getList().size());

        columnNames = new String[inputs.getList().size() + outputs.getList().size()];
        int i = 0;
        String[] inputNames = inputs.getList().stream().map(IDisplayUnit::getName).toArray(String[]::new);
        String[] outputNames = outputs.getList().stream().map(IDisplayUnit::getName).toArray(String[]::new);
        for (String inputName : inputNames)
            columnNames[i++] = inputName;
        for (String outputName : outputNames)
            columnNames[i++] = outputName;

        /*rowCount = 1;
        for (IPositionValueGetter input : inputs.getList()) {
            rowCount = rowCount * IMappingValue.range(input);
        }*/
        fetchData(0,60);
    }

    public void setMappings(ArrayList<LayerMapping> actions) {
        this.mappings = actions;
        updateComponents();
        fireTableStructureChanged();
    }

    @Override
    public int getRowCount() {
        return rowCount;
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    @Override
    public int getColumnCount() {
        return columnNames.length; // Example column count
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Object[] rowData = cache.get(rowIndex);
        if (rowData == null) {
            rowData = fetchRowData(rowIndex);
            cache.put(rowIndex, rowData);
        }
        return rowData[columnIndex];
    }

    public void fetchData(int firstRow, int lastRow) {
        System.out.println("Fetching data from " + firstRow + " to " + lastRow);
        // Fetch data for the visible rows
        cache.clear();
        for (int i = firstRow; i <= lastRow; i++) {
            cache.put(i, fetchRowData(i));
        }
    }

    private Object[] fetchRowData(int rowIndex) {
        // Simulate fetching data from a data source
        String[] row = new String[columnNames.length];
        Arrays.fill(row, "" +rowIndex);
        return row;
    }
}
