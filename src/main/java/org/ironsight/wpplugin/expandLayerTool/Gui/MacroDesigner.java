package org.ironsight.wpplugin.expandLayerTool.Gui;

import org.ironsight.wpplugin.expandLayerTool.operations.LayerMapping;
import org.ironsight.wpplugin.expandLayerTool.operations.LayerMappingContainer;
import org.ironsight.wpplugin.expandLayerTool.operations.MappingMacro;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.*;
import java.util.function.Consumer;

import static org.ironsight.wpplugin.expandLayerTool.Gui.HelpDialog.getHelpButton;

public class MacroDesigner extends JPanel {
    private static final String helpString = "The macro designer allows you to design a macro.\n" +
            "A macro is a collection of actions, like a container. When a macro is applied to the map, it runs each " +
            "action in the specified order, one after the other. Think of it as a collection of simple global " +
            "operations that are bundeled together to achieve a more complex task.\n" +
            "You can add, remove, reorder and edit the actions of the macro here. Be aware that macros can share " +
            "actions, so if you edit one, you will also edit the other. Removed actions are not lost, they remain in " +
            "the global list of actions.";
    Consumer<MappingMacro> onSubmit;
    private MappingMacro macro;
    private JTextField name;
    private JTextArea description;
    private JTable table;
    private JButton addButton, removeButton, moveUpButton, moveDownButton, changeMappingButton;
    private JScrollPane scrollPane;
    private boolean isUpdating;

    MacroDesigner(Consumer<MappingMacro> onSubmit) {
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

        table = new JTable() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Disable editing
            }
        };
        table.setDefaultRenderer(Object.class, new MappingTableCellRenderer());
        scrollPane = new JScrollPane(table);
        this.add(scrollPane, BorderLayout.CENTER);

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(name);
        top.add(descPane);
        this.add(top, BorderLayout.NORTH);

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
        this.add(buttons, BorderLayout.SOUTH);

        prepareTableModel();
    }

    private void onAddMapping() {
        JDialog dialog = new SelectLayerMappingDialog(LayerMappingContainer.INSTANCE.queryAll(), f -> {
            int[] selection = table.getSelectedRows();
            if (table.getSelectedRows().length == 0) {
                selection = new int[]{table.getRowCount()-1};
            }

            //insert any mapping from container at tail of list
            ArrayList<UUID> uids = new ArrayList<>();
            Collections.addAll(uids, macro.mappingUids);

            int counter = 0;
            ArrayList<Integer> newSelection = new ArrayList<>(table.getSelectedRows().length);
            for (int row: selection) {
                int idx = row + counter + 1;
                uids.add(idx, f.getUid());
                newSelection.add(idx);
                counter++;
            }

            UUID[] ids = uids.toArray(new UUID[0]);
            setMacro(macro.withUUIDs(ids), true);
            table.clearSelection();
            for (int row: newSelection) {
                table.addRowSelectionInterval(row, row);
            }
        });
        dialog.setModal(true);
        dialog.setVisible(true);
    }

    private void onMoveUpMapping() {
        if (table.getSelectedRows().length == 0)
            return;
        int anchorRow = table.getSelectedRows()[0];
        if (anchorRow > 0 && anchorRow < table.getRowCount()) {
            UUID[] ids = macro.mappingUids.clone();
            for (int selectedRow: table.getSelectedRows()) {
                ids[selectedRow - 1] = macro.mappingUids[selectedRow];
                ids[selectedRow] = macro.mappingUids[selectedRow - 1];
            }

            shiftRowSelection(table.getSelectedRows(),-1);
            setMacro(macro.withUUIDs(ids), true);
            scrollPane.scrollRectToVisible(table.getCellRect(table.getSelectedRows()[0]
                    , 0,
                    true));
            System.out.println("move mapping up to " + anchorRow);
        }
    }

    private void shiftRowSelection(int[] selectedRows, int shift) {
        table.clearSelection();
        for (int row : Arrays.stream(selectedRows).map(i -> i+shift).toArray()) {
            if (row < 0 || row >= table.getRowCount()) {
                continue;
            }
            table.addRowSelectionInterval(row, row);
        }
    }

    private void onMoveDownMapping() {
        if (table.getSelectedRows().length == 0)
            return;
        int anchorRow = table.getSelectedRows()[table.getSelectedRows().length - 1];
        if (anchorRow >= 0 && anchorRow < table.getRowCount() - 1) {
            UUID[] ids = macro.mappingUids.clone();
            for (int selectedRow: table.getSelectedRows()) {
                ids[selectedRow + 1] = macro.mappingUids[selectedRow];
                ids[selectedRow] = macro.mappingUids[selectedRow + 1];
            }

            shiftRowSelection(table.getSelectedRows(),+1);
            setMacro(macro.withUUIDs(ids), true);
            //scroll to bottom selected row
            scrollPane.scrollRectToVisible(table.getCellRect(table.getSelectedRows()[table.getSelectedRows().length-1]
                    , 0,
                    true));
            System.out.println("move mapping down to " + anchorRow);
        }
    }

    private void onDeleteMapping() {
        HashSet<Integer> toBeRemoved = new HashSet<>();
        for (int row : this.table.getSelectedRows()) {
            toBeRemoved.add(row);
        }

        ArrayList<UUID> newUids = new ArrayList<>();
        for (int i = 0; i < macro.mappingUids.length; i++) {
            if (toBeRemoved.contains(i)) continue;
            newUids.add(macro.mappingUids[i]);
        }

        setMacro(macro.withUUIDs(newUids.toArray(new UUID[0])), true);
    }

    private void onChangeMapping() {
        JDialog dialog = new SelectLayerMappingDialog(LayerMappingContainer.INSTANCE.queryAll(), f -> {
            UUID[] newIds = macro.mappingUids.clone();
            for (int row: this.table.getSelectedRows()) {
                newIds[row] = f.getUid();
            }
            setMacro(macro.withUUIDs(newIds), true);
        });
        dialog.setModal(true);
        dialog.setVisible(true);
    }

    private void prepareTableModel() {
        System.out.println("RESET TABLE MODEL");
        DefaultTableModel model = new DefaultTableModel();
        Object[] columns = new Object[]{"Action"};
        Object[][] data = new Object[0][];
        model.setDataVector(data, columns);
        table.setModel(model);
    }

    private void updateComponents() {
        System.out.println(getClass().getSimpleName() + ": update components");

        name.setText(macro.getName());
        description.setText(macro.getDescription());

        while (table.getModel().getRowCount() < macro.mappingUids.length) {
            ((DefaultTableModel)table.getModel()).addRow(new Object[1]);
        }
        while (table.getModel().getRowCount() > macro.mappingUids.length) {
            ((DefaultTableModel)table.getModel()).removeRow(table.getRowCount() - 1);
        }

        int row = 0;
        for (UUID id : macro.mappingUids) {
            LayerMapping m = LayerMappingContainer.INSTANCE.queryById(id);
            table.setValueAt(m, row++, 0);
        }

        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || isUpdating || Arrays.equals(selectedRows,table.getSelectedRows())) return;
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

    private int[] selectedRows = new int[0];
    public void setMacro(MappingMacro macro, boolean forceUpdate) {
        assert macro != null;
        if (!forceUpdate && this.macro != null && this.macro.equals(macro)) return; //dont update if nothing changed
        isUpdating = true;
        System.out.println(getClass().getSimpleName() + ": set macro");

        this.macro = macro;
        updateComponents();
        isUpdating = false;
    }

}
