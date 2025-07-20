package org.ironsight.wpplugin.macromachine.Gui;

import org.ironsight.wpplugin.macromachine.operations.ValueProviders.*;

import javax.swing.*;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.function.Consumer;

import static org.ironsight.wpplugin.macromachine.Gui.IDisplayUnitCellRenderer.DEFAULT_BACKGROUND;
import static org.ironsight.wpplugin.macromachine.Gui.IDisplayUnitCellRenderer.SELECTED_BACKGROUND;

public class IoSelectLabel extends JPanel {
    private DisplayUnitRenderer renderer = new DisplayUnitRenderer(f->true);
    private IMappingValue selected;
    private final IMappingValueProvider provider;
    private Consumer<IMappingValue> onChangeCallback;
    public IoSelectLabel(Consumer<IMappingValue> onChangeCallback, IMappingValueProvider provider) {
        this.provider = provider;
        this.onChangeCallback = onChangeCallback;
        this.setLayout(new BorderLayout());

        JPanel p = (JPanel) renderer.renderFor(new AnnotationSetter(), false);
        addClickListener(p);
        this.add(p);
    }

    private void showPickingDialog() {
        new DisplayUnitPickerDialog(new ArrayList<IDisplayUnit>(provider.getItems()),this::onPickerSubmit,
                Collections.emptyList(),
                this).setVisible(true);

    }
    private void onPickerSubmit(IDisplayUnit selected) {
        SetSelected((IMappingValue)selected);
        onChangeCallback.accept(((IMappingValue) selected).instantiateFrom(((IMappingValue) selected).getSaveData()));  //return clone
    }
    private void addClickListener(JPanel panel) {
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showPickingDialog();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                panel.setBackground(SELECTED_BACKGROUND);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                panel.setCursor(Cursor.getDefaultCursor());
                panel.setBackground(DEFAULT_BACKGROUND);
            }
        });
    }

    public IMappingValue getSelectedProvider() {
        return selected;
    }

    public void SetSelected(IMappingValue getter) {
        selected = getter;
        Component p = renderer.renderFor(getter, false); //implicitly updates our child component
        this.invalidate();
        this.repaint();
    }
}


