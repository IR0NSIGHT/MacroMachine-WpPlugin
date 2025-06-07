package org.ironsight.wpplugin.macromachine.Gui;

import org.ironsight.wpplugin.macromachine.Gui.MacroTreePanel.MacroTreeNode;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IDisplayUnit;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

import static org.ironsight.wpplugin.macromachine.Gui.LayerMappingTopPanel.header1Font;
import static org.ironsight.wpplugin.macromachine.Gui.LayerMappingTopPanel.header2Font;

class IDisplayUnitCellRenderer extends DefaultTreeCellRenderer {
    JPanel panel = new JPanel();
    JLabel name = new JLabel();
    JTextArea description = new JTextArea();

    public IDisplayUnitCellRenderer() {
        panel.setLayout(new BorderLayout());

        name.setVerticalAlignment(SwingConstants.CENTER);
        name.setFont(header1Font);
        description.setFont(header2Font);
        description.setLineWrap(true);
        description.setOpaque(false);
        panel.add(name, BorderLayout.NORTH);
        panel.add(description, BorderLayout.CENTER);
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
                                                  boolean leaf, int row, boolean hasFocus) {
        String nameText = "";
        String descriptionText = "";
        assert value instanceof MacroTreeNode;
        if (value instanceof MacroTreeNode) {
            MacroTreeNode node = (MacroTreeNode) value;
            IDisplayUnit obj = node.getPayload();
             nameText = obj.getName();
            descriptionText = obj.getDescription();
        }
        name.setText(nameText);
        if (expanded || leaf) {
            description.setText(descriptionText);
            description.setVisible(true);
        } else description.setVisible(false);
        FontMetrics fm = description.getFontMetrics(description.getFont());
        description.setPreferredSize(new Dimension(fm.charWidth('A') * 50, description.getPreferredSize().height));

        if (selected) panel.setBackground(Color.LIGHT_GRAY);
        else panel.setBackground(Color.WHITE);
        panel.invalidate();
        return panel;

    }
}
