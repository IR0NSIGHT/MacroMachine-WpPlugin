package org.ironsight.wpplugin.macromachine.Layers.CityBuilder;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseWheelEvent;
import java.beans.PropertyVetoException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;
import javax.swing.*;
import javax.vecmath.Point3i;

import org.ironsight.wpplugin.macromachine.Gui.GlobalActionPanel;
import org.pepsoft.util.undo.UndoManager;
import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.brushes.Brush;
import org.pepsoft.worldpainter.brushes.RotatedBrush;
import org.pepsoft.worldpainter.brushes.SymmetricBrush;
import org.pepsoft.worldpainter.layers.bo2.WPObjectListCellRenderer;
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
    record PlacementOptions(boolean randomRotate, boolean randomSelect, boolean randomMirror)
    {
    }

    private final OptionsPanel optionsPanel;
    Random random = new Random();
    private ObjectState uiState = new ObjectState(CityLayer.Direction.NORTH, false, 0, Integer.MAX_VALUE,
            Integer.MAX_VALUE);
    private PlacementOptions placementOptions = new PlacementOptions(false, false, false);

    private Paint paint;
    private CityLayer lastLayer = null;

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

    public static void main(String[] args) throws IOException {
        // set up layer
        CityLayer layer = new CityLayer("test-city-layer", "this is a description");
        File dir = new File(
                "C:/Users/Max1M/curseforge/minecraft/Instances/neoforge 1.12.1 camboi shaders/config/worldedit/schematics");
        File[] files = dir.listFiles();
        ArrayList<WPObject> schematics = new ArrayList<>();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    assert file.exists();
                    WPObject schematic = new DefaultCustomObjectProvider().loadObject(file);
                    schematics.add(schematic);
                }
            }
        }
        layer.setObjectList(schematics);

        // set up operation
        var op = new CityEditToolOperation();
        op.setBrush(SymmetricBrush.CONSTANT_SQUARE);
        op.setPaint(new NibbleLayerPaint(layer));

        JFrame frame = new JFrame();
        frame.add(op.optionsPanel);
        frame.pack();
        frame.setVisible(true);

        frame.addMouseWheelListener(l -> {
            int degrees = l.getWheelRotation() * 90;
            System.out.println("wheel rotates brush");
            var rotatedBrush = RotatedBrush.rotate(op.getBrush(), degrees);
            op.setBrush(rotatedBrush);
        });
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
            ObjectState newState;
            switch (keyCode) {
                case KeyEvent.VK_W -> newState = setCurrentStatePosition(oldState.xPos, oldState.yPos - 1, oldState);
                case KeyEvent.VK_S -> newState = setCurrentStatePosition(oldState.xPos, oldState.yPos + 1, oldState);
                case KeyEvent.VK_A -> newState = setCurrentStatePosition(oldState.xPos - 1, oldState.yPos, oldState);
                case KeyEvent.VK_D -> newState = setCurrentStatePosition(oldState.xPos + 1, oldState.yPos, oldState);
                case KeyEvent.VK_C -> newState = setRotation(oldState.rotation.nextRotation(), oldState);
                case KeyEvent.VK_X -> // MIRROR
                        newState = setIsMirrored(!oldState.mirrored, oldState);
                default -> newState = oldState;
            }
            applyToMapAndUI(getSelectedLayer(), newState, oldState);
        } catch (Exception ex) {
            GlobalActionPanel.ErrorPopUp(ex);
        } finally {
            if (getDimension().isEventsInhibited())
                getDimension().setEventsInhibited(false);
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
     * @param direction up (dir<0) or down (dir>0) wheel
     */
    private void onMouseWheel(int direction) {
        int max = optionsPanel.getObjectCount();
        if (max == 0)
            return;
        var oldState = uiState;
        int nextIdx = Math.clamp(oldState.objectIndex + direction, 0, max - 1);
        System.out.println("change index by direction " + direction);
        var newState = setSelectedObjectIndex(nextIdx, oldState);
        applyToUi(newState);
    }

    @Override
    public void interrupt() {
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
            if (this.isCtrlDown() && !inverse) {
                onPickAt(centreX, centreY, cityLayer);
            } else if (this.isCtrlDown() && inverse) { // set position of current object to
                applyToMapAndUI(cityLayer, setCurrentStatePosition(centreX, centreY, uiState), uiState);
            } else if (inverse) {
                onRemoveAt(centreX, centreY, cityLayer);
            } else {
                placeAt(centreX,centreY);
            }
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
        updatePanel();
    }

    @Override
    protected void brushChanged(Brush newBrush) { // aka brush rotated.
        super.brushChanged(newBrush);

        // apply brush rotation
        final ObjectState oldState = uiState;
        final ObjectState newState;
        if (newBrush instanceof RotatedBrush)
            newState = setRotation(
                    CityLayer.Direction.fromCompass((((RotatedBrush) getBrush()).getDegrees() + 360) % 360), oldState);
        else
            newState = setRotation(CityLayer.Direction.NORTH, oldState);
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

        { // update brush radius
            Point3i dim;
            { // get object that is currently selected
                int selectedObjectIndex = uiState.objectIndex;
                if (selectedObjectIndex < 0 || selectedObjectIndex >= layer.getObjectList().size())
                    return;
                WPObject object = layer.getObjectList().get(selectedObjectIndex);
                dim = object.getDimensions();
            }
            int desiredRadius = Math.max(dim.x, dim.y) / 2;
            if (desiredRadius != getBrush().getRadius() && getView() != null) {
                int diff = desiredRadius - getBrush().getRadius();
                BrushControl control = getView().getBrushControl();
                if (diff > 0) {
                    for (int i = 0; i < diff; i++) {
                        control.increaseRadiusByOne();
                    }
                } else {
                    for (int i = 0; i < -diff; i++) {
                        control.decreaseRadiusByOne();
                    }
                }
            }
        }

        SwingUtilities.invokeLater(() -> {
            if (getViewAsWP() != null) {
                getViewAsWP().setBrushRotation(uiState.rotation.toCompass());
            }
        });

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

    private void onPickAt(int centreX, int centreY, CityLayer cityLayer) { //FIXME even at tiny brush sizes, the closest obj should be selected.
        int radius = getBrush().getRadius();
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
        }
    }

    private void onRemoveAt(int centreX, int centreY, CityLayer cityLayer) { //FIXME respect brush shape (round or square) + rotation
        int radius = getBrush().getRadius();
        for (int x = centreX - radius; x < centreX + radius; x++) {
            for (int y = centreY - radius; y < centreY + radius; y++) {
                cityLayer.removeDataAt(getDimension(), x, y);
            }
        }
    }

    private void onAddAt(int centreX, int centreY, CityLayer cityLayer) {
        // add new object
        var newState = setCurrentStatePosition(centreX, centreY, uiState);

        // set position
        applyToMapAndUI(cityLayer, newState, null);

        // ----------- set state for next object -----------
        var nextUiState = newState;
        if (placementOptions.randomRotate()) {
            nextUiState = setRotation(CityLayer.Direction.fromCompass(random.nextInt(4) * 90), nextUiState);
        }

        if (placementOptions.randomSelect()) {
            nextUiState = setSelectedObjectIndex(random.nextInt(optionsPanel.getObjectCount()), nextUiState);
        }

        if (placementOptions.randomMirror()) {
            nextUiState = setIsMirrored(random.nextBoolean(), nextUiState);
        }
        applyToUi(nextUiState);
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
        applyToUi(setSelectedObjectIndex(index, uiState));
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
     * @param x worldPos x
     * @param y worldPos y
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
}
