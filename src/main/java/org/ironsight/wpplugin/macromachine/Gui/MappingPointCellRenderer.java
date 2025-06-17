package org.ironsight.wpplugin.macromachine.Gui;

import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IMappingValue;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

import static org.ironsight.wpplugin.macromachine.Gui.IDisplayUnitCellRenderer.*;

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
        valueRenderer.setPreferredSize(new Dimension(30, 30));
        valueRenderer.setOpaque(false);
        panel.add(valueRenderer, BorderLayout.EAST);
        panel.invalidate();
    }

    public int getPreferredHeight() {
        return panel.getPreferredSize().height;
    }

    public void updateTo(MappingPointValue point, boolean isEditable, boolean isSelected) {
        textLabel.setText(point.mappingValue.valueToString(point.numericValue));

        valueRenderer.setMappingValue(point.mappingValue);
        valueRenderer.setValue(point.numericValue);
        textLabel.setFont(isEditable ? textLabel.getFont().deriveFont(Font.PLAIN) : textLabel.getFont().deriveFont(Font.ITALIC));

        if (isSelected) {
            textLabel.setBackground(SELECTED_BACKGROUND);
            textLabel.setForeground(SELECTED_FOREGROUND);
        } else if (isEditable) {
            textLabel.setForeground(DEFAULT_FOREGROUND);
            textLabel.setBackground(DEFAULT_BACKGROUND);
        } else {
            textLabel.setForeground(INTERPOLATED_FOREGROUND);
            textLabel.setBackground(INTERPOLATED_BACKGROUND);
        }
    }


    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
        assert value != null;
        assert value instanceof MappingPointValue;
        updateTo((MappingPointValue) value, table.isCellEditable(row,column), isSelected);

        return panel;
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends MappingPointValue> list, MappingPointValue value,
                                                  int index, boolean isSelected, boolean cellHasFocus) {
        assert value != null;
        updateTo(value, false, isSelected);

        return panel;
    }
}
