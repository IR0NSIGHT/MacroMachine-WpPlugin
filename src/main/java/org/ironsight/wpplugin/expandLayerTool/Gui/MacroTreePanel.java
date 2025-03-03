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

import static org.ironsight.wpplugin.expandLayerTool.Gui.HelpDialog.getHelpButton;

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

    private static void getExpansionAndSelection(JTree tree, DefaultMutableTreeNode node, LinkedList<UUID> expanded) {
        Object userObject = node.getUserObject();

        // Check if the node's userObject is a valid type with UUID
        if (userObject instanceof SaveableAction) {
            UUID uid = ((SaveableAction) userObject).getUid();

            // Add to expanded if the node is expanded
            if (tree.isExpanded(new TreePath(node.getPath()))) {
                expanded.add(uid);
            }

        }

        // Recursively process child nodes
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) node.getChildAt(i);
            getExpansionAndSelection(tree, childNode, expanded);
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

    private DefaultMutableTreeNode LayerToNode(LayerMapping m) {
        if (m == null) {
            return new DefaultMutableTreeNode("Unknown Mapping");
        } else {
            DefaultMutableTreeNode mappingNode = new DefaultMutableTreeNode(m);
            DefaultMutableTreeNode inputNode = new DefaultMutableTreeNode(m.input);
            DefaultMutableTreeNode outputNode = new DefaultMutableTreeNode(m.output);
            mappingNode.add(inputNode);
            mappingNode.add(outputNode);
            return mappingNode;
        }
    }

    private void update() {
        System.out.println(this.getClass().getSimpleName() + " UPDATE ");
        LinkedList<UUID> expanded = new LinkedList<>();
        LinkedList<UUID> selected = new LinkedList<>();
        if (tree.getModel().getRoot() != null) {
            TreePath[] selectionPaths = tree.getSelectionPaths();
            if (selectionPaths != null) Arrays.stream(selectionPaths).forEach(p -> {
                Object o = p.getLastPathComponent();
                if (o instanceof DefaultMutableTreeNode &&
                        ((DefaultMutableTreeNode) o).getUserObject() instanceof SaveableAction)
                    selected.add(((SaveableAction) ((DefaultMutableTreeNode) o).getUserObject()).getUid());
            });

            //save the currently expanded ones
            getExpansionAndSelection(tree, (DefaultMutableTreeNode) tree.getModel().getRoot(), expanded);
        }


        System.out.println("update macro tree panel");
        root.removeAllChildren();
        ArrayList<MappingMacro> macros = container.queryAll();
        macros.sort(Comparator.comparing(MappingMacro::getName));
        macros.stream()
                .filter(f -> filterString.isEmpty() || f.getName().toLowerCase().contains(filterString) ||
                        f.getDescription().toLowerCase().contains(filterString) || Arrays.stream(f.mappingUids)
                        .map(LayerMappingContainer.INSTANCE::queryById)
                        .anyMatch(action -> action.getName().contains(filterString) ||
                                action.getDescription().contains(filterString)))
                .sorted(new Comparator<MappingMacro>() {
                    @Override
                    public int compare(MappingMacro o1, MappingMacro o2) {
                        return o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
                    }
                })
                .forEach(macro -> {
                    DefaultMutableTreeNode macroNode = new DefaultMutableTreeNode(macro);
                    System.out.println(" create macro node: " + macro.getName());
                    for (UUID uuid : macro.mappingUids) {
                        LayerMapping m = mappingContainer.queryById(uuid);
                        DefaultMutableTreeNode node = LayerToNode(m);
                        macroNode.add(node);
                    }
                    root.add(macroNode);
                });

        DefaultMutableTreeNode allNode = new DefaultMutableTreeNode(new SaveableAction() {
            @Override
            public UUID getUid() {
                return UUID.fromString("123e4567-e89b-12d3-a456-426614174000"); //hardcoded random UUID to make note
                // saveable for slection and expansion.
            }

            @Override
            public String getName() {
                return "All Actions";
            }

            @Override
            public String getDescription() {
                return "all existing actions list";
            }
        });
        mappingContainer.queryAll()
                .stream()
                .filter(f -> filterString.isEmpty() || f.getName().toLowerCase().contains(filterString) ||
                        f.getDescription().toLowerCase().contains(filterString))
                .sorted(new Comparator<LayerMapping>() {
                    @Override
                    public int compare(LayerMapping o1, LayerMapping o2) {
                        return o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
                    }
                })
                .forEach(m -> allNode.add(LayerToNode(m)));
        root.add(allNode);

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
        searchField.setBorder(BorderFactory.createTitledBorder("Search macro"));
        this.add(searchField, BorderLayout.NORTH);


        root = new DefaultMutableTreeNode("All Macros");
        treeModel = new DefaultTreeModel(root);
        tree = new JTree(treeModel);
        tree.setCellRenderer(new IDisplayUnitCellRenderer());
        tree.setRowHeight(-1); //auto set cell height

        tree.getSelectionModel().addTreeSelectionListener(x -> {
            if (tree.getSelectionPaths() != null) selectedMacros = Arrays.stream(tree.getSelectionPaths())
                    .filter(path -> path.getLastPathComponent() instanceof DefaultMutableTreeNode)
                    .map(path -> (DefaultMutableTreeNode) path.getLastPathComponent())
                    .filter(node -> node.getUserObject() instanceof SaveableAction)
                    .map(node -> (SaveableAction) node.getUserObject())
                    .map(SaveableAction::getUid)
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
        JButton addButton = new JButton("Create macro");
        addButton.setToolTipText("Create a new, empty macro.");
        addButton.addActionListener(e -> {
            container.addMapping();
            update();
        });
        buttons.add(addButton);

        JButton addActionButton = new JButton("Create action");
        addActionButton.setToolTipText("Create an new, empty action and add it to all selected macros.");
        addActionButton.addActionListener(e -> {
            LayerMapping m = mappingContainer.addMapping();

            for (UUID macroId : selectedMacros) {
                MappingMacro macro = container.queryById(macroId);
                ArrayList<UUID> ids = new ArrayList<>(macro.mappingUids.length + 1);
                Collections.addAll(ids, macro.mappingUids);
                ids.add(m.getUid());
                container.updateMapping(macro.withUUIDs(ids.toArray(new UUID[0])));
            }

            update();
        });
        buttons.add(addActionButton);

        JButton removeButton = new JButton("Delete");
        removeButton.setToolTipText("Delete all selected macros permanently");
        removeButton.addActionListener(e -> {
            container.deleteMapping(selectedMacros.toArray(new UUID[0]));
            mappingContainer.deleteMapping(selectedMacros.toArray(new UUID[0]));
        });
        buttons.add(removeButton);

        JButton applyButton = new JButton("Apply macros");
        applyButton.setToolTipText(
                "Apply all selected macros to the map. The order in which macros are applied is " + "random.");
        applyButton.addActionListener(f -> onApply());
        buttons.add(applyButton);

        buttons.add(getHelpButton("Global Tree View",
                "This view shows your global list of macros and actions. You can" + " " +
                        "expand the macros, actions and their values.\n" +
                        "Select a macro to open the macro-editor.\n" +
                        "Select an action from a macro or from the 'All actions' node to open the action editor.\n" +
                        "You can create and " +
                        "delete actions and macros in this view. All changes in the global list are directly saved to" +
                        " your " + "save-files. These are global and the same for all projects.\n" + " Press 'Apply'" +
                        " to " +
                        "apply a macro as a global operation to " + "your map."));

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

