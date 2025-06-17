package org.ironsight.wpplugin.macromachine.Gui;

import org.ironsight.wpplugin.macromachine.operations.MappingAction;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IPositionValueGetter;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IPositionValueSetter;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.InputOutputProvider;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

public class LayerMappingTopPanel extends LayerMappingPanel {
    public static final Font header1Font = new Font("SansSerif", Font.PLAIN, 18);
    public static final Font header2Font = new Font("SansSerif", Font.PLAIN, 12);
    public static final Font macroFont = new Font("SansSerif", Font.PLAIN, 18);
    public static final Font actionFont = new Font("SansSerif", Font.PLAIN, 18);
    public static final Font ioFont = new Font("SansSerif", Font.ITALIC, 18);

    boolean isInit;
    private JTextField description;
    private JTextField nameField;
    private ActionTypeComboBox actionTypeComboBox;
    private InputGetterComboBox outputSelect;
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
            MappingAction newMap = mapping.withName(nameField.getText())
                    .withDescription(description.getText()).withType(actionTypeComboBox.getSelectedProvider());
            if (!inputSelect.getSelectedProvider().getName().equals(mapping.input.getName()))  // rebuild map
                newMap = newMap.withInput((IPositionValueGetter) inputSelect.getSelectedProvider())
                        .withNewPoints(mapping.getMappingPoints());
            if (!outputSelect.getSelectedProvider().getName().equals(mapping.output.getName()))  // rebuild map
                newMap = newMap.withOutput((IPositionValueSetter) outputSelect.getSelectedProvider())
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
        inputSelect = new InputGetterComboBox(selected -> {
            if (isAllowEvents()) this.updateFromInputs();
        }, InputOutputProvider.INSTANCE.asInputProvider());
        inputSelect.setBorder(BorderFactory.createTitledBorder("Input value"));
        inputSelect.setFont(header2Font);
        comboboxes.add(inputSelect);


        actionTypeComboBox = new ActionTypeComboBox();
        actionTypeComboBox.setBorder(BorderFactory.createTitledBorder("Action Type"));
        actionTypeComboBox.setFont(header2Font);
        comboboxes.add(actionTypeComboBox);
        actionTypeComboBox.addActionListener(e -> {
            if (isAllowEvents()) SwingUtilities.invokeLater(this::updateFromInputs);
        });

        outputSelect = new InputGetterComboBox(selected -> {
            if (isAllowEvents()) this.updateFromInputs();
        }, InputOutputProvider.INSTANCE.asOutputProvider());
        outputSelect.setBorder(BorderFactory.createTitledBorder("Output value"));
        comboboxes.add(outputSelect);
        outputSelect.setFont(header2Font);
        outputSelect.addActionListener(e -> {
            if (isAllowEvents()) SwingUtilities.invokeLater(this::updateFromInputs);
        });

        isInit = false;
    }

}
