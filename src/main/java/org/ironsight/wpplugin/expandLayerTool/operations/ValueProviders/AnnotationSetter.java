package org.ironsight.wpplugin.expandLayerTool.operations.ValueProviders;

import org.pepsoft.worldpainter.layers.Annotations;

import java.awt.*;

public class AnnotationSetter extends NibbleLayerSetter {

    public AnnotationSetter() {
        super(Annotations.INSTANCE);
    }

    @Override
    public String valueToString(int value) {
        if (value == 0) return "Absent (0)";
        try {
            String name = Annotations.getColourName(value);
            return name + "(" + value + ")";
        } catch (ArrayIndexOutOfBoundsException ex) {
            System.out.println(ex);
        }
        return "ERROR";
    }

    @Override
    public boolean isDiscrete() {
        return true;
    }

    private static final Color[] COLORS = new Color[]{
            Color.WHITE,
            Color.WHITE,
            Color.ORANGE,
            Color.MAGENTA,
            new Color(107, 177, 255),   //LIGHT BLUE
            Color.YELLOW,
            new Color( 34, 153, 84), //LIME
            Color.pink,
            Color.lightGray,
            Color.cyan,
            new Color(128, 0, 128), //purple
            Color.BLUE,
            new Color(165, 42, 42), // brown
            Color.GREEN,
            Color.RED,
            Color.BLACK
    };

    @Override
    public void paint(Graphics g, int value, Dimension dim) {
        g.setColor(COLORS[value]);
        g.fillRect(0, 0, dim.width, dim.height);
     }
}
