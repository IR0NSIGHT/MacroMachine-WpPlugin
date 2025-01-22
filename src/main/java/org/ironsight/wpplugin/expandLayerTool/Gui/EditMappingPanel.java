package org.ironsight.wpplugin.expandLayerTool.Gui;

import org.ironsight.wpplugin.expandLayerTool.operations.LayerMapping;

import javax.swing.*;
import java.util.function.Consumer;

public class EditMappingPanel extends JPanel {
    private final Consumer<LayerMapping> onSubmit;
    private LayerMapping mapping;

    private MappingDisplay mappingDisplay;
    private MappingTextTable table;

    public EditMappingPanel(LayerMapping mapping, Consumer<LayerMapping> onSubmit) {
        super();
        this.mapping = mapping;
        this.onSubmit = onSubmit;
        initComponents();
        updateComponents();
    }

    private void initComponents() {
        mappingDisplay = new MappingDisplay(mapping);
        table = new MappingTextTable(mapping, this::setMapping);
        this.add(mappingDisplay);
        this.add(table);
    }

    private void updateComponents() {
        mappingDisplay.setMapping(mapping);
        table.setMapping(mapping);
    }

    public void setMapping(LayerMapping mapping) {
        this.mapping = mapping;
        updateComponents();
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("TEST PANEL");

        LayerMapping mapper = new LayerMapping(null, null,
                new LayerMapping.MappingPoint[]{
                        new LayerMapping.MappingPoint(20, 10),
                        new LayerMapping.MappingPoint(50, 50),
                        new LayerMapping.MappingPoint(70, 57),
                });

        EditMappingPanel table = new EditMappingPanel(mapper, f -> {
        });

        // Add the outer panel to the frame
        frame.add(table);

        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
