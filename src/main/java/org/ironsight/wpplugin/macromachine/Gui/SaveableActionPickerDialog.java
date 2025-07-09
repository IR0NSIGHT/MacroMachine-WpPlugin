package org.ironsight.wpplugin.macromachine.Gui;

import org.ironsight.wpplugin.macromachine.operations.Macro;
import org.ironsight.wpplugin.macromachine.operations.MappingAction;
import org.ironsight.wpplugin.macromachine.operations.MappingActionContainer;
import org.ironsight.wpplugin.macromachine.operations.SaveableAction;

import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.function.Consumer;

public class SaveableActionPickerDialog extends JDialog {
    public SaveableActionPickerDialog(ArrayList<SaveableAction> layerMappings, Consumer<SaveableAction> onSubmit,
                                      Collection<SaveableAction> topActions, Component parent) {
        super();
        init(layerMappings, onSubmit, topActions);
        this.setModal(true);
        this.toFront();
        this.setAlwaysOnTop(true);
        this.pack();
        if (parent != null) {
            Point parentLocation = parent.getLocationOnScreen();
            this.setLocation(parentLocation);
        }

    }

    public static void main(String[] args) {
        MappingActionContainer container = new MappingActionContainer(null);

        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setTitle("Select Layer Mapping");
        frame.setVisible(true);

        for (int i = 0; i < 20; i++)
            MappingActionContainer.addDefaultMappings(container);
        ArrayList<SaveableAction> layerMappings = new ArrayList<>(container.queryAll());
        Dialog dlg = new SaveableActionPickerDialog(layerMappings , System.out::println,
                Collections.singleton(MappingAction.getNewEmptyAction()), frame);
        dlg.setVisible(true);
    }

    private void init(ArrayList<SaveableAction> items, Consumer<SaveableAction> onSubmit,
                      Collection<SaveableAction> specialTopAction) {
        if (specialTopAction == null) {
            specialTopAction = new ArrayList<>();
        }

        int rowCount = items.size() + specialTopAction.size();
        DefaultTableModel listModel = new DefaultTableModel(new Object[rowCount][1], new String[]{"Items"}){
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(listModel);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setDefaultRenderer(Object.class, new SaveableActionRenderer(f -> false));

        int i;
        for (i = 0; i < items.size(); i++) {
            listModel.setValueAt(items.get(i), i, 0);
        }

        if (specialTopAction != null) {
            for (Object action : specialTopAction) {
                listModel.setValueAt(action, i++, 0);
            }
        }

        System.out.println("item at (7,0) =" + listModel.getValueAt(7,0));

        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            if (table.getSelectedRows() == null || table.getSelectedRows().length == 0)
                return;
            SaveableAction selected = (SaveableAction)table.getValueAt(table.getSelectedRow(),0);
            onSubmit.accept(selected);
            this.dispose();
        });
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
            this.dispose();
        });

        for (int ix = 0; ix < rowCount; ix++) {
            int maxHeight = 0;
            final TableCellRenderer renderer = table.getCellRenderer(ix, 0);
            maxHeight = Math.max(maxHeight, table.prepareRenderer(renderer, ix, 0).getPreferredSize().height);
            table.setRowHeight(ix, maxHeight);
        }

        JPanel panel = new JPanel(new BorderLayout());
        JScrollPane pane = new JScrollPane(table);
        panel.add(pane, BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        getContentPane().add(panel, BorderLayout.CENTER);
    }


}
