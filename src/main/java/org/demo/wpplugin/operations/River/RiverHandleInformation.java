package org.demo.wpplugin.operations.River;

import org.demo.wpplugin.operations.OptionsLabel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.function.Consumer;

public class RiverHandleInformation {
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
        return new float[]{x, y, 10, 3, 5, 25};
    }

    public static OptionsLabel Editor(float[] point, Consumer<float[]> onSubmitCallback) {
        // Create the panel for input fields and submit button
        JPanel panel = new JPanel(new GridLayout(5, 2));

        // Initialize input fields
        JTextField riverRadiusField = new JTextField(""+getValue(point, RiverInformation.RIVER_RADIUS), 10);
        JTextField riverDepthField = new JTextField(""+getValue(point, RiverInformation.RIVER_DEPTH), 10);
        JTextField beachRadiusField = new JTextField(""+getValue(point, RiverInformation.BEACH_RADIUS), 10);
        JTextField transitionRadiusField = new JTextField(""+getValue(point, RiverInformation.TRANSITION_RADIUS), 10);

        // Add labels and text fields to the panel
        panel.add(new JLabel("RIVER_RADIUS:"));
        panel.add(riverRadiusField);
        panel.add(new JLabel("RIVER_DEPTH:"));
        panel.add(riverDepthField);
        panel.add(new JLabel("BEACH_RADIUS:"));
        panel.add(beachRadiusField);
        panel.add(new JLabel("TRANSITION_RADIUS:"));
        panel.add(transitionRadiusField);

        // Create and add the submit button
        JButton submitButton = new JButton("Submit");
        submitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    // Parse the float values from the text fields
                    float riverRadius = Float.parseFloat(riverRadiusField.getText());
                    float riverDepth = Float.parseFloat(riverDepthField.getText());
                    float beachRadius = Float.parseFloat(beachRadiusField.getText());
                    float transitionRadius = Float.parseFloat(transitionRadiusField.getText());

                    float[] out = point.clone();
                    out = setValue(out, RiverInformation.RIVER_RADIUS, riverRadius);
                    out = setValue(out, RiverInformation.RIVER_DEPTH, riverDepth);
                    out = setValue(out, RiverInformation.BEACH_RADIUS, beachRadius);
                    out = setValue(out, RiverInformation.TRANSITION_RADIUS, transitionRadius);
                    onSubmitCallback.accept (out);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(null, "Please enter valid float values.", "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // Add the submit button to the panel
        panel.add(submitButton);
        return () -> new JComponent[]{panel};
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
