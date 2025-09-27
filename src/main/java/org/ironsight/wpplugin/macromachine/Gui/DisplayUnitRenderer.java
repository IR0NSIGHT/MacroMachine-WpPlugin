package org.ironsight.wpplugin.macromachine.Gui;

import org.ironsight.wpplugin.macromachine.operations.Macro;
import org.ironsight.wpplugin.macromachine.operations.MappingAction;
import org.ironsight.wpplugin.macromachine.operations.SaveableAction;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IDisplayUnit;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IPositionValueGetter;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IPositionValueSetter;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;
import java.util.function.Function;

import static org.ironsight.wpplugin.macromachine.Gui.IDisplayUnitCellRenderer.*;
import static org.ironsight.wpplugin.macromachine.Gui.LayerMappingTopPanel.*;

public class DisplayUnitRenderer extends DefaultTreeCellRenderer
        implements TableCellRenderer, ListCellRenderer<Object> {
    private final Function<IDisplayUnit, Boolean> isItemValid;
    JLabel nameLabel = new JLabel();
    JPanel inputoutput = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    JLabel summary = new JLabel();
    JPanel panel = new JPanel(new BorderLayout());
    JLabel iconLabel = new JLabel();

    public DisplayUnitRenderer(Function<IDisplayUnit, Boolean> isItemValid) {
        this.isItemValid = isItemValid;
        init();
    }

    private void init() {
        JPanel iconAndName = new JPanel(new FlowLayout(FlowLayout.LEFT));
        iconAndName.setOpaque(false);
        iconLabel.setPreferredSize(new Dimension(15, 15));
        iconAndName.add(iconLabel);
        iconAndName.add(nameLabel);

        panel.add(iconAndName, BorderLayout.WEST);
        int border = 3;
        panel.setBorder(BorderFactory.createEmptyBorder(border, border, border, border));
        nameLabel.setFont(LayerMappingTopPanel.header1Font);
        nameLabel.setVerticalAlignment(JLabel.CENTER);

        summary.setFont(LayerMappingTopPanel.header2Font);
        summary.setHorizontalTextPosition(SwingConstants.CENTER);

        inputoutput.add(summary);
        inputoutput.setOpaque(false);
        panel.add(inputoutput, BorderLayout.CENTER);
    }

    public void updateTo(Object mapping, boolean isActive) {
        nameLabel.setVisible(true);
        if (mapping instanceof MappingAction) {
            MappingAction lm = (MappingAction) mapping;
            summary.setText(lm.getSummary());
            nameLabel.setText(lm.getName());
            panel.setToolTipText(lm.getToolTipText());
            nameLabel.setFont(actionFont);
            iconLabel.setIcon(IconManager.getIcon(IconManager.Icon.ACTION));
        } else if (mapping instanceof Macro) {
            Macro lm = (Macro) mapping;
            summary.setText("Macro " + lm.getExecutionUUIDs().length + " steps");
            nameLabel.setText(lm.getName());
            panel.setToolTipText(lm.getToolTipText());
            nameLabel.setFont(macroFont);
            iconLabel.setIcon(IconManager.getIcon(IconManager.Icon.MACRO));
        } else if (mapping instanceof IPositionValueGetter) {
            IPositionValueGetter lm = (IPositionValueGetter) mapping;
            summary.setText("");
            nameLabel.setText(lm.getName());
            panel.setToolTipText(lm.getToolTipText());
            nameLabel.setFont(ioFont);
            iconLabel.setIcon(IconManager.getIcon(IconManager.Icon.INPUT));
        } else if (mapping instanceof IPositionValueSetter) {
            IPositionValueSetter lm = (IPositionValueSetter) mapping;
            summary.setText("");
            nameLabel.setText(lm.getName());
            panel.setToolTipText(lm.getToolTipText());
            nameLabel.setFont(ioFont);
            iconLabel.setIcon(IconManager.getIcon(IconManager.Icon.OUTPUT));
        } else {
            summary.setText("");
            nameLabel.setText("All macros");
            panel.setToolTipText("");
            nameLabel.setFont(ioFont);
            iconLabel.setIcon(IconManager.getIcon(IconManager.Icon.MACRO));
        }
        if (mapping instanceof IDisplayUnit && !isItemValid.apply((IDisplayUnit) mapping)) {
            iconLabel.setIcon(IconManager.getIcon(IconManager.Icon.INVALID));
        }
        nameLabel.setForeground(isActive ? DEFAULT_FOREGROUND : INTERPOLATED_FOREGROUND);
        revalidate();
    }


    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
        boolean active = !(value instanceof SaveableAction) || ((SaveableAction) value).isActive();
        updateTo(value, active);
        if (isSelected) {
            panel.setBackground(SELECTED_BACKGROUND);
        } else panel.setBackground(DEFAULT_BACKGROUND);
        return panel;
    }

    public Component renderFor(IDisplayUnit value, boolean isSelected) {
        updateTo(value, true);
        if (isSelected) {
            panel.setBackground(SELECTED_BACKGROUND);
        } else panel.setBackground(DEFAULT_BACKGROUND);
        return panel;
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
                                                  boolean leaf, int row, boolean hasFocus) {
        assert value instanceof MacroTreeNode;
        MacroTreeNode node = (MacroTreeNode) value;
        if (value instanceof MacroTreeNode) {
            switch (((MacroTreeNode) value).payloadType) {
                case MACRO:
                case ACTION:
                    updateTo(((MacroTreeNode) value).payload, node.isActive());
                    break;
                case OUTPUT:
                    updateTo(((MacroTreeNode) value).getOutput(), node.isActive());

                    break;
                case INPUT:
                    updateTo(((MacroTreeNode) value).getInput(), node.isActive());
                    break;
                case INVALID:
                    updateTo(((MacroTreeNode) value).payload, node.isActive());
                    break;
            }
        }

        if (selected) {
            panel.setBackground(SELECTED_BACKGROUND);
        } else panel.setBackground(DEFAULT_BACKGROUND);
        panel.invalidate();
        return panel;

    }

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                                                  boolean cellHasFocus) {
        updateTo(value, true);
        if (isSelected) {
            panel.setBackground(SELECTED_BACKGROUND);
        } else panel.setBackground(DEFAULT_BACKGROUND);
        return panel;
    }
}
