package org.ironsight.wpplugin.macromachine.operations;

import static org.ironsight.wpplugin.macromachine.Gui.MacroMachineWindow.createDialog;

import java.io.File;
import java.io.IOException;
import java.util.*;
import javax.swing.*;
import org.ironsight.wpplugin.macromachine.Gui.GlobalActionPanel;
import org.ironsight.wpplugin.macromachine.WebUIServer;
import org.ironsight.wpplugin.macromachine.operations.FileIO.ContainerIO;
import org.ironsight.wpplugin.macromachine.operations.FileIO.ImportExportPolicy;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.InputOutputProvider;
import org.pepsoft.worldpainter.operations.AbstractBrushOperation;

public class MacroDialogOperation extends AbstractBrushOperation
{
    private static final String NAME = "Macro Operation";
    private static final String DESCRIPTION = "Create complex reusable global operations to automate your workflow.";
    private MacroConcurrentApplicator applicator;

    public MacroDialogOperation() {
        super(NAME, DESCRIPTION, "macrooperation"); // one shot op
        File saveFile = new File(MacroContainer.getActionsFilePath() + "/savefile.macro");

        MacroContainer.SetInstance(new MacroContainer(null));
        MacroContainer macros = MacroContainer.getInstance();
        MappingActionContainer actions = new MappingActionContainer(null);
        MappingActionContainer.SetInstance(actions);

        applicator = new MacroConcurrentApplicator(macros, actions, this::getDimension, uuid -> {
        });
        applicator.start();

        WebUIServer server = new WebUIServer(applicator, MappingActionContainer.getInstance(), macros,
                InputOutputProvider.INSTANCE);
        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        MappingActionContainer layers = MappingActionContainer.getInstance();

        ContainerIO.importFile(layers, macros, saveFile, new ImportExportPolicy(),
                s -> GlobalActionPanel
                        .ErrorPopUpString("Can not load from savefile:\n" + saveFile.getPath() + "\n" + s),
                InputOutputProvider.INSTANCE);

        Runnable saveEverything = () -> ContainerIO.exportToFile(MappingActionContainer.getInstance(),
                MacroContainer.getInstance(), saveFile, new ImportExportPolicy(), System.err::println,
                InputOutputProvider.INSTANCE);

        MappingActionContainer.getInstance().subscribe(saveEverything);
        MacroContainer.getInstance().subscribe(saveEverything);
    }

    public void openDialog() {
        try {
            InputOutputProvider.INSTANCE.updateFrom(getDimension());
            JFrame dialog = createDialog(null, applicator);
            dialog.toFront(); // Bring it to the front
            dialog.requestFocusInWindow();
            dialog.setVisible(true);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
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
