package org.ironsight.wpplugin.macromachine.Gui;

import org.ironsight.wpplugin.macromachine.MacroMachinePlugin;
import org.ironsight.wpplugin.macromachine.operations.MacroApplicator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;

public class MacroMachineWindow extends JDialog {
    public static MacroMachineWindow getDialog() {
        return dialog;
    }

    private static MacroMachineWindow dialog;

    public boolean isStayOnTop() {
        return stayOnTop;
    }

    public void setStayOnTop(boolean alwaysOnTop) {
        this.stayOnTop = alwaysOnTop;
        dialog.setAlwaysOnTop(alwaysOnTop);
    }

    private boolean stayOnTop;
    public MacroMachineWindow(JFrame parent, String title) {
        super(parent,title, false);
        dialog = this;
    }

    public static JDialog createDialog(JFrame parent,
                                       MacroApplicator applyToMap) {
        if (dialog != null) {
            dialog.setVisible(true);
            dialog.toFront();        // Bring it to the front
            dialog.requestFocus();   // Request focus (optional)
            return dialog;
        }

        // Create a JDialog with the parent frame
        dialog = new MacroMachineWindow(parent, "MacroMachine"); // Modal dialog
        dialog.setLocationRelativeTo(parent); // Centers the dialog


        dialog.add(new GlobalActionPanel(applyToMap, dialog));
        dialog.setTitle(
                MacroMachinePlugin.getInstance().getName() + " v" + MacroMachinePlugin.getInstance().getVersion());
        dialog.pack();
        dialog.setAlwaysOnTop(false);

        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            if (event.getID() == WindowEvent.WINDOW_OPENED) {
                if (event.getSource() instanceof JDialog && dialog != event.getSource() && dialog.stayOnTop) {
                    dialog.setAlwaysOnTop(false);
                }
            }
            if (event.getID() == WindowEvent.WINDOW_CLOSED) {
                if (event.getSource() instanceof JDialog && dialog != event.getSource() && dialog.stayOnTop) {
                    dialog.setAlwaysOnTop(true);
                }
            }
        }, AWTEvent.WINDOW_EVENT_MASK);

        return dialog;
    }
}
