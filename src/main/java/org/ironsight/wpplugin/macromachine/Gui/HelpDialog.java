package org.ironsight.wpplugin.macromachine.Gui;

import static org.ironsight.wpplugin.macromachine.Gui.EditActions.LayerMappingTopPanel.header1Font;

import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.DefaultCaret;

public class HelpDialog extends JDialog
{
    public record HelpItem(String input, String description) {
    }

    public HelpDialog(Frame owner, String title, String helpText) {
        this(owner, title, helpText, List.of(), false);
    }

    public HelpDialog(Frame owner, String title, String explanation, List<HelpItem> helpItems) {
        this(owner, title, explanation, helpItems, true);
    }

    private HelpDialog(Frame owner, String title, String helpText, List<HelpItem> helpItems, boolean structuredHelp) {
        super(owner, "Help", true); // Modal dialog
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        setMinimumSize(new Dimension(420, 300));

        JLabel label = new JLabel(title);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setFont(header1Font);
        label.setBorder(new EmptyBorder(16, 16, 0, 16));
        add(label, BorderLayout.NORTH);

        JTextArea helpTextArea = new JTextArea(helpText);
        helpTextArea.setWrapStyleWord(true);
        helpTextArea.setLineWrap(true);
        helpTextArea.setEditable(false);
        helpTextArea.setOpaque(false);
        helpTextArea.setFont(helpTextArea.getFont().deriveFont(14f));
        helpTextArea.setBorder(new EmptyBorder(0, 0, 0, 0));
        helpTextArea.setCaret(new DefaultCaret() {
            @Override
            public void paint(Graphics graphics) {
            }
        });
        helpTextArea.setCursor(Cursor.getDefaultCursor());

        JPanel contentPanel = new JPanel(new BorderLayout(0, 12));
        contentPanel.setBorder(new EmptyBorder(12, 20, 12, 20));
        if (structuredHelp) {
            helpTextArea.setRows(5);
            contentPanel.add(helpTextArea, BorderLayout.NORTH);
            contentPanel.add(createHelpTable(helpItems), BorderLayout.CENTER);
        } else {
            JScrollPane scrollPane = new JScrollPane(helpTextArea);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());
            contentPanel.add(scrollPane, BorderLayout.CENTER);
        }
        add(contentPanel, BorderLayout.CENTER);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBorder(new EmptyBorder(0, 20, 12, 20));
        buttonPanel.add(closeButton);
        add(buttonPanel, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(closeButton);

        setSize(structuredHelp ? new Dimension(620, 500) : new Dimension(500, 360));
        setLocationRelativeTo(owner);
    }

    private static JScrollPane createHelpTable(List<HelpItem> helpItems) {
        String[] columns = {"Interaction", "Effect"};
        Object[][] rows = helpItems.stream()
                .map(item -> new Object[]{item.input(), item.description()})
                .toArray(Object[][]::new);
        JTable table = new JTable(rows, columns);
        table.setRowSelectionAllowed(false);
        table.setFocusable(false);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setRowHeight(table.getRowHeight() + 8);
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumnModel().getColumn(0).setPreferredWidth(180);
        table.getColumnModel().getColumn(1).setPreferredWidth(360);

        JScrollPane tableScrollPane = new JScrollPane(table);
        tableScrollPane.setBorder(BorderFactory.createEtchedBorder());
        tableScrollPane.getViewport().setBackground(table.getBackground());
        return tableScrollPane;
    }

    public static JButton getHelpButton(String title, String helpText) {
        JButton button = createHelpButton();
        button.addActionListener(e -> new HelpDialog(null, title, helpText).setVisible(true));
        return button;
    }

    public static JButton getHelpButton(String title, String explanation, List<HelpItem> helpItems) {
        JButton button = createHelpButton();
        button.addActionListener(e -> new HelpDialog(null, title, explanation, helpItems).setVisible(true));
        return button;
    }

    private static JButton createHelpButton() {
        JButton button = new JButton("?");
        button.setFocusPainted(false);
        return button;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("HelpDialog Example");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(400, 300);
            frame.setLayout(new BorderLayout());

            // Add a help button
            JButton helpButton = new JButton("Help");
            helpButton.addActionListener(e -> {
                String helpText = "This application allows you to perform various tasks:\n"
                        + "1. Use the buttons to add, edit, or remove items.\n"
                        + "2. Navigate through the menu to access settings.\n"
                        + "3. Refer to the documentation for more details.\n"
                        + "For additional assistance, contact support.\n";
                new HelpDialog(frame, "My new application", helpText).setVisible(true);
            });

            frame.add(helpButton, BorderLayout.CENTER);
            frame.setVisible(true);
        });
    }
}
