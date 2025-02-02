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

    public MappingTableCellRenderer() {
        panel.add(nameLabel, BorderLayout.WEST);
        nameLabel.setFont(LayerMappingTopPanel.header1Font);
        nameLabel.setVerticalAlignment(JLabel.CENTER);

        input.setFont(LayerMappingTopPanel.header2Font);
        input.setHorizontalTextPosition(SwingConstants.CENTER);

        output.setFont(LayerMappingTopPanel.header2Font);
        output.setHorizontalTextPosition(SwingConstants.CENTER);

        inputoutput.add(input);
        inputoutput.add(output);
        inputoutput.setOpaque(false);
        panel.add(inputoutput, BorderLayout.CENTER);
    }

    private void updateTo(LayerMapping mapping) {
        input.setText(mapping.input.getName());
        output.setText(mapping.output.getName());
        nameLabel.setText(mapping.getName());
    }


    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
        updateTo((LayerMapping) value);
        if (isSelected) {
            panel.setBackground(table.getSelectionBackground());
        } else panel.setBackground(table.getBackground());
        table.setRowHeight(row, panel.getPreferredSize().height);
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
