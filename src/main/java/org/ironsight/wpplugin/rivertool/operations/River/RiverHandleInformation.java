package org.ironsight.wpplugin.rivertool.operations.River;

import org.ironsight.wpplugin.rivertool.Gui.OptionsLabel;
import org.ironsight.wpplugin.rivertool.Gui.PathHistogram;
import org.ironsight.wpplugin.rivertool.geometry.HeightDimension;
import org.ironsight.wpplugin.rivertool.geometry.PaintDimension;
import org.ironsight.wpplugin.rivertool.operations.ContinuousCurve;
import org.ironsight.wpplugin.rivertool.pathing.Path;
import org.ironsight.wpplugin.rivertool.pathing.PointInterpreter;
import org.ironsight.wpplugin.rivertool.pathing.PointUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.function.Consumer;

import static org.ironsight.wpplugin.rivertool.Gui.OptionsLabel.numericInput;
import static org.ironsight.wpplugin.rivertool.operations.ApplyPath.ApplyRiverOperation.angleOf;
import static org.ironsight.wpplugin.rivertool.operations.EditPath.EditPathOperation.COLOR_CURVE;
import static org.ironsight.wpplugin.rivertool.operations.River.RiverHandleInformation.RiverInformation.*;
import static org.ironsight.wpplugin.rivertool.pathing.PointInterpreter.PointType.RIVER_2D;

public class RiverHandleInformation {
    public static final float INHERIT_VALUE = Float.MIN_VALUE;

    /**
     * @param x
     * @param y
     * @return a handle with given position and the rest of meta values set to INHERIT
     */
    public static float[] riverInformation(int x, int y) {
        return riverInformation(x, y, INHERIT_VALUE, INHERIT_VALUE, INHERIT_VALUE, INHERIT_VALUE, INHERIT_VALUE);
    }

    public static float[] riverInformation(int x, int y, float riverRadius, float riverDepth, float beachRadius,
                                           float transitionRadius, float waterZ) {
        float[] out = new float[RIVER_2D.size];
        out[0] = x;
        out[1] = y;
        out = setValue(out, RiverInformation.RIVER_RADIUS, riverRadius);
        out = setValue(out, RiverInformation.RIVER_DEPTH, riverDepth);
        out = setValue(out, RiverInformation.BEACH_RADIUS, beachRadius);
        out = setValue(out, RiverInformation.TRANSITION_RADIUS, transitionRadius);
        out = setValue(out, RiverInformation.WATER_Z, waterZ);
        return out;
    }

    public static float[] setValue(float[] point, RiverInformation information, float value) {
        float[] out = point.clone();
        out[PositionSize.SIZE_2_D.value + information.idx] = value;
        return out;
    }

    public static float[] positionInformation(float x, float y, PointInterpreter.PointType type) {
        float[] point = new float[type.size];
        point[0] = x;
        point[1] = y;
        return point;
    }

    public static boolean validateRiver2D(ArrayList<float[]> handles) {
        for (float[] handle : handles) {
            if (handle.length != RIVER_2D.size) {
                return false;
            }
            for (RiverInformation information : RiverInformation.values()) {
                float value = getValue(handle, information);
                if (value != INHERIT_VALUE && (value < information.min || value > information.max)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static float getValue(float[] point, RiverInformation information) {
        return point[PositionSize.SIZE_2_D.value + information.idx];
    }

    public static JDialog riverRadiusEditor(JFrame parent, Path path, int selectedHandleIdx,
                                            Consumer<Path> overWritePath,
                                            HeightDimension heightDimension) {
        JDialog dialog = new JDialog(parent, "Dialog Title", true); // Modal
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE); // Only close the dialog

        // Create a custom panel that will display the image
        ContinuousCurve curve = ContinuousCurve.fromPath(path, heightDimension);

        // Calculate dialog size as a percentage of the screen size
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int dialogWidth = (int) (screenSize.width * 0.5); // 50% of screen width
        int dialogHeight = (int) (screenSize.height * 0.5); // 50% of screen height


        PathHistogram imageLabel = new PathHistogram(path, selectedHandleIdx, heightDimension);
        imageLabel.setSize(dialogWidth, dialogHeight);
        // Add the JLabel to the dialog
        dialog.add(imageLabel);


        // Add a listener for when the dialog is closed
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                Path p = imageLabel.getPath();
                overWritePath.accept(p);
            }
        });

        // Set the dialog size
        dialog.setSize(dialogWidth, dialogHeight);

        // Optionally, center the dialog on the screen
        dialog.setLocationRelativeTo(parent);

        // Pack the dialog to ensure proper layout
        dialog.pack();

        return dialog;
    }

    public static OptionsLabel[] Editor(float[] point, Consumer<float[]> onSubmitCallback, Runnable onChanged) {
        OptionsLabel[] options = new OptionsLabel[RiverInformation.values().length];
        int i = 0;
        for (RiverInformation information : RiverInformation.values()) {
            SpinnerNumberModel model = new SpinnerNumberModel(getValue(point, information), information.min - 1,
                    information.max, 1f);

            options[i++] = numericInput(information.displayName, information.toolTip, model, newValue -> {
                float allowed = sanitizeInput(newValue, information);
                onSubmitCallback.accept(setValue(point, information, allowed));
            }, onChanged);
        }
        return options;
    }

    public static void DrawRiverPath(Path path, ContinuousCurve curve, PaintDimension dim) throws IllegalAccessException {
        if (path.type != RIVER_2D) throw new IllegalArgumentException("path is not river: " + path.type);

        for (int i = 1; i < curve.curveLength() - 1; i++) {
            int color = COLOR_CURVE;
            Point curvePointP = curve.getPos(i);
            dim.setValue(curvePointP.x, curvePointP.y, COLOR_CURVE);
            if (i % 10 != 0)    //only mark meta info every 10 blocks
                continue;
            //PointUtils.markPoint(getPoint2D(p), COLOR_CURVE, SIZE_DOT, dim);
            float radius = 0f;
            /*
            for (RiverInformation info : new RiverInformation[]{RIVER_RADIUS, BEACH_RADIUS, TRANSITION_RADIUS}) {
                radius += curve.getInfo(info, i);

                int tangentX = curve.getPosX(i + 1) - curve.getPosX(i - 1);
                int tangentY = curve.getPosY(i + 1) - curve.getPosY(i - 1);
                double tangentAngle = angleOf(tangentX, tangentY);

                int x = (int) Math.round(radius * Math.cos(tangentAngle + Math.toRadians(90)));
                int y = (int) Math.round(radius * Math.sin(tangentAngle + Math.toRadians(90)));
                dim.setValue(curvePointP.x + x, curvePointP.y + y, color);

                x = (int) Math.round(radius * Math.cos(tangentAngle + Math.toRadians(-90)));
                y = (int) Math.round(radius * Math.sin(tangentAngle + Math.toRadians(-90)));
                dim.setValue(curvePointP.x + x, curvePointP.y + y, color);
                color = color + 1 % 15;
            }

             */
        }

        for (int i = 0; i < path.amountHandles(); i++) {
            float[] handle = path.handleByIndex(i);

            //RIVER RADIUS
            if (!(getValue(handle, RIVER_RADIUS) == INHERIT_VALUE))
                PointUtils.drawCircle(
                        PointUtils.getPoint2D(handle),
                        COLOR_CURVE,
                        getValue(handle, RIVER_RADIUS),
                        dim, getValue(handle,
                        RIVER_RADIUS) == RiverHandleInformation.INHERIT_VALUE);

        }
    }

    public static float sanitizeInput(float input, RiverInformation information) {
        if (input < information.min)
            return INHERIT_VALUE;
        return Math.min(input, information.max);
    }

    public enum RiverInformation {
        RIVER_RADIUS(0, "river radius", "radius of the river ", 0, 1000),
        RIVER_DEPTH(1, "river depth", "depth of the" +
                " river ", 0, 1000),
        BEACH_RADIUS(2, "beach radius", "radius of the beach ", 0, 1000),
        TRANSITION_RADIUS(3, "transition radius", "radius of the transition blending with original terrain ", 0,
                1000),
        WATER_Z(4, "water level", "water level position on z axis", -10000, 100000);
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
        SIZE_1_D(1), SIZE_2_D(2), SIZE_3_D(3);
        public final int value;

        PositionSize(int idx) {
            this.value = idx;
        }
    }


}
