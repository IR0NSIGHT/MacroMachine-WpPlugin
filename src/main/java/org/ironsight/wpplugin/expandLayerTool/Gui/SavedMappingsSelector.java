package org.ironsight.wpplugin.expandLayerTool.Gui;

import org.ironsight.wpplugin.expandLayerTool.operations.LayerMapping;
import org.ironsight.wpplugin.expandLayerTool.operations.LayerMappingContainer;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.function.Consumer;

public class SavedMappingsSelector extends JPanel {
    JList<String> list;
    JScrollPane scrollPane;

    private final Consumer<LayerMapping> onSelection;
    public SavedMappingsSelector(Consumer<LayerMapping> onSelection) {
        this.onSelection = onSelection;
        LayerMappingContainer.INSTANCE.onChange = e -> updateSelf();
        initComponents();
        updateSelf();
    }

    private void updateSelf() {
        DefaultListModel<String> model = new DefaultListModel<>();
        for (LayerMapping m : LayerMappingContainer.INSTANCE.getMappings())
            model.addElement(m.getName());
        list.setModel(model);
        list.repaint();
    }

    private void initComponents() {
        // Create the JList
        list = new JList<>();

        // Wrap the JList in a JScrollPane
        scrollPane = new JScrollPane(list);
        //    scrollPane.setBounds(50, 20, 200, 100); // Set size and position
        list.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                String selected = list.getSelectedValue();
                LayerMapping mapping = LayerMappingContainer.INSTANCE.getMapping(selected);
                if (mapping != null) onSelection.accept(mapping);
            }
        });
        this.add(scrollPane);
    }

    public LayerMapping getSelectedProvider() {
        return LayerMappingContainer.INSTANCE.getMapping(list.getSelectedValue());
    }

    public void setTo(LayerMapping m) {
        list.setSelectedValue(m.getName(), true);
    }
}
