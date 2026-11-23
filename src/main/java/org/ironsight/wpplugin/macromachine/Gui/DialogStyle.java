package org.ironsight.wpplugin.macromachine.Gui;

import static org.ironsight.wpplugin.macromachine.Gui.OverlayStyle.*;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.function.BooleanSupplier;
import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.basic.BasicCheckBoxUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.basic.ComboPopup;
import org.ironsight.wpplugin.macromachine.Gui.EditActions.IoSelectLabel;

/**
 * The tool dialogs' own look, independent of WorldPainter's look and feel
 * (which WorldPainter fixes): a dark, flat, charcoal window with #222 panels,
 * lavender focus and selection, white text and muted secondary text, moderate
 * rounding and a 4/8 px spacing rhythm. Every control gets its own UI delegate,
 * so nothing outside the styled dialog changes.
 */
public final class DialogStyle
{
    /** Window background. */
    public static final Color WINDOW = new Color(0x16, 0x16, 0x16);
    /** Panels (the setting groups). */
    public static final Color PANEL = new Color(0x22, 0x22, 0x22);
    /** Raised or hovered surfaces. */
    public static final Color RAISED = new Color(0x2B, 0x2B, 0x2B);
    /** Thin, low-contrast outlines. */
    public static final Color OUTLINE = new Color(255, 255, 255, 30);
    public static final Color OUTLINE_STRONG = new Color(255, 255, 255, 70);
    /** Between related elements / groups, and the window padding. */
    public static final int GAP = 12, PADDING = 20;
    /** Rounding of panels and of controls. */
    public static final int PANEL_RADIUS = 8, CONTROL_RADIUS = 6;

    private DialogStyle() {
    }

    public static Font controlFont() {
        return font(Font.PLAIN, 14f);
    }

    public static Font headingFont() {
        return font(Font.BOLD, 15f);
    }

    /**
     * Styles {@code root} and everything in it: panels transparent on the window or
     * group background, text white, checkboxes, dropdowns, spinners, buttons,
     * scroll panes and output pickers in the dialog look.
     */
    public static void apply(Container root) {
        for (Component child : root.getComponents())
            style(child);
    }

    private static void style(Component component) {
        if (component instanceof IoSelectLabel picker) {
            outputPicker(picker);
            return; // its content is the shared renderer's, restyled there
        }
        if (component instanceof JCheckBox box)
            checkBox(box);
        else if (component instanceof JComboBox<?> combo) {
            comboBox(combo);
            return;
        } else if (component instanceof JSpinner spinner) {
            spinner(spinner);
            return;
        } else if (component instanceof JButton button) {
            button(button);
        } else if (component instanceof JScrollPane scroll) {
            scrollPane(scroll);
        } else if (component instanceof JLabel label) {
            label.setForeground(TEXT);
            label.setFont(controlFont());
        } else if (component instanceof JPanel panel) {
            panel.setOpaque(false);
            panel.setForeground(TEXT);
        }
        if (component instanceof Container container && !(component instanceof JComboBox<?>))
            for (Component child : container.getComponents())
                style(child);
    }

    private static boolean isFocusWithin(Component component) {
        var owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        return owner != null && SwingUtilities.isDescendingFrom(owner, component);
    }

    /** A group of settings: a #222 rounded panel with a semibold heading. */
    public static JPanel group(String title) {
        var group = new JPanel();
        group.setLayout(new BoxLayout(group, BoxLayout.Y_AXIS));
        group.setOpaque(false);
        group.setBorder(new GroupBorder(title));
        return group;
    }

    /**
     * A group's panel and heading. Still a {@link TitledBorder}: the title can be
     * read and changed as usual.
     */
    public static class GroupBorder extends TitledBorder
    {
        public GroupBorder(String title) {
            super(title);
        }

        @Override
        public Insets getBorderInsets(Component component, Insets insets) {
            var metrics = component.getFontMetrics(headingFont());
            int inner = PADDING - 4;
            insets.set(inner + metrics.getHeight() + 4, inner - 4, inner + GAP, inner);
            return insets;
        }

        @Override
        public void paintBorder(Component component, Graphics graphics, int x, int y, int width, int height) {
            var g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // the bottom gap separates this group from the next
                var shape = new RoundRectangle2D.Double(x + 0.5, y + 0.5, width - 1, height - GAP - 1, 2 * PANEL_RADIUS,
                        2 * PANEL_RADIUS);
                g.setColor(PANEL);
                g.fill(shape);
                g.setColor(OUTLINE);
                g.draw(shape);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.setFont(headingFont());
                g.setColor(TEXT);
                g.drawString(getTitle(), x + PADDING - 4, y + PADDING - 4 + g.getFontMetrics().getAscent());
            } finally {
                g.dispose();
            }
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }
    }

    /**
     * Outline of a field: thin and low-contrast, brighter on hover, lavender with
     * keyboard focus; optionally fills the field (for fields whose content are
     * child components).
     */
    public static class FieldBorder extends AbstractBorder
    {
        private final BooleanSupplier hovered, focused;
        private final boolean fill;

        public FieldBorder(BooleanSupplier hovered, BooleanSupplier focused, boolean fill) {
            this.hovered = hovered;
            this.focused = focused;
            this.fill = fill;
        }

        @Override
        public Insets getBorderInsets(Component component, Insets insets) {
            insets.set(2, 2, 2, 2);
            return insets;
        }

        @Override
        public void paintBorder(Component component, Graphics graphics, int x, int y, int width, int height) {
            var g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                var shape = new RoundRectangle2D.Double(x + 0.5, y + 0.5, width - 1, height - 1, 2 * CONTROL_RADIUS,
                        2 * CONTROL_RADIUS);
                boolean hover = component.isEnabled() && hovered.getAsBoolean();
                if (fill) {
                    g.setColor(hover ? RAISED : WINDOW);
                    g.fill(shape);
                }
                boolean focus = focused.getAsBoolean();
                g.setColor(focus ? ACCENT : hover ? OUTLINE_STRONG : BORDER);
                g.setStroke(new BasicStroke(focus ? 1.5f : 1f));
                g.draw(shape);
            } finally {
                g.dispose();
            }
        }
    }

    /**
     * Fills a field's rounded background (for controls painting their own content).
     */
    static void paintField(Graphics graphics, JComponent component, boolean hovered) {
        var g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(hovered && component.isEnabled() ? RAISED : WINDOW);
            g.fill(new RoundRectangle2D.Double(0, 0, component.getWidth(), component.getHeight(), 2 * CONTROL_RADIUS,
                    2 * CONTROL_RADIUS));
        } finally {
            g.dispose();
        }
    }

    // ---- spinners

    /**
     * The overlay's dark spinner in a rounded field outline, lavender with focus.
     */
    public static JSpinner spinner(JSpinner spinner) {
        OverlayStyle.spinner(spinner);
        var border = new FieldBorder(() -> false, () -> isFocusWithin(spinner), false);
        spinner.setBorder(border);
        spinner.setOpaque(false);
        if (spinner.getEditor() instanceof JSpinner.DefaultEditor editor)
            // after the overlay's focus listener, which sets a square outline
            editor.getTextField().addFocusListener(new java.awt.event.FocusAdapter() {
                @Override
                public void focusGained(java.awt.event.FocusEvent event) {
                    spinner.setBorder(border);
                    spinner.repaint();
                }

                @Override
                public void focusLost(java.awt.event.FocusEvent event) {
                    spinner.setBorder(border);
                    spinner.repaint();
                }
            });
        return spinner;
    }

    // ---- checkboxes

    public static JCheckBox checkBox(JCheckBox box) {
        box.setUI(new BasicCheckBoxUI());
        box.setOpaque(false);
        box.setForeground(TEXT);
        box.setFont(controlFont());
        box.setIcon(CHECK_ICON);
        box.setRolloverEnabled(true);
        box.setFocusPainted(false);
        box.setIconTextGap(8);
        box.setBorder(new EmptyBorder(4, 0, 4, 0));
        return box;
    }

    /**
     * A rounded box: dark with a thin outline when off, filled lavender with a dark
     * check when on; brighter on hover, a lavender ring with keyboard focus.
     */
    private static final Icon CHECK_ICON = new Icon() {
        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            var button = (AbstractButton) component;
            var model = button.getModel();
            boolean on = model.isSelected(), hover = model.isRollover() && button.isEnabled();
            var g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                var box = new RoundRectangle2D.Double(x + 2.5, y + 2.5, 15, 15, 9, 9);
                if (!button.isEnabled())
                    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f));
                if (on) {
                    g.setColor(hover ? ACCENT_HOVER : ACCENT);
                    g.fill(box);
                    var check = new Path2D.Double();
                    check.moveTo(x + 6, y + 10.5);
                    check.lineTo(x + 8.8, y + 13.2);
                    check.lineTo(x + 14.2, y + 7.2);
                    g.setColor(ON_ACCENT);
                    g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.draw(check);
                } else {
                    g.setColor(hover ? RAISED : WINDOW);
                    g.fill(box);
                    g.setColor(hover ? OUTLINE_STRONG : BORDER);
                    g.draw(box);
                }
                if (button.isFocusOwner()) {
                    g.setColor(ACCENT);
                    g.setStroke(new BasicStroke(1.5f));
                    g.draw(new RoundRectangle2D.Double(x + 0.75, y + 0.75, 18.5, 18.5, 12, 12));
                }
            } finally {
                g.dispose();
            }
        }

        @Override
        public int getIconWidth() {
            return 20;
        }

        @Override
        public int getIconHeight() {
            return 20;
        }
    };

    // ---- buttons

    /**
     * Footer buttons: the overlay's buttons, with a lavender ring on keyboard
     * focus.
     */
    public static <B extends AbstractButton> B button(B button) {
        OverlayStyle.button(button);
        button.setFocusPainted(true);
        return button;
    }

    public static <B extends AbstractButton> B primaryButton(B button) {
        OverlayStyle.primaryButton(button);
        button.setFocusPainted(true);
        return button;
    }

    // ---- dropdowns

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void comboBox(JComboBox combo) {
        boolean[] hover = {false};
        combo.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                var arrow = new ChevronButton();
                arrow.setName("ComboBox.arrowButton");
                arrow.addMouseListener(hoverListener(combo, hover));
                return arrow;
            }

            @Override
            public void paint(Graphics g, JComponent component) {
                paintField(g, component, hover[0]);
                super.paint(g, component);
            }

            @Override
            public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
                // the rounded field is the background
            }

            @Override
            protected ComboPopup createPopup() {
                var popup = new BasicComboPopup(comboBox) {
                    {
                        scroller.setBorder(null);
                        scrollBar(scroller.getVerticalScrollBar());
                    }
                };
                popup.setBorder(BorderFactory.createLineBorder(BORDER));
                popup.getList().setBackground(PANEL);
                popup.getList().setForeground(TEXT);
                popup.getList().setSelectionBackground(ACCENT_SOFT_SOLID);
                popup.getList().setSelectionForeground(TEXT);
                return popup;
            }
        });
        combo.setOpaque(false);
        combo.setBackground(WINDOW);
        combo.setForeground(TEXT);
        combo.setFont(controlFont());
        combo.setBorder(new FieldBorder(() -> hover[0], () -> isFocusWithin(combo), false));
        combo.addMouseListener(hoverListener(combo, hover));
        if (!(combo.getRenderer() instanceof StyledRenderer))
            combo.setRenderer(new StyledRenderer(combo.getRenderer()));
    }

    private static java.awt.event.MouseListener hoverListener(JComponent component, boolean[] hover) {
        return new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent event) {
                hover[0] = true;
                component.repaint();
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent event) {
                hover[0] = false;
                component.repaint();
            }
        };
    }

    /** A dropdown's arrow: a quiet chevron on the field. */
    private static class ChevronButton extends JButton
    {
        ChevronButton() {
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusable(false);
            setBorder(new EmptyBorder(0, 4, 0, 8));
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(26, 16);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            var g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(TEXT_MUTED);
                g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                double cx = getWidth() / 2.0 - 2, cy = getHeight() / 2.0;
                var chevron = new Path2D.Double();
                chevron.moveTo(cx - 4, cy - 2);
                chevron.lineTo(cx, cy + 2);
                chevron.lineTo(cx + 4, cy - 2);
                g.draw(chevron);
            } finally {
                g.dispose();
            }
        }
    }

    /**
     * Wraps a dropdown's renderer: in the field the value sits on the field's
     * background, in the list on the panel colour, lavender-tinted when selected;
     * text white.
     */
    private static class StyledRenderer implements ListCellRenderer<Object>
    {
        private final ListCellRenderer<Object> inner;

        @SuppressWarnings("unchecked")
        StyledRenderer(ListCellRenderer<?> inner) {
            this.inner = (ListCellRenderer<Object>) inner;
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected,
                boolean focus) {
            @SuppressWarnings("unchecked")
            var cell = inner.getListCellRendererComponent((JList<Object>) list, value, index, selected, focus);
            boolean inField = index < 0;
            restyleCell(cell, true, inField, selected);
            if (cell instanceof JComponent component)
                component.setBorder(new EmptyBorder(4, inField ? 6 : 10, 4, 6));
            return cell;
        }

        private static void restyleCell(Component cell, boolean top, boolean inField, boolean selected) {
            if (cell instanceof JComponent component) {
                component.setOpaque(top && !inField);
                component.setBackground(selected && !inField ? ACCENT_SOFT_SOLID : PANEL);
            }
            if (cell instanceof JLabel label) {
                label.setForeground(TEXT);
                label.setFont(controlFont());
            }
            if (cell instanceof Container container)
                for (Component child : container.getComponents())
                    restyleCell(child, false, inField, selected);
        }
    }

    // ---- output pickers

    /**
     * The output picker (shared with the action editor, which paints it light):
     * restyled as a dark field that brightens on hover, white text.
     */
    public static void outputPicker(IoSelectLabel picker) {
        if (picker.getComponentCount() == 0 || !(picker.getComponent(0) instanceof JComponent inner))
            return;
        boolean[] hover = {false};
        Runnable restyle = () -> {
            restylePickerContent(inner);
            picker.repaint();
        };
        picker.setOpaque(false);
        picker.setBorder(new FieldBorder(() -> hover[0], () -> false, true));
        // after the picker's own hover listener (which paints it lavender)
        inner.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent event) {
                hover[0] = true;
                restyle.run();
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent event) {
                hover[0] = false;
                restyle.run();
            }
        });
        // a new pick re-renders the content in the light look
        inner.addPropertyChangeListener("background", event -> {
            if (inner.isOpaque())
                SwingUtilities.invokeLater(restyle);
        });
        restyle.run();
    }

    private static void restylePickerContent(Component component) {
        if (component instanceof JComponent c)
            c.setOpaque(false);
        if (component instanceof JLabel label) {
            label.setForeground(TEXT);
            label.setFont(controlFont());
        }
        if (component instanceof Container container)
            for (Component child : container.getComponents())
                restylePickerContent(child);
    }

    // ---- scrolling

    public static void scrollPane(JScrollPane scroll) {
        scroll.setBorder(null);
        scroll.setViewportBorder(null);
        scroll.setOpaque(true);
        scroll.setBackground(WINDOW);
        scroll.getViewport().setOpaque(true);
        scroll.getViewport().setBackground(WINDOW);
        scrollBar(scroll.getVerticalScrollBar());
        scrollBar(scroll.getHorizontalScrollBar());
    }

    /** A thin rounded thumb on the window background, no arrow buttons. */
    public static void scrollBar(JScrollBar bar) {
        bar.setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                thumbColor = RAISED;
                trackColor = WINDOW;
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                return zeroButton();
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                return zeroButton();
            }

            private JButton zeroButton() {
                var button = new JButton();
                button.setPreferredSize(new Dimension(0, 0));
                button.setMinimumSize(new Dimension(0, 0));
                button.setMaximumSize(new Dimension(0, 0));
                return button;
            }

            @Override
            protected void paintTrack(Graphics g, JComponent component, Rectangle bounds) {
                g.setColor(WINDOW);
                g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }

            @Override
            protected void paintThumb(Graphics graphics, JComponent component, Rectangle bounds) {
                if (bounds.isEmpty())
                    return;
                var g = (Graphics2D) graphics.create();
                try {
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setColor(isThumbRollover() ? OUTLINE_STRONG : RAISED);
                    g.fill(new RoundRectangle2D.Double(bounds.x + 2, bounds.y + 2, bounds.width - 4, bounds.height - 4,
                            6, 6));
                } finally {
                    g.dispose();
                }
            }
        });
        bar.setPreferredSize(new Dimension(10, 10));
        bar.setUnitIncrement(16);
    }
}
