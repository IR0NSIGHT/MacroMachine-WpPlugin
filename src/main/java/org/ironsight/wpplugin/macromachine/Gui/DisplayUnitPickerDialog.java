package org.ironsight.wpplugin.macromachine.Gui;

import org.ironsight.wpplugin.macromachine.operations.MappingAction;
import org.ironsight.wpplugin.macromachine.operations.MappingActionContainer;
import org.ironsight.wpplugin.macromachine.operations.SaveableAction;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IDisplayUnit;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class DisplayUnitPickerDialog extends JDialog {

    public DisplayUnitPickerDialog(ArrayList<IDisplayUnit> layerMappings,
                                   Consumer<IDisplayUnit> onSubmit,
                                   Collection<IDisplayUnit> topActions,
                                   Component parent) {
        super();
        init(layerMappings, onSubmit, topActions);
        setModal(true);
        setAlwaysOnTop(true);
        pack();
        if (parent != null) {
            setLocation(parent.getLocationOnScreen());
        }
    }

    public static void main(String[] args) {
        MappingActionContainer container = new MappingActionContainer(null);
        for (int i = 0; i < 20; i++) MappingActionContainer.addDefaultMappings(container);

        ArrayList<IDisplayUnit> layerMappings = new ArrayList<>(container.queryAll());

        JFrame frame = new JFrame("Select Layer Mapping");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setVisible(true);

        Dialog dlg = new DisplayUnitPickerDialog(layerMappings,
                System.out::println,
                Collections.singleton(MappingAction.getNewEmptyAction()),
                frame);
        dlg.setVisible(true);
    }

    private void init(ArrayList<IDisplayUnit> items,
                      Consumer<IDisplayUnit> onSubmit,
                      Collection<IDisplayUnit> specialTopAction) {

        DefaultTableModel tableModel = createTableModel(items, specialTopAction);
        JTable table = createTable(tableModel);
        TableRowSorter<TableModel> rowSorter = new TableRowSorter<>(table.getModel());
        table.setRowSorter(rowSorter);

        JButton okButton = createOkButton(table, onSubmit);
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());

        resizeRowHeights(table);
        table.setTableHeader(null);

        JTextField searchField = createSearchField(rowSorter);
        JPanel searchPanel = createSearchPanel(searchField);

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(searchPanel, BorderLayout.NORTH);
        contentPanel.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        getContentPane().add(contentPanel, BorderLayout.CENTER);
        SwingUtilities.invokeLater(searchField::requestFocus);
    }

    private DefaultTableModel createTableModel(ArrayList<IDisplayUnit> items,
                                               Collection<IDisplayUnit> topActions) {
        int rowCount = items.size() + (topActions != null ? topActions.size() : 0);
        DefaultTableModel model = new DefaultTableModel(new Object[rowCount][1], new String[]{"Items"}) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        int i = 0;
        for (IDisplayUnit item : items) {
            model.setValueAt(item, i++, 0);
        }
        if (topActions != null) {
            for (IDisplayUnit action : topActions) {
                model.setValueAt(action, i++, 0);
            }
        }
        return model;
    }

    private JTable createTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setDefaultRenderer(Object.class, new DisplayUnitRenderer(f -> false));
        return table;
    }

    private void resizeRowHeights(JTable table) {
        for (int row = 0; row < table.getRowCount(); row++) {
            TableCellRenderer renderer = table.getCellRenderer(row, 0);
            Component comp = table.prepareRenderer(renderer, row, 0);
            table.setRowHeight(row, comp.getPreferredSize().height);
        }
    }

    private JButton createOkButton(JTable table, Consumer<IDisplayUnit> onSubmit) {
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow >= 0) {
                SaveableAction selected = (SaveableAction) table.getValueAt(selectedRow, 0);
                onSubmit.accept(selected);
                dispose();
            }
        });
        return okButton;
    }

    private JTextField createSearchField(TableRowSorter<TableModel> rowSorter) {
        JTextField searchField = new JTextField(15);
        searchField.setToolTipText("Search...");

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filter(); }
            public void removeUpdate(DocumentEvent e) { filter(); }
            public void changedUpdate(DocumentEvent e) { filter(); }

            private void filter() {
                String text = searchField.getText();
                if (text.trim().isEmpty()) {
                    rowSorter.setRowFilter(null);
                } else {
                    rowSorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(text)));
                }
            }
        });
        return searchField;
    }

    private JPanel createSearchPanel(JTextField searchField) {
        JLabel searchIcon = new JLabel("\uD83D\uDD0D"); // "🔍"
        searchIcon.setFont(new Font("SansSerif", Font.PLAIN, 16));

        JPanel searchPanel = new JPanel(new BorderLayout(5, 0));
        searchPanel.add(searchIcon, BorderLayout.WEST);
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        return searchPanel;
    }
}