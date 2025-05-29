package org.ironsight.wpplugin.macromachine.Gui;

import org.ironsight.wpplugin.macromachine.operations.LayerMapping;
import org.ironsight.wpplugin.macromachine.operations.UniqueList;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IDisplayUnit;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IMappingValue;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IPositionValueGetter;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IPositionValueSetter;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

class VirtualTableModel extends AbstractTableModel {
    private final Map<Integer, Object[]> cache = new HashMap<>();
    private int rowCount = 1000000; // Example large row count
    private ArrayList<LayerMapping> mappings = new ArrayList<>();
    private int[] offsets = new int[0];
    private int[] offsetsModule = new int[0];
    private String[] columnNames = new String[0];
    private IMappingValue[] columnMappers = new IMappingValue[0];

    protected void updateComponents() {

        UniqueList<IPositionValueGetter> inputs = new UniqueList<>();
        UniqueList<IPositionValueSetter> outputs = new UniqueList<>();
        for (LayerMapping action : mappings) {
            inputs.add(action.input);
            outputs.add(action.output);
        }

        ArrayList<IMappingValue> columns = new ArrayList<>();
        inputs.getList().stream().filter(i -> !i.isVirtual()).forEach(columns::add);
        //columns.addAll(outputs.getList());


        columnNames = new String[columns.size()];
        columnNames = columns.stream().map(IDisplayUnit::getName).toArray(String[]::new);
        columnMappers = columns.toArray(new IMappingValue[0]);

        offsets = new int[columnMappers.length];
        offsetsModule = new int[columnMappers.length];
        Arrays.fill(offsets, 1);
        rowCount = 1;
        offsets = columns.stream().mapToInt(io -> {
            int old = rowCount;
            rowCount *= IMappingValue.range(io);
            return old;
        }).toArray();
        offsetsModule = columns.stream().mapToInt(IMappingValue::range).toArray();

        rowCount = Math.min(rowCount, 1000000);
        fetchData(0, 60);
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
        // Fetch data for the visible rows
        cache.clear();
        for (int i = firstRow; i <= lastRow; i++) {
            cache.put(i, fetchRowData(i));
        }
    }

    private Object[] fetchRowData(int rowIndex) {
        return IntStream.range(0, columnMappers.length)
                .mapToObj(i -> new MappingPointValue(rowIndex / offsets[i] % offsetsModule[i], columnMappers[i]))
                .toArray(Object[]::new);
    }
}
