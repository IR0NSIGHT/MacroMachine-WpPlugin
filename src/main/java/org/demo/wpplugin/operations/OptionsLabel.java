package org.demo.wpplugin.operations;

import javax.swing.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public interface OptionsLabel {
    JLabel getLabel();

    JSpinner getSpinner();

    void updateValueFromOption();

    static OptionsLabel numericInput(String text, String tooltip, SpinnerNumberModel number, Consumer<Float> setOptionValue, Supplier<Float> getOptionValue, Runnable updateGuiFromOptions) {
        JLabel textL = new JLabel();
        textL.setText(text);
        textL.setToolTipText(tooltip);

        JSpinner spinner = new JSpinner();
        spinner.setModel(number);
        spinner.setEnabled(true);
        spinner.addChangeListener(
                evt -> {
                    setOptionValue.accept(((Double) spinner.getValue()).floatValue());
                    updateGuiFromOptions.run();
                }
        );

        OptionsLabel oL = new OptionsLabel() {
            @Override
            public JLabel getLabel() {
                return textL;
            }

            @Override
            public JSpinner getSpinner() {
                return spinner;
            }

            @Override
            public void updateValueFromOption() {
                spinner.setValue(getOptionValue.get().doubleValue());
            }
        };
        return oL;
    }
}

