package org.demo.wpplugin.operations.ApplyPath;//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

import org.demo.wpplugin.operations.OptionsLabel;

import javax.swing.*;
import java.util.ArrayList;

public abstract class OperationOptionsPanel<Options> extends JPanel {
    private Options options;
    ArrayList<OptionsLabel> inputs;
    public OperationOptionsPanel(Options options) {
        this.options = options;
        displayOptions(this.options);
    }

    protected abstract ArrayList<OptionsLabel> addComponents(Options options, Runnable onOptionsReconfigured);

    private void initComponents(ArrayList<OptionsLabel> inputs) {
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        GroupLayout.ParallelGroup horizontalGroup = layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING);
        for (OptionsLabel l : inputs) {
            for (JComponent c : l.getLabels())
                horizontalGroup.addComponent(c);
            layout.setHorizontalGroup(horizontalGroup);
        }

        GroupLayout.SequentialGroup sequentialGroup = layout.createSequentialGroup();
        for (OptionsLabel l : inputs) {
            for (JComponent c : l.getLabels())
                sequentialGroup.addComponent(c);
            sequentialGroup.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED);
        }

        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(sequentialGroup
                .addGap(0, 0, 0))
        );
    }

    // Method to remove components and clean up
    public void removeComponentsAndCleanup(ArrayList<OptionsLabel> inputs) {
        if (inputs != null) {
            // Remove all components from the JPanel
            for (OptionsLabel l : inputs) {
                for (JComponent j : l.getLabels())
                    this.remove(j);
            }

            // Revalidate and repaint the panel to update the UI
            this.revalidate();
            this.repaint();
        }
    }

    public void onOptionsReconfigured() {
        removeComponentsAndCleanup(inputs);
        displayOptions(options);
    }

    private void displayOptions(Options options) {
        //clean up old components
        removeComponentsAndCleanup(inputs);

        //construct components
        inputs = addComponents(options, this::onOptionsReconfigured);
        this.revalidate();

        //add components to panel
        this.initComponents(inputs);
    }
}
