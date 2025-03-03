package org.ironsight.wpplugin.expandLayerTool.Gui;

import org.ironsight.wpplugin.expandLayerTool.operations.ValueProviders.IMappingValue;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;

public class MappingPointCellEditor extends DefaultCellEditor implements TableCellEditor {
    static JComboBox<MappingPointValue> dropdown = new JComboBox<>();  //FIXME will static cause problems?

    public MappingPointCellEditor() {
        super(dropdown);
        dropdown.setRenderer(new MappingPointCellRenderer());
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        assert value != null;
        assert value instanceof MappingPointValue;

        dropdown.removeAllItems();

        IMappingValue mappingValue = ((MappingPointValue) value).mappingValue;
        ArrayList<MappingPointValue> arr = new ArrayList<>(mappingValue.getMaxValue() - mappingValue.getMinValue());
        for (int i = mappingValue.getMinValue(); i <= mappingValue.getMaxValue(); i++) {
            MappingPointValue pointValue = new MappingPointValue(i, mappingValue);
            arr.add(pointValue);
        }

        if (mappingValue.isDiscrete()) {
            arr.sort(new Comparator<MappingPointValue>() {
                @Override
                public int compare(MappingPointValue o1, MappingPointValue o2) {
                    return o1.mappingValue.valueToString(o1.numericValue).compareTo(o2.mappingValue.valueToString(o2.numericValue));
                }
            });
        }

        for (MappingPointValue mappingPointValue : arr) {
            dropdown.addItem(mappingPointValue);
        }

        return this.getComponent();
    }
}
