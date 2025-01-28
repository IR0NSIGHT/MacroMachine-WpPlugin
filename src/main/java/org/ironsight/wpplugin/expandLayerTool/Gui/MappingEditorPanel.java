package org.ironsight.wpplugin.expandLayerTool.Gui;

import org.ironsight.wpplugin.expandLayerTool.operations.IPositionValueGetter;
import org.ironsight.wpplugin.expandLayerTool.operations.IPositionValueSetter;
import org.ironsight.wpplugin.expandLayerTool.operations.LayerMapping;
import org.ironsight.wpplugin.expandLayerTool.operations.LayerMappingContainer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.function.Consumer;

public class MappingEditorPanel extends JPanel {
    private final Consumer<LayerMapping> onSubmit;
    private LayerMapping mapping;
    private OutputComboBox outputSelect;
    private InputGetterComboBox inputSelect;
    private MappingGridPanel mappingDisplay;
    private MappingTextTable table;
    private JTextField description;
    private JTextField nameField;
    private ActionTypeComboBox actionTypeComboBox;

    public MappingEditorPanel(Consumer<LayerMapping> onSubmit) {
        super();
        this.onSubmit = onSubmit;
        initComponents();
    }

    public static JDialog createDialog(JFrame parent, Consumer<LayerMapping> applyToMap) {
        // Create a JDialog with the parent frame
        JDialog dialog = new JDialog(parent, "My Dialog", false); // Modal dialog
        JPanel all = new JPanel(new BorderLayout());

        Consumer<LayerMapping> submit = mapping1 -> {
            LayerMappingContainer.INSTANCE.updateMapping(mapping1);
        };

        MappingEditorPanel editor = new MappingEditorPanel(submit);
        all.add(editor, BorderLayout.CENTER);

        SavedMappingsSelector mappingSelector = new SavedMappingsSelector(editor::setMapping);
        mappingSelector.setTo(LayerMappingContainer.INSTANCE.queryMappingsAll()[0]);
        all.add(mappingSelector, BorderLayout.WEST);

        LayerMapping m = LayerMappingContainer.INSTANCE.queryMappingById(mappingSelector.getSelectedProvider());
        editor.setMapping(m);

        JPanel buttons = new JPanel(new FlowLayout());
        JButton apply = new JButton("apply");
        apply.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                submit.accept(editor.mapping);  //SAVE TO CONTAINER ON APPLY
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
                        output.getMinValue())}, LayerMapping.ActionType.SET, "Test", "test description", 1);

        MappingEditorPanel editor = new MappingEditorPanel(f -> {
        });
        editor.setMapping(mapper);

        // Add the outer panel to the frame
        frame.add(editor);

        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    private void initComponents() {
        this.setLayout(new BorderLayout());

        mappingDisplay = new MappingGridPanel();
        table = new MappingTextTable();

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

        nameField = new JTextField();
        nameField.setFont(inputOutputFont);
        top.add(nameField);
        nameField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (mapping.getName().equals(nameField.getText())) return;
                setMapping(mapping.withName(nameField.getText()));
            }
        });
        description = new JTextField();
        description.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (mapping.getDescription().equals(description.getText())) return;
                setMapping(mapping.withDescription(description.getText()));
            }
        });
        description.setFont(inputOutputFont);
        top.add(description);

        inputSelect = new InputGetterComboBox();
        inputSelect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IPositionValueGetter getter = inputSelect.getSelectedProvider();
                if (getter.equals(mapping.input)) return;
                setMapping(mapping.withInput(getter));
            }
        });
        inputSelect.setFont(inputOutputFont);
        top.add(inputSelect);

        outputSelect = new OutputComboBox();
        top.add(outputSelect);
        outputSelect.setFont(inputOutputFont);
        outputSelect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IPositionValueSetter setter = outputSelect.getSelectedProvider();
                if (mapping.output.equals(outputSelect.getSelectedProvider())) return;
                setMapping(mapping.withOutput(setter));
            }
        });

        actionTypeComboBox = new ActionTypeComboBox();
        actionTypeComboBox.setFont(inputOutputFont);
        top.add(actionTypeComboBox);
        actionTypeComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setMapping(mapping.withType(actionTypeComboBox.getSelectedProvider()));
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
        if (this.mapping != null && this.mapping.equals(mapping)) return;
        table.setMapping(mapping);
        mappingDisplay.setMapping(mapping);
        actionTypeComboBox.setTo(mapping.getActionType());
        outputSelect.SetSelected(mapping.output);
        inputSelect.SetSelected(mapping.input);
        description.setText(mapping.getDescription());
        nameField.setText(mapping.getName());

        this.mapping = mapping;
        this.repaint();
    }
}
