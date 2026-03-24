package org.ironsight.wpplugin.macromachine.Gui.EditActions;

import org.ironsight.wpplugin.macromachine.Gui.EditActions.RangeEditor.RangeTableEditor;
import org.ironsight.wpplugin.macromachine.operations.*;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.*;
import org.pepsoft.worldpainter.layers.PineForest;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.UUID;
import java.util.function.Consumer;

import static org.ironsight.wpplugin.macromachine.Gui.HelpDialog.getHelpButton;
import static org.ironsight.wpplugin.macromachine.operations.ValueProviders.IPositionValueSetter.IGNORE_VALUE;

public class ActionDesigner extends LayerMappingPanel
{
    private MappingActionValueTableModel model;
    private BlockingSelectionModel selectionModel;
    private final Consumer<MappingAction> onSubmit;
    private MappingGridPanel gridPanel;
    private RangeTableEditor rangeTableEditor;
    private MappingTextTable table;
    private LayerMappingTopPanel topBar;

    public ActionDesigner(Consumer<MappingAction> onSubmit) {
        super();
        assert onSubmit != null;
        this.onSubmit = onSubmit;

        initialize();

        // ADD CTRL S FOR SAVING
        KeyStroke ctrlS = KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK);
        InputMap inputMap = this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = this.getActionMap();
        inputMap.put(ctrlS, "save");
        actionMap.put("save", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onSave();
                // call your save method here
            }
        });

    }

    @Override
    protected void updateComponents() {
        MappingAction action = mapping;
        boolean isRangeTableAction = !action.getInput().isDiscrete() && action.getOutput().isDiscrete();

        // set visibility for the specific editor we want
        gridPanel.setVisible(!isRangeTableAction && !mapping.input.isDiscrete());
        table.setVisible(!isRangeTableAction);
        rangeTableEditor.setVisible(isRangeTableAction);

        // update editors with values
        gridPanel.setMapping(mapping);
        rangeTableEditor.setMapping(mapping);
        model.rebuildModelFromAction(mapping);
        topBar.setMapping(mapping);

        this.invalidate();
        this.repaint();
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        LayerMappingPanel lmp = new ActionDesigner(System.out::println);
        MappingAction ma = new MappingAction(new PerlinNoiseIO(10, 10, 12345, 3), new AnnotationSetter(),
                new MappingPoint[]{new MappingPoint(3, AnnotationSetter.ANNOTATION_BLUE),
                        new MappingPoint(10, IGNORE_VALUE)},
                ActionType.SET, "Blue annotation perlin blobs", "Create perlin islands with annotation blue",
                UUID.randomUUID());

        var setter = new NibbleLayerSetter(PineForest.INSTANCE, false);
        var OceanHeight = new MappingAction(new TerrainHeightIO(-64, 312), setter,
                new MappingPoint[]{new MappingPoint(0 /* ocean */, 31), new MappingPoint(1, IGNORE_VALUE)},
                ActionType.LIMIT_TO, "No pines in the ocean", "xx", UUID.randomUUID());

        lmp.setMapping(ma);
        frame.add(lmp);
        frame.pack();
        frame.setVisible(true);
    }

    private void onSave() {
        if (rangeTableEditor.isVisible()) {
            onSubmit.accept(rangeTableEditor.validateAndBuildAction());
        } else {
            onSubmit.accept(model.constructMapping());
        }
    }

    @Override
    protected void initComponents() {
        model = new MappingActionValueTableModel();
        selectionModel = new BlockingSelectionModel() {
            @Override
            public void setSelectionInterval(int index0, int index1) {
                super.setSelectionInterval(index0, index1);
                // System.out.println("SET SELECTION INTERVAL " + index0 + ".."+ index1);
            }

            @Override
            public void clearSelection() {
                super.clearSelection();
                // System.out.println("CLEAR SELECTION");
            }
        };

        this.setLayout(new BorderLayout());

        gridPanel = new MappingGridPanel(selectionModel);
        assert model != null;
        assert selectionModel != null;
        JPanel tablePanel = new JPanel();
        table = new MappingTextTable(model, selectionModel);
        tablePanel.add(table);

        rangeTableEditor = new RangeTableEditor();
        tablePanel.add(rangeTableEditor);

        model.addTableModelListener(l -> {
            if (l.getType() == TableModelEvent.DELETE)
                return; // dont update, bc the data is not there yet.
            this.updateMapping(model.constructMapping());
        });
        // set up sync between both components

        gridPanel.setOnUpdate(this::updateMapping);

        topBar = new LayerMappingTopPanel();
        topBar.setOnUpdate(this::updateMapping);

        JButton submitButtom = new JButton("save");
        submitButtom.setToolTipText("Submit action and save to global list. Use ctrl + S as a shortcut.");
        submitButtom.addActionListener(e -> onSave());

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(submitButtom);
        buttonPanel.add(getHelpButton("Action Editor", "Use the Action editor the define what your action does.\n"
                + "\nAn action is somewhat similar to a global operation: It does one thing (change the output "
                + "value) "
                + "for many blocks. Its more flexible than a global operation, because you dont set one output "
                + "value, " + "but many." + "Every action has an input, and output and a type.\n"
                + "Define which value of the input maps to which value of the output. The action can " + "interpolate "
                + "between values if the input/output types allow it. This is shown by the dotted line in the "
                + "graph.\n" + "An action will take a block from the map, read its input value and then set the blocks "
                + "output value "
                + "to the one defined by the mapping. If the mapping is defined so that slope 45° sets pine to "
                + "8, then " + "all blocks with 45° slope will " + "receive pine level 8 when the action is executed.\n"
                + "Actions are part of a macro and  can be reused in multiple macros at once.\n"
                + "Save your changes to the action to save it to your global list of actions.\n"
                + "Add new mapping points by left clicking in the graph\n" + "Delete a point by rightclicking it.\n"
                + "Change a point by dragging it or selecting different values in the table."));

        this.add(topBar, BorderLayout.NORTH);
        this.add(tablePanel, BorderLayout.EAST);
        this.add(buttonPanel, BorderLayout.SOUTH);
        this.add(gridPanel, BorderLayout.CENTER);
    }
}
