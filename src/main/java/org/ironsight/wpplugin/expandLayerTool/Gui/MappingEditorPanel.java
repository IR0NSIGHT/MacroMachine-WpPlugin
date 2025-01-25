package org.ironsight.wpplugin.expandLayerTool.Gui;

import org.ironsight.wpplugin.expandLayerTool.operations.LayerMapping;

import javax.swing.*;
import java.util.function.Consumer;

public class MappingEditorPanel extends JPanel {
    private final Consumer<LayerMapping> onSubmit;
    private LayerMapping mapping;

    private MappingGridPanel mappingDisplay;
    private MappingTextTable table;

    public MappingEditorPanel(LayerMapping mapping, Consumer<LayerMapping> onSubmit) {
        super();
        this.mapping = mapping;
        this.onSubmit = onSubmit;
        initComponents();
    }

    private void initComponents() {
        mappingDisplay = new MappingGridPanel(mapping);
        table = new MappingTextTable(mapping);

        //set up sync between both components
        table.setOnUpdate(mapping1 -> {
            this.mapping = mapping1;
            mappingDisplay.setMapping(mapping);
        });
        table.setOnSelect(mappingDisplay::setSelected);

        mappingDisplay.setOnUpdate(mapping1 -> {
            this.mapping = mapping1;
            table.setMapping(mapping1);
        });
        mappingDisplay.setOnSelect(table::setSelected);

        this.add(mappingDisplay);
        this.add(table);
    }


    public static void main(String[] args) {
        JFrame frame = new JFrame("TEST PANEL");

        LayerMapping mapper = new LayerMapping(null, null,
                new LayerMapping.MappingPoint[]{new LayerMapping.MappingPoint(20, 10),
                        new LayerMapping.MappingPoint(50, 50), new LayerMapping.MappingPoint(70, 57),});

        MappingEditorPanel table = new MappingEditorPanel(mapper, f -> {
        });

        // Add the outer panel to the frame
        frame.add(table);

        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
