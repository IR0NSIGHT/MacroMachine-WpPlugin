package org.ironsight.wpplugin.macromachine.operations;

import org.ironsight.wpplugin.macromachine.Gui.GlobalActionPanel;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.InputOutputProvider;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.WorldPainterView;
import org.pepsoft.worldpainter.layers.LayerManager;
import org.pepsoft.worldpainter.operations.AbstractOperation;

import javax.swing.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.ironsight.wpplugin.macromachine.Gui.ActionEditor.createDialog;

public class MacroDialogOperation extends AbstractOperation implements MacroApplicator {
    private static final String NAME = "Macro Operation";
    private static final String DESCRIPTION = "Create complex reusable global operations to automate your workflow.";
    private static final String ID = "macro_dialog_operation";
    private WorldPainterView mWorldPainterView;

    public MacroDialogOperation() {
        super(NAME, DESCRIPTION, "macrooperation");

        MacroContainer.SetInstance(new MacroContainer(null));
        LayerMappingContainer.SetInstance( new LayerMappingContainer(null));

        MacroContainer.getInstance().readFromFile();
        LayerMappingContainer.getInstance().readFromFile();
        LayerMappingContainer.getInstance().subscribe(() -> LayerMappingContainer.getInstance().writeToFile());
        MacroContainer.getInstance().subscribe(() -> MacroContainer.getInstance().writeToFile());
    }

    private Dimension getDimension() {
        return mWorldPainterView.getDimension();
    }

    public void openDialog() {
        try {
            LayerObjectContainer.getInstance().setWpLayerManager(LayerManager.getInstance());
            LayerObjectContainer.getInstance().setDimension(this.getDimension());
            InputOutputProvider.INSTANCE.updateFrom(getDimension());
            JDialog dialog = createDialog(null, this);
            dialog.toFront();              // Bring it to the front
            dialog.requestFocusInWindow();
            dialog.setVisible(true);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public Collection<ExecutionStatistic> applyLayerAction(Macro macro,
                                                           Consumer<ApplyAction.Progess> setProgress) {
        Collection<ExecutionStatistic> statistics = new ArrayList<>();
        try {
            this.getDimension().setEventsInhibited(true);
            LinkedList<List<UUID>> actionIds = new LinkedList<>();
            List<List<UUID>> steps = macro.collectActions(actionIds);
            List<List<LayerMapping>> executionSteps = steps.stream()
                    .map(stepIds -> stepIds.stream()
                            .map(LayerMappingContainer.getInstance()::queryById)
                            .collect(Collectors.toList()))
                    .collect(Collectors.toList());

            boolean hasNullActions = executionSteps.stream().anyMatch(step -> step.stream().anyMatch(Objects::isNull));
            if (hasNullActions) {
                GlobalActionPanel.ErrorPopUp(
                        "Some actions in the execution list are null. This means they were deleted, but are still " +
                                "linked into a macro." + " The macro can" + " " + "not be applied to the " + "map.");
                return statistics;
            }

            // prepare actions for dimension
            for (List<LayerMapping> step : executionSteps) {
                for (LayerMapping action : step) {
                    try {
                        action.output.prepareForDimension(getDimension());
                        action.input.prepareForDimension(getDimension());
                    } catch (IllegalAccessError e) {
                        GlobalActionPanel.ErrorPopUp(
                                "Action " + action.getName() + " can not be applied to the map." + e.getMessage());
                        return statistics;
                    }
                }
            }

            // ----------------------- macro is ready and can be applied to map
            statistics = ApplyAction.applyExecutionSteps(getDimension(), new TileFilter(), executionSteps, setProgress);
            statistics.forEach(System.out::println);

        } catch (Exception ex) {
            return statistics;
        } finally {
            this.getDimension().setEventsInhibited(false);
        }
        return statistics;
    }

    @Override
    protected void activate() {
        openDialog();
    }

    @Override
    protected void deactivate() {

    }


    @Override
    public void setView(WorldPainterView view) {
        this.mWorldPainterView = view;
    }

    @Override
    public void interrupt() {

    }
}
