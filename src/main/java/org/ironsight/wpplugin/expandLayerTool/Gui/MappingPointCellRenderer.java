package org.ironsight.wpplugin.expandLayerTool.Gui;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class MappingPointCellRenderer implements TableCellRenderer, ListCellRenderer<MappingPointValue> {
    private final JPanel panel;
    private final JLabel textLabel;
    private final MappingValuePreviewPanel valueRenderer;

    public MappingPointCellRenderer() {
        panel = new JPanel(new BorderLayout());
        textLabel = new JLabel("NULL");
        textLabel.setHorizontalAlignment(JLabel.CENTER);
        textLabel.setVerticalAlignment(JLabel.CENTER);
        textLabel.setOpaque(true);
        panel.add(textLabel, BorderLayout.CENTER);

        valueRenderer = new MappingValuePreviewPanel();
        valueRenderer.setPreferredSize(new Dimension(50, 50));
        valueRenderer.setOpaque(true);
        valueRenderer.setBackground(Color.GREEN);
        panel.add(valueRenderer, BorderLayout.EAST);
    }

    public void updateTo(MappingPointValue point) {
        textLabel.setText(point.mappingValue.valueToString(point.numericValue));
        valueRenderer.setMappingValue(point.mappingValue);
        valueRenderer.setValue(point.numericValue);
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
        assert value != null;
        assert value instanceof MappingPointValue;

        updateTo((MappingPointValue) value);

        return panel;
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends MappingPointValue> list, MappingPointValue value,
                                                  int index, boolean isSelected, boolean cellHasFocus) {
        if (isSelected) {
            textLabel.setBackground(list.getSelectionBackground());
            textLabel.setForeground(list.getSelectionForeground());
        } else {
            textLabel.setBackground(list.getBackground());
            textLabel.setForeground(list.getForeground());
        }
        assert value != null;

        updateTo(value);

        return panel;
    }
}
