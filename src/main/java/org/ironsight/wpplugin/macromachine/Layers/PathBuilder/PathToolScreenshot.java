package org.ironsight.wpplugin.macromachine.Layers.PathBuilder;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JSpinner;
import javax.swing.SwingUtilities;

public final class PathToolScreenshot
{
    private PathToolScreenshot() {
    }

    public static void main(String[] args) throws Exception {
        if ((args.length != 2 && args.length != 3) || (!args[1].equals("default") && !args[1].equals("dropdown")
                && !args[1].equals("river") && !args[1].equals("road"))) {
            throw new IllegalArgumentException(
                    "Usage: PathToolScreenshot <output.png> <default|dropdown|river|road> [slope-limit]");
        }
        if (GraphicsEnvironment.isHeadless())
            throw new IllegalStateException("A display is required to capture the Swing window");

        Path output = Path.of(args[0]);
        Files.createDirectories(output.toAbsolutePath().getParent());
        float slopeLimit = args.length == 3 ? Float.parseFloat(args[2]) : 0;
        capture(output, args[1], slopeLimit);
    }

    private static void capture(Path output, String mode, float slopeLimit) throws Exception {
        CaptureState state = new CaptureState();
        SwingUtilities.invokeAndWait(() -> {
            PathTool pathTool = new PathTool();
            JFrame frame = new JFrame("Road Tool Options");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setResizable(false);
            frame.add(pathTool.getOptionsPanel());
            frame.pack();
            frame.setSize(Math.max(420, frame.getWidth()), frame.getHeight());
            frame.setLocation(100, 100);
            state.frame = frame;
            state.dropdown = findComboBox(frame);
            if (state.dropdown == null)
                throw new IllegalStateException("Road options panel does not contain a combo box");
            List<JSpinner> spinners = findSpinners(frame);
            if (spinners.size() < 2)
                throw new IllegalStateException("Road options panel does not contain a slope spinner");
            state.slopeSpinner = spinners.get(1);
            state.slopeSpinner.setValue((double) slopeLimit);
            if (mode.equals("river") || mode.equals("road")) {
                String buttonText = mode.equals("river") ? "River preset" : "Road preset";
                JButton presetButton = findButton(frame, buttonText);
                if (presetButton == null)
                    throw new IllegalStateException("Road options panel does not contain " + buttonText);
                presetButton.doClick();
            }
            if (state.dropdown.getItemCount() != 6)
                throw new IllegalStateException("Expected six transition profiles");
            String expectedProfile = mode.equals("road") ? "Triangle" : "Sinus";
            if (!expectedProfile.equals(String.valueOf(state.dropdown.getSelectedItem())))
                throw new IllegalStateException(expectedProfile + " must be selected for " + mode);
            frame.setVisible(true);
        });

        Thread.sleep(300);
        if (mode.equals("dropdown"))
            SwingUtilities.invokeAndWait(state.dropdown::showPopup);
        Thread.sleep(300);
        Toolkit.getDefaultToolkit().sync();
        Rectangle bounds = captureBounds(state.frame);
        BufferedImage image = new Robot().createScreenCapture(bounds);
        ImageIO.write(image, "png", output.toFile());

        SwingUtilities.invokeAndWait(() -> {
            state.dropdown.hidePopup();
            state.frame.dispose();
        });
    }

    private static Rectangle captureBounds(JFrame frame) {
        Rectangle bounds = frame.getBounds();
        for (Window window : Window.getWindows()) {
            if (window.isVisible() && window != frame)
                bounds = bounds.union(window.getBounds());
        }
        return bounds;
    }

    private static JComboBox<?> findComboBox(Component component) {
        if (component instanceof JComboBox<?> comboBox)
            return comboBox;
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                JComboBox<?> result = findComboBox(child);
                if (result != null)
                    return result;
            }
        }
        return null;
    }

    private static JButton findButton(Component component, String text) {
        if (component instanceof JButton button && text.equals(button.getText()))
            return button;
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                JButton result = findButton(child, text);
                if (result != null)
                    return result;
            }
        }
        return null;
    }

    private static List<JSpinner> findSpinners(Component component) {
        List<JSpinner> spinners = new ArrayList<>();
        if (component instanceof JSpinner spinner)
            spinners.add(spinner);
        if (component instanceof Container container) {
            for (Component child : container.getComponents())
                spinners.addAll(findSpinners(child));
        }
        return spinners;
    }

    private static final class CaptureState
    {
        JFrame frame;
        JComboBox<?> dropdown;
        JSpinner slopeSpinner;
    }
}
