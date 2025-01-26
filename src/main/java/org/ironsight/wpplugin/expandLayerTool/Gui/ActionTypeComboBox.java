package org.ironsight.wpplugin.expandLayerTool.Gui;

import org.ironsight.wpplugin.expandLayerTool.operations.LayerMapping;

import javax.swing.*;

public class ActionTypeComboBox extends JComboBox<String> {

    public ActionTypeComboBox() {
        for (LayerMapping.ActionType t : LayerMapping.ActionType.values())
            addItem(t.getDisplayName());
    }


    public LayerMapping.ActionType getSelectedProvider() {
        return LayerMapping.ActionType.values()[getSelectedIndex()];
    }

    public void setTo(LayerMapping.ActionType type) {
        this.setSelectedIndex(type.ordinal());
    }
}
