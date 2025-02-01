package org.ironsight.wpplugin.expandLayerTool.Gui;

import org.ironsight.wpplugin.expandLayerTool.operations.HeightProvider;
import org.ironsight.wpplugin.expandLayerTool.operations.IMappingValue;
import org.ironsight.wpplugin.expandLayerTool.operations.SlopeProvider;

import javax.swing.*;
import java.awt.*;

public class MappingValuePreviewPanel extends JPanel {
    private IMappingValue mappingValue;
    private int value;
    public MappingValuePreviewPanel(IMappingValue mappingValue, int value) {
        this.mappingValue = mappingValue;
        this.value = value;
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        g.setColor(Color.red);
        mappingValue.paint(g, value, getSize());
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new MappingValuePreviewPanel(new HeightProvider(), 255));
        frame.setPreferredSize(new Dimension(450, 450))
        ;
        frame.pack();
        frame.setVisible(true);
    }
}
