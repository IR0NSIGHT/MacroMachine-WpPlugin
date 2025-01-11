package org.ironsight.wpplugin.expandLayerTool.Gui;

import org.ironsight.wpplugin.expandLayerTool.operations.Gradient;

import javax.swing.*;
import javax.swing.event.ChangeListener;
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
    private boolean blockEventhandling;
    private JTextField[] pointFields;
    private JTextField[] valueFields;
    private JSlider[] sliderFields;

    public GradientEditor(Gradient gradient, Consumer<Gradient> update, Consumer<Gradient> submit) {
        this.gradient = gradient;
        this.update = update;
        this.submit = submit;
        setup();
    }

    private void onSliderInputChange() {    //this is jank but im lazy
        if (blockEventhandling)
            return;
        for (int i = 0; i < gradient.positions.length; i++) {
            gradient.values[i] = sliderFields[i].getValue() / 100f;
        }
        //trigger input update
        SwingUtilities.invokeLater(() -> updateGuiFromGradient(gradient));
    }

    private void updateGuiFromGradient(Gradient gradient) {
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
                sliderFields[i].setValue(valueInt);
                valueFields[i].setText(Integer.toString(valueInt));

                int pointInt = Math.round(100 * gradient.positions[i]);
                pointFields[i].setText(Integer.toString(pointInt));
            }

            if (!invalid) {
                update.accept(gradient);
            }
            warning.setVisible(invalid);
            warningText.setVisible(invalid);
        }
        blockEventhandling = false;
    }

    private void onValueInputChange() {
        if (blockEventhandling)
            return;
        for (int i = 0; i < gradient.positions.length; i++) {
            try {
                gradient.values[i] = Float.parseFloat(valueFields[i].getText()) / 100f;
            } catch (NumberFormatException ignored) {
            }
        }
        //trigger input update
        SwingUtilities.invokeLater(() -> updateGuiFromGradient(gradient));

    }

    private void onPointInputChange() {
        if (blockEventhandling)
            return;
        for (int i = 0; i < gradient.positions.length; i++) {
            gradient.values[i] = Float.parseFloat(pointFields[i].getText()) / 100f;
        }
        //trigger input update
        SwingUtilities.invokeLater(() -> updateGuiFromGradient(gradient));
    }

    private void setup() {
        removeAll();
        valueFields = new JTextField[gradient.positions.length];
        sliderFields = new JSlider[gradient.positions.length];
        pointFields = new JTextField[gradient.positions.length];

        // Panel for editing points
        JPanel baseGrid = new JPanel(new GridLayout(gradient.positions.length + 4, 3, 5, 5));
        baseGrid.setBorder(BorderFactory.createTitledBorder("Points"));
        {   //warning row
            baseGrid.add(warning);
            baseGrid.add(warningText);
            baseGrid.add(new JLabel());
        }

        {   //HEADER
            baseGrid.add(new JLabel("Up to width %"));
            baseGrid.add(new JLabel("apply with chance %"));
            baseGrid.add(new JLabel());
        }

        DocumentListener pointChangeListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                onPointInputChange();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                onPointInputChange();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                // This is fired for attribute changes, which is uncommon for plain text fields.
            }
        };

        DocumentListener valueChangeListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                onValueInputChange();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                onValueInputChange();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                // This is fired for attribute changes, which is uncommon for plain text fields.
            }
        };
        ChangeListener sliderChangeListener = e -> onSliderInputChange();

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
                field.getDocument().addDocumentListener(pointChangeListener);
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
                field.getDocument().addDocumentListener(valueChangeListener);
                baseGrid.add(valueFields[i]);
            }

            {
                sliderFields[i] = new JSlider(0, 100, Math.round(gradient.values[i] * 100));
                sliderFields[i].addChangeListener(sliderChangeListener);
                baseGrid.add(sliderFields[i]);
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

        {
            baseGrid.add(increaseRowsButton);
            baseGrid.add(decreaseRowsButton);
            baseGrid.add(new JLabel());
        }

        {
            baseGrid.add(submitButton);
            baseGrid.add(cancelButton);
            baseGrid.add(new JLabel());
        }

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
