package org.ironsight.wpplugin.macromachine.Gui;

import org.ironsight.wpplugin.macromachine.operations.MappingAction;
import org.ironsight.wpplugin.macromachine.operations.Macro;
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
    JLabel nameLabel = new JLabel();
    JPanel inputoutput = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    JLabel input = new JLabel();
    JLabel output = new JLabel();
    JPanel panel = new JPanel(new BorderLayout());
    JLabel actionType = new JLabel();
    JLabel iconLabel = new JLabel();
    private final Function<IDisplayUnit, Boolean> isItemValid;
    public DisplayUnitRenderer(Function<IDisplayUnit, Boolean> isItemValid) {
        this.isItemValid = isItemValid;
        init();
    }

    private void init(){
        JPanel iconAndName = new JPanel(new FlowLayout(FlowLayout.LEFT));
        iconAndName.setOpaque(false);
        iconLabel.setPreferredSize(new Dimension(15, 15));
        iconAndName.add(iconLabel);
        iconAndName.add(nameLabel);

        panel.add(iconAndName, BorderLayout.WEST);
        int border = 3;
        panel.setBorder(BorderFactory.createEmptyBorder(border,border,border,border));
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

    public void updateTo(Object mapping, boolean isActive) {
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
            input.setText("");
            output.setText("");
            actionType.setText("");
            nameLabel.setText("All macros");
            panel.setToolTipText("");
            nameLabel.setFont(ioFont);
            iconLabel.setIcon(IconManager.getIcon(IconManager.Icon.MACRO));
        }
        if (mapping instanceof IDisplayUnit && !isItemValid.apply((IDisplayUnit) mapping)) {
            iconLabel.setIcon(IconManager.getIcon(IconManager.Icon.INVALID));
        }
        nameLabel.setForeground(isActive ? DEFAULT_FOREGROUND : INTERPOLATED_FOREGROUND);
    }


    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
        boolean active = value instanceof SaveableAction ?((SaveableAction) value).isActive() : true;
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
                    updateTo(((MacroTreeNode) value).getInput(),  node.isActive());
                    break;
                case INVALID:
                    updateTo(((MacroTreeNode) value).payload,  node.isActive());
                    break;
            }
        }

        if (selected) {
            panel.setBackground(SELECTED_BACKGROUND);
        } else panel.setBackground( DEFAULT_BACKGROUND );
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
