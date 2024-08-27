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
    private ArrayList<OptionsLabel> inputs = new ArrayList<>();

    public ApplyPathOperationOptionsPanel(ApplyPathOperationOptions options) {
        this.options = options;
        this.initComponents();
    }

    private void initComponents() {


        inputs.add(numericInput("final width",
                "width of the path at the end.",
                new SpinnerNumberModel(7, 0, 100, 1f),
                w -> options.setFinalWidth(w.intValue()),
                () -> (float) options.getFinalWidth(),
                this::updateGuiFromOptions));

        inputs.add(numericInput("start width",
                "width of the path at start.",
                new SpinnerNumberModel(3, 0, 100, 1f),
                w -> options.setStartWidth(w.intValue()),
                () -> (float) options.getStartWidth(),
                this::updateGuiFromOptions
        ));

        inputs.add(numericInput("random width",
                "each step the rivers radius will randomly increase or decrease. It will stay within +/- percent of the normal width.",
                new SpinnerNumberModel(3, 0, 100, 1f),
                w -> options.setRandomFluctuate(w.intValue()),
                () -> (float) options.getRandomFluctuate(),
                this::updateGuiFromOptions));

        inputs.add(numericInput("fluctuation speed",
                "how fast the random fluctuation appears. low number = less extreme change",
                new SpinnerNumberModel(1, 0, 100, 1f),
                w -> options.setFluctuationSpeed(w.intValue()),
                () -> (float) options.getFluctuationSpeed(),
                this::updateGuiFromOptions));

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

        updateGuiFromOptions();
    }

    private void updateGuiFromOptions() {
        for (OptionsLabel l : inputs)
            l.updateValueFromOption();
    }
}
