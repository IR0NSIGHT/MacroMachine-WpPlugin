package org.demo.wpplugin.operations;//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//


import javax.swing.*;

public class ApplyPathOperationOptionsPanel extends JPanel {
    private JLabel labelRandomPercent;
    private JSpinner spinnerRandomPercent;
    private JLabel labelfluctuationSpeed;
    private JSpinner spinnerfluctuationSpeed;
    private JLabel labelBaseRadius;
    private JSpinner spinnerBaseRadius;

    private JLabel labelGrowthPerStep;
    private JSpinner spinnerStepPerGrowth;
    private ApplyPathOperationOptions options;

    public ApplyPathOperationOptionsPanel(ApplyPathOperationOptions options) {
        this();
        this.setOptions(options);
    }

    public ApplyPathOperationOptionsPanel() {
        this.options = new ApplyPathOperationOptions();
        this.initComponents();
    }

    private void initComponents() {
        labelRandomPercent = new JLabel();
        labelRandomPercent.setText("random fluctuation");
        labelRandomPercent.setToolTipText("each step the rivers radius will randomly increase or decrease. It will stay within +/- percent of the normal width.");

        spinnerRandomPercent = new javax.swing.JSpinner();
        spinnerRandomPercent.setModel(new javax.swing.SpinnerNumberModel(2, 0, 100, 1));
        spinnerRandomPercent.setEnabled(true);
        spinnerRandomPercent.addChangeListener(evt -> options.setRandomFluctuate(((int) spinnerRandomPercent.getValue()) / 100d));

        labelfluctuationSpeed = new JLabel();
        labelfluctuationSpeed.setText("fluctuation speed");
        labelfluctuationSpeed.setToolTipText("how fast the random fluctuation appears. low number = less extreme change");

        spinnerfluctuationSpeed = new javax.swing.JSpinner();
        spinnerfluctuationSpeed.setModel(new javax.swing.SpinnerNumberModel(1, 0, 100, 1f));
        spinnerfluctuationSpeed.setEnabled(true);
        spinnerfluctuationSpeed.addChangeListener(evt -> options.setFluctuationSpeed(((double) spinnerfluctuationSpeed.getValue())));


        labelBaseRadius = new JLabel();
        labelBaseRadius.setText("start width");
        labelBaseRadius.setToolTipText("width of the path at start.");

        spinnerBaseRadius = new javax.swing.JSpinner();
        spinnerBaseRadius.setModel(new javax.swing.SpinnerNumberModel(3, 0, 100, 1));
        spinnerBaseRadius.setEnabled(true);
        spinnerBaseRadius.addChangeListener(evt -> options.setStartWidth((int) spinnerBaseRadius.getValue()));


        labelGrowthPerStep = new JLabel();
        labelGrowthPerStep.setText("final width");
        labelGrowthPerStep.setToolTipText("width of the path at the end.");

        spinnerStepPerGrowth = new javax.swing.JSpinner();
        spinnerStepPerGrowth.setModel(new javax.swing.SpinnerNumberModel(7, 0, 100, 1));
        spinnerStepPerGrowth.setEnabled(true);
        spinnerStepPerGrowth.addChangeListener(evt -> options.setFinalWidth((int) spinnerStepPerGrowth.getValue()));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(labelBaseRadius)
                        .addComponent(spinnerBaseRadius)
                        .addComponent(labelGrowthPerStep)
                        .addComponent(spinnerStepPerGrowth)
                        .addComponent(labelRandomPercent)
                        .addComponent(spinnerRandomPercent)
                        .addComponent(labelfluctuationSpeed)
                        .addComponent(spinnerfluctuationSpeed)
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(labelBaseRadius)
                                .addComponent(spinnerBaseRadius)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)

                                .addComponent(labelGrowthPerStep)
                                .addComponent(spinnerStepPerGrowth)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)

                                .addComponent(labelRandomPercent)
                                .addComponent(spinnerRandomPercent)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)

                                .addComponent(labelfluctuationSpeed)
                                .addComponent(spinnerfluctuationSpeed)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)

                                .addGap(0, 0, 0))
        );
    }

    private void setControlStates() {
        //this.checkBoxRemoveExistingLayers.setEnabled(this.checkBoxLayers.isSelected());
    }

    public ApplyPathOperationOptions getOptions() {
        return this.options;
    }

    public void setOptions(ApplyPathOperationOptions options) {
        this.options = options;
        //this.checkBoxHeight.setSelected(options.isCopyHeights());
        //this.checkBoxTerrain.setSelected(options.isCopyTerrain());
        //this.checkBoxFluids.setSelected(options.isCopyFluids());
        //this.checkBoxLayers.setSelected(options.isCopyLayers());
        //this.checkBoxRemoveExistingLayers.setSelected(options.isRemoveExistingLayers());
        //this.checkBoxBiomes.setSelected(options.isCopyBiomes());
        //this.checkBoxAnnotations.setSelected(options.isCopyAnnotations());
        //this.checkBoxFeather.setSelected(options.isDoBlending());
        //this.checkBoxCreateNewTiles.setSelected(options.isCreateNewTiles());
        //this.setControlStates();
    }

}
