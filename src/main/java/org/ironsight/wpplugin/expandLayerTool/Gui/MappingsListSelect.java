package org.ironsight.wpplugin.expandLayerTool.Gui;

import org.ironsight.wpplugin.expandLayerTool.operations.LayerMapping;
import org.ironsight.wpplugin.expandLayerTool.operations.LayerMappingContainer;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.UUID;
import java.util.function.Consumer;

public class MappingsListSelect extends LayerMappingPanel {
    JList<ListItem> list;
    JScrollPane scrollPane;
    JButton addButton;
    JButton removeButton;

    public MappingsListSelect(Consumer<LayerMapping> onSelection) {
        super();
        LayerMappingContainer.INSTANCE.subscribe(this::updateComponents);
        updateComponents();
        this.setOnUpdate(onSelection);
    }

    @Override
    protected void updateComponents() {
        DefaultListModel<ListItem> model = new DefaultListModel<>();
        for (LayerMapping m : LayerMappingContainer.INSTANCE.queryMappingsAll()) {
            model.addElement(new ListItem(m));
        }
        list.setModel(model);
        if (this.mapping != null) list.setSelectedValue(new ListItem(this.mapping), true);
        list.repaint();
    }

    @Override
    protected void initComponents() {
        this.setLayout(new BorderLayout());
        // Create the JList
        list = new JList<>();

        // Wrap the JList in a JScrollPane
        scrollPane = new JScrollPane(list);
        //    scrollPane.setBounds(50, 20, 200, 100); // Set size and position
        list.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                ListItem item = list.getSelectedValue();
                if (item == null) return;
                updateMapping(item.mappingItem);
            }
        });

        addButton = new JButton("Add");
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                LayerMapping ignored = LayerMappingContainer.INSTANCE.addMapping();
            }
        });
        removeButton = new JButton("Remove");
        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                UUID uid = getSelectedProvider();
                LayerMappingContainer.INSTANCE.deleteMapping(uid);
            }
        });

        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.add(addButton, BorderLayout.EAST);
        buttonPanel.add(removeButton, BorderLayout.WEST);
        this.add(buttonPanel, BorderLayout.SOUTH);
        this.add(scrollPane, BorderLayout.CENTER);
    }

    public UUID getSelectedProvider() {
        return (this.mapping != null) ? this.mapping.getUid() : null;
    }

    private static class ListItem {
        final LayerMapping mappingItem;

        public ListItem(LayerMapping mappingItem) {
            assert mappingItem != null;
            this.mappingItem = mappingItem;
        }

        @Override
        public String toString() {
            return this.mappingItem.getName();
        }

        @Override
        public boolean equals(Object obj) { //by UID
            return obj instanceof ListItem && this.mappingItem.getUid().equals(((ListItem) obj).mappingItem.getUid());
        }
    }
}
