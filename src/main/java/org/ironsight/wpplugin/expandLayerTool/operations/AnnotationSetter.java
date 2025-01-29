package org.ironsight.wpplugin.expandLayerTool.operations;

import org.pepsoft.worldpainter.layers.Annotations;

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
}
