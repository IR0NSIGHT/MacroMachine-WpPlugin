package org.ironsight.wpplugin.macromachine.Gui.ItemPicker;

import org.ironsight.wpplugin.macromachine.Gui.DisplayUnitRenderer;
import org.ironsight.wpplugin.macromachine.operations.Macro;
import org.ironsight.wpplugin.macromachine.operations.MappingAction;
import org.ironsight.wpplugin.macromachine.operations.MappingActionContainer;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IDisplayUnit;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.ironsight.wpplugin.macromachine.Gui.LayerMappingTopPanel.header1Font;
import static org.ironsight.wpplugin.macromachine.Gui.MacroDesigner.getDefaultFiltersAndEmptyAction;

public class DisplayUnitPickerDialog extends JDialog {

    private final DisplayValueRowFilter rowFilter = new DisplayValueRowFilter();
    private HashSet<PickerFilterOption> activeFilters = new HashSet<>();
    private TableRowSorter<TableModel> rowSorter;

    public DisplayUnitPickerDialog(ArrayList<IDisplayUnit> layerMappings,
                                   Consumer<IDisplayUnit> onSubmit,
                                   Collection<IDisplayUnit> topActions,
                                   Component parent, PickerFilterOption... filters) {
        super();
        this.setLayout(new BorderLayout());

        init(layerMappings, onSubmit, topActions);
        initFilters(filters);
        setModal(true);
        setAlwaysOnTop(true);
        pack();
        if (parent != null) {
            setLocation(parent.getLocationOnScreen());
        }
    }

    public static void main(String[] args) {
        // CUSTOM ACTIONS
        MappingActionContainer container = new MappingActionContainer(null);
        for (int i = 0; i < 20; i++) MappingActionContainer.addDefaultMappings(container);

        // MACROS
        ArrayList<Macro> macros = new ArrayList<>(5);
        for (int i = 0; i < 5; i++) {
            macros.add(new Macro("Macro_" + i, "", new UUID[0], UUID.randomUUID(), new boolean[0]));
        }

        // DEFAULT ACTIONS
        Collection<MappingAction> defaultMappings = getDefaultFiltersAndEmptyAction();
        Collection<UUID> defaultUUIDs = new ArrayList<>();
        defaultMappings.stream().forEach(m -> defaultUUIDs.add(((MappingAction) m).getUid()));

        // SELECT ALL
        ArrayList<IDisplayUnit> allItems = new ArrayList<>();
        allItems.addAll(container.queryAll());
        allItems.addAll(macros);
        allItems.addAll(defaultMappings);


        JFrame frame = new JFrame("Select Layer Mapping");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setVisible(true);

        PickerFilterOption noMacroFilter = new PickerFilterOption("macros", "show macros") {
            @Override
            public boolean block(Object item) {
                return item instanceof Macro;
            }
        };

        PickerFilterOption noDefaultFilter = new PickerFilterOption("defaults", "show default actions") {
            Set<UUID> matchingIds = defaultMappings.stream()
                    .map(MappingAction::getUid)
                    .collect(Collectors.toCollection(HashSet::new));

            @Override
            public boolean block(Object item) {
                return item instanceof MappingAction && matchingIds.contains(((MappingAction) item).getUid());
            }
        };

        PickerFilterOption noCustomActionsFilter = new PickerFilterOption("custom","show user created actions") {
            Set<UUID> matchingIds = container.queryAll().stream()
                    .map(MappingAction::getUid)
                    .collect(Collectors.toCollection(HashSet::new));
            @Override
            public boolean block(Object item) {
                return item instanceof MappingAction && matchingIds.contains(((MappingAction) item).getUid());
            }
        };

        Dialog dlg = new DisplayUnitPickerDialog(allItems,
                System.out::println,
                Collections.singleton(MappingAction.getNewEmptyAction()),
                frame, noMacroFilter, noDefaultFilter, noCustomActionsFilter);
        dlg.setVisible(true);
    }

    private void addFilter(PickerFilterOption filter) {
        activeFilters.add(filter);
        rowFilter.setFilters(activeFilters.toArray(PickerFilterOption[]::new));
        rowSorter.setRowFilter(rowFilter);
    }

    private void removeFilter(PickerFilterOption filter) {
        activeFilters.remove(filter);
        rowFilter.setFilters(activeFilters.toArray(PickerFilterOption[]::new));
        rowSorter.setRowFilter(rowFilter);
    }

    private void initFilters(PickerFilterOption... filters) {
        JPanel filterCheckboxes = new JPanel(new FlowLayout());
        for (PickerFilterOption f : filters) {
            JCheckBox filterCheckbox = new JCheckBox(f.getDisplayName());
            filterCheckbox.setToolTipText(f.getTooltip());
            filterCheckboxes.add(filterCheckbox);
            ActionListener updateFilter = l -> {
                if (filterCheckbox.isSelected()) {
                    removeFilter(f); //no need to filter out
                } else {
                    addFilter(f); //"macros" is unchecked, add macro filter
                }
            };
            filterCheckbox.addActionListener(updateFilter);
            filterCheckbox.setSelected(true);
            updateFilter.actionPerformed(null);

        }
        this.add(filterCheckboxes, BorderLayout.NORTH);
    }

    private void init(ArrayList<IDisplayUnit> items,
                      Consumer<IDisplayUnit> onSubmit,
                      Collection<IDisplayUnit> specialTopAction) {

        DefaultTableModel tableModel = createTableModel(items, specialTopAction);
        JTable table = createTable(tableModel);
        rowSorter = new TableRowSorter<>(table.getModel());
        table.setRowSorter(rowSorter);
        rowSorter.setRowFilter(rowFilter);

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
        table.setDefaultRenderer(Object.class, new DisplayUnitRenderer(f -> true));
        return table;
    }

    private void resizeRowHeights(JTable table) {
        if (table.getRowCount() < 1)
            return;
        TableCellRenderer renderer = table.getCellRenderer(0, 0);
        Component comp = table.prepareRenderer(renderer, 0, 0);
        int height = comp.getPreferredSize().height;
        for (int i = 0; i < table.getRowCount(); i++)
            table.setRowHeight(i, height);
    }

    private JButton createOkButton(JTable table, Consumer<IDisplayUnit> onSubmit) {
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow >= 0) {
                IDisplayUnit selected = (IDisplayUnit) table.getValueAt(selectedRow, 0);
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
            public void insertUpdate(DocumentEvent e) {
                filter();
            }

            public void removeUpdate(DocumentEvent e) {
                filter();
            }

            public void changedUpdate(DocumentEvent e) {
                filter();
            }

            private void filter() {
                String text = searchField.getText();
                rowFilter.setString(text);
                rowSorter.setRowFilter(rowFilter);
            }
        });
        return searchField;
    }

    private JPanel createSearchPanel(JTextField searchField) {
        JLabel searchIcon = new JLabel("\uD83D\uDD0D"); // "🔍"
        searchIcon.setFont(header1Font);

        JPanel searchPanel = new JPanel(new BorderLayout(5, 0));
        searchPanel.add(searchIcon, BorderLayout.WEST);
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        return searchPanel;
    }

}