package org.ironsight.wpplugin.macromachine.threeDRendering;

import static org.ironsight.wpplugin.macromachine.threeDRendering.CubeArrayRenderer.RenderOutcome.SUBMITTED;
import static org.ironsight.wpplugin.macromachine.threeDRendering.CubeArrayRenderer.RenderOutcome.SUPERSEDED;
import static org.ironsight.wpplugin.macromachine.threeDRendering.RenderFixtures.object;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.ironsight.cubearray.render.CubeSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.pepsoft.worldpainter.objects.WPObject;

@Timeout(10)
class CubeArrayRendererTest
{
    final List<String> prepared = new CopyOnWriteArrayList<>();
    final List<String> shown = new CopyOnWriteArrayList<>();
    final List<String> cameraResets = new CopyOnWriteArrayList<>();
    final List<CubeSetup> shownSetups = new CopyOnWriteArrayList<>();
    final CountDownLatch warming = new CountDownLatch(1);
    final CompletableFuture<Void> warmAllowed = new CompletableFuture<>();
    final CompletableFuture<Void> stopped = new CompletableFuture<>();
    CubeArrayRenderer renderer = new CubeArrayRenderer(this::prepare, this::warm, this::resetAndShow, this::show,
            this::stop);
    CubeSetup warmedSetup;

    CubeSetup prepare(WPObject object) {
        prepared.add(object.getName());
        return RenderFixtures.setup(object.getName());
    }

    CompletableFuture<Void> warm(CubeSetup setup) {
        warmedSetup = setup;
        warming.countDown();
        return warmAllowed;
    }

    CompletableFuture<Void> show(CubeSetup setup) {
        shown.add(setup.name);
        shownSetups.add(setup);
        return CompletableFuture.completedFuture(null);
    }

    CompletableFuture<Void> resetAndShow(CubeSetup setup) {
        cameraResets.add(setup.name);
        return show(setup);
    }

    CompletableFuture<Void> stop() {
        stopped.complete(null);
        return stopped;
    }

    @AfterEach
    void cleanup() throws Exception {
        renderer.shutdownAsync().get(3, TimeUnit.SECONDS);
        if (renderer.worker != null) {
            renderer.worker.join(3000);
            assertFalse(renderer.worker.isAlive());
        }
    }

    @Test
    void warmUpIsNonblockingIdempotentAndNeverShowsWindow() throws Exception {
        var first = renderer.warmUp();
        assertTrue(warming.await(2, TimeUnit.SECONDS));
        var observers = java.util.stream.IntStream.range(0, 20)
                .mapToObj(index -> CompletableFuture.supplyAsync(renderer::warmUp).thenCompose(Function.identity()))
                .toList();
        assertFalse(first.isDone());
        assertEquals(List.of(), shown);
        first.cancel(false);
        warmAllowed.complete(null);
        CompletableFuture.allOf(observers.toArray(CompletableFuture[]::new)).get(3, TimeUnit.SECONDS);
        assertEquals(List.of("3D Preview"), prepared);
        assertEquals(List.of(), shown);
    }

    @Test
    void onlyNewestRequestSurvivesWarmUp() throws Exception {
        var a = renderer.render(object("A"));
        assertTrue(warming.await(2, TimeUnit.SECONDS));
        var b = renderer.render(object("B"));
        var c = renderer.render(object("C"));
        assertEquals(SUPERSEDED, a.get(2, TimeUnit.SECONDS));
        assertEquals(SUPERSEDED, b.get(2, TimeUnit.SECONDS));
        warmAllowed.complete(null);
        assertEquals(SUBMITTED, c.get(3, TimeUnit.SECONDS));
        assertEquals(List.of("3D Preview", "C"), prepared);
        assertEquals(List.of("C"), shown);
    }

    @Test
    void staleExportSkipsConversionAndIntermediateRequestNeverRuns() throws Exception {
        var exportEntered = new CountDownLatch(1);
        var exportAllowed = new CompletableFuture<Void>();
        warmAllowed.complete(null);
        var a = renderer.render(() -> blockedObject(exportEntered, exportAllowed, "A"));
        assertTrue(exportEntered.await(2, TimeUnit.SECONDS));
        var b = renderer.render(() -> fail("Superseded supplier must never run"));
        var c = renderer.render(object("C"));
        exportAllowed.complete(null);
        assertEquals(SUPERSEDED, a.get(3, TimeUnit.SECONDS));
        assertEquals(SUPERSEDED, b.get(3, TimeUnit.SECONDS));
        assertEquals(SUBMITTED, c.get(3, TimeUnit.SECONDS));
        assertEquals(List.of("3D Preview", "C"), prepared);
        assertEquals(List.of("C"), shown);
    }

    @Test
    void concurrentRequestsDuringWarmUpStillHaveOnlyOneSurvivor() throws Exception {
        renderer.warmUp();
        assertTrue(warming.await(2, TimeUnit.SECONDS));
        var callers = java.util.stream.IntStream.range(0, 100)
                .mapToObj(index -> CompletableFuture.supplyAsync(() -> renderer.render(object("request-" + index))))
                .toList();
        CompletableFuture.allOf(callers.toArray(CompletableFuture[]::new)).get(3, TimeUnit.SECONDS);
        var newest = renderer.render(object("newest"));
        for (var caller : callers)
            assertEquals(SUPERSEDED, caller.get().get(2, TimeUnit.SECONDS));
        warmAllowed.complete(null);
        assertEquals(SUBMITTED, newest.get(3, TimeUnit.SECONDS));
        assertEquals(List.of("3D Preview", "newest"), prepared);
        assertEquals(List.of("newest"), shown);
    }

    @Test
    void staleConversionNeverReachesRenderer() throws Exception {
        var conversionEntered = new CountDownLatch(1);
        var conversionAllowed = new CompletableFuture<Void>();
        renderer = new CubeArrayRenderer(object -> convertBlocked(object, conversionEntered, conversionAllowed),
                this::warm, this::resetAndShow, this::show, this::stop);
        warmAllowed.complete(null);
        var a = renderer.render(object("A"));
        assertTrue(conversionEntered.await(2, TimeUnit.SECONDS));
        var b = renderer.render(object("B"));
        conversionAllowed.complete(null);
        assertEquals(SUPERSEDED, a.get(3, TimeUnit.SECONDS));
        assertEquals(SUBMITTED, b.get(3, TimeUnit.SECONDS));
        assertEquals(List.of("B"), shown);
    }

    CubeSetup convertBlocked(WPObject object, CountDownLatch entered, CompletableFuture<Void> allowed) {
        if (object.getName().equals("A"))
            blockedObject(entered, allowed, "A");
        return prepare(object);
    }

    @Test
    void rendererBackpressureRetainsOnlyOnePendingRequest() throws Exception {
        var showEntered = new CountDownLatch(1);
        var showAllowed = new CompletableFuture<Void>();
        renderer = new CubeArrayRenderer(this::prepare, this::warm,
                setup -> blockedShow(setup, showEntered, showAllowed),
                setup -> blockedShow(setup, showEntered, showAllowed), this::stop);
        warmAllowed.complete(null);
        var first = renderer.render(object("first"));
        assertTrue(showEntered.await(2, TimeUnit.SECONDS));
        var replaced = new java.util.ArrayList<CompletableFuture<CubeArrayRenderer.RenderOutcome>>();
        for (int index = 0; index < 100; index++)
            replaced.add(renderer.render(object("request-" + index)));
        for (int index = 0; index < 99; index++)
            assertEquals(SUPERSEDED, replaced.get(index).get(2, TimeUnit.SECONDS));
        assertEquals(List.of("first"), shown);
        assertEquals(List.of("3D Preview", "first"), prepared);
        showAllowed.complete(null);
        assertEquals(SUBMITTED, first.get(3, TimeUnit.SECONDS));
        assertEquals(SUBMITTED, replaced.get(99).get(3, TimeUnit.SECONDS));
        assertEquals(List.of("first", "request-99"), shown);
        assertEquals(101, renderer.latestId);
    }

    CompletableFuture<Void> blockedShow(CubeSetup setup, CountDownLatch entered, CompletableFuture<Void> allowed) {
        shown.add(setup.name);
        entered.countDown();
        return allowed;
    }

    @Test
    void requestFailureDoesNotPreventLaterRender() throws Exception {
        warmAllowed.complete(null);
        var failure = new IllegalArgumentException("invalid selection");
        var bad = renderer.render(() -> {
            throw failure;
        });
        assertSame(failure, assertThrows(ExecutionException.class, () -> bad.get(3, TimeUnit.SECONDS)).getCause());
        assertEquals(SUBMITTED, renderer.render(object("valid")).get(3, TimeUnit.SECONDS));
        assertEquals(List.of("valid"), shown);
    }

    @Test
    void initializationFailureCompletesPendingAndRejectsLaterWork() throws Exception {
        var ready = renderer.warmUp();
        var pending = renderer.render(object("pending"));
        var failure = new IllegalStateException("GL unavailable");
        warmAllowed.completeExceptionally(failure);
        assertSame(failure, assertThrows(ExecutionException.class, () -> ready.get(3, TimeUnit.SECONDS)).getCause());
        assertSame(failure, assertThrows(ExecutionException.class, () -> pending.get(3, TimeUnit.SECONDS)).getCause());
        var later = renderer.render(object("later"));
        assertSame(failure, assertThrows(ExecutionException.class, () -> later.get(3, TimeUnit.SECONDS)).getCause());
        assertEquals(List.of(), shown);
    }

    @Test
    void shutdownDuringWarmUpRejectsPendingAndNeverShows() throws Exception {
        var pending = renderer.render(object("pending"));
        assertTrue(warming.await(2, TimeUnit.SECONDS));
        renderer.shutdownAsync().get(3, TimeUnit.SECONDS);
        assertTrue(pending.isCompletedExceptionally());
        assertTrue(renderer.render(object("later")).isCompletedExceptionally());
        assertEquals(List.of(), shown);
    }

    @Test
    void shutdownBeforeFirstUseIsTerminalAndDoesNotStartWorker() throws Exception {
        renderer.shutdownAsync().get(2, TimeUnit.SECONDS);
        assertTrue(renderer.warmUp().isCompletedExceptionally());
        assertTrue(renderer.render(object("later")).isCompletedExceptionally());
        assertNull(renderer.worker);
        assertEquals(List.of(), prepared);
    }

    @Test
    void shutdownDuringExportCompletesActiveAndPending() throws Exception {
        var entered = new CountDownLatch(1);
        var allowed = new CompletableFuture<Void>();
        warmAllowed.complete(null);
        var active = renderer.render(() -> blockedObject(entered, allowed, "active"));
        assertTrue(entered.await(2, TimeUnit.SECONDS));
        var pending = renderer.render(object("pending"));
        renderer.shutdownAsync();
        assertTrue(active.isCompletedExceptionally());
        assertTrue(pending.isCompletedExceptionally());
        allowed.complete(null);
        renderer.shutdownAsync().get(3, TimeUnit.SECONDS);
        assertEquals(List.of(), shown);
    }

    @Test
    void rerenderUsesLastSuccessfulSetupWithoutConvertingAgain() throws Exception {
        warmAllowed.complete(null);
        assertEquals(SUBMITTED, renderer.render(object("scene")).get(3, TimeUnit.SECONDS));
        assertEquals(SUBMITTED, renderer.reRender().get(3, TimeUnit.SECONDS));
        assertEquals(List.of("3D Preview", "scene"), prepared);
        assertEquals(List.of("scene", "scene"), shown);
        assertEquals(List.of("scene"), cameraResets);
    }

    @Test
    void openingDuringWarmUpReusesWelcomeSetupWithoutRememberingItAsTerrain() throws Exception {
        var first = renderer.openPreview();
        assertTrue(warming.await(2, TimeUnit.SECONDS));
        var another = renderer.openPreview();
        assertFalse(first.isDone());
        assertEquals(1, renderer.latestId);
        warmAllowed.complete(null);
        assertEquals(SUBMITTED, first.get(3, TimeUnit.SECONDS));
        assertEquals(SUBMITTED, another.get(3, TimeUnit.SECONDS));
        assertEquals(List.of("3D Preview"), prepared);
        assertEquals(List.of("3D Preview"), shown);
        assertSame(warmedSetup, shownSetups.get(0));
        assertEquals(List.of(), cameraResets);
        assertEquals(SUPERSEDED, renderer.reRender().get(2, TimeUnit.SECONDS));
    }

    @Test
    void terrainSupersedesPendingWelcomeAndRepeatedActivationCannotDisplaceIt() throws Exception {
        var welcome = renderer.openPreview();
        assertTrue(warming.await(2, TimeUnit.SECONDS));
        var terrain = renderer.render(object("terrain"));
        var activation = renderer.openPreview();
        assertEquals(SUPERSEDED, welcome.get(2, TimeUnit.SECONDS));
        assertEquals(2, renderer.latestId);
        warmAllowed.complete(null);
        assertEquals(SUBMITTED, terrain.get(3, TimeUnit.SECONDS));
        assertEquals(SUBMITTED, activation.get(3, TimeUnit.SECONDS));
        assertEquals(List.of("terrain"), shown);
        assertEquals(List.of("terrain"), cameraResets);
    }

    @Test
    void activationDuringExportDoesNotInvalidateTerrainOrQueueWelcome() throws Exception {
        var entered = new CountDownLatch(1);
        var allowed = new CompletableFuture<Void>();
        warmAllowed.complete(null);
        var terrain = renderer.render(() -> blockedObject(entered, allowed, "terrain"));
        assertTrue(entered.await(2, TimeUnit.SECONDS));
        var activation = renderer.openPreview();
        assertEquals(1, renderer.latestId);
        allowed.complete(null);
        assertEquals(SUBMITTED, terrain.get(3, TimeUnit.SECONDS));
        assertEquals(SUBMITTED, activation.get(3, TimeUnit.SECONDS));
        assertEquals(List.of("terrain"), shown);
    }

    @Test
    void firstTerrainAfterWelcomeResetsCameraAndLaterOpeningRetainsTerrain() throws Exception {
        warmAllowed.complete(null);
        assertEquals(SUBMITTED, renderer.openPreview().get(3, TimeUnit.SECONDS));
        assertEquals(SUBMITTED, renderer.render(object("first")).get(3, TimeUnit.SECONDS));
        assertEquals(SUBMITTED, renderer.render(object("second")).get(3, TimeUnit.SECONDS));
        assertEquals(SUBMITTED, renderer.openPreview().get(3, TimeUnit.SECONDS));
        assertEquals(List.of("3D Preview", "first", "second"), prepared);
        assertEquals(List.of("3D Preview", "first", "second", "second"), shown);
        assertEquals(List.of("first"), cameraResets);
        assertSame(shownSetups.get(2), shownSetups.get(3));
    }

    @Test
    void failedFirstTerrainSubmissionDoesNotDisableNextCameraReset() throws Exception {
        var failure = new IllegalArgumentException("cannot show terrain");
        renderer = new CubeArrayRenderer(this::prepare, this::warm,
                setup -> setup.name.equals("invalid") ? CompletableFuture.failedFuture(failure) : resetAndShow(setup),
                this::show, this::stop);
        warmAllowed.complete(null);
        var invalid = renderer.render(object("invalid"));
        assertSame(failure, assertThrows(ExecutionException.class, () -> invalid.get(3, TimeUnit.SECONDS)).getCause());
        assertEquals(SUBMITTED, renderer.render(object("valid")).get(3, TimeUnit.SECONDS));
        assertEquals(List.of("valid"), cameraResets);
        assertEquals(List.of("valid"), shown);
    }

    @Test
    void inFlightWelcomeCannotReplayAfterNewTerrain() throws Exception {
        var entered = new CountDownLatch(1);
        var allowed = new CompletableFuture<Void>();
        renderer = new CubeArrayRenderer(this::prepare, this::warm, this::resetAndShow,
                setup -> blockedShow(setup, entered, allowed), this::stop);
        warmAllowed.complete(null);
        var welcome = renderer.openPreview();
        assertTrue(entered.await(2, TimeUnit.SECONDS));
        var terrain = renderer.render(object("terrain"));
        var activation = renderer.openPreview();
        allowed.complete(null);
        assertEquals(SUBMITTED, welcome.get(3, TimeUnit.SECONDS));
        assertEquals(SUBMITTED, terrain.get(3, TimeUnit.SECONDS));
        assertEquals(SUBMITTED, activation.get(3, TimeUnit.SECONDS));
        assertEquals(List.of("3D Preview", "terrain"), shown);
        assertEquals(List.of("terrain"), cameraResets);
    }

    static WPObject blockedObject(CountDownLatch entered, CompletableFuture<Void> allowed, String name) {
        entered.countDown();
        allowed.join();
        return object(name);
    }
}
