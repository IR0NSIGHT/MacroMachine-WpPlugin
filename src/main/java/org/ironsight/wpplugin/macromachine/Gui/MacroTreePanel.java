package org.ironsight.wpplugin.macromachine.Gui;

import org.ironsight.wpplugin.macromachine.MacroMachinePlugin;
import org.ironsight.wpplugin.macromachine.operations.*;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.ironsight.wpplugin.macromachine.Gui.HelpDialog.getHelpButton;

public class MacroTreePanel extends JPanel {
    private final MappingMacroContainer container;
    private final LayerMappingContainer mappingContainer;
    private final MacroApplicator applyToMap;
    DefaultMutableTreeNode root;
    DefaultTreeModel treeModel;
    JTree tree;
    TreePath[] selectionpaths;
    ISelectItemCallback onSelectAction;
    private LinkedList<UUID> selectedMacros;
    private String filterString = "";
    private JButton applyButton;


    MacroTreePanel(MappingMacroContainer container, LayerMappingContainer mappingContainer,
                   MacroApplicator applyToMap,ISelectItemCallback onSelectAction) {
        this.applyToMap = applyToMap;
        this.container = container;
        this.mappingContainer = mappingContainer;
        this.onSelectAction = onSelectAction;
        init();
        update();
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

    private GlobalActionPanel.SELECTION_TPYE isInputOrOutputNode(DefaultMutableTreeNode node) {
        Object userObject = node.getUserObject();
        if (node.getParent() instanceof DefaultMutableTreeNode) {
            Object parentUserObject = ((DefaultMutableTreeNode) node.getParent()).getUserObject();
            if (!(parentUserObject instanceof LayerMapping))
                return GlobalActionPanel.SELECTION_TPYE.INVALID;
            if (userObject.equals(((LayerMapping) parentUserObject).input))
                return GlobalActionPanel.SELECTION_TPYE.INPUT;
            if (userObject.equals(((LayerMapping) parentUserObject).output))
                return GlobalActionPanel.SELECTION_TPYE.OUTPUT;
            return GlobalActionPanel.SELECTION_TPYE.INVALID;
        }
        return GlobalActionPanel.SELECTION_TPYE.INVALID;
    }


    private DefaultMutableTreeNode LayerToNode(SaveableAction m) {
        if (m == null) {
            return new DefaultMutableTreeNode("Unknown Mapping");
        } else {
            DefaultMutableTreeNode mappingNode = new DefaultMutableTreeNode(m);
            if (m instanceof LayerMapping) {
                DefaultMutableTreeNode inputNode = new DefaultMutableTreeNode(((LayerMapping) m).input);
                DefaultMutableTreeNode outputNode = new DefaultMutableTreeNode(((LayerMapping) m).output);
                mappingNode.add(inputNode);
                mappingNode.add(outputNode);
            } else if (m instanceof MappingMacro) {
                for (UUID uuid : ((MappingMacro) m).executionUUIDs) {
                    SaveableAction child = mappingContainer.queryById(uuid);
                    if (child == null) {
                        child = container.queryById(uuid);  //FIXME rename container to macroContainer
                    }
                    mappingNode.add(LayerToNode(child));
                }
            }
            return mappingNode;
        }
    }

    private void update() {
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


        root.removeAllChildren();
        ArrayList<MappingMacro> macros = container.queryAll();
        macros.sort(Comparator.comparing(MappingMacro::getName));
        macros.stream()
                .filter(f -> filterString.isEmpty() || f.getName().toLowerCase().contains(filterString) ||
                        f.getDescription().toLowerCase().contains(filterString) || Arrays.stream(f.executionUUIDs)
                        .map(LayerMappingContainer.INSTANCE::queryById).filter(Objects::nonNull)
                        .anyMatch(action -> action.getName().contains(filterString) ||
                                action.getDescription().contains(filterString)))
                .sorted(Comparator.comparing(o -> o.getName().toLowerCase()))
                .forEach(macro -> {
                    DefaultMutableTreeNode macroNode = new DefaultMutableTreeNode(macro);
                    for (UUID uuid : macro.executionUUIDs) {
                        SaveableAction m = mappingContainer.queryById(uuid);
                        if (m == null) m = container.queryById(uuid);
                        DefaultMutableTreeNode node = LayerToNode(m);
                        macroNode.add(node);
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

    private LayerMapping getParentLayerMapping(DefaultMutableTreeNode node) {
        TreeNode parent = node.getParent();
        if (parent instanceof DefaultMutableTreeNode) {
            Object userObject = ((DefaultMutableTreeNode) parent).getUserObject();
            if (userObject instanceof LayerMapping) {
                return (LayerMapping) userObject;
            }
        }
        return null;
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
                // Check if the selected node is an input or output node
                switch (isInputOrOutputNode((DefaultMutableTreeNode)o)) {
                    case INPUT:
                        onSelectAction.onSelect(getParentLayerMapping((DefaultMutableTreeNode)o),
                                GlobalActionPanel.SELECTION_TPYE.INPUT);
                        break;
                    case OUTPUT:
                        onSelectAction.onSelect(getParentLayerMapping((DefaultMutableTreeNode)o),
                                GlobalActionPanel.SELECTION_TPYE.OUTPUT);
                        break;
                    default:
                        if (((DefaultMutableTreeNode) o).getUserObject() instanceof SaveableAction) {
                            SaveableAction selectedAction = (SaveableAction) (((DefaultMutableTreeNode) o).getUserObject());
                            applyButton.setVisible(selectedAction instanceof MappingMacro);
                            GlobalActionPanel.SELECTION_TPYE type = GlobalActionPanel.SELECTION_TPYE.INVALID;
                            if (selectedAction instanceof MappingMacro)
                                type = GlobalActionPanel.SELECTION_TPYE.MACRO;
                            if (selectedAction instanceof LayerMapping)
                                type = GlobalActionPanel.SELECTION_TPYE.ACTION;
                            onSelectAction.onSelect(selectedAction,type);
                        }
                }
            }
            else {
                MacroMachinePlugin.error("the selected tree node is of unknonw type:" + o);
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

        JButton removeButton = new JButton("Delete");
        removeButton.setToolTipText("Delete all selected macros permanently");
        removeButton.addActionListener(e -> {

            //remove Mapping Actions from all macros
            HashSet<UUID> deletedUUIDS = new HashSet<>();
            deletedUUIDS.addAll(selectedMacros);
            for (MappingMacro m : container.queryAll()) {
                MappingMacro updated = m.withUUIDs(Arrays.stream(m.executionUUIDs)
                        .filter(a -> !deletedUUIDS.contains(a))
                        .toArray(UUID[]::new));
                container.updateMapping(updated, f -> {
                    throw new RuntimeException(f);
                });
            }

            // Delete action / Macro in containers
            container.deleteMapping(selectedMacros.toArray(new UUID[0]));
            mappingContainer.deleteMapping(selectedMacros.toArray(new UUID[0]));
        });
        buttons.add(removeButton);

        applyButton = new JButton("Apply macros");
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
                        " to " + "apply a macro as a global operation to " + "your map."));

        scrollPane.setPreferredSize(new Dimension(500, 600));


        this.add(buttons, BorderLayout.SOUTH);
        this.invalidate();
    }

    private long lastProgressUpdate = 0;
    private void onSetProgress(ApplyAction.Progess progess) {
        SwingUtilities.invokeLater(() -> {
            if (Math.abs(lastProgressUpdate - System.currentTimeMillis()) < 100)
                return;
            lastProgressUpdate = System.currentTimeMillis();
            applyButton.setText(String.format("%d/%d: %d%%", progess.step+1, progess.totalSteps,
                    Math.round(progess.progressInStep)));
            applyButton.repaint();
        });
    }

    private boolean macroInAction;

    private void onApply() {
        if (macroInAction)
            return;
        macroInAction = true;
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        SwingUtilities.invokeLater(() -> {
            applyButton.setEnabled(false);
            applyButton.setText("Starting...");
            applyButton.repaint();
        });
        // Submit a task to the ExecutorService
        final MacroTreePanel panel = this;
        executorService.submit(() -> {
            long startTime = System.currentTimeMillis();
            //get macros
            for (UUID id : selectedMacros) {
                MappingMacro macro = container.queryById(id);
                if (macro != null) {
                    applyToMap.applyLayerAction(macro, panel::onSetProgress);
                }
            }

            long diff = System.currentTimeMillis() - startTime;
            try {   //always take at least 1/2 a second for a macro execution to give visual feedback that it ran.
                long minimumWait = 350;
                if (diff < minimumWait) {
                    for (long i = diff; i < minimumWait; i+=10) {
                        onSetProgress(new ApplyAction.Progess(0,1,(100f*i)/minimumWait));
                        Thread.sleep(10);
                    }

                }
            } catch (InterruptedException ex) {
                MacroMachinePlugin.error(ex.getMessage());
            }

            SwingUtilities.invokeLater(() -> {
                applyButton.setText("Apply macros");
                applyButton.repaint();
            });
            macroInAction = false;
            applyButton.setEnabled(true);
        });

        // Shutdown the ExecutorService
        executorService.shutdown();

    }
}

