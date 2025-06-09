package org.ironsight.wpplugin.macromachine.Gui;

import org.ironsight.wpplugin.macromachine.operations.MappingAction;
import org.ironsight.wpplugin.macromachine.operations.Macro;
import org.ironsight.wpplugin.macromachine.operations.SaveableAction;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IDisplayUnit;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IMappingValue;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IPositionValueGetter;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IPositionValueSetter;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;

import static org.ironsight.wpplugin.macromachine.Gui.LayerMappingTopPanel.*;

class SaveableActionRenderer extends DefaultTreeCellRenderer
        implements TableCellRenderer, ListCellRenderer<SaveableAction> {
    JLabel nameLabel = new JLabel();
    JPanel inputoutput = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    JLabel input = new JLabel();
    JLabel output = new JLabel();
    JPanel panel = new JPanel(new BorderLayout());
    JLabel actionType = new JLabel();
    JLabel iconLabel = new JLabel();

    public SaveableActionRenderer() {
        JPanel iconAndName = new JPanel(new FlowLayout(FlowLayout.LEFT));
        iconAndName.setOpaque(false);
        iconLabel.setPreferredSize(new Dimension(20, 20));
        iconAndName.add(iconLabel);
        iconAndName.add(nameLabel);

        panel.add(iconAndName, BorderLayout.WEST);
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
        if (mapping instanceof MappingAction) {
            MappingAction lm = (MappingAction) mapping;
            input.setText(lm.input.getName());
            output.setText(lm.output.getName());
            actionType.setText(lm.actionType.getDisplayName());
            nameLabel.setText(lm.getName());
            panel.setToolTipText(lm.getToolTipText());
            nameLabel.setFont(actionFont);
            iconLabel.setIcon(IconManager.getIcon(IconManager.Icon.ACTION));
        } else if (mapping instanceof Macro) {
            Macro lm = (Macro) mapping;
            input.setText("");
            output.setText(lm.getExecutionUUIDs().length + " steps");
            actionType.setText("Macro");
            nameLabel.setText(lm.getName());
            panel.setToolTipText(lm.getToolTipText());
            nameLabel.setFont(macroFont);
            iconLabel.setIcon(IconManager.getIcon(IconManager.Icon.MACRO));
        } else if (mapping instanceof IPositionValueGetter) {
            IPositionValueGetter lm = (IPositionValueGetter) mapping;
            input.setText("");
            output.setText("");
            actionType.setText("");
            nameLabel.setText(lm.getName());
            panel.setToolTipText(lm.getToolTipText());
            nameLabel.setFont(ioFont);
            iconLabel.setIcon(IconManager.getIcon(IconManager.Icon.INPUT));
        } else if (mapping instanceof IPositionValueSetter) {
            IPositionValueSetter lm = (IPositionValueSetter) mapping;
            input.setText("");
            output.setText("");
            actionType.setText("");
            nameLabel.setText(lm.getName());
            panel.setToolTipText(lm.getToolTipText());
            nameLabel.setFont(ioFont);
            iconLabel.setIcon(IconManager.getIcon(IconManager.Icon.OUTPUT));
        } else {
            nameLabel.setText("UNKNOWN ACTION: " + mapping);
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
    public Component getListCellRendererComponent(JList<? extends SaveableAction> list, SaveableAction value, int index,
                                                  boolean isSelected, boolean cellHasFocus) {
        updateTo(value);
        if (isSelected) {
            panel.setBackground(list.getSelectionBackground());
        } else panel.setBackground(list.getBackground());
        return panel;
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
                                                  boolean leaf, int row, boolean hasFocus) {
        assert value instanceof MacroTreePanel.MacroTreeNode;
        if (value instanceof MacroTreePanel.MacroTreeNode) {
            switch (((MacroTreePanel.MacroTreeNode) value).payloadType) {
                case MACRO:
                case ACTION:
                    updateTo(((MacroTreePanel.MacroTreeNode) value).payload);
                    break;
                case OUTPUT:
                    updateTo(((MacroTreePanel.MacroTreeNode) value).getOutput());

                    break;
                case INPUT:
                    updateTo(((MacroTreePanel.MacroTreeNode) value).getInput());
                    break;
                case INVALID:
            }
        }

        if (selected) {
            panel.setBackground(getBackgroundSelectionColor());
        } else panel.setBackground(getBackground());
        panel.invalidate();
        return panel;

    }
}
