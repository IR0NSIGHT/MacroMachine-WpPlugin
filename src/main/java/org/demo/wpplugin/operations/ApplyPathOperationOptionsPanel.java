package org.demo.wpplugin.operations;//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//


import javax.swing.*;

public class ApplyPathOperationOptionsPanel extends JPanel {
    private JLabel labelRandomPercent;
    private JSpinner spinnerRandomPercent;

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
        labelRandomPercent.setToolTipText("each step the rivers radius will randomly increase or decrease by x percent. Be very conservative because of compounding effects.");

        spinnerRandomPercent = new javax.swing.JSpinner();
        spinnerRandomPercent.setModel(new javax.swing.SpinnerNumberModel(2, 0, 100, 1));
        spinnerRandomPercent.setEnabled(true);
        spinnerRandomPercent.addChangeListener(evt -> options.setRandomFluctuate(((int) spinnerRandomPercent.getValue()) / 100d));


        labelBaseRadius = new JLabel();
        labelBaseRadius.setText("base radius");
        labelBaseRadius.setToolTipText("base number how thick the path will be. growth and random fluctuation is added on top");

        spinnerBaseRadius = new javax.swing.JSpinner();
        spinnerBaseRadius.setModel(new javax.swing.SpinnerNumberModel(3, 0, 100, 1));
        spinnerBaseRadius.setEnabled(true);
        spinnerBaseRadius.addChangeListener(evt -> options.setBaseRadius((int) spinnerBaseRadius.getValue()));


        labelGrowthPerStep = new JLabel();
        labelGrowthPerStep.setText("steps per growth");
        labelGrowthPerStep.setToolTipText("how many steps it along the path it takes for an increase of 1 in width. Zero for ignore");

        spinnerStepPerGrowth = new javax.swing.JSpinner();
        spinnerStepPerGrowth.setModel(new javax.swing.SpinnerNumberModel(500, 0, 1000, 1));
        spinnerStepPerGrowth.setEnabled(true);
        spinnerStepPerGrowth.addChangeListener(evt -> options.setStepsPerGrowth((int) spinnerStepPerGrowth.getValue()));

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
