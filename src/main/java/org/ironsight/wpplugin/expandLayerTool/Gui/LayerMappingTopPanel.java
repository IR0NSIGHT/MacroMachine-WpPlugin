package org.ironsight.wpplugin.expandLayerTool.Gui;

import org.ironsight.wpplugin.expandLayerTool.operations.ValueProviders.IPositionValueGetter;
import org.ironsight.wpplugin.expandLayerTool.operations.ValueProviders.IPositionValueSetter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LayerMappingTopPanel extends LayerMappingPanel {
    public static final Font header1Font = new Font("SansSerif", Font.BOLD, 24);
    public static final Font header2Font = new Font("SansSerif", Font.BOLD, 18);
    private JTextField description;
    private JTextField nameField;
    private ActionTypeComboBox actionTypeComboBox;
    private OutputComboBox outputSelect;
    private InputGetterComboBox inputSelect;

    public LayerMappingTopPanel() {
        super();
    }

    @Override
    protected void updateComponents() {
        actionTypeComboBox.setTo(mapping.getActionType());
        outputSelect.SetSelected(mapping.output);
        inputSelect.SetSelected(mapping.input);
        description.setText(mapping.getDescription());
        nameField.setText(mapping.getName());
        this.repaint();
    }
    boolean isInit = true;

    @Override
    protected void initComponents() {
        this.setLayout(new BorderLayout());

        JPanel textInputs = new JPanel(new BorderLayout());
        this.add(textInputs, BorderLayout.NORTH);

        nameField = new JTextField();
        nameField.setFont(header1Font);
        textInputs.add(nameField, BorderLayout.WEST);
        nameField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isInit)
                    return;
                updateMapping(mapping.withName(nameField.getText()));
            }
        });
        description = new JTextField();
        description.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isInit)
                    return;
                updateMapping(mapping.withDescription(description.getText()));
            }
        });
        description.setFont(header2Font);
        textInputs.add(description, BorderLayout.CENTER);


        JPanel comboboxes = new JPanel(new GridLayout(0, 3));
        this.add(comboboxes, BorderLayout.CENTER);
        inputSelect = new InputGetterComboBox();
        inputSelect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isInit)
                    return;
                IPositionValueGetter getter = inputSelect.getSelectedProvider();
                updateMapping(mapping.withInput(getter));
            }
        });
        inputSelect.setFont(header2Font);
        comboboxes.add(inputSelect);


        outputSelect = new OutputComboBox();
        comboboxes.add(outputSelect);
        outputSelect.setFont(header2Font);
        outputSelect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isInit)
                    return;
                IPositionValueSetter setter = outputSelect.getSelectedProvider();
                updateMapping(mapping.withOutput(setter));
            }
        });

        actionTypeComboBox = new ActionTypeComboBox();
        actionTypeComboBox.setFont(header2Font);
        comboboxes.add(actionTypeComboBox);
        actionTypeComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isInit)
                    return;
                updateMapping(mapping.withType(actionTypeComboBox.getSelectedProvider()));
            }
        });
        isInit = false;
    }

}
