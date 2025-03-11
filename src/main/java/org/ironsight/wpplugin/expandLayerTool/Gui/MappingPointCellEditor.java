package org.ironsight.wpplugin.expandLayerTool.Gui;

import org.ironsight.wpplugin.expandLayerTool.operations.ValueProviders.IMappingValue;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EventObject;

public class MappingPointCellEditor extends DefaultCellEditor implements TableCellEditor {
    static JComboBox<MappingPointValue> dropdown = new FixedComboBox();

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
            MappingPointValue pointValue = ((MappingPointValue) value).withValue(i);
            arr.add(pointValue);
        }

        if (mappingValue.isDiscrete()) {
            arr.sort(new Comparator<MappingPointValue>() {
                @Override
                public int compare(MappingPointValue o1, MappingPointValue o2) {
                    return o1.mappingValue.valueToString(o1.numericValue)
                            .compareTo(o2.mappingValue.valueToString(o2.numericValue));
                }
            });
        }
        for (MappingPointValue mappingPointValue : arr) {
            dropdown.addItem(mappingPointValue);
        }
        SwingUtilities.invokeLater(() -> {
            dropdown.requestFocusInWindow();
            dropdown.showPopup();
        });
        dropdown.setSelectedItem(value);
        assert value.equals(dropdown.getSelectedItem());
        return this.getComponent();
    }

    static class FixedComboBox extends JComboBox<MappingPointValue> {
        // this class is necessary to use dropdowns in tables as cell editors
        // default dropdowns will submit the selection on the first key press, making proper search-by-typing impossible
        // this class suppresses submit events when the event was caused by character searching.
        private boolean keyCharSelection;
        @Override
        public boolean selectWithKeyChar(char keyChar) {
            keyCharSelection = true;
            boolean event = super.selectWithKeyChar(keyChar);
            keyCharSelection = false;
            return event;
        }

        @Override
        protected void fireActionEvent() {
            if (keyCharSelection) {
                System.out.println("SUPPRESS ACTION EVENT");
                return;
            }
            System.out.println("FIRE ACTION EVENT");
            super.fireActionEvent();
        }
    }
}
