package org.ironsight.wpplugin.macromachine.Gui.EditActions.RangeEditor;

import org.ironsight.wpplugin.macromachine.Gui.EditActions.*;
import org.ironsight.wpplugin.macromachine.operations.ActionType;
import org.ironsight.wpplugin.macromachine.operations.MappingAction;
import org.ironsight.wpplugin.macromachine.operations.MappingPoint;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.*;
import org.pepsoft.worldpainter.Terrain;
import org.pepsoft.worldpainter.layers.PineForest;

import javax.swing.*;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.EventObject;
import java.util.UUID;

import static org.ironsight.wpplugin.macromachine.operations.ValueProviders.IPositionValueSetter.IGNORE_VALUE;

public class RangeTableEditor extends LayerMappingPanel
{
    private final RangeTableModel model;
    private final JPopupMenu rightClickMenu;
    private final JTable table;
    private final JScrollPane scrollPane;
    private final ValuePreviewWindow previewWindow;

    public RangeTableEditor() {
        initialize();

        this.setLayout(new BorderLayout());
        { // prepare main body components
            { // table
                model = new RangeTableModel();
                table = new JTable(model) {
                    @Override
                    public boolean editCellAt(int row, int column, EventObject e) {
                        // first click -> select
                        if (!table.isCellSelected(row, column)) {
                            SwingUtilities.invokeLater(() -> table.addRowSelectionInterval(row, row));
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
                };
                table.setDefaultEditor(MappingPointValue.class, new MappingPointCellEditor(new int[]{0, 1}));
                var cellRenderer = new MappingPointCellRenderer();
                table.setDefaultRenderer(MappingPointValue.class, cellRenderer);
                table.setRowHeight(cellRenderer.getPreferredHeight());

                scrollPane = new JScrollPane();
                scrollPane.setViewportView(table);

                this.add(scrollPane, BorderLayout.CENTER);
            }
            {
                var sorter = new TableRowSorter<>(table.getModel());
                sorter.setSortsOnUpdates(true);
                table.setRowSorter(sorter);
            }
            {   // preview window
                previewWindow = new ValuePreviewWindow(table);
            }
            { // buttons
                JPanel buttons = new JPanel();
                this.add(buttons, BorderLayout.SOUTH);

                JButton validate = new JButton("validate");
                validate.setToolTipText(
                        "will update the table to ensure that ranges do not overlap and contain only legal values.");
                validate.addActionListener(a -> model.enforceDataValidation());
                buttons.add(validate);

                {
                    JCheckBox showPreviewWindow = new JCheckBox("Preview Window");
                    showPreviewWindow.setToolTipText("Show preview window when hovering over values in the table");
                    showPreviewWindow.setSelected(false);
                    showPreviewWindow.addActionListener(previewWindow);
                    buttons.add(showPreviewWindow);
                }
            }
            { // right click menu
                rightClickMenu = new JPopupMenu();
                rightClickMenu.setLayout(new GridLayout(0, 1));

                {
                    JButton addRow = new JButton("Add row");
                    addRow.addActionListener(e -> onAddRow());
                    rightClickMenu.add(addRow);
                }

                {
                    JButton deleteRow = new JButton("Delete row");
                    deleteRow.addActionListener(e -> onDeleteRow());
                    rightClickMenu.add(deleteRow);
                }
            }
        }

        initTableListener();
    }

    public static String Explain(MappingPointValue inputStart, MappingPointValue inputEnd, MappingPointValue output,
            ActionType actionType) {
        StringBuilder builder = new StringBuilder();
        builder.append("Where ")
                .append(inputStart.mappingValue.getName())
                .append(" is between ")
                .append(inputStart.mappingValue.valueToString(inputStart.numericValue))
                .append(" and ")
                .append(inputStart.mappingValue.valueToString(inputEnd.numericValue))
                .append(", ");
        if (output.mappingValue instanceof IPositionValueSetter setter && setter.isIgnoreValue(output.numericValue)) {
            builder.append(" do nothing.");
        } else {
            builder.append(actionType.getExplanationFor(output)).append(".");
        }

        return builder.toString();
    }

    private String getToolTipForRow(int row) {
        if (model.getValueAt(row, 0) instanceof MappingPointValue inputStart
                && model.getValueAt(row, 1) instanceof MappingPointValue inputEnd
                && model.getValueAt(row, 2) instanceof MappingPointValue output) {
            return Explain(inputStart, inputEnd, output, mapping.getActionType());
        }
        return "";
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        LayerMappingPanel lmp = new RangeTableEditor();
        MappingAction ma = new MappingAction(new PerlinNoiseIO(10, 10, 12345, 3), new AnnotationSetter(),
                new MappingPoint[]{new MappingPoint(3, AnnotationSetter.ANNOTATION_BLUE),
                        new MappingPoint(10, IGNORE_VALUE)},
                ActionType.SET, "Blue annotation perlin blobs", "Create perlin islands with annotation blue",
                UUID.randomUUID());

        var setter = new NibbleLayerSetter(PineForest.INSTANCE, false);
        var HeightForest = new MappingAction(new TerrainHeightIO(-64, 312), setter,
                new MappingPoint[]{new MappingPoint(0 /* ocean */, 31), new MappingPoint(1, IGNORE_VALUE)},
                ActionType.LIMIT_TO, "No pines in the ocean", "xx", UUID.randomUUID());

        var SlopeTerrain = new MappingAction(new SlopeProvider(), new TerrainProvider(),
                new MappingPoint[]{new MappingPoint(30, Terrain.GRASS.ordinal()),
                        new MappingPoint(50, Terrain.STONE_MIX.ordinal()),
                        new MappingPoint(90, Terrain.DEEPSLATE.ordinal())},
                ActionType.SET, "paint slopes", " paint slopes ", UUID.randomUUID());

        lmp.setMapping(SlopeTerrain);
        frame.add(lmp);
        frame.pack();
        frame.setVisible(true);
    }

    private int[] getSelectedModelRows() {
        return Arrays.stream(table.getSelectedRows()).map(table::convertRowIndexToModel).sorted().toArray();
    }

    private void onDeleteRow() {
        model.deleteRows(getSelectedModelRows());
    }

    private void onAddRow() {
        model.addRows(getSelectedModelRows());
    }

    public MappingAction validateAndBuildAction() {
        model.enforceDataValidation();
        return model.constructAction();
    }

    @Override
    protected void updateComponents() {
        model.setAction(this.mapping);
    }

    @Override
    protected void initComponents() {

    }

    private void initTableListener() {
        var adapter = new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                super.mouseReleased(e);
                if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e))
                    rightClickMenu.show(table, e.getX(), e.getY());
            }
        };
        table.addMouseListener(adapter);
        scrollPane.addMouseListener(adapter);
    }
}
