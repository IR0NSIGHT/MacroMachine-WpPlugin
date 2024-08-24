package org.demo.wpplugin.operations;//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//


import javax.swing.*;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ApplyPathOperationOptionsPanel extends JPanel {
    private JLabel labelRandomPercent;
    private JSpinner spinnerRandomPercent;
    private JLabel labelfluctuationSpeed;
    private JSpinner spinnerfluctuationSpeed;

    private JLabel labelGrowthPerStep;
    private JSpinner spinnerStepPerGrowth;
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
                () -> (float) options.getFinalWidth()));

        inputs.add(numericInput("start width",
                "width of the path at start.",
                new SpinnerNumberModel(3, 0, 100, 1f),
                w -> options.setStartWidth(w.intValue()),
                () -> (float) options.getStartWidth()
        ));

        inputs.add(numericInput("random width",
                "each step the rivers radius will randomly increase or decrease. It will stay within +/- percent of the normal width.",
                new SpinnerNumberModel(3, 0, 100, 1f),
                w -> options.setRandomFluctuate(w.intValue()),
                () -> (float) options.getRandomFluctuate()));

        inputs.add(numericInput("fluctuation speed",
                "how fast the random fluctuation appears. low number = less extreme change",
                new SpinnerNumberModel(1, 0, 100, 1f),
                w -> options.setFluctuationSpeed(w.intValue()),
                () -> (float) options.getFluctuationSpeed()));



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

    private OptionsLabel numericInput(String text, String tooltip, SpinnerNumberModel number, Consumer<Float> setOptionValue, Supplier<Float> getOptionValue) {
        JLabel textL = new JLabel();
        textL.setText(text);
        textL.setToolTipText(tooltip);

        JSpinner spinner = new JSpinner();
        spinner.setModel(number);
        spinner.setEnabled(true);
        spinner.addChangeListener(
                evt -> {
                    setOptionValue.accept(((Double) spinner.getValue()).floatValue());
                    updateGuiFromOptions();
                }
        );

        OptionsLabel oL = new OptionsLabel() {
            @Override
            public JLabel getLabel() {
                return textL;
            }

            @Override
            public JSpinner getSpinner() {
                return spinner;
            }

            @Override
            public void updateValueFromOption() {
                spinner.setValue(getOptionValue.get().doubleValue());
            }
        };
        return oL;
    }

    private void updateGuiFromOptions() {
        for (OptionsLabel l : inputs)
            l.updateValueFromOption();
    }

    interface OptionsLabel {
        JLabel getLabel();

        JSpinner getSpinner();

        void updateValueFromOption();
    }

}
