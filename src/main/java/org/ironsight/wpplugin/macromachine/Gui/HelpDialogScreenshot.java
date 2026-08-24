package org.ironsight.wpplugin.macromachine.Gui;

import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Automation to show each tool help dialog in isolation and screenshot it to
 * target/help-screenshots. Only tool helps (HelpCatalog) are captured. Used by
 * mvn verify via exec-maven-plugin.
 */
public final class HelpDialogScreenshot
{
    private static final int FALLBACK_WIDTH = 620;
    private static final int FALLBACK_HEIGHT = 500;
    private static final int HEADLESS_EDT_LAYOUT_DELAY_MS = 350;
    private static final int DISPOSE_TIMEOUT_SECONDS = 2;

    private HelpDialogScreenshot() {
    }

    public static void main(String[] args) throws Exception {
        String outputDirArg = args.length > 0 ? args[0] : "target/help-screenshots";
        Path outputDir = Path.of(outputDirArg);
        int exitCode = generateScreenshots(outputDir);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    /**
     * Generates one PNG per {@link HelpCatalog} entry. Returns 0 on success, 1 on
     * any failure. Does not call {@code System.exit} — suitable for tests and
     * exec-maven-plugin which expects a non-zero exit to fail the build via the
     * caller.
     */
    public static int generateScreenshots(Path outputDir) throws IOException, InterruptedException {
        Files.createDirectories(outputDir);
        initLookAndFeel();
        logHeadlessIfNeeded();

        boolean anyFailure = false;
        for (HelpCatalog catalog : HelpCatalog.values()) {
            Path output = outputDir.resolve(catalog.getFileName());
            try {
                captureOne(catalog, output);
                System.out.println("[HelpDialogScreenshot] Wrote " + output + " (" + catalog.getTitle() + ")");
            } catch (Exception e) {
                anyFailure = true;
                System.err.println("[HelpDialogScreenshot] FAILED for " + catalog + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        if (anyFailure) {
            System.err.println("[HelpDialogScreenshot] One or more screenshots failed");
            return 1;
        }
        System.out.println("[HelpDialogScreenshot] All screenshots written to " + outputDir.toAbsolutePath());
        return 0;
    }

    private static void initLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.out.println("[HelpDialogScreenshot] Could not set system L&F: " + e.getMessage());
        }
    }

    private static void logHeadlessIfNeeded() {
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println(
                    "[HelpDialogScreenshot] Headless environment detected - attempting off-screen paint fallback");
        }
    }

    private static void captureOne(HelpCatalog catalog, Path output) throws Exception {
        Files.createDirectories(output.toAbsolutePath().getParent());
        if (GraphicsEnvironment.isHeadless()) {
            captureHeadless(catalog, output);
        } else {
            captureHeaded(catalog, output);
        }
    }

    private static void captureHeadless(HelpCatalog catalog, Path output) throws Exception {
        HelpDialog dialog = createPackedDialog(catalog);
        try {
            ensureNonZeroSize(dialog);
            BufferedImage image = renderOffscreen(dialog);
            writePng(image, output);
        } finally {
            disposeOnEdtAndWait(dialog);
        }
    }

    private static void captureHeaded(HelpCatalog catalog, Path output) throws Exception {
        HelpDialog dialog = createVisibleDialog(catalog);
        try {
            Thread.sleep(HEADLESS_EDT_LAYOUT_DELAY_MS);
            Toolkit.getDefaultToolkit().sync();

            if (!dialog.isDisplayable()) {
                throw new IllegalStateException("Dialog not displayable for " + catalog);
            }
            ensureNonZeroSize(dialog);

            BufferedImage image = captureWithRobotOrPaint(dialog, catalog);
            writePng(image, output);
        } finally {
            disposeOnEdtAsyncAndAwait(dialog);
            Thread.sleep(100);
        }
    }

    private static HelpDialog createPackedDialog(HelpCatalog catalog) throws Exception {
        AtomicReference<HelpDialog> ref = new AtomicReference<>();
        AtomicReference<Exception> error = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                HelpDialog dialog = catalog.createDialog(null);
                dialog.setModal(false);
                dialog.pack();
                ref.set(dialog);
            } catch (Exception e) {
                error.set(e);
            }
        });
        if (error.get() != null) {
            throw new RuntimeException("Cannot create dialog for " + catalog, error.get());
        }
        HelpDialog dialog = ref.get();
        if (dialog == null) {
            throw new IllegalStateException("Dialog not created for " + catalog);
        }
        return dialog;
    }

    private static HelpDialog createVisibleDialog(HelpCatalog catalog) throws Exception {
        AtomicReference<HelpDialog> ref = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            HelpDialog dialog = catalog.createDialog(null);
            dialog.setModal(false);
            dialog.pack();
            dialog.setLocation(100, 100);
            dialog.setVisible(true);
            ref.set(dialog);
        });
        HelpDialog dialog = ref.get();
        if (dialog == null) {
            throw new IllegalStateException("Dialog not created for " + catalog);
        }
        return dialog;
    }

    private static void ensureNonZeroSize(HelpDialog dialog) {
        int w = dialog.getWidth();
        int h = dialog.getHeight();
        if (w <= 0 || h <= 0) {
            dialog.setSize(FALLBACK_WIDTH, FALLBACK_HEIGHT);
        }
    }

    private static BufferedImage renderOffscreen(HelpDialog dialog) {
        int w = dialog.getWidth();
        int h = dialog.getHeight();
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        try {
            dialog.paint(g2);
        } finally {
            g2.dispose();
        }
        return image;
    }

    private static BufferedImage captureWithRobotOrPaint(HelpDialog dialog, HelpCatalog catalog) {
        try {
            java.awt.Robot robot = new java.awt.Robot();
            java.awt.Point loc = dialog.getLocationOnScreen();
            java.awt.Rectangle bounds = new java.awt.Rectangle(loc.x, loc.y, dialog.getWidth(), dialog.getHeight());
            return robot.createScreenCapture(bounds);
        } catch (Exception e) {
            System.out.println("[HelpDialogScreenshot] Robot capture failed for " + catalog
                    + ", falling back to paint: " + e.getMessage());
            return renderOffscreen(dialog);
        }
    }

    private static void writePng(BufferedImage image, Path output) throws IOException {
        ImageIO.write(image, "png", output.toFile());
    }

    private static void disposeOnEdtAndWait(HelpDialog dialog) throws InterruptedException {
        try {
            SwingUtilities.invokeAndWait(dialog::dispose);
        } catch (Exception ignored) {
        }
    }

    private static void disposeOnEdtAsyncAndAwait(HelpDialog dialog) throws InterruptedException {
        CountDownLatch disposed = new CountDownLatch(1);
        SwingUtilities.invokeLater(() -> {
            dialog.dispose();
            disposed.countDown();
        });
        disposed.await(DISPOSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }
}
