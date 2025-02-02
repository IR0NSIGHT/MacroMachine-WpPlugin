package org.ironsight.wpplugin.expandLayerTool.Gui;

import org.ironsight.wpplugin.expandLayerTool.operations.MappingPoint;
import org.ironsight.wpplugin.expandLayerTool.operations.ValueProviders.IMappingValue;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class MappingPointCellRenderer implements TableCellRenderer {
    private final JPanel panel;
    private final JLabel textLabel;
    private final MappingValuePreviewPanel valueRenderer;
    IMappingValue input;
    IMappingValue output;

    public MappingPointCellRenderer(IMappingValue input, IMappingValue output) {
        this.input = input;
        this.output = output;

        panel = new JPanel(new BorderLayout());
        textLabel = new JLabel("NULL");
        textLabel.setHorizontalAlignment(JLabel.CENTER);
        textLabel.setVerticalAlignment(JLabel.CENTER);
        panel.add(textLabel, BorderLayout.CENTER);

        valueRenderer = new MappingValuePreviewPanel();
        valueRenderer.setPreferredSize(new Dimension(50, 50));
        valueRenderer.setOpaque(true);
        valueRenderer.setBackground(Color.GREEN);
        panel.add(valueRenderer, BorderLayout.EAST);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
        if (isSelected) {
            textLabel.setBackground(table.getSelectionBackground());
            textLabel.setForeground(table.getSelectionForeground());
        } else {
            textLabel.setBackground(table.getBackground());
            textLabel.setForeground(table.getForeground());
        }

        IMappingValue mappingValue;
        int numericValue;
        if (column == 0) {
            mappingValue = input;
            numericValue = ((MappingPoint) value).input;
        } else {
            mappingValue = output;
            numericValue = ((MappingPoint) value).output;
        }

        textLabel.setText(mappingValue.valueToString(numericValue));
        valueRenderer.setMappingValue(mappingValue);
        valueRenderer.setValue(numericValue);
        return panel;
    }
}
