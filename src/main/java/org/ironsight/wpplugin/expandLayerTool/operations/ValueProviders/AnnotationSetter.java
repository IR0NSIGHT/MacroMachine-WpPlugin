package org.ironsight.wpplugin.expandLayerTool.operations.ValueProviders;

import org.checkerframework.checker.units.qual.A;
import org.ironsight.wpplugin.expandLayerTool.operations.ProviderType;
import org.pepsoft.worldpainter.layers.Annotations;

import java.awt.*;

public class AnnotationSetter implements IPositionValueGetter, IPositionValueSetter {
    private static final Color[] COLORS = new Color[]{Color.WHITE, Color.WHITE, Color.ORANGE, Color.MAGENTA,
            new Color(107, 177, 255),   //LIGHT BLUE
            Color.YELLOW, new Color(34, 153, 84), //LIME
            Color.pink, Color.lightGray, Color.cyan, new Color(128, 0, 128), //purple
            Color.BLUE, new Color(165, 42, 42), // brown
            Color.GREEN, Color.RED, Color.BLACK};
    private static AnnotationSetter instance;

    @Override
    public int getMaxValue() {
        return 0;
    }

    @Override
    public int getMinValue() {
        return 0;
    }

    @Override
    public void prepareForDimension(org.pepsoft.worldpainter.Dimension dim) {

    }

    @Override
    public IMappingValue instantiateFrom(Object[] data) {
        return getInstance();
    }

    private static AnnotationSetter getInstance() {
        if (instance == null) instance = new AnnotationSetter();
        return instance;
    }

    @Override
    public Object[] getSaveData() {
        return new Object[0];
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

    @Override
    public void paint(Graphics g, int value, Dimension dim) {
        g.setColor(COLORS[value]);
        g.fillRect(0, 0, dim.width, dim.height);
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.ANNOTATION;
    }

    @Override
    public String getName() {
        return Annotations.INSTANCE.getName();
    }

    @Override
    public String getDescription() {
        return Annotations.INSTANCE.getName();
    }

    @Override
    public int getValueAt(org.pepsoft.worldpainter.Dimension dim, int x, int y) {
        return dim.getLayerValueAt(Annotations.INSTANCE, x, y);
    }

    @Override
    public void setValueAt(org.pepsoft.worldpainter.Dimension dim, int x, int y, int value) {
        dim.setLayerValueAt(Annotations.INSTANCE, x, y, value);
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && this.getClass().equals(obj.getClass());
    }
}
