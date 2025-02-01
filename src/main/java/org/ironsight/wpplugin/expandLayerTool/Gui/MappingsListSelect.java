package org.ironsight.wpplugin.expandLayerTool.Gui;

import org.ironsight.wpplugin.expandLayerTool.operations.*;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.function.Consumer;

public class MappingsListSelect extends LayerMappingPanel {
    JList<String> list;
    JScrollPane scrollPane;
    JButton addButton;
    JButton removeButton;
    HashMap<String, Integer> nameToUid = new HashMap<>();

    public MappingsListSelect(Consumer<LayerMapping> onSelection) {
        super();
        LayerMappingContainer.INSTANCE.subscribe(this::updateComponents);
        updateComponents();
        this.setOnUpdate(onSelection);
    }

    @Override
    protected void updateComponents() {
        int uid = getSelectedProvider();
        nameToUid.clear();
        DefaultListModel<String> model = new DefaultListModel<>();
        for (LayerMapping m : LayerMappingContainer.INSTANCE.queryMappingsAll()) {
            nameToUid.put(m.getName(), m.getUid());
            model.addElement(m.getName());
        }
        list.setModel(model);
        list.setSelectedValue(LayerMappingContainer.INSTANCE.queryMappingById(uid), true);
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
                String selected = list.getSelectedValue();
                LayerMapping mapping =
                        LayerMappingContainer.INSTANCE.queryMappingById(nameToUid.getOrDefault(selected, -1)); //FIXME
                if (mapping != null) updateMapping(mapping);
            }
        });

        addButton = new JButton("Add");
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                LayerMapping newMap = new LayerMapping(new SlopeProvider(), new StonePaletteApplicator(),
                        new MappingPoint[0], ActionType.SET, "new mapping", "description of mapping", -1);
                LayerMappingContainer.INSTANCE.addMapping(newMap);
            }
        });
        removeButton = new JButton("Remove");
        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int uid = getSelectedProvider();
                LayerMappingContainer.INSTANCE.deleteMapping(uid);
            }
        });

        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.add(addButton, BorderLayout.EAST);
        buttonPanel.add(removeButton, BorderLayout.WEST);
        this.add(buttonPanel, BorderLayout.SOUTH);
        this.add(scrollPane, BorderLayout.CENTER);
    }

    public int getSelectedProvider() {
        return nameToUid.getOrDefault(list.getSelectedValue(), -1);
    }
}
