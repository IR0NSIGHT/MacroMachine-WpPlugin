package org.ironsight.wpplugin.expandLayerTool.Gui;

import org.ironsight.wpplugin.expandLayerTool.operations.ValueProviders.IMappingValue;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class MappingPointCellRenderer implements TableCellRenderer {
    private final JPanel panel;

    public MappingPointCellRenderer(IMappingValue getter, int value) {
        JLabel label = new JLabel(getter.valueToString(value));
        label.setHorizontalAlignment(JLabel.CENTER);
        label.setVerticalAlignment(JLabel.CENTER);
        JLabel icon = new JLabel() {
            @Override
            public Dimension getPreferredSize() {
                Dimension size = super.getPreferredSize();
                int max = Math.min(size.width, size.height); // Ensure it's square
                return new Dimension(max, max);
            }
        };
        icon.setPreferredSize(new Dimension(50,50));
        icon.setOpaque(true);
        icon.setBackground(Color.GREEN);

        panel = new JPanel(new BorderLayout());
        panel.add(label, BorderLayout.CENTER);
        panel.add(icon, BorderLayout.EAST);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
        return panel;
    }
}
