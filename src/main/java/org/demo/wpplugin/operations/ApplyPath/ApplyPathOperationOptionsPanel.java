package org.demo.wpplugin.operations.ApplyPath;//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

import org.demo.wpplugin.operations.OptionsLabel;

import javax.swing.*;
import java.util.ArrayList;

import static org.demo.wpplugin.operations.OptionsLabel.numericInput;

public class ApplyPathOperationOptionsPanel extends JPanel {
    private ApplyPathOperationOptions options;
    ArrayList<OptionsLabel> inputs;
    public ApplyPathOperationOptionsPanel(ApplyPathOperationOptions options) {
        this.options = options;
        displayOptions(this.options);
    }

    private static ArrayList<OptionsLabel> addComponents(ApplyPathOperationOptions options, Runnable updateGuiFromOptions) {
        ArrayList<OptionsLabel> inputs = new ArrayList<>();

        inputs.add(numericInput("final width",
                "width of the path at the end.",
                new SpinnerNumberModel(options.getFinalWidth(), 0, 100, 1f),
                w -> options.setFinalWidth(w.intValue()),
                () -> (float) options.getFinalWidth(),
                updateGuiFromOptions));

        inputs.add(numericInput("start width",
                "width of the path at start.",
                new SpinnerNumberModel(options.getStartWidth(), 0, 100, 1f),
                w -> options.setStartWidth(w.intValue()),
                () -> (float) options.getStartWidth(),
                updateGuiFromOptions
        ));

        inputs.add(numericInput("random width",
                "each step the rivers radius will randomly increase or decrease. It will stay within +/- percent of the normal width.",
                new SpinnerNumberModel(options.getRandomFluctuate(), 0, 100, 1f),
                w -> options.setRandomFluctuate(w.intValue()),
                () -> (float) options.getRandomFluctuate(),
                updateGuiFromOptions));

        inputs.add(numericInput("fluctuation speed",
                "how fast the random fluctuation appears. low number = less extreme change",
                new SpinnerNumberModel(options.getFluctuationSpeed(), 0, 100, 1f),
                w -> options.setFluctuationSpeed(w.intValue()),
                () -> (float) options.getFluctuationSpeed(),
                updateGuiFromOptions));

        return inputs;
    }

    private void initComponents(ArrayList<OptionsLabel> inputs) {
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        GroupLayout.ParallelGroup group = layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING);
        for (OptionsLabel l : inputs)
            group.addComponent(l.getLabel()).addComponent(l.getSpinner());
        layout.setHorizontalGroup(group);

        GroupLayout.SequentialGroup sequentialGroup = layout.createSequentialGroup();
        for (OptionsLabel l : inputs)
            sequentialGroup.addComponent(l.getLabel()).addComponent(l.getSpinner()).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED);

        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(sequentialGroup
                .addGap(0, 0, 0))
        );
    }

    // Method to remove components and clean up
    public void removeComponentsAndCleanup(ArrayList<OptionsLabel> inputs) {
        if (inputs != null) {
            // Remove all components from the JPanel
            for (OptionsLabel l : inputs) {
                this.remove(l.getLabel());
                this.remove(l.getSpinner());
            }

            // Revalidate and repaint the panel to update the UI
            this.revalidate();
            this.repaint();
        }
    }

    private void onOptionsReconfigured() {
        removeComponentsAndCleanup(inputs);
        displayOptions(options);
    }

    private void displayOptions(ApplyPathOperationOptions options) {
        //clean up old components
        removeComponentsAndCleanup(inputs);

        //construct components
        inputs = addComponents(options, this::onOptionsReconfigured);

        //add components to panel
        this.initComponents(inputs);
    }
}
