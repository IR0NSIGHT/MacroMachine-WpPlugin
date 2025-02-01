package org.ironsight.wpplugin.expandLayerTool.Gui;

import org.ironsight.wpplugin.expandLayerTool.operations.ValueProviders.AnnotationSetter;
import org.ironsight.wpplugin.expandLayerTool.operations.ValueProviders.IMappingValue;

import javax.swing.*;
import java.awt.*;

public class MappingValuePreviewPanel extends JPanel {
    private final IMappingValue mappingValue;
    private final int value;

    public MappingValuePreviewPanel(IMappingValue mappingValue, int value) {
        this.mappingValue = mappingValue;
        this.value = value;
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new MappingValuePreviewPanel(new AnnotationSetter(), 9));
        frame.setPreferredSize(new Dimension(450, 450));
        frame.pack();
        frame.setVisible(true);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        g.setColor(Color.red);
        mappingValue.paint(g, value, getSize());
    }
}
