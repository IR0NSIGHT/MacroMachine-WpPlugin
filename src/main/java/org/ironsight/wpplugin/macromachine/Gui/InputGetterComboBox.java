package org.ironsight.wpplugin.macromachine.Gui;

import org.ironsight.wpplugin.macromachine.operations.ValueProviders.*;

import javax.swing.*;
import javax.swing.event.ListDataListener;
import java.util.*;
import java.util.function.Consumer;

public class InputGetterComboBox extends JComboBox<IMappingValue> {
    private final IOComboBoxModel model;
    private final IMappingValueProvider provider;
    public InputGetterComboBox(Consumer<IMappingValue> onChangeCallback, IMappingValueProvider provider) {
        this.provider = provider;
        this.model = new IOComboBoxModel(onChangeCallback);
        this.setModel(model);
        this.setRenderer(new SaveableActionRenderer(MacroTreePanel::isValidItem));
        provider.subscribeToUpdates(this::updateSelf);
        updateSelf();
    }

    public void updateSelf() {
        model.setAllowedIOs(provider.getItems());
        this.invalidate();
        this.repaint();
    }

    public IMappingValue getSelectedProvider() {
        return ((IOComboBoxModel)this.getModel()).selected;
    }

    public void SetSelected(IMappingValue getter) {
        model.setSelectedNoUpdate(getter);
        this.invalidate();
        this.repaint();
    }

    public class IOComboBoxModel implements ComboBoxModel<IMappingValue> {
        private final Consumer<IMappingValue> onUserSelectsItem;
        public IOComboBoxModel(Consumer<IMappingValue> onUserSelectsItem ) {
            this.onUserSelectsItem = onUserSelectsItem;
        }
        private IMappingValue[] values;
        private IMappingValue selected = null;
        public void setAllowedIOs(Collection<IMappingValue> values) {
            this.values = values.toArray(new IMappingValue[0]);
            Arrays.sort(this.values, Comparator.comparing(f -> f.getName().toLowerCase()));
        }

        @Override
        public Object getSelectedItem() {
            return selected;
        }

        public void setSelectedNoUpdate(IMappingValue item) {
            this.selected = item;
        }

        @Override
        public void setSelectedItem(Object anItem) {
            assert anItem instanceof IMappingValue;
            selected = (IMappingValue) anItem;
            onUserSelectsItem.accept(selected);
        }

        @Override
        public int getSize() {
            return values.length;
        }

        @Override
        public IMappingValue getElementAt(int index) {
            return values[index];
        }

        @Override
        public void addListDataListener(ListDataListener l) {

        }

        @Override
        public void removeListDataListener(ListDataListener l) {

        }
    }
}


