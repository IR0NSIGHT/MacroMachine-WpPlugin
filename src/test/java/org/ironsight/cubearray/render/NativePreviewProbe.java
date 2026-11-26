package org.ironsight.cubearray.render;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;

// Test-only access to CubeArray's owner-thread seam; no reflection or native calls on the EDT.
public class NativePreviewProbe
{
    final AtomicInteger creations = new AtomicInteger();
    final RendererService service = new RendererService(this::createRenderer);
    TrackingRenderer renderer;

    InstancedCubes createRenderer(CubeSetup setup) {
        creations.incrementAndGet();
        renderer = new TrackingRenderer(setup);
        return renderer;
    }

    public RendererService service() {
        return service;
    }

    public CompletableFuture<Snapshot> snapshot() {
        var result = new CompletableFuture<Snapshot>();
        renderer.enqueueSharedCommand(() -> capture(result));
        return result;
    }

    void capture(CompletableFuture<Snapshot> result) {
        try {
            long window = glfwGetCurrentContext();
            result.complete(new Snapshot(window, glfwGetWindowAttrib(window, GLFW_VISIBLE), GL.getCapabilities(),
                    glGetError(), renderer.instanceUploads, creations.get(), renderer.getCameraState()));
        } catch (Throwable error) {
            result.completeExceptionally(error);
        }
    }

    public void setCamera() {
        renderer.setCamera(20, 35, 12);
    }

    public void closeWindow() {
        renderer.enqueueSharedCommand(NativePreviewProbe::closeCurrentWindow);
    }

    static void closeCurrentWindow() {
        glfwSetWindowShouldClose(glfwGetCurrentContext(), true);
    }

    public boolean ownerAlive() {
        return renderer.owner.isAlive();
    }

    public long contextAfterCleanup() {
        return renderer.contextAfterCleanup;
    }

    public record Snapshot(long window, int visible, GLCapabilities capabilities, int glError, int uploads,
            int creations, InstancedCubes.CameraState camera) {
    }

    static class TrackingRenderer extends InstancedCubes
    {
        int instanceUploads;
        long contextAfterCleanup = -1;
        Thread owner;

        TrackingRenderer(CubeSetup setup) {
            super(setup);
        }

        @Override
        void runSharedService() throws Exception {
            owner = Thread.currentThread();
            try {
                super.runSharedService();
            } finally {
                contextAfterCleanup = glfwGetCurrentContext();
            }
        }

        @Override
        void uploadInstanceData() {
            super.uploadInstanceData();
            instanceUploads++;
        }
    }
}
