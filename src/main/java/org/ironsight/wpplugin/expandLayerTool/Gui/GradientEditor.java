package org.ironsight.wpplugin.expandLayerTool.Gui;

import org.ironsight.wpplugin.expandLayerTool.operations.Gradient;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.function.Consumer;

public class GradientEditor extends JPanel {
    private final Consumer<Gradient> submit;
    private final Consumer<Gradient> update;
    JLabel warning = new JLabel("WARNING:");
    JLabel warningText = new JLabel("some entries are invalid");
    private Gradient gradient;
    private boolean invalid;
    private JTextField[] pointFields;
    private JTextField[] valueFields;

    public GradientEditor(Gradient gradient, Consumer<Gradient> update, Consumer<Gradient> submit) {
        this.gradient = gradient;
        this.update = update;
        this.submit = submit;
        setup();
    }

    private void onInputChanged() {
        invalid = false;
        Gradient gr = this.gradient;
        try {
            // Parse updated points and values
            float[] updatedPoints = new float[gradient.positions.length];
            float[] updatedValues = new float[gradient.positions.length];

            for (int i = 0; i < gradient.positions.length; i++) {
                updatedPoints[i] = Float.parseFloat(pointFields[i].getText()) / 100f;
            }
            for (int i = 0; i < gradient.positions.length; i++) {
                updatedValues[i] = Float.parseFloat(valueFields[i].getText()) / 100f;
            }

            // Trigger callback with updated arrays
            gr = new Gradient(updatedPoints, updatedValues);

            //validate points are monotone rising
            for (int i = 1; i < updatedPoints.length; i++) {
                if (!(updatedPoints[i - 1] < updatedPoints[i])) {
                    invalid = true;
                    break;
                }
            }
        } catch (NumberFormatException ignored) {
            invalid = true;
        } catch (Exception ex) {
            invalid = true;
        } finally {
            this.gradient = gr;
            if (!invalid) {
                update.accept(gr);
            }
            warning.setVisible(invalid);
            warningText.setVisible(invalid);
        }
    }

    private void setup() {
        removeAll();
        valueFields = new JTextField[gradient.positions.length];
        pointFields = new JTextField[gradient.positions.length];

        // Panel for editing points
        JPanel baseGrid = new JPanel(new GridLayout(gradient.positions.length + 4, 2, 5, 5));
        baseGrid.setBorder(BorderFactory.createTitledBorder("Points"));
        baseGrid.add(warning);
        baseGrid.add(warningText);

        baseGrid.add(new JLabel("Up to width %"));
        baseGrid.add(new JLabel("apply with chance %"));

        DocumentListener textFieldCallback = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                onInputChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                onInputChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                // This is fired for attribute changes, which is uncommon for plain text fields.
            }

        };

        for (int i = 0; i < gradient.positions.length; i++) {
            {
                pointFields[i] = new JTextField(String.valueOf(Math.round(gradient.positions[i] * 100)));
                JTextField field = pointFields[i];
                field.addFocusListener(new FocusAdapter() {
                    @Override
                    public void focusGained(FocusEvent e) {
                        field.selectAll();
                    }
                });
                field.getDocument().addDocumentListener(textFieldCallback);
                baseGrid.add(pointFields[i]);
            }

            {
                valueFields[i] = new JTextField(String.valueOf(Math.round(gradient.values[i] * 100)));
                JTextField field = valueFields[i];
                field.addFocusListener(new FocusAdapter() {
                    @Override
                    public void focusGained(FocusEvent e) {
                        field.selectAll();
                    }
                });
                field.getDocument().addDocumentListener(textFieldCallback);
                baseGrid.add(valueFields[i]);
            }
        }

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
        JButton submitButton = new JButton("Submit");
        JButton cancelButton = new JButton("Cancel");
        baseGrid.add(increaseRowsButton);
        baseGrid.add(decreaseRowsButton);
        baseGrid.add(submitButton);
        baseGrid.add(cancelButton);

        submitButton.addActionListener((ActionEvent e) -> {
            submit.accept(this.gradient);
        });

        warning.setVisible(invalid);
        warningText.setVisible(invalid);

        this.add(baseGrid);
        revalidate();
        repaint();
    }


}
