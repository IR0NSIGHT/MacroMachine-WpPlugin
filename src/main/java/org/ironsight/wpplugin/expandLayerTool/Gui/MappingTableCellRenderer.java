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

    public void updateTo(Object mapping) {
        if (mapping instanceof LayerMapping) {
            LayerMapping lm = (LayerMapping) mapping;
            input.setText(lm.input.getName());
            output.setText(lm.output.getName());
            actionType.setText(lm.actionType.getDisplayName());
            nameLabel.setText(lm.getName());
            panel.setToolTipText(lm.getDescription());
        } else {
            nameLabel.setText("UNKNOWN ACTION");
            input.setText("");
            output.setText("");
            actionType.setText("");
            panel.setToolTipText("this action does not exist. It will be ignored.");
        }
    }


    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
        updateTo(value);
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
