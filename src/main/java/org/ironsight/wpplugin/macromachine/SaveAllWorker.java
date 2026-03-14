package org.ironsight.wpplugin.macromachine;

import org.ironsight.wpplugin.macromachine.operations.FileIO.ContainerIO;
import org.ironsight.wpplugin.macromachine.operations.FileIO.ImportExportPolicy;
import org.ironsight.wpplugin.macromachine.operations.MacroContainer;
import org.ironsight.wpplugin.macromachine.operations.MappingActionContainer;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.InputOutputProvider;

import java.io.File;

public class SaveAllWorker
{
    private final MappingActionContainer actionContainer;
    private final MacroContainer macroContainer;
    private final File saveFile;
    private volatile boolean flagged = false;

    public SaveAllWorker(MacroContainer macroContainer, MappingActionContainer actionContainer, File saveFile) {
        this.macroContainer = macroContainer;
        this.actionContainer = actionContainer;
        this.saveFile = saveFile;
    }

    public synchronized void flagForSave() {
        flagged = true;
    }

    private synchronized void unflag() {
        flagged = false;
    }

    private void doSave() {
        // make clones
        var actionsContainer = this.actionContainer.copy();
        var macroContainer = this.macroContainer.copy();

        ContainerIO.exportToFile(actionsContainer, macroContainer, saveFile, new ImportExportPolicy(),
                System.err::println, InputOutputProvider.INSTANCE);
        System.out.println("Saved everything to file " + saveFile);
    }

    public void start() {
        Thread thread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                if (flagged) {
                    doSave();
                    unflag();
                }

                try {
                    Thread.sleep(50); // check interval
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        thread.setDaemon(true);
        thread.start();
    }
}
