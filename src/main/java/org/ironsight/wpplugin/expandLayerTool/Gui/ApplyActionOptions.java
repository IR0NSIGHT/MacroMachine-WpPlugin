package org.ironsight.wpplugin.expandLayerTool.Gui;

import org.pepsoft.worldpainter.layers.Layer;

import javax.swing.*;
import javax.swing.event.ListDataListener;

public class ApplyActionOptions {
    public ActionType actionType;
    public float value;
    public Layer layer;

    public ApplyActionOptions() {
    }

    public ApplyActionOptions(ActionType actionType, float value, Layer layer) {
        this.actionType = actionType;
        this.value = value;
        this.layer = layer;
    }

    public enum ActionType {
        INCREMENT("increment"), DECREMENT("decrement"), MULTIPLY("multiply with"), DIVIDE("divide by"), SET("set to");

        ActionType(String displayName) {
            this.displayName = displayName;
        }

        private final String displayName;

        public String getDisplayName() {
            return displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    static class ActionTypeComboModel implements ComboBoxModel<ActionType> {
        private ActionType selectedItem; // The currently selected item
        private final ActionType[] items; // Array of all ActionType enum values

        // Constructor
        public ActionTypeComboModel() {
            // Populate the items array with all values of the ActionType enum
            items = ActionType.values();
            if (items.length > 0) {
                selectedItem = items[0]; // Default to the first item
            }
        }

        @Override
        public void setSelectedItem(Object anItem) {
            if (anItem instanceof ActionType) {
                selectedItem = (ActionType) anItem;
            }
        }

        @Override
        public Object getSelectedItem() {
            return selectedItem;
        }

        @Override
        public int getSize() {
            return items.length;
        }

        @Override
        public ActionType getElementAt(int index) {
            return items[index];
        }

        @Override
        public void addListDataListener(ListDataListener l) {
            // Not required for this basic implementation
        }

        @Override
        public void removeListDataListener(ListDataListener l) {
            // Not required for this basic implementation
        }
    }

}
