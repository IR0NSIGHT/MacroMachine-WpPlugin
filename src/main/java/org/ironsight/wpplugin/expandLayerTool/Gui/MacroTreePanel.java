package org.ironsight.wpplugin.expandLayerTool.Gui;

import org.ironsight.wpplugin.expandLayerTool.operations.LayerMapping;
import org.ironsight.wpplugin.expandLayerTool.operations.LayerMappingContainer;
import org.ironsight.wpplugin.expandLayerTool.operations.MappingMacro;
import org.ironsight.wpplugin.expandLayerTool.operations.MappingMacroContainer;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.UUID;

public class MacroTreePanel extends JPanel {
    private final MappingMacroContainer container;
    private final LayerMappingContainer mappingContainer;
    DefaultMutableTreeNode root;
    DefaultTreeModel treeModel;

    MacroTreePanel(MappingMacroContainer container, LayerMappingContainer mappingContainer) {
        this.container = container;
        this.mappingContainer = mappingContainer;
        init();
        update();
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
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(new MacroTreePanel(macros, layers), BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);
    }

    private void update() {
        root.removeAllChildren();
        ArrayList<MappingMacro> macros = container.queryAll();
        macros.sort(Comparator.comparing(MappingMacro::getName));
        for (MappingMacro macro : macros) {
            DefaultMutableTreeNode macroNode = new DefaultMutableTreeNode(macro);
            System.out.println(" create macro node: " + macro.getName());
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
        treeModel.reload(root);
        // Collapse all nodes initially
        for (int i = 1; i < tree.getRowCount(); i++) {
            tree.collapseRow(i);
        }
        revalidate();
        repaint();
    }
    JTree tree;
    private void init() {
        container.subscribe(this::update);
        mappingContainer.subscribe(this::update);

        this.setLayout(new BorderLayout());
        root = new DefaultMutableTreeNode("All Macros");
        treeModel = new DefaultTreeModel(root);
        tree = new JTree(treeModel);
        tree.setCellRenderer(new IDisplayUnitCellRenderer());
        JScrollPane scrollPane = new JScrollPane(tree);
        this.add(scrollPane, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout());
        JButton addButton = new JButton("Add");
        addButton.addActionListener(e -> {
            container.addMapping();
            update();
        });
        buttons.add(addButton);

        JButton removeButton = new JButton("Remove");
        removeButton.addActionListener(e -> {
            Object obj = tree.getLastSelectedPathComponent();
            if (obj instanceof DefaultMutableTreeNode) {
                DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) obj;
                Object userObj = treeNode.getUserObject();
                if (userObj instanceof MappingMacro) {
                    container.deleteMapping(((MappingMacro) userObj).getUid());
                    update();
                }
            }
        });
        buttons.add(removeButton);

        JButton editButton = new JButton("Edit");
        editButton.addActionListener(e -> {
            Object obj = tree.getLastSelectedPathComponent();
            if (obj instanceof DefaultMutableTreeNode) {
                DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) obj;
                Object userObj = treeNode.getUserObject();
                if (userObj instanceof MappingMacro) {
                    JDialog macroDialog =
                            MacroDesigner.getDesignerDialog((Frame) SwingUtilities.getWindowAncestor(this),
                                    (MappingMacro) userObj,
                                    container::updateMapping);
                    macroDialog.setVisible(true);

                }
            }
        });
        scrollPane.setPreferredSize(new Dimension(800, 600));

        buttons.add(editButton);
        this.add(buttons, BorderLayout.SOUTH);
        this.invalidate();
    }
}

