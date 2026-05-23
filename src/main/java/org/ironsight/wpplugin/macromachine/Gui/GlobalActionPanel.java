package org.ironsight.wpplugin.macromachine.Gui;

import static org.ironsight.wpplugin.macromachine.Gui.MacroMachineWindow.createDialog;

import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.function.Consumer;
import javax.swing.*;
import org.ironsight.wpplugin.macromachine.Gui.EditActions.ActionDesigner;
import org.ironsight.wpplugin.macromachine.Gui.EditActions.InputOutputEditor;
import org.ironsight.wpplugin.macromachine.Gui.TreeView.MacroTreePanel;
import org.ironsight.wpplugin.macromachine.MacroMachinePlugin;
import org.ironsight.wpplugin.macromachine.SaveAllWorker;
import org.ironsight.wpplugin.macromachine.WebUIServer;
import org.ironsight.wpplugin.macromachine.operations.*;
import org.ironsight.wpplugin.macromachine.operations.ApplyToMap.ApplyAction;
import org.ironsight.wpplugin.macromachine.operations.FileIO.ContainerIO;
import org.ironsight.wpplugin.macromachine.operations.FileIO.ImportExportPolicy;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.EditableIO;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IPositionValueGetter;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IPositionValueSetter;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.InputOutputProvider;
import org.ironsight.wpplugin.macromachine.threeDRendering.SurfaceObject;
import org.pepsoft.worldpainter.dynmap.DynmapPreviewer;
import org.pepsoft.worldpainter.objects.WPObject;

// top level panel that contains a selection list of macros/layers/input/output on the left, like a
// file browser
// and an editor for the currently selected action on the right
public class GlobalActionPanel extends JPanel implements ISelectItemCallback {
  public static final String MAPPING_EDITOR = "mappingEditor";
  public static final String INVALID_SELECTION = "invalidSelection";
  public static final String MACRO_DESIGNER = "macroDesigner";
  public static final String INPUT_OUTPUT_DESIGNER = "inputoutputdesigner";
  static final int MAX_LOG_LINES = 2000;
  static JTextArea logPanel;
  private static DynmapPreviewer previewer = null;
  private static GlobalActionPanel INSTANCE;
  private static WPObject surfaceObject = new SurfaceObject();
  MacroTreePanel macroTreePanel;
  MacroDesigner macroDesigner;
  ActionDesigner mappingEditor;
  InputOutputEditor ioEditor;
  // consumes macro to apply to map. callback for "user pressed apply-macro"
  MacroApplicator applyMacro;
  CardLayout layout;
  JPanel editorPanel;
  private UUID currentSelectedMacro;
  private UUID currentSelectedLayer;
  private SELECTION_TPYE selectionType = SELECTION_TPYE.INVALID;
  private Window dialog;
  private JTabbedPane tabbedPane;
  private boolean showTabbedPane = true;
  private boolean rerender3d = false;
  private Dimension[] expandedStateSizes = new Dimension[2];

  public GlobalActionPanel(MacroApplicator applyToMap, Window dialog) {
    INSTANCE = this;
    this.applyMacro = applyToMap;
    this.dialog = dialog;
    init();
  }

  public static void main(String[] args) {
    MacroContainer macros = new MacroContainer("./src/main/resources/DefaultMacros.json");
    MacroContainer.SetInstance(macros);
    MappingActionContainer actions =
        new MappingActionContainer("./src/main/resources/DefaultActions.json");
    MappingActionContainer.SetInstance(actions);

    MacroApplicator applicator = MacroApplicator.mock();

    // Start web UI server
    WebUIServer server = new WebUIServer(applicator, actions, macros);
    try {
      server.start();
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }

    File saveFile = new File("./plugins/macroMachine/mySavefile.macro");

    MappingActionContainer layers = MappingActionContainer.getInstance();

    ContainerIO.importFile(
        layers,
        macros,
        saveFile,
        new ImportExportPolicy(),
        s -> ErrorPopUpString("Can not load from savefile:\n" + saveFile.getPath() + "\n" + s),
        InputOutputProvider.INSTANCE);

    SaveAllWorker fileWriter =
        new SaveAllWorker(
            MacroContainer.getInstance(), MappingActionContainer.getInstance(), saveFile);
    fileWriter.start();

    MappingActionContainer.getInstance().subscribe(fileWriter::flagForSave);
    MacroContainer.getInstance().subscribe(fileWriter::flagForSave);
    InputOutputProvider.INSTANCE.updateFrom(null);
    // Create and show a JFrame
    JFrame frame = new JFrame("Main Window");
    frame.setSize(500, 500);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setVisible(true);

    JFrame diag = createDialog(frame, applicator);
    diag.setVisible(true);

    // Stop server on close
    diag.addWindowListener(
        new WindowAdapter() {
          @Override
          public void windowClosing(WindowEvent e) {
            server.stop();
          }
        });
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

  public static void ErrorPopUpString(String message) {
    logMessage(message);
    JOptionPane.showMessageDialog(
        null,
        message,
        "Error",
        // Title of the dialog
        JOptionPane.ERROR_MESSAGE
        // Type of message (error icon)
        );
  }

  public static void ErrorPopUp(Exception ex) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    ex.printStackTrace(pw);
    String message = sw.toString();
    logMessage(message);
    JOptionPane.showMessageDialog(
        null,
        message,
        "Error",
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

  private static DynmapPreviewer getPreviewer() {
    if (previewer == null) previewer = new DynmapPreviewer();
    return previewer;
  }

  public static void flagForChangedSurfaceObject() {
    if (INSTANCE == null) return; // gui wasnt opened before.
    INSTANCE.rerender3d = true;
    if (getPreviewer().isShowing())
      SwingUtilities.invokeLater(
          () -> {
            INSTANCE.doRender3d();
          });
  }

  public static WPObject getSurfaceObject() {
    return surfaceObject;
  }

  public static void setSurfaceObject(WPObject surfaceObject) {
    GlobalActionPanel.surfaceObject = surfaceObject;
  }

  private void onUpdate() {
    MappingAction mapping = MappingActionContainer.getInstance().queryById(currentSelectedLayer);
    Macro macro = MacroContainer.getInstance().queryById(currentSelectedMacro);
    if (macro == null && selectionType == SELECTION_TPYE.MACRO)
      selectionType = SELECTION_TPYE.INVALID;

    if (mapping == null
        && (selectionType == SELECTION_TPYE.ACTION
            || selectionType == SELECTION_TPYE.INPUT
            || selectionType == SELECTION_TPYE.OUTPUT)) selectionType = SELECTION_TPYE.INVALID;
    // FIXME: notify MacroTreePanel that an item was selected.

    switch (selectionType) {
      case MACRO:
        macroDesigner.onUserSelectedMacro(macro);
        layout.show(editorPanel, MACRO_DESIGNER);
        macroTreePanel.selectItemInTree(macro);
        break;
      case ACTION:
        mappingEditor.setMapping(mapping);
        layout.show(editorPanel, MAPPING_EDITOR);
        macroTreePanel.selectItemInTree(mapping);
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

  private void doRender3d() {

    getPreviewer().setObject(getSurfaceObject(), null); // immediate redraw
    rerender3d = false;
  }

  private void init() {
    MacroContainer macroContainer = MacroContainer.getInstance();
    MappingActionContainer actionContainer = MappingActionContainer.getInstance();

    // macroContainer.subscribe(() -> SwingUtilities.invokeLater(this::onUpdate));
    // actionContainer.subscribe(() -> SwingUtilities.invokeLater(this::onUpdate));

    this.setLayout(new BorderLayout());
    macroTreePanel =
        new MacroTreePanel(
            MacroContainer.getInstance(),
            MappingActionContainer.getInstance(),
            applyMacro,
            this::onSelect);
    macroTreePanel.setMaximumSize(new Dimension(200, 0));

    macroDesigner = new MacroDesigner(actionContainer, this::onSubmitMacro, macroContainer, this);
    mappingEditor = new ActionDesigner(this::onSubmitMapping);
    ioEditor =
        new InputOutputEditor(
            action ->
                MappingActionContainer.getInstance()
                    .updateMapping(action, GlobalActionPanel::ErrorPopUpString));
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

    JPanel logPanel = new JPanel(new BorderLayout());
    GlobalActionPanel.logPanel = new JTextArea("");
    GlobalActionPanel.logPanel.setEditable(false); // Make it read-only
    GlobalActionPanel.logPanel.setFont(new Font("Monospaced", Font.PLAIN, 12));

    JScrollPane writeWindowScroll = new JScrollPane(GlobalActionPanel.logPanel);
    logPanel.add(writeWindowScroll, BorderLayout.CENTER);
    writeWindowScroll.setPreferredSize(new Dimension(500, 600));

    tabbedPane.add("log", logPanel);

    getPreviewer().setInclination(30);
    getPreviewer().setObject(new SurfaceObject() /* empty dummy */, null);
    tabbedPane.add("3d", getPreviewer());

    tabbedPane.addTab("Web UI", new WebUIViewPanel());

    previewer.addHierarchyListener(
        e -> {
          if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0
              && isShowing()
              && rerender3d) {
            SwingUtilities.invokeLater(() -> doRender3d());
          }
        });

    this.add(macroTreePanel, BorderLayout.WEST);

    JButton toggleTabbedPane = new JButton("expand/shrink");
    toggleTabbedPane.addActionListener(
        e -> {
          showLargeVersion(!showTabbedPane);
          toggleTabbedPane.setText(!showTabbedPane ? "expand" : "shrink");
        });

    JButton alwaysOnTopButton = new JButton("on top");
    alwaysOnTopButton.addActionListener(
        e -> {
          MacroMachineWindow window = MacroMachineWindow.getDialog();
          boolean target = !window.isStayOnTop();
          alwaysOnTopButton.setText(!target ? "set: on top" : "set: not on top");
          window.setStayOnTop(target);
        });

    JPanel topButtons = new JPanel(new FlowLayout());
    topButtons.add(alwaysOnTopButton);
    topButtons.add(toggleTabbedPane);
    this.add(topButtons, BorderLayout.NORTH);

    onUpdate();
  }

  private void showLargeVersion(boolean expanded) {
    // save current state

    expandedStateSizes[expanded ? 0 : 1] = this.dialog.getSize();
    tabbedPane.setVisible(expanded);
    showTabbedPane = expanded;

    assert tabbedPane.getPreferredSize().height < 1000
        : "debug: sometimes the preferred height is 2000 px";
    Dimension desired = expandedStateSizes[expanded ? 1 : 0];
    if (desired == null) this.dialog.pack();
    else this.dialog.setSize(desired);
  }

  @Override
  public void onSelect(SaveableAction action, SELECTION_TPYE type) {
    selectionType = type;
    switch (selectionType) {
      case MACRO:
        currentSelectedMacro = action.getUid();
      case ACTION:
        currentSelectedLayer = action.getUid();
    }
    onUpdate();
  }

  private void onSubmitMapping(MappingAction mapping) {
    MappingActionContainer.getInstance().updateMapping(mapping, f -> {});
  }

  private void onSubmitMacro(Macro macro) {
    MacroContainer.getInstance()
        .updateMapping(
            macro,
            e -> {
              ErrorPopUpString("Unable to save macro: " + e);
            });
  }

  private void onSubmitInputOutput(EditableIO io) {
    assert io instanceof IPositionValueGetter || io instanceof IPositionValueSetter;
  }

  public enum SELECTION_TPYE {
    MACRO,
    ACTION,
    INPUT,
    OUTPUT,
    INVALID,
    NONE
  }

  public interface ApplyToMapCallback {
    void applyToMap(Macro macro, Consumer<ApplyAction.Progess> setProgress);

    void applyToMap(Macro macro, ApplyAction.Progess progess);
  }
}
