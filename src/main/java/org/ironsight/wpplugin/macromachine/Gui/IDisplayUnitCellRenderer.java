package org.ironsight.wpplugin.macromachine.Gui;

import org.ironsight.wpplugin.macromachine.Gui.MacroTreePanel.MacroTreeNode;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IDisplayUnit;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

import static org.ironsight.wpplugin.macromachine.Gui.LayerMappingTopPanel.header1Font;
import static org.ironsight.wpplugin.macromachine.Gui.LayerMappingTopPanel.header2Font;

public class IDisplayUnitCellRenderer{

    public static final Color SELECTED_BACKGROUND = new Color(205, 199, 255);
    public static final Color SELECTED_FOREGROUND = Color.BLACK;

    public static final Color DEFAULT_BACKGROUND = Color.WHITE;
    public static final Color DEFAULT_FOREGROUND = Color.DARK_GRAY;
    public static final Color INTERPOLATED_BACKGROUND = new Color(232, 232, 232);
    public static final Color INTERPOLATED_FOREGROUND = new Color(176, 176, 176);

}
