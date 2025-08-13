package org.ironsight.wpplugin.macromachine.Gui;

import org.ironsight.wpplugin.macromachine.Gui.ItemPicker.DisplayUnitPickerDialog;
import org.ironsight.wpplugin.macromachine.Gui.ItemPicker.PickerFilterOption;
import org.ironsight.wpplugin.macromachine.operations.*;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.ironsight.wpplugin.macromachine.Gui.HelpDialog.getHelpButton;
import static org.ironsight.wpplugin.macromachine.operations.ValueProviders.IMappingValue.getAllPointsForDiscreteIO;

public class MacroDesigner extends JPanel {
    Consumer<Macro> onSubmit;
    PickerFilterOption macroFilter = new PickerFilterOption<>("macros", "show macros") {
        @Override
        public boolean block(Object item) {
            return item instanceof Macro;
        }
    };
    UUIDFilterOptions customActionsFilter = new UUIDFilterOptions("custom", "show user created actions");
    UUIDFilterOptions defaultFilter = new UUIDFilterOptions("defaults", "show default actions");
    private Macro macro;
    private JTextField name;
    private JTextArea description;
    private JTable table;
    private JButton addButton, removeButton, moveUpButton, moveDownButton;
    private JScrollPane scrollPane;
    private boolean isUpdating;
    private int[] selectedRows = new int[0];

    MacroDesigner(Consumer<Macro> onSubmit) {
        this.onSubmit = onSubmit;
        init();

        this.defaultFilter.setActive(true);
        this.customActionsFilter.setActive(false);
        this.macroFilter.setActive(false);
    }

    public static Collection<MappingAction> getDefaultFiltersAndEmptyAction() {
        LinkedList<MappingAction> items = new LinkedList<>();
        items.add(MappingAction.getNewEmptyAction());
        items.add(new MappingAction(new AlwaysIO(),
                ActionFilterIO.instance,
                new MappingPoint[]{
                        new MappingPoint(0, ActionFilterIO.PASS_VALUE)
                },
                ActionType.SET,
                "Filter: Reset",
                "Default filter: allow all blocks",
                null
        ));
        items.add(new MappingAction(new WaterDepthProvider(),
                ActionFilterIO.instance,
                new MappingPoint[]{
                        new MappingPoint(0, ActionFilterIO.BLOCK_VALUE),
                        new MappingPoint(1, ActionFilterIO.PASS_VALUE)
                },
                ActionType.LIMIT_TO,
                "Filter: Only On Water",
                "Default filter: block all blocks that are not below waterlevel",
                null
        ));
        items.add(new MappingAction(new WaterDepthProvider(),
                ActionFilterIO.instance,
                new MappingPoint[]{
                        new MappingPoint(0, ActionFilterIO.PASS_VALUE),
                        new MappingPoint(1, ActionFilterIO.BLOCK_VALUE)
                },
                ActionType.LIMIT_TO,
                "Filter: Only On Land",
                "Default filter: block all blocks that are not above waterlevel",
                null
        ));
        items.add(new MappingAction(new TerrainHeightIO(-64, 319),
                ActionFilterIO.instance,
                new MappingPoint[]{
                        new MappingPoint(73, ActionFilterIO.PASS_VALUE),
                        new MappingPoint(319, ActionFilterIO.BLOCK_VALUE)
                },
                ActionType.LIMIT_TO,
                "Filter: Below height",
                "Default filter: block all blocks that are above this level",
                null
        ));
        items.add(new MappingAction(new TerrainHeightIO(-64, 319),
                ActionFilterIO.instance,
                new MappingPoint[]{
                        new MappingPoint(73, ActionFilterIO.BLOCK_VALUE),
                        new MappingPoint(319, ActionFilterIO.PASS_VALUE)
                },
                ActionType.LIMIT_TO,
                "Filter: Above height",
                "Default filter: block all blocks that below this level",
                null
        ));
        items.add(new MappingAction(new SlopeProvider(),
                ActionFilterIO.instance,
                new MappingPoint[]{
                        new MappingPoint(45, ActionFilterIO.BLOCK_VALUE),
                        new MappingPoint(90, ActionFilterIO.PASS_VALUE)
                },
                ActionType.LIMIT_TO,
                "Filter: Above degrees",
                "Default filter: block all blocks that are flatter than this angle",
                null
        ));
        items.add(new MappingAction(new SlopeProvider(),
                ActionFilterIO.instance,
                new MappingPoint[]{
                        new MappingPoint(45, ActionFilterIO.PASS_VALUE),
                        new MappingPoint(90, ActionFilterIO.BLOCK_VALUE)
                },
                ActionType.LIMIT_TO,
                "Filter: Below degrees",
                "Default filter: block all blocks that are steeper than this angle",
                null
        ));
        MappingPoint[] allBiomesBlocked = getAllPointsForDiscreteIO(new VanillaBiomeProvider(),
                ActionFilterIO.BLOCK_VALUE);
        allBiomesBlocked[1] = new MappingPoint(1 /*plains*/, ActionFilterIO.PASS_VALUE);
        items.add(new MappingAction(new VanillaBiomeProvider(),
                ActionFilterIO.instance,
                allBiomesBlocked,
                ActionType.LIMIT_TO,
                "Filter: Only on biome",
                "Default filter: block all blocks that are not this biome type",
                null
        ));
        MappingPoint[] allBiomesPass = getAllPointsForDiscreteIO(new VanillaBiomeProvider(),
                ActionFilterIO.PASS_VALUE);
        allBiomesBlocked[1] = new MappingPoint(1 /*plains*/, ActionFilterIO.BLOCK_VALUE);
        items.add(new MappingAction(new VanillaBiomeProvider(),
                ActionFilterIO.instance,
                allBiomesPass,
                ActionType.LIMIT_TO,
                "Filter: Except on biome",
                "Default filter: block all blocks that are this biome type",
                null
        ));
        {
            MappingPoint[] allTerrainsPass = getAllPointsForDiscreteIO(new TerrainProvider(),
                    ActionFilterIO.BLOCK_VALUE);
            allTerrainsPass[0] = new MappingPoint(0 /*grass*/, ActionFilterIO.PASS_VALUE);
            items.add(new MappingAction(new TerrainProvider(),
                    ActionFilterIO.instance,
                    allTerrainsPass,
                    ActionType.LIMIT_TO,
                    "Filter: Only on terrain",
                    "Default filter: block all blocks that are this terrain type",
                    null
            ));
        }
        {
            MappingPoint[] allTerrains = getAllPointsForDiscreteIO(new TerrainProvider(),
                    ActionFilterIO.PASS_VALUE);
            allTerrains[0] = new MappingPoint(0 /*grass*/, ActionFilterIO.BLOCK_VALUE);
            items.add(new MappingAction(new TerrainProvider(),
                    ActionFilterIO.instance,
                    allTerrains,
                    ActionType.LIMIT_TO,
                    "Filter: Except on terrain",
                    "Default filter: block all blocks that are not this terrain type",
                    null
            ));
        }
        items.add(new MappingAction(new SelectionIO(),
                ActionFilterIO.instance,
                new MappingPoint[]{
                        new MappingPoint(0, ActionFilterIO.BLOCK_VALUE),
                        new MappingPoint(1, ActionFilterIO.PASS_VALUE),
                },
                ActionType.LIMIT_TO,
                "Filter: Inside Selection",
                "Default filter: block all blocks that are not in selection.",
                null
        ));

        items.add(new MappingAction(new SelectionIO(),
                ActionFilterIO.instance,
                new MappingPoint[]{
                        new MappingPoint(0, ActionFilterIO.PASS_VALUE),
                        new MappingPoint(1, ActionFilterIO.BLOCK_VALUE),
                },
                ActionType.LIMIT_TO,
                "Filter: Outside Selection",
                "Default filter: block all blocks that are  in selection.",
                null
        ));
        items.add(new MappingAction(new AlwaysIO(),
                ActionFilterIO.instance,
                new MappingPoint[]{
                        new MappingPoint(0, ActionFilterIO.PASS_VALUE),
                },
                ActionType.SET,
                "Filter: Reset, allow all",
                "Default filter: block nothing.",
                null
        ));

        MappingPoint[] allAnnotationsBlock = getAllPointsForDiscreteIO(new AnnotationSetter(),
                ActionFilterIO.BLOCK_VALUE);
        allAnnotationsBlock[9] = new MappingPoint(9 /*cyan*/, ActionFilterIO.PASS_VALUE);
        items.add(new MappingAction(new AnnotationSetter(),
                ActionFilterIO.instance,
                allAnnotationsBlock,
                ActionType.LIMIT_TO,
                "Filter: Only On Annotations Cyan",
                "Default filter: block all blocks that are not cyan annotated.",
                null
        ));
        items.add(new MappingAction(new AlwaysIO(),
                new TerrainProvider(),
                new MappingPoint[]{
                        new MappingPoint(0, 0)
                },
                ActionType.SET,
                "Apply: Grass",
                "Default action: apply grass to all blocks that have passed the filter",
                null
        ));
        return items;
    }

    private JPopupMenu createPopupMenu(int row, int column, int[] selectedRows) {
        //row is index of action
        UUID id = macro.getExecutionUUIDs()[row];
        boolean active = macro.getActiveActions()[row];

        JPopupMenu menu = new JPopupMenu();

        {
            JButton toggleActiveButton = new JButton();
            toggleActiveButton.setText(macro.getActiveActions()[row] ? "disable" : "enable");
            toggleActiveButton.setToolTipText("disabled items will be skipped when the macro is executed.");
            toggleActiveButton.addActionListener(e -> onToggleEnableItem(row, toggleActiveButton));
            menu.add(toggleActiveButton);
        }

        {
            JButton nestToMacroButton = new JButton("nest into macro");
            nestToMacroButton.setToolTipText("move all selected items into a nested macro.");
            nestToMacroButton.addActionListener(l -> onMoveToNestedMacro(selectedRows));
            menu.add(nestToMacroButton);
        }

        return menu;
    }

    private void onMoveToNestedMacro(int[] selectedRows) {
        // ask user for macro name
        String input = JOptionPane.showInputDialog(
                null,
                "New macro name:",
                "Input Needed",
                JOptionPane.PLAIN_MESSAGE
        );

        if (input == null) {
            return;
        }

        MappingActionContainer actionContainer = MappingActionContainer.getInstance();
        UUID[] selectedUUIDs =
                Arrays.stream(selectedRows)
                        .mapToObj(row -> {
                            UUID original = macro.getExecutionUUIDs()[row];
                            if (!actionContainer.queryContains(original))
                                return original; // its not a mapping and doesnt need cloning.
                            MappingAction clone =
                                    actionContainer.addMapping().withValuesFrom(actionContainer.queryById(original));
                            actionContainer.updateMapping(clone, GlobalActionPanel::ErrorPopUp);
                            return clone.getUid(); //clone action and return clones UUID
                        })
                        .toArray(UUID[]::new);

        Macro nested = MacroContainer.getInstance().addMapping().withName(input).withUUIDs(selectedUUIDs);
        MacroContainer.getInstance().updateMapping(nested, GlobalActionPanel::ErrorPopUp);

        ArrayList<UUID> remainingUUIDs = new ArrayList<>();
        ArrayList<Boolean> activeItems = new ArrayList<>();
        {
            remainingUUIDs.addAll(Arrays.asList(macro.getExecutionUUIDs()));
            for (boolean active : macro.getActiveActions())
                activeItems.add(active);
            HashSet<Integer> removedRows = new HashSet<>();
            for (int row : selectedRows) {
                removedRows.add(row);
            }

            // delete rows from the back
            for (int row = remainingUUIDs.size() - 1; row >= 0; row--) {
                if (removedRows.contains(row)) {
                    remainingUUIDs.remove(row);
                    activeItems.remove(row);
                }
            }
            remainingUUIDs.add(selectedRows[0], nested.getUid());
            activeItems.add(selectedRows[0], true);
        }

        boolean[] active = new boolean[activeItems.size()];
        int i = 0;
        for (boolean a : activeItems)
            active[i++] = a;
        Macro updatedSelf = macro.withUUIDs(remainingUUIDs.toArray(UUID[]::new), active);
        setMacro(updatedSelf, true);
    }

    private void onToggleEnableItem(int row, JButton button) {
        boolean isTargetRowActive = macro.getActiveActions()[row];
        boolean[] activeState = macro.getActiveActions();
        for (int idx : selectedRows) {
            activeState[idx] = !isTargetRowActive;
        }
        Macro m = macro.withUUIDs(macro.getExecutionUUIDs(), activeState);
        setMacro(m, true);
        button.setText(!isTargetRowActive ? "disable" : "enable");
    }

    private void init() {
        this.setLayout(new BorderLayout());

        name = new JTextField("Name goes here");
        name.setEditable(true);
        name.setFont(LayerMappingTopPanel.header1Font);
        name.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                setMacro(macro.withName(name.getText()), false);
            }
        });

        description = new JTextArea("Description goes here");
        description.setEditable(true);
        description.setFont(LayerMappingTopPanel.header2Font);
        description.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                setMacro(macro.withDescription(description.getText()), false);
            }
        });

        // Set preferred size to limit to 10 lines
        int lineCount = 6; // Number of lines
        FontMetrics metrics = description.getFontMetrics(description.getFont());
        int lineHeight = metrics.getHeight();
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        JScrollPane descPane = new JScrollPane(description);
        descPane.setPreferredSize(new Dimension(0, lineHeight * lineCount));

        JPanel editorPanel = new JPanel(new BorderLayout());
        table = new JTable() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Disable editing
            }
        };
        table.setDefaultRenderer(Object.class, new DisplayUnitRenderer(MacroTreePanel::isValidItem));
        //FIXME add actual
        // check if
        // action is usable
        scrollPane = new JScrollPane(table);
        editorPanel.add(scrollPane, BorderLayout.CENTER);


        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) {
                    int row = table.rowAtPoint(e.getPoint());
                    int column = table.columnAtPoint(e.getPoint());

                    // Check if a valid cell is clicked
                    if (row >= 0 && column >= 0) {
                        table.addRowSelectionInterval(row, row); // Select the clicked row
                        table.addColumnSelectionInterval(column, column);

                        // Show the popup menu
                        JPopupMenu popupMenu = createPopupMenu(row, column, table.getSelectedRows());
                        popupMenu.show(table, e.getX(), e.getY());
                    }
                }
            }
        });

        JPanel nameAndDescriptionPanel = new JPanel();
        nameAndDescriptionPanel.setLayout(new BoxLayout(nameAndDescriptionPanel, BoxLayout.Y_AXIS));
        nameAndDescriptionPanel.add(name);
        nameAndDescriptionPanel.add(descPane);
        this.add(nameAndDescriptionPanel, BorderLayout.NORTH);

        JPanel buttons = new JPanel(new FlowLayout());
        addButton = new JButton("Add");
        addButton.setToolTipText("Add an existing action to this macro below the last selected one.");
        addButton.addActionListener(e -> onAddMapping());
        buttons.add(addButton);

        removeButton = new JButton("Remove");
        removeButton.setToolTipText("Remove an existing action from this macro. Action is not permanently deleted and" +
                " still exists in global list.");
        removeButton.addActionListener(e -> onDeleteMapping());
        buttons.add(removeButton);

        moveUpButton = new JButton("Move Up");
        moveUpButton.setToolTipText("Move up the selected action in the order of execution.");
        moveUpButton.addActionListener(e -> onMoveUpMapping());
        buttons.add(moveUpButton);

        moveDownButton = new JButton("Move Down");
        moveDownButton.setToolTipText("Move down the selected action in the order of execution.");
        moveDownButton.addActionListener(e -> onMoveDownMapping());
        buttons.add(moveDownButton);

        JButton submitButton = new JButton("Save");
        submitButton.setToolTipText("submit macro and save changes to global list.");
        submitButton.addActionListener(e -> onSubmit.accept(this.macro));
        buttons.add(submitButton);

        buttons.add(getHelpButton("Macro Editor",
                "In the macro editor, you define which actions are executed and in " +
                        "which order. The top-most action is run first, then the next one and so on. All actions in a" +
                        " macro " + "are always executed when the macro is executed.\n" +
                        "A macro is a collection of actions, similar to a group of global operations. It has a name " +
                        "and " + "description and can be reused in any project.\n" +
                        "Use the save button to save your changes to the global list."));
        editorPanel.add(buttons, BorderLayout.SOUTH);

        // Create a JTabbedPane
        JTabbedPane tabbedPane = new JTabbedPane();

        JPanel panel3 = new JPanel();
        panel3.add(new JLabel("This is the content of Tab 3"));

        // Add tabs to the JTabbedPane
        tabbedPane.addTab("Actions", editorPanel);

        this.add(tabbedPane, BorderLayout.CENTER);

        prepareTableModel();
    }

    private void onAddMapping() {
        ArrayList<IDisplayUnit> macrosAndActions = new ArrayList<>();
        macrosAndActions.addAll(MappingActionContainer.getInstance().queryAll());
        macrosAndActions.addAll(MacroContainer.getInstance().queryAll());
        Collection<MappingAction> defaultActions = getDefaultFiltersAndEmptyAction();


        defaultFilter.setPassUUIDs(defaultActions.stream()
                .map(MappingAction::getUid)
                .collect(Collectors.toCollection(HashSet::new)));

        customActionsFilter.setPassUUIDs(MappingActionContainer.getInstance().queryAll().stream()
                .map(MappingAction::getUid)
                .collect(Collectors.toCollection(HashSet::new)));

        JDialog dialog =
                new DisplayUnitPickerDialog(macrosAndActions, this::onPickerSelection, new ArrayList<>(defaultActions),
                        this, macroFilter, defaultFilter, customActionsFilter);
        dialog.setModal(true);
        dialog.setVisible(true);
    }

    private void onPickerSelection(IDisplayUnit selected) {
        Macro macro = this.macro;
        if (selected instanceof MappingAction)
            selected = ((MappingAction) selected).deepCopy();
        ArrayList<Integer> newSelection = new ArrayList<>();
        Macro newMacro = Macro.insertSaveableActionToList(macro.clone(), (SaveableAction) selected,
                () -> MappingActionContainer.getInstance().addMapping(),
                a -> MappingActionContainer.getInstance().updateMapping(a, GlobalActionPanel::ErrorPopUp),
                table.getSelectedRows(), newSelection);
        setMacro(newMacro, true);
        assert this.macro.equals(newMacro) : "macro was added an action, but action is not " +
                "present after gui update";
        table.clearSelection();
        for (int row : newSelection) {
            table.addRowSelectionInterval(row, row);
        }
    }

    private void onMoveUpMapping() {
        if (table.getSelectedRows().length == 0) return;
        int anchorRow = table.getSelectedRows()[0];
        if (anchorRow > 0 && anchorRow < table.getRowCount()) {
            Macro macro = this.macro;
            UUID[] ids = macro.executionUUIDs.clone();
            boolean[] active = macro.getActiveActions();
            for (int selectedRow : table.getSelectedRows()) {
                ids[selectedRow - 1] = macro.executionUUIDs[selectedRow];
                active[selectedRow - 1] = macro.getActiveActions()[selectedRow];
                ids[selectedRow] = macro.executionUUIDs[selectedRow - 1];
                active[selectedRow] = macro.getActiveActions()[selectedRow - 1];
            }

            shiftRowSelection(table.getSelectedRows(), -1);
            setMacro(macro.withUUIDs(ids, active), true);
            scrollPane.scrollRectToVisible(table.getCellRect(table.getSelectedRows()[0], 0, true));
        }
    }

    private void shiftRowSelection(int[] selectedRows, int shift) {
        table.clearSelection();
        for (int row : Arrays.stream(selectedRows).map(i -> i + shift).toArray()) {
            if (row < 0 || row >= table.getRowCount()) {
                continue;
            }
            table.addRowSelectionInterval(row, row);
        }
    }

    private void onMoveDownMapping() {
        if (table.getSelectedRows().length == 0) return;
        int anchorRow = table.getSelectedRows()[table.getSelectedRows().length - 1];
        if (anchorRow >= 0 && anchorRow < table.getRowCount() - 1) {
            UUID[] ids = macro.executionUUIDs.clone();
            boolean[] active = macro.getActiveActions().clone();
            for (int selectedRow : table.getSelectedRows()) {
                ids[selectedRow + 1] = macro.executionUUIDs[selectedRow];
                ids[selectedRow] = macro.executionUUIDs[selectedRow + 1];
                active[selectedRow + 1] = macro.getActiveActions()[selectedRow];
                active[selectedRow] = macro.getActiveActions()[selectedRow + 1];
            }

            shiftRowSelection(table.getSelectedRows(), +1);
            setMacro(macro.withUUIDs(ids, active), true);
            //scroll to bottom selected row
            scrollPane.scrollRectToVisible(table.getCellRect(table.getSelectedRows()[table.getSelectedRows().length -
                    1], 0, true));
        }
    }

    private void onDeleteMapping() {
        HashSet<Integer> toBeRemoved = new HashSet<>();
        for (int row : this.table.getSelectedRows()) {
            toBeRemoved.add(row);
        }
        Macro macro = this.macro;
        for (int i = macro.executionUUIDs.length - 1; i >= 0; i--) {
            if (!toBeRemoved.contains(i)) continue;
            macro = macro.withRemovedItem(i);
        }
        setMacro(macro, true);
    }

    private void prepareTableModel() {
        DefaultTableModel model = new DefaultTableModel();
        Object[] columns = new Object[]{"Action"};
        Object[][] data = new Object[0][];
        model.setDataVector(data, columns);
        table.setModel(model);
    }

    private void updateComponents() {
        name.setText(macro.getName());
        description.setText(macro.getDescription());

        while (table.getModel().getRowCount() < macro.executionUUIDs.length) {
            ((DefaultTableModel) table.getModel()).addRow(new Object[1]);
        }
        while (table.getModel().getRowCount() > macro.executionUUIDs.length) {
            ((DefaultTableModel) table.getModel()).removeRow(table.getRowCount() - 1);
        }

        int row = 0;
        for (UUID id : macro.executionUUIDs) {
            SaveableAction m = MappingActionContainer.getInstance().queryById(id);
            if (m == null)
                m = MacroContainer.getInstance().queryById(id);
            m.setActive(macro.getActiveActions()[row]);
            table.setValueAt(m, row++, 0);
        }

        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || isUpdating || Arrays.equals(selectedRows, table.getSelectedRows())) return;
            selectedRows = table.getSelectedRows();
        });

        final int rowCount = table.getRowCount();
        final int colCount = table.getColumnCount();
        for (int ix = 0; ix < rowCount; ix++) {
            int maxHeight = 0;
            for (int j = 0; j < colCount; j++) {
                final TableCellRenderer renderer = table.getCellRenderer(ix, j);
                maxHeight = Math.max(maxHeight, table.prepareRenderer(renderer, ix, j).getPreferredSize().height);
            }
            table.setRowHeight(ix, maxHeight);
        }
        invalidate();
        repaint();
    }

    public void setMacro(Macro macro, boolean forceUpdate) {
        assert macro != null;
        if (!forceUpdate && this.macro != null && this.macro.equals(macro)) return; //dont update if nothing changed
        isUpdating = true;

        this.macro = macro;
        updateComponents();
        isUpdating = false;
    }

    private class UUIDFilterOptions extends PickerFilterOption {
        Set<UUID> matchingIds = new HashSet<>();

        public UUIDFilterOptions(String displayName, String tooltip) {
            super(displayName, tooltip);
        }

        public void setPassUUIDs(Set<UUID> set) {
            this.matchingIds = set;
        }

        @Override
        public boolean block(Object item) {
            return item instanceof SaveableAction && matchingIds.contains(((SaveableAction) item).getUid());
        }
    }

}
