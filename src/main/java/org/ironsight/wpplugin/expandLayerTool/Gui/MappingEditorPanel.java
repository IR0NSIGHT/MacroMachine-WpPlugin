package org.ironsight.wpplugin.expandLayerTool.Gui;

import org.ironsight.wpplugin.expandLayerTool.operations.LayerMapping;
import org.ironsight.wpplugin.expandLayerTool.operations.LayerMappingContainer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.function.Consumer;

public class MappingEditorPanel extends JPanel {
    private final Consumer<LayerMapping> onSubmit;
    private LayerMapping mapping;

    private MappingGridPanel mappingDisplay;
    private MappingTextTable table;
    private LayerMappingTopPanel topBar;

    public MappingEditorPanel(Consumer<LayerMapping> onSubmit) {
        super();
        this.onSubmit = onSubmit;
        initComponents();
    }

    public static JDialog createDialog(JFrame parent, Consumer<LayerMapping> applyToMap) {
        // Create a JDialog with the parent frame
        JDialog dialog = new JDialog(parent, "My Dialog", false); // Modal dialog
        JPanel all = new JPanel(new BorderLayout());

        Consumer<LayerMapping> submit = mapping1 -> {
            LayerMappingContainer.INSTANCE.updateMapping(mapping1);
        };

        MappingEditorPanel editor = new MappingEditorPanel(submit);
        all.add(editor, BorderLayout.CENTER);

        SavedMappingsSelector mappingSelector = new SavedMappingsSelector(editor::setMapping);
        mappingSelector.setTo(LayerMappingContainer.INSTANCE.queryMappingsAll()[0]);
        all.add(mappingSelector, BorderLayout.WEST);

        LayerMapping m = LayerMappingContainer.INSTANCE.queryMappingById(mappingSelector.getSelectedProvider());
        editor.setMapping(m);

        JPanel buttons = new JPanel(new FlowLayout());
        JButton apply = new JButton("apply");
        apply.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                submit.accept(editor.mapping);  //SAVE TO CONTAINER ON APPLY
                applyToMap.accept(editor.mapping);
            }
        });
        buttons.add(apply);
        all.add(buttons, BorderLayout.SOUTH);

        dialog.add(all);
        dialog.setLocationRelativeTo(parent); // Center the dialog relative to the parent frame
        dialog.pack();
        return dialog;
    }

    private void initComponents() {
        this.setLayout(new BorderLayout());

        mappingDisplay = new MappingGridPanel();
        table = new MappingTextTable();

        //set up sync between both components
        table.setOnUpdate(this::setMapping);
        table.setOnSelect(mappingDisplay::setSelected);

        mappingDisplay.setOnUpdate(this::setMapping);
        mappingDisplay.setOnSelect(table::setSelected);

        topBar = new LayerMappingTopPanel();
        topBar.setOnChange(this::setMapping);

        JButton submitButtom = new JButton("save");
        submitButtom.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onSubmit.accept(mapping);
            }
        });


        this.add(topBar, BorderLayout.NORTH);
        this.add(table, BorderLayout.EAST);
        this.add(submitButtom, BorderLayout.SOUTH);
        this.add(mappingDisplay, BorderLayout.CENTER);

    }

    public void setMapping(LayerMapping mapping) {
        if (this.mapping != null && this.mapping.equals(mapping)) return;
        table.setMapping(mapping);
        mappingDisplay.setMapping(mapping);
        topBar.setMapping(mapping);

        this.mapping = mapping;
        this.repaint();
    }
}
