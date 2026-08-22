package org.ironsight.wpplugin.macromachine.Layers.CityBuilder;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.beans.PropertyVetoException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;
import java.util.Random;
import javax.swing.*;

import org.ironsight.wpplugin.macromachine.Gui.GlobalActionPanel;
import org.pepsoft.util.swing.TiledImageViewer;
import org.pepsoft.util.undo.UndoManager;
import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.brushes.Brush;
import org.pepsoft.worldpainter.brushes.RotatedBrush;
import org.pepsoft.worldpainter.brushes.SymmetricBrush;
import org.pepsoft.worldpainter.objects.WPObject;
import org.pepsoft.worldpainter.operations.AbstractBrushOperation;
import org.pepsoft.worldpainter.operations.PaintOperation;
import org.pepsoft.worldpainter.painting.LayerPaint;
import org.pepsoft.worldpainter.painting.NibbleLayerPaint;
import org.pepsoft.worldpainter.painting.Paint;

/**
 */
public class CityEditToolOperation extends AbstractBrushOperation implements PaintOperation, KeyEventDispatcher
{
    private static CityEditToolOperation instance;
    record PlacementOptions(boolean randomRotate, boolean randomSelect, boolean randomMirror) {
    }

    private final OptionsPanel optionsPanel;
    Random random = new Random();
    private ObjectState uiState = new ObjectState(CityLayer.Direction.NORTH, false, 0, Integer.MAX_VALUE,
            Integer.MAX_VALUE);
    private PlacementOptions placementOptions = new PlacementOptions(false, false, false);

    private Paint paint;
    private CityLayer lastLayer = null;

    private WorldPainterView overlayView;
    private final JComponent dragOverlay = new DragOverlay();
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
            if (!(e instanceof MouseEvent event) || !isActive() || overlayView == null)
                return;
            if (!SwingUtilities.isDescendingFrom(event.getComponent(), overlayView)
                    && event.getComponent() != overlayView)
                return;

            Point viewPoint = SwingUtilities.convertPoint(event.getComponent(), event.getPoint(), overlayView);
            DragOverlay overlay = (DragOverlay) dragOverlay;
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
                    if (SwingUtilities.isLeftMouseButton(event))
                        overlay.endDrag();
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
            if (e.isShiftDown() || e.isControlDown() || e.isAltDown() || e.isMetaDown())
                return false;
            handleKeyInteraction(e.getKeyCode());
        }
        return false; // return false to allow other listeners to handle the event
    }

    /** Applies one unmodified keyboard interaction from the city tool. */
    void handleKeyInteraction(int keyCode) {
        try {
            if (!getDimension().isEventsInhibited())
                getDimension().setEventsInhibited(true);
            var oldState = uiState;
            CityLayer layer = getSelectedLayer();
            boolean requiresSelection = keyCode == KeyEvent.VK_Q || keyCode == KeyEvent.VK_W || keyCode == KeyEvent.VK_A
                    || keyCode == KeyEvent.VK_S || keyCode == KeyEvent.VK_D || keyCode == KeyEvent.VK_C
                    || keyCode == KeyEvent.VK_X;
            if (requiresSelection && (layer == null || layer.getInformationAt(oldState.xPos, oldState.yPos) == null))
                return;

            ObjectState newState;
            switch (keyCode) {
                case KeyEvent.VK_Q -> newState = randomizeState(oldState);
                case KeyEvent.VK_DELETE -> {
                    deleteSelected();
                    return;
                }
                case KeyEvent.VK_W -> newState = setCurrentStatePosition(oldState.xPos, oldState.yPos - 1, oldState);
                case KeyEvent.VK_S -> newState = setCurrentStatePosition(oldState.xPos, oldState.yPos + 1, oldState);
                case KeyEvent.VK_A -> newState = setCurrentStatePosition(oldState.xPos - 1, oldState.yPos, oldState);
                case KeyEvent.VK_D -> newState = setCurrentStatePosition(oldState.xPos + 1, oldState.yPos, oldState);
                case KeyEvent.VK_C -> newState = setRotation(oldState.rotation.nextRotation(), oldState);
                case KeyEvent.VK_X -> // MIRROR
                    newState = setIsMirrored(!oldState.mirrored, oldState);
                default -> newState = oldState;
            }
            applyToMapAndUI(layer, newState, oldState);
        } catch (Exception ex) {
            GlobalActionPanel.ErrorPopUp(ex);
        } finally {
            if (getDimension().isEventsInhibited())
                getDimension().setEventsInhibited(false);
        }
    }

    void handleClick(int centreX, int centreY, boolean rightClick, boolean ctrlDown) {
        CityLayer layer = getSelectedLayer();
        if (layer == null)
            return;

        if (ctrlDown && !rightClick) {
            placeAt(centreX, centreY);
        } else if (rightClick) {
            if (layer.getInformationAt(uiState.xPos, uiState.yPos) != null) {
                applyToMapAndUI(layer, setCurrentStatePosition(centreX, centreY, uiState), uiState);
            }
        } else {
            onPickAt(centreX, centreY, layer);
        }
    }

    /** Places one building at the coordinates */
    void placeAt(int centreX, int centreY) {
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
        var newState = setSelectedObjectIndex(nextIdx, oldState);
        CityLayer layer = getSelectedLayer();
        if (layer != null && layer.getInformationAt(oldState.xPos, oldState.yPos) == null) {
            applyToUi(newState);
        } else {
            applyToMapAndUI(layer, newState, oldState);
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
            ((DragOverlay) dragOverlay).setMapView(view);
            previousViewListener = view.getViewListener();
            view.setViewListener(overlayViewListener);
            view.addComponentListener(overlayResizeListener);
            view.add(dragOverlay);
            view.setComponentZOrder(dragOverlay, 0);
            resizeDragOverlay();
        }
    }

    private void resizeDragOverlay() {
        if (overlayView != null)
            dragOverlay.setBounds(0, 0, overlayView.getWidth(), overlayView.getHeight());
    }

    private void detachDragOverlay() {
        if (overlayView == null)
            return;
        overlayView.removeComponentListener(overlayResizeListener);
        overlayView.remove(dragOverlay);
        if (overlayView.getViewListener() == overlayViewListener)
            overlayView.setViewListener(previousViewListener);
        overlayView.repaint();
        ((DragOverlay) dragOverlay).setMapView(null);
        previousViewListener = null;
        overlayView = null;
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
        ((DragOverlay) dragOverlay).endDrag();
        detachDragOverlay();
        super.deactivate();
    }

    @Override
    protected void brushChanged(Brush newBrush) {
        super.brushChanged(newBrush);

        ObjectState oldState = uiState;
        ObjectState newState;
        if (newBrush instanceof RotatedBrush rotatedBrush) {
            newState = setRotation(CityLayer.Direction.fromCompass((rotatedBrush.getDegrees() + 360) % 360), oldState);
        } else {
            newState = setRotation(CityLayer.Direction.NORTH, oldState);
        }
        applyToUi(newState);
    }

    protected void paintChanged(Paint ignored) {
        if (lastLayer != null)
            lastLayer.setIsSelectedPaint(false);
        if (getSelectedLayer() != null)
            getSelectedLayer().setIsSelectedPaint(true);
        lastLayer = getSelectedLayer();
        updatePanel();
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
        optionsPanel.setSelectedIndex(uiState.objectIndex);

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
        layer.setSelected(newState);

        applyToUi(newState);
        if (getViewAsWP() != null) { // force a tile renderer update //FIXME use less frequently, this will force ALL
                                     // tiles to be rerendered.
            getViewAsWP().refreshTilesForLayer(layer, false);
        }
    }

    private void onPickAt(int centreX, int centreY, CityLayer cityLayer) { // FIXME even at tiny brush sizes, the
                                                                           // closest obj should be selected.
        int radius = Math.max(1, getBrush().getRadius());
        int lastIndex = -1;
        float lastDist = Float.MAX_VALUE;
        int lastX = 0, lastY = 0;
        for (int x = centreX - radius; x < centreX + radius; x++) {
            for (int y = centreY - radius; y < centreY + radius; y++) {
                ObjectState state = cityLayer.getInformationAt(x, y);
                if (state != null) {
                    float currentDist = dist(centreX, centreY, x, y);
                    if (currentDist < lastDist) {
                        lastDist = currentDist;
                        lastIndex = state.objectIndex;
                        lastX = x;
                        lastY = y;
                    }
                }
            }
        }
        if (lastIndex != -1) {
            ObjectState mapState = cityLayer.getInformationAt(lastX, lastY);
            if (mapState == null)
                return;
            applyToUi(mapState);

            cityLayer.setSelected(mapState);
            if (getViewAsWP() != null) {
                getViewAsWP().refreshTilesForLayer(getSelectedLayer(), false);
            }
        } else {
            deselect(cityLayer);
        }
    }

    private void deselect(CityLayer layer) {
        layer.setSelected(null);
        applyToUi(new ObjectState(uiState.rotation, uiState.mirrored, uiState.objectIndex, Integer.MAX_VALUE,
                Integer.MAX_VALUE));
        if (getViewAsWP() != null)
            getViewAsWP().refreshTilesForLayer(layer, false);
    }

    private void deleteSelected() {
        CityLayer layer = getSelectedLayer();
        if (layer == null || layer.getInformationAt(uiState.xPos, uiState.yPos) == null)
            return;

        layer.removeDataAt(getDimension(), uiState.xPos, uiState.yPos);
        deselect(layer);
    }

    private void onRemoveAt(int centreX, int centreY, CityLayer cityLayer) { // FIXME respect brush shape (round or
                                                                             // square) + rotation
        int radius = getBrush().getRadius();
        for (int x = centreX - radius; x < centreX + radius; x++) {
            for (int y = centreY - radius; y < centreY + radius; y++) {
                cityLayer.removeDataAt(getDimension(), x, y);
            }
        }
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
        if (rotation == this.uiState.rotation)
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
        ObjectState oldState = uiState;
        ObjectState newState = setSelectedObjectIndex(index, oldState);
        CityLayer layer = getSelectedLayer();
        if (layer != null && layer.getInformationAt(oldState.xPos, oldState.yPos) != null) {
            applyToMapAndUI(layer, newState, oldState);
        } else {
            applyToUi(newState);
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

    private static class DragOverlay extends JComponent
    {
        private static final int CELL_SIZE = 10;
        private static final Color LIGHT_CELL = new Color(255, 255, 255, 80);
        private static final Color DARK_CELL = new Color(255, 0, 0, 80);
        private static final Color BORDER = new Color(255, 255, 255, 180);

        private WorldPainterView mapView;
        private Point dragStartWorld;
        private Point dragEndWorld;

        DragOverlay() {
            setOpaque(false);
        }

        @Override
        public boolean contains(int x, int y) {
            return false;
        }

        void startDrag(Point viewPoint) {
            dragStartWorld = mapView != null ? mapView.viewToWorld(viewPoint) : viewPoint;
            dragEndWorld = dragStartWorld;
            repaint();
        }

        void updateDrag(Point viewPoint) {
            if (mapView != null)
                dragEndWorld = mapView.viewToWorld(viewPoint);
            repaint();
        }

        void endDrag() {
            dragStartWorld = null;
            dragEndWorld = null;
            repaint();
        }

        boolean isDragging() {
            return dragStartWorld != null;
        }

        void setMapView(WorldPainterView mapView) {
            this.mapView = mapView;
            repaint();
        }

        @Override
        public void paint(Graphics graphics) {
            // Set a breakpoint here to verify that Swing paints this child overlay.
            super.paint(graphics);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                if (mapView != null) {
                    Rectangle worldBounds = mapView.viewToWorld(0, 0, getWidth(), getHeight());
                    int startX = Math.floorDiv(worldBounds.x, CELL_SIZE) * CELL_SIZE;
                    int startY = Math.floorDiv(worldBounds.y, CELL_SIZE) * CELL_SIZE;
                    int endX = worldBounds.x + worldBounds.width + CELL_SIZE;
                    int endY = worldBounds.y + worldBounds.height + CELL_SIZE;

                    for (int worldY = startY; worldY < endY; worldY += CELL_SIZE) {
                        for (int worldX = startX; worldX < endX; worldX += CELL_SIZE) {
                            Point topLeft = mapView.worldToView(worldX, worldY);
                            Point bottomRight = mapView.worldToView(worldX + CELL_SIZE, worldY + CELL_SIZE);
                            g.setColor((Math.floorDiv(worldX, CELL_SIZE) + Math.floorDiv(worldY, CELL_SIZE)) % 2 == 0
                                    ? LIGHT_CELL
                                    : DARK_CELL);
                            g.fillRect(topLeft.x, topLeft.y, bottomRight.x - topLeft.x, bottomRight.y - topLeft.y);
                        }
                    }
                }

                if (mapView != null && dragStartWorld != null && dragEndWorld != null) {
                    Rectangle worldSelection = new Rectangle(Math.min(dragStartWorld.x, dragEndWorld.x),
                            Math.min(dragStartWorld.y, dragEndWorld.y), Math.abs(dragEndWorld.x - dragStartWorld.x) + 1,
                            Math.abs(dragEndWorld.y - dragStartWorld.y) + 1);
                    Rectangle rectangle = mapView.worldToView(worldSelection);
                    g.setColor(BORDER);
                    g.drawRect(rectangle.x, rectangle.y, rectangle.width - 1, rectangle.height - 1);
                }
            } finally {
                g.dispose();
            }
        }
    }
}
