package org.ironsight.wpplugin.rivertool.Gui;

import org.ironsight.wpplugin.rivertool.operations.River.RiverHandleInformation;

import javax.swing.*;
import java.text.ParseException;
import java.util.function.Consumer;

public interface OptionsLabel {
    static OptionsLabel numericInput(String text, String tooltip, SpinnerNumberModel number,
                                     Consumer<Float> setOptionValue, Runnable notifyAfterReconfigure) {
        JLabel textL = new JLabel();
        textL.setText(text);
        textL.setToolTipText(tooltip);

        JSpinner spinner = new JSpinner();
        spinner.setModel(number);
        JFormattedTextField f = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
        f.setFormatterFactory(new JFormattedTextField.AbstractFormatterFactory() {
            @Override
            public JFormattedTextField.AbstractFormatter getFormatter(JFormattedTextField tf) {
                // return your formatter
                return new JFormattedTextField.AbstractFormatter() {
                    @Override
                    public Object stringToValue(String text) throws ParseException {
                        if (text.equals("INHERIT")) {
                            return (double) RiverHandleInformation.INHERIT_VALUE;
                        }
                        return Double.parseDouble(text);
                    }

                    @Override
                    public String valueToString(Object value) throws ParseException {
                        if (value instanceof Double) {
                            float val = ((Double) value).floatValue();
                            if (val == RiverHandleInformation.INHERIT_VALUE)
                                return "INHERIT";
                            return String.format("%.2f",val);
                        }
                        return value.toString();
                    }
                };
            }
        });

        spinner.setEnabled(true);
        spinner.addChangeListener(
                evt -> {
                    setOptionValue.accept(((Double) spinner.getValue()).floatValue());
                    notifyAfterReconfigure.run();
                }
        );

        // Create a horizontal Box to hold the label and spinner on the same line
        Box horizontalBox = Box.createHorizontalBox();
        horizontalBox.add(textL);
        horizontalBox.add(Box.createHorizontalStrut(5)); // add spacing between label and spinner
        horizontalBox.add(spinner);

        return () -> new JComponent[]{horizontalBox};
    }

    JComponent[] getLabels();
}

