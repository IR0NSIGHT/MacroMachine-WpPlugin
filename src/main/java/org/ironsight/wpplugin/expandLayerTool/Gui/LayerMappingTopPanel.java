package org.ironsight.wpplugin.expandLayerTool.Gui;

import org.ironsight.wpplugin.expandLayerTool.operations.IPositionValueGetter;
import org.ironsight.wpplugin.expandLayerTool.operations.IPositionValueSetter;
import org.ironsight.wpplugin.expandLayerTool.operations.LayerMapping;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.function.Consumer;

public class LayerMappingTopPanel extends JPanel {
    private Consumer<LayerMapping> onChange;
    private JTextField description;
    private JTextField nameField;
    private ActionTypeComboBox actionTypeComboBox;
    private OutputComboBox outputSelect;
    private InputGetterComboBox inputSelect;
    private LayerMapping mapping;

    public LayerMappingTopPanel() {
        init();
    }

    private void init() {
        this.setLayout(new BorderLayout());

        JPanel textInputs = new JPanel(new BorderLayout());
        this.add(textInputs, BorderLayout.NORTH);
        Font inputOutputFont = new Font("SansSerif", Font.BOLD, 24);

        nameField = new JTextField();
        nameField.setFont(inputOutputFont);
        textInputs.add(nameField, BorderLayout.WEST);
        nameField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateMapping(mapping.withName(nameField.getText()));
            }
        });
        description = new JTextField();
        description.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateMapping(mapping.withDescription(description.getText()));
            }
        });
        description.setFont(inputOutputFont);
        textInputs.add(description, BorderLayout.CENTER);


        JPanel comboboxes = new JPanel(new GridLayout(0, 3));
        this.add(comboboxes, BorderLayout.CENTER);
        inputSelect = new InputGetterComboBox();
        inputSelect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IPositionValueGetter getter = inputSelect.getSelectedProvider();
                updateMapping(mapping.withInput(getter));
            }
        });
        inputSelect.setFont(inputOutputFont);
        comboboxes.add(inputSelect);


        outputSelect = new OutputComboBox();
        comboboxes.add(outputSelect);
        outputSelect.setFont(inputOutputFont);
        outputSelect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IPositionValueSetter setter = outputSelect.getSelectedProvider();
                updateMapping(mapping.withOutput(setter));
            }
        });

        actionTypeComboBox = new ActionTypeComboBox();
        actionTypeComboBox.setFont(inputOutputFont);
        comboboxes.add(actionTypeComboBox);
        actionTypeComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateMapping(mapping.withType(actionTypeComboBox.getSelectedProvider()));
            }
        });
    }

    private void update() {
        actionTypeComboBox.setTo(mapping.getActionType());
        outputSelect.SetSelected(mapping.output);
        inputSelect.SetSelected(mapping.input);
        description.setText(mapping.getDescription());
        nameField.setText(mapping.getName());
    }

    private void updateMapping(LayerMapping layerMapping) {
        if (this.mapping != null && this.mapping.equals(layerMapping)) return;
        setMapping(layerMapping);
        onChange.accept(layerMapping);
    }

    public void setMapping(LayerMapping layerMapping) {
        if (this.mapping != null && this.mapping.equals(layerMapping)) {
            return;
        }
        this.mapping = layerMapping;
        update();
    }

    public void setOnChange(Consumer<LayerMapping> onChange) {
        this.onChange = onChange;
    }
}
