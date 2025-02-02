package org.ironsight.wpplugin.expandLayerTool.Gui;

import org.ironsight.wpplugin.expandLayerTool.operations.LayerMapping;
import org.ironsight.wpplugin.expandLayerTool.operations.LayerMappingContainer;
import org.ironsight.wpplugin.expandLayerTool.operations.MappingMacro;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.UUID;

public class MacroDesigner extends JPanel {
    private MappingMacro macro;

    private JLabel name, description;
    private JTable table;
    private JButton addButton, removeButton;
    private JScrollPane scrollPane;
    private int selectedRow;

    MacroDesigner() {
        init();
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setTitle("Macro Designer");

        LayerMappingContainer.addDefaultMappings(LayerMappingContainer.INSTANCE);
        MappingMacro mappingMacro = new MappingMacro("test macro",
                "it does cool things on your map",
                Arrays.stream(LayerMappingContainer.INSTANCE.queryMappingsAll())
                        .map(LayerMapping::getUid)
                        .toArray(UUID[]::new),
                UUID.randomUUID());

        MacroDesigner designer = new MacroDesigner();
        designer.setMacro(mappingMacro);
        frame.add(designer);

        frame.setSize(new Dimension(400, 400));
        frame.setVisible(true);
    }

    private void init() {
        this.setLayout(new BorderLayout());

        name = new JLabel("Name goes here");
        name.setFont(LayerMappingTopPanel.header1Font);
        description = new JLabel("Description goes here");
        description.setFont(LayerMappingTopPanel.header2Font);
        table = new JTable();
        table.setDefaultEditor(Object.class,
                new MappingTableCellEditor(this::onDeleteMapping,
                        this::onEditMapping,
                        this::onMoveUpMapping,
                        this::onMoveDownMapping));
        table.setDefaultRenderer(Object.class, new MappingTableCellRenderer());
        scrollPane = new JScrollPane(table);
        this.add(scrollPane, BorderLayout.CENTER);

        JPanel top = new JPanel(new GridLayout(0, 1));
        top.add(name);
        top.add(description);
        this.add(top, BorderLayout.NORTH);

        JPanel buttons = new JPanel(new FlowLayout());
        JButton applyButton = new JButton("Apply");
        applyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onApplyButtonPressed();
            }
        });
        buttons.add(applyButton);

        addButton = new JButton("Add");
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                LayerMapping[] all = LayerMappingContainer.INSTANCE.queryMappingsAll();
                if (all.length == 0) return;
                UUID next = all[0].getUid();
                UUID[] ids = Arrays.copyOf(macro.mappingUids, macro.mappingUids.length + 1);
                ids[ids.length - 1] = next;
                MappingMacro mappingMacro = macro.withUUIDs(ids);
                setMacro(mappingMacro);
            }
        });
        buttons.add(addButton);
        this.add(buttons, BorderLayout.SOUTH);
    }

    private void onMoveUpMapping(LayerMapping mapping) {

    }

    private void onMoveDownMapping(LayerMapping mapping) {

    }

    private void onDeleteMapping(LayerMapping mapping) {

    }

    private void onEditMapping(LayerMapping mapping) {

    }

    private void onApplyButtonPressed() {

    }

    private void update() {
        assert macro.allMappingsReady(LayerMappingContainer.INSTANCE);

        name.setText(macro.getName());
        description.setText(macro.getDescription());

        DefaultTableModel model = new DefaultTableModel();
        Object[] columns = new Object[]{"Action"};
        Object[][] data = new Object[macro.mappingUids.length][];

        int i = 0;
        for (UUID id : macro.mappingUids) {
            LayerMapping m = LayerMappingContainer.INSTANCE.queryMappingById(id);
            assert m != null;

            data[i++] = new Object[]{m};
        }
        model.setDataVector(data, columns);
        table.setModel(model);
        table.getModel().addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE) {
                //collect the UUIDs in order
                UUID[] ids = new UUID[table.getModel().getRowCount()];
                //get edited cell
                for (int ii = 0; ii < ids.length; ii++) {
                    ids[ii] = ((LayerMapping) table.getModel().getValueAt(ii, 0)).getUid();
                }
                SwingUtilities.invokeLater(() -> {
                    int editedRow = e.getFirstRow();
                    selectedRow = editedRow;
                    this.setMacro(macro.withUUIDs(ids));
                });
            }
        });


        final int rowCount = table.getRowCount();
        final int colCount = table.getColumnCount();
        for (int ix = 0; ix < rowCount; ix++) {
            int maxHeight = 0;
            for (int j = 0; j < colCount; j++) {
                final TableCellRenderer renderer = table.getCellRenderer(ix, j);
                maxHeight = Math.max(maxHeight, table.prepareRenderer(renderer, ix, j).getPreferredSize().height);
            }
            table.setRowHeight(ix, maxHeight );
        }
        invalidate();
        repaint();

        // Get the row index of the edited row (the row that triggered the update)
        if (selectedRow < table.getRowCount()) {
            System.out.println("scroll to row:" + selectedRow);
            Rectangle view = table.getCellRect(selectedRow, 0, true);
            scrollPane.getViewport().scrollRectToVisible(view);
        }
    }

    public void setMacro(MappingMacro macro) {
        assert macro != null;
        if (this.macro != null && this.macro.equals(macro)) return; //dont update if nothing changed
        this.macro = macro;
        update();
    }

}
