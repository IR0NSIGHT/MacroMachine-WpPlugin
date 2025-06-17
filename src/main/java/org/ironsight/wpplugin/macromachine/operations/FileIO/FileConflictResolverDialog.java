package org.ironsight.wpplugin.macromachine.operations.FileIO;

import org.ironsight.wpplugin.macromachine.Gui.MacroTreePanel;
import org.ironsight.wpplugin.macromachine.Gui.SaveableActionRenderer;
import org.ironsight.wpplugin.macromachine.operations.Macro;
import org.ironsight.wpplugin.macromachine.operations.MappingAction;
import org.ironsight.wpplugin.macromachine.operations.SaveableAction;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.AnnotationSetter;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.NibbleLayerSetter;
import org.pepsoft.worldpainter.layers.PineForest;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class FileConflictResolverDialog extends JDialog {

    private boolean overwrite = false;
    private boolean remember = false;

    public FileConflictResolverDialog(JFrame owner, SaveableAction original, SaveableAction imported) {
        // Create a modal dialog
        super(owner, "Resolve " +
                        "Import Conflict"
                , true);
        init(owner, original, imported);
    }
    public FileConflictResolverDialog(JDialog owner, SaveableAction original, SaveableAction imported) {
        // Create a modal dialog
        super(owner, "Resolve " +
                        "Import Conflict"
                , true);
        init(owner, original, imported);
    }

    public static void main(String[] args) {
        // Example usage
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(400, 300);
            frame.setVisible(true);

            MappingAction a = MappingAction.getNewEmptyAction()
                    .withName("Paint trees on cyan v1")
                    .withInput(new AnnotationSetter())
                    .withOutput(new NibbleLayerSetter(
                            PineForest.INSTANCE, false));
            FileConflictResolverDialog dialog = new FileConflictResolverDialog(frame,
                    a,a.withName("Paint trees on cyan v2"));
            dialog.setVisible(true);
        });
    }

    private void init(Component owner, SaveableAction original, SaveableAction imported) {
        setMinimumSize(new Dimension(300, 150));

        setLayout(new BorderLayout());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 20, 10, 20));
        this.add(panel);

        JPanel center = new JPanel(new GridLayout(3, 1));
        JPanel originalPanel = (JPanel) new SaveableActionRenderer(MacroTreePanel::isValidItem).renderFor(original, false);
        originalPanel.setBorder(BorderFactory.createTitledBorder("Existing:"));
        JPanel importedPanel = (JPanel) new SaveableActionRenderer(MacroTreePanel::isValidItem).renderFor(imported, false);
        importedPanel.setBorder(BorderFactory.createTitledBorder("Imported:"));

        JLabel conflictLabel =
                new JLabel(
                        "The imported " + (original instanceof Macro ? "macro" : "action") + " already exists in your " +
                                "savefile:",
                        SwingConstants.CENTER);
        center.add(conflictLabel);
        center.add(originalPanel);
        center.add(importedPanel);
        // Create a panel for the buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        // Create the Overwrite button
        JButton overwriteButton = new JButton("Overwrite existing");
        overwriteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                overwrite = true;
                dispose(); // Close the dialog
            }
        });

        // Create the Skip button
        JButton skipButton = new JButton("Skip imported");
        skipButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                overwrite = false;
                dispose(); // Close the dialog
            }
        });

        JCheckBox remeberButton = new JCheckBox("apply to all remaining conflicts");

        remeberButton.addActionListener(a -> {
            remember = remeberButton.isSelected();
        });
        // Add buttons to the button panel
        buttonPanel.add(overwriteButton);
        buttonPanel.add(skipButton);
        buttonPanel.add(remeberButton);

        // Add components to the dialog
        panel.add(center, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        // Center the dialog relative to the owner frame
        setLocationRelativeTo(owner);
        pack();
    }

    public boolean isOverwrite() {
        return overwrite;
    }

    public boolean isRemember() {
        return remember;
    }
}
