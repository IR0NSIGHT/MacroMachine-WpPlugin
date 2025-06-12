package org.ironsight.wpplugin.macromachine.Gui;

import org.ironsight.wpplugin.macromachine.MacroMachinePlugin;
import org.ironsight.wpplugin.macromachine.operations.*;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.*;
import java.util.function.Consumer;

import static org.ironsight.wpplugin.macromachine.Gui.HelpDialog.getHelpButton;
import static org.ironsight.wpplugin.macromachine.operations.ValueProviders.IMappingValue.getAllPointsForDiscreteIO;

public class MacroDesigner extends JPanel {
    Consumer<Macro> onSubmit;
    private Macro macro;
    private JTextField name;
    private JTextArea description;
    private JTable table;
    private JButton addButton, removeButton, moveUpButton, moveDownButton, changeMappingButton;
    private JScrollPane scrollPane;
    private boolean isUpdating;
    private int[] selectedRows = new int[0];

    MacroDesigner(Consumer<Macro> onSubmit) {
        this.onSubmit = onSubmit;
        init();
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
        table.setDefaultRenderer(Object.class, new SaveableActionRenderer());
        scrollPane = new JScrollPane(table);
        editorPanel.add(scrollPane, BorderLayout.CENTER);


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

        changeMappingButton = new JButton("Change Mapping");
        changeMappingButton.setToolTipText("Change action to another one from the global list.");
        changeMappingButton.addActionListener(e -> onChangeMapping());
        buttons.add(changeMappingButton);

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

    private Collection<SaveableAction> getDefaultFiltersAndEmptyAction() {
        LinkedList<SaveableAction> items = new LinkedList<>();
        items.add(MappingAction.getNewEmptyAction());
        items.add(new MappingAction(new WaterDepthProvider(),
                ActionFilterIO.instance,
                new MappingPoint[]{
                        new MappingPoint(0, ActionFilterIO.BLOCK_VALUE),
                        new MappingPoint(0, ActionFilterIO.PASS_VALUE)
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
        items.add(new MappingAction(new TerrainHeightIO(-64,319),
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
        items.add(new MappingAction(new TerrainHeightIO(-64,319),
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
        return items;
    }

    private void onAddMapping() {
        ArrayList<SaveableAction> macrosAndActions = new ArrayList<>();
        macrosAndActions.addAll(MappingActionContainer.getInstance().queryAll());
        macrosAndActions.addAll(MacroContainer.getInstance().queryAll());
        JDialog dialog = new SaveableActionPickerDialog(macrosAndActions, selected -> {
            Macro macro = this.macro;

            ArrayList<Integer> newSelection = new ArrayList<>();
            Macro newMacro = Macro.insertSaveableActionToList(macro.clone(), selected,
                    () -> MappingActionContainer.getInstance().addMapping(),
                    a -> MappingActionContainer.getInstance().updateMapping(a, MacroMachinePlugin::error),
                    table.getSelectedRows(), newSelection);
            setMacro(newMacro, true);
            assert this.macro.equals(newMacro) : "macro was added an action, but action is not " +
                    "present after gui update";
            table.clearSelection();
            for (int row : newSelection) {
                table.addRowSelectionInterval(row, row);
            }
        }, getDefaultFiltersAndEmptyAction(), this);
        dialog.setModal(true);
        dialog.setVisible(true);
    }

    private void onMoveUpMapping() {
        if (table.getSelectedRows().length == 0) return;
        int anchorRow = table.getSelectedRows()[0];
        if (anchorRow > 0 && anchorRow < table.getRowCount()) {
            UUID[] ids = macro.executionUUIDs.clone();
            for (int selectedRow : table.getSelectedRows()) {
                ids[selectedRow - 1] = macro.executionUUIDs[selectedRow];
                ids[selectedRow] = macro.executionUUIDs[selectedRow - 1];
            }

            shiftRowSelection(table.getSelectedRows(), -1);
            setMacro(macro.withUUIDs(ids), true);
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
            for (int selectedRow : table.getSelectedRows()) {
                ids[selectedRow + 1] = macro.executionUUIDs[selectedRow];
                ids[selectedRow] = macro.executionUUIDs[selectedRow + 1];
            }

            shiftRowSelection(table.getSelectedRows(), +1);
            setMacro(macro.withUUIDs(ids), true);
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

        ArrayList<UUID> newUids = new ArrayList<>();
        for (int i = 0; i < macro.executionUUIDs.length; i++) {
            if (toBeRemoved.contains(i)) continue;
            newUids.add(macro.executionUUIDs[i]);
        }

        setMacro(macro.withUUIDs(newUids.toArray(new UUID[0])), true);
    }

    private void onChangeMapping() {
        ArrayList<SaveableAction> macrosAndActions = new ArrayList<>();
        macrosAndActions.addAll(MappingActionContainer.getInstance().queryAll());
        macrosAndActions.addAll(MacroContainer.getInstance().queryAll());
        JDialog dialog = new SaveableActionPickerDialog(macrosAndActions, selected -> {
            if (selected instanceof Macro) {
                setMacro(macro.withReplacedUUIDs(this.table.getSelectedRows(), selected.getUid()), true);
            } else {
                if (selected.getUid() == null)
                    selected = MappingActionContainer.getInstance().addMapping();
                Macro temp = this.macro;
                for (int targetIdx : this.table.getSelectedRows()) {
                    MappingAction action = MappingActionContainer.getInstance().addMapping();
                    MappingActionContainer.getInstance().updateMapping(action.withValuesFrom((MappingAction) selected),
                            MacroMachinePlugin::error);
                    temp = temp.withReplacedUUIDs(new int[]{targetIdx}, action.getUid());
                }
                setMacro(temp, true);
            }


        }, getDefaultFiltersAndEmptyAction(), this);
        dialog.setModal(true);
        dialog.setVisible(true);
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

}
