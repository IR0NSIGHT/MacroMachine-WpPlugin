package org.ironsight.wpplugin.macromachine.Gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import java.util.Arrays;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSpinner;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicMenuItemUI;
import javax.swing.plaf.basic.BasicPopupMenuSeparatorUI;
import javax.swing.plaf.basic.BasicRadioButtonMenuItemUI;
import javax.swing.plaf.basic.BasicSpinnerUI;

/**
 * Look of the map tool overlays, after the MacroMachine website: charcoal
 * surfaces (#161616, panels #222222) with rounded corners and a hairline
 * border, white text with #B0B0B0 for secondary text, lavender (#CDC7FF) for
 * selection, active toggles and primary buttons. Swing controls on the map get
 * this look instead of the look and feel (light desktop buttons on a map read
 * as dated and noisy).
 */
public final class OverlayStyle
{
    /**
     * Panels, labels, buttons on the map: website surface #222222, nearly opaque.
     */
    public static final Color SURFACE = new Color(34, 34, 34, 240);
    /** Popups (opaque: windows cannot be translucent everywhere): panel #222222. */
    public static final Color SURFACE_SOLID = new Color(34, 34, 34);
    public static final Color SURFACE_HOVER = new Color(43, 43, 43, 245);
    public static final Color SURFACE_PRESSED = new Color(52, 52, 52, 250);
    /** Text fields inside surfaces. */
    public static final Color FIELD = new Color(22, 22, 22);
    public static final Color HAIRLINE = new Color(255, 255, 255, 56);
    /** Popup and field borders. */
    public static final Color BORDER = new Color(83, 83, 83);
    public static final Color TEXT = Color.WHITE;
    public static final Color TEXT_MUTED = new Color(176, 176, 176);
    public static final Color TEXT_DISABLED = new Color(110, 110, 110);
    /** Lavender: selection, handles, active toggles, primary buttons. */
    public static final Color ACCENT = new Color(205, 199, 255);
    /** Lavender lightened towards white (website --accent-hover). */
    public static final Color ACCENT_HOVER = new Color(225, 221, 255);
    /** Lavender behind active toggles. */
    public static final Color ACCENT_SOFT = new Color(205, 199, 255, 44);
    /** Menu selection on #222222. */
    public static final Color ACCENT_SOFT_SOLID = new Color(66, 64, 84);
    /** Text on lavender. */
    public static final Color ON_ACCENT = new Color(22, 22, 22);
    /**
     * Selected nodes and handles on the map: the website's light teal (#8BC4D3).
     */
    public static final Color MAP_SELECTION = new Color(139, 196, 211);
    /** Dark edge behind text on the map (keeps labels readable on any terrain). */
    public static final Color OUTLINE = new Color(22, 22, 22);
    /**
     * Previewed (not yet applied) results: amber, like WorldPainter's yellow
     * preview layer (the website's teal would vanish on water and grass).
     */
    public static final Color PREVIEW = new Color(251, 191, 36);
    public static final Color WARNING = new Color(251, 146, 60);
    public static final Color DANGER = new Color(229, 107, 107);
    /** Website green (actions / paths). */
    public static final Color SUCCESS = new Color(132, 188, 133);
    /**
     * Website accents for other tools: macros (plum), inputs (indigo), outputs
     * (teal).
     */
    public static final Color PLUM = new Color(163, 73, 164), INDIGO = new Color(94, 55, 175),
            TEAL = new Color(85, 158, 178);
    /** Panels and labels: near-square corners (website cards). */
    public static final int RADIUS = 4;
    /** Outline of secondary (outlined) buttons. */
    public static final Color BUTTON_OUTLINE = new Color(255, 255, 255, 102);

    private static Font baseFont;

    private OverlayStyle() {
    }

    /**
     * UI font: Segoe UI where installed (Windows), else the default dialog font.
     */
    public static Font font(int style, float size) {
        if (baseFont == null) {
            String[] families = GraphicsEnvironment.isHeadless()
                    ? new String[0]
                    : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
            var installed = Arrays.asList(families);
            String family = installed.contains("Inter")
                    ? "Inter"
                    : installed.contains("Segoe UI") ? "Segoe UI" : Font.DIALOG;
            baseFont = new Font(family, Font.PLAIN, 12);
        }
        return baseFont.deriveFont(style, size);
    }

    /** Fully rounded ends (buttons). */
    public static Shape pill(double x, double y, double width, double height) {
        return new RoundRectangle2D.Double(x, y, width, height, height, height);
    }

    public static Shape rounded(double x, double y, double width, double height) {
        return new RoundRectangle2D.Double(x, y, width, height, 2 * RADIUS, 2 * RADIUS);
    }

    /** A surface: rounded fill with a hairline border. */
    public static void paintSurface(Graphics2D g, double x, double y, double width, double height, Color fill) {
        Graphics2D target = (Graphics2D) g.create();
        try {
            target.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            target.setColor(fill);
            target.fill(rounded(x, y, width, height));
            if (fill.getAlpha() > 120) {
                target.setColor(HAIRLINE);
                target.setStroke(new BasicStroke(1f));
                target.draw(rounded(x + 0.5, y + 0.5, width - 1, height - 1));
            }
        } finally {
            target.dispose();
        }
    }

    /** A transparent panel that paints itself as a surface. */
    public static class SurfacePanel extends JPanel
    {
        public SurfacePanel(java.awt.LayoutManager layout) {
            super(layout);
            setOpaque(false);
            setForeground(TEXT);
            setBorder(new EmptyBorder(10, 12, 10, 12));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            paintSurface((Graphics2D) graphics, 0, 0, getWidth(), getHeight(), SURFACE);
        }
    }

    /** A label on a surface (like the viewport header). */
    public static class SurfaceLabel extends JLabel
    {
        @Override
        protected void paintComponent(Graphics graphics) {
            paintSurface((Graphics2D) graphics, 0, 0, getWidth(), getHeight(), getBackground());
            super.paintComponent(graphics);
        }

        @Override
        public boolean isOpaque() {
            return false;
        }
    }

    /**
     * Buttons and toggles on the map: surface, lighter on hover, accent when on.
     */
    public static <B extends AbstractButton> B button(B button) {
        button.setUI(new SurfaceButtonUI());
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setRolloverEnabled(true);
        button.setForeground(TEXT);
        button.setFont(font(Font.PLAIN, 14f));
        button.setBorder(new EmptyBorder(5, 12, 5, 12));
        button.setMargin(new Insets(5, 12, 5, 12));
        button.setIconTextGap(6);
        for (Component child : button.getComponents())
            if (child instanceof JLabel label) {
                label.setForeground(TEXT);
                label.setFont(button.getFont());
            }
        return button;
    }

    /** The highlighted button of a group (e.g. Apply). */
    public static <B extends AbstractButton> B primaryButton(B button) {
        button(button);
        button.putClientProperty(PRIMARY, Boolean.TRUE);
        button.setFont(font(Font.BOLD, 14f));
        return button;
    }

    private static final String PRIMARY = "OverlayStyle.primary";

    private static class SurfaceButtonUI extends BasicButtonUI
    {
        @Override
        public void paint(Graphics graphics, JComponent component) {
            var button = (AbstractButton) component;
            ButtonModel model = button.getModel();
            boolean primary = Boolean.TRUE.equals(button.getClientProperty(PRIMARY));
            Color fill = !button.isEnabled()
                    ? SURFACE
                    : model.isPressed() && model.isArmed()
                            ? SURFACE_PRESSED
                            : model.isRollover() ? SURFACE_HOVER : SURFACE;
            // primary actions and active toggles: solid lavender (website's primary
            // button / pressed filter chip), lighter on hover
            boolean lavender = button.isEnabled() && (primary || model.isSelected());
            if (lavender)
                fill = model.isPressed() && model.isArmed()
                        ? ACCENT.darker()
                        : model.isRollover() ? ACCENT_HOVER : ACCENT;
            // pill shaped (website buttons): lavender for primary actions, outlined
            // charcoal for the rest; an active toggle gets a lavender tint and edge
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                double w = button.getWidth(), h = button.getHeight();
                g.setColor(fill);
                g.fill(pill(0, 0, w, h));
                if (!lavender) {
                    g.setColor(model.isRollover() && button.isEnabled() ? TEXT : BUTTON_OUTLINE);
                    g.setStroke(new BasicStroke(1f));
                    g.draw(pill(0.5, 0.5, w - 1, h - 1));
                }
            } finally {
                g.dispose();
            }
            button.setForeground(!button.isEnabled() ? TEXT_DISABLED : lavender ? ON_ACCENT : TEXT);
            for (Component child : button.getComponents())
                if (child instanceof JLabel label)
                    label.setForeground(button.getForeground());
            ((Graphics2D) graphics).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            super.paint(graphics, component);
        }

        @Override
        protected void paintButtonPressed(Graphics g, AbstractButton b) {
            // the surface shows the pressed state
        }
    }

    /** Dark popup menu: graphite, hairline border, accent selection. */
    public static JPopupMenu popup(JPopupMenu menu) {
        menu.setBackground(SURFACE_SOLID);
        menu.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(4, 0, 4, 0)));
        for (Component child : menu.getComponents())
            styleMenuChild(child);
        return menu;
    }

    private static void styleMenuChild(Component child) {
        if (child instanceof JRadioButtonMenuItem item) {
            item.setUI(new BasicRadioButtonMenuItemUI() {
                @Override
                protected void installDefaults() {
                    super.installDefaults();
                    checkIcon = CURRENT_DOT;
                    selectionBackground = ACCENT_SOFT_SOLID;
                    selectionForeground = TEXT;
                    disabledForeground = TEXT_DISABLED;
                    acceleratorForeground = TEXT_MUTED;
                }
            });
            menuItemLook(item);
        } else if (child instanceof JMenuItem item) {
            item.setUI(new BasicMenuItemUI() {
                @Override
                protected void installDefaults() {
                    super.installDefaults();
                    selectionBackground = ACCENT_SOFT_SOLID;
                    selectionForeground = TEXT;
                    disabledForeground = TEXT_DISABLED;
                    acceleratorForeground = TEXT_MUTED;
                }
            });
            menuItemLook(item);
        } else if (child instanceof JPopupMenu.Separator separator) {
            separator.setUI(new BasicPopupMenuSeparatorUI());
            separator.setForeground(BORDER);
            separator.setBackground(SURFACE_SOLID);
        } else if (child instanceof JComponent component) {
            component.setOpaque(false);
            component.setForeground(TEXT);
            for (Component inner : component.getComponents())
                if (inner instanceof AbstractButton button)
                    button(button);
                else if (inner instanceof JLabel label) {
                    label.setForeground(TEXT_MUTED);
                    label.setFont(font(Font.BOLD, 13f));
                }
        }
    }

    /**
     * Marks the current choice in a menu: a small lavender dot (nothing for the
     * others).
     */
    private static final javax.swing.Icon CURRENT_DOT = new javax.swing.Icon() {
        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            if (!(component instanceof AbstractButton button) || !button.isSelected())
                return;
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(ACCENT);
                g.fillOval(x + 1, y + 2, 6, 6);
            } finally {
                g.dispose();
            }
        }

        @Override
        public int getIconWidth() {
            return 8;
        }

        @Override
        public int getIconHeight() {
            return 10;
        }
    };

    private static void menuItemLook(JMenuItem item) {
        item.setOpaque(true);
        item.setBackground(SURFACE_SOLID);
        item.setForeground(TEXT);
        item.setFont(font(Font.PLAIN, 14f));
        item.setBorder(new EmptyBorder(6, 10, 6, 14));
    }

    /** Spinner in a surface: dark field, light text, quiet arrows. */
    public static JSpinner spinner(JSpinner spinner) {
        spinner.setUI(new BasicSpinnerUI() {
            @Override
            protected Component createNextButton() {
                return arrow(javax.swing.SwingConstants.NORTH, "Spinner.nextButton");
            }

            @Override
            protected Component createPreviousButton() {
                return arrow(javax.swing.SwingConstants.SOUTH, "Spinner.previousButton");
            }

            private Component arrow(int direction, String name) {
                var button = new javax.swing.plaf.basic.BasicArrowButton(direction, FIELD, FIELD, TEXT_MUTED, FIELD);
                button.setName(name);
                button.setBorder(new EmptyBorder(0, 2, 0, 2));
                if (direction == javax.swing.SwingConstants.NORTH)
                    installNextButtonListeners(button);
                else
                    installPreviousButtonListeners(button);
                return button;
            }
        });
        spinner.setBorder(BorderFactory.createLineBorder(BORDER));
        spinner.setBackground(FIELD);
        if (spinner.getEditor() instanceof JSpinner.DefaultEditor editor) {
            var field = editor.getTextField();
            field.setBackground(FIELD);
            field.setForeground(TEXT);
            field.setCaretColor(TEXT);
            field.setSelectionColor(ACCENT_SOFT_SOLID);
            field.setSelectedTextColor(TEXT);
            field.setBorder(new EmptyBorder(3, 6, 3, 4));
            field.setFont(font(Font.PLAIN, 14f));
            // visible keyboard focus: lavender edge while typing
            field.addFocusListener(new java.awt.event.FocusAdapter() {
                @Override
                public void focusGained(java.awt.event.FocusEvent event) {
                    spinner.setBorder(BorderFactory.createLineBorder(ACCENT));
                }

                @Override
                public void focusLost(java.awt.event.FocusEvent event) {
                    spinner.setBorder(BorderFactory.createLineBorder(BORDER));
                }
            });
        }
        return spinner;
    }

    /** Label text inside surfaces. */
    public static JLabel label(JLabel label, boolean muted) {
        label.setForeground(muted ? TEXT_MUTED : TEXT);
        label.setFont(font(Font.PLAIN, 14f));
        return label;
    }

    /** Small plain button, e.g. the strip's "Reset zoom". */
    public static JButton smallButton(JButton button) {
        button(button);
        button.setFont(font(Font.PLAIN, 12.5f));
        button.setBorder(new EmptyBorder(2, 8, 2, 8));
        return button;
    }
}
