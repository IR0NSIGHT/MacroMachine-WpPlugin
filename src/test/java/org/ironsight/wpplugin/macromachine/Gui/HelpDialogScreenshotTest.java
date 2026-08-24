package org.ironsight.wpplugin.macromachine.Gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class HelpDialogScreenshotTest
{

    @Test
    void generatesScreenshotsInTempDir(@TempDir Path tempDir) throws Exception {
        int exitCode = HelpDialogScreenshot.generateScreenshots(tempDir);
        assertEquals(0, exitCode, "generateScreenshots should return 0 on success");

        for (HelpCatalog catalog : HelpCatalog.values()) {
            Path file = tempDir.resolve(catalog.getFileName());
            assertTrue(Files.exists(file), "Screenshot not generated for " + catalog);
            var img = ImageIO.read(file.toFile());
            assertNotNull(img, "Image is null for " + catalog);
            assertTrue(img.getWidth() > 300 && img.getHeight() > 200,
                    "Image too small for " + catalog + ": " + img.getWidth() + "x" + img.getHeight());
        }
    }

    @Test
    void generatesScreenshotsToTargetHelpScreenshots(@TempDir Path tempDir) throws Exception {
        Path targetDir = Path.of("target/help-screenshots");
        int exitCode = HelpDialogScreenshot.generateScreenshots(targetDir);
        assertEquals(0, exitCode);

        for (HelpCatalog catalog : HelpCatalog.values()) {
            Path file = targetDir.resolve(catalog.getFileName());
            assertTrue(Files.exists(file), "Missing target file " + file);
            var img = ImageIO.read(file.toFile());
            assertNotNull(img);
        }

        // also verify generation works for an isolated temp dir without relying on
        // target
        Path secondRun = tempDir.resolve("second");
        Files.createDirectories(secondRun);
        int secondExit = HelpDialogScreenshot.generateScreenshots(secondRun);
        assertEquals(0, secondExit);
        for (HelpCatalog catalog : HelpCatalog.values()) {
            assertTrue(Files.exists(secondRun.resolve(catalog.getFileName())));
        }
    }
}
