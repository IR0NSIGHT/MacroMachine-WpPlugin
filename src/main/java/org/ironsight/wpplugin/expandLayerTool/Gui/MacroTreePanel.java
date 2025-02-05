package org.ironsight.wpplugin.expandLayerTool.Gui;

import org.ironsight.wpplugin.expandLayerTool.operations.LayerMapping;
import org.ironsight.wpplugin.expandLayerTool.operations.LayerMappingContainer;
import org.ironsight.wpplugin.expandLayerTool.operations.MappingMacro;
import org.ironsight.wpplugin.expandLayerTool.operations.MappingMacroContainer;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.*;
import java.util.stream.Collectors;

public class MacroTreePanel extends JPanel {
    private final MappingMacroContainer container;
    private final LayerMappingContainer mappingContainer;
    DefaultMutableTreeNode root;
    DefaultTreeModel treeModel;
    JTree tree;
    TreePath[] selectionpaths;
    private LinkedList<UUID> selectedMacros;
    private String filterString = "";

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

    private void restoreSelectionPaths(JTree tree, TreePath[] savedPaths) {
        if (savedPaths != null) {
            ArrayList<TreePath> validPaths = new ArrayList<TreePath>();
            for (TreePath path : savedPaths) {
                if (pathStillExists(tree, path)) {
                    validPaths.add(path);
                }
            }
            tree.setSelectionPaths(validPaths.toArray(new TreePath[0]));
        }
    }

    // Helper method to check if a path still exists in the tree
    private boolean pathStillExists(JTree tree, TreePath path) {
        javax.swing.tree.TreeModel model = tree.getModel();
        Object[] nodes = path.getPath();
        Object currentNode = model.getRoot();

        for (int i = 1; i < nodes.length; i++) { // Skip the root
            boolean found = false;
            for (int j = 0; j < model.getChildCount(currentNode); j++) {
                Object child = model.getChild(currentNode, j);
                if (child.equals(nodes[i])) {
                    currentNode = child;
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    private void update() {
        root.removeAllChildren();
        ArrayList<MappingMacro> macros = container.queryAll();
        macros.sort(Comparator.comparing(MappingMacro::getName));
        macros.stream()
                .filter(f -> filterString.isEmpty() || f.getName().toLowerCase().contains(filterString) ||
                        f.getDescription().toLowerCase().contains(filterString))
                .forEach(macro -> {
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
                });
        treeModel.reload(root);
        // Collapse all nodes initially
        for (int i = 1; i < tree.getRowCount(); i++) {
            tree.collapseRow(i);
        }

        restoreSelectionPaths(tree, selectionpaths);
        revalidate();
        repaint();
    }

    private void init() {
        container.subscribe(this::update);
        mappingContainer.subscribe(this::update);
        this.setLayout(new BorderLayout());

        // Create a search field
        JTextField searchField = new JTextField();
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterString = searchField.getText().toLowerCase();
                update();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterString = searchField.getText().toLowerCase();
                update();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterString = searchField.getText().toLowerCase();
                update();
            }
        });
        this.add(searchField, BorderLayout.NORTH);


        root = new DefaultMutableTreeNode("All Macros");
        treeModel = new DefaultTreeModel(root);
        tree = new JTree(treeModel);
        tree.setCellRenderer(new IDisplayUnitCellRenderer());
        tree.getSelectionModel().addTreeSelectionListener(x -> {
            if (tree.getSelectionPaths() != null) selectedMacros = Arrays.stream(tree.getSelectionPaths())
                    .filter(path -> path.getLastPathComponent() instanceof DefaultMutableTreeNode)
                    .map(path -> (DefaultMutableTreeNode) path.getLastPathComponent())
                    .filter(node -> node.getUserObject() instanceof MappingMacro)
                    .map(node -> (MappingMacro) node.getUserObject())
                    .map(MappingMacro::getUid)
                    .collect(Collectors.toCollection(LinkedList::new));
        });
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
            container.deleteMapping(selectedMacros.toArray(new UUID[0]));
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

