package org.demo.wpplugin.operations.River;

import org.demo.wpplugin.geometry.PaintDimension;
import org.demo.wpplugin.layers.renderers.DemoLayerRenderer;
import org.demo.wpplugin.operations.OptionsLabel;
import org.demo.wpplugin.pathing.Path;
import org.demo.wpplugin.pathing.PointInterpreter;
import org.demo.wpplugin.pathing.PointUtils;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.function.Consumer;

import static org.demo.wpplugin.operations.ApplyPath.ApplyRiverOperation.angleOf;
import static org.demo.wpplugin.operations.EditPath.EditPathOperation.*;
import static org.demo.wpplugin.operations.OptionsLabel.numericInput;
import static org.demo.wpplugin.operations.River.RiverHandleInformation.RiverInformation.RIVER_RADIUS;
import static org.demo.wpplugin.pathing.PointUtils.getPoint2D;
import static org.demo.wpplugin.pathing.PointUtils.markLine;

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
            SpinnerNumberModel model = new SpinnerNumberModel(getValue(point, information), INHERIT_VALUE, 100, 1f);

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

    public static void DrawRiverPath(Path path, PaintDimension dim, int selectedIdx) throws IllegalAccessException {
        if (path.type != PointInterpreter.PointType.RIVER_2D)
            throw new IllegalArgumentException("path is not river: " + path.type);
        ArrayList<float[]> curve = path.continousCurve();
        int[] curveIdxHandles = path.handleToCurveIdx();

        int startIdx = curveIdxHandles[Math.min(Math.max(0, selectedIdx - 2), curveIdxHandles.length - 1)];
        int endIdx = curveIdxHandles[Math.min(Math.max(0, selectedIdx + 2), curveIdxHandles.length - 1)];

        float[] riverPosition_X = new float[curve.size()];
        float[] riverPosition_Y = new float[curve.size()];
        for (int i = 0; i < curve.size(); i++) {
            float[] p = curve.get(i);
            riverPosition_X[i] = p[0];
            riverPosition_Y[i] = p[1];
        }


        for (int i = 1; i < curve.size()-1; i++) {
            float[] p = curve.get(i);
            int color = startIdx < i && i < endIdx ? DemoLayerRenderer.Dark_Cyan : COLOR_CURVE;
            PointUtils.markPoint(getPoint2D(p), COLOR_CURVE, SIZE_DOT, dim);

            for (RiverInformation info : RiverInformation.values()) {
                float radius = getValue(p, info);
                Point curvePointP = getPoint2D(p);

                int tangentX = Math.round(curve.get(i+1)[0]-curve.get(i-1)[0]);
                int tangentY =  Math.round(curve.get(i+1)[1]-curve.get(i-1)[1]);
                double tangentAngle = angleOf(tangentX, tangentY);

                int x = (int) Math.round(radius * Math.cos(tangentAngle + Math.toRadians(90)));
                int y = (int) Math.round(radius * Math.sin(tangentAngle + Math.toRadians(90)));
                dim.setValue(curvePointP.x + x, curvePointP.y + y, color);

                x = (int) Math.round(radius * Math.cos(tangentAngle + Math.toRadians(-90)));
                y = (int) Math.round(radius * Math.sin(tangentAngle + Math.toRadians(-90)));
                dim.setValue(curvePointP.x + x, curvePointP.y + y, color);
                color = color+1%15;
            }
        }

        if (path.amountHandles() > 1) {
            markLine(getPoint2D(path.handleByIndex(0)), getPoint2D(path.handleByIndex(1)), COLOR_HANDLE, dim);
            markLine(getPoint2D(path.getTail()), getPoint2D(path.getPreviousPoint(path.getTail())), COLOR_HANDLE, dim);
        }

        for (int i = 0; i < path.amountHandles(); i++) {
            float[] handle = path.handleByIndex(i);
            int size = SIZE_MEDIUM_CROSS;
            PointUtils.markPoint(getPoint2D(handle), DemoLayerRenderer.Yellow, size,
                    dim);

            //RIVER RADIUS
            if (!(getValue(handle, RIVER_RADIUS) == INHERIT_VALUE))
                PointUtils.drawCircle(getPoint2D(handle), getValue(handle, RIVER_RADIUS), dim,
                        getValue(handle, RIVER_RADIUS) == RiverHandleInformation.INHERIT_VALUE);

        }
    }

    public enum RiverInformation {
        RIVER_RADIUS(0, "river radius", "radius of the river ", 0, 100),
        RIVER_DEPTH(1, "river depth", "depth of the river ", 0, 100),
        BEACH_RADIUS(2, "beach radius", "radius of the beach ", 0, 100),
        TRANSITION_RADIUS(3, "transition radius", "radius of the transition blending with original terrain ", 0, 100),
        ;
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
}
