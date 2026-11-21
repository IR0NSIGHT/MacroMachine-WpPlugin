# Path tool map UI overhaul — LLM proposal

## Plan conventions and state — LLM proposal

**Status: Proposed.** This is the active planning document; no implementation or verification has run. Authorship labels are **Human developer**, **LLM proposal**, **Mixed authorship**, and **Unattributed**. Approval is tracked separately as Proposed, Agreed, or Rejected. Save VS Code edits to disk before asking the assistant to read them.

## Original request — Human developer

> the UI for the path tool is terrible. it needs an overhaul, requierment is a similarly easy to use UI liek the city-tool has. copy the citytools map-overlay into path tool so that path nodes can be placed by rightclick, selected by leftclick, including drag&multi select and the dragged to move.

### UX clarification — Human developer

> the UX has to be as similar as possible to citylayer. this includes the icon appearing and vanishing

## Goal and scope — LLM proposal

**Status: Proposed interpretation of the request.** Make the existing WorldPainter Road Tool (`PathTool`) a direct map-based node editor with the City Tool's overlay presentation. Right-click adds nodes, left-click selects, empty-map dragging box-selects, and dragging a node moves the selection. Keep road/river generation, presets, paint, water, terrain, slope and transition controls. This concerns the Java/Swing plugin, not `webUI` or the older `rivertool` editor.

Do not introduce a second path format, multiple independent paths, segment insertion, clipboard tools, a Java version upgrade, or changes to City Tool controls in this overhaul.

## Relevant existing APIs and types — LLM proposal

**Status: Repository observations; not proposed new APIs.** Paths below are relative to the repository root. Java files under `src/main/java/org/ironsight/wpplugin/macromachine/`:

| Location | Existing API/type | Relevance |
| --- | --- | --- |
| `Layers/PathBuilder/PathTool.java:37` | `PathTool extends AbstractBrushOperation implements PaintOperation, UndoListener, KeyEventDispatcher` | Keep brush radius and the WorldPainter tool integration. Registered in `MacroMachinePlugin.java:107`. |
| `PathTool.java:91,443–469` | `AttributeKey<ArrayList<Float>> PATHHANDLES_KEY`; `ArrayList<Point4f> getCurrentPath()`; `void setCurrentPath(ArrayList<Point4f>)` (currently private) | `PATHTOOL-PATHHANDLES` stores ordered flat `[x,y,z,w]` tuples: world X/Y, height, radius. Setter calls `dimension.setAttribute(PATHHANDLES_KEY, flatData, true)`. Retain serialization compatibility. |
| `PathTool.java:500–532` | `tick(int centreX, int centreY, boolean inverse, boolean first, float dynamicLevel)` | Currently inverse/right-click clears the path without adding a node; left-click appends. This is the control mapping to replace, not preserve. |
| `PathTool.java:472–498,534–663` | `deleteLastPathPoint()`; `OnSmoothPath(List<Point4f> pathHandles, boolean onlyPreview)` (currently private) | Existing preview regeneration and Apply pipeline. Generation mutates its input list by padding short paths. Use deep working copies, never the editor's authoritative nodes. |
| `PathTool.java:128–148,685–703` | `dispatchKeyEvent(KeyEvent)`; `activate()`/`deactivate()`; `undoPerformed()`/`redoPerformed()` | Backspace is handled globally while active; no map-focus guard. Undo callbacks are empty and no listener registration was found in this class. Do not assume node undo/redo works. |
| `Layers/PathBuilder/PathToolBackend.java:26–61` | `PathResult { List<Point4f> path; HashMap<Point4f,Integer> handlesToPathIndex; }`; `getPathFromHandles(List<Point4f>, float)` | Existing interpolation remains authoritative. Selection must not use mutable `Point4f` values as identity. |
| `Layers/CityBuilder/CityEditToolOperation.java:336–382,957–1338` | Private nested `DragOverlay extends JComponent`; `setMapView(WorldPainterView)`, `startDrag(Point)`, `updateDrag(Point)`, `endDrag()`, `getDragBounds()`, `isBoxSelection()`, outline and text methods | Reuse the mouse-transparent overlay, animated tool title, transient messages and selection rectangle styling. It stores geometry in world space and converts during painting. |
| `CityEditToolOperation.java:961–976,1073–1233` | `showOverlayText(String)`, `setOverlayText(String)`, `clearOverlayText()`, `showTransientInfoText(String,int)`, `layoutOverlayText()` | Activation shows the title/icon at 2× scale near the upper center; after 300 ms it animates for 500 ms to the top-right margin, shrinking and tinting white to light gray with smoothstep easing. Deactivation clears/removes it immediately, not via a title fade-out. Transient messages separately fade over their last 500 ms. |
| `CityEditToolOperation.java:123–160,288–301,436–459,651–698` | Mouse-event listener; `handleClick(int,int,boolean,boolean)`; `selectWithinBox(CityLayer,Rectangle)` | City left-click toggles membership; box selection replaces selection. City Ctrl-left places; right-click relocates selection. It does not implement node-style drag-to-move, so copying the listener verbatim is insufficient. |
| WorldPainter view calls used by City Tool | `viewToWorld(Point)`, `worldToView(...)`, `getViewListener()`/`setViewListener(...)`, component resize listener | Track zoom, pan and resize; preserve and restore the previous view listener. Do not capture sidebar interactions. |
| `Gui/HelpCatalog.java:38–59` | `HelpCatalog.ROAD`, `HelpDialog.HelpItem` | Duplicates Road Tool help intentionally; `HelpCatalogTest.roadHelpStaysInSyncWithPathTool()` checks equality. Update both help definitions. |
| `Layers/PathBuilder/PathToolScreenshot.java` | Component names `PathTool.Apply`, `PathTool.TransitionProfiles`; screenshot harness finds the slope spinner by position | Preserve names; if layout changes affect spinner traversal, update the harness deliberately. |

Build constraints: `pom.xml` targets Java 21, WorldPainter 2.26.3, and uses Maven Surefire/JUnit Jupiter and Spotless. Use Java-21-compatible APIs and existing dependencies; Java 25-only conventions are not applicable without a separate approved upgrade.

## Interaction contract — LLM proposal

**Status: Proposed except for the explicitly agreed selection, height, shared-presentation and City UX-parity decisions recorded below.** Gesture thresholds, extra actions and preview timing remain proposals.

| Gesture/action | Proposed result |
| --- | --- |
| Right-click map | Append exactly one node to the ordered path, using terrain height and current brush radius. First click creates the first node. Never clear the path or move existing selection. |
| Left-click node | Match City Tool: toggle that node's membership, allowing additive multi-selection without modifiers. Defer toggling until release so dragging an already-selected node does not deselect it. |
| Left-click empty map | Clear selection without adding, deleting or moving nodes. |
| Left-drag empty map | Show the City-style selection rectangle; on release replace selection with nodes whose centers lie inside, inclusively. Normalize all drag directions. |
| Left-drag selected node | Move all selected nodes by one common world-space delta, preserving relative positions and path order. |
| Left-drag unselected node | Select only that node, then move it. |
| Small pointer motion | Treat movement below a screen-pixel threshold as a click, independent of zoom. No accidental selection rectangle or movement. |
| New path button | Explicitly clear nodes, selection, preview annotations and cached tiles; replaces the old destructive right-click reset. |
| Escape | Cancel an in-progress drag without committing it; otherwise clear selection. |
| Delete / Backspace / Ctrl+A | Delete selected nodes / preserve existing remove-last behavior / select all nodes. Handle only while map interaction owns focus, never while typing in controls. |
| Apply | Commit the previewed path through the existing terrain/paint/water pipeline; preserve existing post-Apply path retention unless explicitly changed. |

**Status: Agreed for terrain-height resampling.** Moving nodes updates X/Y and resamples each moved node's stored height from terrain at its destination, including when Snap to terrain is off. Preserve radius and relative X/Y offsets, not relative heights. The existing Snap to terrain filter still governs the interpolated path between nodes. Radius changes affect new nodes, not previously stored nodes. Guard placement/movement over missing tiles and avoid partial group movement (guard behavior remains proposed).

### City UX parity, including icon lifecycle — LLM proposal

**Status: Agreed requirement to match City Layer UX as closely as possible; the following are repository-grounded acceptance details.** Reuse the same presentation behavior, not an approximate Path-only redesign:

- Show the Path Tool's title/icon on activation using the City introduction's scale, placement, timing, easing, tint and movement to the top-right corner. Keep the settled badge visible for the same lifecycle as City; do not add hover-only visibility or an independent auto-hide timer.
- Clear the title/icon and messages when the tool deactivates or its overlay detaches, stopping timers so they cannot reappear afterward. Match City's immediate disappearance; do not invent a new exit animation. Reactivation replays the same introduction cleanly, including after rapid tool switching mid-animation.
- Preserve City typography, icon/text spacing, responsive sizing, background, selection-outline/marquee styling and transient-message placement/fade through the shared presentation. Tool-specific title/icon and node geometry may differ; interaction differences are limited to the requested path placement/movement and necessary node semantics.
- Treat City as the reference during extraction: do not simplify away its animation or change City to fit a divergent Path design. Keep path-specific generation controls without introducing gratuitous visual or behavioral differences in the shared map UX.

## Design and tradeoffs — LLM proposal

**Status: Proposed.**

- **Status: Agreed for shared presentation.** Extract only the reusable overlay presentation into a small shared Swing component; proposed location is `Gui`, with configurable title/icon. City retains its current gestures and object logic; Path Tool owns node rendering and selection/movement. A literal copy would have lower initial City risk but duplicate hundreds of lines of label animation, timer and coordinate handling. Do not build a general editor framework.
- Use ordered node indices for transient selection and drag snapshots; rebuild/remap selection after deletion and clear it on path replacement, dimension changes and undo/redo. Keep selection and temporary drag state out of persisted attributes.
- Own each mouse gesture in one place. Audit `AbstractBrushOperation` tick/event delivery before wiring: the host and an added listener must not both add/move nodes. Do not rely on `tick` alone for press/drag/release. Use view-scoped listeners where host behavior permits, with explicit active/map-component checks.
- Distinguish pending click, box selection and move gesture. Capture a deep original-node snapshot on press. Draw temporary node markers and a connecting path during movement; commit nodes and regenerate the full terrain preview once on release. Avoid running the expensive tile pipeline for every mouse-motion event. The full terrain preview can remain at its prior position until release; the moving overlay must make that distinction clear.
- Show all node handles in screen-sized markers, selected handles with clear contrasting outlines, plus a selection count and City-style transient messages. Hit-test nearest node in screen pixels with deterministic ties. Keep map navigation available and the overlay mouse-transparent.
- Regeneration must clear the old annotation footprint even when moved away or reduced to zero nodes. Preserve the prior event-inhibition state with `try/finally`; adding, moving and deleting must not alter actual terrain/paint/water until Apply. Explicitly invalidate cached terrain after Apply or relevant external edits so subsequent previews do not use stale baselines.
- Attach overlay, listeners and timers only for the active view; detach and cancel gestures on deactivation/view or dimension changes. Handle release outside the map so no gesture remains stuck. Verify host undo registration and attribute history before promising one undo step per completed edit; never one per pointer-motion event.
- Keep panel changes focused: concise gesture help, selection count, New path and existing Apply/presets; group current generation controls for readability. No unrelated terrain-algorithm rewrite or new dependencies.

## Implementation steps — LLM proposal

**Status: Proposed; execute only in Build mode after decisions are settled.**

1. Characterize host event ownership, view/dimension switching and attribute undo behavior; establish focused tests using the City Tool's test-view pattern and fresh test dimensions.
2. Extract/reuse the City overlay presentation, preserving City visuals, interactions, listener chaining and cleanup, including the exact title/icon introduction, settled badge, disappearance and message fading. Add path-specific node/path drawing and map-coordinate hit-testing.
3. Add transient selection/gesture state and route right-click placement, City-style toggle/box selection and single/group dragging through a single gesture owner. Resample moved node heights from destination terrain, preserving radii, path ordering and tuple serialization.
4. Centralize completed-node edits and preview refresh, including empty paths, cancellation, selection remapping, event inhibition and cache invalidation. Integrate actual undo/redo refresh using verified host APIs, or document the precise limitation for a decision before shipping.
5. Update panel actions/status and both help definitions. Preserve screenshot component names and adjust the harness only if needed.
6. Run targeted tests, formatting/build checks and in-WorldPainter acceptance checks below; record failures and any host-specific limits.

## Verification strategy — LLM proposal

**Status: Proposed; not run in planning mode.**

- New `PathTool` interaction tests: one node per right-click; left-click never appends; selected/unselected/empty click behavior; click-versus-drag threshold; repeated/overlapping nodes; screen-space hit testing across zoom; box edges, negative coordinates and all drag directions.
- Movement tests: single and discontiguous multi-selection; shared X/Y delta; exact destination-terrain heights for every moved node with Snap to terrain on and off; unchanged order/radius; unchanged unselected nodes; no mutation before release; cancellation; outside-map release; no partial update at invalid locations; selection index correctness after deletion.
- Persistence/preview tests: flat tuple round-trip, zero/one/two/three nodes, regeneration removes the old footprint, moving/deleting affects preview but not terrain until Apply, event-inhibition restoration after failure, fresh cached baselines after Apply, undo/redo and view/dimension switch behavior.
- Regression: existing `PathToolBackendTest`, `CityEditToolOperationTest`, `HelpCatalogTest`; City click/box selection and overlay lifecycle stay unchanged. Add lifecycle/painting checks that helper-only tests would miss.
- UX parity: compare City and Path overlays at introduction, animation completion, settled state and deactivation; check matching timing/layout/tint and transient-message fade. Test switching tools mid-animation, rapid deactivate/reactivate, resize during animation and timer cleanup: no residual icon, delayed reappearance or accumulated overlays. Use lifecycle/state assertions plus side-by-side visual checks rather than timing-sensitive sleeps alone.
- In Build mode, run targeted Maven tests first, then `mvn verify` from repository root. Verify includes screenshot execution and may require a graphical desktop. No webUI checks are needed unless that scope changes.
- Manual WorldPainter acceptance: create a road with right-clicks; select several nodes by clicks and a marquee; drag the group at multiple zoom levels; verify pan/resize alignment and navigation; delete/cancel/start new; use road/river presets and Apply; switch tools/dimensions and exercise undo/redo. Check there are no duplicate additions, sticky drags or leaked overlays.

## Follow-up answers — Human developer

In response to selection, movement-height and overlay-reuse questions, respectively:

> exactly like citylayer selecting schematics.

> Resample terrain height

> Shared presentation (Recommended)

## Decision register — LLM proposal

1. **Agreed:** City-style selection: ordinary left-click toggles membership, empty-map click deselects, and box selection replaces the selection. Adapt schematic hit-testing to node markers; dragging selected nodes remains the originally requested path-specific addition.
2. **Agreed:** Resample every moved node's stored height from destination terrain; preserving old heights is **Rejected**.
3. **Agreed:** Share overlay presentation between City and Path tools, without changing City controls. Path-only duplication is not the selected approach.
4. **Proposed:** Additional panel actions/shortcuts, pixel thresholds, release-time full-preview regeneration, precise extraction API and undo integration/limitations. These were not approved by the three answers above.
5. **Agreed:** Match City Layer UX as closely as possible, explicitly including the icon appearing and vanishing. Shared presentation must preserve the City lifecycle and animation, not merely its static appearance.
