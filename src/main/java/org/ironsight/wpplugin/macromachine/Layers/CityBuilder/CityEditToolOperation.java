package org.ironsight.wpplugin.macromachine.Layers.CityBuilder;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyVetoException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.vecmath.Point3i;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.SVGLoader;
import org.ironsight.wpplugin.macromachine.Gui.CtrlDoubleTap;
import org.ironsight.wpplugin.macromachine.Gui.DialogStyle;
import org.ironsight.wpplugin.macromachine.Gui.GlobalActionPanel;
import org.ironsight.wpplugin.macromachine.Gui.HelpDialog;
import org.ironsight.wpplugin.macromachine.Gui.HelpDialog.HelpItem;
import org.ironsight.wpplugin.macromachine.Gui.MapToolOverlay;
import org.ironsight.wpplugin.macromachine.Gui.OperationPanel;
import org.ironsight.wpplugin.macromachine.Gui.OverlayStyle;
import org.ironsight.wpplugin.macromachine.Gui.PreciseDrag;
import org.ironsight.wpplugin.macromachine.Gui.RangeSlider;
import org.ironsight.wpplugin.macromachine.Layers.PathBuilder.LucideIcons;
import org.ironsight.wpplugin.macromachine.Layers.PathBuilder.PathTool;
import org.ironsight.wpplugin.macromachine.Layers.PathBuilder.PathTransformBox;
import org.ironsight.wpplugin.macromachine.Layers.PathBuilder.TransformBoxEditor;
import org.pepsoft.util.swing.TiledImageViewer;
import org.pepsoft.util.undo.UndoManager;
import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.objects.WPObject;
import org.pepsoft.worldpainter.operations.MouseOrTabletOperation;
import org.pepsoft.worldpainter.operations.PaintOperation;
import org.pepsoft.worldpainter.painting.LayerPaint;
import org.pepsoft.worldpainter.painting.Paint;

/**
 * City Tool: places and edits the houses (schematics) of a City Layer. Like the
 * Path Tool, all controls float on the map: the "Schematics" menu (type of the
 * selected or next house, rotate / mirror, randomizing), the "Layer" menu
 * (tutorial layer, import, the dimension's City Layers), overlays and add mode.
 * Houses are selected like path nodes and get the transform box (move, rotate
 * in quarter turns, spread by scaling their positions).
 */
public class CityEditToolOperation extends MouseOrTabletOperation implements PaintOperation, KeyEventDispatcher
{
    public static final String HELP_TITLE = "City Tool";
    public static final String HELP_EXPLANATION = """
            Use the City Tool to place and edit buildings in a City Layer. Pick the City Layer in the Layer menu (or import one from a file), then click houses to select them.
            Click the card showing the current schematic to pick another one from a grid: it becomes the type of the selected houses (or of the next house you add). The Schematics menu rotates, mirrors and randomizes them.
            Selected houses get a transform box: drag the move handle to move them, the rotate handle to turn them (a group turns by any angle, each house facing the nearest quarter turn), the scale handles to spread them apart or pack them together. The crosshair is the pivot: drag it, click it to centre it again.

            City Layers are not compatible with undo/redo. Do not use undo/redo while editing one.
            """;
    public static final List<HelpItem> HELP_ITEMS = List.of(
            new HelpItem("Left click", "Select the building under the cursor"),
            new HelpItem("Ctrl + left click",
                    "Add or remove a building from the selection; on empty map: place a new building"),
            new HelpItem("Add (button) or double-tap Ctrl", "Every click on empty map places a new building"),
            new HelpItem("Add random (Add dropdown)",
                    "Every new building is a random pick of the schematics ticked in the picker"),
            new HelpItem("Shift while dragging", "Precise drag: a tenth of the mouse's speed"),
            new HelpItem("Left-button drag on a building", "Move the selection"),
            new HelpItem("Left-button drag on empty map", "Select buildings fully inside the drag rectangle"),
            new HelpItem("Shift + mouse wheel", "Change the selected building type"),
            new HelpItem("Ctrl + A", "Select all buildings"),
            new HelpItem("Escape", "Leave add mode, else clear the selection"),
            new HelpItem("Ctrl + C", "Copy selected buildings"), new HelpItem("Ctrl + X", "Cut selected buildings"),
            new HelpItem("Ctrl + V", "Paste buildings at the cursor"),
            new HelpItem("Q", "Randomize selected buildings using the random options of the Add menu"),
            new HelpItem("W/A/S/D", "Move selected buildings"),
            new HelpItem("C",
                    "Rotate selected buildings around the selection center; in add mode or with nothing selected: the next building"),
            new HelpItem("X", "Mirror selected buildings; in add mode or with nothing selected: the next building"),
            new HelpItem("Delete", "Delete selected buildings"));

    private static final int OVERLAY_ICON_SIZE = 256;
    /** Hit radius of the transform handles (px). */
    static final double HANDLE_HIT_RADIUS = 12;
    /** Thumbnails in the Schematics menu (px). */
    static final int GRID_TILE = 150;
    /** Outline of every house (overlay part): white, half transparent. */
    static final Color OUTLINE_COLOR = new Color(255, 255, 255, 140);
    /** The current type's preview card below the toolbar (px). */

    private static CityEditToolOperation instance;

    /** What new houses (and Q) randomize. */
    record PlacementOptions(boolean randomRotate, boolean randomSelect, boolean randomMirror) {
    }
    private record ClipboardEntry(CityLayer.Direction rotation, boolean mirrored, int objectIndex, long offsetX,
            long offsetY) {
    }

    Random random = new Random();
    private ObjectState uiState = new ObjectState(CityLayer.Direction.NORTH, false, 0, Integer.MAX_VALUE,
            Integer.MAX_VALUE);
    private PlacementOptions placementOptions = new PlacementOptions(false, false, false);
    private final Map<Point, ObjectState> selectedStates = new LinkedHashMap<>();
    private List<ClipboardEntry> clipboard = List.of();
    private CityLayer clipboardLayer;
    private volatile Point cursorWorldPosition;

    private Paint paint;

    /** WorldPainter's tool settings stay empty: everything lives on the map. */
    private final JPanel optionsPanel = new JPanel();
    private final JToggleButton addMode = new JToggleButton("Add", false);
    /**
     * "Add random": every house placed is a random pick from the schematics ticked
     * in the picker ({@link #randomPool}), instead of the current type.
     */
    private boolean addRandom;
    /** Schematics (indices) ticked for "Add random"; per layer. */
    private final java.util.TreeSet<Integer> randomPool = new java.util.TreeSet<>();
    /** Double-tapping Ctrl toggles add mode, as in the Path Tool. */
    private final CtrlDoubleTap ctrlTaps = new CtrlDoubleTap(() -> this.cursorOverMap && this.gestureStart == null,
            () -> addMode.setSelected(!addMode.isSelected()));
    /** Shift during a drag: a tenth of the mouse's speed, as in the Path Tool. */
    private final PreciseDrag preciseDrag = new PreciseDrag();
    private final JToggleButton showOverlays = new JToggleButton("", true);
    /**
     * Overlay part: the layer paints in its color instead of the schematics'
     * blocks.
     */
    private final JToggleButton showHighlight = new JToggleButton("Highlight colors", true);
    /** Overlay part: an outline around every house of the layer. */
    private final JToggleButton showOutlines = new JToggleButton("Outlines", true);
    /** Overlay part: the ">" on the front edge of house outlines. */
    private final JToggleButton showFacing = new JToggleButton("Facing arrows", true);
    private final CityOverlay overlay = new CityOverlay();
    private WorldPainterView overlayView;
    private volatile boolean cursorOverMap;
    private TiledImageViewer.ViewListener previousViewListener;
    private final TiledImageViewer.ViewListener overlayViewListener = changedView -> {
        if (previousViewListener != null)
            previousViewListener.viewChanged(changedView);
        syncWorld();
        overlay.repaint();
    };
    private final ComponentAdapter overlayResizeListener = new ComponentAdapter() {
        @Override
        public void componentResized(ComponentEvent event) {
            resizeOverlay();
        }
    };
    private final MouseAdapter mapMouse = new MouseAdapter() {
        @Override
        public void mouseEntered(MouseEvent event) {
            cursorOverMap = true;
        }

        @Override
        public void mouseExited(MouseEvent event) {
            // moving onto a HUD button also fires an exit; only leaving the bounds counts
            if (!new Rectangle(event.getComponent().getSize()).contains(event.getPoint())) {
                cursorOverMap = false;
                cursorWorldPosition = null;
                setHovered(null);
            }
        }

        @Override
        public void mouseMoved(MouseEvent event) {
            cursorOverMap = true;
            trackCursor(event.getPoint());
            if (gestureStart == null)
                setHovered(hitTest(event.getPoint()));
            if (addMode.isSelected())
                overlay.repaint(); // the preview of the next house follows the mouse
        }

        @Override
        public void mousePressed(MouseEvent event) {
            cursorOverMap = true;
            ctrlTaps.interrupt();
            trackCursor(event.getPoint());
            if (SwingUtilities.isLeftMouseButton(event) && !event.isAltDown() && !event.isMetaDown())
                handlePress(event.getPoint(), event.isControlDown(), event.isShiftDown());
        }

        @Override
        public void mouseDragged(MouseEvent event) {
            cursorOverMap = true;
            trackCursor(event.getPoint());
            handleDrag(event.getPoint(), event.isShiftDown());
        }

        @Override
        public void mouseReleased(MouseEvent event) {
            if (SwingUtilities.isLeftMouseButton(event))
                handleRelease(event.getPoint(), event.isShiftDown());
        }
    };

    // ---- gesture state (view coordinates unless named "world") ----

    /** Something on the map that reacts to the mouse. */
    enum HitKind {
        HOUSE(Cursor.MOVE_CURSOR), MOVE(Cursor.MOVE_CURSOR), ROTATE(Cursor.HAND_CURSOR), SCALE(
                Cursor.HAND_CURSOR), PIVOT(Cursor.MOVE_CURSOR);

        final int cursor;

        HitKind(int cursor) {
            this.cursor = cursor;
        }
    }

    /**
     * @param side
     *            scale handles: PathTransformBox.SCALE_*; otherwise 0
     * @param house
     *            the house under the mouse for {@link HitKind#HOUSE}
     */
    record Hit(HitKind kind, int side, ObjectState house) {
    }

    private Hit hovered;
    private Cursor cursorBeforeHover;
    private boolean cursorOverridden;
    private Point gestureStart;
    private Point gestureWorldStart;
    private Hit gestureHit;
    private boolean gestureAdditive;
    private boolean gestureDragged;
    /** The transform the current drag would apply to the selection, or null. */
    private UnaryOperator<ObjectState> gestureTransform;
    private String gestureHeader;
    /**
     * The transform box, shared with the Path Tool: box angle and placed pivot per
     * selection, the running handle drag, hit testing and painting.
     */
    private final TransformBoxEditor transformEditor = new TransformBoxEditor();
    /**
     * Identity of the current selection for the transform editor: replaced when the
     * user selects other houses, kept when the selected houses are moved, turned or
     * spread (their positions change, the selection does not).
     */
    private Object selectionToken = new Object();

    private static BufferedImage loadOverlayIcon() {
        var svgUrl = Objects.requireNonNull(CityEditToolOperation.class.getResource("/icons/castle.svg"),
                "City Tool overlay SVG not found");
        SVGDocument document = Objects.requireNonNull(new SVGLoader().load(svgUrl),
                "City Tool overlay SVG could not be loaded");
        BufferedImage icon = new BufferedImage(OVERLAY_ICON_SIZE, OVERLAY_ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = icon.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            var viewBox = document.viewBox();
            graphics.scale(OVERLAY_ICON_SIZE / viewBox.getWidth(), OVERLAY_ICON_SIZE / viewBox.getHeight());
            document.render(null, graphics);
        } finally {
            graphics.dispose();
        }
        return icon;
    }

    public CityEditToolOperation() {
        super("City Tool", "Edit city layers using this tool", "city-edit-tool-operation");
        instance = this;
        addMode.setName("CityTool.AddMode");
        addMode.getModel().addItemListener(event -> {
            updateCursor();
            overlay.showTransientInfoText(addMode.isSelected()
                    ? "Add mode: left click on empty map places a house (Esc to leave)"
                    : "Add mode off", 2400);
            overlay.repaint();
        });
        showOverlays.setName("CityTool.Overlays");
        showOverlays.getModel().addItemListener(event -> {
            applyHighlightColors();
            overlay.repaint();
        });
        showHighlight.setName("CityTool.Overlays.HighlightColors");
        showHighlight.getModel().addItemListener(event -> applyHighlightColors());
        showOutlines.setName("CityTool.Overlays.Outlines");
        showOutlines.getModel().addItemListener(event -> overlay.repaint());
        showFacing.setName("CityTool.Overlays.Facing");
        showFacing.getModel().addItemListener(event -> overlay.repaint());

        Toolkit.getDefaultToolkit().addAWTEventListener(e -> {
            if (e instanceof MouseWheelEvent ev && isActive()
                    && (ev.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0) {
                if (ev.getComponent()
                        .equals(SwingUtilities.getDeepestComponentAt(ev.getComponent(), ev.getX(), ev.getY()))) { // fire
                    // only once
                    onMouseWheel(ev.getWheelRotation());
                }
            }
        }, AWTEvent.MOUSE_WHEEL_EVENT_MASK);

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);
    }

    void setPlacementOptions(PlacementOptions placementOptions) {
        this.placementOptions = Objects.requireNonNull(placementOptions);
    }

    PlacementOptions placementOptions() {
        return placementOptions;
    }

    public static void updateInstance() {
        if (instance != null)
            instance.updatePanel();
    }

    public static UndoManager getUndoManager(Dimension obj) throws IllegalAccessException, NoSuchFieldException {
        Field f = obj.getClass().getDeclaredField("undoManager");
        f.setAccessible(true);
        return (UndoManager) f.get(obj);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        // typing into a field (the picker's search, a spinner) is no map shortcut
        if (isActive() && overlayView != null && !PathTool.allowsMapShortcut(overlayView,
                KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner()))
            return false;
        if (isActive() && getDimension() != null)
            ctrlTaps.onKey(e);
        if (e.getID() == KeyEvent.KEY_PRESSED) {
            if (!isActive() || getDimension() == null)
                return false;
            if (!cursorOverMap)
                return false;
            if (e.isControlDown() && !e.isShiftDown() && !e.isAltDown() && !e.isMetaDown()) {
                if (e.getKeyCode() == KeyEvent.VK_C || e.getKeyCode() == KeyEvent.VK_X
                        || e.getKeyCode() == KeyEvent.VK_V) {
                    handleKeyInteraction(e.getKeyCode(), true);
                    return true;
                }
            }
            if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_A) {
                handleKeyInteraction(e.getKeyCode(), true);
                return false;
            }
            if (e.isShiftDown() || e.isControlDown() || e.isAltDown() || e.isMetaDown())
                return false;
            handleKeyInteraction(e.getKeyCode(), false);
        }
        return false; // return false to allow other listeners to handle the event
    }

    static boolean isMapComponent(Component mapView, Component eventComponent) {
        return mapView != null && eventComponent != null
                && (eventComponent == mapView || SwingUtilities.isDescendingFrom(eventComponent, mapView));
    }

    /** Applies one unmodified keyboard interaction from the city tool. */
    void handleKeyInteraction(int keyCode) {
        handleKeyInteraction(keyCode, false);
    }

    void handleKeyInteraction(int keyCode, boolean controlDown) {
        try {
            if (!getDimension().isEventsInhibited())
                getDimension().setEventsInhibited(true);
            CityLayer layer = getSelectedLayer();
            if (controlDown && keyCode == KeyEvent.VK_A) {
                selectAll(layer);
                return;
            }
            if (controlDown) {
                switch (keyCode) {
                    case KeyEvent.VK_C -> copySelection(layer);
                    case KeyEvent.VK_X -> cutSelection(layer);
                    case KeyEvent.VK_V -> pasteClipboard(layer);
                    default -> {
                    }
                }
                return;
            }
            if (keyCode == KeyEvent.VK_ESCAPE) {
                if (overlay.lightboxOpen())
                    overlay.closeLightbox();
                else if (operationPanel != null)
                    closeOperation();
                else if (overlay.typeGridOpen())
                    overlay.closeTypeGrid();
                else if (gestureStart != null)
                    cancelGesture();
                else if (addMode.isSelected())
                    addMode.setSelected(false);
                else if (layer != null)
                    deselect(layer);
                return;
            }
            // C / X in add mode or with nothing selected turn or mirror the next house
            if ((keyCode == KeyEvent.VK_C || keyCode == KeyEvent.VK_X) && layer != null && setsUpNextHouse()) {
                if (keyCode == KeyEvent.VK_C)
                    turnNext(1);
                else
                    mirrorNext();
                return;
            }
            boolean requiresSelection = keyCode == KeyEvent.VK_Q || keyCode == KeyEvent.VK_W || keyCode == KeyEvent.VK_A
                    || keyCode == KeyEvent.VK_S || keyCode == KeyEvent.VK_D || keyCode == KeyEvent.VK_C
                    || keyCode == KeyEvent.VK_X;
            if (requiresSelection && (layer == null || selectedStates.isEmpty()))
                return;

            switch (keyCode) {
                case KeyEvent.VK_Q -> applyToSelection(layer, this::randomizeState);
                case KeyEvent.VK_DELETE -> deleteSelected();
                case KeyEvent.VK_W -> moveSelection(layer, 0, -1);
                case KeyEvent.VK_S -> moveSelection(layer, 0, 1);
                case KeyEvent.VK_A -> moveSelection(layer, -1, 0);
                case KeyEvent.VK_D -> moveSelection(layer, 1, 0);
                case KeyEvent.VK_C -> rotateSelection(layer);
                case KeyEvent.VK_X -> mirrorSelection(layer);
                default -> {
                }
            }
        } catch (Exception ex) {
            GlobalActionPanel.ErrorPopUp(ex);
        } finally {
            if (getDimension().isEventsInhibited())
                getDimension().setEventsInhibited(false);
        }
    }

    /** Runs an edit of the layer with WorldPainter's events held back. */
    private void edit(Runnable change) {
        Dimension dimension = getDimension();
        boolean inhibited = dimension.isEventsInhibited();
        if (!inhibited)
            dimension.setEventsInhibited(true);
        try {
            change.run();
        } catch (Exception ex) {
            GlobalActionPanel.ErrorPopUp(ex);
        } finally {
            if (!inhibited && dimension.isEventsInhibited())
                dimension.setEventsInhibited(false);
        }
    }

    private void selectAll(CityLayer layer) {
        if (layer == null)
            return;

        clearSelection();
        for (ObjectState state : layer.getAllObjectStates())
            addSelectedState(state);

        if (selectedStates.isEmpty()) {
            deselect(layer);
            showStatusMessage(selectionStatusMessage());
            return;
        }

        uiState = new ArrayList<>(selectedStates.values()).getLast();
        applyToUi(uiState);
        refreshLayer(layer);
        showStatusMessage(selectionStatusMessage());
    }

    /**
     * A left click at a world position: with Ctrl it toggles the house there in the
     * selection, on empty map it places a house; a plain click selects only the
     * house there (or adds one in add mode, else clears the selection).
     */
    void handleClick(int centreX, int centreY, boolean ctrlDown) {
        cursorWorldPosition = new Point(centreX, centreY);
        CityLayer layer = getSelectedLayer();
        if (layer == null)
            return;

        ObjectState house = houseAt(layer, centreX, centreY);
        if (house == null) {
            if (ctrlDown || addMode.isSelected())
                placeAt(centreX, centreY);
            else {
                deselect(layer);
                showStatusMessage(selectionStatusMessage());
            }
        } else if (ctrlDown)
            toggleSelected(layer, house);
        else {
            selectOnly(house);
            refreshLayer(layer);
            showStatusMessage(selectionStatusMessage());
        }
    }

    /**
     * Places one building at the coordinates (randomized as the Schematics menu
     * says).
     */
    void placeAt(int centreX, int centreY) {
        cursorWorldPosition = new Point(centreX, centreY);
        CityLayer layer = getSelectedLayer();
        if (layer == null || layer.getObjectList().isEmpty())
            return;
        ObjectState template = randomizeState(uiState);
        if (addRandom) {
            randomPool.removeIf(index -> index >= layer.getObjectList().size());
            if (randomPool.isEmpty()) {
                showStatusMessage("Add random: tick schematics in the picker first (click the schematic card)");
                return;
            }
            var pool = new ArrayList<>(randomPool);
            template = setSelectedObjectIndex(pool.get(random.nextInt(pool.size())), template);
        }
        var newState = setCurrentStatePosition(centreX, centreY, template);
        ObjectState chosen = uiState;
        edit(() -> {
            layer.setDataAt(getDimension(), newState.xPos, newState.yPos, newState);
            selectOnly(newState);
            // the next house starts again from what the user chose, not from a random pick
            uiState = setCurrentStatePosition(newState.xPos, newState.yPos, chosen);
            refreshLayer(layer);
        });
    }

    /**
     * select the next schematic from the list, apply.
     *
     * @param direction
     *            up (dir<0) or down (dir>0) wheel
     */
    void onMouseWheel(int direction) {
        CityLayer layer = getSelectedLayer();
        int max = layer == null ? 0 : layer.getObjectList().size();
        if (max == 0)
            return;
        int nextIdx = Math.clamp(uiState.objectIndex + direction, 0, max - 1);
        setType(nextIdx);
    }

    /**
     * Sets the type of the selected houses, or of the next house without a
     * selection.
     */
    void setType(int index) {
        CityLayer layer = getSelectedLayer();
        if (layer == null)
            return;
        if (!selectedStates.isEmpty())
            applyToSelection(layer, state -> setSelectedObjectIndex(index, state));
        else
            applyToUi(setSelectedObjectIndex(index, uiState));
    }

    @Override
    public void interrupt() {
    }

    // ---- overlay lifecycle ----

    private void attachOverlay() {
        WorldPainterView view = getView();
        if (view == null || overlayView == view)
            return;
        detachOverlay();
        overlayView = view;
        overlay.setMapView(view);
        previousViewListener = view.getViewListener();
        view.setViewListener(overlayViewListener);
        view.addComponentListener(overlayResizeListener);
        view.addMouseListener(mapMouse);
        view.addMouseMotionListener(mapMouse);
        view.add(overlay);
        view.setComponentZOrder(overlay, 0);
        resizeOverlay();
        overlay.refresh();
        overlay.showOverlayText("City Tool");
    }

    private void resizeOverlay() {
        if (overlayView != null)
            overlay.setBounds(0, 0, overlayView.getWidth(), overlayView.getHeight());
    }

    private void detachOverlay() {
        cancelGesture();
        setHovered(null);
        cursorOverMap = false;
        cursorWorldPosition = null;
        overlay.clearOverlayText();
        overlay.clearInfoText();
        if (overlayView == null)
            return;
        overlayView.removeMouseListener(mapMouse);
        overlayView.removeMouseMotionListener(mapMouse);
        overlayView.removeComponentListener(overlayResizeListener);
        overlayView.remove(overlay);
        if (overlayView.getViewListener() == overlayViewListener)
            overlayView.setViewListener(previousViewListener);
        overlayView.repaint();
        selectedStates.clear();
        overlay.setMapView(null);
        previousViewListener = null;
        overlayView = null;
    }

    // ---- selection ----

    private void addSelectedState(ObjectState state) {
        selectedStates.put(new Point(state.xPos, state.yPos), state);
        selectionToken = new Object();
        overlay.repaint();
    }

    private void removeSelectedState(ObjectState state) {
        selectedStates.remove(new Point(state.xPos, state.yPos));
        selectionToken = new Object();
        overlay.repaint();
    }

    private void clearSelection() {
        selectedStates.clear();
        selectionToken = new Object(); // the next selection starts with an upright box
        overlay.repaint();
    }

    /** The selected houses (exposed for tests). */
    List<ObjectState> selection() {
        return List.copyOf(selectedStates.values());
    }

    void selectWithinBox(CityLayer layer, Rectangle selectionBounds) {
        selectWithinBox(layer, selectionBounds, false);
    }

    void selectWithinBox(CityLayer layer, Rectangle selectionBounds, boolean additive) {
        if (!additive)
            clearSelection();
        for (ObjectState state : layer.getAllObjectStates())
            if (selectionBounds.contains(footprint(layer, state)))
                addSelectedState(state);

        if (selectedStates.isEmpty()) {
            deselect(layer);
        } else {
            uiState = new ArrayList<>(selectedStates.values()).getLast();
            applyToUi(uiState);
            refreshLayer(layer);
        }
        showStatusMessage(selectionStatusMessage());
    }

    private void selectOnly(ObjectState state) {
        clearSelection();
        addSelectedState(state);
        applyToUi(state);
    }

    private void toggleSelected(CityLayer layer, ObjectState house) {
        if (selectedStates.containsKey(new Point(house.xPos, house.yPos))) {
            removeSelectedState(house);
            if (selectedStates.isEmpty()) {
                deselect(layer);
            } else {
                uiState = new ArrayList<>(selectedStates.values()).getLast();
                applyToUi(uiState);
            }
        } else {
            addSelectedState(house);
            uiState = house;
            applyToUi(uiState);
        }
        showStatusMessage(selectionStatusMessage());
    }

    @Override
    public JPanel getOptionsPanel() {
        return optionsPanel;
    }

    private CityLayer getSelectedLayer() {
        syncWorld();
        if (getPaint() instanceof LayerPaint layerPaint && layerPaint.getLayer() instanceof CityLayer cityLayer
                && inThisWorld(cityLayer))
            return cityLayer;
        return null;
    }

    /** The world (dimension) the tool's state belongs to. */
    private Dimension knownDimension;
    /**
     * Last answer of {@link #inThisWorld}: the layer it was asked about, and the
     * answer.
     */
    private CityLayer checkedLayer;
    private boolean checkedLayerInWorld;

    /**
     * WorldPainter may keep the paint of the previous world when another one is
     * opened: a City Layer of another world is no layer of this one (its houses
     * would show, and dragging them would write them into this map).
     */
    private boolean inThisWorld(CityLayer layer) {
        if (layer != checkedLayer) {
            checkedLayer = layer;
            checkedLayerInWorld = getView() == null || CityLayerLibrary.inWorld(layer, getDimension());
        }
        return checkedLayerInWorld;
    }

    /**
     * Another world was opened: the selection, clipboard, picks for Add random and
     * open panels belong to the previous one.
     */
    private void syncWorld() {
        Dimension dimension = getView() == null ? null : getDimension();
        if (dimension == knownDimension)
            return;
        boolean switched = knownDimension != null;
        knownDimension = dimension;
        checkedLayer = null;
        if (!switched)
            return;
        cancelGesture();
        clearSelection();
        clearClipboard();
        transformEditor.resetPivot();
        randomPool.clear();
        closeOperation();
        overlay.closeTypeGrid();
        overlay.refresh();
    }

    /**
     * A {@code >} on the outside of a house's front edge (view), like
     * {@code |   |>}: an unrotated schematic faces north (up on the map);
     * WorldPainter turns each step clockwise, so EAST faces right. {@code null}
     * where the outline is too small to carry one.
     */
    /** Space between a house's front edge and its ">" (px). */
    static final double FACING_GAP = 4;

    static java.awt.geom.Path2D.Double facingArrow(Rectangle2D bounds, CityLayer.Direction rotation) {
        double size = Math.min(bounds.getWidth(), bounds.getHeight());
        if (size < 6)
            return null;
        double dx = 0, dy = 0;
        switch (rotation) {
            case NORTH -> dy = -1;
            case EAST -> dx = 1;
            case SOUTH -> dy = 1;
            case WEST -> dx = -1;
        }
        double half = (dx != 0 ? bounds.getWidth() : bounds.getHeight()) / 2;
        // a little way out from the middle of the front edge ("| >", not "|>"), the
        // chevron's tip further out
        double gap = FACING_GAP;
        double edgeX = bounds.getCenterX() + dx * (half + gap), edgeY = bounds.getCenterY() + dy * (half + gap);
        double head = Math.min(8, Math.max(4, size * 0.3));
        double tipX = edgeX + dx * head, tipY = edgeY + dy * head;
        var chevron = new java.awt.geom.Path2D.Double();
        chevron.moveTo(edgeX - dy * head, edgeY + dx * head);
        chevron.lineTo(tipX, tipY);
        chevron.lineTo(edgeX + dy * head, edgeY - dx * head);
        return chevron;
    }

    /** World rectangle a house covers on the map. */
    static Rectangle footprint(CityLayer layer, ObjectState state) {
        WPObject object = layer.getObjectForState(state);
        if (object == null)
            return new Rectangle(state.xPos, state.yPos, 1, 1);
        Point3i dimensions = object.getDimensions();
        Point3i offset = object.getOffset();
        return new Rectangle(state.xPos + offset.x, state.yPos + offset.y, dimensions.x, dimensions.y);
    }

    /**
     * The house whose footprint covers a world position; of several, the smallest
     * (a house standing inside a bigger one stays clickable).
     */
    static ObjectState houseAt(CityLayer layer, int x, int y) {
        ObjectState best = null;
        long bestArea = Long.MAX_VALUE;
        for (ObjectState state : layer.getAllObjectStates()) {
            Rectangle bounds = footprint(layer, state);
            long area = (long) bounds.width * bounds.height;
            if (bounds.contains(x, y) && area < bestArea) {
                best = state;
                bestArea = area;
            }
        }
        return best;
    }

    @Override
    protected void tick(int centreX, int centreY, boolean inverse, boolean first, float dynamicLevel) {
        // every click and drag is handled by the map mouse listener (handles, box
        // selection, add mode); a right click does nothing
    }

    private void ensureLayerHasUndoManager(CityLayer layer, Dimension dimension) {
        try {
            UndoManager undoManager = getUndoManager(dimension);
            undoManager.removeListener(layer); // remove otherwise we add over and over
            layer.registerLayer(undoManager);
        } catch (IllegalAccessException | NoSuchFieldException ex) {
            GlobalActionPanel.ErrorPopUp(ex);
        }
    }

    @Override
    protected void activate() throws PropertyVetoException {
        super.activate();
        addMode.setSelected(false);
        // overlay switches are not persisted: every activation starts with all on,
        // highlight colors as the layer has them
        syncHighlightFromLayer();
        showOverlays.setSelected(true);
        showOutlines.setSelected(true);
        showFacing.setSelected(true);
        attachOverlay();
        updatePanel();
    }

    @Override
    protected void deactivate() {
        closeOperation();
        overlay.endDrag();
        detachOverlay();
        super.deactivate();
    }

    protected void paintChanged(Paint ignored) {
        checkedLayer = null;
        cancelGesture();
        clearSelection();
        clearClipboard();
        transformEditor.resetPivot();
        randomPool.clear(); // indices belong to the old layer
        overlay.closeTypeGrid();
        closeOperation();
        // a newly chosen layer shows its own highlight colors setting, unless the
        // overlays are off (then it is shown without them too)
        if (showOverlays.isSelected())
            syncHighlightFromLayer();
        else
            applyHighlightColors();
        updatePanel();
    }

    private void applyToUi(ObjectState uiState) {
        CityLayer layer = getSelectedLayer();
        if (layer == null)
            return;
        this.uiState = uiState;
        overlay.refresh();
    }

    private void applyToSelection(CityLayer layer, UnaryOperator<ObjectState> transform) {
        if (selectedStates.isEmpty())
            return;

        List<ObjectState> oldStates = new ArrayList<>(selectedStates.values());
        int activeIndex = 0;
        for (int i = 0; i < oldStates.size(); i++) {
            ObjectState state = oldStates.get(i);
            if (state.xPos == uiState.xPos && state.yPos == uiState.yPos) {
                activeIndex = i;
                break;
            }
        }
        List<ObjectState> newStates = oldStates.stream().map(transform).toList();

        for (ObjectState state : oldStates)
            layer.removeDataAt(getDimension(), state.xPos, state.yPos);
        for (ObjectState state : newStates)
            layer.setDataAt(getDimension(), state.xPos, state.yPos, state);

        selectedStates.clear();
        for (ObjectState state : newStates)
            selectedStates.put(new Point(state.xPos, state.yPos), state);

        uiState = newStates.get(Math.min(activeIndex, newStates.size() - 1));
        applyToUi(uiState);
        refreshLayer(layer);
        overlay.repaint();
    }

    private void moveSelection(CityLayer layer, int deltaX, int deltaY) {
        applyToSelection(layer, moveBy(deltaX, deltaY));
        transformEditor.movePivot(selectionToken, deltaX, deltaY);
    }

    private void rotateSelection(CityLayer layer) {
        if (selectedStates.isEmpty())
            return;

        Point center = getSelectionCenter(new ArrayList<>(selectedStates.values()));
        applyToSelection(layer, state -> rotateStateAround(state, center));
        transformEditor.turnBox(selectionToken, 90);
    }

    /** Angle of the transform box for the current selection (degrees). */
    double boxAngle() {
        return transformEditor.boxAngle(selectionToken);
    }

    private void mirrorSelection(CityLayer layer) {
        applyToSelection(layer, state -> setIsMirrored(!state.mirrored, state));
    }

    private Point getSelectionCenter(List<ObjectState> states) {
        long minX = Long.MAX_VALUE;
        long minY = Long.MAX_VALUE;
        long maxX = Long.MIN_VALUE;
        long maxY = Long.MIN_VALUE;
        for (ObjectState state : states) {
            minX = Math.min(minX, state.xPos);
            minY = Math.min(minY, state.yPos);
            maxX = Math.max(maxX, state.xPos);
            maxY = Math.max(maxY, state.yPos);
        }
        return new Point(Math.toIntExact(Math.floorDiv(minX + maxX, 2)),
                Math.toIntExact(Math.floorDiv(minY + maxY, 2)));
    }

    /**
     * A quarter turn counter-clockwise on screen around {@code center}: the
     * position and the house itself (WorldPainter turns objects clockwise per step,
     * so this is a step back).
     */
    static ObjectState rotateStateAround(ObjectState state, Point center) {
        long relativeX = (long) state.xPos - center.x;
        long relativeY = (long) state.yPos - center.y;
        int x = Math.toIntExact((long) center.x + relativeY);
        int y = Math.toIntExact((long) center.y - relativeX);
        return new ObjectState(state.rotation.counterClockwise(), state.mirrored, state.objectIndex, x, y);
    }

    // ---- transforms (pure, so tests and the drag preview share them) ----

    static UnaryOperator<ObjectState> moveBy(int deltaX, int deltaY) {
        return state -> new ObjectState(state.rotation, state.mirrored, state.objectIndex, state.xPos + deltaX,
                state.yPos + deltaY);
    }

    /**
     * {@code quarterTurns} quarter turns counter-clockwise on screen around
     * {@code pivot}.
     */
    static UnaryOperator<ObjectState> rotateAround(Point pivot, int quarterTurns) {
        int turns = Math.floorMod(quarterTurns, 4);
        return state -> {
            ObjectState turned = state;
            for (int i = 0; i < turns; i++)
                turned = rotateStateAround(turned, pivot);
            return turned;
        };
    }

    /**
     * Turns the houses' positions by {@code degrees} counter-clockwise on screen
     * around {@code pivot} (any angle); each house turns by the nearest whole
     * number of quarter turns, so the houses stay aligned to the block grid.
     */
    static UnaryOperator<ObjectState> rotateAround(Point2D pivot, double degrees) {
        double radians = Math.toRadians(degrees);
        double cos = Math.cos(radians), sin = Math.sin(radians);
        int turns = Math.floorMod((int) Math.round(degrees / 90), 4);
        return state -> {
            double relativeX = state.xPos - pivot.getX(), relativeY = state.yPos - pivot.getY();
            // y points down on screen: counter-clockwise is (x, y) -> (x cos + y sin, -x
            // sin + y cos)
            int x = (int) Math.round(pivot.getX() + relativeX * cos + relativeY * sin);
            int y = (int) Math.round(pivot.getY() - relativeX * sin + relativeY * cos);
            CityLayer.Direction rotation = state.rotation;
            for (int i = 0; i < turns; i++)
                rotation = rotation.counterClockwise();
            return new ObjectState(rotation, state.mirrored, state.objectIndex, x, y);
        };
    }

    /**
     * Spreads or packs the houses' positions along the axes of a box turned by
     * {@code degrees} (counter-clockwise on screen) around {@code pivot}. World and
     * view share their orientation, so the view geometry applies as is.
     */
    static UnaryOperator<ObjectState> scaleAround(Point2D pivot, double scaleX, double scaleY, double degrees) {
        if (degrees == 0)
            return scaleAround(pivot, scaleX, scaleY);
        var transform = PathTransformBox.axisScaling(pivot, scaleX, scaleY, degrees);
        return state -> {
            Point2D moved = transform.transform(new Point2D.Double(state.xPos, state.yPos), null);
            return new ObjectState(state.rotation, state.mirrored, state.objectIndex, (int) Math.round(moved.getX()),
                    (int) Math.round(moved.getY()));
        };
    }

    /**
     * Spreads (factor above 1) or packs (below 1) the houses' positions around
     * {@code pivot}; the houses keep their size.
     */
    static UnaryOperator<ObjectState> scaleAround(Point2D pivot, double scaleX, double scaleY) {
        return state -> new ObjectState(state.rotation, state.mirrored, state.objectIndex,
                (int) Math.round(pivot.getX() + (state.xPos - pivot.getX()) * scaleX),
                (int) Math.round(pivot.getY() + (state.yPos - pivot.getY()) * scaleY));
    }

    /** Whether the transform would put two houses on the same spot. */
    static boolean collides(Collection<ObjectState> states, UnaryOperator<ObjectState> transform) {
        var anchors = new HashSet<Point>();
        for (ObjectState state : states) {
            ObjectState moved = transform.apply(state);
            if (!anchors.add(new Point(moved.xPos, moved.yPos)))
                return true;
        }
        return false;
    }

    /** Applies a transform to the selection (if it keeps the houses apart). */
    boolean transformSelection(UnaryOperator<ObjectState> transform) {
        CityLayer layer = getSelectedLayer();
        if (layer == null || selectedStates.isEmpty())
            return false;
        if (collides(selectedStates.values(), transform)) {
            showStatusMessage("Not applied: two houses would land on the same spot");
            return false;
        }
        edit(() -> applyToSelection(layer, transform));
        return true;
    }

    private void refreshLayer(CityLayer layer) {
        if (getViewAsWP() != null)
            getViewAsWP().refreshTilesForLayer(layer, false);
    }

    private void deselect(CityLayer layer) {
        clearSelection();
        applyToUi(new ObjectState(uiState.rotation, uiState.mirrored, uiState.objectIndex, Integer.MAX_VALUE,
                Integer.MAX_VALUE));
        refreshLayer(layer);
    }

    private void deleteSelected() {
        CityLayer layer = getSelectedLayer();
        if (layer == null || selectedStates.isEmpty())
            return;

        for (ObjectState state : selectedStates.values())
            layer.removeDataAt(getDimension(), state.xPos, state.yPos);
        deselect(layer);
    }

    private void copySelection(CityLayer layer) {
        if (layer == null || selectedStates.isEmpty())
            return;

        ObjectState anchor = selectedStates.get(new Point(uiState.xPos, uiState.yPos));
        if (anchor == null)
            anchor = selectedStates.values().iterator().next();

        List<ClipboardEntry> entries = new ArrayList<>(selectedStates.size());
        addClipboardEntry(entries, anchor, anchor);
        for (ObjectState state : selectedStates.values()) {
            if (state.xPos != anchor.xPos || state.yPos != anchor.yPos)
                addClipboardEntry(entries, state, anchor);
        }

        clipboard = entries;
        clipboardLayer = layer;
        showStatusMessage(objectCountMessage(entries.size(), "copied to clipboard"));
    }

    private void addClipboardEntry(List<ClipboardEntry> entries, ObjectState state, ObjectState anchor) {
        long offsetX = (long) state.xPos - anchor.xPos;
        long offsetY = (long) state.yPos - anchor.yPos;
        entries.add(new ClipboardEntry(state.rotation, state.mirrored, state.objectIndex, offsetX, offsetY));
    }

    private void cutSelection(CityLayer layer) {
        if (layer == null || selectedStates.isEmpty())
            return;

        copySelection(layer);
        int count = selectedStates.size();
        deleteSelected();
        showStatusMessage(objectCountMessage(count, "cut to clipboard"));
    }

    private void pasteClipboard(CityLayer layer) {
        if (layer == null || clipboard.isEmpty() || clipboardLayer != layer) {
            if (clipboardLayer != layer)
                clearClipboard();
            showStatusMessage("Clipboard is empty");
            return;
        }

        Point cursor = cursorWorldPosition;
        if (cursor == null) {
            showStatusMessage("Move the cursor over the map to paste");
            return;
        }

        List<ObjectState> newStates = new ArrayList<>(clipboard.size());
        for (ClipboardEntry entry : clipboard) {
            if (entry.objectIndex < 0 || entry.objectIndex >= layer.getObjectList().size()) {
                showStatusMessage("Clipboard cannot be pasted into this layer");
                return;
            }
            long x = cursor.x + entry.offsetX;
            long y = cursor.y + entry.offsetY;
            if (x < Integer.MIN_VALUE || x > Integer.MAX_VALUE || y < Integer.MIN_VALUE || y > Integer.MAX_VALUE) {
                showStatusMessage("Clipboard does not fit at the cursor");
                return;
            }
            newStates.add(new ObjectState(entry.rotation, entry.mirrored, entry.objectIndex, (int) x, (int) y));
        }

        for (ObjectState state : newStates)
            layer.removeDataAt(getDimension(), state.xPos, state.yPos);
        for (ObjectState state : newStates)
            layer.setDataAt(getDimension(), state.xPos, state.yPos, state);

        clearSelection();
        for (ObjectState state : newStates)
            addSelectedState(state);
        uiState = newStates.getFirst();
        applyToUi(uiState);
        refreshLayer(layer);
        showStatusMessage(objectCountMessage(newStates.size(), "pasted from clipboard"));
    }

    private void clearClipboard() {
        clipboard = List.of();
        clipboardLayer = null;
    }

    private static String objectCountMessage(int count, String suffix) {
        return count == 1 ? "1 object " + suffix : count + " objects " + suffix;
    }

    private String selectionStatusMessage() {
        return selectedStates.isEmpty() ? "No objects selected" : objectCountMessage(selectedStates.size(), "selected");
    }

    private void showStatusMessage(String message) {
        if (overlayView != null)
            overlay.showTransientInfoText(message, 3000);
    }

    void setCursorWorldPosition(int x, int y) {
        cursorWorldPosition = new Point(x, y);
    }

    private void trackCursor(Point viewPoint) {
        if (overlayView != null)
            cursorWorldPosition = overlayView.viewToWorld(viewPoint);
    }

    /** The state with the random parts the Schematics menu asks for. */
    private ObjectState randomizeState(ObjectState oldState) {
        ObjectState newState = oldState;
        CityLayer layer = getSelectedLayer();
        if (placementOptions.randomRotate()) {
            newState = setRotation(CityLayer.Direction.fromCompass(random.nextInt(4) * 90), newState);
        }
        if (placementOptions.randomSelect() && layer != null && !layer.getObjectList().isEmpty()) {
            newState = setSelectedObjectIndex(random.nextInt(layer.getObjectList().size()), newState);
        }
        if (placementOptions.randomMirror()) {
            newState = setIsMirrored(random.nextBoolean(), newState);
        }
        return newState;
    }

    /** Which view of a house a card shows large. */
    enum HouseView {
        FRONT("front"), SIDE("side"), TOP("top");

        final String label;

        HouseView(String label) {
            this.label = label;
        }
    }

    /** What a Randomize operation of the Schematics menu changes. */
    enum Randomized {
        ROTATION("Randomize rotation",
                "Turns each selected house to one of the ticked rotations, at random. Positions stay."), MIRRORING(
                        "Randomize mirroring",
                        "Mirrors each selected house or not, at random, among the ticked options. Positions stay."), TYPE(
                                "Randomize type",
                                "Gives each selected house one of the ticked schematics, at random. Position, rotation and mirroring stay.");

        final String title;
        final String description;

        Randomized(String title, String description) {
            this.title = title;
            this.description = description;
        }
    }

    /** The open Randomize panel, or {@code null}. */
    private JPanel operationPanel;
    /**
     * The open Randomize operation's result, previewed on the map, or {@code null}.
     */
    private UnaryOperator<ObjectState> operationPreview;
    private Runnable operationRefresh;

    /**
     * Opens the panel of a Randomize operation (shared with the Path Tool's
     * operations): a checkbox per allowed final state (quarter turns, mirrored or
     * not, the layer's schematics) and a seed; the selection previews the result,
     * Apply commits it, Cancel / Escape / a click on the map discards it.
     */
    void openRandomize(Randomized what) {
        CityLayer layer = getSelectedLayer();
        if (layer == null || selectedStates.isEmpty())
            return;
        closeOperation();
        var houses = new ArrayList<>(selectedStates.values());
        var states = new ArrayList<JCheckBox>();
        var values = new ArrayList<Object>();
        switch (what) {
            case ROTATION -> {
                for (CityLayer.Direction direction : CityLayer.Direction.values()) {
                    states.add(new JCheckBox(direction.toCompass() + "\u00b0", true));
                    values.add(direction);
                }
            }
            case MIRRORING -> {
                states.add(new JCheckBox("Unmirrored", true));
                values.add(false);
                states.add(new JCheckBox("Mirrored", true));
                values.add(true);
            }
            case TYPE -> {
                var objects = layer.getObjectList();
                for (int i = 0; i < objects.size(); i++) {
                    states.add(new JCheckBox(schematicName(objects.get(i), i), true));
                    values.add(i);
                }
            }
        }
        var list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        for (int i = 0; i < states.size(); i++) {
            states.get(i).setName("CityTool.Randomize.Allowed." + i);
            states.get(i).setAlignmentX(Component.LEFT_ALIGNMENT);
            list.add(states.get(i));
        }
        var seed = OverlayStyle.spinner(new JSpinner(new SpinnerNumberModel(1, 0, 99999, 1)));
        seed.setName("CityTool.Randomize.Seed");
        var seedRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        seedRow.add(new JLabel("Seed"));
        seedRow.add(seed);
        var controls = new JPanel(new BorderLayout(0, 8));
        var header = new JPanel(new BorderLayout(8, 0));
        header.add(new JLabel("Allowed results:"), BorderLayout.WEST);
        JButton selectAll = null, deselectAll = null;
        if (what == Randomized.TYPE) {
            // many schematics: tick or untick them all at once
            selectAll = OverlayStyle.smallButton(new JButton("Select all"));
            selectAll.setName("CityTool.Randomize.SelectAll");
            deselectAll = OverlayStyle.smallButton(new JButton("Deselect all"));
            deselectAll.setName("CityTool.Randomize.DeselectAll");
            var all = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
            all.add(selectAll);
            all.add(deselectAll);
            header.add(all, BorderLayout.EAST);
        }
        controls.add(header, BorderLayout.NORTH);
        JComponent allowed = list;
        if (states.size() > 8) {
            // many schematics: a scrolling list
            var scroll = new JScrollPane(list, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            scroll.setBorder(BorderFactory.createEmptyBorder());
            scroll.getViewport().setOpaque(false);
            scroll.setOpaque(false);
            scroll.setPreferredSize(new java.awt.Dimension(list.getPreferredSize().width + 20, 220));
            allowed = scroll;
        }
        controls.add(allowed, BorderLayout.CENTER);
        controls.add(seedRow, BorderLayout.SOUTH);
        var note = new JLabel("Tick at least one");
        note.setName("CityTool.Randomize.Note");
        note.setForeground(OverlayStyle.WARNING);
        note.setVisible(false);
        operationPanel = OperationPanel.create("CityTool", what.title, what.description, controls, note,
                this::closeOperation, this::applyOperation);
        operationRefresh = () -> {
            var allowedValues = new ArrayList<Object>();
            for (int i = 0; i < states.size(); i++)
                if (states.get(i).isSelected())
                    allowedValues.add(values.get(i));
            note.setVisible(allowedValues.isEmpty());
            var apply = OperationPanel.find(operationPanel, "CityTool.OperationApply");
            if (apply != null)
                apply.setEnabled(!allowedValues.isEmpty());
            operationPreview = allowedValues.isEmpty()
                    ? null
                    : randomized(houses, what, allowedValues, ((Number) seed.getValue()).longValue());
            overlay.relayoutOperationPanel();
            overlay.repaint();
        };
        for (JCheckBox state : states)
            state.addActionListener(event -> operationRefresh.run());
        if (selectAll != null) {
            selectAll.addActionListener(event -> {
                states.forEach(state -> state.setSelected(true));
                operationRefresh.run();
            });
            deselectAll.addActionListener(event -> {
                states.forEach(state -> state.setSelected(false));
                operationRefresh.run();
            });
        }
        seed.addChangeListener(event -> operationRefresh.run());
        overlay.showOperationPanel(operationPanel);
        operationRefresh.run();
    }

    /**
     * Each house gets one of the {@code allowed} values of {@code what}, at random
     * (the same {@code seed} gives the same result).
     */
    static UnaryOperator<ObjectState> randomized(List<ObjectState> houses, Randomized what, List<Object> allowed,
            long seed) {
        var random = new Random(seed);
        var results = new java.util.HashMap<Point, ObjectState>();
        for (ObjectState house : houses) {
            Object value = allowed.get(random.nextInt(allowed.size()));
            ObjectState result = switch (what) {
                case ROTATION -> new ObjectState((CityLayer.Direction) value, house.mirrored, house.objectIndex,
                        house.xPos, house.yPos);
                case MIRRORING ->
                    new ObjectState(house.rotation, (Boolean) value, house.objectIndex, house.xPos, house.yPos);
                case TYPE -> new ObjectState(house.rotation, house.mirrored, (Integer) value, house.xPos, house.yPos);
            };
            results.put(new Point(house.xPos, house.yPos), result);
        }
        return state -> results.getOrDefault(new Point(state.xPos, state.yPos), state);
    }

    /** Apply of the open Randomize panel. */
    void applyOperation() {
        var result = operationPreview;
        closeOperation();
        if (result != null)
            transformSelection(result);
    }

    /** Closes the Randomize panel without changing anything. */
    void closeOperation() {
        if (operationPanel != null)
            overlay.hideOperationPanel(operationPanel);
        operationPanel = null;
        operationPreview = null;
        operationRefresh = null;
        overlay.repaint();
    }

    /** The open Randomize panel (exposed for tests), or {@code null}. */
    JPanel operationPanel() {
        return operationPanel;
    }

    private ObjectState setRotation(CityLayer.Direction rotation, ObjectState oldState) {
        if (rotation == oldState.rotation)
            return oldState;
        return new ObjectState(rotation, oldState.mirrored, oldState.objectIndex, oldState.xPos, oldState.yPos);
    }

    private ObjectState setIsMirrored(boolean mirrored, ObjectState oldState) {
        return new ObjectState(oldState.rotation, mirrored, oldState.objectIndex, oldState.xPos, oldState.yPos);
    }

    private ObjectState setSelectedObjectIndex(int index, ObjectState oldState) {
        if (index == oldState.objectIndex)
            return oldState;
        CityLayer layer = getSelectedLayer();
        if (layer == null || index < 0 || index >= layer.getObjectList().size())
            return oldState;

        return new ObjectState(oldState.rotation, oldState.mirrored, index, oldState.xPos, oldState.yPos);
    }

    private boolean syncingHighlight;

    /** The "Highlight colors" overlay switch shows the layer's setting. */
    private void syncHighlightFromLayer() {
        CityLayer layer = getSelectedLayer();
        if (layer == null)
            return;
        syncingHighlight = true;
        try {
            showHighlight.setSelected(layer.isUseHighlightColors());
        } finally {
            syncingHighlight = false;
        }
    }

    /**
     * The layer paints in its color while the overlays and their "Highlight colors"
     * part are on, else its schematics' blocks.
     */
    private void applyHighlightColors() {
        CityLayer layer = getSelectedLayer();
        if (layer == null || syncingHighlight)
            return;
        boolean wanted = showOverlays.isSelected() && showHighlight.isSelected();
        if (layer.isUseHighlightColors() == wanted)
            return;
        layer.setUseHighlightColors(wanted);
        refreshLayer(layer);
    }

    /**
     * Whether an overlay part is drawn: its own switch and the master switch are
     * on.
     */
    private boolean shows(JToggleButton part) {
        return showOverlays.isSelected() && part.isSelected();
    }

    /** The individual overlay switches, in menu order (exposed for tests). */
    List<JToggleButton> overlayParts() {
        return List.of(showHighlight, showOutlines, showFacing);
    }

    /**
     * Sets every overlay part on or off by {@code shown}; switches the master
     * switch on, so the choice is visible.
     */
    private void showOverlayParts(java.util.function.Predicate<JToggleButton> shown) {
        showOverlays.setSelected(true);
        for (JToggleButton part : overlayParts())
            part.setSelected(shown.test(part));
    }

    /**
     * overwrites the current states position
     *
     * @param x
     *            worldPos x
     * @param y
     *            worldPos y
     */
    private ObjectState setCurrentStatePosition(int x, int y, ObjectState oldState) {
        return new ObjectState(oldState.rotation, oldState.mirrored, oldState.objectIndex, x, y);
    }

    private void updatePanel() {
        CityLayer layer = getSelectedLayer();
        if (layer != null && (uiState.objectIndex < 0 || uiState.objectIndex >= layer.getObjectList().size()))
            uiState = new ObjectState(uiState.rotation, uiState.mirrored, 0, uiState.xPos, uiState.yPos);
        overlay.refresh();
    }

    private WorldPainter getViewAsWP() {
        if (getView() instanceof WorldPainter wp)
            return wp;
        return null;
    }

    @Override
    public Paint getPaint() {
        return paint;
    }

    @Override
    public void setPaint(Paint paint) {
        if (this.paint == paint)
            return;
        this.paint = paint;
        paintChanged(paint);
    }

    // ---- mouse gestures on the map ----

    /**
     * The houses as the map shows them now: moved along while a transform is
     * dragged.
     */
    private List<ObjectState> shownSelection() {
        var states = new ArrayList<>(selectedStates.values());
        var shown = gestureTransform != null ? gestureTransform : operationPreview;
        if (shown == null)
            return states;
        return states.stream().map(shown).toList();
    }

    private Point2D.Double toViewExact(Point2D world) {
        // worldToView rounds; go through a far point for the scale
        Point origin = overlayView.worldToView(0, 0);
        Point far = overlayView.worldToView(1024, 1024);
        double scaleX = (far.x - origin.x) / 1024.0, scaleY = (far.y - origin.y) / 1024.0;
        return new Point2D.Double(origin.x + world.getX() * scaleX, origin.y + world.getY() * scaleY);
    }

    private Point2D.Double toWorldExact(Point2D view) {
        Point origin = overlayView.viewToWorld(new Point(0, 0));
        Point far = overlayView.viewToWorld(new Point(1024, 1024));
        double scaleX = (far.x - origin.x) / 1024.0, scaleY = (far.y - origin.y) / 1024.0;
        return new Point2D.Double(origin.x + view.getX() * scaleX, origin.y + view.getY() * scaleY);
    }

    /** View rectangle of a world rectangle. */
    private Rectangle2D.Double toView(Rectangle world) {
        var topLeft = toViewExact(new Point2D.Double(world.x, world.y));
        var bottomRight = toViewExact(new Point2D.Double(world.x + world.width, world.y + world.height));
        return new Rectangle2D.Double(topLeft.x, topLeft.y, bottomRight.x - topLeft.x, bottomRight.y - topLeft.y);
    }

    /**
     * Transform box around the houses' footprints, in the frame of a box turned by
     * {@code degrees} ({@link PathTransformBox#frame} maps it to the view); null
     * without houses.
     */
    private Rectangle2D.Double transformBox(CityLayer layer, List<ObjectState> houses, double degrees) {
        var corners = new ArrayList<Point2D.Double>();
        for (ObjectState house : houses) {
            var bounds = toView(footprint(layer, house));
            corners.add(new Point2D.Double(bounds.getMinX(), bounds.getMinY()));
            corners.add(new Point2D.Double(bounds.getMaxX(), bounds.getMinY()));
            corners.add(new Point2D.Double(bounds.getMaxX(), bounds.getMaxY()));
            corners.add(new Point2D.Double(bounds.getMinX(), bounds.getMaxY()));
        }
        return PathTransformBox.box(corners, IntStream.range(0, corners.size()).boxed().toList(), degrees);
    }

    /** Centre of the selection's footprints (world). */
    private Point2D.Double selectionCentre(CityLayer layer) {
        Rectangle union = null;
        for (ObjectState house : selectedStates.values()) {
            Rectangle bounds = footprint(layer, house);
            union = union == null ? bounds : union.union(bounds);
        }
        return union == null ? null : new Point2D.Double(union.getCenterX(), union.getCenterY());
    }

    /**
     * Pivot for rotating and scaling (world): placed by the user, else the centre
     * of the (possibly turned) box.
     */
    Point2D.Double pivotWorld() {
        return transformEditor.pivot(selectionToken, this::boxCentre);
    }

    /** The pivot as drawn: dragged, or carried along with a move. */
    private Point2D.Double displayedPivot() {
        return transformEditor.displayedPivot(selectionToken, this::boxCentre);
    }

    private Point2D.Double boxCentre() {
        CityLayer layer = getSelectedLayer();
        if (layer == null)
            return null;
        if (overlayView == null || selectedStates.isEmpty())
            return selectionCentre(layer);
        double angle = boxAngle();
        var box = transformBox(layer, new ArrayList<>(selectedStates.values()), angle);
        return toWorldExact(PathTransformBox.frame(angle).transform(PathTransformBox.centre(box), null));
    }

    /**
     * The transform box as drawn: around the houses as shown, turned along while
     * rotating.
     */
    private TransformBoxEditor.ShownBox shownBox(CityLayer layer) {
        double angle = transformEditor.shownAngle(selectionToken);
        var box = transformBox(layer, shownSelection(), angle);
        return box == null ? null : new TransformBoxEditor.ShownBox(PathTransformBox.frame(angle), box);
    }

    private static HitKind hitKindOf(TransformBoxEditor.Handle handle) {
        return switch (handle) {
            case MOVE -> HitKind.MOVE;
            case ROTATE -> HitKind.ROTATE;
            case SCALE -> HitKind.SCALE;
            case PIVOT -> HitKind.PIVOT;
        };
    }

    private static TransformBoxEditor.Handle handleOf(HitKind kind) {
        return switch (kind) {
            case MOVE -> TransformBoxEditor.Handle.MOVE;
            case ROTATE -> TransformBoxEditor.Handle.ROTATE;
            case SCALE -> TransformBoxEditor.Handle.SCALE;
            case PIVOT -> TransformBoxEditor.Handle.PIVOT;
            case HOUSE -> null;
        };
    }

    /** What a press at {@code point} grabs: a transform handle, else a house. */
    Hit hitTest(Point point) {
        CityLayer layer = getSelectedLayer();
        if (overlayView == null || layer == null)
            return null;
        if (showOverlays.isSelected() && !selectedStates.isEmpty()) {
            boolean single = selectedStates.size() < 2;
            var handle = transformEditor.hitTest(point, shownBox(layer), single, true,
                    single ? null : toViewExact(displayedPivot()), HANDLE_HIT_RADIUS * HANDLE_HIT_RADIUS);
            if (handle != null)
                return new Hit(hitKindOf(handle.handle()), handle.axis(), null);
        }
        Point world = overlayView.viewToWorld(point);
        ObjectState house = houseAt(layer, world.x, world.y);
        return house == null ? null : new Hit(HitKind.HOUSE, 0, house);
    }

    private void setHovered(Hit hit) {
        if (Objects.equals(hit, hovered))
            return;
        hovered = hit;
        updateCursor();
        overlay.repaint();
    }

    /**
     * Cursor for the hovered element, a crosshair on empty map in add mode, else
     * the map's own.
     */
    private void updateCursor() {
        if (overlayView == null)
            return;
        Cursor wanted = hovered != null
                ? Cursor.getPredefinedCursor(hovered.kind().cursor)
                : addMode.isSelected() ? Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR) : null;
        if (wanted == null) {
            if (cursorOverridden)
                overlayView.setCursor(cursorBeforeHover);
            cursorOverridden = false;
            return;
        }
        if (!cursorOverridden)
            cursorBeforeHover = overlayView.getCursor();
        cursorOverridden = true;
        overlayView.setCursor(wanted);
    }

    void handlePress(Point point, boolean additive, boolean shift) {
        CityLayer layer = getSelectedLayer();
        if (overlayView == null || layer == null || getDimension() == null)
            return;
        ensureLayerHasUndoManager(layer, getDimension());
        closeOperation(); // clicking the map discards a previewed operation
        cancelGesture();
        Hit hit = hitTest(point);
        if (shift && hit == null)
            return; // Shift + drag on empty map stays WorldPainter's
        gestureStart = new Point(point);
        gestureWorldStart = overlayView.viewToWorld(point);
        gestureHit = hit;
        gestureAdditive = additive;
        gestureDragged = false;
        preciseDrag.start(point);
        if (hit != null && hit.kind() != HitKind.HOUSE)
            transformEditor.start(new TransformBoxEditor.Hit(handleOf(hit.kind()), hit.side()), pivotWorld(),
                    toWorldExact(new Point2D.Double(point.x, point.y)), selectedStates.size() < 2);
        setHovered(hit); // the grabbed element stays highlighted while dragging
        if (hit == null)
            overlay.startDrag(point);
    }

    void handleDrag(Point point) {
        handleDrag(point, false);
    }

    /**
     * @param precise
     *            Shift held: handles and houses follow at a tenth of the mouse's
     *            speed (box selection follows the mouse)
     */
    void handleDrag(Point point, boolean precise) {
        if (gestureStart == null || overlayView == null)
            return;
        boolean wasPrecise = preciseDrag.precise();
        Point2D.Double cursor = preciseDrag.update(point, precise);
        if (gestureStart.distanceSq(point) >= 9)
            gestureDragged = true;
        if (!gestureDragged)
            return;
        if (gestureHit == null) {
            overlay.updateDrag(point);
            return;
        }
        if (precise && !wasPrecise)
            overlay.showTransientInfoText("Precise drag", 1200);
        if (gestureHit.kind() == HitKind.HOUSE && !transformEditor.dragging()) {
            // dragging a house moves the selection (the house joins it first)
            var house = gestureHit.house();
            if (!selectedStates.containsKey(new Point(house.xPos, house.yPos))) {
                if (!gestureAdditive)
                    clearSelection();
                addSelectedState(house);
            }
            transformEditor.start(new TransformBoxEditor.Hit(TransformBoxEditor.Handle.MOVE, 0), pivotWorld(),
                    toWorldExact(new Point2D.Double(gestureStart.x, gestureStart.y)), false);
        }
        // the virtual cursor, in world terms: the editor keeps the transform there
        var readout = transformEditor.update(toWorldExact(cursor), boxAngle(), value -> value);
        gestureHeader = readout == null ? null : readout.label() + ": " + readout.value();
        Point2D.Double pivot = transformEditor.pivotStart();
        gestureTransform = switch (transformEditor.kind()) {
            case MOVE -> moveBy(transformEditor.moveX(), transformEditor.moveY());
            // a group turns by any angle, each house facing the nearest quarter turn; a
            // single house turns in quarter turns (the editor snaps them)
            case ROTATE -> selectedStates.size() > 1
                    ? rotateAround(pivot, transformEditor.degrees())
                    : rotateAround(new Point((int) Math.round(pivot.x), (int) Math.round(pivot.y)),
                            (int) Math.round(transformEditor.degrees() / 90));
            case SCALE -> scaleAround(pivot, transformEditor.scaleX(), transformEditor.scaleY(), boxAngle());
            case PIVOT -> null;
        };
        overlay.repaint();
    }

    void handleRelease(Point point) {
        handleRelease(point, false);
    }

    void handleRelease(Point point, boolean precise) {
        if (gestureStart == null)
            return;
        CityLayer layer = getSelectedLayer();
        try {
            handleDrag(point, precise);
            if (layer == null)
                return;
            if (gestureHit == null) {
                if (gestureDragged) {
                    Rectangle bounds = overlay.getDragBounds();
                    if (bounds != null)
                        selectWithinBox(layer, bounds, gestureAdditive);
                } else {
                    Point world = overlayView.viewToWorld(gestureStart);
                    edit(() -> handleClick(world.x, world.y, gestureAdditive));
                }
                return;
            }
            switch (gestureHit.kind()) {
                case HOUSE -> {
                    if (!gestureDragged) {
                        var house = gestureHit.house();
                        if (gestureAdditive)
                            toggleSelected(layer, house);
                        else {
                            selectOnly(house);
                            showStatusMessage(selectionStatusMessage());
                        }
                        return;
                    }
                    finishTransform();
                }
                case MOVE, ROTATE, SCALE, PIVOT -> finishTransform();
            }
        } finally {
            cancelGesture();
            setHovered(cursorOverMap ? hitTest(point) : null);
            overlay.repaint();
        }
    }

    /**
     * Applies the dragged transform (if it keeps the houses apart) and lets the
     * editor carry the pivot along / turn the box.
     */
    private void finishTransform() {
        boolean applied = gestureDragged && gestureTransform != null && transformSelection(gestureTransform);
        if (transformEditor.finish(selectionToken, gestureDragged, applied))
            showStatusMessage("Pivot reset to the centre");
    }

    private void cancelGesture() {
        gestureStart = null;
        gestureWorldStart = null;
        gestureHit = null;
        gestureDragged = false;
        gestureTransform = null;
        gestureHeader = null;
        transformEditor.cancel();
        overlay.endDrag();
        overlay.repaint();
    }

    // ---- menus ----

    private static JMenuItem item(String text, String name, boolean enabled, String disabledHint, Runnable action) {
        var item = new JMenuItem(text);
        item.setName(name);
        item.setEnabled(enabled);
        if (!enabled)
            item.setToolTipText(disabledHint);
        item.addActionListener(event -> action.run());
        return item;
    }

    private static JMenuItem heading(String text) {
        var item = new JMenuItem(text);
        item.setEnabled(false);
        item.setFont(OverlayStyle.font(Font.BOLD, 12f));
        return item;
    }

    /** A switch in a menu: the lavender dot shows it is on. */
    private static JRadioButtonMenuItem toggleItem(String text, String name, boolean on, String tooltip,
            java.util.function.Consumer<Boolean> changed) {
        var item = new JRadioButtonMenuItem(text, on);
        item.setName(name);
        item.setToolTipText(tooltip);
        item.addActionListener(event -> changed.accept(item.isSelected()));
        return item;
    }

    static String schematicName(WPObject object, int index) {
        String name = object == null ? null : object.getName();
        return name == null || name.isBlank() ? "Schematic " + (index + 1) : name;
    }

    /**
     * "Schematics" menu: rotate / mirror / delete, randomizing now and for new
     * houses (exposed for tests). The type is picked in the grid the type card
     * opens.
     */
    JPopupMenu schematicsMenu() {
        var menu = new JPopupMenu();
        CityLayer layer = getSelectedLayer();
        boolean any = !selectedStates.isEmpty();
        String needSelection = "Select houses first";
        if (layer == null) {
            menu.add(item("Choose a City Layer in the Layer menu", "CityTool.NoLayer", false, null, () -> {
            }));
            return menu;
        }
        menu.add(item("Choose schematic...", "CityTool.ChooseSchematic", true, null, () -> openTypeGrid()));
        menu.addSeparator();
        menu.add(item("Rotate 90° (C)", "CityTool.Rotate", any, needSelection,
                () -> edit(() -> rotateSelection(layer))));
        menu.add(item("Mirror (X)", "CityTool.Mirror", any, needSelection, () -> edit(() -> mirrorSelection(layer))));
        menu.add(item("Delete (Del)", "CityTool.Delete", any, needSelection, () -> edit(this::deleteSelected)));
        menu.addSeparator();
        menu.add(item("Randomize rotation...", "CityTool.RandomizeRotation", any, needSelection,
                () -> openRandomize(Randomized.ROTATION)));
        menu.add(item("Randomize mirroring...", "CityTool.RandomizeMirroring", any, needSelection,
                () -> openRandomize(Randomized.MIRRORING)));
        menu.add(item("Randomize type...", "CityTool.RandomizeType", any, needSelection,
                () -> openRandomize(Randomized.TYPE)));
        return menu;
    }

    /**
     * "Layer" menu: the tutorial layer, WorldPainter's layer import, the
     * dimension's City Layers alphabetically (exposed for tests).
     */
    JPopupMenu layerMenu() {
        var menu = new JPopupMenu();
        Dimension dimension = getView() == null ? null : getDimension();
        CityLayer current = getSelectedLayer();
        boolean tutorial = CityLayerLibrary.hasTutorialLayer();
        menu.add(item("Tutorial layer", "CityTool.TutorialLayer", tutorial && dimension != null,
                "The tutorial layer is not bundled yet", this::useTutorialLayer));
        menu.add(item("Import from file...", "CityTool.ImportLayer", dimension != null, "Open a world first",
                this::importLayerFromFile));
        menu.addSeparator();
        var layers = CityLayerLibrary.cityLayers(dimension);
        if (layers.isEmpty())
            menu.add(item("No City Layers in this dimension", "CityTool.NoLayers", false, null, () -> {
            }));
        var group = new ButtonGroup();
        for (CityLayer layer : layers) {
            var choice = new JRadioButtonMenuItem(layer.getName(), layer == current);
            choice.setName("CityTool.Layer." + layer.getName());
            choice.addActionListener(event -> selectLayer(layer));
            group.add(choice);
            menu.add(choice);
        }
        menu.addSeparator();
        menu.add(item("Help...", "CityTool.Help", true, null,
                () -> new HelpDialog(null, HELP_TITLE, HELP_EXPLANATION, HELP_ITEMS).setVisible(true)));
        return menu;
    }

    /**
     * Makes {@code layer} WorldPainter's paint, and the tool's. WorldPainter only
     * hands a paint to the tool when a layer button becomes selected: if its button
     * already is (WorldPainter and the tool disagree, e.g. after another world was
     * opened), nothing would happen, so the tool then takes the layer itself.
     */
    void selectLayer(CityLayer layer) {
        CityLayerLibrary.select(layer, this::setPaint);
        if (!painting(layer))
            setPaint(org.pepsoft.worldpainter.painting.PaintFactory.createLayerPaint(layer));
        if (getSelectedLayer() != layer)
            System.err.println("City Tool: could not select layer '" + layer.getName() + "' (paint "
                    + (painting(layer) ? "set" : "not set") + ", in this world: "
                    + CityLayerLibrary.inWorld(layer, getView() == null ? null : getDimension()) + ")");
    }

    /** Whether the tool's paint is {@code layer} (the very same layer). */
    private boolean painting(CityLayer layer) {
        return getPaint() instanceof LayerPaint layerPaint && layerPaint.getLayer() == layer;
    }

    /**
     * Selects a layer that was just added to the world. WorldPainter may build its
     * button only after the import returns, so selecting right away can find
     * nothing: then again once the import has settled, and if WorldPainter still
     * does not hand it over, the tool takes the layer's paint itself.
     */
    void selectNewLayer(CityLayer layer) {
        selectLayer(layer);
        if (painting(layer))
            return;
        SwingUtilities.invokeLater(() -> {
            selectLayer(layer);
            if (!painting(layer))
                setPaint(org.pepsoft.worldpainter.painting.PaintFactory.createLayerPaint(layer));
        });
    }

    private void useTutorialLayer() {
        try {
            CityLayer layer = CityLayerLibrary.addTutorialLayer(getDimension());
            if (layer != null)
                selectNewLayer(layer);
        } catch (Exception ex) {
            GlobalActionPanel.ErrorPopUp(ex);
        }
    }

    private void importLayerFromFile() {
        try {
            CityLayer layer = CityLayerLibrary.importFromFile(getDimension());
            if (layer != null)
                selectNewLayer(layer);
        } catch (Exception ex) {
            GlobalActionPanel.ErrorPopUp(ex);
        }
    }

    /**
     * Turns the selected houses in place, or the next house without a selection, by
     * {@code quarterTurns} counter-clockwise on screen (negative: clockwise).
     */
    void turnNext(int quarterTurns) {
        CityLayer layer = getSelectedLayer();
        if (layer == null)
            return;
        UnaryOperator<ObjectState> turn = state -> {
            CityLayer.Direction rotation = state.rotation;
            for (int i = 0; i < Math.floorMod(quarterTurns, 4); i++)
                rotation = rotation.counterClockwise();
            return setRotation(rotation, state);
        };
        if (!setsUpNextHouse())
            edit(() -> applyToSelection(layer, turn));
        else
            applyToUi(turn.apply(uiState));
        overlay.repaint();
    }

    /** The type of the next house to place; the selected houses keep theirs. */
    void setNextType(int index) {
        if (getSelectedLayer() != null)
            applyToUi(setSelectedObjectIndex(index, uiState));
    }

    /** Apply type (card): the next house's type for every selected house. */
    void applyTypeToSelection() {
        CityLayer layer = getSelectedLayer();
        if (layer == null || selectedStates.isEmpty())
            return;
        int index = uiState.objectIndex;
        edit(() -> applyToSelection(layer, state -> setSelectedObjectIndex(index, state)));
        showStatusMessage(objectCountMessage(selectedStates.size(), "got the type"));
    }

    /** Mirrors the selected houses, or the next house without a selection. */
    void mirrorNext() {
        CityLayer layer = getSelectedLayer();
        if (layer == null)
            return;
        if (!setsUpNextHouse())
            edit(() -> mirrorSelection(layer));
        else
            applyToUi(setIsMirrored(!uiState.mirrored, uiState));
        overlay.repaint();
    }

    /**
     * How the next house will be placed: type, rotation, mirroring (exposed for
     * tests).
     */
    ObjectState nextHouse() {
        return uiState;
    }

    /**
     * Turning and mirroring set up the next house in add mode (a house just placed
     * is selected, but the next one is what is being prepared) or with nothing
     * selected; otherwise they change the selected houses.
     */
    private boolean setsUpNextHouse() {
        return addMode.isSelected() || selectedStates.isEmpty();
    }

    /** Switches between "Add" and "Add random" (and turns add mode on). */
    void setAddRandom(boolean random) {
        addRandom = random;
        overlay.refresh();
        if (addMode.isSelected())
            overlay.repaint();
        else
            addMode.setSelected(true);
        if (random && randomPool.isEmpty())
            showStatusMessage("Add random: tick the schematics to pick from in the picker (click the schematic card)");
    }

    boolean addRandom() {
        return addRandom;
    }

    /** Ticks or unticks a schematic for "Add random". */
    void toggleRandomPool(int index) {
        if (!randomPool.remove(index))
            randomPool.add(index);
        overlay.refresh();
    }

    /** The schematics ticked for "Add random" (exposed for tests). */
    Set<Integer> randomPool() {
        return Set.copyOf(randomPool);
    }

    /** The picker shows checkboxes: add mode with "Add random". */
    boolean pickingRandomPool() {
        return addMode.isSelected() && addRandom;
    }

    /**
     * "Add" dropdown: plain add or random add, and what new houses get at random
     * (exposed for tests).
     */
    JPopupMenu addMenu() {
        var menu = new JPopupMenu();
        var group = new ButtonGroup();
        var plain = new JRadioButtonMenuItem("Add", !addRandom);
        plain.setName("CityTool.Add.Plain");
        plain.setToolTipText("Each click places the current schematic");
        plain.addActionListener(event -> setAddRandom(false));
        var random = new JRadioButtonMenuItem("Add random", addRandom);
        random.setName("CityTool.Add.Random");
        random.setToolTipText("Each click places a random pick of the schematics ticked in the picker");
        random.addActionListener(event -> setAddRandom(true));
        group.add(plain);
        group.add(random);
        menu.add(plain);
        menu.add(random);
        menu.addSeparator();
        menu.add(heading("New houses and Q get a random"));
        var options = placementOptions;
        menu.add(toggleItem("Rotation", "CityTool.RandomRotation", options.randomRotate(),
                "New houses (and Q) get a random rotation",
                on -> setPlacementOptions(new PlacementOptions(on, options.randomSelect(), options.randomMirror()))));
        menu.add(toggleItem("Mirroring", "CityTool.RandomMirroring", options.randomMirror(),
                "New houses (and Q) are mirrored at random",
                on -> setPlacementOptions(new PlacementOptions(options.randomRotate(), options.randomSelect(), on))));
        menu.add(toggleItem("Type", "CityTool.RandomType", options.randomSelect(),
                "New houses (and Q) get a random schematic (Add random picks from the ticked ones)",
                on -> setPlacementOptions(new PlacementOptions(options.randomRotate(), on, options.randomMirror()))));
        return menu;
    }

    /** The add-mode switch (exposed for tests). */
    JToggleButton addModeToggle() {
        return addMode;
    }

    /**
     * Opens the schematic picker over the whole viewport (what clicking the type
     * card does): the pick is the next house's type.
     */
    void openTypeGrid() {
        overlay.openTypeGrid(false);
    }

    /**
     * Change type (card): opens the picker; the schematic picked there goes to the
     * selected houses (closing it without a pick changes nothing).
     */
    void changeTypeOfSelection() {
        if (getSelectedLayer() != null && !selectedStates.isEmpty())
            overlay.openTypeGrid(true);
    }

    boolean typeGridOpen() {
        return overlay.typeGridOpen();
    }

    /** The picker's line breaking of names (exposed for tests). */
    static List<String> wrapForTest(String text, FontMetrics metrics, int width) {
        return TypeGrid.wrap(text, metrics, width);
    }

    /**
     * The schematics the picker shows after search and filters (exposed for tests).
     */
    List<Integer> typeGridVisible() {
        return List.copyOf(overlay.typeGrid.visible);
    }

    /** What the type card shows for the next house (exposed for tests). */
    TypeGrid.TileContent cardContentForTest() {
        return overlay.typeCard.content(getSelectedLayer());
    }

    /** The type card (exposed for tests). */
    JComponent typeCardForTest() {
        return overlay.typeCard;
    }

    /**
     * One view of the next house, enlarged over the map: a dark backdrop and a
     * large square with the view and its dimension lines. A click anywhere or Esc
     * closes it.
     */
    private class ViewLightbox extends JComponent
    {
        private TypeGrid.TileContent content;
        private String title = "";

        ViewLightbox() {
            setOpaque(false);
            setVisible(false);
            setName("CityTool.EnlargedView");
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent event) {
                    overlay.closeLightbox();
                }
            });
        }

        void show(TypeGrid.TileContent content, String title) {
            this.content = content;
            this.title = title;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            if (content == null)
                return;
            var grid = overlay.typeGrid;
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.setColor(grid.bright() ? new Color(232, 232, 232, 230) : new Color(22, 22, 22, 220));
                g.fillRect(0, 0, getWidth(), getHeight());
                int size = Math.max(160, Math.min(getWidth(), getHeight()) - 140);
                var area = new Rectangle((getWidth() - size) / 2, (getHeight() - size) / 2 + 12, size, size);
                OverlayStyle.paintSurface(g, area.x - 12, area.y - 44, area.width + 24, area.height + 56,
                        grid.tileFill(false, false));
                g.setFont(OverlayStyle.font(Font.BOLD, 16f));
                g.setColor(grid.text());
                g.drawString(title + " \u00b7 " + content.view().label, area.x, area.y - 18);
                g.setFont(OverlayStyle.font(Font.PLAIN, 12f));
                g.setColor(grid.muted());
                String hint = "Esc or a click closes";
                g.drawString(hint, area.x + area.width - g.getFontMetrics().stringWidth(hint), area.y - 18);
                grid.paintMainView(g, area, content, content.view());
            } finally {
                g.dispose();
            }
        }
    }

    /** Whether the enlarged view is open (exposed for tests). */
    boolean enlargedViewOpen() {
        return overlay.lightboxOpen();
    }

    /** Clicks the type card at {@code point} (exposed for tests). */
    void clickTypeCard(Point point) {
        overlay.typeCard.clicked(point);
    }

    /** The picker itself (exposed for tests). */
    JComponent typeGridComponent() {
        return overlay.typeGrid;
    }

    /** The picker's bright-mode switch (exposed for tests). */
    JToggleButton typeGridBrightToggle() {
        return overlay.typeGrid.brightToggle;
    }

    /**
     * Picks a schematic in the open picker (exposed for tests): like clicking its
     * tile.
     */
    void pickTypeInGrid(int index) {
        overlay.typeGrid.pick(index);
    }

    /** The overlays dropdown next to the eye button (exposed for tests). */
    JPopupMenu overlaysMenu() {
        return overlay.overlaysMenu();
    }

    JToggleButton overlaysSwitch() {
        return showOverlays;
    }

    // ---- the floating HUD ----

    /**
     * Header text at the top centre: the running drag, add mode, or what to do
     * first.
     */
    String viewportHeader() {
        if (gestureHeader != null)
            return gestureHeader;
        CityLayer layer = getSelectedLayer();
        if (layer == null)
            return "Choose a City Layer in the Layer menu";
        if (layer.getObjectList().isEmpty())
            return "This City Layer has no schematics: edit the layer to add some";
        if (addMode.isSelected() && addRandom)
            return randomPool.isEmpty()
                    ? "Add random: tick schematics in the picker (click the schematic card)"
                    : "Click on the map to place a random pick of " + randomPool.size()
                            + (randomPool.size() == 1 ? " schematic" : " schematics");
        if (addMode.isSelected())
            return "Click on the map to place: "
                    + schematicName(layer.getObjectList().get(uiState.objectIndex), uiState.objectIndex);
        return null;
    }

    private class CityOverlay extends MapToolOverlay
    {
        private final PathTool.DropdownButton schematicsButton = new PathTool.DropdownButton("Schematics");
        private final PathTool.DropdownButton layerButton = new PathTool.DropdownButton("Layer");
        private final JToggleButton overlaysButton = new JToggleButton(LucideIcons.icon("eye", 16, OverlayStyle.TEXT));
        private final JButton overlaysMenuButton = new JButton(PathTool.DROPDOWN_ARROW);
        private final JButton addMenuButton = new JButton(PathTool.DROPDOWN_ARROW);
        private final JToggleButton addButton = new JToggleButton("Add",
                LucideIcons.icon("plus", 16, OverlayStyle.TEXT));
        private final TypeCard typeCard = new TypeCard();
        private final TypeGrid typeGrid = new TypeGrid();

        CityOverlay() {
            super(loadOverlayIcon());
            schematicsButton.setName("CityTool.SchematicsMenu");
            schematicsButton.setToolTipText("Rotation, mirroring and randomizing of the houses");
            schematicsButton.addActionListener(event -> {
                if (schematicsButton.isShowing())
                    OverlayStyle.popup(schematicsMenu()).show(schematicsButton, 0, schematicsButton.getHeight());
            });
            add(schematicsButton);
            layerButton.setName("CityTool.LayerMenu");
            layerButton.setToolTipText("Choose, import or load the tutorial City Layer");
            layerButton.addActionListener(event -> {
                if (layerButton.isShowing())
                    OverlayStyle.popup(layerMenu()).show(layerButton, 0, layerButton.getHeight());
            });
            add(layerButton);
            overlaysButton.setName("CityTool.OverlaysButton");
            overlaysButton.setModel(showOverlays.getModel());
            overlaysButton.setFocusable(false);
            overlaysButton
                    .setToolTipText("Show overlays (highlight colors, house outlines, selected houses, transform box)");
            add(overlaysButton);
            overlaysMenuButton.setName("CityTool.OverlaysMenu");
            overlaysMenuButton.setFocusable(false);
            overlaysMenuButton.setToolTipText("Choose which overlays to show");
            overlaysMenuButton.addActionListener(event -> {
                if (overlaysMenuButton.isShowing())
                    OverlayStyle.popup(overlaysMenu()).show(overlaysMenuButton, 0, overlaysMenuButton.getHeight());
            });
            add(overlaysMenuButton);
            addButton.setName("CityTool.AddButton");
            addButton.setModel(addMode.getModel());
            addButton.setFocusable(false);
            addButton.setToolTipText("Add mode: left click on empty map places a house. Ctrl+click always places.");
            add(addButton);
            addMenuButton.setName("CityTool.AddMenu");
            addMenuButton.setFocusable(false);
            addMenuButton.setToolTipText("Add or Add random (a random pick of the schematics ticked in the picker)");
            addMenuButton.addActionListener(event -> {
                if (addMenuButton.isShowing())
                    OverlayStyle.popup(addMenu()).show(addMenuButton, 0, addMenuButton.getHeight());
            });
            add(addMenuButton);
            for (AbstractButton control : List.of(schematicsButton, layerButton, overlaysButton, overlaysMenuButton,
                    addButton, addMenuButton))
                OverlayStyle.button(control);
            overlaysMenuButton.setBorder(new EmptyBorder(5, 6, 5, 6));
            addMenuButton.setBorder(new EmptyBorder(5, 6, 5, 6));
            overlaysButton.setSelectedIcon(LucideIcons.icon("eye", 16, OverlayStyle.ON_ACCENT));
            addButton.setSelectedIcon(LucideIcons.icon("plus", 16, OverlayStyle.ON_ACCENT));
            typeCard.setName("CityTool.TypeCard");
            typeCard.setToolTipText(
                    "The next house: click a small view to show it large, the large view to enlarge it, the name to choose another schematic");
            typeCard.addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent event) {
                    typeCard.hovered(event.getPoint());
                }
            });
            typeCard.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseExited(MouseEvent event) {
                    typeCard.hovered(null);
                }
            });
            typeCard.addMouseListener(new MouseAdapter() {
                // press and release on the card (mouseClicked is lost when the mouse moves
                // a pixel in between)
                private boolean pressed;

                @Override
                public void mousePressed(MouseEvent event) {
                    pressed = SwingUtilities.isLeftMouseButton(event);
                }

                @Override
                public void mouseReleased(MouseEvent event) {
                    if (pressed && SwingUtilities.isLeftMouseButton(event) && typeCard.contains(event.getPoint()))
                        typeCard.clicked(event.getPoint());
                    pressed = false;
                }
            });
            add(typeCard);
            typeGrid.setName("CityTool.TypeGrid");
            typeGrid.setVisible(false);
            add(typeGrid);
            setFont(OverlayStyle.font(Font.PLAIN, 14f));
        }

        /**
         * One row per overlay part: its toggle (sharing the switch's model), "Only" and
         * "All but"; "All" in the title row. Plain buttons, not menu items, so the
         * popup stays open while clicking (as in the Path Tool).
         */
        JPopupMenu overlaysMenu() {
            var menu = new JPopupMenu("Overlays");
            var rows = new JPanel(new GridBagLayout());
            rows.setOpaque(false);
            rows.setBorder(new EmptyBorder(4, 6, 4, 6));
            var cell = new GridBagConstraints();
            cell.insets = new Insets(2, 2, 2, 2);
            cell.fill = GridBagConstraints.HORIZONTAL;
            var title = new JLabel("Overlays");
            title.setFont(title.getFont().deriveFont(Font.BOLD));
            cell.gridy = 0;
            cell.gridx = 0;
            cell.gridwidth = 2;
            rows.add(title, cell);
            var all = overlayMenuButton("All", "CityTool.Overlays.All", "Show all overlays");
            all.addActionListener(event -> showOverlayParts(part -> true));
            cell.gridx = 2;
            cell.gridwidth = 1;
            rows.add(all, cell);
            for (JToggleButton part : overlayParts()) {
                cell.gridy++;
                var toggle = new JToggleButton(part.getText());
                toggle.setModel(part.getModel());
                toggle.setName(part.getName());
                toggle.setFocusable(false);
                toggle.setToolTipText(part == showHighlight
                        ? "Paint the layer in its color instead of the schematics' blocks"
                        : part == showOutlines
                                ? "Outline every house of the layer"
                                : "A \">\" on the front edge of each house outline (which way it faces)");
                cell.gridx = 0;
                rows.add(toggle, cell);
                var only = overlayMenuButton("Only", part.getName() + ".Only", "Show only this");
                only.addActionListener(event -> showOverlayParts(other -> other == part));
                cell.gridx = 1;
                rows.add(only, cell);
                var allBut = overlayMenuButton("All but", part.getName() + ".AllBut", "Show all but this");
                allBut.addActionListener(event -> showOverlayParts(other -> other != part));
                cell.gridx = 2;
                rows.add(allBut, cell);
            }
            menu.add(rows);
            return menu;
        }

        private JButton overlayMenuButton(String text, String name, String tooltip) {
            var button = new JButton(text);
            button.setName(name);
            button.setFocusable(false);
            button.setToolTipText(tooltip);
            return button;
        }

        void openTypeGrid(boolean forSelection) {
            CityLayer layer = getSelectedLayer();
            if (layer == null || layer.getObjectList().isEmpty())
                return;
            typeGrid.forSelection = forSelection;
            typeGrid.open(layer);
            typeGrid.setBounds(0, 0, getWidth(), getHeight());
            setComponentZOrder(typeGrid, 0);
            typeGrid.setVisible(true);
            repaint();
        }

        void closeTypeGrid() {
            if (!typeGrid.isVisible())
                return;
            typeGrid.setVisible(false);
            repaint();
        }

        boolean typeGridOpen() {
            return typeGrid.isVisible();
        }

        /** The card's large view, enlarged over the map (click or Esc closes it). */
        private final ViewLightbox lightbox = new ViewLightbox();

        void enlarge(TypeGrid.TileContent content, String title) {
            lightbox.show(content, title);
            lightbox.setBounds(0, 0, getWidth(), getHeight());
            if (lightbox.getParent() == null)
                add(lightbox);
            setComponentZOrder(lightbox, 0);
            lightbox.setVisible(true);
            repaint();
        }

        boolean lightboxOpen() {
            return lightbox.isVisible();
        }

        void closeLightbox() {
            if (lightbox.isVisible()) {
                lightbox.setVisible(false);
                repaint();
            }
        }

        /** Buttons and card follow the chosen layer and type. */
        void refresh() {
            CityLayer layer = getSelectedLayer();
            schematicsButton.setEnabled(layer != null);
            typeCard.setVisible(layer != null && !layer.getObjectList().isEmpty());
            typeCard.repaint();
            layoutToolbar();
        }

        private void layoutToolbar() {
            int height = schematicsButton.getPreferredSize().height;
            // two characters in from the left: WorldPainter draws an icon in the view's
            // top left corner
            var metrics = schematicsButton.getFontMetrics(schematicsButton.getFont());
            int left = 8 + 2 * metrics.charWidth('M');
            schematicsButton.setBounds(left, 8, schematicsButton.getPreferredSize().width, height);
            layerButton.setBounds(schematicsButton.getX() + schematicsButton.getWidth() + 4, 8,
                    layerButton.getPreferredSize().width, height);
            int x = layerButton.getX() + layerButton.getWidth() + 8;
            int toggleWidth = overlaysButton.getPreferredSize().width;
            overlaysButton.setBounds(x, 8, toggleWidth, height);
            overlaysMenuButton.setBounds(x + toggleWidth, 8, overlaysMenuButton.getPreferredSize().width, height);
            x = overlaysMenuButton.getX() + overlaysMenuButton.getWidth() + 8;
            addButton.setText(addRandom ? "Add random" : "Add");
            addButton.setBounds(x, 8, addButton.getPreferredSize().width, height);
            addMenuButton.setBounds(addButton.getX() + addButton.getWidth(), 8, addMenuButton.getPreferredSize().width,
                    height);
            var card = typeCard.getPreferredSize();
            typeCard.setBounds(8, toolbarBottom() + 8, card.width, card.height);
            layoutOperationPanel();
            if (typeGrid.isVisible())
                typeGrid.setBounds(0, 0, getWidth(), getHeight());
            for (PathTool.DropdownButton dropdown : List.of(schematicsButton, layerButton))
                dropdown.validate();
            repaint();
        }

        int toolbarBottom() {
            return schematicsButton.getY() + schematicsButton.getHeight();
        }

        /** Operation panels go below the type card (or the toolbar without one). */
        @Override
        protected int operationPanelTop() {
            return typeCard.isVisible() ? typeCard.getY() + typeCard.getHeight() + 8 : toolbarBottom() + 8;
        }

        @Override
        public void setBounds(int x, int y, int width, int height) {
            super.setBounds(x, y, width, height);
            layoutToolbar();
        }

        /** Mouse-transparent like the base overlay, except over the HUD controls. */
        @Override
        public boolean contains(int x, int y) {
            if (typeGrid.isVisible() || lightbox.isVisible())
                return true; // the picker / the enlarged view covers the whole viewport
            if (operationPanelContains(x, y))
                return true;
            for (Component control : List.of(schematicsButton, layerButton, overlaysButton, overlaysMenuButton,
                    addButton, addMenuButton, typeCard))
                if (control.isVisible() && control.getBounds().contains(x, y))
                    return true;
            return false;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (overlayView == null)
                return;
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                CityLayer layer = getSelectedLayer();
                if (layer != null && showOverlays.isSelected())
                    paintHouses(g, layer);
                if (layer != null)
                    paintNextHouse(g, layer);
                String header = viewportHeader();
                if (header != null)
                    paintHeader(g, header);
            } finally {
                g.dispose();
            }
        }

        private void paintHouses(Graphics2D g, CityLayer layer) {
            if (shows(showOutlines))
                paintOutlines(g, layer);
            if (hovered != null && hovered.kind() == HitKind.HOUSE
                    && !selectedStates.containsKey(new Point(hovered.house().xPos, hovered.house().yPos))) {
                g.setColor(OverlayStyle.TEXT);
                g.setStroke(new BasicStroke(1.5f));
                drawOutline(g, toView(footprint(layer, hovered.house())), hovered.house());
            }
            if (selectedStates.isEmpty())
                return;
            if (gestureTransform != null || operationPreview != null) {
                // where the houses were: dashed, quiet
                g.setColor(OverlayStyle.TEXT_MUTED);
                g.setStroke(
                        new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10, new float[]{4, 3}, 0));
                for (ObjectState house : selectedStates.values())
                    g.draw(toView(footprint(layer, house)));
            }
            var shown = shownSelection();
            g.setColor(OverlayStyle.MAP_SELECTION);
            g.setStroke(new BasicStroke(2f));
            for (ObjectState house : shown)
                drawOutline(g, toView(footprint(layer, house)), house);
            paintTransformBox(g, layer);
        }

        /**
         * Add mode: the house a click would place, at the mouse, as a preview (amber,
         * dashed) with its facing, so its rotation can be set before placing it. Not
         * for Add random (the type is picked on the click) or over another house.
         */
        private void paintNextHouse(Graphics2D g, CityLayer layer) {
            Point cursor = cursorWorldPosition;
            if (!addMode.isSelected() || addRandom || cursor == null || !cursorOverMap || gestureStart != null
                    || hovered != null || typeGrid.isVisible() || layer.getObjectList().isEmpty())
                return;
            var next = new ObjectState(uiState.rotation, uiState.mirrored, uiState.objectIndex, cursor.x, cursor.y);
            var bounds = toView(footprint(layer, next));
            g.setColor(OverlayStyle.PREVIEW);
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10, new float[]{5, 3}, 0));
            g.draw(bounds);
            var arrow = facingArrow(bounds, next.rotation);
            if (arrow != null) {
                g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(arrow);
            }
        }

        /**
         * A thin outline around every house of the layer in view (selected ones get
         * their own outline on top).
         */
        private void paintOutlines(Graphics2D g, CityLayer layer) {
            var visible = new Rectangle(0, 0, getWidth(), getHeight());
            g.setColor(OUTLINE_COLOR);
            g.setStroke(new BasicStroke(1f));
            for (ObjectState house : layer.getAllObjectStates()) {
                var bounds = toView(footprint(layer, house));
                if (bounds.intersects(visible))
                    drawOutline(g, bounds, house);
            }
        }

        /**
         * A house's outline with the arrow of its facing (see {@link #facingArrow}).
         */
        private void drawOutline(Graphics2D g, Rectangle2D.Double bounds, ObjectState house) {
            g.draw(bounds);
            var arrow = shows(showFacing) ? facingArrow(bounds, house.rotation) : null;
            if (arrow != null) {
                var stroke = g.getStroke();
                g.setStroke(new BasicStroke(Math.max(1.5f, ((BasicStroke) stroke).getLineWidth()),
                        BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(arrow);
                g.setStroke(stroke);
            }
        }

        /**
         * Transform box around the selection: rotate handle above, move handle left,
         * scale handles on the corner and edges and the pivot (scale and pivot only for
         * several houses: one house has nothing to spread).
         */
        private void paintTransformBox(Graphics2D g, CityLayer layer) {
            boolean single = selectedStates.size() < 2;
            var hoveredHandle = hovered == null || hovered.kind() == HitKind.HOUSE
                    ? null
                    : new TransformBoxEditor.Hit(handleOf(hovered.kind()), hovered.side());
            TransformBoxEditor.paint(g, shownBox(layer), single, true, single ? null : toViewExact(displayedPivot()),
                    hoveredHandle);
        }

        private boolean isHovered(HitKind kind, int side) {
            return hovered != null && hovered.kind() == kind && hovered.side() == side;
        }

        /** Blender-like header box at the top centre, below the toolbar. */
        private void paintHeader(Graphics2D g, String text) {
            g.setFont(OverlayStyle.font(Font.BOLD, 16.5f));
            FontMetrics metrics = g.getFontMetrics();
            int padding = 8;
            int width = metrics.stringWidth(text) + 2 * padding, height = metrics.getHeight() + padding;
            int cardRight = typeCard.isVisible() ? typeCard.getX() + typeCard.getWidth() : 0;
            int x = Math.max((getWidth() - width) / 2, Math.max(cardRight, operationPanelRight()) + 8);
            int y = toolbarBottom() + 8;
            OverlayStyle.paintSurface(g, x, y, width, height, OverlayStyle.SURFACE);
            g.setColor(OverlayStyle.TEXT);
            g.drawString(text, x + padding, y + padding / 2 + metrics.getAscent());
        }
    }

    /**
     * The current type (selected house or the next one): thumbnail and name; opens
     * the Schematics menu.
     */
    private class TypeCard extends JComponent
    {
        /** Height of the button row under the texts (px). */
        private static final int BUTTON_ROW = 30;
        private final List<JButton> buttons = new ArrayList<>();
        private final JButton applyType;

        TypeCard() {
            setOpaque(false);
            setLayout(null);
            setBorder(new EmptyBorder(8, 8, 8, 8));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addButton("rotate-ccw", "CityTool.TypeCard.RotateLeft",
                    "Turn a quarter counter-clockwise: the selected houses, or the next house (C)", () -> turnNext(1));
            addButton("rotate-cw", "CityTool.TypeCard.RotateRight",
                    "Turn a quarter clockwise: the selected houses, or the next house", () -> turnNext(-1));
            addButton("flip-horizontal-2", "CityTool.TypeCard.Mirror",
                    "Mirror the selected houses, or the next house (X)", this::mirror);
            applyType = OverlayStyle.button(new JButton("Change type"));
            applyType.setName("CityTool.TypeCard.ChangeType");
            applyType.setToolTipText("Pick a schematic for the selected houses");
            applyType.setFocusable(false);
            applyType.setFont(OverlayStyle.font(Font.PLAIN, 12.5f));
            applyType.setBorder(new EmptyBorder(4, 9, 4, 9));
            applyType.setCursor(Cursor.getDefaultCursor());
            applyType.addActionListener(event -> changeTypeOfSelection());
            buttons.add(applyType);
            add(applyType);
        }

        private void mirror() {
            mirrorNext();
        }

        private void addButton(String icon, String name, String tooltip, Runnable action) {
            var button = OverlayStyle.button(new JButton(LucideIcons.icon(icon, 16, OverlayStyle.TEXT)));
            button.setName(name);
            button.setToolTipText(tooltip);
            button.setFocusable(false);
            button.setBorder(new EmptyBorder(4, 9, 4, 9));
            button.setCursor(Cursor.getDefaultCursor());
            button.addActionListener(event -> action.run());
            buttons.add(button);
            add(button);
        }

        @Override
        public void doLayout() {
            int x = 8, y = getHeight() - 8 - BUTTON_ROW + 3;
            for (JButton button : buttons) {
                var size = button.getPreferredSize();
                button.setBounds(x, y, size.width, BUTTON_ROW - 6);
                x += size.width + 4;
            }
        }

        @Override
        public void setBounds(int x, int y, int width, int height) {
            super.setBounds(x, y, width, height);
            doLayout();
        }

        /**
         * The buttons are hidden while picking for Add random (the card shows the pool
         * then).
         */
        void updateButtons() {
            boolean shown = !pickingRandomPool();
            for (JButton button : buttons)
                if (button.isVisible() != shown)
                    button.setVisible(shown);
            boolean any = !selectedStates.isEmpty();
            if (applyType.isEnabled() != any)
                applyType.setEnabled(any);
        }

        private String title() {
            CityLayer layer = getSelectedLayer();
            if (layer == null || uiState.objectIndex >= layer.getObjectList().size())
                return "";
            return schematicName(layer.getObjectList().get(uiState.objectIndex), uiState.objectIndex);
        }

        private String subtitle() {
            if (pickingRandomPool())
                return "Add random: " + randomPool.size() + " ticked";
            String mirrored = uiState.mirrored ? ", mirrored" : "";
            String rotation = uiState.rotation.toCompass() + "°" + mirrored;
            return selectedStates.size() > 1 ? selectedStates.size() + " selected · " + rotation : rotation;
        }

        /** The tile content of the next house, kept while the house does not change. */
        private TypeGrid.TileContent cached;
        private Object cachedKey;
        /** The view the card shows large. */
        private HouseView view = HouseView.TOP;
        /** The small view under the mouse: shown large while hovered. */
        private HouseView hoverView;

        /** Width of the card: a picker tile, wider if the button row needs it. */
        private int cardWidth() {
            int row = 0;
            for (JButton button : buttons)
                row += button.getPreferredSize().width + 4;
            return Math.max(GRID_TILE, row - 4 + 16);
        }

        /**
         * The next house as a picker tile shows it (turned and mirrored as it will be
         * placed).
         */
        private TypeGrid.TileContent content(CityLayer layer) {
            Object key = java.util.List.of(layer, layer.getObjectList(), uiState.objectIndex, uiState.rotation,
                    uiState.mirrored, cardWidth(), view, String.valueOf(hoverView));
            if (!key.equals(cachedKey)) {
                WPObject object = layer.getObjectForState(uiState);
                if (object == null)
                    return null;
                var size = object.getDimensions();
                // front and side are the house's own, whichever way it is turned: drawn from
                // the unturned (but mirrored) house; the top view shows the turn
                WPObject upright = layer.getObjectForState(
                        new ObjectState(CityLayer.Direction.NORTH, uiState.mirrored, uiState.objectIndex, 0, 0));
                cached = new TypeGrid.TileContent(layer.getSchematicImage(uiState), new int[]{size.x, size.y, size.z},
                        SchematicViews.front(upright), SchematicViews.side(upright),
                        TypeGrid.wrap(title(), getFontMetrics(TypeGrid.nameFont()), cardWidth() - 16), uiState.rotation,
                        hoverView != null ? hoverView : view, view);
                cachedKey = key;
            }
            return cached;
        }

        /**
         * The mouse is at {@code point} (null: gone): a small view under it is shown
         * large.
         */
        void hovered(Point point) {
            int small = point == null ? -1 : TypeGrid.thumbAt(new Rectangle(0, 0, getWidth(), getHeight()), point);
            HouseView thumb = small >= 0 ? HouseView.values()[small] : null;
            if (thumb != hoverView) {
                hoverView = thumb;
                repaint();
            }
        }

        /**
         * A click on the card: a small view becomes the large one, the large view opens
         * enlarged over the map, anywhere else opens the schematic picker.
         */
        void clicked(Point point) {
            var tile = new Rectangle(0, 0, getWidth(), getHeight());
            int small = TypeGrid.thumbAt(tile, point);
            if (small >= 0) {
                view = HouseView.values()[small];
                repaint();
            } else if (TypeGrid.mainRect(tile).contains(point)) {
                CityLayer layer = getSelectedLayer();
                var content = layer == null ? null : content(layer);
                if (content != null)
                    overlay.enlarge(content, title());
            } else
                overlay.openTypeGrid(false);
        }

        /** Height of the tile part: the picker's tile for this name. */
        private int tileHeight(CityLayer layer) {
            var content = content(layer);
            return overlay.typeGrid.tileHeight(cardWidth(), content == null ? 1 : content.nameLines().size());
        }

        @Override
        public java.awt.Dimension getPreferredSize() {
            CityLayer layer = getSelectedLayer();
            if (layer == null)
                return new java.awt.Dimension(cardWidth(), BUTTON_ROW);
            var subtitleMetrics = getFontMetrics(OverlayStyle.font(Font.PLAIN, 12f));
            boolean withButtons = !pickingRandomPool();
            return new java.awt.Dimension(cardWidth(),
                    tileHeight(layer) - 4 + subtitleMetrics.getHeight() + (withButtons ? BUTTON_ROW : 8));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            CityLayer layer = getSelectedLayer();
            if (layer == null)
                return;
            updateButtons();
            var content = content(layer);
            if (content == null)
                return;
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                // exactly a picker tile (bright mode included), on a surface reaching the
                // buttons
                var grid = overlay.typeGrid;
                grid.paintTileContent(g, new Rectangle(0, 0, getWidth(), getHeight()), content, false, false);
                int y = tileHeight(layer) - 8;
                g.setFont(OverlayStyle.font(Font.PLAIN, 12f));
                var metrics = g.getFontMetrics();
                g.setColor(grid.muted());
                g.drawString(clip(subtitle(), metrics, getWidth() - 16), 8, y + metrics.getAscent());
            } finally {
                g.dispose();
            }
        }

        private static String clip(String text, FontMetrics metrics, int width) {
            if (metrics.stringWidth(text) <= width)
                return text;
            String ellipsis = "…";
            int end = text.length();
            while (end > 0 && metrics.stringWidth(text.substring(0, end) + ellipsis) > width)
                end--;
            return text.substring(0, end) + ellipsis;
        }
    }

    /**
     * Schematic picker across the whole viewport: a grid of the layer's schematics
     * (top-down picture, name, size); the current one in lavender. Clicking a tile
     * picks it; clicking elsewhere or Esc closes; the wheel scrolls.
     */
    class TypeGrid extends JComponent
    {
        private static final int MARGIN = 32, GAP = 12;
        /** The search and size filter row, between the subtitle and the tiles. */
        private static final int FILTER_ROW_Y = 74, FILTER_ROW_HEIGHT = 28;

        /** Top of the tiles: below the search row, and the size filters while shown. */
        private int header() {
            return filterRow.isVisible() ? 152 : 118;
        }
        /** Tile height without the name (picture, measurements, padding). */
        /** Space between the three small views (px). */
        static final int THUMB_GAP = 6;
        /** Height of the labels under the small views (px). */
        static final int THUMB_LABEL = 13;
        /** All tiles are as tall as the longest name needs (one line at least). */
        private int tileHeight = 300;
        /** Room left of and below the picture for its measurements (px). */
        private static final int MEASURE_LEFT = 34, MEASURE_BOTTOM = 22;
        private final List<Image> pictures = new ArrayList<>();
        /** Per schematic: its front and side view (see SchematicViews). */
        private final List<Image> fronts = new ArrayList<>(), sides = new ArrayList<>();
        private final List<String> names = new ArrayList<>();
        /** The small view under the mouse and its tile: shown large while hovered. */
        private HouseView hoverThumb;
        private int hoverThumbTile = -1;
        /** The view each tile shows large (top unless a small view was clicked). */
        private final Map<Integer, HouseView> tileViews = new java.util.HashMap<>();
        /** Each name broken into lines that fit the tile, so it shows in full. */
        private final List<List<String>> nameLines = new ArrayList<>();
        /** Width (x), length (y) and height (z) in blocks per schematic. */
        private final List<int[]> sizes = new ArrayList<>();
        private int hover = -1;
        private int scroll;
        /**
         * Bright mode: light backdrop and tiles, for dark schematics that vanish on
         * charcoal. Kept while the tool lives, so the picker reopens the way it was
         * left.
         */
        private final JToggleButton brightToggle = new JToggleButton("Bright",
                LucideIcons.icon("sun", 16, OverlayStyle.TEXT));
        /** Search by name: every word typed must appear in the name (any case). */
        private final JTextField search = new JTextField() {
            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                if (!getText().isEmpty())
                    return;
                var g = (Graphics2D) graphics.create();
                try {
                    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g.setColor(OverlayStyle.TEXT_DISABLED);
                    g.setFont(getFont());
                    var insets = getInsets();
                    g.drawString("Search schematics", insets.left,
                            (getHeight() + g.getFontMetrics().getAscent() - g.getFontMetrics().getDescent()) / 2);
                } finally {
                    g.dispose();
                }
            }
        };
        /**
         * Size filters: width (x), length (y), height; each on when its checkbox is
         * ticked.
         */
        private final JCheckBox[] sizeFilters = {new JCheckBox("Width"), new JCheckBox("Length"),
                new JCheckBox("Height")};
        private final RangeSlider[] sizeRanges = new RangeSlider[3];
        private final JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        private final JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        /**
         * Shows / hides the size filters; counts the active ones while they are hidden.
         */
        private final JToggleButton filterButton = new JToggleButton(LucideIcons.icon("funnel", 16, OverlayStyle.TEXT));
        /** The schematics the search and filters let through, in layer order. */
        private final List<Integer> visible = new ArrayList<>();
        /**
         * Add random: tick or untick every schematic at once (only shown with the
         * checkboxes).
         */
        private final JButton selectAll = OverlayStyle.smallButton(new JButton("Select all"));
        private final JButton deselectAll = OverlayStyle.smallButton(new JButton("Deselect all"));

        TypeGrid() {
            setOpaque(false);
            setLayout(null);
            brightToggle.setName("CityTool.TypeGrid.Bright");
            brightToggle.setFocusable(false);
            brightToggle.setToolTipText("Bright background, for dark schematics");
            brightToggle.getModel().addItemListener(event -> {
                repaint();
                overlay.typeCard.repaint(); // the card follows the picker's bright mode
            });
            OverlayStyle.button(brightToggle);
            brightToggle.setSelectedIcon(LucideIcons.icon("sun", 16, OverlayStyle.ON_ACCENT));
            add(brightToggle);
            search.setName("CityTool.TypeGrid.Search");
            search.setColumns(16);
            search.setFont(OverlayStyle.font(Font.PLAIN, 13.5f));
            search.setBackground(OverlayStyle.FIELD);
            search.setForeground(OverlayStyle.TEXT);
            search.setCaretColor(OverlayStyle.TEXT);
            search.setSelectionColor(OverlayStyle.ACCENT_SOFT_SOLID);
            search.setSelectedTextColor(OverlayStyle.TEXT);
            search.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(OverlayStyle.BORDER),
                    new EmptyBorder(3, 8, 3, 8)));
            search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                @Override
                public void insertUpdate(javax.swing.event.DocumentEvent event) {
                    refilter();
                }

                @Override
                public void removeUpdate(javax.swing.event.DocumentEvent event) {
                    refilter();
                }

                @Override
                public void changedUpdate(javax.swing.event.DocumentEvent event) {
                    refilter();
                }
            });
            filterRow.setOpaque(false);
            filterRow.setVisible(false);
            searchRow.setOpaque(false);
            searchRow.add(search);
            filterButton.setName("CityTool.TypeGrid.Filters");
            filterButton.setFocusable(false);
            filterButton.setToolTipText("Filter by size (width, length, height)");
            OverlayStyle.button(filterButton);
            filterButton.setSelectedIcon(LucideIcons.icon("funnel", 16, OverlayStyle.ON_ACCENT));
            filterButton.setBorder(new EmptyBorder(4, 8, 4, 8));
            filterButton.addActionListener(event -> {
                filterRow.setVisible(filterButton.isSelected());
                setBounds(getBounds()); // the tiles move below (or back up to) the filter row
                refilter();
            });
            searchRow.add(filterButton);
            add(searchRow);
            String[] names = {"Width", "Length", "Height"};
            for (int axis = 0; axis < 3; axis++) {
                sizeFilters[axis].setName("CityTool.TypeGrid.Filter." + names[axis]);
                sizeFilters[axis].setToolTipText(
                        "Only schematics whose " + names[axis].toLowerCase(Locale.ROOT) + " (blocks) is in the range");
                DialogStyle.checkBox(sizeFilters[axis]);
                sizeFilters[axis].addActionListener(event -> refilter());
                int filter = axis;
                sizeRanges[axis] = new RangeSlider(0, 1);
                sizeRanges[axis].setName("CityTool.TypeGrid.Filter." + names[axis] + ".Range");
                sizeRanges[axis].setToolTipText(
                        "Drag the handles: smallest and largest " + names[axis].toLowerCase(Locale.ROOT) + " (blocks)");
                sizeRanges[axis].addChangeListener(event -> {
                    // moving a handle switches its filter on
                    if (!sizeFilters[filter].isSelected())
                        sizeFilters[filter].setSelected(true);
                    refilter();
                });
                filterRow.add(Box.createHorizontalStrut(8));
                filterRow.add(sizeFilters[axis]);
                filterRow.add(sizeRanges[axis]);
            }
            add(filterRow);
            // Esc in a field closes the picker like everywhere else on it
            var escape = new java.awt.event.KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent event) {
                    if (event.getKeyCode() == KeyEvent.VK_ESCAPE)
                        overlay.closeTypeGrid();
                }
            };
            search.addKeyListener(escape);
            selectAll.setName("CityTool.TypeGrid.SelectAll");
            selectAll.setFocusable(false);
            selectAll.setToolTipText("Tick every schematic shown for Add random");
            selectAll.addActionListener(event -> {
                randomPool.addAll(visible); // the tiles the search and filters show
                overlay.refresh();
                repaint();
            });
            add(selectAll);
            deselectAll.setName("CityTool.TypeGrid.DeselectAll");
            deselectAll.setFocusable(false);
            deselectAll.setToolTipText("Untick every schematic shown");
            deselectAll.addActionListener(event -> {
                randomPool.removeAll(visible);
                overlay.refresh();
                repaint();
            });
            add(deselectAll);
            var mouse = new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent event) {
                    int index = tileAt(event.getPoint());
                    if (index >= 0)
                        previewed = index;
                    if (index != hover) {
                        hover = index;
                        setCursor(Cursor.getPredefinedCursor(index >= 0 ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                        repaint();
                    }
                    // a small view under the mouse is shown large while hovered
                    int small = index >= 0 ? thumbAt(tileBounds(index), event.getPoint()) : -1;
                    HouseView thumb = small >= 0 ? HouseView.values()[small] : null;
                    if (thumb != hoverThumb || index != hoverThumbTile) {
                        hoverThumb = thumb;
                        hoverThumbTile = index;
                        repaint();
                    }
                }

                @Override
                public void mouseExited(MouseEvent event) {
                    hover = -1;
                    repaint();
                }

                // press and release, not mouseClicked: Swing drops the click when the
                // mouse moves even a pixel in between, so many clicks did nothing
                private int pressedTile = -1;
                private boolean pressed;

                @Override
                public void mousePressed(MouseEvent event) {
                    pressed = SwingUtilities.isLeftMouseButton(event);
                    pressedTile = pressed ? tileAt(event.getPoint()) : -1;
                }

                @Override
                public void mouseReleased(MouseEvent event) {
                    if (!pressed || !SwingUtilities.isLeftMouseButton(event))
                        return;
                    pressed = false;
                    int index = tileAt(event.getPoint());
                    int view = index >= 0 && index == pressedTile ? thumbAt(tileBounds(index), event.getPoint()) : -1;
                    if (view >= 0) {
                        // a small view: shown large in this tile; no pick, no tick
                        tileViews.put(index, HouseView.values()[view]);
                        repaint();
                    } else if (index >= 0 && index == pressedTile && pickingRandomPool()) {
                        // Add random: a click anywhere on a tile ticks it; the picker stays open
                        toggleRandomPool(index);
                        repaint();
                    } else if (index >= 0 && index == pressedTile)
                        pick(index);
                    else if (index < 0 && pressedTile < 0 && !previewBounds().contains(event.getPoint()))
                        overlay.closeTypeGrid();
                }

                @Override
                public void mouseWheelMoved(MouseWheelEvent event) {
                    scroll = Math.max(0, Math.min(maxScroll(), scroll + event.getWheelRotation() * 60));
                    hover = tileAt(event.getPoint());
                    repaint();
                }
            };
            addMouseListener(mouse);
            addMouseMotionListener(mouse);
            addMouseWheelListener(mouse);
        }

        void open(CityLayer layer) {
            selectAll.setVisible(pickingRandomPool());
            deselectAll.setVisible(pickingRandomPool());
            pictures.clear();
            fronts.clear();
            sides.clear();
            names.clear();
            nameLines.clear();
            sizes.clear();
            var objects = layer.getObjectList();
            for (int i = 0; i < objects.size(); i++) {
                pictures.add(layer.getSchematicImage(new ObjectState(CityLayer.Direction.NORTH, false, i, 0, 0)));
                fronts.add(SchematicViews.front(objects.get(i)));
                sides.add(SchematicViews.side(objects.get(i)));
                names.add(schematicName(objects.get(i), i));
                nameLines.add(wrap(names.get(i), getFontMetrics(nameFont()), GRID_TILE - 16));
                var dimensions = objects.get(i).getDimensions();
                sizes.add(new int[]{dimensions.x, dimensions.y, dimensions.z});
            }
            int lines = nameLines.stream().mapToInt(List::size).max().orElse(1);
            tileHeight = tileHeight(lines);
            tileViews.clear();
            previewed = -1;
            hover = -1;
            scroll = 0;
            resetFilters();
            SwingUtilities.invokeLater(search::requestFocusInWindow); // type to search right away
        }

        /**
         * No search, filters off, ranges spanning the layer's smallest to largest
         * schematic.
         */
        private void resetFilters() {
            search.setText("");
            filterButton.setSelected(false);
            filterRow.setVisible(false);
            for (int axis = 0; axis < 3; axis++) {
                int low = Integer.MAX_VALUE, high = 0;
                for (int[] size : sizes) {
                    low = Math.min(low, size[axis]);
                    high = Math.max(high, size[axis]);
                }
                if (sizes.isEmpty())
                    low = 0;
                sizeRanges[axis].setRange(low, high);
                sizeRanges[axis].setValues(low, high);
                sizeFilters[axis].setSelected(false); // setting the range switched it on
            }
            refilter();
        }

        /** The schematics shown: matching the search and every ticked size range. */
        private void refilter() {
            visible.clear();
            String[] words = search.getText().trim().toLowerCase(Locale.ROOT).split("\\s+");
            for (int i = 0; i < names.size(); i++) {
                String name = names.get(i).toLowerCase(Locale.ROOT);
                boolean matches = true;
                for (String word : words)
                    matches &= word.isEmpty() || name.contains(word);
                for (int axis = 0; axis < 3 && matches; axis++)
                    if (sizeFilters[axis].isSelected()) {
                        int size = sizes.get(i)[axis];
                        matches = size >= sizeRanges[axis].getLow() && size <= sizeRanges[axis].getHigh();
                    }
                if (matches)
                    visible.add(i);
            }
            int active = 0;
            for (JCheckBox filter : sizeFilters)
                if (filter.isSelected())
                    active++;
            filterButton.setText(active == 0 ? "" : Integer.toString(active));
            filterButton.setToolTipText(active == 0
                    ? "Filter by size (width, length, height)"
                    : "Filter by size: " + active + (active == 1 ? " filter" : " filters") + " active");
            scroll = Math.min(scroll, maxScroll());
            hover = -1;
            repaint();
        }

        /**
         * The checkbox of tile {@code index} (only shown while picking for Add random).
         */
        private Rectangle checkbox(int index) {
            Rectangle tile = tileBounds(index);
            return new Rectangle(tile.x + 6, tile.y + 6, 22, 22);
        }

        private void paintCheckbox(Graphics2D g, int index) {
            var box = checkbox(index);
            boolean ticked = randomPool.contains(index);
            var shape = new java.awt.geom.RoundRectangle2D.Double(box.x + 0.5, box.y + 0.5, box.width - 1,
                    box.height - 1, 6, 6);
            g.setColor(ticked ? OverlayStyle.ACCENT : bright() ? Color.WHITE : OverlayStyle.FIELD);
            g.fill(shape);
            g.setStroke(new BasicStroke(1.5f));
            g.setColor(ticked ? OverlayStyle.ACCENT : muted());
            g.draw(shape);
            if (ticked)
                LucideIcons.paint(g, "check", new Point2D.Double(box.getCenterX(), box.getCenterY()), 16, 0,
                        OverlayStyle.ON_ACCENT, null);
        }

        /** Opened by Change type: the pick goes to the selected houses. */
        private boolean forSelection;

        void pick(int index) {
            setNextType(index);
            if (forSelection)
                applyTypeToSelection();
            overlay.closeTypeGrid();
        }

        boolean bright() {
            return brightToggle.isSelected();
        }

        @Override
        public void setBounds(int x, int y, int width, int height) {
            super.setBounds(x, y, width, height);
            var size = brightToggle.getPreferredSize();
            // left of the close cross, level with the title
            brightToggle.setBounds(width - MARGIN - 24 - size.width, 36 - size.height / 2, size.width, size.height);
            var searchSize = searchRow.getPreferredSize();
            searchRow.setBounds(MARGIN - 6, FILTER_ROW_Y, Math.min(searchSize.width, width - 2 * MARGIN),
                    FILTER_ROW_HEIGHT + 4);
            searchRow.validate();
            var row = filterRow.getPreferredSize();
            filterRow.setBounds(MARGIN - 6 - 8, FILTER_ROW_Y + 36, Math.min(row.width, width - 2 * MARGIN),
                    FILTER_ROW_HEIGHT);
            filterRow.validate();
            // left of Bright: Deselect all, then Select all
            var deselect = deselectAll.getPreferredSize();
            deselectAll.setBounds(brightToggle.getX() - 8 - deselect.width, 36 - deselect.height / 2, deselect.width,
                    deselect.height);
            var select = selectAll.getPreferredSize();
            selectAll.setBounds(deselectAll.getX() - 4 - select.width, 36 - select.height / 2, select.width,
                    select.height);
        }

        // ---- palette: charcoal, or light in bright mode ----

        private Color text() {
            return bright() ? new Color(22, 22, 22) : OverlayStyle.TEXT;
        }

        private Color muted() {
            return bright() ? new Color(96, 96, 96) : OverlayStyle.TEXT_MUTED;
        }

        private Color tileFill(boolean current, boolean hovered) {
            if (bright())
                return current
                        ? new Color(229, 226, 255)
                        : hovered ? new Color(238, 238, 244) : new Color(250, 250, 250);
            return current
                    ? OverlayStyle.ACCENT_SOFT_SOLID
                    : hovered ? OverlayStyle.SURFACE_HOVER : OverlayStyle.SURFACE;
        }

        private Color tileEdge(boolean current, boolean hovered) {
            if (bright())
                return current ? OverlayStyle.INDIGO : hovered ? new Color(60, 60, 60) : new Color(0, 0, 0, 40);
            return current ? OverlayStyle.ACCENT : OverlayStyle.TEXT;
        }

        private int columns() {
            int reserve = previewWidth() > 0 ? previewWidth() + GAP : 0;
            return Math.max(1, (getWidth() - 2 * MARGIN - reserve + GAP) / (GRID_TILE + GAP));
        }

        /**
         * The tile last under the mouse: the preview shows it (the current type at
         * first).
         */
        private int previewed = -1;

        /**
         * Width of the preview card on the right: three tiles, smaller where the
         * viewport is too short for it, 0 (no preview) where it leaves no room for a
         * column of tiles.
         */
        int previewWidth() {
            int width = 3 * GRID_TILE + 2 * GAP;
            int room = getHeight() - header() - MARGIN;
            int lines = previewLines(width);
            while (width > GRID_TILE && tileHeight(width, lines) > room) {
                width -= 12;
                lines = previewLines(width);
            }
            if (width < GRID_TILE || getWidth() < 2 * MARGIN + GRID_TILE + GAP + width)
                return 0;
            return width;
        }

        private int previewLines(int width) {
            int index = previewIndex();
            return index < 0 ? 1 : wrap(names.get(index), getFontMetrics(nameFont()), width - 16).size();
        }

        /** The schematic the preview shows, or -1. */
        private int previewIndex() {
            int index = hover >= 0 ? hover : previewed >= 0 ? previewed : uiState.objectIndex;
            return index >= 0 && index < names.size() ? index : -1;
        }

        /** The preview card's bounds (empty without a preview). */
        Rectangle previewBounds() {
            int width = previewWidth();
            if (width == 0)
                return new Rectangle();
            return new Rectangle(getWidth() - MARGIN - width, header(), width, tileHeight(width, previewLines(width)));
        }

        private Rectangle tileBounds(int index) {
            int slot = visible.indexOf(index);
            if (slot < 0)
                return new Rectangle(-10000, -10000, 0, 0); // filtered out
            index = slot;
            int columns = columns();
            int gridWidth = columns * GRID_TILE + (columns - 1) * GAP;
            int reserve = previewWidth() > 0 ? previewWidth() + GAP : 0;
            int left = Math.max(MARGIN, (getWidth() - reserve - gridWidth) / 2);
            int row = index / columns, column = index % columns;
            return new Rectangle(left + column * (GRID_TILE + GAP), header() + row * (tileHeight + GAP) - scroll,
                    GRID_TILE, tileHeight);
        }

        private int maxScroll() {
            int rows = (visible.size() + columns() - 1) / columns();
            int content = rows * (tileHeight + GAP) - GAP;
            return Math.max(0, content - (getHeight() - header() - MARGIN));
        }

        private int tileAt(Point point) {
            if (point.y < header() - GAP)
                return -1;
            for (int i : visible)
                if (tileBounds(i).contains(point))
                    return i;
            return -1;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                // the select buttons belong to the checkboxes: only in add mode with Add random
                for (JCheckBox filter : sizeFilters)
                    if (!text().equals(filter.getForeground()))
                        filter.setForeground(text());
                for (RangeSlider range : sizeRanges)
                    if (!muted().equals(range.getForeground()))
                        range.setForeground(muted());
                boolean ticking = pickingRandomPool();
                if (selectAll.isVisible() != ticking) {
                    selectAll.setVisible(ticking);
                    deselectAll.setVisible(ticking);
                }
                g.setColor(bright() ? new Color(232, 232, 232, 245) : new Color(22, 22, 22, 235));
                g.fillRect(0, 0, getWidth(), getHeight());

                g.setFont(OverlayStyle.font(Font.BOLD, 20f));
                g.setColor(text());
                g.drawString("Choose a schematic", MARGIN, 40);
                g.setFont(OverlayStyle.font(Font.PLAIN, 13f));
                g.setColor(muted());
                int selected = selectedStates.size();
                String target = forSelection && selected > 0
                        ? "Change type: for the " + (selected == 1 ? "selected house" : selected + " selected houses")
                        : "For the next house you place";
                if (pickingRandomPool())
                    target = "Add random: tick the schematics to pick from (" + randomPool.size()
                            + " ticked): click a schematic to tick it";
                g.drawString(target + " · Esc or a click outside closes", MARGIN, 62);
                LucideIcons.paint(g, "x", new Point2D.Double(getWidth() - MARGIN, 36), 20, 0, muted(), null);

                g.clipRect(0, header() - GAP, getWidth(), getHeight());
                if (visible.isEmpty()) {
                    g.setFont(OverlayStyle.font(Font.PLAIN, 14f));
                    g.setColor(muted());
                    g.drawString("No schematic matches the search and filters", MARGIN, header() + 24);
                }
                for (int i : visible) {
                    Rectangle tile = tileBounds(i);
                    if (tile.y > getHeight() || tile.y + tile.height < header() - GAP)
                        continue;
                    paintTile(g, i, tile);
                }
                paintPreview(g);
            } finally {
                g.dispose();
            }
        }

        /**
         * What a tile shows: the top view (with a margin), the size, the front and side
         * views, the name's lines, which way the top view faces and which view is
         * large.
         */
        record TileContent(Image picture, int[] size, Image front, Image side, List<String> nameLines,
                CityLayer.Direction facing, HouseView view, HouseView chosen) {
        }

        private TileContent content(int index) {
            return new TileContent(pictures.get(index), sizes.get(index), fronts.get(index), sides.get(index),
                    nameLines.get(index), CityLayer.Direction.NORTH,
                    index == hoverThumbTile && hoverThumb != null
                            ? hoverThumb
                            : tileViews.getOrDefault(index, HouseView.TOP),
                    tileViews.getOrDefault(index, HouseView.TOP));
        }

        // ---- tile layout: a square large view, three small views in a row, the name

        static int mainSize(int width) {
            return width - 16;
        }

        static int thumbSize(int width) {
            return (width - 16 - 2 * THUMB_GAP) / 3;
        }

        static Rectangle mainRect(Rectangle tile) {
            int size = mainSize(tile.width);
            return new Rectangle(tile.x + 8, tile.y + 8, size, size);
        }

        /** Small view {@code k}: 0 front, 1 side, 2 top. */
        static Rectangle thumbRect(Rectangle tile, int k) {
            int size = thumbSize(tile.width);
            return new Rectangle(tile.x + 8 + k * (size + THUMB_GAP), tile.y + 8 + mainSize(tile.width) + 6, size,
                    size);
        }

        /** The small view under {@code point}, or -1. */
        static int thumbAt(Rectangle tile, Point point) {
            for (int k = 0; k < 3; k++)
                if (thumbRect(tile, k).contains(point))
                    return k;
            return -1;
        }

        static int nameTop(Rectangle tile) {
            return tile.y + 8 + mainSize(tile.width) + 6 + thumbSize(tile.width) + THUMB_LABEL + 6;
        }

        /** Height of a tile {@code width} wide whose name takes {@code lines} lines. */
        int tileHeight(int width, int lines) {
            return 8 + mainSize(width) + 6 + thumbSize(width) + THUMB_LABEL + 6
                    + lines * getFontMetrics(nameFont()).getHeight() + 10;
        }

        int tileHeight(int lines) {
            return tileHeight(GRID_TILE, lines);
        }

        /**
         * The hovered (or last hovered) schematic as a large card on the right: the
         * same tile, three tiles wide, with its large view as the tile shows it.
         */
        private void paintPreview(Graphics2D g) {
            int index = previewIndex();
            var bounds = previewBounds();
            if (index < 0 || bounds.isEmpty())
                return;
            var small = content(index);
            var content = new TileContent(small.picture(), small.size(), small.front(), small.side(),
                    wrap(names.get(index), getFontMetrics(nameFont()), bounds.width - 16), small.facing(), small.view(),
                    small.chosen());
            paintTileContent(g, bounds, content, false, false);
        }

        private void paintTile(Graphics2D g, int index, Rectangle tile) {
            paintTileContent(g, tile, content(index), index == uiState.objectIndex, index == hover);
            if (pickingRandomPool())
                paintCheckbox(g, index);
        }

        /**
         * A tile: surface, the large view with its measurements, the three small views
         * (the large one marked) and the name. Shared by the picker and the type card.
         */
        void paintTileContent(Graphics2D g, Rectangle tile, TileContent content, boolean current, boolean hovered) {
            OverlayStyle.paintSurface(g, tile.x, tile.y, tile.width, tile.height, tileFill(current, hovered));
            if (current || hovered || bright()) {
                g.setColor(tileEdge(current, hovered));
                g.setStroke(new BasicStroke(current ? 2f : 1f));
                g.draw(OverlayStyle.rounded(tile.x + 1, tile.y + 1, tile.width - 2, tile.height - 2));
            }
            paintMainView(g, mainRect(tile), content, content.view());
            paintThumbnails(g, tile, content);
            int y = nameTop(tile);
            g.setFont(nameFont());
            var metrics = g.getFontMetrics();
            g.setColor(text());
            for (String line : content.nameLines()) {
                g.drawString(line, tile.x + 8, y + metrics.getAscent());
                y += metrics.getHeight();
            }
        }

        /**
         * One view large in {@code area}, with dimension lines: top view width and
         * length (height in the corner) and the front's ">"; front view width and
         * height; side view length and height.
         */
        void paintMainView(Graphics2D g, Rectangle area, TileContent content, HouseView view) {
            int[] size = content.size();
            Image picture = switch (view) {
                case TOP -> content.picture();
                case FRONT -> content.front();
                case SIDE -> content.side();
            };
            if (picture == null)
                return;
            int across = view == HouseView.SIDE ? size[1] : size[0];
            int up = view == HouseView.TOP ? size[1] : size[2];
            int width = picture.getWidth(null), height = picture.getHeight(null);
            int areaX = area.x + MEASURE_LEFT, areaY = area.y + 12;
            int areaWidth = area.width - MEASURE_LEFT - 4, areaHeight = area.height - MEASURE_BOTTOM - 12;
            double scale = Math.min((double) areaWidth / width, (double) areaHeight / height);
            int w = Math.max(1, (int) Math.round(width * scale)), h = Math.max(1, (int) Math.round(height * scale));
            int imageX = areaX + (areaWidth - w) / 2, imageY = areaY + (areaHeight - h) / 2;
            var quality = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(picture, imageX, imageY, w, h, null);
            if (quality != null)
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, quality);
            // the top view has a margin around the house; the measurements span the house
            double marginX = (width - across) / 2.0 * scale, marginY = (height - up) / 2.0 * scale;
            var house = new Rectangle2D.Double(imageX + marginX, imageY + marginY, across * scale, up * scale);
            paintMeasurements(g, house, Integer.toString(across),
                    view == HouseView.TOP ? Integer.toString(up) : up + "h",
                    view == HouseView.TOP ? size[2] + "h" : null);
            if (view == HouseView.TOP) {
                // which side is the front: the map outlines' ">", subtle, without the outline
                var facing = facingArrow(house, content.facing());
                if (facing != null) {
                    g.setColor(muted());
                    g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.draw(facing);
                }
            }
        }

        /**
         * The three small views, labelled; the one shown large is marked in lavender.
         */
        private void paintThumbnails(Graphics2D g, Rectangle tile, TileContent content) {
            g.setFont(OverlayStyle.font(Font.PLAIN, 10.5f));
            var metrics = g.getFontMetrics();
            var quality = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
            for (HouseView view : HouseView.values()) {
                var box = thumbRect(tile, view.ordinal());
                boolean chosen = view == content.chosen(), previewed = view == content.view() && !chosen;
                // the chosen one only hinted at: a thin, faint lavender edge
                g.setColor(bright() ? new Color(0, 0, 0, 18) : new Color(255, 255, 255, 14));
                g.fill(OverlayStyle.rounded(box.x, box.y, box.width, box.height));
                g.setColor(chosen
                        ? (bright() ? new Color(94, 55, 175, 150) : new Color(205, 199, 255, 150))
                        : previewed ? text() : bright() ? new Color(0, 0, 0, 50) : OverlayStyle.HAIRLINE);
                g.setStroke(new BasicStroke(previewed ? 1.5f : 1f));
                g.draw(OverlayStyle.rounded(box.x + 0.5, box.y + 0.5, box.width - 1, box.height - 1));
                Image image = switch (view) {
                    case TOP -> content.picture();
                    case FRONT -> content.front();
                    case SIDE -> content.side();
                };
                if (image != null) {
                    int pad = 4, room = box.width - 2 * pad;
                    double scale = Math.min((double) room / image.getWidth(null),
                            (double) room / image.getHeight(null));
                    int w = Math.max(1, (int) Math.round(image.getWidth(null) * scale)),
                            h = Math.max(1, (int) Math.round(image.getHeight(null) * scale));
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                    g.drawImage(image, box.x + (box.width - w) / 2, box.y + (box.height - h) / 2, w, h, null);
                }
                g.setColor(previewed ? text() : muted());
                g.drawString(view.label, box.x + (box.width - metrics.stringWidth(view.label)) / 2,
                        box.y + box.height + metrics.getAscent());
            }
            if (quality != null)
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, quality);
        }

        static Font nameFont() {
            return OverlayStyle.font(Font.BOLD, 13f);
        }

        /**
         * {@code text} in lines of at most {@code width} pixels: broken between words,
         * a word longer than a line broken between characters.
         */
        static List<String> wrap(String text, FontMetrics metrics, int width) {
            var lines = new ArrayList<String>();
            var line = new StringBuilder();
            for (String word : text.trim().split("\\s+")) {
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (metrics.stringWidth(candidate) <= width) {
                    line.setLength(0);
                    line.append(candidate);
                    continue;
                }
                if (!line.isEmpty()) {
                    lines.add(line.toString());
                    line.setLength(0);
                }
                // a word wider than the tile: as many characters per line as fit
                String rest = word;
                while (metrics.stringWidth(rest) > width) {
                    int end = 1;
                    while (end < rest.length() && metrics.stringWidth(rest.substring(0, end + 1)) <= width)
                        end++;
                    lines.add(rest.substring(0, end));
                    rest = rest.substring(end);
                }
                line.append(rest);
            }
            if (!line.isEmpty() || lines.isEmpty())
                lines.add(line.toString());
            return lines;
        }

        /**
         * Dimension lines like a technical drawing: {@code across} below the house,
         * {@code up} on its left, {@code corner} (e.g. the height "12h") in the corner
         * between them.
         */
        private void paintMeasurements(Graphics2D g, Rectangle2D.Double house, String across, String up,
                String corner) {
            g.setFont(OverlayStyle.font(Font.PLAIN, 11.5f));
            var metrics = g.getFontMetrics();
            g.setStroke(new BasicStroke(1f));
            g.setColor(muted());
            int tick = 3;
            double lineY = house.getMaxY() + 6;
            g.draw(new Line2D.Double(house.getMinX(), lineY, house.getMaxX(), lineY));
            g.draw(new Line2D.Double(house.getMinX(), lineY - tick, house.getMinX(), lineY + tick));
            g.draw(new Line2D.Double(house.getMaxX(), lineY - tick, house.getMaxX(), lineY + tick));
            float textY = (float) (lineY + 3 + metrics.getAscent());
            g.drawString(across, (float) (house.getCenterX() - metrics.stringWidth(across) / 2.0), textY);
            double lineX = house.getMinX() - 6;
            g.draw(new Line2D.Double(lineX, house.getMinY(), lineX, house.getMaxY()));
            g.draw(new Line2D.Double(lineX - tick, house.getMinY(), lineX + tick, house.getMinY()));
            g.draw(new Line2D.Double(lineX - tick, house.getMaxY(), lineX + tick, house.getMaxY()));
            g.drawString(up, (float) (lineX - 4 - metrics.stringWidth(up)),
                    (float) (house.getCenterY() + metrics.getAscent() / 2.0 - 1));
            if (corner != null) {
                g.setColor(text());
                g.drawString(corner, (float) (lineX - 4 - metrics.stringWidth(corner)), textY);
            }
        }
    }
}