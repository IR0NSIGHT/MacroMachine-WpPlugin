package org.ironsight.wpplugin.expandLayerTool.Gui;

import org.ironsight.wpplugin.expandLayerTool.operations.Gradient;

import javax.swing.*;
import java.awt.*;
import java.util.Random;

public class GradientDisplay extends JPanel {
    private Gradient gradient;

    public GradientDisplay(Gradient gradient) {
        this.gradient = gradient;
    }


    public void setGradient(Gradient gradient) {
        this.gradient = gradient;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.RED);
        int scale = 10;
        for (int w = 0; w < getWidth(); w += scale) {
            Random r = new Random(w);
            float chance = gradient.getValue((float) w / getWidth());
            for (int h = 0; h < getHeight(); h += scale) {
                if (chance > r.nextFloat()) {
                    g2.fillRect(w, h, scale, scale);
                }
            }
        }
    }
}