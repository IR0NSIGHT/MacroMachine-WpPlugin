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
    public static final String INVALID_SELECTION = "invalidSelection";
    public static final String MACRO_DESIGNER = "macroDesigner";
    MacroTreePanel macroTreePanel;
    MacroDesigner macroDesigner;
    ActionEditor mappingEditor;

    //consumes macro to apply to map. callback for "user pressed apply-macro"
    Consumer<MappingMacro> applyMacro;
    CardLayout layout;
    JPanel editorPanel;
    private UUID currentSelectedMacro;
    private UUID currentSelectedLayer;

    public GlobalActionPanel(Consumer<MappingMacro> applyMacro) {
        this.applyMacro = applyMacro;
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

    private void onUpdate() {
        LayerMapping mapping = LayerMappingContainer.INSTANCE.queryById(currentSelectedLayer);
        MappingMacro macro = MappingMacroContainer.getInstance().queryById(currentSelectedMacro);
        if (macro == null && selectionType == SELECTION_TPYE.MACRO)
            selectionType = SELECTION_TPYE.INVALID;

        if (mapping == null && selectionType == SELECTION_TPYE.ACTION)
            selectionType = SELECTION_TPYE.INVALID;


        switch (selectionType) {
            case MACRO:
                macroDesigner.setMacro(macro, true);
                layout.show(editorPanel, MACRO_DESIGNER);
                break;
            case ACTION:
                mappingEditor.setMapping(mapping);
                layout.show(editorPanel, MAPPING_EDITOR);
                break;
            case INVALID:
                layout.show(editorPanel, INVALID_SELECTION);
                break;
        }
    }

    private void init() {
        MappingMacroContainer.getInstance().subscribe(this::onUpdate);
        LayerMappingContainer.INSTANCE.subscribe(this::onUpdate);

        this.setLayout(new BorderLayout());
        macroTreePanel = new MacroTreePanel(MappingMacroContainer.getInstance(),
                LayerMappingContainer.INSTANCE,
                this.applyMacro,
                this::onSelect);
        macroTreePanel.setMaximumSize(new Dimension(200, 0));

        macroDesigner = new MacroDesigner(this::onSubmitMacro);
        mappingEditor = new ActionEditor(this::onSubmitMapping);

        editorPanel = new JPanel(new CardLayout());
        editorPanel.add(mappingEditor, MAPPING_EDITOR);
        editorPanel.add(macroDesigner, MACRO_DESIGNER);
        editorPanel.add(new JPanel(), INVALID_SELECTION);
        layout = (CardLayout) editorPanel.getLayout();
        layout.show(editorPanel, MACRO_DESIGNER);
        this.add(macroTreePanel, BorderLayout.WEST);
        this.add(editorPanel, BorderLayout.CENTER);
    }

    enum SELECTION_TPYE {
        MACRO,
        ACTION,
        INVALID
    }
    private SELECTION_TPYE selectionType = SELECTION_TPYE.INVALID;
    private void onSelect(SaveableAction action) {
        if (action instanceof MappingMacro) {
            currentSelectedMacro = action.getUid();
            selectionType = SELECTION_TPYE.MACRO;
        } else if (action instanceof LayerMapping) {
            currentSelectedLayer = action.getUid();
            selectionType = SELECTION_TPYE.ACTION;
        }
        onUpdate();
    }

    private void onSubmitMapping(LayerMapping mapping) {
        LayerMappingContainer.INSTANCE.updateMapping(mapping);
    }

    private void onSubmitMacro(MappingMacro macro) {
        MappingMacroContainer.getInstance().updateMapping(macro);
    }

}
