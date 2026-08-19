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
import javax.imageio.ImageIO;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public final class PathToolScreenshot
{
    private PathToolScreenshot() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2 || (!args[1].equals("default") && !args[1].equals("dropdown"))) {
            throw new IllegalArgumentException("Usage: PathToolScreenshot <output.png> <default|dropdown>");
        }
        if (GraphicsEnvironment.isHeadless())
            throw new IllegalStateException("A display is required to capture the Swing window");

        Path output = Path.of(args[0]);
        Files.createDirectories(output.toAbsolutePath().getParent());
        capture(output, args[1].equals("dropdown"));
    }

    private static void capture(Path output, boolean openDropdown) throws Exception {
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
            if (state.dropdown.getItemCount() != 6)
                throw new IllegalStateException("Expected six transition profiles");
            if (!"Sinus".equals(String.valueOf(state.dropdown.getSelectedItem())))
                throw new IllegalStateException("Sinus must be the default transition profile");
            frame.setVisible(true);
        });

        Thread.sleep(300);
        if (openDropdown)
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

    private static final class CaptureState
    {
        JFrame frame;
        JComboBox<?> dropdown;
    }
}
