package org.ironsight.wpplugin.expandLayerTool.Gui;

import org.ironsight.wpplugin.expandLayerTool.operations.ValueProviders.IPositionValueGetter;
import org.ironsight.wpplugin.expandLayerTool.operations.ValueProviders.InputOutputProvider;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

public class InputGetterComboBox extends JComboBox<String> {
    Map<String, IPositionValueGetter> stringToGetter = new HashMap<>();

    public InputGetterComboBox() {
        InputOutputProvider.INSTANCE.subscribe(this::updateSelf);
        updateSelf();
    }

    public void updateSelf() {
        this.removeAllItems();
        for (IPositionValueGetter getter : InputOutputProvider.INSTANCE.getters) {
            addGetter(getter);
        }
    }

    private void addGetter(IPositionValueGetter getter) {
        this.stringToGetter.put(getter.getName(), getter);
        this.addItem(getter.getName());
    }

    public IPositionValueGetter getSelectedProvider() {
        return stringToGetter.get((String) getSelectedItem());
    }

    public void SetSelected(IPositionValueGetter getter) {
        this.setSelectedItem(getter.getName());
    }
}


