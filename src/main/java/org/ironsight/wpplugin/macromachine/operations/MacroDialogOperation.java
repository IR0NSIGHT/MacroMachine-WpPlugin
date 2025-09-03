package org.ironsight.wpplugin.macromachine.operations;

import org.ironsight.wpplugin.macromachine.Gui.GlobalActionPanel;
import org.ironsight.wpplugin.macromachine.operations.ApplyToMap.ApplyAction;
import org.ironsight.wpplugin.macromachine.operations.ApplyToMap.ApplyActionCallback;
import org.ironsight.wpplugin.macromachine.operations.FileIO.ContainerIO;
import org.ironsight.wpplugin.macromachine.operations.FileIO.ImportExportPolicy;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.ActionFilterIO;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.InputOutputProvider;
import org.pepsoft.worldpainter.CustomLayerControllerWrapper;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.operations.*;

import javax.swing.*;
import java.io.File;
import java.util.*;

import static org.ironsight.wpplugin.macromachine.Gui.MacroMachineWindow.createDialog;

public class MacroDialogOperation extends AbstractBrushOperation implements MacroApplicator {
    private static final String NAME = "Macro Operation";
    private static final String DESCRIPTION = "Create complex reusable global operations to automate your workflow.";

    public MacroDialogOperation() {
        super(NAME, DESCRIPTION, "macrooperation"); //one shot op
        File saveFile = new File(MacroContainer.getActionsFilePath() + "/savefile.macro");

        MacroContainer.SetInstance(new MacroContainer(null));
        MacroContainer macros = MacroContainer.getInstance();
        MappingActionContainer.SetInstance(new MappingActionContainer(null));
        MappingActionContainer layers = MappingActionContainer.getInstance();

        ContainerIO.importFile(layers, macros, saveFile, new ImportExportPolicy(),
                s -> GlobalActionPanel.ErrorPopUpString("Can not load from savefile:\n" + saveFile.getPath() + "\n" + s), InputOutputProvider.INSTANCE);

        Runnable saveEverything = () -> ContainerIO.exportToFile(MappingActionContainer.getInstance(), MacroContainer.getInstance(), saveFile,
                new ImportExportPolicy(), System.err::println, InputOutputProvider.INSTANCE);

        MappingActionContainer.getInstance().subscribe(saveEverything);
        MacroContainer.getInstance().subscribe(saveEverything);
    }

    public void openDialog() {
        try {
            InputOutputProvider.INSTANCE.updateFrom(getDimension());
            JFrame dialog = createDialog(null, this);
            dialog.toFront();              // Bring it to the front
            dialog.requestFocusInWindow();
            dialog.setVisible(true);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public Collection<ExecutionStatistic> applyLayerAction(Macro macro,
                                                           ApplyActionCallback callback) {
        ApplyAction.ApplicationContext context = new ApplyAction.ApplicationContext(
                getDimension(),
                MacroContainer.getInstance(),
                MappingActionContainer.getInstance(),
                InputOutputProvider.INSTANCE,
                new CustomLayerControllerWrapper(),
                ActionFilterIO.instance
        );

        return Macro.applyMacroToDimension(context, macro, callback);
    }

    @Override
    protected void activate() {
        openDialog();
    }

    @Override
    protected void deactivate() {

    }

    @Override
    protected void tick(int centreX, int centreY, boolean inverse, boolean first, float dynamicLevel) {

    }

    @Override
    public void interrupt() {

    }
}
