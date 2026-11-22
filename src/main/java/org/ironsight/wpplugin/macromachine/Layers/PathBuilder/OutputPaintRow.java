package org.ironsight.wpplugin.macromachine.Layers.PathBuilder;

import java.awt.FlowLayout;
import java.util.Arrays;
import javax.swing.*;
import org.ironsight.wpplugin.macromachine.Gui.EditActions.IoSelectLabel;
import org.ironsight.wpplugin.macromachine.Gui.EditActions.MappingPointCellRenderer;
import org.ironsight.wpplugin.macromachine.Gui.EditActions.MappingPointValue;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IPositionValueSetter;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.InputOutputProvider;

/**
 * "[x] Paint riverbed with [output] [value]": a checkbox, the output picker of
 * the action editor (a terrain, a layer, ...) and a dropdown of that output's
 * values, as in the action's mapping table.
 */
class OutputPaintRow extends JPanel
{
    private final JCheckBox enabled;
    private final IoSelectLabel outputPicker;
    private final JComboBox<MappingPointValue> values = new JComboBox<>();
    private IPositionValueSetter output;
    private Runnable onChange = () -> {
    };
    private boolean updating;

    /**
     * {@code beforePick} runs on the press that opens the output picker (fills the
     * output list for the current world).
     */
    OutputPaintRow(String text, String tooltip, Runnable beforePick) {
        super(new FlowLayout(FlowLayout.LEFT));
        enabled = new JCheckBox(text);
        enabled.setToolTipText(tooltip);
        enabled.addActionListener(event -> changed());
        outputPicker = new IoSelectLabel(picked -> {
            if (picked instanceof IPositionValueSetter setter) {
                output = setter;
                fillValues(firstValue(setter));
                changed();
            }
        }, InputOutputProvider.INSTANCE.asOutputProvider());
        // the picker opens on click (IoSelectLabel listens on its inner panel); the
        // press before it refreshes the list
        for (var inner : outputPicker.getComponents())
            inner.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mousePressed(java.awt.event.MouseEvent event) {
                    beforePick.run();
                }
            });
        outputPicker.setToolTipText("Click to choose a terrain, a layer or another output");
        values.setRenderer(new MappingPointCellRenderer());
        values.addActionListener(event -> changed());
        add(enabled);
        add(outputPicker);
        add(values);
    }

    void onChange(Runnable onChange) {
        this.onChange = onChange;
    }

    PathTool.OutputPaint get() {
        var selected = (MappingPointValue) values.getSelectedItem();
        return new PathTool.OutputPaint(enabled.isSelected(), output,
                selected != null ? selected.numericValue : output == null ? 0 : firstValue(output));
    }

    void set(PathTool.OutputPaint paint) {
        updating = true;
        try {
            enabled.setSelected(paint.enabled());
            output = paint.output();
            outputPicker.SetSelected(output);
            fillValues(paint.value());
        } finally {
            updating = false;
        }
    }

    JCheckBox checkbox() {
        return enabled;
    }

    JComboBox<MappingPointValue> valueDropdown() {
        return values;
    }

    private void fillValues(int selectedValue) {
        boolean wasUpdating = updating;
        updating = true;
        try {
            values.removeAllItems();
            var template = new MappingPointValue(selectedValue, output);
            Arrays.stream(output.getAllOutputValues())
                    .filter(value -> !output.isIgnoreValue(value))
                    .mapToObj(template::withValue)
                    .sorted()
                    .forEach(values::addItem);
            values.setSelectedItem(template);
        } finally {
            updating = wasUpdating;
        }
    }

    private static int firstValue(IPositionValueSetter output) {
        return Arrays.stream(output.getAllOutputValues())
                .filter(value -> !output.isIgnoreValue(value))
                .findFirst()
                .orElse(output.getMinValue());
    }

    private void changed() {
        if (!updating)
            onChange.run();
    }
}
