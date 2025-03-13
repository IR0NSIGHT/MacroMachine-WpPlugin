package org.ironsight.wpplugin.expandLayerTool.Gui;

import org.ironsight.wpplugin.expandLayerTool.operations.LayerMapping;
import org.ironsight.wpplugin.expandLayerTool.operations.LayerMappingContainer;
import org.ironsight.wpplugin.expandLayerTool.operations.MappingMacro;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.*;
import java.util.List;
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
    private int selectedRow;
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
        };;
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

        buttons.add(getHelpButton("Macro Editor","In the macro editor, you define which actions are executed and in " +
                "which order. The top-most action is run first, then the next one and so on. All actions in a macro " +
                "are always executed when the macro is executed.\n" +
                "A macro is a collection of actions, similar to a group of global operations. It has a name and " +
                "description and can be reused in any project.\n" +
                "Use the save button to save your changes to the global list."));
        this.add(buttons, BorderLayout.SOUTH);
    }

    private void onAddMapping() {
        JDialog dialog = new SelectLayerMappingDialog(LayerMappingContainer.INSTANCE.queryAll(), f -> {
            //insert any mapping from container at tail of list
            ArrayList<UUID> uids = new ArrayList<>();
            Collections.addAll(uids, macro.mappingUids);

            if (selectedRow <0 || selectedRow >= uids.size()) {
                selectedRow = uids.size() - 1;
            }
            uids.add(selectedRow+1, f.getUid());
            selectedRow = selectedRow + 1;

            UUID[] ids = uids.toArray(new UUID[0]);
            MappingMacro mappingMacro = macro.withUUIDs(ids);
            setMacro(mappingMacro, true);
            setMacro(macro.withUUIDs(ids), true);
        });
        dialog.setModal(true);
        dialog.setVisible(true);
    }

    private void onMoveUpMapping() {
        if (selectedRow > 0 && selectedRow < table.getRowCount()) {
            UUID[] ids = macro.mappingUids.clone();
            ids[selectedRow - 1] = macro.mappingUids[selectedRow];
            ids[selectedRow] = macro.mappingUids[selectedRow - 1];
            selectedRow = selectedRow - 1;
            setMacro(macro.withUUIDs(ids), true);
            System.out.println("move mapping up to " + selectedRow);
        }
    }

    private void onMoveDownMapping() {
        if (selectedRow >= 0 && selectedRow < table.getRowCount() - 1) {
            UUID[] ids = macro.mappingUids.clone();
            ids[selectedRow + 1] = macro.mappingUids[selectedRow];
            ids[selectedRow] = macro.mappingUids[selectedRow + 1];
            selectedRow = selectedRow + 1;
            setMacro(macro.withUUIDs(ids), true);
            System.out.println("move mapping down to " + selectedRow);
        }
    }

    private void onDeleteMapping() {
        if (macro.mappingUids.length != 0 && selectedRow != -1) {
            UUID[] newIds = new UUID[macro.mappingUids.length - 1];
            int j = 0;
            for (int i = 0; i < macro.mappingUids.length; i++) {
                if (i != selectedRow) {
                    newIds[j++] = macro.mappingUids[i];
                }
            }
            selectedRow = Math.max(0, Math.min(newIds.length - 1, selectedRow));
            setMacro(macro.withUUIDs(newIds), true);
        }
    }

    private void onChangeMapping() {
        JDialog dialog = new SelectLayerMappingDialog(LayerMappingContainer.INSTANCE.queryAll(), f -> {
            UUID[] newIds = macro.mappingUids.clone();
            newIds[selectedRow] = f.getUid();
            setMacro(macro.withUUIDs(newIds), true);
        });
        dialog.setModal(true);
        dialog.setVisible(true);
    }

    private void updateComponents() {
        System.out.println(getClass().getSimpleName() + ": update components");

        name.setText(macro.getName());
        description.setText(macro.getDescription());

        DefaultTableModel model = new DefaultTableModel();
        Object[] columns = new Object[]{"Action"};
        Object[][] data = new Object[macro.mappingUids.length][];

        int i = 0;
        for (UUID id : macro.mappingUids) {
            LayerMapping m = LayerMappingContainer.INSTANCE.queryById(id);
            data[i++] = new Object[]{m};
        }
        model.setDataVector(data, columns);
        table.setModel(model);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || isUpdating || selectedRow == table.getSelectedRow()) return;
            selectedRow = table.getSelectedRow();
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
        table.getSelectionModel().setSelectionInterval(selectedRow, selectedRow);
        // Get the row index of the edited row (the row that triggered the update)
        if (selectedRow < table.getRowCount()) {
            System.out.println("scroll to row:" + selectedRow);
            Rectangle view = table.getCellRect(selectedRow, 0, true);
            scrollPane.getViewport().scrollRectToVisible(view);
        }
    }

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
