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
import org.ironsight.wpplugin.macromachine.Gui.GlobalActionPanel;
import org.ironsight.wpplugin.macromachine.Gui.HelpDialog;
import org.ironsight.wpplugin.macromachine.Gui.HelpDialog.HelpItem;
import org.ironsight.wpplugin.macromachine.Gui.MapToolOverlay;
import org.ironsight.wpplugin.macromachine.Gui.OverlayStyle;
import org.ironsight.wpplugin.macromachine.Layers.PathBuilder.LucideIcons;
import org.ironsight.wpplugin.macromachine.Layers.PathBuilder.PathTool;
import org.ironsight.wpplugin.macromachine.Layers.PathBuilder.PathTransformBox;
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
            new HelpItem("Add (button)", "Every click on empty map places a new building"),
            new HelpItem("Left-button drag on a building", "Move the selection"),
            new HelpItem("Left-button drag on empty map", "Select buildings fully inside the drag rectangle"),
            new HelpItem("Right click", "Move the selection center to the cursor"),
            new HelpItem("Shift + mouse wheel", "Change the selected building type"),
            new HelpItem("Ctrl + A", "Select all buildings"),
            new HelpItem("Escape", "Leave add mode, else clear the selection"),
            new HelpItem("Ctrl + C", "Copy selected buildings"), new HelpItem("Ctrl + X", "Cut selected buildings"),
            new HelpItem("Ctrl + V", "Paste buildings at the cursor"),
            new HelpItem("Q", "Randomize selected buildings using the random options of the Schematics menu"),
            new HelpItem("W/A/S/D", "Move selected buildings"),
            new HelpItem("C", "Rotate selected buildings around the selection center"),
            new HelpItem("X", "Mirror selected buildings"), new HelpItem("Delete", "Delete selected buildings"));

    private static final int OVERLAY_ICON_SIZE = 256;
    /** Hit radius of the transform handles (px). */
    static final double HANDLE_HIT_RADIUS = 12;
    /** Thumbnails in the Schematics menu (px). */
    static final int GRID_TILE = 150;
    /** Outline of every house (overlay part): white, half transparent. */
    static final Color OUTLINE_COLOR = new Color(255, 255, 255, 140);
    /** The current type's preview card below the toolbar (px). */
    static final int CARD_THUMBNAIL = 72;

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
    private final JToggleButton showOverlays = new JToggleButton("", true);
    /**
     * Overlay part: the layer paints in its color instead of the schematics'
     * blocks.
     */
    private final JToggleButton showHighlight = new JToggleButton("Highlight colors", true);
    /** Overlay part: an outline around every house of the layer. */
    private final JToggleButton showOutlines = new JToggleButton("Outlines", true);
    private final CityOverlay overlay = new CityOverlay();
    private WorldPainterView overlayView;
    private volatile boolean cursorOverMap;
    private TiledImageViewer.ViewListener previousViewListener;
    private final TiledImageViewer.ViewListener overlayViewListener = changedView -> {
        if (previousViewListener != null)
            previousViewListener.viewChanged(changedView);
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
        }

        @Override
        public void mousePressed(MouseEvent event) {
            cursorOverMap = true;
            trackCursor(event.getPoint());
            if (SwingUtilities.isLeftMouseButton(event) && !event.isAltDown() && !event.isMetaDown())
                handlePress(event.getPoint(), event.isControlDown(), event.isShiftDown());
        }

        @Override
        public void mouseDragged(MouseEvent event) {
            cursorOverMap = true;
            trackCursor(event.getPoint());
            handleDrag(event.getPoint());
        }

        @Override
        public void mouseReleased(MouseEvent event) {
            if (SwingUtilities.isLeftMouseButton(event))
                handleRelease(event.getPoint());
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
     * Pivot placed by dragging it (world); belongs to the selection it was placed
     * for.
     */
    private Point2D.Double customPivot;
    private Set<Point> customPivotSelection = Set.of();
    private Point2D.Double draggedPivot;

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
                if (overlay.typeGridOpen())
                    overlay.closeTypeGrid();
                else if (gestureStart != null)
                    cancelGesture();
                else if (addMode.isSelected())
                    addMode.setSelected(false);
                else if (layer != null)
                    deselect(layer);
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
     * A click at a world position: right click moves the selection there; Ctrl +
     * left click toggles the house there in the selection, on empty map it places a
     * house; a plain left click selects only the house there (or adds one in add
     * mode, else clears the selection).
     */
    void handleClick(int centreX, int centreY, boolean rightClick, boolean ctrlDown) {
        cursorWorldPosition = new Point(centreX, centreY);
        CityLayer layer = getSelectedLayer();
        if (layer == null)
            return;

        if (rightClick) {
            moveSelectionTo(layer, centreX, centreY);
            return;
        }
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
        var newState = setCurrentStatePosition(centreX, centreY, template);
        edit(() -> {
            layer.setDataAt(getDimension(), newState.xPos, newState.yPos, newState);
            selectOnly(newState);
            // the next house is randomized afresh, from the type the user chose
            uiState = setCurrentStatePosition(newState.xPos, newState.yPos, uiState);
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
        overlay.repaint();
    }

    private void removeSelectedState(ObjectState state) {
        selectedStates.remove(new Point(state.xPos, state.yPos));
        overlay.repaint();
    }

    private void clearSelection() {
        selectedStates.clear();
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
        if (getPaint() instanceof LayerPaint layerPaint && layerPaint.getLayer() instanceof CityLayer cityLayer) {
            return cityLayer;
        }
        return null;
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
        // left clicks and drags are handled by the map mouse listener (handles, box
        // selection); right click moves the selection to the cursor
        if (!inverse || !first)
            return;
        if (getPaint() instanceof LayerPaint layerPaint && layerPaint.getLayer() instanceof CityLayer cityLayer) {
            ensureLayerHasUndoManager(cityLayer, getDimension());
            edit(() -> handleClick(centreX, centreY, true, false));
        }
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
        attachOverlay();
        updatePanel();
    }

    @Override
    protected void deactivate() {
        overlay.endDrag();
        detachOverlay();
        super.deactivate();
    }

    protected void paintChanged(Paint ignored) {
        cancelGesture();
        clearSelection();
        clearClipboard();
        customPivot = null;
        overlay.closeTypeGrid();
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
        if (customPivot != null)
            movePivot(deltaX, deltaY);
    }

    private void moveSelectionTo(CityLayer layer, int x, int y) {
        if (selectedStates.isEmpty())
            return;
        Point center = getSelectionCenter(new ArrayList<>(selectedStates.values()));
        moveSelection(layer, x - center.x, y - center.y);
    }

    private void rotateSelection(CityLayer layer) {
        if (selectedStates.isEmpty())
            return;

        Point center = getSelectionCenter(new ArrayList<>(selectedStates.values()));
        applyToSelection(layer, state -> rotateStateAround(state, center));
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

    /** A quarter turn counter-clockwise on screen around {@code center}. */
    static ObjectState rotateStateAround(ObjectState state, Point center) {
        long relativeX = (long) state.xPos - center.x;
        long relativeY = (long) state.yPos - center.y;
        int x = Math.toIntExact((long) center.x + relativeY);
        int y = Math.toIntExact((long) center.y - relativeX);
        return new ObjectState(state.rotation.nextRotation(), state.mirrored, state.objectIndex, x, y);
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
                rotation = rotation.nextRotation();
            return new ObjectState(rotation, state.mirrored, state.objectIndex, x, y);
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

    /** Randomizes one property of the selected houses (Schematics menu). */
    void randomizeSelection(boolean rotation, boolean type, boolean mirror) {
        CityLayer layer = getSelectedLayer();
        if (layer == null || selectedStates.isEmpty())
            return;
        var saved = placementOptions;
        placementOptions = new PlacementOptions(rotation, type, mirror);
        try {
            edit(() -> applyToSelection(layer, this::randomizeState));
        } finally {
            placementOptions = saved;
        }
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
        return List.of(showHighlight, showOutlines);
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
        if (gestureTransform == null)
            return states;
        return states.stream().map(gestureTransform).toList();
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

    /** Transform box (view) around the houses' footprints; null without houses. */
    private Rectangle2D.Double transformBox(CityLayer layer, List<ObjectState> houses) {
        var corners = new ArrayList<Point2D.Double>();
        for (ObjectState house : houses) {
            var bounds = toView(footprint(layer, house));
            corners.add(new Point2D.Double(bounds.getMinX(), bounds.getMinY()));
            corners.add(new Point2D.Double(bounds.getMaxX(), bounds.getMaxY()));
        }
        return PathTransformBox.box(corners, IntStream.range(0, corners.size()).boxed().toList());
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
     * Pivot for rotating and scaling (world): placed by the user, else the
     * selection's centre.
     */
    Point2D.Double pivotWorld() {
        if (draggedPivot != null)
            return draggedPivot;
        if (customPivot != null && customPivotSelection.equals(selectedStates.keySet()))
            return customPivot;
        CityLayer layer = getSelectedLayer();
        return layer == null ? null : selectionCentre(layer);
    }

    private void movePivot(int deltaX, int deltaY) {
        if (customPivot == null || !customPivotSelection.equals(selectedStates.keySet()))
            return;
        customPivot = new Point2D.Double(customPivot.x + deltaX, customPivot.y + deltaY);
    }

    /** What a press at {@code point} grabs: a transform handle, else a house. */
    Hit hitTest(Point point) {
        CityLayer layer = getSelectedLayer();
        if (overlayView == null || layer == null)
            return null;
        if (showOverlays.isSelected() && !selectedStates.isEmpty()) {
            var box = transformBox(layer, shownSelection());
            if (box != null) {
                var candidates = new ArrayList<Hit>();
                var points = new ArrayList<Point2D>();
                candidates.add(new Hit(HitKind.MOVE, 0, null));
                points.add(PathTransformBox.moveHandle(box));
                candidates.add(new Hit(HitKind.ROTATE, 0, null));
                points.add(PathTransformBox.rotateHandle(box));
                if (selectedStates.size() > 1) {
                    for (int axis : new int[]{PathTransformBox.SCALE_UNIFORM, PathTransformBox.SCALE_X,
                            PathTransformBox.SCALE_Y}) {
                        candidates.add(new Hit(HitKind.SCALE, axis, null));
                        points.add(PathTransformBox.scaleHandle(box, axis));
                    }
                    candidates.add(new Hit(HitKind.PIVOT, 0, null));
                    points.add(toViewExact(pivotWorld()));
                }
                Hit best = null;
                double bestDistance = HANDLE_HIT_RADIUS * HANDLE_HIT_RADIUS;
                for (int i = 0; i < candidates.size(); i++) {
                    double distance = points.get(i).distanceSq(point);
                    if (distance <= bestDistance) {
                        bestDistance = distance;
                        best = candidates.get(i);
                    }
                }
                if (best != null)
                    return best;
            }
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
        cancelGesture();
        Hit hit = hitTest(point);
        if (shift && hit == null)
            return; // Shift + drag on empty map stays WorldPainter's
        gestureStart = new Point(point);
        gestureWorldStart = overlayView.viewToWorld(point);
        gestureHit = hit;
        gestureAdditive = additive;
        gestureDragged = false;
        setHovered(hit); // the grabbed element stays highlighted while dragging
        if (hit == null)
            overlay.startDrag(point);
    }

    void handleDrag(Point point) {
        if (gestureStart == null || overlayView == null)
            return;
        if (gestureStart.distanceSq(point) >= 9)
            gestureDragged = true;
        if (!gestureDragged)
            return;
        if (gestureHit == null) {
            overlay.updateDrag(point);
            return;
        }
        Point world = overlayView.viewToWorld(point);
        switch (gestureHit.kind()) {
            case HOUSE, MOVE -> {
                if (gestureHit.kind() == HitKind.HOUSE
                        && !selectedStates.containsKey(new Point(gestureHit.house().xPos, gestureHit.house().yPos))) {
                    if (!gestureAdditive)
                        clearSelection();
                    addSelectedState(gestureHit.house());
                }
                int deltaX = world.x - gestureWorldStart.x, deltaY = world.y - gestureWorldStart.y;
                gestureTransform = moveBy(deltaX, deltaY);
                gestureHeader = "Move " + deltaX + ", " + deltaY;
            }
            case ROTATE -> {
                Point2D.Double pivot = pivotWorld();
                double degrees = PathTransformBox.rotationDegrees(toViewExact(pivot), gestureStart, point);
                if (selectedStates.size() > 1) {
                    // a group turns by any angle; each house faces the nearest quarter turn
                    gestureTransform = rotateAround(pivot, degrees);
                    gestureHeader = "Rotate " + Math.round(degrees) + "°";
                } else {
                    int turns = (int) Math.round(degrees / 90);
                    gestureTransform = rotateAround(new Point((int) Math.round(pivot.x), (int) Math.round(pivot.y)),
                            turns);
                    gestureHeader = "Rotate " + Math.floorMod(turns * 90, 360) + "°";
                }
            }
            case SCALE -> {
                Point2D.Double pivot = pivotWorld();
                var pivotView = toViewExact(pivot);
                int axis = gestureHit.side();
                double scaleX = 1, scaleY = 1;
                if (axis == PathTransformBox.SCALE_UNIFORM)
                    scaleX = scaleY = PathTransformBox.scaleFactor(pivotView, gestureStart, point);
                else if (axis == PathTransformBox.SCALE_X)
                    scaleX = PathTransformBox.axisScaleFactor(pivotView, gestureStart, point, 0, axis);
                else
                    scaleY = PathTransformBox.axisScaleFactor(pivotView, gestureStart, point, 0, axis);
                gestureTransform = scaleAround(pivot, scaleX, scaleY);
                gestureHeader = axis == PathTransformBox.SCALE_UNIFORM
                        ? String.format(Locale.ROOT, "Spread %.2f", scaleX)
                        : axis == PathTransformBox.SCALE_X
                                ? String.format(Locale.ROOT, "Spread X %.2f", scaleX)
                                : String.format(Locale.ROOT, "Spread Y %.2f", scaleY);
            }
            case PIVOT -> {
                draggedPivot = toWorldExact(point);
                gestureHeader = "Pivot";
            }
        }
        overlay.repaint();
    }

    void handleRelease(Point point) {
        if (gestureStart == null)
            return;
        CityLayer layer = getSelectedLayer();
        try {
            handleDrag(point);
            if (layer == null)
                return;
            if (gestureHit == null) {
                if (gestureDragged) {
                    Rectangle bounds = overlay.getDragBounds();
                    if (bounds != null)
                        selectWithinBox(layer, bounds, gestureAdditive);
                } else {
                    Point world = overlayView.viewToWorld(gestureStart);
                    edit(() -> handleClick(world.x, world.y, false, gestureAdditive));
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
                    commitGestureTransform(true);
                }
                case MOVE -> commitGestureTransform(true);
                case ROTATE, SCALE -> commitGestureTransform(false);
                case PIVOT -> {
                    if (gestureDragged && draggedPivot != null) {
                        customPivot = draggedPivot;
                        customPivotSelection = Set.copyOf(selectedStates.keySet());
                    } else
                        customPivot = null; // a click puts the pivot back in the centre
                }
            }
        } finally {
            cancelGesture();
            setHovered(cursorOverMap ? hitTest(point) : null);
            overlay.repaint();
        }
    }

    private void commitGestureTransform(boolean move) {
        if (gestureTransform == null)
            return;
        var transform = gestureTransform;
        boolean pivotFollows = move && customPivot != null && customPivotSelection.equals(selectedStates.keySet());
        ObjectState probe = selectedStates.values().iterator().next();
        ObjectState moved = transform.apply(probe);
        if (transformSelection(transform) && pivotFollows) {
            customPivot = new Point2D.Double(customPivot.x + moved.xPos - probe.xPos,
                    customPivot.y + moved.yPos - probe.yPos);
            customPivotSelection = Set.copyOf(selectedStates.keySet());
        }
    }

    private void cancelGesture() {
        gestureStart = null;
        gestureWorldStart = null;
        gestureHit = null;
        gestureDragged = false;
        gestureTransform = null;
        gestureHeader = null;
        draggedPivot = null;
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
        menu.add(item("Rotate 90° (C)", "CityTool.Rotate", any, needSelection,
                () -> edit(() -> rotateSelection(layer))));
        menu.add(item("Mirror (X)", "CityTool.Mirror", any, needSelection, () -> edit(() -> mirrorSelection(layer))));
        menu.add(item("Delete (Del)", "CityTool.Delete", any, needSelection, () -> edit(this::deleteSelected)));
        menu.addSeparator();
        menu.add(item("Randomize rotation", "CityTool.RandomizeRotation", any, needSelection,
                () -> randomizeSelection(true, false, false)));
        menu.add(item("Randomize mirroring", "CityTool.RandomizeMirroring", any, needSelection,
                () -> randomizeSelection(false, false, true)));
        menu.add(item("Randomize type", "CityTool.RandomizeType", any, needSelection,
                () -> randomizeSelection(false, true, false)));
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
                "New houses (and Q) get a random schematic",
                on -> setPlacementOptions(new PlacementOptions(options.randomRotate(), on, options.randomMirror()))));
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

    void selectLayer(CityLayer layer) {
        CityLayerLibrary.select(layer, this::setPaint);
    }

    private void useTutorialLayer() {
        try {
            CityLayer layer = CityLayerLibrary.addTutorialLayer(getDimension());
            if (layer != null)
                selectLayer(layer);
        } catch (Exception ex) {
            GlobalActionPanel.ErrorPopUp(ex);
        }
    }

    private void importLayerFromFile() {
        try {
            CityLayer layer = CityLayerLibrary.importFromFile(getDimension());
            if (layer != null)
                selectLayer(layer);
        } catch (Exception ex) {
            GlobalActionPanel.ErrorPopUp(ex);
        }
    }

    /** The add-mode switch (exposed for tests). */
    JToggleButton addModeToggle() {
        return addMode;
    }

    /**
     * Opens the schematic picker over the whole viewport (what clicking the type
     * card does).
     */
    void openTypeGrid() {
        overlay.openTypeGrid();
    }

    boolean typeGridOpen() {
        return overlay.typeGridOpen();
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
            for (AbstractButton control : List.of(schematicsButton, layerButton, overlaysButton, overlaysMenuButton,
                    addButton))
                OverlayStyle.button(control);
            overlaysMenuButton.setBorder(new EmptyBorder(5, 6, 5, 6));
            overlaysButton.setSelectedIcon(LucideIcons.icon("eye", 16, OverlayStyle.ON_ACCENT));
            addButton.setSelectedIcon(LucideIcons.icon("plus", 16, OverlayStyle.ON_ACCENT));
            typeCard.setName("CityTool.TypeCard");
            typeCard.setToolTipText("Type of the selected houses (or the next house): click to choose another");
            typeCard.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent event) {
                    openTypeGrid();
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
                        : "Outline every house of the layer");
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

        void openTypeGrid() {
            CityLayer layer = getSelectedLayer();
            if (layer == null || layer.getObjectList().isEmpty())
                return;
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
            addButton.setBounds(x, 8, addButton.getPreferredSize().width, height);
            var card = typeCard.getPreferredSize();
            typeCard.setBounds(8, toolbarBottom() + 8, card.width, card.height);
            if (typeGrid.isVisible())
                typeGrid.setBounds(0, 0, getWidth(), getHeight());
            for (PathTool.DropdownButton dropdown : List.of(schematicsButton, layerButton))
                dropdown.validate();
            repaint();
        }

        int toolbarBottom() {
            return schematicsButton.getY() + schematicsButton.getHeight();
        }

        @Override
        public void setBounds(int x, int y, int width, int height) {
            super.setBounds(x, y, width, height);
            layoutToolbar();
        }

        /** Mouse-transparent like the base overlay, except over the HUD controls. */
        @Override
        public boolean contains(int x, int y) {
            if (typeGrid.isVisible())
                return true; // the picker covers the whole viewport
            for (Component control : List.of(schematicsButton, layerButton, overlaysButton, overlaysMenuButton,
                    addButton, typeCard))
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
                g.draw(toView(footprint(layer, hovered.house())));
            }
            if (selectedStates.isEmpty())
                return;
            if (gestureTransform != null) {
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
                g.draw(toView(footprint(layer, house)));
            paintTransformBox(g, layer, shown);
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
                    g.draw(bounds);
            }
        }

        /**
         * Transform box around the selection: rotate handle above, move handle left,
         * scale handles on the corner and edges and the pivot (scale and pivot only for
         * several houses: one house has nothing to spread).
         */
        private void paintTransformBox(Graphics2D g, CityLayer layer, List<ObjectState> shown) {
            var box = transformBox(layer, shown);
            if (box == null)
                return;
            g.setStroke(new BasicStroke(1.5f));
            g.setColor(OverlayStyle.TEXT);
            g.draw(box);
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Line2D.Double(PathTransformBox.topCentre(box), PathTransformBox.rotateHandle(box)));
            g.draw(new Line2D.Double(PathTransformBox.leftCentre(box), PathTransformBox.moveHandle(box)));
            PathTool.paintIconHandle(g, "rotate-ccw", PathTransformBox.rotateHandle(box), 0,
                    isHovered(HitKind.ROTATE, 0));
            PathTool.paintIconHandle(g, "move", PathTransformBox.moveHandle(box), 0, isHovered(HitKind.MOVE, 0));
            if (selectedStates.size() < 2)
                return;
            PathTool.paintIconHandle(g, "move-diagonal-2",
                    PathTransformBox.scaleHandle(box, PathTransformBox.SCALE_UNIFORM), 0,
                    isHovered(HitKind.SCALE, PathTransformBox.SCALE_UNIFORM));
            PathTool.paintIconHandle(g, "move-horizontal", PathTransformBox.scaleHandle(box, PathTransformBox.SCALE_X),
                    0, isHovered(HitKind.SCALE, PathTransformBox.SCALE_X));
            PathTool.paintIconHandle(g, "move-vertical", PathTransformBox.scaleHandle(box, PathTransformBox.SCALE_Y), 0,
                    isHovered(HitKind.SCALE, PathTransformBox.SCALE_Y));
            Point2D.Double pivot = pivotWorld();
            if (pivot != null)
                PathTool.paintIconHandle(g, "crosshair", toViewExact(pivot), 0, isHovered(HitKind.PIVOT, 0));
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
            int x = Math.max((getWidth() - width) / 2, cardRight + 8);
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
        TypeCard() {
            setOpaque(false);
            setBorder(new EmptyBorder(8, 8, 8, 8));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        private String title() {
            CityLayer layer = getSelectedLayer();
            if (layer == null || uiState.objectIndex >= layer.getObjectList().size())
                return "";
            return schematicName(layer.getObjectList().get(uiState.objectIndex), uiState.objectIndex);
        }

        private String subtitle() {
            String mirrored = uiState.mirrored ? ", mirrored" : "";
            String rotation = uiState.rotation.toCompass() + "°" + mirrored;
            return selectedStates.size() > 1 ? selectedStates.size() + " selected · " + rotation : rotation;
        }

        @Override
        public java.awt.Dimension getPreferredSize() {
            var titleMetrics = getFontMetrics(OverlayStyle.font(Font.BOLD, 13f));
            var subtitleMetrics = getFontMetrics(OverlayStyle.font(Font.PLAIN, 12f));
            int text = Math.max(titleMetrics.stringWidth(title()), subtitleMetrics.stringWidth(subtitle()));
            int width = Math.min(320, Math.max(CARD_THUMBNAIL, text) + 16);
            return new java.awt.Dimension(width,
                    CARD_THUMBNAIL + 16 + titleMetrics.getHeight() + subtitleMetrics.getHeight() + 4);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            CityLayer layer = getSelectedLayer();
            if (layer == null)
                return;
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                OverlayStyle.paintSurface(g, 0, 0, getWidth(), getHeight(), OverlayStyle.SURFACE);
                Image image = layer.getSchematicImage(uiState);
                if (image != null) {
                    int width = image.getWidth(null), height = image.getHeight(null);
                    double scale = Math.min((double) CARD_THUMBNAIL / width, (double) CARD_THUMBNAIL / height);
                    int w = Math.max(1, (int) Math.round(width * scale)),
                            h = Math.max(1, (int) Math.round(height * scale));
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                    g.drawImage(image, (getWidth() - w) / 2, 8 + (CARD_THUMBNAIL - h) / 2, w, h, null);
                }
                int y = 8 + CARD_THUMBNAIL + 4;
                g.setFont(OverlayStyle.font(Font.BOLD, 13f));
                var metrics = g.getFontMetrics();
                g.setColor(OverlayStyle.TEXT);
                g.drawString(clip(title(), metrics, getWidth() - 16), 8, y + metrics.getAscent());
                y += metrics.getHeight();
                g.setFont(OverlayStyle.font(Font.PLAIN, 12f));
                metrics = g.getFontMetrics();
                g.setColor(OverlayStyle.TEXT_MUTED);
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
    private class TypeGrid extends JComponent
    {
        private static final int MARGIN = 32, HEADER = 84, GAP = 12, PICTURE = 126;
        private static final int TILE_HEIGHT = GRID_TILE + 22;
        /** Room left of and below the picture for its measurements (px). */
        private static final int MEASURE_LEFT = 34, MEASURE_BOTTOM = 22;
        private final List<Image> pictures = new ArrayList<>();
        private final List<String> names = new ArrayList<>();
        /** Width (x), length (y) and height (z) in blocks per schematic. */
        private final List<int[]> sizes = new ArrayList<>();
        private int hover = -1;
        private int scroll;

        TypeGrid() {
            setOpaque(false);
            var mouse = new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent event) {
                    int index = tileAt(event.getPoint());
                    if (index != hover) {
                        hover = index;
                        setCursor(Cursor.getPredefinedCursor(index >= 0 ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                        repaint();
                    }
                }

                @Override
                public void mouseExited(MouseEvent event) {
                    hover = -1;
                    repaint();
                }

                @Override
                public void mouseClicked(MouseEvent event) {
                    if (!SwingUtilities.isLeftMouseButton(event))
                        return;
                    int index = tileAt(event.getPoint());
                    if (index >= 0)
                        pick(index);
                    else
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
            pictures.clear();
            names.clear();
            sizes.clear();
            var objects = layer.getObjectList();
            for (int i = 0; i < objects.size(); i++) {
                pictures.add(layer.getSchematicImage(new ObjectState(CityLayer.Direction.NORTH, false, i, 0, 0)));
                names.add(schematicName(objects.get(i), i));
                var dimensions = objects.get(i).getDimensions();
                sizes.add(new int[]{dimensions.x, dimensions.y, dimensions.z});
            }
            hover = -1;
            scroll = 0;
        }

        void pick(int index) {
            setType(index);
            overlay.closeTypeGrid();
        }

        private int columns() {
            return Math.max(1, (getWidth() - 2 * MARGIN + GAP) / (GRID_TILE + GAP));
        }

        private Rectangle tileBounds(int index) {
            int columns = columns();
            int gridWidth = columns * GRID_TILE + (columns - 1) * GAP;
            int left = Math.max(MARGIN, (getWidth() - gridWidth) / 2);
            int row = index / columns, column = index % columns;
            return new Rectangle(left + column * (GRID_TILE + GAP), HEADER + row * (TILE_HEIGHT + GAP) - scroll,
                    GRID_TILE, TILE_HEIGHT);
        }

        private int maxScroll() {
            int rows = (pictures.size() + columns() - 1) / columns();
            int content = rows * (TILE_HEIGHT + GAP) - GAP;
            return Math.max(0, content - (getHeight() - HEADER - MARGIN));
        }

        private int tileAt(Point point) {
            if (point.y < HEADER - GAP)
                return -1;
            for (int i = 0; i < pictures.size(); i++)
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
                g.setColor(new Color(22, 22, 22, 235));
                g.fillRect(0, 0, getWidth(), getHeight());

                g.setFont(OverlayStyle.font(Font.BOLD, 20f));
                g.setColor(OverlayStyle.TEXT);
                g.drawString("Choose a schematic", MARGIN, 40);
                g.setFont(OverlayStyle.font(Font.PLAIN, 13f));
                g.setColor(OverlayStyle.TEXT_MUTED);
                int selected = selectedStates.size();
                String target = selected > 1
                        ? "For the " + selected + " selected houses"
                        : selected == 1 ? "For the selected house" : "For the next house you place";
                g.drawString(target + " · Esc or a click outside closes", MARGIN, 62);
                LucideIcons.paint(g, "x", new Point2D.Double(getWidth() - MARGIN, 36), 20, 0, OverlayStyle.TEXT_MUTED,
                        null);

                g.clipRect(0, HEADER - GAP, getWidth(), getHeight());
                for (int i = 0; i < pictures.size(); i++) {
                    Rectangle tile = tileBounds(i);
                    if (tile.y > getHeight() || tile.y + tile.height < HEADER - GAP)
                        continue;
                    paintTile(g, i, tile);
                }
            } finally {
                g.dispose();
            }
        }

        private void paintTile(Graphics2D g, int index, Rectangle tile) {
            boolean current = index == uiState.objectIndex, hovered = index == hover;
            OverlayStyle.paintSurface(g, tile.x, tile.y, tile.width, tile.height,
                    current
                            ? OverlayStyle.ACCENT_SOFT_SOLID
                            : hovered ? OverlayStyle.SURFACE_HOVER : OverlayStyle.SURFACE);
            if (current || hovered) {
                g.setColor(current ? OverlayStyle.ACCENT : OverlayStyle.TEXT);
                g.setStroke(new BasicStroke(current ? 2f : 1f));
                g.draw(OverlayStyle.rounded(tile.x + 1, tile.y + 1, tile.width - 2, tile.height - 2));
            }
            Image picture = pictures.get(index);
            int[] size = sizes.get(index);
            if (picture != null) {
                // the picture shows the house with a margin of CityLayer's image space;
                // the measurements span the house itself
                int width = picture.getWidth(null), height = picture.getHeight(null);
                int areaX = tile.x + MEASURE_LEFT, areaY = tile.y + 10;
                int areaWidth = tile.width - MEASURE_LEFT - 10, areaHeight = PICTURE - MEASURE_BOTTOM + 8;
                double scale = Math.min((double) areaWidth / width, (double) areaHeight / height);
                int w = Math.max(1, (int) Math.round(width * scale)), h = Math.max(1, (int) Math.round(height * scale));
                int imageX = areaX + (areaWidth - w) / 2, imageY = areaY + (areaHeight - h) / 2;
                var quality = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g.drawImage(picture, imageX, imageY, w, h, null);
                if (quality != null)
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, quality);
                double margin = (width - size[0]) / 2.0 * scale;
                double marginY = (height - size[1]) / 2.0 * scale;
                paintMeasurements(g,
                        new Rectangle2D.Double(imageX + margin, imageY + marginY, size[0] * scale, size[1] * scale),
                        size);
            }
            int y = tile.y + 10 + PICTURE + 8;
            g.setFont(OverlayStyle.font(Font.BOLD, 13f));
            var metrics = g.getFontMetrics();
            g.setColor(OverlayStyle.TEXT);
            g.drawString(TypeCard.clip(names.get(index), metrics, tile.width - 16), tile.x + 8,
                    y + metrics.getAscent());
        }

        /**
         * Dimension lines like a technical drawing: the width (x) below the house, its
         * length (y) on the left, the height in the corner between them ("12h").
         */
        private void paintMeasurements(Graphics2D g, Rectangle2D.Double house, int[] size) {
            g.setFont(OverlayStyle.font(Font.PLAIN, 11.5f));
            var metrics = g.getFontMetrics();
            g.setStroke(new BasicStroke(1f));
            g.setColor(OverlayStyle.TEXT_MUTED);
            int tick = 3;
            // width: a line below the house with end ticks, the number under its middle
            double lineY = house.getMaxY() + 6;
            g.draw(new Line2D.Double(house.getMinX(), lineY, house.getMaxX(), lineY));
            g.draw(new Line2D.Double(house.getMinX(), lineY - tick, house.getMinX(), lineY + tick));
            g.draw(new Line2D.Double(house.getMaxX(), lineY - tick, house.getMaxX(), lineY + tick));
            String width = Integer.toString(size[0]);
            float textY = (float) (lineY + 3 + metrics.getAscent());
            g.drawString(width, (float) (house.getCenterX() - metrics.stringWidth(width) / 2.0), textY);
            // length: a line left of the house, the number left of its middle
            double lineX = house.getMinX() - 6;
            g.draw(new Line2D.Double(lineX, house.getMinY(), lineX, house.getMaxY()));
            g.draw(new Line2D.Double(lineX - tick, house.getMinY(), lineX + tick, house.getMinY()));
            g.draw(new Line2D.Double(lineX - tick, house.getMaxY(), lineX + tick, house.getMaxY()));
            String length = Integer.toString(size[1]);
            g.drawString(length, (float) (lineX - 4 - metrics.stringWidth(length)),
                    (float) (house.getCenterY() + metrics.getAscent() / 2.0 - 1));
            // height: in the bottom left corner, level with the width
            String height = size[2] + "h";
            g.setColor(OverlayStyle.TEXT);
            g.drawString(height, (float) (lineX - 4 - metrics.stringWidth(height)), textY);
        }
    }
}