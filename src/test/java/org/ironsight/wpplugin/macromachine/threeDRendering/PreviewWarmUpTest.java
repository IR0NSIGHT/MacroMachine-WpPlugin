package org.ironsight.wpplugin.macromachine.threeDRendering;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(10)
class PreviewWarmUpTest
{
    @Test
    void startsImmediatelyOffEdtWithoutBlockingGuiAndRunsOnce() throws Exception {
        var calls = new AtomicInteger();
        var background = new CompletableFuture<Thread>();
        var allowed = new CompletableFuture<Void>();
        var ready = new CompletableFuture<Void>();
        var warmUp = new PreviewWarmUp(() -> {
            calls.incrementAndGet();
            assertFalse(SwingUtilities.isEventDispatchThread());
            background.complete(Thread.currentThread());
            allowed.join();
            return ready;
        });
        try {
            SwingUtilities.invokeAndWait(warmUp::start);
            var worker = background.get(2, TimeUnit.SECONDS);
            assertEquals("3D-preview-startup", worker.getName());
            assertTrue(worker.isDaemon());
            assertEquals(Thread.MIN_PRIORITY, worker.getPriority());
            // Initialization is still blocked; another EDT turn must remain possible.
            SwingUtilities.invokeAndWait(warmUp::start);
            assertEquals(1, calls.get());
            assertFalse(warmUp.completion.isDone());
            allowed.complete(null);
            // Starting initialization is not the same as the renderer becoming ready.
            assertFalse(warmUp.completion.isDone());
            ready.complete(null);
            warmUp.start().get(2, TimeUnit.SECONDS);
            assertEquals(1, calls.get());
        } finally {
            allowed.complete(null);
            ready.complete(null);
        }
    }

    @Test
    void synchronousInitializationFailureIsReportedWithoutEscapingToGui() throws Exception {
        var failure = new IllegalStateException("cannot initialize renderer");
        var warmUp = new PreviewWarmUp(() -> {
            throw failure;
        });
        SwingUtilities.invokeAndWait(warmUp::start);
        var error = assertThrows(ExecutionException.class, () -> warmUp.start().get(2, TimeUnit.SECONDS));
        assertSame(failure, error.getCause());
    }

    @Test
    void asynchronousWarmUpFailureIsReportedAndDoesNotStartAnotherAttempt() throws Exception {
        var calls = new AtomicInteger();
        var ready = new CompletableFuture<Void>();
        var warmUp = new PreviewWarmUp(() -> {
            calls.incrementAndGet();
            return ready;
        });
        SwingUtilities.invokeAndWait(warmUp::start);
        var failure = new IllegalArgumentException("native initialization failed");
        ready.completeExceptionally(failure);
        var error = assertThrows(ExecutionException.class, () -> warmUp.start().get(2, TimeUnit.SECONDS));
        assertSame(failure, error.getCause());
        assertEquals(1, calls.get());
    }
}
