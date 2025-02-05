package org.ironsight.wpplugin.expandLayerTool.Gui;

import org.ironsight.wpplugin.expandLayerTool.operations.LayerMapping;
import org.ironsight.wpplugin.expandLayerTool.operations.LayerMappingContainer;
import org.ironsight.wpplugin.expandLayerTool.operations.MappingMacro;
import org.ironsight.wpplugin.expandLayerTool.operations.MappingMacroContainer;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.util.UUID;

public class MacroTreePanel extends JPanel {
    private final MappingMacroContainer container;
    private final LayerMappingContainer mappingContainer;

    MacroTreePanel(MappingMacroContainer container, LayerMappingContainer mappingContainer) {
        this.container = container;
        this.mappingContainer = mappingContainer;
        init();
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setTitle("Macro Tree Panel");

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

        frame.getContentPane().add(new MacroTreePanel(macros, layers), BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);
    }

    private void init() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root");

        for (MappingMacro macro : container.queryAll()) {
            DefaultMutableTreeNode macroNode = new DefaultMutableTreeNode(macro);
            for (UUID uuid : macro.mappingUids) {
                LayerMapping m = mappingContainer.queryById(uuid);
                if (m == null) {
                    macroNode.add(new DefaultMutableTreeNode("Unknown Mapping"));
                } else {
                    DefaultMutableTreeNode mappingNode = new DefaultMutableTreeNode(m);
                    macroNode.add(mappingNode);
                    DefaultMutableTreeNode inputNode = new DefaultMutableTreeNode(m.input);
                    DefaultMutableTreeNode outputNode = new DefaultMutableTreeNode(m.output);
                    mappingNode.add(inputNode);
                    mappingNode.add(outputNode);
                }
            }
            root.add(macroNode);
        }

        JTree tree = new JTree(root);
        tree.setLayout(new BorderLayout());
        tree.setCellRenderer(new IDisplayUnitCellRenderer());
        JScrollPane scrollPane = new JScrollPane(tree);
        this.add(scrollPane);
    }
}

