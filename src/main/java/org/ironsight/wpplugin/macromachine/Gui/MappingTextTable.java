package org.ironsight.wpplugin.macromachine.Gui;

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
import java.util.Arrays;
import java.util.EventObject;
import java.util.List;

import static java.awt.Image.SCALE_FAST;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;

public class MappingTextTable extends JPanel {
    private final MappingActionValueTableModel tableModel;
    private final JTable numberTable;
    JScrollPane scrollPane;
    ValuePreviewWindow previeWindow;
    private boolean isFilterForMappingPoints = true;
    private JCheckBox groupValuesCheckBox;
    private JCheckBox showPreviewWindow;
    private TableRowSorter<MappingActionValueTableModel> sorter;
    private int[] selectedViewRows = new int[0];
    private BlockingSelectionModel blockingSelectionModel;

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
        numberTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!numberTable.isEditing() && e.getButton() == MouseEvent.BUTTON3)
                    selectionModel.clearSelection(); // right click}
            }
        });

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
    }

    private void onAddControlPoint(ActionEvent actionEvent) {
        final int[] selectedModelRows = getSelectedModelRows();
        boolean somethingSelected = selectedModelRows != null && selectedModelRows.length != 0;
        // nothing is selected -> add control point at the very top
        if (!somethingSelected) {
            int addedPointRow = tableModel.insertMappingPointNear(0);
            if (addedPointRow != -1)
                SwingUtilities.invokeLater(() -> {
                    blockingSelectionModel.setSelectionModelRow(addedPointRow);
                });
            return;
        }

        assert somethingSelected;

        // find out if selection contains only control points or now
        boolean atLeastOneNotControlPoint = false;
        for (int row : selectedModelRows) {
            if (!tableModel.isMappingPoint(row)) {
                atLeastOneNotControlPoint = true;
                break;
            }
        }

        if (atLeastOneNotControlPoint) {
            // at least one selected row is not a control point -> set to control point
            tableModel.setIsMappingPoint(selectedModelRows, true);
            // dont edit selection, didnt change.
        } else {
            // all selected points are control points, insert near the end of selection
            int addedPointRow = tableModel.insertMappingPointNear(selectedModelRows[selectedModelRows.length - 1]);
            if (addedPointRow != -1)
                SwingUtilities.invokeLater(() -> {
                    blockingSelectionModel.setSelectionModelRow(addedPointRow);
                });
        }
    }

    private void onRemoveControlPoint(ActionEvent actionEvent) {
        if (numberTable.getSelectedRow() != -1) {
            tableModel.deleteMappingPointAt(getSelectedModelRows());
        }
    }

    protected void initComponents() {
        isFilterForMappingPoints = true;

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
        setRowFilter(isFilterForMappingPoints);

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
            groupValuesCheckBox = new JCheckBox("Only Control Points");
            groupValuesCheckBox.setSelected(isFilterForMappingPoints);
            groupValuesCheckBox.addActionListener(f -> {
                this.isFilterForMappingPoints = groupValuesCheckBox.isSelected();
                setRowFilter(isFilterForMappingPoints);
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


        {
            JButton addMappingPointButton = new JButton("add control point");
            addMappingPointButton.addActionListener(this::onAddControlPoint);
            buttons.add(addMappingPointButton);
        }
        {
            JButton button = new JButton("remove control point");
            button.addActionListener(this::onRemoveControlPoint);
            buttons.add(button);
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


