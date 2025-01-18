package org.ironsight.wpplugin.expandLayerTool.Gui;

import org.ironsight.wpplugin.expandLayerTool.operations.Gradient;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.function.Consumer;

public class GradientEditor extends JPanel {
    private final Consumer<Gradient> submit;
    private final Consumer<Gradient> update;
    JLabel[] warnings = new JLabel[]{new JLabel("WARNING:"), new JLabel("some entries are invalid"), new JLabel(),
            new JLabel()};
    private Gradient gradient;
    private boolean invalid;
    private boolean blockEventhandling;
    private JTextField[] positionTextFields;
    private JTextField[] valueTextFields;
    private NumericSlider[] valueSliders;
    private NumericSlider[] positionSliders;

    public GradientEditor(Gradient gradient, Consumer<Gradient> update, Consumer<Gradient> submit) {
        this.gradient = gradient;
        this.update = update;
        this.submit = submit;
        setup();
    }

    private void setup() {
        removeAll();
        valueSliders = new NumericSlider[gradient.positions.length];
        positionSliders = new NumericSlider[gradient.positions.length];

        Consumer<JComponent[]> addRow;
        {
            // Create a panel with GridBagLayout
            JPanel baseGrid = new JPanel(new GridLayout(0, 2));

            for (Component comp : baseGrid.getComponents()) {
                ((JComponent) comp).setAlignmentX(Component.TOP_ALIGNMENT); // Center align horizontally
            }

            addRow = components -> {
                baseGrid.add(components[0]);
                baseGrid.add(components[1]);
            };
            this.add(baseGrid);
        }
        addRow.accept(warnings);
        Arrays.stream(warnings).forEach(errorLabel -> {
            errorLabel.setFont(new Font("Arial", Font.BOLD, 24)); // Bold font, size 24

            // Set text color to red
            errorLabel.setForeground(Color.RED);

        });

        addRow.accept(new JLabel[]{new JLabel("Up to width %"), new JLabel("apply with chance %")});

        ChangeListener sliderChangeListener = e -> onSliderInputChange();

        for (int i = 0; i < gradient.positions.length; i++) {
            {
                valueSliders[i] = new NumericSlider();
                valueSliders[i].addChangeListener(sliderChangeListener);
            }

            {
                positionSliders[i] = new NumericSlider();
                positionSliders[i].addChangeListener(sliderChangeListener);
            }
        }

        for (int i = 0; i < positionSliders.length; i++)
            addRow.accept(new JComponent[]{positionSliders[i], valueSliders[i]});

        {
            JButton increaseRowsButton = new JButton("+");
            increaseRowsButton.addActionListener(e -> {
                float[] updatedPoints = new float[gradient.positions.length + 1];
                float[] updatedValues = new float[gradient.positions.length + 1];
                for (int i = 0; i < gradient.positions.length; i++) {
                    updatedPoints[i] = gradient.positions[i];
                    updatedValues[i] = gradient.values[i];
                }
                gradient = new Gradient(updatedPoints, updatedValues);
                setup();
            });

            JButton decreaseRowsButton = new JButton("-");
            decreaseRowsButton.addActionListener(e -> {
                float[] updatedPoints = new float[gradient.positions.length - 1];
                float[] updatedValues = new float[gradient.positions.length - 1];
                for (int i = 0; i < updatedPoints.length; i++) {
                    updatedPoints[i] = gradient.positions[i];
                    updatedValues[i] = gradient.values[i];
                }
                gradient = new Gradient(updatedPoints, updatedValues);
                setup();
            });
            addRow.accept(new JComponent[]{increaseRowsButton, decreaseRowsButton});
        }
        {
            JButton submitButton = new JButton("Submit");
            JButton cancelButton = new JButton("Cancel");
            submitButton.addActionListener((ActionEvent e) -> {
                submit.accept(this.gradient);
            });
            addRow.accept(new JComponent[]{submitButton, cancelButton});
        }

        SwingUtilities.invokeLater(() -> updateGradient(gradient));
        revalidate();
        repaint();
    }

    private Gradient validateGradient(Gradient gradient) {
        float[] points = gradient.positions.clone();
        float[] values = gradient.values.clone();

        for (int i = 1; i < points.length; i++) {
            points[i] = Math.max(points[i - 1] + 0.01f, points[i]);
        }

        for (int i = 0; i < points.length; i++) {
            points[i] = Math.max(0f, points[i]);
            points[i] = Math.min(1f, points[i]);

            values[i] = Math.max(0f, values[i]);
            values[i] = Math.min(1f, values[i]);
        }

        Gradient out = new Gradient(points, values);
        return out;
    }

    private void updateGradient(Gradient gradient) {
        gradient = validateGradient(gradient);
        this.gradient = gradient;
        blockEventhandling = true;
        invalid = false;
        try {
            //validate points are monotone rising
            for (int i = 1; i < gradient.positions.length; i++) {
                if (!(gradient.positions[i - 1] < gradient.positions[i])) {
                    invalid = true;
                    break;
                }
            }
        } catch (NumberFormatException ignored) {
            invalid = true;
        } catch (Exception ex) {
            invalid = true;
        } finally {
            for (int i = 0; i < gradient.positions.length; i++) {
                int valueInt = Math.round(100 * gradient.values[i]);
                valueSliders[i].setValue(valueInt);

                int pointInt = Math.round(100 * gradient.positions[i]);
                positionSliders[i].setValue(pointInt);
            }

            if (!invalid) {
                update.accept(gradient);
            }
            Arrays.stream(warnings).forEach(w -> w.setVisible(invalid));
        }
        blockEventhandling = false;
    }

    private void onSliderInputChange() {
        if (blockEventhandling) return;
        for (int i = 0; i < gradient.positions.length; i++) {
            gradient.values[i] = valueSliders[i].getValue() / 100f;
            gradient.positions[i] = positionSliders[i].getValue() / 100f;
        }
        //trigger input update
        SwingUtilities.invokeLater(() -> updateGradient(gradient));
    }

    private void onValueInputChange() {
        if (blockEventhandling) return;
        for (int i = 0; i < gradient.positions.length; i++) {
            try {
                float value = Float.parseFloat(valueTextFields[i].getText()) / 100f;
                if (value < 0) value = 0;
                if (value > 1) value = 1;
                gradient.values[i] = value;

            } catch (NumberFormatException ignored) {
            }
        }
        //trigger input update
        SwingUtilities.invokeLater(() -> updateGradient(gradient));

    }

    private void onPointInputChange() {
        if (blockEventhandling) return;
        for (int i = 0; i < gradient.positions.length; i++) {
            try {
                float value = Float.parseFloat(positionTextFields[i].getText()) / 100f;
                if (value < 0) value = 0;
                if (value > 1) value = 1;
                gradient.positions[i] = value;
            } catch (NumberFormatException ignored) {
            }
        }
        //trigger input update
        SwingUtilities.invokeLater(() -> updateGradient(gradient));
    }


}
