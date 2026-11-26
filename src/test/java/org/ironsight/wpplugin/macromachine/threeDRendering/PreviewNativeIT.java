package org.ironsight.wpplugin.macromachine.threeDRendering;

import static org.ironsight.wpplugin.macromachine.threeDRendering.CubeArrayRenderer.RenderOutcome.SUBMITTED;
import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.opengl.GL33.GL_NO_ERROR;

import java.awt.GraphicsEnvironment;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import org.ironsight.cubearray.render.CubeSetup;
import org.ironsight.cubearray.render.NativePreviewProbe;
import org.ironsight.cubearray.render.NativePreviewProbe.Snapshot;
import org.ironsight.wpplugin.macromachine.Gui.MacroMachineWindow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.pepsoft.worldpainter.objects.WPObject;

@Timeout(60)
class PreviewNativeIT
{
    @Test
    @EnabledIfSystemProperty(named = "preview.initialCameraRegression", matches = "true", disabledReason = "Known CubeArray v1.7.9 transition bug; opt in to reproduce")
    void firstSceneUsesRealBoundsAfterHiddenWarmUp() throws Exception {
        var probe = new NativePreviewProbe();
        var service = probe.service();
        var renderer = new CubeArrayRenderer(PreviewNativeIT::differentSizedSetup, service::warmUp, service::show,
                service::showOrReplace, service::shutdownAsync);
        try {
            renderer.warmUp().get(15, TimeUnit.SECONDS);
            assertEquals(GLFW_FALSE, snapshot(probe).visible());
            renderer.render(RenderFixtures.object("large terrain")).get(10, TimeUnit.SECONDS);
            Snapshot first = await(probe, state -> state.visible() == GLFW_TRUE && state.uploads() == 2
                    && Math.abs(state.camera().yaw() - Math.toRadians(45)) < 0.000001);
            // Correct framing is twice the real scene's largest dimension, not the
            // warm-up's.
            assertEquals(200f, first.camera().radius());
        } finally {
            renderer.shutdownAsync().get(10, TimeUnit.SECONDS);
        }
    }

    static CubeSetup differentSizedSetup(WPObject object) {
        var setup = RenderFixtures.setup(object.getName());
        if (object.getName().equals("large terrain")) {
            setup.min.set(-50);
            setup.max.set(50);
        }
        return setup;
    }

    @Test
    void warmRenderReplaceCloseAndReopenReuseTheHiddenContext() throws Exception {
        assertTrue(System.getProperty("os.name").startsWith("Windows"), "Windows natives are bundled");
        assertFalse(GraphicsEnvironment.isHeadless(), "An interactive OpenGL desktop is required");
        var probe = new NativePreviewProbe();
        var service = probe.service();
        var renderer = new CubeArrayRenderer(PreviewNativeIT::differentSizedSetup, service::warmUp, service::show,
                service::showOrReplace, service::shutdownAsync);
        long start = System.nanoTime();
        try {
            renderer.warmUp().get(15, TimeUnit.SECONDS);
            Snapshot warm = snapshot(probe);
            assertNotEquals(0L, warm.window());
            assertEquals(GLFW_FALSE, warm.visible());
            assertEquals(1, warm.uploads());
            assertEquals(GL_NO_ERROR, warm.glError());
            assertNull(MacroMachineWindow.getDialog());
            long warmed = System.nanoTime();

            assertEquals(SUBMITTED, renderer.openPreview().get(10, TimeUnit.SECONDS));
            Snapshot visible = await(probe, state -> state.visible() == GLFW_TRUE);
            assertEquals(1, visible.uploads());
            assertEquals(warm.window(), visible.window());
            assertSame(warm.capabilities(), visible.capabilities());
            assertEquals(1, visible.creations());
            assertNull(MacroMachineWindow.getDialog());
            long shown = System.nanoTime();

            // CubeArray's introductory transition overrides camera changes during its first
            // second.
            await(probe, state -> Math.abs(state.camera().yaw() - Math.toRadians(45)) < 0.000001);
            assertEquals(SUBMITTED, renderer.render(RenderFixtures.object("large terrain")).get(10, TimeUnit.SECONDS));
            Snapshot terrain = await(probe,
                    state -> state.uploads() == 2 && Math.abs(state.camera().radius() - 200) < 0.001);
            assertEquals(warm.window(), terrain.window());
            probe.setCamera();
            Snapshot moved = await(probe, state -> Math.abs(state.camera().radius() - 12) < 0.001);
            assertEquals(SUBMITTED, renderer.render(RenderFixtures.object("second")).get(10, TimeUnit.SECONDS));
            Snapshot replaced = await(probe, state -> state.uploads() == 3);
            assertEquals(moved.camera(), replaced.camera());

            probe.closeWindow();
            Snapshot closed = await(probe, state -> state.visible() == GLFW_FALSE);
            assertEquals(warm.window(), closed.window());
            assertEquals(SUBMITTED, renderer.openPreview().get(10, TimeUnit.SECONDS));
            Snapshot reopened = await(probe, state -> state.visible() == GLFW_TRUE);
            assertEquals(warm.window(), reopened.window());
            assertSame(warm.capabilities(), reopened.capabilities());
            assertEquals(1, reopened.creations());
            assertEquals(GL_NO_ERROR, reopened.glError());

            service.hide().get(5, TimeUnit.SECONDS);
            var image = service.renderScreenshot(RenderFixtures.setup("pixels"), 64, 48, null)
                    .get(10, TimeUnit.SECONDS);
            assertEquals(64, image.getWidth());
            assertEquals(48, image.getHeight());
            assertNotEquals(image.getRGB(0, 0), image.getRGB(32, 24));
            System.out.printf("3D preview warm-up: %.1f ms; warm request to visible uploaded scene: %.1f ms%n",
                    (warmed - start) / 1_000_000.0, (shown - warmed) / 1_000_000.0);
        } finally {
            renderer.shutdownAsync().get(10, TimeUnit.SECONDS);
        }
        assertFalse(probe.ownerAlive());
        assertEquals(0L, probe.contextAfterCleanup());
    }

    static Snapshot snapshot(NativePreviewProbe probe) throws Exception {
        return probe.snapshot().get(5, TimeUnit.SECONDS);
    }

    static Snapshot await(NativePreviewProbe probe, Predicate<Snapshot> condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        Snapshot state;
        do {
            state = snapshot(probe);
            if (condition.test(state))
                return state;
        } while (System.nanoTime() < deadline);
        throw new AssertionError("Native renderer state did not converge: " + state);
    }
}
