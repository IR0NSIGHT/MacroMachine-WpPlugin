package org.ironsight.wpplugin.expandLayerTool.Gui;

import org.ironsight.wpplugin.expandLayerTool.operations.ValueProviders.IMappingValue;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;

public class MappingPointCellEditor extends DefaultCellEditor implements TableCellEditor {
    static JComboBox<MappingPointValue> dropdown = new JComboBox<>();  //FIXME will static cause problems?

    public MappingPointCellEditor(IMappingValue input, IMappingValue output) {
        super(dropdown);
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        assert value != null;
        assert value instanceof MappingPointValue;

        dropdown.removeAllItems();
        IMappingValue mappingValue = ((MappingPointValue) value).mappingValue;
        for (int i = mappingValue.getMinValue(); i <= mappingValue.getMaxValue(); i++) {
            MappingPointValue pointValue = new MappingPointValue(i, mappingValue);
            dropdown.addItem(pointValue);
        }
        return this.getComponent();
    }

}
