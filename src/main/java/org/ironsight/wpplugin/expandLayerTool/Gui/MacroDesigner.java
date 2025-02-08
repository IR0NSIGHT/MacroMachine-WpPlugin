package org.ironsight.wpplugin.expandLayerTool.Gui;

import org.ironsight.wpplugin.expandLayerTool.operations.LayerMapping;
import org.ironsight.wpplugin.expandLayerTool.operations.LayerMappingContainer;
import org.ironsight.wpplugin.expandLayerTool.operations.MappingMacro;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.Consumer;

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

    public static void main(String[] args) {
        LayerMappingContainer.addDefaultMappings(LayerMappingContainer.INSTANCE);
        MappingMacro mappingMacro = new MappingMacro("test macro",
                "it does cool things on your map",
                LayerMappingContainer.INSTANCE.queryAll().stream().map(LayerMapping::getUid).toArray(UUID[]::new),
                UUID.randomUUID());

        JDialog dialog = getDesignerDialog(null, mappingMacro, f -> {
        });
        dialog.setVisible(true);
    }

    public static JDialog getDesignerDialog(Frame parent, MappingMacro macro, Consumer<MappingMacro> onSubmit) {
        JDialog dialog = new JDialog(parent);
        dialog.setTitle("Macro Designer");
        MacroDesigner designer = new MacroDesigner(onSubmit);
        designer.setMacro(macro, true);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(designer, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout());
        panel.add(buttons, BorderLayout.SOUTH);

        JButton submitButton = new JButton("submit");
        buttons.add(submitButton);
        submitButton.addActionListener(e -> {
            onSubmit.accept(designer.macro);
            dialog.dispose();
        });

        JButton helpButton = HelpDialog.getHelpButton("Macro Designer", helpString);
        buttons.add(helpButton);

        dialog.setContentPane(panel);
        dialog.setModal(true);
        dialog.pack();
        return dialog;
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

        table = new JTable();
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
        addButton.addActionListener(e -> onAddMapping());
        buttons.add(addButton);

        removeButton = new JButton("Remove");
        removeButton.addActionListener(e -> onDeleteMapping());
        buttons.add(removeButton);

        moveUpButton = new JButton("Move Up");
        moveUpButton.addActionListener(e -> onMoveUpMapping());
        buttons.add(moveUpButton);

        moveDownButton = new JButton("Move Down");
        moveDownButton.addActionListener(e -> onMoveDownMapping());
        buttons.add(moveDownButton);

        changeMappingButton = new JButton("Change Mapping");
        changeMappingButton.addActionListener(e -> onChangeMapping());
        buttons.add(changeMappingButton);

        JButton submitButton = new JButton("Submit");
        submitButton.addActionListener(e -> onSubmit.accept(this.macro));
        buttons.add(submitButton);
        this.add(buttons, BorderLayout.SOUTH);
    }

    private void onAddMapping() {
        //insert any mapping from container at tail of list
        ArrayList<LayerMapping> all = LayerMappingContainer.INSTANCE.queryAll();
        if (all.isEmpty()) return;
        UUID next = all.get(0).getUid();
        UUID[] ids = Arrays.copyOf(macro.mappingUids, macro.mappingUids.length + 1);
        ids[ids.length - 1] = next;
        selectedRow = ids.length - 1;
        MappingMacro mappingMacro = macro.withUUIDs(ids);
        setMacro(mappingMacro, true);

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
        assert macro.allMappingsReady(LayerMappingContainer.INSTANCE);

        name.setText(macro.getName());
        description.setText(macro.getDescription());

        DefaultTableModel model = new DefaultTableModel();
        Object[] columns = new Object[]{"Action"};
        Object[][] data = new Object[macro.mappingUids.length][];

        int i = 0;
        for (UUID id : macro.mappingUids) {
            LayerMapping m = LayerMappingContainer.INSTANCE.queryById(id);
            assert m != null;

            data[i++] = new Object[]{m};
        }
        model.setDataVector(data, columns);
        table.setModel(model);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || isUpdating) return;
            selectedRow = table.getSelectedRow();
            System.out.println(" ROW SELECTED:  " + selectedRow);
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
