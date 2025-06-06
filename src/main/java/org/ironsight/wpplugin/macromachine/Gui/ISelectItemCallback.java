package org.ironsight.wpplugin.macromachine.Gui;

import org.ironsight.wpplugin.macromachine.operations.SaveableAction;

public interface ISelectItemCallback {
    void onSelect(SaveableAction action, GlobalActionPanel.SELECTION_TPYE type);
}
