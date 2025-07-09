package org.ironsight.wpplugin.macromachine.Gui;

import org.ironsight.wpplugin.macromachine.operations.ValueProviders.*;

import javax.swing.*;
import javax.swing.event.ListDataListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.*;
import java.util.function.Consumer;

public class InputGetterComboBox extends JComboBox<IMappingValue> {
    private final IOComboBoxModel model;
    private final IMappingValueProvider provider;
    boolean keyCharSelection; //prevents dropdown from submitting on any keypress (allows multi key inputs)

    public InputGetterComboBox(Consumer<IMappingValue> onChangeCallback, IMappingValueProvider provider) {
        this.provider = provider;
        this.model = new IOComboBoxModel();
        this.setModel(model);
        this.setRenderer(new DisplayUnitRenderer(MacroTreePanel::isValidItem));
        provider.subscribeToUpdates(this::updateSelf);
        updateSelf();
        this.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {

            }

            @Override
            public void focusLost(FocusEvent e) {
                onChangeCallback.accept(model.selected);
            }
        });
    }

    public void updateSelf() {
        model.setAllowedIOs(provider.getItems());
        this.invalidate();
        this.repaint();
    }

    @Override
    public boolean selectWithKeyChar(char keyChar) {
        keyCharSelection = true;
        boolean event = super.selectWithKeyChar(keyChar);
        keyCharSelection = false;
        return event;
    }

    @Override
    protected void fireActionEvent() {
        if (keyCharSelection) {
            return;
        }
        super.fireActionEvent();
    }

    public IMappingValue getSelectedProvider() {
        return ((IOComboBoxModel) this.getModel()).selected;
    }

    public void SetSelected(IMappingValue getter) {
        model.setSelectedNoUpdate(getter);
        this.invalidate();
        this.repaint();
    }

    public class IOComboBoxModel implements ComboBoxModel<IMappingValue> {
        private IMappingValue[] values;
        private IMappingValue selected = null;

        public IOComboBoxModel() {
        }

        public void setAllowedIOs(Collection<IMappingValue> values) {
            this.values = values.toArray(new IMappingValue[0]);
            Comparator<IMappingValue> com =
                    Comparator.comparingInt(o -> o instanceof ILayerGetter && ((ILayerGetter)o).isCustomLayer() ? 1 :
                            -1);
            com =
                    com.thenComparing(IMappingValue::getName);

            Arrays.sort(this.values, com); // Finally sort by name);
        }

        @Override
        public Object getSelectedItem() {
            return selected;
        }

        @Override
        public void setSelectedItem(Object anItem) {
            assert anItem instanceof IMappingValue;
            selected = (IMappingValue) anItem;
        }

        public void setSelectedNoUpdate(IMappingValue item) {
            this.selected = item;
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


