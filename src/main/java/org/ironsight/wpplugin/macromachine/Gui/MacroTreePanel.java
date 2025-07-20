package org.ironsight.wpplugin.macromachine.Gui;

import org.ironsight.wpplugin.macromachine.MacroMachinePlugin;
import org.ironsight.wpplugin.macromachine.operations.*;
import org.ironsight.wpplugin.macromachine.operations.ApplyToMap.BreakpointButtonPanel;
import org.ironsight.wpplugin.macromachine.operations.ApplyToMap.BreakpointListener;
import org.ironsight.wpplugin.macromachine.operations.ApplyToMap.UserApplyActionCallback;
import org.ironsight.wpplugin.macromachine.operations.FileIO.ConflictResolveImportPolicy;
import org.ironsight.wpplugin.macromachine.operations.FileIO.ContainerIO;
import org.ironsight.wpplugin.macromachine.operations.FileIO.MacroExportPolicy;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.*;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.ironsight.wpplugin.macromachine.Gui.HelpDialog.getHelpButton;

public class MacroTreePanel extends JPanel {
    private final MacroContainer container;
    private final MappingActionContainer mappingContainer;
    private final MacroApplicator applyToMap;
    DefaultTreeModel treeModel;
    JTree tree;
    ISelectItemCallback onSelectAction;
    JPopupMenu popupMenu = new JPopupMenu();
    private BreakpointButtonPanel debuggerUI;
    private UserApplyActionCallback macroExecutionCallback;
    private String filterString = "";
    private long lastProgressUpdate = 0;
    private boolean macroIsCurrentlyExecuting;
    private boolean blockUpdates = false;
    private BreakpointListener treeStepper;

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
            return InputOutputProvider.INSTANCE.existsLayerWithId(((ILayerGetter) item).getLayerId());
        } else if (item instanceof IPositionValueGetter && item instanceof ILayerGetter) {
            return InputOutputProvider.INSTANCE.existsLayerWithId(((ILayerGetter) item).getLayerId());
        } else {
            return true;
        }
    }

    public UserApplyActionCallback getUserCallback() {
        return new UserApplyActionCallback(debuggerUI, ActionFilterIO.instance.isDebugMode());
    }

    private boolean verifyTreePathExists(MacroTreeNode node, Object[] path, int index, Object[] newPath) {
        newPath[index] = node;
        if (index >= path.length - 1)
            return node.equals(path[index]);
        else {
            for (MacroTreeNode child : node.getChildren()) {
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
        for (MacroTreeNode child : node.getChildren()) {
            findExpandedPaths(tree, child, expanded);
        }
        return expanded;
    }

    private List<TreePath> findAllPaths(JTree tree, MacroTreeNode node, LinkedList<TreePath> allPaths) {
        TreePath path = node.getPath();
        allPaths.add(path);
        for (MacroTreeNode child : node.getChildren()) {
            findAllPaths(tree, child, allPaths);
        }
        return allPaths;
    }

    private TreePath findPathEquivalent(Object[] oldPath, MacroTreeNode newNode, int index, Object[] newPath) {
        newPath[index] = newNode;
        if (index >= oldPath.length - 1)
            return new TreePath(newPath);
        else {
            for (MacroTreeNode child : newNode.getChildren()) {
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

    ArrayList<UUID> getSelectedUUIDs(boolean macros, boolean actions) {
        HashSet<UUID> seen = new HashSet<>();
        ArrayList<UUID> selectedUUIDs = new ArrayList<>();
        if (tree.getSelectionPaths() == null)
            return selectedUUIDs;

        TreePath[] selectedPaths = tree.getSelectionPaths();
        // Convert to list and sort based on tree order
        Arrays.sort(selectedPaths,
                Comparator.comparingInt(path -> Arrays.asList(tree.getPathForRow(0).getPath())
                        .indexOf(path.getLastPathComponent())));

        for (TreePath selected : selectedPaths) {
            System.out.println("selected path in tree:" + selected.getLastPathComponent());
            MacroTreeNode node = (MacroTreeNode) selected.getLastPathComponent();
            switch (node.payloadType) {
                case MACRO:
                    if (macros) {
                        selectedUUIDs.add(node.getMacro().getUid());
                        seen.add(node.getMacro().getUid());
                    }
                    break;
                case ACTION:
                    if (actions) {
                        selectedUUIDs.add(node.getAction().getUid());
                        seen.add(node.getAction().getUid());
                    }
                    break;
                default:
                    ; //nothing
            }
        }
        return selectedUUIDs;
    }

    private void onTreeItemRightClick(MacroTreeNode node, MouseEvent e) {

        System.out.println("source right clicked");
        popupMenu.show(e.getComponent(), e.getX(), e.getY());
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
        tree.setSelectionModel(new ToggleSelectionModel());
        tree.setRootVisible(false);
        tree.setCellRenderer(new DisplayUnitRenderer(MacroTreePanel::isValidItem));
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
            } else {
                onItemInTreeSelected(null, GlobalActionPanel.SELECTION_TPYE.NONE);
            }
        });

        tree.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {

            }

            @Override
            public void mousePressed(MouseEvent e) {

            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    TreePath selectedPath = tree.getPathForLocation(e.getX(), e.getY());
                    if (selectedPath == null || !tree.isPathSelected(selectedPath)) {
                        return; // No node at the click location OR not selected
                    }
                    MacroTreeNode node = (MacroTreeNode) selectedPath.getLastPathComponent();
                    if (node == null)
                        return;
                    onTreeItemRightClick(node, e);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }
        });
        JScrollPane scrollPane = new JScrollPane(tree,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        this.add(scrollPane, BorderLayout.CENTER);

        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.Y_AXIS));
        buttons.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton addButton = new JButton("Create macro");
        addButton.setToolTipText("Create a new, empty macro.");
        addButton.addActionListener(e -> this.onAddMacroPressed(false));


        JButton removeButton = new JButton("Delete");
        removeButton.setToolTipText("Delete all selected macros permanently");
        removeButton.addActionListener(e -> {
            blockUpdates = true;
            HashSet<UUID> deletedUUIDS = new HashSet<>(getSelectedUUIDs(true, true));
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

        JButton helpButton = getHelpButton("Global Tree View",
                "This view shows your global list of macros and actions. You can" + " " +
                        "expand the macros, actions and their values.\n" +
                        "Select a macro to open the macro-editor.\n" +
                        "Select an action from a macro or from the 'All actions' node to open the action editor.\n" +
                        "You can create and " +
                        "delete actions and macros in this view. All changes in the global list are directly saved to" +
                        " your " + "save-files. These are global and the same for all projects.\n" + " Press 'Apply'" +
                        " to " + "apply a macro as a global operation to " + "your map.");

        scrollPane.setPreferredSize(new Dimension(500, 600));

        JButton exportMacroButton = new JButton("Export");
        exportMacroButton.addActionListener(f -> onExportMacroPressed());

        JButton importMacroButton = new JButton("Import");
        importMacroButton.addActionListener(f -> onImportMacroPressed());

        JButton createNewFromButton = new JButton("Add to new macro");
        createNewFromButton.addActionListener(e -> onAddMacroPressed(true));

        JButton[] buttonArr = new JButton[]{
                removeButton,
                exportMacroButton,
                createNewFromButton
        };
        for (JButton b : buttonArr) {
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, b.getPreferredSize().height));
            buttons.add(b);
        }

        debuggerUI = new BreakpointButtonPanel(this::onApply, this::getTreeStepper);

        popupMenu.add(buttons);

        JPanel bottomButtons = new JPanel(new FlowLayout());
        bottomButtons.add(importMacroButton);
        bottomButtons.add(helpButton);
        bottomButtons.add(addButton);
        bottomButtons.add(debuggerUI);
        this.add(bottomButtons, BorderLayout.SOUTH);
        this.invalidate();
    }

    protected BreakpointListener getTreeStepper() {
        return treeStepper;
    }

    protected void setStepperToPath(TreePath path) {
        SwingUtilities.invokeLater(() -> {
            tree.setSelectionPaths(new TreePath[]{path});
        });
    }

    private void onAddMacroPressed(boolean useSelectedAsChildren) {
        UUID[] uidArr = new UUID[0];
        if (useSelectedAsChildren) {
            Collection<UUID> actionUids = getSelectedUUIDs(false, true);
            Collection<UUID> macroUids = getSelectedUUIDs(true, false);
            uidArr = new UUID[actionUids.size() + macroUids.size()];
            int idx = 0;
            for (UUID uuid : actionUids) {
                MappingAction clone = mappingContainer.addMapping().withValuesFrom(mappingContainer.queryById(uuid));
                mappingContainer.updateMapping(clone, MacroMachinePlugin::error);
                uidArr[idx++] = clone.getUid();
            }
            for (UUID macroId : macroUids) {
                uidArr[idx++] = macroId;
            }
        }

        Macro macro = container.addMapping().withUUIDs(uidArr);
        container.updateMapping(macro, MacroMachinePlugin::error);
        HashSet<UUID> set = new HashSet<>();
        set.add(macro.getUid());
        update(set);
    }

    private void onExportMacroPressed() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select a directory");
        fileChooser.setCurrentDirectory(new File(MacroMachineWindow.getDialog().getLastDirectoryPicked()));
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY); // Only allow directory selection
        fileChooser.setAcceptAllFileFilterUsed(false); // Optional: Disable the "All Files" filter

        int result = fileChooser.showOpenDialog(null); // Use null or a valid parent component
        MacroMachineWindow.getDialog().setLastDirectoryPicked(fileChooser.getCurrentDirectory().getPath());

        if (result == JFileChooser.APPROVE_OPTION) {
            assert (fileChooser.getSelectedFile() != null) : "user confirmed without selection?";
            String outputDir = fileChooser.getSelectedFile().getPath();
            for (UUID macroId : getSelectedUUIDs(true, false)) {
                Macro lastItem = container.queryById(macroId);
                MacroExportPolicy policy = new MacroExportPolicy(lastItem, MacroContainer.getInstance());
                String fileName = sanitizeFileName(lastItem.getName()) + ".macro";
                File macroFile = new File(outputDir + "/" + fileName);
                ContainerIO.exportToFile(MappingActionContainer.getInstance(),
                        MacroContainer.getInstance(),
                        macroFile,
                        policy,
                        MacroMachinePlugin::error, InputOutputProvider.INSTANCE);
            }
        }
    }

    private void onImportMacroPressed() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select a file");
        fileChooser.setCurrentDirectory(new File(MacroMachineWindow.getDialog().getLastDirectoryPicked()));
        fileChooser.setFileFilter(new FileNameExtensionFilter("Only MacroMachine files", "macro"));
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setMultiSelectionEnabled(true);
        int result = fileChooser.showOpenDialog(this);
        MacroMachineWindow.getDialog().setLastDirectoryPicked(fileChooser.getCurrentDirectory().getPath());
        if (result == JFileChooser.APPROVE_OPTION) {
            for (File selected : fileChooser.getSelectedFiles()) {
                ContainerIO.importFile(MappingActionContainer.getInstance(),
                        MacroContainer.getInstance(),
                        selected,
                        new ConflictResolveImportPolicy(MacroContainer.getInstance(),
                                MappingActionContainer.getInstance(), SwingUtilities.getWindowAncestor(this)),
                        GlobalActionPanel::ErrorPopUp, InputOutputProvider.INSTANCE
                );
            }
        }
    }

    private void onItemInTreeSelected(SaveableAction item, GlobalActionPanel.SELECTION_TPYE type) {
        onSelectAction.onSelect(item, type);
    }

    private void onApply(boolean isDebug) {
        ActionFilterIO.instance.setDebugMode(isDebug);
        if (macroIsCurrentlyExecuting)
            return;
        macroIsCurrentlyExecuting = true;
        ExecutorService executorService = Executors.newFixedThreadPool(1);

        // Submit a task to the ExecutorService
        final MacroTreePanel panel = this;
        TreePath selected = tree.getSelectionPath();
        if (selected == null || selected.getLastPathComponent() == null ||
                ((MacroTreeNode) selected.getLastPathComponent()).getPayloadType() !=
                        GlobalActionPanel.SELECTION_TPYE.MACRO) {
            return;
        }

        final Macro executingMacro = container.queryById(((MacroTreeNode) selected.getLastPathComponent()).getMacro()
                .getUid());
        if (executingMacro == null)
            return;
        //get macros
        //FIXME move getTreeStepper into constructor of callback? instead of wierd pingpong spgehtti
        macroExecutionCallback = panel.getUserCallback();

        treeStepper = new TreeDebugStepperUI((MacroTreeNode) selected.getLastPathComponent(),
                container,
                this.mappingContainer,
                this::setStepperToPath);
        executorService.submit(() -> {
            applyToMap.applyLayerAction(executingMacro, macroExecutionCallback);
            macroIsCurrentlyExecuting = false;
        });

        // Shutdown the ExecutorService
        executorService.shutdown();

    }

    static class ToggleSelectionModel extends DefaultTreeSelectionModel {
        @Override
        public void setSelectionPath(TreePath path) {
            if (isPathSelected(path)) {
                removeSelectionPath(path); // Deselect if already selected
            } else {
                super.setSelectionPath(path); // Select otherwise
            }
        }
    }
}

