package org.ironsight.wpplugin.expandLayerTool.Gui;

import org.ironsight.wpplugin.expandLayerTool.operations.LayerMapping;
import org.ironsight.wpplugin.expandLayerTool.operations.MappingMacro;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.function.Consumer;

public class ActionEditor extends LayerMappingPanel {
    private static JDialog dialog;
    private final Consumer<LayerMapping> onSubmit;
    private MappingGridPanel mappingDisplay;
    private MappingTextTable table;
    private LayerMappingTopPanel topBar;

    public ActionEditor(Consumer<LayerMapping> onSubmit) {
        super();
        this.onSubmit = onSubmit;
    }

    public static JDialog createDialog(JFrame parent, Consumer<MappingMacro> applyToMap) {
        if (dialog != null) {
            dialog.setVisible(true);
            dialog.toFront();        // Bring it to the front
            dialog.requestFocus();   // Request focus (optional)
            return dialog;
        }

        // Create a JDialog with the parent frame
        dialog = new JDialog(parent, "My Dialog", false); // Modal dialog
        dialog.setLocationRelativeTo(null); // Centers the dialog


        dialog.add(new GlobalActionPanel(applyToMap));
        dialog.setLocationRelativeTo(parent); // Center the dialog relative to the parent frame
        dialog.pack();
        return dialog;
    }

    @Override
    protected void updateComponents() {
        table.setMapping(mapping);
        mappingDisplay.setMapping(mapping);
        topBar.setMapping(mapping);
        this.repaint();
    }

    @Override
    protected void initComponents() {
        this.setLayout(new BorderLayout());

        mappingDisplay = new MappingGridPanel();
        table = new MappingTextTable();

        //set up sync between both components
        table.setOnUpdate(this::updateMapping);
        table.setOnSelect(mappingDisplay::setSelected);

        mappingDisplay.setOnUpdate(this::updateMapping);
        mappingDisplay.setOnSelect(table::setSelected);

        topBar = new LayerMappingTopPanel();
        topBar.setOnUpdate(this::updateMapping);

        JButton submitButtom = new JButton("save");
        submitButtom.setToolTipText("Submit action and save to global list.");
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
}
