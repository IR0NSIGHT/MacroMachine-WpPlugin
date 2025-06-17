package org.ironsight.wpplugin.macromachine.Gui;

import org.ironsight.wpplugin.macromachine.MacroMachinePlugin;
import org.ironsight.wpplugin.macromachine.operations.*;
import org.ironsight.wpplugin.macromachine.operations.FileIO.ConflictResolveImportPolicy;
import org.ironsight.wpplugin.macromachine.operations.FileIO.ContainerIO;
import org.ironsight.wpplugin.macromachine.operations.FileIO.MacroExportPolicy;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.*;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.ironsight.wpplugin.macromachine.Gui.HelpDialog.getHelpButton;
import static org.ironsight.wpplugin.macromachine.MacroMachinePlugin.error;

public class MacroTreePanel extends JPanel {
    private final MacroContainer container;
    private final MappingActionContainer mappingContainer;
    private final MacroApplicator applyToMap;
    DefaultTreeModel treeModel;
    JTree tree;
    ISelectItemCallback onSelectAction;
    private String filterString = "";
    private JButton applyButton;
    private long lastProgressUpdate = 0;
    private boolean macroIsCurrentlyExecuting;
    private boolean blockUpdates = false;
    private String lastImportFilePath = System.getProperty("user.home");
    private String lastExportFilePath = System.getProperty("user.home");

    private SaveableAction lastSelectedItem = null;

    MacroTreePanel(MacroContainer container, MappingActionContainer mappingContainer,
                   MacroApplicator applyToMap, ISelectItemCallback onSelectAction) {
        this.applyToMap = applyToMap;
        this.container = container;
        this.mappingContainer = mappingContainer;
        this.onSelectAction = onSelectAction;
        init();
        update();
    }

    public static String sanitizeFileName(String input) {
        // Define the set of illegal characters for common file systems
        String illegalChars = "[\\\\/:*?\"<>|]";

        // Replace illegal characters with an underscore
        String sanitized = input.replaceAll(illegalChars, "");

        // Optionally, you can also replace or remove other unwanted characters
        // For example, replace whitespace with a single underscore
        sanitized = sanitized.replaceAll("\\s+", "-");

        // Remove leading or trailing periods or spaces that could cause issues
        sanitized = sanitized.trim();
        sanitized = sanitized.replaceAll("^\\.+|\\.+$", "");

        return sanitized;
    }

    public static boolean isValidItem(IDisplayUnit item) {
        if (item instanceof Macro) {
            boolean valid = true;
            for (UUID childId : ((Macro) item).getExecutionUUIDs()) {
                if (MacroContainer.getInstance().queryContains(childId))
                    valid = valid && isValidItem(MacroContainer.getInstance().queryById(childId));
                else if (MappingActionContainer.getInstance().queryContains(childId))
                    valid = valid && isValidItem(MappingActionContainer.getInstance().queryById(childId));
                else
                    valid = false;
            }
            return valid;
        } else if (item instanceof MappingAction) {
            return isValidItem(((MappingAction) item).getInput()) && isValidItem(((MappingAction) item).getOutput());
        } else if (item instanceof IPositionValueSetter && item instanceof ILayerGetter) {
            return InputOutputProvider.INSTANCE.asOutputProvider().existsItem(item);
        } else if (item instanceof IPositionValueGetter && item instanceof ILayerGetter) {
            return InputOutputProvider.INSTANCE.asInputProvider().existsItem(item);
        } else {
            return true;
        }
    }

    private boolean verifyTreePathExists(MacroTreeNode node, Object[] path, int index, Object[] newPath) {
        newPath[index] = node;
        if (index >= path.length - 1)
            return node.equals(path[index]);
        else {
            for (MacroTreeNode child : node.children) {
                if (child.equals(path[index + 1]))
                    return verifyTreePathExists(child, path, index + 1, newPath);
            }
            return false;
        }
    }

    private List<TreePath> findExpandedPaths(JTree tree, MacroTreeNode node, LinkedList<TreePath> expanded) {
        TreePath path = node.getPath();
        if (tree.isExpanded(path))
            expanded.add(path);
        for (MacroTreeNode child : node.children) {
            findExpandedPaths(tree, child, expanded);
        }
        return expanded;
    }

    private List<TreePath> findAllPaths(JTree tree, MacroTreeNode node, LinkedList<TreePath> allPaths) {
        TreePath path = node.getPath();
        allPaths.add(path);
        for (MacroTreeNode child : node.children) {
            findAllPaths(tree, child, allPaths);
        }
        return allPaths;
    }

    private TreePath findPathEquivalent(Object[] oldPath, MacroTreeNode newNode, int index, Object[] newPath) {
        newPath[index] = newNode;
        if (index >= oldPath.length - 1)
            return new TreePath(newPath);
        else {
            for (MacroTreeNode child : newNode.children) {
                if (child.equals(oldPath[index + 1])) {
                    return findPathEquivalent(oldPath, child, index + 1, newPath);
                }
            }
            return null;
        }
    }

    private void update() {
        update(new HashSet<>(0));
    }

    private void update(Set<UUID> selectItems) {
        if (blockUpdates)
            return;
        MacroContainer macroContainer = MacroContainer.getInstance();
        MacroTreeNode newRoot = new MacroTreeNode(MappingActionContainer.getInstance(), macroContainer);

        LinkedList<TreePath> newSelections = new LinkedList<>();
        LinkedList<TreePath> newExpanded = new LinkedList<>();
        if (!selectItems.isEmpty()) {
            List<TreePath> allPaths = findAllPaths(tree, newRoot, new LinkedList<>());
            for (TreePath path : allPaths) {
                MacroTreeNode node = ((MacroTreeNode) path.getLastPathComponent());
                switch (node.payloadType) {
                    case MACRO:
                        if (selectItems.contains(node.getMacro().getUid())) {
                            newExpanded.add(path);
                            newSelections.add(path);
                        }
                        break;
                    case ACTION:
                        if (selectItems.contains(node.getAction().getUid())) {
                            newExpanded.add(path);
                            newSelections.add(path);
                        }
                        break;
                    default:
                        ; //nothing
                }
            }
        } else if (filterString.isEmpty()) { //carry over old state
            // create treepaths to keep previous selection and carry it over to newRoot.
            TreePath[] selectionPaths = tree.getSelectionPaths();
            if (selectionPaths != null) {
                for (TreePath selectionPath : selectionPaths) {
                    Object[] newTreePath = new Object[selectionPath.getPathCount()];
                    if (verifyTreePathExists(newRoot, selectionPath.getPath(), 0, newTreePath)) {
                        newSelections.add(new TreePath(newTreePath));
                    }
                }
            }

            //find expanded paths
            List<TreePath> expanded =
                    findExpandedPaths(tree, (MacroTreeNode) tree.getModel().getRoot(), new LinkedList<>());
            for (TreePath oldPath : expanded) {
                TreePath newPath = findPathEquivalent(oldPath.getPath(), newRoot, 0,
                        new Object[oldPath.getPath().length]);
                if (newPath != null)
                    newExpanded.add(newPath);
            }
        } else {
            // find all paths that end in an item that matches the filterstring.
            List<TreePath> allPaths = findAllPaths(tree, newRoot, new LinkedList<>());
            for (TreePath path : allPaths) {
                IDisplayUnit payload = ((MacroTreeNode) path.getLastPathComponent()).getPayload();
                if (IDisplayUnit.matchesFilterString(filterString, payload))
                    newSelections.add(path);
            }
        }

        //apply changes
        tree.setModel(new DefaultTreeModel(newRoot));
        tree.setSelectionPaths(newSelections.toArray(new TreePath[0]));
        for (TreePath p : newExpanded) {
            tree.expandPath(p);
        }
        revalidate();
        repaint();
    }

    Set<UUID> getSelectedUUIDs(boolean macros, boolean actions) {
        HashSet<UUID> selectedUUIDs = new HashSet<>();
        for (TreePath selected : tree.getSelectionPaths()) {
            MacroTreeNode node = (MacroTreeNode) selected.getLastPathComponent();
            switch (node.payloadType) {
                case MACRO:
                    if (macros)
                        selectedUUIDs.add(node.getMacro().getUid());
                    break;
                case ACTION:
                    if (actions)
                        selectedUUIDs.add(node.getAction().getUid());
                    break;
                default:
                    ; //nothing
            }
        }
        return selectedUUIDs;
    }

    private void init() {
        container.subscribe(this::update);
        mappingContainer.subscribe(this::update);
        this.setLayout(new BorderLayout());

        // Create a search field
        JTextField searchField = new JTextField();
        searchField.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                filterString = searchField.getText().toLowerCase();
                update();
            }
        });
        searchField.setBorder(BorderFactory.createTitledBorder("Search macro"));
        this.add(searchField, BorderLayout.NORTH);


        treeModel = new DefaultTreeModel(new MacroTreeNode(mappingContainer, container));
        tree = new JTree(treeModel);
        tree.setRootVisible(false);
        tree.setCellRenderer(new SaveableActionRenderer(MacroTreePanel::isValidItem));
        tree.setRowHeight(-1); //auto set cell height
        tree.addTreeSelectionListener(e -> {
            JTree tree = (JTree) e.getSource();
            TreePath selectedPath = tree.getSelectionPath();
            if (selectedPath != null) {
                MacroTreeNode selectedNode = (MacroTreeNode) selectedPath.getLastPathComponent();
                switch (selectedNode.getPayloadType()) {
                    case MACRO:
                        onItemInTreeSelected(selectedNode.getMacro(), selectedNode.getPayloadType());
                        break;
                    case ACTION:
                    case INPUT:
                    case OUTPUT:
                        onItemInTreeSelected(selectedNode.getAction(), selectedNode.getPayloadType());
                        break;
                    case INVALID:
                        onItemInTreeSelected(null, selectedNode.getPayloadType());
                        break;
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
            Macro macro = container.addMapping();
            HashSet<UUID> set = new HashSet<>();
            set.add(macro.getUid());
            update(set);

        });
        buttons.add(addButton);

        JButton removeButton = new JButton("Delete");
        removeButton.setToolTipText("Delete all selected macros permanently");
        removeButton.addActionListener(e -> {
            blockUpdates = true;
            Set<UUID> deletedUUIDS = getSelectedUUIDs(true, true);
            ArrayList<Macro> updatedMacros = new ArrayList<>();
            //remove Mapping Actions from all macros
            for (Macro m : container.queryAll()) {
                if (deletedUUIDS.contains(m.getUid()))
                    continue;
                Macro updated = m.withUUIDs(Arrays.stream(m.executionUUIDs)
                        .filter(a -> !deletedUUIDS.contains(a))
                        .toArray(UUID[]::new));
                updatedMacros.add(updated);
            }

            container.updateMapping(MacroMachinePlugin::error, updatedMacros.toArray(new Macro[0]));

            // Delete action / Macro in containers
            container.deleteMapping(deletedUUIDS.toArray(new UUID[0]));
            mappingContainer.deleteMapping(deletedUUIDS.toArray(new UUID[0]));
            blockUpdates = false;
            update();
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

        JButton exportMacroButton = new JButton("Export");
        exportMacroButton.addActionListener(f -> onExportMacroPressed());
        buttons.add(exportMacroButton);

        JButton importMacroButton = new JButton("Import");
        importMacroButton.addActionListener(f -> onImportMacroPressed());
        buttons.add(importMacroButton);

        this.add(buttons, BorderLayout.SOUTH);
        this.invalidate();
    }

    private void onExportMacroPressed() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select a directory");
        fileChooser.setCurrentDirectory(new File(lastExportFilePath));
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY); // Only allow directory selection
        fileChooser.setAcceptAllFileFilterUsed(false); // Optional: Disable the "All Files" filter

        int result = fileChooser.showOpenDialog(null); // Use null or a valid parent component
        lastExportFilePath = fileChooser.getCurrentDirectory().getPath();

        if (result == JFileChooser.APPROVE_OPTION) {
            assert (fileChooser.getSelectedFile() != null) : "user confirmed without selection?";
            String outputDir = fileChooser.getSelectedFile().getPath();
            for (UUID macroId : getSelectedUUIDs(true, false)) {
                Macro lastItem = container.queryById(macroId);
                MacroExportPolicy policy = new MacroExportPolicy(lastItem, MacroContainer.getInstance());
                String fileName = sanitizeFileName(lastItem.getName()) + ".macro";
                File macroFile = new File(outputDir + "/" + fileName);
                ContainerIO.exportFile(MappingActionContainer.getInstance(),
                        MacroContainer.getInstance(),
                        macroFile,
                        policy,
                        MacroMachinePlugin::error);
            }
        }
    }

    private void onImportMacroPressed() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select a file");
        fileChooser.setCurrentDirectory(new File(lastImportFilePath));
        fileChooser.setFileFilter(new FileNameExtensionFilter("Only MacroMachine files", "macro"));
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setMultiSelectionEnabled(true);
        int result = fileChooser.showOpenDialog(this);
        lastImportFilePath = fileChooser.getCurrentDirectory().getPath();
        if (result == JFileChooser.APPROVE_OPTION) {
            for (File selected : fileChooser.getSelectedFiles()) {
                ContainerIO.importFile(MappingActionContainer.getInstance(),
                        MacroContainer.getInstance(),
                        selected,
                        new ConflictResolveImportPolicy(MacroContainer.getInstance(),
                                MappingActionContainer.getInstance(), SwingUtilities.getWindowAncestor(this)),
                        MacroMachinePlugin::error
                );
            }
        }
    }

    private void onItemInTreeSelected(SaveableAction item, GlobalActionPanel.SELECTION_TPYE type) {
        applyButton.setEnabled(type == GlobalActionPanel.SELECTION_TPYE.MACRO);
        onSelectAction.onSelect(item, type);
        lastSelectedItem = item;
    }

    private void onSetProgress(ApplyAction.Progess progess) {
        SwingUtilities.invokeLater(() -> {
            if (Math.abs(lastProgressUpdate - System.currentTimeMillis()) < 100)
                return;
            lastProgressUpdate = System.currentTimeMillis();
            if (progess.totalSteps != 1) {
                applyButton.setText(String.format("%d/%d: %d%%",
                        progess.step + 1,
                        progess.totalSteps,
                        Math.round(progess.progressInStep)));
            } else {
                applyButton.setText(String.format("%d%%", Math.round(progess.progressInStep)));
            }

            applyButton.repaint();
        });
    }

    private void onApply() {
        if (macroIsCurrentlyExecuting)
            return;
        macroIsCurrentlyExecuting = true;
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
            for (UUID id : getSelectedUUIDs(true, false)) {
                Macro macro = container.queryById(id);
                if (macro != null) {
                    applyToMap.applyLayerAction(macro, panel::onSetProgress);
                }
            }

            long diff = System.currentTimeMillis() - startTime;
            try {   //always take at least 1/2 a second for a macro execution to give visual feedback that it ran.
                long minimumWait = 350;
                if (diff < minimumWait) {
                    for (long i = diff; i < minimumWait; i += 10) {
                        onSetProgress(new ApplyAction.Progess(0, 1, (100f * i) / minimumWait));
                        Thread.sleep(10);
                    }

                }
            } catch (InterruptedException ex) {
                error(ex.getMessage());
            }

            SwingUtilities.invokeLater(() -> {
                applyButton.setText("Apply macros");
                applyButton.repaint();
            });
            macroIsCurrentlyExecuting = false;
            applyButton.setEnabled(true);
        });

        // Shutdown the ExecutorService
        executorService.shutdown();

    }

    static class MacroTreeNode implements TreeNode {
        final Object payload;
        GlobalActionPanel.SELECTION_TPYE payloadType;
        private MacroTreeNode[] children;
        private MacroTreeNode parent;

        public MacroTreeNode(MappingActionContainer actions, MacroContainer macros) {
            //ROOT NODE
            children = new MacroTreeNode[macros.queryAll().size()];
            int i = 0;
            for (Macro macro : macros.queryAll()
                    .stream()
                    .sorted(Comparator.comparing(Macro::getName))
                    .toArray(Macro[]::new)) {
                children[i++] = new MacroTreeNode(macro, actions, macros);
            }
            for (MacroTreeNode child : children)
                child.setParent(this);
            payloadType = GlobalActionPanel.SELECTION_TPYE.INVALID;
            payload = new IDisplayUnit() {
                @Override
                public String getName() {
                    return "All macros";
                }

                @Override
                public String getDescription() {
                    return "root";
                }

                @Override
                public String getToolTipText() {
                    return "";
                }
            };
            assert parent == null;
        }

        public MacroTreeNode(Macro macro, MappingActionContainer actions, MacroContainer macros) {
            payload = macro;
            LinkedList<MacroTreeNode> nodes = new LinkedList<>();
            for (UUID id : macro.getExecutionUUIDs()) {
                if (macros.queryContains(id))
                    nodes.add(new MacroTreeNode(macros.queryById(id), actions, macros));
                else if (actions.queryContains(id))
                    nodes.add(new MacroTreeNode(actions.queryById(id)));
            }
            children = nodes.toArray(new MacroTreeNode[0]);

            for (MacroTreeNode child : children)
                child.setParent(this);
            payloadType = GlobalActionPanel.SELECTION_TPYE.MACRO;
        }

        public MacroTreeNode(MappingAction action) {
            payload = action;
            children = new MacroTreeNode[2];
            children[0] = new MacroTreeNode(action.input, action);
            children[1] = new MacroTreeNode(action.output, action);
            for (MacroTreeNode child : children)
                child.setParent(this);
            payloadType = GlobalActionPanel.SELECTION_TPYE.ACTION;
        }

        public MacroTreeNode(IPositionValueSetter output, MappingAction action) {
            payload = action;
            children = new MacroTreeNode[0];
            payloadType = GlobalActionPanel.SELECTION_TPYE.OUTPUT;
        }

        public MacroTreeNode(IPositionValueGetter input, MappingAction action) {
            payload = action;
            children = new MacroTreeNode[0];
            payloadType = GlobalActionPanel.SELECTION_TPYE.INPUT;
        }

        public TreePath getPath() {
            LinkedList<Object> path = new LinkedList<>();
            path.add(this);
            MacroTreeNode it = this;
            while (it.getParent() != null) {
                it = (MacroTreeNode) it.getParent();
                path.add(0, it);
            }

            return new TreePath(path.toArray(new Object[0]));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MacroTreeNode that = (MacroTreeNode) o;
            if (this.getPayloadType() != that.getPayloadType())
                return false;
            switch (getPayloadType()) {
                case INVALID: //root
                    return true;
                case MACRO:
                    return this.getMacro().getUid().equals(that.getMacro().getUid());
                case ACTION:
                case INPUT:
                case OUTPUT:
                    return this.getAction().getUid().equals(that.getAction().getUid());
                default:
                    throw new RuntimeException("incomplete enum");
            }
        }

        @Override
        public TreeNode getChildAt(int childIndex) {
            return children[childIndex];
        }

        @Override
        public int getChildCount() {
            return children.length;
        }

        @Override
        public TreeNode getParent() {
            return parent;
        }

        private void setParent(MacroTreeNode node) {
            this.parent = node;
        }

        @Override
        public int getIndex(TreeNode node) {
            if (node == null) {
                return -1;
            }
            for (int i = 0; i < getChildCount(); i++) {
                if (getChildAt(i) == node) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public boolean getAllowsChildren() {
            return true;
        }

        @Override
        public boolean isLeaf() {
            return children.length == 0;
        }

        @Override
        public Enumeration<? extends TreeNode> children() {
            return Collections.enumeration(Arrays.asList(children));
        }

        public GlobalActionPanel.SELECTION_TPYE getPayloadType() {
            return payloadType;
        }

        public MappingAction getAction() {
            return (MappingAction) payload;
        }

        public Macro getMacro() {
            return (Macro) payload;
        }

        public IPositionValueGetter getInput() {
            return ((MappingAction) payload).input;
        }

        public IPositionValueSetter getOutput() {
            return ((MappingAction) payload).output;
        }

        public IDisplayUnit getPayload() {
            switch (payloadType) {
                case INVALID:
                case MACRO:
                case ACTION:
                    return (IDisplayUnit) payload;
                case INPUT:
                    return getInput();
                case OUTPUT:
                    return getOutput();
                default:
                    return null;
            }
        }

        @Override
        public String toString() {
            return "MacroTreeNode{" +
                    "payload=" + ((IDisplayUnit) payload).getName() +
                    ", payloadType=" + payloadType +
                    '}';
        }
    }
}

