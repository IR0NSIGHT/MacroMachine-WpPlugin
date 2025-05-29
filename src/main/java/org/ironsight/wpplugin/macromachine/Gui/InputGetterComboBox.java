package org.ironsight.wpplugin.macromachine.Gui;

import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IPositionValueGetter;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.InputOutputProvider;

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
        System.out.println("INPUT DROPDOWN SET TO " + getter.getName());
        this.setSelectedItem(getter.getName());
        assert this.getSelectedProvider().equals(getter) : this.getSelectedProvider() + " != " + getter.getName();
    }
}


