package org.ironsight.wpplugin.macromachine.Gui;

import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IMappingValue;
import org.pepsoft.minecraft.Block;

import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EventObject;

public class MappingPointCellEditor implements TableCellEditor {
    private final JComboBox<MappingPointValue> dropdown = new FixedComboBox();
    private final ArrayList<CellEditorListener> listeners = new ArrayList<>();
    private final FocusListener onDropdownFocusLost = new FocusListener() {
        @Override
        public void focusGained(FocusEvent e) {

        }

        @Override
        public void focusLost(FocusEvent e) {
            cancelCellEditing();
        }
    };
    public MappingPointCellEditor(BlockingSelectionModel selectionModel) {
        dropdown.setRenderer(new MappingPointCellRenderer());
    }    private ActionListener onComboboxSelected = e -> {
        stopCellEditing();
    };

    @Override
    public Object getCellEditorValue() {
        return dropdown.getSelectedItem();
    }

    @Override
    public boolean isCellEditable(EventObject anEvent) {
        return true;
    }

    @Override
    public boolean shouldSelectCell(EventObject anEvent) {
        return true;
    }

    @Override
    public boolean stopCellEditing() {
        System.out.println("CELLEDITOR - STOP EDIT");
        dropdown.removeActionListener(onComboboxSelected);
        dropdown.removeFocusListener(onDropdownFocusLost);

        for (CellEditorListener l : new ArrayList<>(listeners)) {
            l.editingStopped(new ChangeEvent(this));
        }
        return true;
    }

    @Override
    public void cancelCellEditing() {
        System.out.println("CELLEDITOR - CANCEL EDIT");
        dropdown.removeActionListener(onComboboxSelected);
        dropdown.removeFocusListener(onDropdownFocusLost);
        for (CellEditorListener l : new ArrayList<>(listeners)) {
            l.editingCanceled(new ChangeEvent(this));
        }
    }

    @Override
    public void addCellEditorListener(CellEditorListener l) {
        System.out.println("ADD CELL EDITOR LISTENER");
        listeners.add(l);
    }

    @Override
    public void removeCellEditorListener(CellEditorListener l) {
        System.out.println("REMOVE CELL EDITOR LISTENER");
        listeners.remove(l);
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

        dropdown.setSelectedItem(value);
        assert value.equals(dropdown.getSelectedItem());
        SwingUtilities.invokeLater(() -> {
            if (dropdown.requestFocusInWindow())
                dropdown.showPopup();
        });
        dropdown.addActionListener(onComboboxSelected);
        dropdown.addFocusListener(onDropdownFocusLost);
        return dropdown;
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
                return;
            }
            super.fireActionEvent();
        }
    }




}
