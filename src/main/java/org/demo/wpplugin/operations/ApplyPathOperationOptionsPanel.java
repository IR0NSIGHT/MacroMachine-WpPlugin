package org.demo.wpplugin.operations;//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;

public class ApplyPathOperationOptionsPanel extends JPanel {
    private JSpinner spinnerGrowthPerStep;
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
        spinnerGrowthPerStep = new javax.swing.JSpinner();
        spinnerGrowthPerStep.setFont(spinnerGrowthPerStep.getFont().deriveFont(spinnerGrowthPerStep.getFont().getSize() - 1f));
        spinnerGrowthPerStep.setModel(new javax.swing.SpinnerNumberModel(0, 0, 255, .01));
        spinnerGrowthPerStep.setEnabled(false);
        spinnerGrowthPerStep.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                options.setGrowthPerStep((float) spinnerGrowthPerStep.getValue());
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(spinnerGrowthPerStep)
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(spinnerGrowthPerStep)
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
