package org.ironsight.wpplugin.expandLayerTool.Gui;

import org.ironsight.wpplugin.expandLayerTool.operations.*;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MacroTreePanel extends JPanel {
    private final MappingMacroContainer container;
    private final LayerMappingContainer mappingContainer;
    private final Consumer<MappingMacro> applyToMap;
    DefaultMutableTreeNode root;
    DefaultTreeModel treeModel;
    JTree tree;
    TreePath[] selectionpaths;
    Consumer<SaveableAction> onSelectAction;
    private LinkedList<UUID> selectedMacros;
    private String filterString = "";

    MacroTreePanel(MappingMacroContainer container, LayerMappingContainer mappingContainer,
                   Consumer<MappingMacro> applyToMap, Consumer<SaveableAction> onSelectAction) {
        this.applyToMap = applyToMap;
        this.container = container;
        this.mappingContainer = mappingContainer;
        this.onSelectAction = onSelectAction;
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
        frame.getContentPane().add(new MacroTreePanel(macros, layers, f -> {
            System.out.println("apply macro " + f);
        }, f -> {
        }), BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);
    }

    private static void getExpansionAndSelection(JTree tree, DefaultMutableTreeNode node, LinkedList<UUID> expanded,
                                                 LinkedList<UUID> selected) {
        Object userObject = node.getUserObject();

        // Check if the node's userObject is a valid type with UUID
        if (userObject instanceof SaveableAction) {
            UUID uid = ((SaveableAction) userObject).getUid();

            // Add to expanded if the node is expanded
            if (tree.isExpanded(new TreePath(node.getPath()))) {
                expanded.add(uid);
            }

            // Add to selected if the node is selected
            TreePath selectionPath = tree.getSelectionPath();
            if (selectionPath != null && selectionPath.getLastPathComponent() == node) {
                selected.add(uid);
            }
        }

        // Recursively process child nodes
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) node.getChildAt(i);
            getExpansionAndSelection(tree, childNode, expanded, selected);
        }
    }

    private static void applyExpansionAndSelection(JTree tree, DefaultMutableTreeNode node, Set<UUID> expanded,
                                                   Set<UUID> selected) {
        Object userObject = node.getUserObject();

        // Check if the node's userObject is a valid type with UUID
        if (userObject instanceof SaveableAction) {
            UUID uid = ((SaveableAction) userObject).getUid();

            // Add to expanded if the node is expanded
            if (expanded.contains(uid)) {
                tree.expandPath(new TreePath(node.getPath()));
            }

            // Add to selected if the node is selected
            if (selected.contains(uid)) {
                tree.addSelectionPath(new TreePath(node.getPath()));
            }
        }

        // Recursively process child nodes
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) node.getChildAt(i);
            applyExpansionAndSelection(tree, childNode, expanded, selected);
        }
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
        LinkedList<UUID> expanded = new LinkedList<>();
        LinkedList<UUID> selected = new LinkedList<>();
        if (tree.getModel().getRoot() != null) {
            //save the currently expanded ones
            getExpansionAndSelection(tree, (DefaultMutableTreeNode) tree.getModel().getRoot(), expanded, selected);
        }


        System.out.println("update macro tree panel");
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

        if (!expanded.isEmpty() && !selected.isEmpty() && tree.getModel().getRoot() != null) applyExpansionAndSelection(
                tree,
                (DefaultMutableTreeNode) tree.getModel().getRoot(),
                new HashSet<>(expanded),
                new HashSet<>(selected));

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
            Object o = tree.getLastSelectedPathComponent();
            if (o instanceof DefaultMutableTreeNode) {
                if (((DefaultMutableTreeNode) o).getUserObject() instanceof SaveableAction) {
                    onSelectAction.accept((SaveableAction) (((DefaultMutableTreeNode) o).getUserObject()));
                }
            }

        });
        JScrollPane scrollPane = new JScrollPane(tree,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        this.add(scrollPane, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout());
        JButton addButton = new JButton("Add macro");
        addButton.addActionListener(e -> {
            container.addMapping();
            update();
        });
        buttons.add(addButton);

        JButton addActionButton = new JButton("Add action");
        addActionButton.addActionListener(e -> {
            mappingContainer.addMapping();
            update();
        });
        buttons.add(addActionButton);

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
                    Window parent = SwingUtilities.getWindowAncestor(this);
                    JDialog macroDialog =
                            MacroDesigner.getDesignerDialog(parent instanceof Frame ? (Frame) parent : null,
                                    (MappingMacro) userObj,
                                    container::updateMapping);
                    macroDialog.setVisible(true);

                }
            }
        });
        buttons.add(editButton);

        JButton applyButton = new JButton("Apply");
        applyButton.addActionListener(f -> onApply());
        buttons.add(applyButton);

        scrollPane.setPreferredSize(new Dimension(500, 600));


        this.add(buttons, BorderLayout.SOUTH);
        this.invalidate();
    }

    private void onApply() {
        //get macros
        for (UUID id : selectedMacros) {
            MappingMacro macro = container.queryById(id);
            if (macro != null) {
                System.out.println("apply macro " + macro);
                applyToMap.accept(macro);
            }
        }

    }
}

