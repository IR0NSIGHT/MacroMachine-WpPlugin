package org.demo.wpplugin.operations.River;

import org.demo.wpplugin.geometry.PaintDimension;
import org.demo.wpplugin.layers.PathPreviewLayer;
import org.demo.wpplugin.operations.EditPath.EditPathOperation;
import org.demo.wpplugin.operations.OptionsLabel;
import org.demo.wpplugin.pathing.Path;
import org.demo.wpplugin.pathing.PointInterpreter;
import org.demo.wpplugin.pathing.PointUtils;

import javax.swing.*;
import java.util.ArrayList;
import java.util.function.Consumer;

import static org.demo.wpplugin.operations.EditPath.EditPathOperation.*;
import static org.demo.wpplugin.operations.OptionsLabel.numericInput;
import static org.demo.wpplugin.operations.River.RiverHandleInformation.RiverInformation.RIVER_RADIUS;
import static org.demo.wpplugin.pathing.PointUtils.getPoint2D;

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

    /**
     * @param x
     * @param y
     * @return a handle with given position and the rest of meta values set to INHERIT
     */
    public static float[] riverInformation(int x, int y) {
        return new float[]{x, y, INHERIT_VALUE, INHERIT_VALUE, INHERIT_VALUE, INHERIT_VALUE};
    }

    public static boolean validateRiver2D(float[] handle) {
        if (handle.length != PointInterpreter.PointType.RIVER_2D.size) {
            return false;
        }
        for (RiverInformation information : RiverInformation.values()) {
            float value = getValue(handle, information);
            if (value != INHERIT_VALUE && (value < information.min || value > information.max)) {
                return false;
            }
        }
        return true;
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
        RIVER_RADIUS(0,"river radius", "radius of the river ",0,50),
        RIVER_DEPTH(1, "river depth", "depth of the river ",0,20),
        BEACH_RADIUS(2, "beach radius", "radius of the beach ",0,20),
        TRANSITION_RADIUS(3,"transition radius", "radius of the transition blending with original terrain ",0,50),;
        public final int idx;
        public final String displayName;
        public final String toolTip;
        public final float min;
        public final float max;
        RiverInformation(int idx, String displayName, String toolTip, float min, float max) {
            this.min = min;
            this.max = max;
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

    public static void DrawRiverPath(Path path, PaintDimension dim) {
        Path clone = path.clone();
        if (path.type != PointInterpreter.PointType.RIVER_2D)
            throw new IllegalArgumentException("path is not river: " + path.type);
        assert path.equals(clone);
        ArrayList<float[]> curve = path.continousCurve();
        assert path.equals(clone);

        for (float[] p : path.continousCurve()) {
            PointUtils.markPoint(getPoint2D(p), COLOR_CURVE, SIZE_DOT, dim);
        }

        assert path.equals(clone);
        float[] radii = interpolateRadii(path);
        assert path.equals(clone);

        for (int i = 0; i < path.amountHandles(); i++) {
            float[] handle = path.handleByIndex(i);
            PointUtils.markPoint(getPoint2D(handle), COLOR_HANDLE, SIZE_MEDIUM_CROSS,
                    dim);

            //RIVER RADIUS
            float thisRadius = radii[i];
            PointUtils.drawCircle(getPoint2D(handle), thisRadius, dim,
                    PathPreviewLayer.INSTANCE,
                    getValue(handle, RIVER_RADIUS) == RiverHandleInformation.INHERIT_VALUE);

        }
        assert path.equals(clone);

    }
}
