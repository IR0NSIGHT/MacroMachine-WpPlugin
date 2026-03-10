package org.ironsight.wpplugin.macromachine.Gui;

import org.ironsight.wpplugin.macromachine.operations.ActionType;
import org.ironsight.wpplugin.macromachine.operations.ProviderType;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IPositionValueGetter;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IPositionValueSetter;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.awt.Image.SCALE_FAST;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;
import static org.ironsight.wpplugin.macromachine.Gui.MappingActionValueTableModel.INPUT_COLUMN_IDX;
import static org.ironsight.wpplugin.macromachine.Gui.MappingActionValueTableModel.OUTPUT_COLUMN_IDX;

public class MappingTextTable extends JPanel {
    private final MappingActionValueTableModel tableModel;
    private final JTable numberTable;
    JScrollPane scrollPane;
    ValuePreviewWindow previeWindow;
    private boolean isFilterHideAutomaticValues = false;
    private JCheckBox groupValuesCheckBox;
    private JCheckBox showPreviewWindow;
    private TableRowSorter<MappingActionValueTableModel> sorter;
    private int[] selectedViewRows = new int[0];
    private BlockingSelectionModel blockingSelectionModel;

    private String getToolTipForRow(int row) {
        if (tableModel.getValueAt(row, INPUT_COLUMN_IDX) instanceof MappingPointValue input &&
            tableModel.getValueAt(row, OUTPUT_COLUMN_IDX) instanceof MappingPointValue output) {
            return Explain(input, output, tableModel.constructMapping().getActionType());
        }
        return "";
    }

    public static void main(String[] args) {
        var allGetterSetters = Arrays.stream(ProviderType.values()).map(ProviderType::fromTypeDefault).toList();
        var allGetters = allGetterSetters.stream().filter(g -> g instanceof IPositionValueGetter).map(g -> (IPositionValueGetter)g).toList();
        var allSetters = allGetterSetters.stream().filter(g -> g instanceof IPositionValueSetter).map(g -> (IPositionValueSetter)g).toList();
        Random r = new Random(42069);
        Function<int[],Integer> getRandom = arr -> {
            if (arr.length == 0)
                return 0;
            return arr[r.nextInt(arr.length)];
        };
        for (var type: ActionType.values()) {
        for (var setter: allSetters) {
            for (var getter : allGetters) {
                    var input = new MappingPointValue(getRandom.apply(getter.getAllInputValues()),getter);
                    var output = new MappingPointValue(getRandom.apply(setter.getAllOutputValues()),setter);

                    var explained = Explain(input,output,type);
                    System.out.println(explained);
                }
            }
        }
    }

    public static String Explain(MappingPointValue input, MappingPointValue output, ActionType actionType) {
        StringBuilder builder = new StringBuilder();
        builder.append("Where ")
                .append(input.mappingValue.getName())
                .append(" is ")

                .append(input.mappingValue.valueToString(input.numericValue))
                .append(", ");
        if (output.mappingValue instanceof IPositionValueSetter setter && setter.isIgnoreValue(output.numericValue)) {
            builder.append(" do nothing.");
        } else {
            builder.append(actionType.getExplanationFor(output)).append(".");
        }

        return builder.toString();
    }
    public MappingTextTable(MappingActionValueTableModel model, BlockingSelectionModel selectionModel) {
        this.blockingSelectionModel = selectionModel;
        numberTable = new JTable() {
            @Override
            public boolean editCellAt(int row, int column, EventObject e) {
                // first click -> select
                if (!numberTable.isCellSelected(row, column)) {
                    SwingUtilities.invokeLater(() -> numberTable.addRowSelectionInterval(row, row));
                    return false;
                }
                return super.editCellAt(row, column, e);
            }

            @Override
            public String getToolTipText(MouseEvent e) {
                Point p = e.getPoint();
                int row = rowAtPoint(p);
                int col = columnAtPoint(p);

                if (row == -1 || col == -1) {
                    return null;
                }

                return getToolTipForRow(row);
            }

            @Override
            public TableCellEditor getCellEditor(int row, int column) {
                // second click -> edit
                blockingSelectionModel.setSelectionBlocked(true);
                return super.getCellEditor(row, column);
            }

            @Override
            public void editingStopped(ChangeEvent e) {
                TableCellEditor editor = getCellEditor();
                if (editor != null) {
                    Object value = editor.getCellEditorValue();

                    model.setValuesAt((MappingPointValue) value, getSelectedModelRows(), editingColumn);
                    removeEditor();
                }
            }

            @Override
            public void removeEditor() {
                super.removeEditor();
                blockingSelectionModel.setSelectionBlocked(false);
            }
        };
        selectionModel.setTable(numberTable);
        numberTable.setModel(model);
        numberTable.setSelectionModel(selectionModel);
        this.tableModel = model;
        initComponents();
        addListeners(model, selectionModel);
    }

    private int[] getSelectedModelRows() {
        int[] selectedModelRows =
                Arrays.stream(numberTable.getSelectedRows())
                        .map(viewRow -> numberTable.convertRowIndexToModel(viewRow))
                        .toArray();
        return selectedModelRows;
    }

    protected void updateComponents() {
        TableRowSorter<?> sorter = (TableRowSorter<?>) numberTable.getRowSorter();
        List<? extends RowSorter.SortKey> sortKeys = sorter.getSortKeys();
// force stop current edit, because the amount of rows changes, and otherwise will cause array index out
        // of bounds
        if (numberTable.isEditing()) {
            TableCellEditor editor = numberTable.getCellEditor();
            if (editor != null) {
                editor.stopCellEditing(); // or editor.cancelCellEditing();
            }
        }

        sorter.setSortKeys(sortKeys);
        sorter.sort();

    }

    private void setRowFilter(boolean groupValues) {
        if (!groupValues) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(new RowFilter<MappingActionValueTableModel, Integer>() {
                @Override
                public boolean include(Entry<? extends MappingActionValueTableModel, ? extends Integer> entry) {
                    return tableModel.isMappingPoint(entry.getIdentifier());
                }
            });
        }
    }

    private void addListeners(MappingActionValueTableModel model, BlockingSelectionModel selectionModel) {
        // Add listener to scroll to the selected row
        selectionModel.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            try {
                int lastViewRow = numberTable.convertRowIndexToView(selectionModel.getLastSelectedModelRow());
                numberTable.scrollRectToVisible(numberTable.getCellRect(lastViewRow, 0, true));
            } catch (IndexOutOfBoundsException ignored) {
            }
        });

        model.addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE) {
                SwingUtilities.invokeLater(this::updateComponents);

            }
        });
        MouseAdapter rightClickListener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!numberTable.isEditing() && e.getButton() == MouseEvent.BUTTON3) {
                    // Show the popup menu
                    JPopupMenu popupMenu = createRightClickMenu(selectionModel.getSelectedModelRows());
                    popupMenu.show(numberTable, e.getX(), e.getY());
                }
            }
        };
        numberTable.addMouseListener(rightClickListener);

// Save selected row table
        numberTable.getSelectionModel().addListSelectionListener(e -> {
            selectedViewRows = numberTable.getSelectedRows();
        });

// Restore selected raw table after a value was changed in the table
        model.addTableModelListener(e -> SwingUtilities.invokeLater(() -> {
            try {
                selectionModel.addSelectionRows(selectedViewRows);
            } catch (IllegalArgumentException ignored) {
                // view row amount might be MUCH lower than expected. just ignore it
            }
        }));

        numberTable.addMouseListener(previeWindow);
        numberTable.addMouseMotionListener(previeWindow);
        scrollPane.addMouseWheelListener(previeWindow);
        scrollPane.addMouseListener(rightClickListener);
    }

    private JPopupMenu createRightClickMenu(int[] selectedModelRows) {
        JPopupMenu menu = new JPopupMenu();
        menu.setLayout(new GridLayout(0,1));

        boolean fixedValuesSelected = Arrays.stream(selectedModelRows).anyMatch(tableModel::isMappingPoint);
        boolean interpolatedValuesSelected = Arrays.stream(selectedModelRows).anyMatch(r -> !tableModel.isMappingPoint(r));
        {
            JButton button = new JButton("clear selection");
            button.addActionListener(e -> {
                clearSelection();
                menu.setVisible(false);
            });
            menu.add(button);
        }

        if (interpolatedValuesSelected) {
            JButton button = new JButton("make fixed value");
            button.setToolTipText("fixed values are set by you. They influence the values of nearby automatic values.");
            button.addActionListener(e -> {
                this.onMakeFixedValue(e);
                menu.setVisible(false);
            });
            menu.add(button);
        }

        if (fixedValuesSelected) {
            JButton button = new JButton("make automatic value");
            button.setToolTipText("automatic values are calculated, based on the nearest fixed values. You can not edit them directly.");
            button.addActionListener(e -> {
                this.onMakeAutomaticValue();
                menu.setVisible(false);
            });
            menu.add(button);
        }

        return menu;
    }

    private void clearSelection() {
        this.blockingSelectionModel.clearSelection();
    }

    private void onMakeFixedValue(ActionEvent actionEvent) {
        final int[] selectedModelRows = getSelectedModelRows();
        boolean somethingSelected = selectedModelRows != null && selectedModelRows.length != 0;
        if (!somethingSelected) {
            return;
        }
        // easy case: a point is not yet a mapping point
        var notYetMappingPoints = Arrays.stream(selectedModelRows)
                .filter(row -> !tableModel.isMappingPoint(row))
                .toArray();
        tableModel.setIsMappingPoint(notYetMappingPoints, true);
        SwingUtilities.invokeLater(() -> {
            Arrays.stream(notYetMappingPoints).forEach(blockingSelectionModel::setSelectionModelRow);
        });
    }

    private void onMakeAutomaticValue() {
        if (numberTable.getSelectedRow() != -1) {
            tableModel.deleteMappingPointAt(getSelectedModelRows());
        }
    }

    protected void initComponents() {
        previeWindow = new ValuePreviewWindow();

        this.setLayout(new BorderLayout());
        Border padding = new EmptyBorder(20, 20, 20, 20); // 20px padding on all sides
        Border whiteBorder = new EmptyBorder(5, 5, 5, 5); // 5px white border
        setBorder(BorderFactory.createCompoundBorder(whiteBorder, padding));

        // Add a TableModelListener to get a callback when a cell is edited
        numberTable.setModel(tableModel);
        sorter = new TableRowSorter<>(tableModel);
        sorter.setSortsOnUpdates(true);
        numberTable.setRowSorter(sorter);
        setRowFilter(isFilterHideAutomaticValues);

        Font font = new Font("Arial", Font.PLAIN, 24);
        numberTable.setFont(font);
        MappingPointCellRenderer cellRenderer = new MappingPointCellRenderer();
        numberTable.setDefaultRenderer(MappingPointValue.class, cellRenderer);
        numberTable.setRowHeight(cellRenderer.getPreferredHeight());
        numberTable.setDefaultEditor(Object.class, new MappingPointCellEditor());
        scrollPane = new JScrollPane(numberTable);
        this.add(scrollPane, BorderLayout.CENTER);
        JPanel buttons = new JPanel();
        {
            groupValuesCheckBox = new JCheckBox("Hide automatic values");
            groupValuesCheckBox.setSelected(isFilterHideAutomaticValues);
            groupValuesCheckBox.addActionListener(f -> {
                this.isFilterHideAutomaticValues = groupValuesCheckBox.isSelected();
                setRowFilter(isFilterHideAutomaticValues);
            });
            buttons.add(groupValuesCheckBox);
        }

        {
            showPreviewWindow = new JCheckBox("Preview Window");
            showPreviewWindow.setToolTipText("Show preview window when hovering over values in the table");
            showPreviewWindow.setSelected(false);
            showPreviewWindow.addActionListener(previeWindow);
            buttons.add(showPreviewWindow);
        }

        this.add(buttons, BorderLayout.SOUTH);


    }

    private class ValuePreviewWindow extends MouseAdapter implements ActionListener {
        JWindow previewWindow;
        JLabel previewLabel;
        private int row, col;
        private boolean allowWindow = false;

        public ValuePreviewWindow() {
            previewWindow = new JWindow();
            previewLabel = new JLabel();
            previewLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
            previewWindow.add(previewLabel);
            previewWindow.setSize(200, 200); // Size of enlarged image
        }

        void setMouseOver(Point p) {
            int row = numberTable.rowAtPoint(p);
            int col = numberTable.columnAtPoint(p);
            if (this.row == row && this.col == col)
                return;

            this.row = row;
            this.col = col;
            if (row >= 0 && (col == 1 || col == 0)) {
                Object cell = numberTable.getValueAt(row, col);
                if (!(cell instanceof MappingPointValue))
                    return;

                // Scale the image (larger)
                BufferedImage scaledImage = new BufferedImage(100, 100, TYPE_INT_RGB);
                MappingPointValue mpv = (MappingPointValue) cell;
                mpv.mappingValue.paint(scaledImage.getGraphics(),
                        mpv.numericValue,
                        new Dimension(scaledImage.getWidth(),
                                scaledImage.getHeight()));

                previewLabel.setIcon(new ImageIcon(scaledImage.getScaledInstance(previewWindow.getWidth(),
                        previewWindow.getHeight(),
                        SCALE_FAST)));

                // Position the window near the mouse
                Point locationOnScreen = numberTable.getLocationOnScreen();
                previewWindow.setLocation(locationOnScreen.x + (int) p.getX() + 15,
                        locationOnScreen.y + (int) p.getY() + 15);
                previewWindow.setVisible(allowWindow);
            }
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            Point tablePoint = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), numberTable);
            setMouseOver(tablePoint);
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            row = -1;
            col = -1;
            previewWindow.setVisible(allowWindow);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            row = -1;
            col = -1;
            previewWindow.setVisible(false);
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            setMouseOver(e.getPoint());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            allowWindow = showPreviewWindow.isSelected();
        }
    }
}


