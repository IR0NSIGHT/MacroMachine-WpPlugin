package org.ironsight.wpplugin.macromachine.threeDRendering;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.ironsight.cubearray.render.CubeSetup;
import org.ironsight.cubearray.render.InstancedCubes;
import org.ironsight.cubearray.schematic.SchemReader;
import org.pepsoft.worldpainter.objects.WPObject;

/**
 * Singleton that manages a single Cubearray InstancedCubes 3D renderer window.
 * <p>
 * Converts WPObject instances to CubeSetup via SchemReader.prepareData() and
 * renders them in a standalone GLFW window.
 */
public class CubeArrayRenderer
{

    private static final CubeArrayRenderer INSTANCE = new CubeArrayRenderer();

    private volatile InstancedCubes renderer;
    private volatile Thread renderThread;
    private final AtomicBoolean rendering = new AtomicBoolean(false);
    private volatile CubeSetup lastCubeSetup;

    private CubeArrayRenderer() {
    }

    public static CubeArrayRenderer getInstance() {
        return INSTANCE;
    }

    /**
     * Render a WPObject in the 3D viewer window.
     * <p>
     * If a window is already open, closes it and opens a new one with the new data.
     * If no window is open, creates a new one.
     *
     * @param wpObject the WorldPainter object to render
     */
    public void render(WPObject wpObject) {
        if (wpObject == null) {
            return;
        }

        // Close existing window if open
        if (rendering.get() && renderThread != null) {
            renderThread.interrupt();
            try {
                renderThread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            rendering.set(false);
            renderer = null;
            renderThread = null;
        }

        try {
            CubeSetup cubeSetup = SchemReader.prepareData(List.of(wpObject));
            lastCubeSetup = cubeSetup;
            startRenderThread(cubeSetup);
        } catch (Exception e) {
            throw new RuntimeException("Failed to prepare Cubearray render data", e);
        }
    }

    /**
     * Re-render the last rendered WPObject (if any).
     * <p>
     * Useful when the user closes the window and wants to see the same data again.
     */
    public void reRender() {
        if (lastCubeSetup != null) {
            if (rendering.get() && renderThread != null) {
                renderThread.interrupt();
                try {
                    renderThread.join(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                rendering.set(false);
                renderer = null;
                renderThread = null;
            }
            startRenderThread(lastCubeSetup);
        }
    }

    /**
     * Returns true if a render window is currently open.
     */
    public boolean isRendering() {
        return rendering.get() && renderThread != null && renderThread.isAlive();
    }

    private void startRenderThread(CubeSetup cubeSetup) {
        renderer = new InstancedCubes(cubeSetup);
        rendering.set(true);

        renderThread = new Thread(() -> {
            try {
                renderer.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                System.err.println("[CubeArrayRenderer] Render thread error: " + e.getMessage());
                e.printStackTrace();
            } finally {
                rendering.set(false);
                renderer = null;
            }
        }, "CubeArrayRenderer");

        renderThread.setDaemon(true);
        renderThread.start();
    }
}
