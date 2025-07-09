package org.ironsight.wpplugin.macromachine.operations;

import org.ironsight.wpplugin.macromachine.Gui.GlobalActionPanel;
import org.ironsight.wpplugin.macromachine.operations.FileIO.ContainerIO;
import org.ironsight.wpplugin.macromachine.operations.FileIO.ImportExportPolicy;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.ActionFilterIO;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.InputOutputProvider;
import org.pepsoft.worldpainter.CustomLayerControllerWrapper;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.WorldPainterView;
import org.pepsoft.worldpainter.layers.CustomLayer;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.layers.LayerManager;
import org.pepsoft.worldpainter.operations.*;

import javax.swing.*;
import java.io.File;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.ironsight.wpplugin.macromachine.Gui.GlobalActionPanel.ErrorPopUp;
import static org.ironsight.wpplugin.macromachine.Gui.MacroMachineWindow.createDialog;
import static org.ironsight.wpplugin.macromachine.operations.FileIO.ContainerIO.getUsedLayers;

public class MacroDialogOperation extends AbstractOperation implements MacroApplicator {
    private static final String NAME = "Macro Operation";
    private static final String DESCRIPTION = "Create complex reusable global operations to automate your workflow.";

    public MacroDialogOperation() {
        super(NAME, DESCRIPTION, "macrooperation"); //one shot op
        File saveFile = new File(MacroContainer.getActionsFilePath()+"/savefile.macro");

        MacroContainer.SetInstance(new MacroContainer(null));
        MacroContainer macros = MacroContainer.getInstance();
        MappingActionContainer.SetInstance(new MappingActionContainer(null));
        MappingActionContainer layers = MappingActionContainer.getInstance();

        ContainerIO.importFile(layers, macros, saveFile, new ImportExportPolicy(),
                s -> ErrorPopUp("Can not load from savefile:\n"+saveFile.getPath()+"\n"+s), InputOutputProvider.INSTANCE);

        Runnable saveEverything = () -> ContainerIO.exportToFile(MappingActionContainer.getInstance(), MacroContainer.getInstance(), saveFile,
                new ImportExportPolicy(), System.err::println, InputOutputProvider.INSTANCE);

        MappingActionContainer.getInstance().subscribe(saveEverything);
        MacroContainer.getInstance().subscribe(saveEverything);
    }

    public void openDialog() {
        try {
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
            this.getDimension().rememberChanges();
            List<UUID> steps = macro.collectActions(new LinkedList<>());
            List<MappingAction> executionSteps = steps.stream()
                            .map(MappingActionContainer.getInstance()::queryById)
                            .collect(Collectors.toList());

            boolean hasNullActions = executionSteps.stream().anyMatch(Objects::isNull);
            if (hasNullActions) {
                ErrorPopUp(
                        "Some actions in the execution list are null. This means they were deleted, but are still " +
                                "linked into a macro." + " The macro can" + " " + "not be applied to the " + "map.");
                return statistics;
            }


            // prepare actions for dimension
            for (MappingAction action : executionSteps) {
                try {
                    action.output.prepareForDimension(getDimension());
                    action.input.prepareForDimension(getDimension());
                } catch (IllegalAccessError e) {
                    ErrorPopUp(
                            "Action " + action.getName() + " can not be applied to the map." + e.getMessage());
                    return statistics;
                } catch (OutOfMemoryError e) {
                    ErrorPopUp(
                            "Action " + action.getName() + " consumed more memory than available:" + e.getMessage());
                    return statistics;
                } catch (Exception e) {
                    ErrorPopUp(
                            "Action " + action.getName() + " caused an excpetion:" + e.getMessage());
                    return statistics;
                }
            }
            CustomLayerControllerWrapper controller = new CustomLayerControllerWrapper();
            for (Layer l : getUsedLayers(executionSteps, InputOutputProvider.INSTANCE, System.err::println) ) {
                // add new layer to this .world
                if (l instanceof CustomLayer && !controller.containsLayer(l)) {
                    ((CustomLayer) l).setPalette("MacroMachine");
                    controller.registerCustomLayer((CustomLayer) l, true);
                }
            }

            try {
                ActionFilterIO.instance.prepareForDimension(getDimension());
            } catch (Exception e) {
                ErrorPopUp(
                        "ActionFilter Preparation caused an excpetion:" + e.getMessage());
                return statistics;
            }

            // ----------------------- macro is ready and can be applied to map
            statistics = ApplyAction.applyExecutionSteps(getDimension(), executionSteps, setProgress);

            ActionFilterIO.instance.releaseAfterApplication();
            statistics.forEach(f -> GlobalActionPanel.logMessage(f.toString()));

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

   /* @Override
    public void setView(WorldPainterView view) {
        this.mWorldPainterView = view;
    }
*/
    @Override
    public void interrupt() {

    }
}
