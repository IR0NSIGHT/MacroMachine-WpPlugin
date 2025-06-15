package org.ironsight.wpplugin.macromachine.Gui;

import org.ironsight.wpplugin.macromachine.MacroMachinePlugin;
import org.ironsight.wpplugin.macromachine.operations.*;
import org.ironsight.wpplugin.macromachine.operations.FileIO.ContainerIO;
import org.ironsight.wpplugin.macromachine.operations.FileIO.ImportExportPolicy;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.EditableIO;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IPositionValueGetter;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IPositionValueSetter;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.ironsight.wpplugin.macromachine.Gui.ActionEditor.createDialog;

// top level panel that contains a selection list of macros/layers/input/output on the left, like a file browser
// and an editor for the currently selected action on the right
public class GlobalActionPanel extends JPanel implements ISelectItemCallback {
    public static final String MAPPING_EDITOR = "mappingEditor";
    public static final String INVALID_SELECTION = "invalidSelection";
    public static final String MACRO_DESIGNER = "macroDesigner";
    public static final String INPUT_OUTPUT_DESIGNER = "inputoutputdesigner";
    static final int MAX_LOG_LINES = 2000;
    static JTextArea logPanel;
    MacroTreePanel macroTreePanel;
    MacroDesigner macroDesigner;
    ActionEditor mappingEditor;
    InputOutputEditor ioEditor;
    //consumes macro to apply to map. callback for "user pressed apply-macro"
    MacroApplicator applyMacro;
    CardLayout layout;
    JPanel editorPanel;
    private UUID currentSelectedMacro;
    private UUID currentSelectedLayer;
    private SELECTION_TPYE selectionType = SELECTION_TPYE.INVALID;
    private Window dialog;
    private JTabbedPane tabbedPane;
    private boolean showTabbedPane = true;

    public GlobalActionPanel(MacroApplicator applyToMap, Window dialog) {
        this.applyMacro = applyToMap;
        this.dialog = dialog;
        init();
    }

    public static void main(String[] args) {
        File saveFile = new File("./plugins/macroMachine/mySavefile.macro");

        MacroContainer.SetInstance(new MacroContainer("./src/main/resources/DefaultMacros.json"));
        MacroContainer macros = MacroContainer.getInstance();
        MappingActionContainer.SetInstance(new MappingActionContainer("./src/main/resources/DefaultActions.json"));
        MappingActionContainer layers = MappingActionContainer.getInstance();

        ContainerIO.importFile(layers, macros, saveFile, new ImportExportPolicy(), System.err::println);

        Runnable saveEverything = () -> ContainerIO.exportFile(MappingActionContainer.getInstance(), MacroContainer.getInstance(), saveFile,
                new ImportExportPolicy(), System.err::println);
        MappingActionContainer.getInstance().subscribe(saveEverything);
        MacroContainer.getInstance().subscribe(saveEverything);
        JDialog diag = createDialog(null, (macro, setProgress) -> {
            for (int i = 0; i < 30; i++) {
                try {
                    Thread.sleep(100);
                    setProgress.accept(new ApplyAction.Progess(0, 1, 100f * i / 30f));
                } catch (InterruptedException ex) {
                    ;
                }
            }
            return Collections.emptyList();
        });
        diag.setVisible(true);
    }

    /**
     * Returns the current timestamp in a human-readable format.
     *
     * @return The current timestamp as a String in the format "yyyy-MM-dd HH:mm:ss".
     */
    public static String getCurrentTimestamp() {
        // Get the current date and time
        LocalDateTime now = LocalDateTime.now();

        // Define the formatter for the desired human-readable format
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // Format the current date and time
        return now.format(formatter);
    }

    public static void ErrorPopUp(String message) {
        logMessage(message);
        JOptionPane.showMessageDialog(null, message, "Error",
                // Title of the dialog
                JOptionPane.ERROR_MESSAGE
                // Type of message (error icon)
        );

    }

    // Method to log messages
    public static void logMessage(String message) {
        if (logPanel != null) {
            // Append the new log message
            logPanel.append(getCurrentTimestamp() + ":\n");
            logPanel.append(message + "\n");

            // Limit the number of lines in the log text area
            int lineCount = logPanel.getLineCount();
            if (lineCount > MAX_LOG_LINES) {
                try {
                    int end = logPanel.getLineEndOffset(lineCount - 1 - MAX_LOG_LINES);
                    logPanel.replaceRange("", 0, end);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // Scroll to the end
            logPanel.setCaretPosition(logPanel.getDocument().getLength());
        } else {
            System.err.println(message);
        }
        MacroMachinePlugin.error(message);
    }

    private void setProgress(ApplyAction.Progess progress) {
        logMessage("step " + progress.step + "/" + progress.totalSteps + ": " + progress.progressInStep + "%");
    }

    public void applyToMap(Macro macro, ApplyAction.Progess progess) {
        Collection<ExecutionStatistic> statistic = applyMacro.applyLayerAction(macro, this::setProgress);
        logMessage("apply macro " + macro.getName() + " to map:\n" +
                statistic.stream().map(ExecutionStatistic::toString).collect(Collectors.joining("\n")));
    }

    private void onUpdate() {
        MappingAction mapping = MappingActionContainer.getInstance().queryById(currentSelectedLayer);
        Macro macro = MacroContainer.getInstance().queryById(currentSelectedMacro);
        if (macro == null && selectionType == SELECTION_TPYE.MACRO) selectionType = SELECTION_TPYE.INVALID;

        if (mapping == null && selectionType != SELECTION_TPYE.MACRO) selectionType = SELECTION_TPYE.INVALID;

        switch (selectionType) {
            case MACRO:
                macroDesigner.setMacro(macro, true);
                layout.show(editorPanel, MACRO_DESIGNER);
                break;
            case ACTION:
                mappingEditor.setMapping(mapping);
                layout.show(editorPanel, MAPPING_EDITOR);
                break;
            case INPUT:
                ioEditor.setMapping(mapping);
                ioEditor.setIsInput(true);
                layout.show(editorPanel, INPUT_OUTPUT_DESIGNER);
                break;
            case OUTPUT:
                ioEditor.setIsInput(false);
                ioEditor.setMapping(mapping);
                layout.show(editorPanel, INPUT_OUTPUT_DESIGNER);
                break;
            case INVALID:
                layout.show(editorPanel, INVALID_SELECTION);
                break;
        }

    }

    private void init() {
        MacroContainer.getInstance().subscribe(this::onUpdate);
        MappingActionContainer.getInstance().subscribe(this::onUpdate);

        this.setLayout(new BorderLayout());
        macroTreePanel = new MacroTreePanel(MacroContainer.getInstance(),
                MappingActionContainer.getInstance(),
                applyMacro,
                this::onSelect);
        macroTreePanel.setMaximumSize(new Dimension(200, 0));

        macroDesigner = new MacroDesigner(this::onSubmitMacro);
        mappingEditor = new ActionEditor(this::onSubmitMapping);
        ioEditor = new InputOutputEditor(action -> MappingActionContainer.getInstance().updateMapping(action,
                MacroMachinePlugin::error));
        editorPanel = new JPanel(new CardLayout());
        editorPanel.add(mappingEditor, MAPPING_EDITOR);
        editorPanel.add(macroDesigner, MACRO_DESIGNER);
        editorPanel.add(ioEditor, INPUT_OUTPUT_DESIGNER);
        editorPanel.add(new JPanel(), INVALID_SELECTION);
        layout = (CardLayout) editorPanel.getLayout();
        layout.show(editorPanel, MACRO_DESIGNER);

        this.tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Designer", editorPanel);
        this.add(tabbedPane, BorderLayout.CENTER);

        JPanel executionPanel = new JPanel(new BorderLayout());
        logPanel = new JTextArea();
        logPanel.setEditable(false); // Make it read-only
        logPanel.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JScrollPane writeWindowScroll = new JScrollPane(logPanel);
        executionPanel.add(writeWindowScroll, BorderLayout.CENTER);
        writeWindowScroll.setPreferredSize(new Dimension(500, 600));

        tabbedPane.add("Log", executionPanel);

        this.add(macroTreePanel, BorderLayout.WEST);

        JButton toggleTabbedPane = new JButton("expand/shrink");
        toggleTabbedPane.addActionListener(e -> {
            showLargeVersion(!showTabbedPane);
        });
        this.add(toggleTabbedPane, BorderLayout.NORTH);
        onUpdate();
    }

    private void showLargeVersion(boolean show) {
        tabbedPane.setVisible(show);
        showTabbedPane = show;

        assert tabbedPane.getPreferredSize().height < 1000 : "debug: sometimes the preferred height is 2000 px";
        this.dialog.pack();
    }

    @Override
    public void onSelect(SaveableAction action, SELECTION_TPYE type) {
        selectionType = type;
        if (action instanceof Macro) {
            currentSelectedMacro = action.getUid();
        } else if (action instanceof MappingAction) {
            currentSelectedLayer = action.getUid();
        }
        onUpdate();
    }

    private void onSubmitMapping(MappingAction mapping) {
        MappingActionContainer.getInstance().updateMapping(mapping, f -> {
        });
    }

    private void onSubmitMacro(Macro macro) {
        MacroContainer.getInstance().updateMapping(macro, e -> {
            ErrorPopUp("Unable to save macro: " + e);
        });
    }

    private void onSubmitInputOutput(EditableIO io) {
        assert io instanceof IPositionValueGetter || io instanceof IPositionValueSetter;

    }

    enum SELECTION_TPYE {
        MACRO, ACTION, INPUT, OUTPUT, INVALID
    }

    public interface ApplyToMapCallback {
        void applyToMap(Macro macro, Consumer<ApplyAction.Progess> setProgress);

        void applyToMap(Macro macro, ApplyAction.Progess progess);
    }
}
