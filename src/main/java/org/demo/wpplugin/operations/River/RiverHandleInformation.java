package org.demo.wpplugin.operations.River;

import org.demo.wpplugin.operations.OptionsLabel;

import javax.swing.*;
import java.util.function.Consumer;

import static org.demo.wpplugin.operations.OptionsLabel.numericInput;

public class RiverHandleInformation {
    public static final float INHERIT_VALUE = -1;

    public static float getValue(float[] point, RiverInformation information) {
        return point[PositionSize.SIZE_2_D.value + information.idx];
    }

    public static float[] setValue(float[] point, RiverInformation information, float value) {
        float[] out = point.clone();
        out[PositionSize.SIZE_2_D.value + information.idx] = value;
        return out;
    }

    public static float[] riverInformation(int x, int y, float riverRadius, float riverDepth, int beachRadius,
                                           int transitionRadius) {
        return new float[]{x, y, riverRadius, riverDepth, beachRadius, transitionRadius};
    }

    public static float[] riverInformation(int x, int y) {
        return new float[]{x, y, INHERIT_VALUE, INHERIT_VALUE, INHERIT_VALUE, INHERIT_VALUE};
    }

    public static OptionsLabel[] Editor(float[] point, Consumer<float[]> onSubmitCallback, Runnable onChanged) {
        OptionsLabel[] options = new OptionsLabel[4];

        {
            SpinnerNumberModel model = new SpinnerNumberModel(getValue(point, RiverInformation.RIVER_RADIUS),INHERIT_VALUE,100,1f);
            options[0] = numericInput("river radius",
                    "radius of the river at this handle",
                    model,
                    newValue -> {
                        onSubmitCallback.accept(setValue(point, RiverInformation.RIVER_RADIUS, newValue));
                    },
                    onChanged
            );
        }

        {
            SpinnerNumberModel model = new SpinnerNumberModel(0f,INHERIT_VALUE,100,1f);
            model.setValue(getValue(point, RiverInformation.RIVER_DEPTH));
            options[1] = numericInput("river depth",
                    "water depth of the river at this handle",
                    model, newValue -> {
                        onSubmitCallback.accept(setValue(point, RiverInformation.RIVER_DEPTH, newValue));
                    },
                    onChanged
            );
        }

        {
            SpinnerNumberModel model = new SpinnerNumberModel(0f,INHERIT_VALUE,100,1f);
            model.setValue(getValue(point, RiverInformation.BEACH_RADIUS));
            options[2] = numericInput("beach radius",
                    "radius of the beach around the river at this handle",
                    model,
                    newValue -> {
                        onSubmitCallback.accept(setValue(point, RiverInformation.BEACH_RADIUS, newValue));
                    },
                    onChanged
            );
        }
        {
            SpinnerNumberModel model = new SpinnerNumberModel(0f,INHERIT_VALUE,100,1f);
            model.setValue(getValue(point, RiverInformation.TRANSITION_RADIUS));
            options[3] = numericInput("transition radius",
                    "radius of the smooth transition into the original landscape at this handle",
                    model, newValue -> {
                        onSubmitCallback.accept(setValue(point, RiverInformation.TRANSITION_RADIUS, newValue));
                    },
                    onChanged
            );
        }
        return options;
    }

    public enum RiverInformation {
        RIVER_RADIUS(0),
        RIVER_DEPTH(1),
        BEACH_RADIUS(2),
        TRANSITION_RADIUS(3);
        public final int idx;

        RiverInformation(int idx) {
            this.idx = idx;
        }
    }

    public enum PositionSize {
        SIZE_1_D(1),
        SIZE_2_D(2),
        SIZE_3_D(3);
        public final int value;

        PositionSize(int idx) {
            this.value = idx;
        }
    }
}
