package org.demo.wpplugin.operations;

import javax.swing.*;
import java.util.function.Consumer;

public interface OptionsLabel {
    static OptionsLabel numericInput(String text, String tooltip, SpinnerNumberModel number, Consumer<Float> setOptionValue, Runnable notifyAfterReconfigure) {
        JLabel textL = new JLabel();
        textL.setText(text);
        textL.setToolTipText(tooltip);

        JSpinner spinner = new JSpinner();
        spinner.setModel(number);
        spinner.setEnabled(true);
        spinner.addChangeListener(
                evt -> {
                    setOptionValue.accept(((Double) spinner.getValue()).floatValue());
                    notifyAfterReconfigure.run();
                }
        );

        return () -> new JComponent[]{textL, spinner};
    }

    JComponent[] getLabels();
}

