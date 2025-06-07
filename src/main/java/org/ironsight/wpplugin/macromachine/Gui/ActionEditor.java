package org.ironsight.wpplugin.macromachine.Gui;

import org.ironsight.wpplugin.macromachine.MacroMachinePlugin;
import org.ironsight.wpplugin.macromachine.operations.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.ironsight.wpplugin.macromachine.Gui.HelpDialog.getHelpButton;

public class ActionEditor extends LayerMappingPanel {
    private static JDialog dialog;
    private final Consumer<LayerMapping> onSubmit;
    private MappingGridPanel mappingDisplay;
    private MappingTextTable table;
    private LayerMappingTopPanel topBar;

    public ActionEditor(Consumer<LayerMapping> onSubmit) {
        super();
        this.onSubmit = onSubmit;
    }

    public static JDialog createDialog(JFrame parent,
                                       MacroApplicator applyToMap) {
        if (dialog != null) {
            dialog.setVisible(true);
            dialog.toFront();        // Bring it to the front
            dialog.requestFocus();   // Request focus (optional)
            return dialog;
        }

        // Create a JDialog with the parent frame
        dialog = new JDialog(parent, "My Dialog", false); // Modal dialog
        dialog.setLocationRelativeTo(null); // Centers the dialog


        dialog.add(new GlobalActionPanel(applyToMap, dialog));
        dialog.setTitle(MacroMachinePlugin.getInstance().getName() + " v" + MacroMachinePlugin.getInstance().getVersion());
        dialog.setLocationRelativeTo(parent); // Center the dialog relative to the parent frame
        dialog.pack();
        dialog.setAlwaysOnTop(false);
        return dialog;
    }

    @Override
    protected void updateComponents() {
        mappingDisplay.setVisible(!mapping.input.isDiscrete());
        table.setMapping(mapping);
        mappingDisplay.setMapping(mapping);
        topBar.setMapping(mapping);
        this.repaint();
    }

    @Override
    protected void initComponents() {
        this.setLayout(new BorderLayout());

        mappingDisplay = new MappingGridPanel();
        table = new MappingTextTable();

        //set up sync between both components
        table.setOnUpdate(this::updateMapping);
        table.setOnSelect(mappingDisplay::setSelectedInputs);

        mappingDisplay.setOnUpdate(this::updateMapping);
        mappingDisplay.setOnSelect(table::setSelectedInputs);

        topBar = new LayerMappingTopPanel();
        topBar.setOnUpdate(this::updateMapping);

        JButton submitButtom = new JButton("save");
        submitButtom.setToolTipText("Submit action and save to global list.");
        submitButtom.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onSubmit.accept(mapping);
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(submitButtom);
        buttonPanel.add(getHelpButton("Action Editor",
                "Use the Action editor the define what your action does.\n" +
                        "\nAn action is somewhat similar to a global operation: It does one thing (change the output " +
                        "value) " +
                        "for many blocks. Its more flexible than a global operation, because you dont set one output " +
                        "value, " +
                        "but many." + "Every action has an input, and output and a type.\n" +
                        "Define which value of the input maps to which value of the output. The action can " +
                        "interpolate " +
                        "between values if the input/output types allow it. This is shown by the dotted line in the " +
                        "graph.\n" +
                        "An action will take a block from the map, read its input value and then set the blocks " +
                        "output value " +
                        "to the one defined by the mapping. If the mapping is defined so that slope 45° sets pine to " +
                        "8, then " +
                        "all blocks with 45° slope will " + "receive pine level 8 when the action is executed.\n" +
                        "Actions are part of a macro and  can be reused in multiple macros at once.\n" +
                        "Save your changes to the action to save it to your global list of actions.\n" +
                        "Add new mapping points by left clicking in the graph\n" +
                        "Delete a point by rightclicking it.\n" +
                        "Change a point by dragging it or selecting different values in the table."));

        this.add(topBar, BorderLayout.NORTH);
        this.add(table, BorderLayout.EAST);
        this.add(buttonPanel, BorderLayout.SOUTH);
        this.add(mappingDisplay, BorderLayout.CENTER);
    }
}
