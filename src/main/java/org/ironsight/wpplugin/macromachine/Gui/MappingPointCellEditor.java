package org.ironsight.wpplugin.macromachine.Gui;

import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IMappingValue;
import org.pepsoft.minecraft.Block;

import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EventObject;

public class MappingPointCellEditor implements TableCellEditor {
    private final JComboBox<MappingPointValue> dropdown = new JComboBox<>();
    private final ArrayList<CellEditorListener> listeners = new ArrayList<>();

    public MappingPointCellEditor() {
        dropdown.setRenderer(new MappingPointCellRenderer());
    }

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
        dropdown.removePopupMenuListener(onComboboxSelected);
        dropdown.removeFocusListener(onDropdownFocusLost);

        for (CellEditorListener l : new ArrayList<>(listeners)) {
            l.editingStopped(new ChangeEvent(this));
        }
        return true;
    }

    @Override
    public void cancelCellEditing() {
        System.out.println("CELLEDITOR - CANCEL EDIT");
        dropdown.removePopupMenuListener(onComboboxSelected);
        dropdown.removeFocusListener(onDropdownFocusLost);
        for (CellEditorListener l : new ArrayList<>(listeners)) {
            l.editingCanceled(new ChangeEvent(this));
        }
    }    private final FocusListener onDropdownFocusLost = new FocusListener() {
        @Override
        public void focusGained(FocusEvent e) {

        }

        @Override
        public void focusLost(FocusEvent e) {
            cancelCellEditing();
        }
    };

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
            arr.sort(Comparator.comparing(o -> o.mappingValue.valueToString(o.numericValue)));
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
        dropdown.addPopupMenuListener(onComboboxSelected);
        dropdown.addFocusListener(onDropdownFocusLost);
        return dropdown;
    }



    private PopupMenuListener onComboboxSelected = new PopupMenuListener() {
        @Override
        public void popupMenuWillBecomeVisible(PopupMenuEvent e) {

        }

        @Override
        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            stopCellEditing();
        }

        @Override
        public void popupMenuCanceled(PopupMenuEvent e) {
            cancelCellEditing();
        }
    };


}
