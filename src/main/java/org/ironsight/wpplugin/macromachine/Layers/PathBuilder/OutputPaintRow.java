package org.ironsight.wpplugin.macromachine.Layers.PathBuilder;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;
import java.util.Collection;
import javax.swing.*;
import org.ironsight.wpplugin.macromachine.Gui.EditActions.IoSelectLabel;
import org.ironsight.wpplugin.macromachine.Gui.EditActions.MappingPointCellRenderer;
import org.ironsight.wpplugin.macromachine.Gui.EditActions.MappingPointValue;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IPositionValueSetter;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IMappingValue;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IMappingValueProvider;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.InputOutputProvider;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IntermediateValueIO;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.StonePaletteApplicator;

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
    private boolean labelOnly;

    /**
     * {@code beforePick} runs on the press that opens the output picker (fills the
     * output list for the current world).
     */
    OutputPaintRow(String text, String tooltip, Runnable beforePick) {
        // one line, never wrapping: the value dropdown takes the rest of the width
        // and shrinks when it is short (a wrapped line would be cut off)
        super(new GridBagLayout());
        enabled = new JCheckBox(text);
        enabled.setToolTipText(tooltip);
        enabled.addActionListener(event -> changed());
        outputPicker = new IoSelectLabel(picked -> {
            if (picked instanceof IPositionValueSetter setter) {
                output = setter;
                fillValues(firstValue(setter));
                changed();
            }
        }, paintableOutputs());
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
        var renderer = new MappingPointCellRenderer();
        var empty = new DefaultListCellRenderer();
        // the mapping table's renderer needs a value; an empty dropdown has none
        values.setRenderer((list, value, index, selected, focus) -> value == null
                ? empty.getListCellRendererComponent(list, "", index, selected, focus)
                : renderer.getListCellRendererComponent(list, value, index, selected, focus));
        values.addActionListener(event -> changed());
        values.setMinimumSize(new java.awt.Dimension(60, values.getPreferredSize().height));
        add(enabled, cell(0, 0));
        add(outputPicker, cell(1, 8));
        var fill = cell(2, 8);
        fill.weightx = 1;
        fill.fill = GridBagConstraints.HORIZONTAL;
        add(values, fill);
    }

    private static GridBagConstraints cell(int column, int left) {
        var cell = new GridBagConstraints();
        cell.gridx = column;
        cell.gridy = 0;
        cell.anchor = GridBagConstraints.WEST;
        cell.insets = new Insets(4, left, 4, 0);
        return cell;
    }

    /**
     * The action outputs a path paints with: no action filter, no intermediate
     * value (those only steer other actions), no stone palette.
     */
    static IMappingValueProvider paintableOutputs() {
        var all = InputOutputProvider.INSTANCE.asOutputProvider();
        return new IMappingValueProvider() {
            @Override
            public Collection<IMappingValue> getItems() {
                return all.getItems().stream().filter(OutputPaintRow::paintable).toList();
            }

            @Override
            public void subscribeToUpdates(Runnable r) {
                all.subscribeToUpdates(r);
            }

            @Override
            public boolean existsItem(Object item) {
                return item instanceof IMappingValue value && paintable(value) && all.existsItem(item);
            }
        };
    }

    static boolean paintable(IMappingValue output) {
        return !output.isVirtual() && !(output instanceof IntermediateValueIO)
                && !(output instanceof StonePaletteApplicator);
    }

    /**
     * No checkbox: the row always paints, its text becomes a plain label (Apply as
     * paint).
     */
    void labelOnly() {
        labelOnly = true;
        enabled.setSelected(true);
        enabled.setVisible(false);
        add(new JLabel(enabled.getText()), cell(0, 0), 0); // the hidden checkbox's cell
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
            enabled.setSelected(labelOnly || paint.enabled());
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
            Arrays.stream(paintValues(output)).mapToObj(template::withValue).sorted().forEach(values::addItem);
            values.setSelectedItem(template);
            if (values.getSelectedItem() == null && values.getItemCount() > 0)
                values.setSelectedIndex(0); // a value this output does not have
        } finally {
            updating = wasUpdating;
        }
    }

    private static int firstValue(IPositionValueSetter output) {
        int[] values = paintValues(output);
        return values.length == 0 ? output.getMinValue() : values[0];
    }

    /**
     * The values {@code output} can paint, without "skip". Outputs that list none
     * (spraypaint) offer their min to max range.
     */
    static int[] paintValues(IPositionValueSetter output) {
        int[] listed = Arrays.stream(output.getAllOutputValues())
                .filter(value -> !output.isIgnoreValue(value))
                .toArray();
        return listed.length > 0
                ? listed
                : java.util.stream.IntStream.rangeClosed(output.getMinValue(), output.getMaxValue()).toArray();
    }

    private void changed() {
        if (!updating)
            onChange.run();
    }
}
