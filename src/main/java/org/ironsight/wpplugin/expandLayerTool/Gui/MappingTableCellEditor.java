package org.ironsight.wpplugin.expandLayerTool.Gui;

import org.ironsight.wpplugin.expandLayerTool.operations.LayerMapping;
import org.ironsight.wpplugin.expandLayerTool.operations.LayerMappingContainer;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

class MappingTableCellEditor extends DefaultCellEditor {
    final JComboBox<LayerMapping> mappingDropdown;
    JPanel panel = new JPanel();
    JButton deleteButton = new JButton("Delete");
    JButton editButton = new JButton("Edit");
    JButton moveUpButton = new JButton("Up");
    JButton moveDownButton = new JButton("Down");

    Consumer<LayerMapping> deleteMapping, editMapping, moveUp, moveDown;

    public MappingTableCellEditor(Consumer<LayerMapping> deleteMapping, Consumer<LayerMapping> editMapping,
                                  Consumer<LayerMapping> moveUp, Consumer<LayerMapping> moveDown) {
        super(new JComboBox<>());
        this.mappingDropdown =
                (JComboBox<LayerMapping>) getComponent(); // Retrieve the JComboBox from DefaultCellEditor

        this.deleteMapping = deleteMapping;
        this.editMapping = editMapping;
        this.moveUp = moveUp;
        this.moveDown = moveDown;

        this.mappingDropdown.setRenderer(new MappingTableCellRenderer());
        init();
    }

    private void init() {
        panel.setLayout(new BorderLayout());
        panel.add(mappingDropdown, BorderLayout.SOUTH);

        JPanel buttons = new JPanel(new FlowLayout()); // Adjust layout for more buttons
        buttons.add(editButton);
        buttons.add(deleteButton);
        buttons.add(moveUpButton); // Optional
        buttons.add(moveDownButton); // Optional
        panel.add(buttons, BorderLayout.NORTH);

        // Add action listeners
        deleteButton.addActionListener(e -> {
            LayerMapping mapping = (LayerMapping) mappingDropdown.getSelectedItem();
            if (mapping != null) deleteMapping.accept(mapping);
        });

        editButton.addActionListener(e -> {
            LayerMapping mapping = (LayerMapping) mappingDropdown.getSelectedItem();
            if (mapping != null) editMapping.accept(mapping);
        });

        moveUpButton.addActionListener(e -> {
            LayerMapping mapping = (LayerMapping) mappingDropdown.getSelectedItem();
            if (mapping != null) moveUp.accept(mapping);
        });

        moveDownButton.addActionListener(e -> {
            LayerMapping mapping = (LayerMapping) mappingDropdown.getSelectedItem();
            if (mapping != null) moveDown.accept(mapping);
        });
        // Reorder button listeners can be added here
    }

    @Override
    public Component getTreeCellEditorComponent(JTree tree, Object value, boolean isSelected, boolean expanded,
                                                boolean leaf, int row) {
        if (!(value instanceof LayerMapping)) {
            throw new IllegalArgumentException("Invalid value for MappingListEditor");
        }
        LayerMapping mapping = (LayerMapping) value;

        mappingDropdown.removeAllItems();
        for (LayerMapping m : LayerMappingContainer.INSTANCE.queryMappingsAll()) {
            mappingDropdown.addItem(m);
        }
        mappingDropdown.setSelectedItem(mapping);

        return panel; // Return the custom panel
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        if (!(value instanceof LayerMapping)) {
            throw new IllegalArgumentException("Invalid value for MappingListEditor");
        }
        LayerMapping mapping = (LayerMapping) value;

        mappingDropdown.removeAllItems();
        for (LayerMapping m : LayerMappingContainer.INSTANCE.queryMappingsAll()) {
            mappingDropdown.addItem(m);
        }
        mappingDropdown.setSelectedItem(mapping);
        return panel; // Return the custom panel
    }
}
