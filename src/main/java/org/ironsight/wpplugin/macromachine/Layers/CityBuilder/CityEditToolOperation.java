package org.ironsight.wpplugin.macromachine.Layers.CityBuilder;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyVetoException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.function.UnaryOperator;
import javax.swing.*;
import javax.vecmath.Point3i;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.SVGLoader;
import org.ironsight.wpplugin.macromachine.Gui.GlobalActionPanel;
import org.ironsight.wpplugin.macromachine.Gui.MapToolOverlay;
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
 */
public class CityEditToolOperation extends MouseOrTabletOperation implements PaintOperation, KeyEventDispatcher
{
    private static final int OVERLAY_ICON_SIZE = 256;
    private static CityEditToolOperation instance;
    record PlacementOptions(boolean randomRotate, boolean randomSelect, boolean randomMirror) {
    }
    private record ClipboardEntry(CityLayer.Direction rotation, boolean mirrored, int objectIndex, long offsetX,
            long offsetY) {
    }

    private final OptionsPanel optionsPanel;
    Random random = new Random();
    private ObjectState uiState = new ObjectState(CityLayer.Direction.NORTH, false, 0, Integer.MAX_VALUE,
            Integer.MAX_VALUE);
    private PlacementOptions placementOptions = new PlacementOptions(false, false, false);
    private final Map<Point, ObjectState> selectedStates = new LinkedHashMap<>();
    private final Map<Point, Long> selectedOutlineIds = new HashMap<>();
    private List<ClipboardEntry> clipboard = List.of();
    private CityLayer clipboardLayer;
    private volatile Point cursorWorldPosition;
    private Timer statusMessageTimer;
    private boolean updatingPanelSelection;

    private Paint paint;

    private WorldPainterView overlayView;
    private volatile boolean cursorOverMap;
    private final MapToolOverlay dragOverlay = new MapToolOverlay(loadOverlayIcon());
    private TiledImageViewer.ViewListener previousViewListener;
    private final TiledImageViewer.ViewListener overlayViewListener = changedView -> {
        if (previousViewListener != null)
            previousViewListener.viewChanged(changedView);
        dragOverlay.repaint();
    };
    private final java.awt.event.ComponentAdapter overlayResizeListener = new java.awt.event.ComponentAdapter() {
        @Override
        public void componentResized(java.awt.event.ComponentEvent event) {
            resizeDragOverlay();
        }
    };

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
        int color = Color.WHITE.getRGB() & 0x00ffffff;
        for (int y = 0; y < icon.getHeight(); y++) {
            for (int x = 0; x < icon.getWidth(); x++) {
                int argb = icon.getRGB(x, y);
                icon.setRGB(x, y, (argb & 0xff000000) | color);
            }
        }
        return icon;
    }

    public CityEditToolOperation() {
        super("City Tool", "Edit city layers using this tool", "city-edit-tool-operation");
        instance = this;
        optionsPanel = new OptionsPanel(this::setPlacementOptions, this::onObjectSelectionChanged,
                this::setUseHighlightColors, this::getSelectedLayer, () -> uiState);

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

        Toolkit.getDefaultToolkit().addAWTEventListener(e -> {
            if (!(e instanceof MouseEvent event))
                return;
            cursorOverMap = isMapComponent(overlayView, event.getComponent());
            if (!isActive() || !cursorOverMap)
                return;

            Point viewPoint = SwingUtilities.convertPoint(event.getComponent(), event.getPoint(), overlayView);
            if (event.getID() == MouseEvent.MOUSE_EXITED && !new Rectangle(overlayView.getSize()).contains(viewPoint)) {
                cursorOverMap = false;
                cursorWorldPosition = null;
                return;
            }
            cursorWorldPosition = overlayView.viewToWorld(viewPoint);
            MapToolOverlay overlay = dragOverlay;
            switch (event.getID()) {
                case MouseEvent.MOUSE_PRESSED -> {
                    if (SwingUtilities.isLeftMouseButton(event))
                        overlay.startDrag(viewPoint);
                }
                case MouseEvent.MOUSE_DRAGGED -> {
                    if (overlay.isDragging())
                        overlay.updateDrag(viewPoint);
                }
                case MouseEvent.MOUSE_RELEASED -> {
                    if (SwingUtilities.isLeftMouseButton(event)) {
                        if (overlay.isBoxSelection()) {
                            CityLayer layer = getSelectedLayer();
                            if (layer != null)
                                selectWithinBox(layer, overlay.getDragBounds());
                        }
                        overlay.endDrag();
                    }
                }
                default -> {
                }
            }
        }, AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);

        KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();

        // Add a global key event dispatcher
        manager.addKeyEventDispatcher(this);
    }

    void setPlacementOptions(PlacementOptions placementOptions) {
        this.placementOptions = Objects.requireNonNull(placementOptions);
        optionsPanel.setPlacementOptions(placementOptions);
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
            boolean requiresSelection = keyCode == KeyEvent.VK_Q || keyCode == KeyEvent.VK_W || keyCode == KeyEvent.VK_A
                    || keyCode == KeyEvent.VK_S || keyCode == KeyEvent.VK_D || keyCode == KeyEvent.VK_C
                    || keyCode == KeyEvent.VK_X;
            if (requiresSelection && (layer == null || selectedStates.isEmpty()))
                return;

            switch (keyCode) {
                case KeyEvent.VK_Q -> applyToSelection(layer, this::randomizeState);
                case KeyEvent.VK_DELETE -> deleteSelected();
                case KeyEvent.VK_ESCAPE -> {
                    if (layer != null)
                        deselect(layer);
                }
                case KeyEvent.VK_W -> moveSelection(layer, 0, -1);
                case KeyEvent.VK_S -> moveSelection(layer, 0, 1);
                case KeyEvent.VK_A -> moveSelection(layer, -1, 0);
                case KeyEvent.VK_D -> moveSelection(layer, 1, 0);
                case KeyEvent.VK_C -> rotateSelection(layer);
                case KeyEvent.VK_X -> applyToSelection(layer, state -> setIsMirrored(!state.mirrored, state));
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

    private void selectAll(CityLayer layer) {
        if (layer == null)
            return;

        clearSelection();
        for (ObjectState state : layer.getAllObjectStates())
            addSelectedState(state, layer);

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

    void handleClick(int centreX, int centreY, boolean rightClick, boolean ctrlDown) {
        cursorWorldPosition = new Point(centreX, centreY);
        CityLayer layer = getSelectedLayer();
        if (layer == null)
            return;

        if (ctrlDown && !rightClick) {
            placeAt(centreX, centreY);
        } else if (rightClick) {
            moveSelectionTo(layer, centreX, centreY);
        } else {
            onPickAt(centreX, centreY, layer);
        }
    }

    /** Places one building at the coordinates */
    void placeAt(int centreX, int centreY) {
        cursorWorldPosition = new Point(centreX, centreY);
        CityLayer layer = getSelectedLayer();
        if (layer != null)
            onAddAt(centreX, centreY, layer);
    }

    /**
     * select the next schematic from the list, apply.
     *
     * @param direction
     *            up (dir<0) or down (dir>0) wheel
     */
    void onMouseWheel(int direction) {
        int max = optionsPanel.getObjectCount();
        if (max == 0)
            return;
        var oldState = uiState;
        int nextIdx = Math.clamp(oldState.objectIndex + direction, 0, max - 1);
        System.out.println("change index by direction " + direction);
        CityLayer layer = getSelectedLayer();
        if (layer != null && !selectedStates.isEmpty()) {
            applyToSelection(layer, state -> setSelectedObjectIndex(nextIdx, state));
        } else {
            applyToUi(setSelectedObjectIndex(nextIdx, uiState));
        }
    }

    @Override
    public void interrupt() {
    }

    private void attachDragOverlay() {
        WorldPainterView view = getView();
        if (view == null || overlayView == view)
            return;
        detachDragOverlay();
        overlayView = view;
        if (view != null) {
            dragOverlay.setMapView(view);
            previousViewListener = view.getViewListener();
            view.setViewListener(overlayViewListener);
            view.addComponentListener(overlayResizeListener);
            view.add(dragOverlay);
            view.setComponentZOrder(dragOverlay, 0);
            resizeDragOverlay();
            dragOverlay.showOverlayText("City Tool");
        }
    }

    private void resizeDragOverlay() {
        if (overlayView != null)
            dragOverlay.setBounds(0, 0, overlayView.getWidth(), overlayView.getHeight());
    }

    private void detachDragOverlay() {
        cursorOverMap = false;
        cursorWorldPosition = null;
        if (statusMessageTimer != null) {
            statusMessageTimer.stop();
            statusMessageTimer = null;
        }
        optionsPanel.setStatusMessage(null);
        dragOverlay.clearOverlayText();
        dragOverlay.clearInfoText();
        if (overlayView == null)
            return;
        overlayView.removeComponentListener(overlayResizeListener);
        overlayView.remove(dragOverlay);
        if (overlayView.getViewListener() == overlayViewListener)
            overlayView.setViewListener(previousViewListener);
        overlayView.repaint();
        clearSelectedObjectOutlines();
        selectedStates.clear();
        dragOverlay.clearOutlines();
        dragOverlay.setMapView(null);
        previousViewListener = null;
        overlayView = null;
    }

    private void updateSelectedObjectOutlines(CityLayer layer) {
        clearSelectedObjectOutlines();
        for (ObjectState state : selectedStates.values()) {
            WPObject object = layer.getObjectForState(state);
            if (object == null)
                continue;
            Point3i dimensions = object.getDimensions();
            Point3i offset = object.getOffset();
            Point anchor = new Point(state.xPos, state.yPos);
            selectedOutlineIds.put(anchor, dragOverlay.addOutline(
                    new Rectangle(state.xPos + offset.x, state.yPos + offset.y, dimensions.x, dimensions.y)));
        }
    }

    private void clearSelectedObjectOutlines() {
        for (long outlineId : selectedOutlineIds.values())
            dragOverlay.removeOutline(outlineId);
        selectedOutlineIds.clear();
    }

    private void addSelectedState(ObjectState state, CityLayer layer) {
        Point anchor = new Point(state.xPos, state.yPos);
        selectedStates.put(anchor, state);
        updateSelectionCount();
        WPObject object = layer.getObjectForState(state);
        if (object == null)
            return;
        Point3i dimensions = object.getDimensions();
        Point3i offset = object.getOffset();
        selectedOutlineIds.put(anchor, dragOverlay
                .addOutline(new Rectangle(state.xPos + offset.x, state.yPos + offset.y, dimensions.x, dimensions.y)));
    }

    private void removeSelectedState(ObjectState state) {
        Point anchor = new Point(state.xPos, state.yPos);
        Long outlineId = selectedOutlineIds.remove(anchor);
        if (outlineId != null)
            dragOverlay.removeOutline(outlineId);
        selectedStates.remove(anchor);
        updateSelectionCount();
    }

    private void clearSelection() {
        selectedStates.clear();
        clearSelectedObjectOutlines();
        updateSelectionCount();
    }

    private void updateSelectionCount() {
        optionsPanel.setSelectionCount(selectedStates.size());
    }

    void selectWithinBox(CityLayer layer, Rectangle selectionBounds) {
        clearSelection();
        for (ObjectState state : layer.getAllObjectStates()) {
            WPObject object = layer.getObjectForState(state);
            if (object == null)
                continue;
            Point3i dimensions = object.getDimensions();
            Point3i offset = object.getOffset();
            Rectangle objectBounds = new Rectangle(state.xPos + offset.x, state.yPos + offset.y, dimensions.x,
                    dimensions.y);
            if (selectionBounds.contains(objectBounds))
                addSelectedState(state, layer);
        }

        if (selectedStates.isEmpty()) {
            deselect(layer);
            showStatusMessage(selectionStatusMessage());
        } else {
            uiState = new ArrayList<>(selectedStates.values()).getLast();
            applyToUi(uiState);
            refreshLayer(layer);
            showStatusMessage(selectionStatusMessage());
        }
    }

    private void selectOnly(CityLayer layer, ObjectState state) {
        clearSelection();
        addSelectedState(state, layer);
        applyToUi(state);
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

    private float dist(int x1, int y1, int x2, int y2) {
        int dx = x1 - x2;
        int dy = y1 - y2;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    @Override
    protected void tick(int centreX, int centreY, boolean inverse, boolean first, float dynamicLevel) {
        if (!getDimension().isEventsInhibited())
            getDimension().setEventsInhibited(true);
        if (getPaint() instanceof LayerPaint layerPaint && layerPaint.getLayer() instanceof CityLayer cityLayer) {
            ensureLayerHasUndoManager(cityLayer, getDimension());
            handleClick(centreX, centreY, inverse, this.isCtrlDown());
        }
        if (getDimension().isEventsInhibited())
            getDimension().setEventsInhibited(false);
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
        attachDragOverlay();
        updatePanel();
    }

    @Override
    protected void deactivate() {
        dragOverlay.endDrag();
        detachDragOverlay();
        super.deactivate();
    }

    protected void paintChanged(Paint ignored) {
        clearSelection();
        clearClipboard();
        updatePanel();
        updateOverlayText();
    }

    private void updateOverlayText() {
        if (overlayView != null)
            dragOverlay.setOverlayText("City Tool");
        else
            dragOverlay.clearOverlayText();
    }

    private void applyToUi(ObjectState uiState) {
        // deliberate let uiState == this.uiState pass, brush radius must be forced back
        // to current selected obj.

        System.out.println("### City Tool UI state changed to:" + uiState);
        CityLayer layer = getSelectedLayer();
        if (layer == null)
            return;

        this.uiState = uiState;

        // update list
        updatingPanelSelection = true;
        try {
            optionsPanel.setSelectedIndex(uiState.objectIndex);
        } finally {
            updatingPanelSelection = false;
        }

        optionsPanel.revalidate();
        optionsPanel.repaint();
    }

    private void applyToMapAndUI(CityLayer layer, ObjectState newState, ObjectState oldState) {
        if (newState.equals(oldState))
            return;
        if (layer == null)
            return;
        if (oldState != null)
            layer.removeDataAt(getDimension(), oldState.xPos, oldState.yPos);
        layer.setDataAt(getDimension(), newState.xPos, newState.yPos, newState);
        selectOnly(layer, newState);
        if (getViewAsWP() != null) { // force a tile renderer update //FIXME use less frequently, this will force ALL
                                     // tiles to be rerendered.
            getViewAsWP().refreshTilesForLayer(layer, false);
        }
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
        updateSelectionCount();
        updateSelectedObjectOutlines(layer);

        uiState = newStates.get(Math.min(activeIndex, newStates.size() - 1));
        applyToUi(uiState);
        refreshLayer(layer);
    }

    private void moveSelection(CityLayer layer, int deltaX, int deltaY) {
        applyToSelection(layer, state -> setCurrentStatePosition(state.xPos + deltaX, state.yPos + deltaY, state));
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

    private ObjectState rotateStateAround(ObjectState state, Point center) {
        long relativeX = (long) state.xPos - center.x;
        long relativeY = (long) state.yPos - center.y;
        int x = Math.toIntExact((long) center.x + relativeY);
        int y = Math.toIntExact((long) center.y - relativeX);
        return new ObjectState(state.rotation.nextRotation(), state.mirrored, state.objectIndex, x, y);
    }

    private void refreshLayer(CityLayer layer) {
        if (getViewAsWP() != null)
            getViewAsWP().refreshTilesForLayer(layer, false);
    }

    private void onPickAt(int centreX, int centreY, CityLayer cityLayer) {
        int radius = getSelectionRadius(cityLayer);
        ObjectState selectedState = null;
        float lastDist = Float.MAX_VALUE;
        for (int x = centreX - radius; x < centreX + radius; x++) {
            for (int y = centreY - radius; y < centreY + radius; y++) {
                ObjectState state = cityLayer.getInformationAt(x, y);
                if (state == null)
                    continue;
                WPObject object = cityLayer.getObjectForState(state);
                if (object == null)
                    continue;
                Point3i dimensions = object.getDimensions();
                Point3i offset = object.getOffset();
                Rectangle bounds = new Rectangle(state.xPos + offset.x, state.yPos + offset.y, dimensions.x,
                        dimensions.y);
                if (!bounds.contains(centreX, centreY))
                    continue;

                float currentDist = dist(centreX, centreY, x, y);
                if (currentDist < lastDist) {
                    lastDist = currentDist;
                    selectedState = state;
                }
            }
        }
        if (selectedState != null) {
            Point anchor = new Point(selectedState.xPos, selectedState.yPos);
            if (selectedStates.containsKey(anchor)) {
                removeSelectedState(selectedState);
                if (selectedStates.isEmpty()) {
                    deselect(cityLayer);
                } else {
                    uiState = new ArrayList<>(selectedStates.values()).getLast();
                    applyToUi(uiState);
                    showStatusMessage(selectionStatusMessage());
                }
            } else {
                addSelectedState(selectedState, cityLayer);
                uiState = selectedState;
                applyToUi(uiState);
                showStatusMessage(selectionStatusMessage());
            }
        } else {
            deselect(cityLayer);
            showStatusMessage(selectionStatusMessage());
        }
    }

    private int getSelectionRadius(CityLayer cityLayer) {
        int radius = 1;
        for (WPObject object : cityLayer.getObjectList()) {
            Point3i dimensions = object.getDimensions();
            radius = Math.max(radius, Math.max(dimensions.x, dimensions.y));
        }
        return radius;
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
        updateClipboardCount();
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
            addSelectedState(state, layer);
        uiState = newStates.getFirst();
        applyToUi(uiState);
        refreshLayer(layer);
        showStatusMessage(objectCountMessage(newStates.size(), "pasted from clipboard"));
    }

    private void clearClipboard() {
        clipboard = List.of();
        clipboardLayer = null;
        updateClipboardCount();
    }

    private void updateClipboardCount() {
        optionsPanel.setClipboardCount(clipboard.size());
    }

    private static String objectCountMessage(int count, String suffix) {
        return count == 1 ? "1 object " + suffix : count + " objects " + suffix;
    }

    private String selectionStatusMessage() {
        return selectedStates.isEmpty() ? "No objects selected" : objectCountMessage(selectedStates.size(), "selected");
    }

    private void showStatusMessage(String message) {
        optionsPanel.setStatusMessage(message);
        if (statusMessageTimer != null)
            statusMessageTimer.stop();
        statusMessageTimer = new Timer(3000, event -> {
            if (statusMessageTimer != event.getSource())
                return;
            statusMessageTimer = null;
            optionsPanel.setStatusMessage(null);
        });
        statusMessageTimer.setRepeats(false);
        statusMessageTimer.start();
        if (overlayView != null)
            dragOverlay.showTransientInfoText(message, 3000);
    }

    void setCursorWorldPosition(int x, int y) {
        cursorWorldPosition = new Point(x, y);
    }

    private void onAddAt(int centreX, int centreY, CityLayer cityLayer) {
        var newState = setCurrentStatePosition(centreX, centreY, uiState);
        applyToMapAndUI(cityLayer, newState, null);
    }

    private ObjectState randomizeState(ObjectState oldState) {
        ObjectState newState = oldState;
        if (placementOptions.randomRotate()) {
            newState = setRotation(CityLayer.Direction.fromCompass(random.nextInt(4) * 90), newState);
        }
        if (placementOptions.randomSelect()) {
            newState = setSelectedObjectIndex(random.nextInt(optionsPanel.getObjectCount()), newState);
        }
        if (placementOptions.randomMirror()) {
            newState = setIsMirrored(random.nextBoolean(), newState);
        }
        return newState;
    }

    private ObjectState setRotation(CityLayer.Direction rotation, ObjectState oldState) {
        if (rotation == oldState.rotation)
            return oldState;
        System.out.println("set rotation from" + oldState.rotation + " to " + rotation);
        return new ObjectState(rotation, oldState.mirrored, oldState.objectIndex, oldState.xPos, oldState.yPos);
    }

    private ObjectState setIsMirrored(boolean mirrored, ObjectState oldState) {
        return new ObjectState(oldState.rotation, mirrored, oldState.objectIndex, oldState.xPos, oldState.yPos);
    }

    private ObjectState setSelectedObjectIndex(int index, ObjectState oldState) {
        if (index == oldState.objectIndex)
            return oldState;
        if (index < 0 || index >= getSelectedLayer().getObjectList().size())
            return oldState;

        return new ObjectState(oldState.rotation, oldState.mirrored, index, oldState.xPos, oldState.yPos);
    }

    private void onObjectSelectionChanged(int index) {
        if (updatingPanelSelection)
            return;
        CityLayer layer = getSelectedLayer();
        if (layer != null && !selectedStates.isEmpty()) {
            applyToSelection(layer, state -> setSelectedObjectIndex(index, state));
        } else {
            applyToUi(setSelectedObjectIndex(index, uiState));
        }
    }

    private void setUseHighlightColors(boolean selected) {
        CityLayer layer = getSelectedLayer();
        if (layer == null)
            return;
        layer.setUseHighlightColors(selected);
        if (getViewAsWP() != null)
            getViewAsWP().refreshTilesForLayer(layer, false);
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
        updateSelectionCount();
        updateClipboardCount();
        if (getPaint() instanceof LayerPaint layerPaint && layerPaint.getLayer() instanceof CityLayer cityLayer) {
            optionsPanel.setObjects(cityLayer.getObjectList());
            applyToUi(setSelectedObjectIndex(0, uiState)); // some safety thing to always be inside of list bound?
            optionsPanel.setHighlightColorsSelected(cityLayer.isUseHighlightColors());
            optionsPanel.showLayer(true);
        } else {
            optionsPanel.showLayer(false);
        }
        optionsPanel.revalidate();
        optionsPanel.repaint();
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

}
