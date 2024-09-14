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
        OptionsLabel[] options = new OptionsLabel[RiverInformation.values().length];
        int i = 0;
        for (RiverInformation information : RiverInformation.values()) {
            SpinnerNumberModel model = new SpinnerNumberModel(getValue(point,information),INHERIT_VALUE,100,1f);

            options[i++] = numericInput(information.displayName,
                    information.toolTip,
                    model,
                    newValue -> {
                        onSubmitCallback.accept(setValue(point, information, newValue));
                    },
                    onChanged
            );
        }
        return options;
    }

    public enum RiverInformation {
        RIVER_RADIUS(0,"river radius", "radius of the river "),
        RIVER_DEPTH(1, "river depth", "depth of the river "),
        BEACH_RADIUS(2, "beach radius", "radius of the beach "),
        TRANSITION_RADIUS(3,"transition radius", "radius of the transition blending with original terrain "),;
        public final int idx;
        public final String displayName;
        public final String toolTip;
        RiverInformation(int idx, String displayName, String toolTip) {
            this.displayName = displayName;
            this.toolTip = toolTip;
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
