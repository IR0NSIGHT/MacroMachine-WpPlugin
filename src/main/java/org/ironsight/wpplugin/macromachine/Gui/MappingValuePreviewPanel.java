package org.ironsight.wpplugin.macromachine.Gui;

import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IMappingValue;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.StonePaletteApplicator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MappingValuePreviewPanel extends JPanel {
    private IMappingValue mappingValue;
    private int value;

    public MappingValuePreviewPanel() {
        this.setOpaque(true);
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                System.out.println("ICON WAS CLICKED");
                super.mouseClicked(e);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                System.out.println("Mouse entered the panel");
            }

            @Override
            public void mouseExited(MouseEvent e) {
                System.out.println("Mouse exited the panel");
            }
        });
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        MappingValuePreviewPanel panel = new MappingValuePreviewPanel();
        panel.setMappingValue(new StonePaletteApplicator());
        panel.setValue(12);
        frame.add(panel);
        frame.setPreferredSize(new Dimension(450, 450));
        frame.pack();
        frame.setVisible(true);
    }

    public void setMappingValue(IMappingValue mappingValue) {
        this.mappingValue = mappingValue;
    }

    public void setValue(int value) {
        if (mappingValue != null) this.value = value;
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        mappingValue.paint(g, IMappingValue.sanitizeValue(value, mappingValue), getSize());
    }
}
