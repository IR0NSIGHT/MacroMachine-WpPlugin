package org.ironsight.wpplugin.expandLayerTool.Gui;

import javax.swing.*;
import java.awt.*;

import static org.ironsight.wpplugin.expandLayerTool.Gui.LayerMappingTopPanel.header1Font;

public class HelpDialog extends JDialog {

    public HelpDialog(Frame owner, String title, String helpText) {
        super(owner, "Help", true); // Modal dialog
        setLayout(new BorderLayout());
        setSize(400, 300);
        setLocationRelativeTo(owner);

        JLabel label = new JLabel(title);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        add(label, BorderLayout.NORTH);
        label.setFont(header1Font);

        // Create the help text area
        JTextArea helpTextArea = new JTextArea(helpText);
        helpTextArea.setWrapStyleWord(true);
        helpTextArea.setLineWrap(true);
        helpTextArea.setEditable(false);
        helpTextArea.setOpaque(false);
        helpTextArea.setFont(helpTextArea.getFont().deriveFont(14f));

        // Add the text area to a scroll pane
        JScrollPane scrollPane = new JScrollPane(helpTextArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(scrollPane, BorderLayout.CENTER);

        // Add a close button
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(closeButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    public static JButton getHelpButton(String title, String helpText) {
        JButton button = new JButton("?");
        button.addActionListener(e -> new HelpDialog(null, title, helpText).setVisible(true));
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
                String helpText = "This application allows you to perform various tasks:\n" +
                        "1. Use the buttons to add, edit, or remove items.\n" +
                        "2. Navigate through the menu to access settings.\n" +
                        "3. Refer to the documentation for more details.\n" +
                        "For additional assistance, contact support.\n";
                new HelpDialog(frame, "My new application", helpText).setVisible(true);
            });

            frame.add(helpButton, BorderLayout.CENTER);
            frame.setVisible(true);
        });
    }
}
