package org.ironsight.wpplugin.macromachine.operations.ApplyToMap;

import org.ironsight.wpplugin.macromachine.operations.MappingAction;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.ironsight.wpplugin.macromachine.Gui.IDisplayUnitCellRenderer.DEFAULT_BACKGROUND;
import static org.ironsight.wpplugin.macromachine.Gui.IDisplayUnitCellRenderer.DEFAULT_FOREGROUND;
import static org.ironsight.wpplugin.macromachine.Gui.LayerMappingTopPanel.header1Font;

public class BreakpointButtonPanel extends JPanel implements DebugUserInterface {
    private ArrayList<MappingAction> breakpoints;
    private JButton stepperButton;
    private JButton startDebugButton;
    private JButton abortButton;
    private JButton startButton;
    private JProgressBar progressBar;
    private boolean doContinue;
    private boolean doAbort = false;
    private boolean isRunning = false;
    private BreakpointListener stepperVisulaizer;
    private Supplier<BreakpointListener> getStepperVisualizer;

    public BreakpointButtonPanel(Consumer<Boolean> onUserStartsMacro,
                                 Supplier<BreakpointListener> getStepperVisualizer) {
        this.getStepperVisualizer = getStepperVisualizer;
        this.startButton = new MacroMachineButton("▶");
        startButton.setToolTipText("Start macro");
        startButton.addActionListener(l -> onUserStartsMacro.accept(false));

        this.stepperButton = new MacroMachineButton("⏩");
        stepperButton.setToolTipText("Continue next step");
        this.abortButton = new MacroMachineButton("◼");
        abortButton.setToolTipText("abort current macro");

        startDebugButton = new MacroMachineButton("\uD83D\uDC1E"); /*bug*/
        startDebugButton.addActionListener(l -> onUserStartsMacro.accept(Boolean.TRUE));
        startDebugButton.setToolTipText("Start macro with debugger");

        this.setLayout(new BorderLayout());

        progressBar = new JProgressBar();
        progressBar.setMinimum(0);
        progressBar.setMaximum(100);
        progressBar.setStringPainted(true); // Show percentage text
        this.add(progressBar, BorderLayout.NORTH);

        JPanel buttons = new JPanel(new FlowLayout());
        this.add(buttons, BorderLayout.CENTER);
        buttons.add(startButton);
        buttons.add(startDebugButton);
        buttons.add(stepperButton);
        buttons.add(abortButton);

        stepperButton.addActionListener(f -> this.onStepperButton());
        abortButton.addActionListener(f -> this.onAbortButton());
        setButtonsActive(false);
    }

    public static void main(String[] args) {
        // Create a JFrame
        JFrame frame = new JFrame("Emoji Button Example");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new java.awt.FlowLayout());
        frame.setSize(300, 200);

        DebugUserInterface panel = new BreakpointButtonPanel(f -> {
        }, () ->
                new BreakpointListener() {
                    @Override
                    public void OnReachedBreakpoint(int idx) {

                    }

                    @Override
                    public void PostReachedBreakpoint(int idx) {

                    }

                    @Override
                    public void SetBreakpoints(ArrayList<MappingAction> breakpoints) {

                    }

                    @Override
                    public void afterEverything() {

                    }
                }
        );
        // Add the button to the frame
        frame.add((JPanel) panel);

        // Make the frame visible
        frame.setVisible(true);
        for (int i = 0; i < 5; i++) {
            panel.CheckBreakpointStatus(i, MappingAction.getNewEmptyAction().withName("Action " + i));
        }
    }

    private void onStepperButton() {
        doContinue = true;
    }

    private void onAbortButton() {
        this.doAbort = true;
    }

    @Override
    public BreakpointReaction CheckBreakpointStatus(int index, MappingAction action) {
        if (doAbort) {
            return BreakpointReaction.ABORT;
        }
        if (doContinue) {
            return BreakpointReaction.CONTINUE;
        }
        return BreakpointReaction.WAIT;
    }

    @Override
    public void OnReachedBreakpoint(int idx) {
        SwingUtilities.invokeLater(()->{
            stepperButton.setToolTipText("continue with " + breakpoints.get(idx));
            stepperButton.setEnabled(true);
            progressBar.setValue(idx);
        });
        stepperVisulaizer.OnReachedBreakpoint(idx);
    }

    @Override
    public void PostReachedBreakpoint(int idx) {
        doContinue = false;
        SwingUtilities.invokeLater(()->{
            stepperButton.setToolTipText("continue");
            stepperButton.setEnabled(false);
        });
    }

    private void resetState() {
        doAbort = false;
    }

    @Override
    public void SetBreakpoints(ArrayList<MappingAction> breakpoints) {
        this.breakpoints = breakpoints;
        doAbort = false;
        this.stepperVisulaizer = getStepperVisualizer.get();
        stepperVisulaizer.SetBreakpoints(breakpoints);

        SwingUtilities.invokeLater(() ->{
            this.progressBar.setMinimum(0);
            this.progressBar.setMaximum(breakpoints.size());
            setButtonsActive(true);
        });
    }

    @Override
    public boolean isAbort() {
        return doAbort;
    }

    private void setButtonsActive(boolean isRunning) {
        startButton.setEnabled(!isRunning);
        startDebugButton.setEnabled(!isRunning);
        abortButton.setEnabled(isRunning);
        stepperButton.setEnabled(isRunning);
        progressBar.setValue(isRunning ? 0 : progressBar.getMaximum());
        progressBar.setVisible(isRunning);
    }

    @Override
    public void afterEverything() {
        setButtonsActive(false);
        stepperVisulaizer.afterEverything();
    }

    class MacroMachineButton extends JButton {
        public MacroMachineButton(String text) {
            this.setBackground(DEFAULT_BACKGROUND);
            this.setForeground(DEFAULT_FOREGROUND);
            this.setFont(header1Font);
            this.setText(text);
        }
    }

    ;
}
