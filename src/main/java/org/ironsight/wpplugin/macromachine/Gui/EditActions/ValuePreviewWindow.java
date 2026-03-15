package org.ironsight.wpplugin.macromachine.Gui.EditActions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

import static java.awt.Image.SCALE_FAST;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;

public class ValuePreviewWindow extends MouseAdapter implements ActionListener{
    private final JTable table;
    JWindow previewWindow;
    JLabel previewLabel;
    private int row, col;
    private boolean allowWindow = false;

    public ValuePreviewWindow(JTable table) {
        this.table = table;
        previewWindow = new JWindow();
        previewLabel = new JLabel();
        previewLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        previewWindow.add(previewLabel);
        previewWindow.setSize(200, 200); // Size of enlarged image

        table.addMouseListener(this);
        table.addMouseMotionListener(this);
    }

    void setMouseOver(Point p) {
        int row = table.rowAtPoint(p);
        int col = table.columnAtPoint(p);
        if (this.row == row && this.col == col)
            return;
        if (row == -1 || col == -1)
            return;

        this.row = row;
        this.col = col;
        if (row >= 0) {
            Object cell = table.getValueAt(row, col);
            if (!(cell instanceof MappingPointValue))
                return;

            // Scale the image (larger)
            BufferedImage scaledImage = new BufferedImage(100, 100, TYPE_INT_RGB);
            MappingPointValue mpv = (MappingPointValue) cell;
            mpv.mappingValue.paint(scaledImage.getGraphics(), mpv.numericValue,
                    new Dimension(scaledImage.getWidth(), scaledImage.getHeight()));

            previewLabel.setIcon(new ImageIcon(scaledImage.getScaledInstance(previewWindow.getWidth(),
                    previewWindow.getHeight(), SCALE_FAST)));

            // Position the window near the mouse
            Point locationOnScreen = table.getLocationOnScreen();
            previewWindow.setLocation(locationOnScreen.x + (int) p.getX() + 15,
                    locationOnScreen.y + (int) p.getY() + 15);
            previewWindow.setVisible(allowWindow);
        }
    }

    private boolean enabled;
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        Point tablePoint = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), table);
        setMouseOver(tablePoint);
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        row = -1;
        col = -1;
        previewWindow.setVisible(allowWindow);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        row = -1;
        col = -1;
        previewWindow.setVisible(false);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        setMouseOver(e.getPoint());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof JCheckBox cb)
            allowWindow = cb.isSelected();
        else
            assert false : "you re using it wrong";
    }
}
