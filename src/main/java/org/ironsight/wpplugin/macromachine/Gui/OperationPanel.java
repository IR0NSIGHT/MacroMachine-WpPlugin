package org.ironsight.wpplugin.macromachine.Gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;

/**
 * The small floating panel of a map tool operation (Path Tool: Subdivide,
 * Randomize position, ...; City Tool: Randomize rotation, ...), like Blender's
 * "Adjust Last Operation": a title, the operation's controls, an optional note
 * and Cancel / Apply. {@link MapToolOverlay#showOperationPanel} places it below
 * the tool's toolbar.
 */
public final class OperationPanel
{
    private OperationPanel() {
    }

    /**
     * @param namePrefix
     *            component names: {@code <prefix>.OperationPanel},
     *            {@code <prefix>.OperationCancel}, {@code <prefix>.OperationApply}
     * @param note
     *            shown above the buttons (e.g. a warning), or {@code null}
     */
    public static JPanel create(String namePrefix, String title, JComponent controls, JComponent note, Runnable cancel,
            Runnable apply) {
        JPanel panel = new OverlayStyle.SurfacePanel(new BorderLayout(8, 8));
        panel.setName(namePrefix + ".OperationPanel");
        var heading = new JLabel(title);
        heading.setForeground(OverlayStyle.TEXT);
        heading.setFont(OverlayStyle.font(Font.BOLD, 15f));
        panel.add(heading, BorderLayout.NORTH);
        styleControls(controls);
        panel.add(controls, BorderLayout.CENTER);
        var buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        buttons.setOpaque(false);
        var cancelButton = OverlayStyle.button(new JButton("Cancel"));
        cancelButton.setName(namePrefix + ".OperationCancel");
        cancelButton.addActionListener(event -> cancel.run());
        var applyButton = OverlayStyle.primaryButton(new JButton("Apply"));
        applyButton.setName(namePrefix + ".OperationApply");
        applyButton.addActionListener(event -> apply.run());
        buttons.add(cancelButton);
        buttons.add(applyButton);
        var south = new JPanel(new BorderLayout(0, 4));
        south.setOpaque(false);
        if (note != null)
            south.add(note, BorderLayout.NORTH);
        south.add(buttons, BorderLayout.SOUTH);
        panel.add(south, BorderLayout.SOUTH);
        return panel;
    }

    /**
     * The panel's component named {@code name} (e.g. its Apply button), or
     * {@code null}.
     */
    public static Component find(Container container, String name) {
        for (Component child : container.getComponents()) {
            if (name.equals(child.getName()))
                return child;
            if (child instanceof Container inner) {
                var found = find(inner, name);
                if (found != null)
                    return found;
            }
        }
        return null;
    }

    /**
     * Controls on the panel's dark surface: labels muted light text, checkboxes the
     * dialog look, other buttons pills.
     */
    public static void styleControls(Container container) {
        // the controls may be a bare text (a note), not a panel of them
        if (container instanceof JLabel label) {
            styleLabel(label);
            return;
        }
        if (container instanceof JComponent component && !(container instanceof JSpinner))
            component.setOpaque(false);
        for (Component child : container.getComponents()) {
            if (child instanceof JLabel label)
                styleLabel(label);
            else if (child instanceof JCheckBox box)
                DialogStyle.checkBox(box);
            else if (child instanceof AbstractButton button)
                OverlayStyle.button(button);
            else if (child instanceof Container inner && !(child instanceof JSpinner))
                styleControls(inner);
        }
    }

    /** Texts on the panel: muted light text (unless coloured on purpose). */
    private static void styleLabel(JLabel label) {
        label.setOpaque(false);
        if (label.getForeground() == null || label.getForeground() instanceof javax.swing.plaf.UIResource)
            OverlayStyle.label(label, true);
    }
}
