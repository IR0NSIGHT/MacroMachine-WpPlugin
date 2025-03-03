package org.ironsight.wpplugin.expandLayerTool.Gui;

import org.ironsight.wpplugin.expandLayerTool.operations.ActionType;

import javax.swing.*;
import java.util.Arrays;
import java.util.Comparator;

public class ActionTypeComboBox extends JComboBox<String> {

    public ActionTypeComboBox() {
        for (ActionType t :
                Arrays.stream(ActionType.values()).sorted(Comparator.comparing(ActionType::getDisplayName
        )).toArray(ActionType[]::new))
            addItem(t.getDisplayName());
    }

    public ActionType getSelectedProvider() {
        return ActionType.values()[getSelectedIndex()];
    }

    public void setTo(ActionType type) {
        this.setSelectedIndex(type.ordinal());
    }
}

