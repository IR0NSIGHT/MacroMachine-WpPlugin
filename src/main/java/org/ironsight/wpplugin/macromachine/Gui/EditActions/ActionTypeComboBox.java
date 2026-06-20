package org.ironsight.wpplugin.macromachine.Gui.EditActions;

import javax.swing.*;
import org.ironsight.wpplugin.macromachine.operations.ActionType;

public class ActionTypeComboBox extends JComboBox<String>
{

    public ActionTypeComboBox() {
        for (ActionType t : ActionType.values())
            addItem(t.getDisplayName());
    }

    public ActionType getSelectedProvider() {
        return ActionType.values()[getSelectedIndex()];
    }

    public void setTo(ActionType type) {
        this.setSelectedIndex(type.ordinal());
        assert this.getSelectedProvider().equals(type);
    }
}
