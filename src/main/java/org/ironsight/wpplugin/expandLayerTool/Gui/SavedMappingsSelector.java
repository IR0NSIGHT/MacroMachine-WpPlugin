package org.ironsight.wpplugin.expandLayerTool.Gui;

import org.ironsight.wpplugin.expandLayerTool.operations.LayerMapping;
import org.ironsight.wpplugin.expandLayerTool.operations.LayerMappingContainer;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.util.HashMap;
import java.util.function.Consumer;

public class SavedMappingsSelector extends JPanel {
    private final Consumer<LayerMapping> onSelection;
    JList<String> list;
    JScrollPane scrollPane;
    HashMap<String, Integer> nameToUid = new HashMap<>();

    public SavedMappingsSelector(Consumer<LayerMapping> onSelection) {
        this.onSelection = onSelection;
        initComponents();
        updateSelf();
        LayerMappingContainer.INSTANCE.subscribe(this::updateSelf);
    }

    private void updateSelf() {
        int uid = getSelectedProvider();
        nameToUid.clear();
        DefaultListModel<String> model = new DefaultListModel<>();
        for (LayerMapping m : LayerMappingContainer.INSTANCE.queryMappingsAll()) {
            nameToUid.put(m.getName(), m.getUid());
            model.addElement(m.getName());
        }
        list.setModel(model);
        setTo(LayerMappingContainer.INSTANCE.queryMappingById(uid));
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
                LayerMapping mapping =
                        LayerMappingContainer.INSTANCE.queryMappingById(nameToUid.getOrDefault(selected, -1)); //FIXME
                if (mapping != null) onSelection.accept(mapping);
            }
        });
        this.add(scrollPane);
    }

    public int getSelectedProvider() {
        return nameToUid.getOrDefault(list.getSelectedValue(), -1);
    }

    public void setTo(LayerMapping m) {
        list.setSelectedValue(m == null ? "null" : m.getName(), true);
    }
}
