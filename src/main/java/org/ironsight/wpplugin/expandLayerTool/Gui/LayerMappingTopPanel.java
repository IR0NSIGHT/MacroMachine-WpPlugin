package org.ironsight.wpplugin.expandLayerTool.Gui;

import org.ironsight.wpplugin.expandLayerTool.operations.LayerMapping;

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
                    .withDescription(description.getText())
                    .withInput(inputSelect.getSelectedProvider())
                    .withOutput(outputSelect.getSelectedProvider())
                    .withType(actionTypeComboBox.getSelectedProvider());
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
        nameField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                super.focusLost(e);
                if (isAllowEvents()) SwingUtilities.invokeLater(() -> updateFromInputs());
            }
        });

        description = new JTextField();
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
        inputSelect.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (isAllowEvents()) SwingUtilities.invokeLater(() -> updateFromInputs());
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
                if (isAllowEvents()) SwingUtilities.invokeLater(() -> updateFromInputs());
            }
        });

        actionTypeComboBox = new ActionTypeComboBox();
        actionTypeComboBox.setFont(header2Font);
        comboboxes.add(actionTypeComboBox);
        actionTypeComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isAllowEvents()) SwingUtilities.invokeLater(() -> updateFromInputs());
            }
        });
        isInit = false;
    }

}
