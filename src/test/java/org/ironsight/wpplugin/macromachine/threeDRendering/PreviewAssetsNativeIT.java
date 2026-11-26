package org.ironsight.wpplugin.macromachine.threeDRendering;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;
import org.ironsight.cubearray.render.NativePreviewProbe;
import org.ironsight.cubearray.platform.ResourceUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(60)
class PreviewAssetsNativeIT
{
    @Test
    void realGrassAndFlowerSceneOpensWithoutRepeatingItsWarmUpUpload() throws Exception {
        var probe = new NativePreviewProbe();
        var service = probe.service();
        var renderer = new CubeArrayRenderer(CubeArrayRenderer::prepareObject, service::warmUp, service::show,
                service::showOrReplace, service::shutdownAsync);
        var startup = new PreviewWarmUp(renderer::warmUp);
        try {
            SwingUtilities.invokeAndWait(startup::start);
            startup.start().get(30, TimeUnit.SECONDS);
            var hidden = PreviewNativeIT.snapshot(probe);
            assertEquals(org.lwjgl.glfw.GLFW.GLFW_FALSE, hidden.visible());
            assertEquals(1, hidden.uploads());
            assertEquals("3D Preview", renderer.welcomeSetup.name);
            // Grass has base/overlay elements; the flower has two crossed planes.
            assertEquals(4, renderer.welcomeSetup.positions.length);
            renderer.openPreview().get(10, TimeUnit.SECONDS);
            var visible = PreviewNativeIT.await(probe, state -> state.visible() == org.lwjgl.glfw.GLFW.GLFW_TRUE);
            assertEquals(hidden.window(), visible.window());
            assertSame(hidden.capabilities(), visible.capabilities());
            assertEquals(1, visible.uploads());
            service.hide().get(5, TimeUnit.SECONDS);
            var image = service.renderScreenshot(renderer.welcomeSetup, 128, 128, null).get(10, TimeUnit.SECONDS);
            var pixels = image.getRGB(0, 0, 128, 128, null, 0, 128);
            assertTrue(Arrays.stream(pixels).distinct().count() > 4, "Expected textured scene pixels, not sky only");
        } finally {
            renderer.shutdownAsync().get(10, TimeUnit.SECONDS);
        }
    }

    @Test
    void bundledAssetsPrepareARealTexturedWarmUpObject() {
        Path isolatedRoot = Path.of(System.getenv("LOCALAPPDATA")).toAbsolutePath().normalize();
        assertTrue(isolatedRoot.endsWith("native-preview-data"), "Run through the native-preview-tests profile");
        var setup = CubeArrayRenderer.prepareObject(RenderFixtures.object("stone"));
        assertEquals("stone", setup.name);
        assertEquals(1, setup.positions.length);
        assertEquals(1, setup.colorIndices.length);
        assertTrue(setup.textureAtlas.getWidth() > 1);
        Path install = ResourceUtils.getInstallPath().toAbsolutePath().normalize();
        assertTrue(install.startsWith(isolatedRoot));
        assertTrue(Files.isDirectory(install.resolve(ResourceUtils.TEXTURE_PACK_ROOT)));
    }
}
