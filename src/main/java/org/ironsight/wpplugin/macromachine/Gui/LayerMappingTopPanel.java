package org.ironsight.wpplugin.macromachine.Gui;

import org.ironsight.wpplugin.macromachine.operations.LayerMapping;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

public class LayerMappingTopPanel extends LayerMappingPanel {
    public static final Font header1Font = new Font("SansSerif", Font.BOLD, 24);
    public static final Font header2Font = new Font("SansSerif", Font.BOLD, 18);
    boolean isInit;
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

    private void updateFromInputs() {
        if (!isInit && isAllowEvents() && mapping != null) {
            LayerMapping newMap = mapping.withName(nameField.getText())
                    .withDescription(description.getText()).withType(actionTypeComboBox.getSelectedProvider());
            if (!inputSelect.getSelectedProvider().getName().equals(mapping.input.getName()))  // rebuild map
                newMap = newMap.withInput(inputSelect.getSelectedProvider())
                        .withNewPoints(mapping.getMappingPoints());
            if (!outputSelect.getSelectedProvider().getName().equals(mapping.output.getName()))  // rebuild map
                newMap = newMap.withOutput(outputSelect.getSelectedProvider())
                        .withNewPoints(mapping.getMappingPoints());

            updateMapping(newMap);
        }
    }


    @Override
    protected void initComponents() {
        isInit = true;
        this.setLayout(new BorderLayout());

        JPanel textInputs = new JPanel(new BorderLayout());
        this.add(textInputs, BorderLayout.NORTH);

        nameField = new JTextField();
        nameField.setFont(header1Font);
        textInputs.add(nameField, BorderLayout.WEST);
        nameField.setColumns(10);
        nameField.setBorder(BorderFactory.createTitledBorder("Action name"));

        nameField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                super.focusLost(e);
                if (isAllowEvents()) SwingUtilities.invokeLater(() -> updateFromInputs());
            }
        });

        description = new JTextField();
        description.setBorder(BorderFactory.createTitledBorder("Description"));

        description.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                super.focusLost(e);
                if (isAllowEvents()) SwingUtilities.invokeLater(() -> updateFromInputs());
            }
        });
        description.setFont(header2Font);
        textInputs.add(description, BorderLayout.CENTER);


        JPanel comboboxes = new JPanel(new GridLayout(0, 3));
        this.add(comboboxes, BorderLayout.CENTER);
        inputSelect = new InputGetterComboBox();
        inputSelect.setBorder(BorderFactory.createTitledBorder("Input value"));
        inputSelect.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (isAllowEvents()) SwingUtilities.invokeLater(() -> updateFromInputs());
            }
        });
        inputSelect.setFont(header2Font);
        comboboxes.add(inputSelect);


        actionTypeComboBox = new ActionTypeComboBox();
        actionTypeComboBox.setBorder(BorderFactory.createTitledBorder("Action Type"));
        actionTypeComboBox.setFont(header2Font);
        comboboxes.add(actionTypeComboBox);
        actionTypeComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isAllowEvents()) SwingUtilities.invokeLater(() -> updateFromInputs());
            }
        });

        outputSelect = new OutputComboBox();
        outputSelect.setBorder(BorderFactory.createTitledBorder("Output value"));
        comboboxes.add(outputSelect);
        outputSelect.setFont(header2Font);
        outputSelect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isAllowEvents()) SwingUtilities.invokeLater(() -> updateFromInputs());
            }
        });

        isInit = false;
    }

}
