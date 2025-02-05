package org.ironsight.wpplugin.expandLayerTool.Gui;

import org.ironsight.wpplugin.expandLayerTool.operations.*;

import javax.swing.*;
import java.awt.*;
import java.util.UUID;
import java.util.function.Consumer;

// top level panel that contains a selection list of macros/layers/input/output on the left, like a file browser
// and an editor for the currently selected action on the right
public class GlobalActionPanel extends JPanel {
    public static final String MAPPING_EDITOR = "mappingEditor";
    public static final String MACRO_DESIGNER = "macroDesigner";
    MacroTreePanel macroTreePanel;
    MacroDesigner macroDesigner;
    MappingEditorPanel mappingEditor;

    Consumer<MappingMacro> macroConsumer;
    CardLayout layout;
    JPanel editorPanel;

    public GlobalActionPanel(Consumer<MappingMacro> macroConsumer) {
        this.macroConsumer = macroConsumer;
        init();
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        MappingMacroContainer macros = MappingMacroContainer.getInstance();
        LayerMappingContainer layers = LayerMappingContainer.INSTANCE;

        for (int i = 0; i < 10; i++) {
            MappingMacro macro = macros.addMapping();
            UUID[] ids = new UUID[13];
            for (int j = 0; j < ids.length; j++) {
                LayerMapping mapping = layers.addMapping().withName("Mapping Action" + i + "_" + j);
                layers.updateMapping(mapping);
                ids[j] = mapping.getUid();
            }
            macro = macro.withUUIDs(ids).withName("ActionMacro_" + i);
            macros.updateMapping(macro);
        }

        frame.add(new GlobalActionPanel(f -> {
        }));

        frame.pack();
        frame.setVisible(true);
    }

    private void init() {
        this.setLayout(new BorderLayout());
        macroTreePanel = new MacroTreePanel(MappingMacroContainer.getInstance(),
                LayerMappingContainer.INSTANCE,
                this.macroConsumer,
                this::onSelect);
        macroTreePanel.setMaximumSize(new Dimension(200, 0));

        macroDesigner = new MacroDesigner(this::onSubmitMacro);
        mappingEditor = new MappingEditorPanel(this::onSubmitMapping);

        editorPanel = new JPanel(new CardLayout());
        editorPanel.add(mappingEditor, MAPPING_EDITOR);
        editorPanel.add(macroDesigner, MACRO_DESIGNER);
        layout = (CardLayout) editorPanel.getLayout();
        layout.show(editorPanel, MACRO_DESIGNER);
        this.add(macroTreePanel, BorderLayout.WEST);
        this.add(editorPanel, BorderLayout.CENTER);
    }

    private void onSelect(SaveableAction action) {
        if (action instanceof MappingMacro) {
            macroDesigner.setMacro((MappingMacro) action, false);
            layout.show(editorPanel, MACRO_DESIGNER);
        } else if (action instanceof LayerMapping) {
            mappingEditor.setMapping((LayerMapping) action);
            layout.show(editorPanel, MAPPING_EDITOR);
        }
    }

    private void onSubmitMapping(LayerMapping mapping) {
        LayerMappingContainer.INSTANCE.updateMapping(mapping);
    }

    private void onSubmitMacro(MappingMacro macro) {
        MappingMacroContainer.getInstance().updateMapping(macro);
    }

    private void onEditMacro(MappingMacro mapping) {
    }

    private void onEditMapping(LayerMapping mapping) {
    }

    private void update() {

    }
}
