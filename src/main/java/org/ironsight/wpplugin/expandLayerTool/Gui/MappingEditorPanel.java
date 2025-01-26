package org.ironsight.wpplugin.expandLayerTool.Gui;

import org.ironsight.wpplugin.expandLayerTool.operations.IPositionValueGetter;
import org.ironsight.wpplugin.expandLayerTool.operations.IPositionValueSetter;
import org.ironsight.wpplugin.expandLayerTool.operations.LayerMapping;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.function.Consumer;

public class MappingEditorPanel extends JPanel {
    private final Consumer<LayerMapping> onSubmit;
    private LayerMapping mapping;

    private MappingGridPanel mappingDisplay;
    private MappingTextTable table;

    public MappingEditorPanel(LayerMapping mapping, Consumer<LayerMapping> onSubmit) {
        super();
        this.mapping = mapping;
        this.onSubmit = onSubmit;
        initComponents();
    }

    private void initComponents() {
        this.setLayout(new BorderLayout());

        mappingDisplay = new MappingGridPanel(mapping);
        table = new MappingTextTable(mapping);

        //set up sync between both components
        table.setOnUpdate(mapping1 -> {
            this.mapping = mapping1;
            mappingDisplay.setMapping(mapping);
        });
        table.setOnSelect(mappingDisplay::setSelected);

        mappingDisplay.setOnUpdate(mapping1 -> {
            this.mapping = mapping1;
            table.setMapping(mapping1);
        });
        mappingDisplay.setOnSelect(table::setSelected);

        JPanel top = new JPanel(new FlowLayout());
        Font inputOutputFont = new Font("SansSerif", Font.BOLD, 24);

        JTextField nameField = new JTextField(mapping.getName());
        nameField.setFont(inputOutputFont);
        top.add(nameField);
        nameField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mapping.setName(nameField.getText());
            }
        });
        JTextField description = new JTextField(mapping.getDescription());
        description.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mapping.setDescription(description.getText());
            }
        });
        description.setFont(inputOutputFont);
        top.add(description);

        InputGetterComboBox inputSelect = new InputGetterComboBox();
        inputSelect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IPositionValueGetter getter = inputSelect.getSelectedProvider();
                setMapping(new LayerMapping(getter, mapping.output, mapping.getMappingPoints(), mapping.actionType));
            }
        });
        inputSelect.setFont(inputOutputFont);
        top.add(inputSelect);
        inputSelect.SetSelected(mapping.input);

        OutputComboBox outputSelect = new OutputComboBox();
        top.add(outputSelect);
        outputSelect.setFont(inputOutputFont);
        outputSelect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IPositionValueSetter setter = outputSelect.getSelectedProvider();
                setMapping(new LayerMapping(mapping.input, setter, mapping.getMappingPoints(), mapping.actionType));
            }
        });
        outputSelect.SetSelected(mapping.output);

        ActionTypeComboBox actionTypeComboBox = new ActionTypeComboBox();
        actionTypeComboBox.setFont(inputOutputFont);
        top.add(actionTypeComboBox);
        actionTypeComboBox.setTo(mapping.getActionType());
        actionTypeComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mapping.setActionType(actionTypeComboBox.getSelectedProvider());
                setMapping(mapping);
            }
        });

        JButton submitButtom = new JButton("save");
        submitButtom.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onSubmit.accept(mapping);
            }
        });


        this.add(top, BorderLayout.NORTH);
        this.add(table, BorderLayout.EAST);
        this.add(submitButtom, BorderLayout.SOUTH);
        this.add(mappingDisplay, BorderLayout.CENTER);

    }

    public void setMapping(LayerMapping mapping) {
        table.setMapping(mapping);
        mappingDisplay.setMapping(mapping);
        this.mapping = mapping;
    }

    public static JDialog createDialog(JFrame parent, LayerMapping mapping, Consumer<LayerMapping> applyToMap) {
        // Create a JDialog with the parent frame
        JDialog dialog = new JDialog(parent, "My Dialog", false); // Modal dialog
        JPanel all = new JPanel(new BorderLayout());

        Consumer<LayerMapping> submit = mapping1 -> {

        };
        MappingEditorPanel editor = new MappingEditorPanel(mapping, submit);
        all.add(editor, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout());
        JButton apply = new JButton("apply");
        apply.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                applyToMap.accept(editor.mapping);
            }
        });
        buttons.add(apply);
        all.add(buttons, BorderLayout.SOUTH);

        dialog.add(all);
        dialog.setLocationRelativeTo(parent); // Center the dialog relative to the parent frame
        dialog.pack();
        return dialog;
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("TEST PANEL");

        IPositionValueGetter input = new LayerMapping.SlopeProvider();
        IPositionValueSetter output = new LayerMapping.StonePaletteApplicator();
        LayerMapping mapper = new LayerMapping(input, output,
                new LayerMapping.MappingPoint[]{new LayerMapping.MappingPoint(input.getMinValue(),
                        output.getMinValue())}, LayerMapping.ActionType.SET);

        MappingEditorPanel editor = new MappingEditorPanel(mapper, f -> {
        });

        // Add the outer panel to the frame
        frame.add(editor);

        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
