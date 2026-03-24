package org.ironsight.wpplugin.macromachine.Gui.EditActions;

import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IMappingValue;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IPositionValueGetter;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IPositionValueSetter;

import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventObject;

public class MappingPointCellEditor implements TableCellEditor
{
    private final JComboBox<MappingPointValue> dropdown = new JComboBox<>();
    private final ArrayList<CellEditorListener> listeners = new ArrayList<>();
    private final int[] inputColumns;
    public MappingPointCellEditor(int[] inputColumns) {
        this.inputColumns = inputColumns;
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
        dropdown.removePopupMenuListener(onComboboxSelected);
        dropdown.removeFocusListener(onDropdownFocusLost);

        for (CellEditorListener l : new ArrayList<>(listeners)) {
            l.editingStopped(new ChangeEvent(this));
        }
        return true;
    }

    @Override
    public void cancelCellEditing() {
        dropdown.removePopupMenuListener(onComboboxSelected);
        dropdown.removeFocusListener(onDropdownFocusLost);
        for (CellEditorListener l : new ArrayList<>(listeners)) {
            l.editingCanceled(new ChangeEvent(this));
        }
    }
    private final FocusListener onDropdownFocusLost = new FocusListener() {
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
        listeners.add(l);
    }

    @Override
    public void removeCellEditorListener(CellEditorListener l) {
        listeners.remove(l);
    }

    private boolean isInputColumn(int column) {
        for (int c : inputColumns) {
            if (c == column)
                return true;
        }
        return false;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int viewRow,
            int viewColumn) {
        assert value != null;
        assert value instanceof MappingPointValue;
        dropdown.removeAllItems();

        // TODO dont display skip for inputs

        { // collect avaialbe values to offer in the dropdown for user selection
            IMappingValue mappingValue = ((MappingPointValue) value).mappingValue;
            java.util.List<MappingPointValue> arr;
            int modelColumn = table.convertColumnIndexToModel(viewColumn);
            int[] values;

            boolean isInputColumn = isInputColumn(modelColumn);
            if (isInputColumn && mappingValue instanceof IPositionValueGetter getter) {
                values = getter.getAllInputValues();
            } else if (!isInputColumn && mappingValue instanceof IPositionValueSetter setter) {
                values = setter.getAllOutputValues();
            } else {
                assert false;
                values = mappingValue.getAllPossibleValues();
            }

            arr = Arrays.stream(values).mapToObj(((MappingPointValue) value)::withValue).toList();
            arr.stream().sorted().forEach(dropdown::addItem);
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
