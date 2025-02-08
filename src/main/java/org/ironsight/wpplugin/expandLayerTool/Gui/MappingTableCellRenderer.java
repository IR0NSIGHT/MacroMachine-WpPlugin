package org.ironsight.wpplugin.expandLayerTool.Gui;

import org.ironsight.wpplugin.expandLayerTool.operations.LayerMapping;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

class MappingTableCellRenderer implements TableCellRenderer, ListCellRenderer<LayerMapping> {
    JLabel nameLabel = new JLabel();
    JPanel inputoutput = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    JLabel input = new JLabel();
    JLabel output = new JLabel();
    JPanel panel = new JPanel(new BorderLayout());
    JLabel actionType = new JLabel();

    public MappingTableCellRenderer() {
        panel.add(nameLabel, BorderLayout.WEST);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
        nameLabel.setFont(LayerMappingTopPanel.header1Font);
        nameLabel.setVerticalAlignment(JLabel.CENTER);

        input.setFont(LayerMappingTopPanel.header2Font);
        input.setHorizontalTextPosition(SwingConstants.CENTER);

        output.setFont(LayerMappingTopPanel.header2Font);
        output.setHorizontalTextPosition(SwingConstants.CENTER);

        actionType.setFont(LayerMappingTopPanel.header2Font);
        actionType.setHorizontalTextPosition(SwingConstants.CENTER);

        inputoutput.add(input);
        inputoutput.add(actionType);
        inputoutput.add(output);
        inputoutput.setOpaque(false);
        panel.add(inputoutput, BorderLayout.CENTER);
    }

    public void updateTo(LayerMapping mapping) {
        input.setText(mapping.input.getName());
        output.setText(mapping.output.getName());
        actionType.setText(mapping.actionType.getDisplayName());
        nameLabel.setText(mapping.getName());
        panel.setToolTipText(mapping.getDescription());
    }


    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
        updateTo((LayerMapping) value);
        nameLabel.setText(row + ": " + ((LayerMapping) value).getName());
        if (isSelected) {
            panel.setBackground(table.getSelectionBackground());
        } else panel.setBackground(table.getBackground());
        return panel;
    }


    @Override
    public Component getListCellRendererComponent(JList<? extends LayerMapping> list, LayerMapping value, int index,
                                                  boolean isSelected, boolean cellHasFocus) {
        updateTo(value);
        if (isSelected) {
            panel.setBackground(list.getSelectionBackground());
        } else panel.setBackground(list.getBackground());
        return panel;
    }
}
