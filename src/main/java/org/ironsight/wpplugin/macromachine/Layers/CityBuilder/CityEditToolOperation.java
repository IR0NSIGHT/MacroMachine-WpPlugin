package org.ironsight.wpplugin.macromachine.Layers.CityBuilder;

import static org.ironsight.wpplugin.macromachine.Gui.HelpDialog.getHelpButton;

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
 * STARMADE MOD CREATOR: Max1M DATE: 19.08.2025 TIME: 14:54
 */
public class CityEditToolOperation extends AbstractBrushOperation implements PaintOperation, KeyEventDispatcher {
    private static final String HelpTitle = "City Editor";
    private static final String HELPTEXT = """
            this tool is for editing City Layers, a new special type of Custom Object Layer.
            1. Create or import a city layer (make sure your schematic offsets are centered and not 0,0,0)
            2. select the city layer
            3. select the city editor tool
            4. select a custom brush (the one with the little arrow showing the rotation)
            - Left click to place a building
            - Right click to delete all buildings inside the brush area
            
            - CTRL + left click to select a building type on the map
            - CTRL + right click to move last placed building to new position
            
            - SHIFT + mousewheel to scroll the building type list
            - ALT + mousewheel to rotate brush
            
            - X key : mirror last selected building on map
            - C key : rotate last selected building on map
            - AWSD key : move last selected building on map
            
            Warning: This layer is NOT compatible with undo/redo. Do NOT use undo/redo with this layer.
            
            """;
    private static CityEditToolOperation instance;
    private final JPanel optionsPanel;
    private final JPanel contentPanel;
    private final JList<WPObject> list;
    private final JLabel warningLabel;
    Random random = new Random();
    JCheckBox isRandomMirroredCheckbox;
    JCheckBox randomSelectCheckBox;
    JCheckBox rotateCheckBox;
    JCheckBox useHighlightColorsCheckbox;
    private ObjectState uiState = new ObjectState(CityLayer.Direction.NORTH, false, 0, Integer.MAX_VALUE,
            Integer.MAX_VALUE);
    private int lastCentreX = Integer.MAX_VALUE, lastCentreY = Integer.MAX_VALUE; // FIXME are these obsolete with state
    // carrying xy?
    private boolean isAutoRandomRotate = false;
    private boolean isAutoRandomSelect = false;
    private boolean isAutoRandomMirror = false;

    private Paint paint;
    private CityLayer lastLayer = null;

    public CityEditToolOperation() {
        super("City Tool", "Edit city layers using this tool", "city-edit-tool-operation");
        instance = this;
        optionsPanel = new JPanel();
        contentPanel = new JPanel();
        list = new JList<>();
        warningLabel = new JLabel("Please select a city layer");

        init();
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
            try {
                if (!getDimension().isEventsInhibited())
                    getDimension().setEventsInhibited(true);
                var oldState = uiState;
                ObjectState newState;
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_W:
                        newState = setCurrentStatePosition(oldState.xPos, oldState.yPos - 1, oldState);
                        break;
                    case KeyEvent.VK_S:
                        newState = setCurrentStatePosition(oldState.xPos, oldState.yPos + 1, oldState);
                        break;

                    case KeyEvent.VK_A:
                        newState = setCurrentStatePosition(oldState.xPos - 1, oldState.yPos, oldState);
                        break;
                    case KeyEvent.VK_D:
                        newState = setCurrentStatePosition(oldState.xPos + 1, oldState.yPos, oldState);
                        break;
                    case KeyEvent.VK_C:
                        newState = setRotation(oldState.rotation.nextRotation(), oldState);
                        break;

                    case KeyEvent.VK_X: // MIRROR
                        newState = setIsMirrored(!oldState.mirrored, oldState);
                        break;
                    default:
                        newState = oldState;
                        break;
                }
                applyToMapAndUI(getSelectedLayer(), newState, oldState);
            } catch (Exception ex) {
                GlobalActionPanel.ErrorPopUp(ex);
            } finally {
                if (getDimension().isEventsInhibited())
                    getDimension().setEventsInhibited(false);
            }
        }
        return false; // return false to allow other listeners to handle the event
    }

    /**
     * select the next schematic from the list, apply.
     *
     * @param direction
     */
    private void onMouseWheel(int direction) {
        int max = list.getModel().getSize();
        if (max == 0)
            return;
        var oldState = uiState;
        int nextIdx = Math.max(0, Math.min((oldState.objectIndex + direction), max - 1));
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
                onAddAt(centreX, centreY, cityLayer);
            }
        }
        if (getDimension().isEventsInhibited())
            getDimension().setEventsInhibited(false);
    }

    private void ensureLayerHasUndoManager(CityLayer layer, Dimension dimension) {
        try {
            UndoManager undoManager = getUndoManager(dimension);
            undoManager.removeListener(layer); // gotta remove otherwise we add over and over
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

        //apply brush rotation
        final ObjectState oldState = uiState;
        final ObjectState newState;
        if (newBrush instanceof RotatedBrush)
            newState = setRotation(CityLayer.Direction.fromCompass((((RotatedBrush) getBrush()).getDegrees() + 360) % 360), oldState);
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
        //deliberate let uiState == this.uiState pass, brush radius must be forced back to current selected obj.

        System.out.println("### City Tool UI state changed to:" + uiState);
        CityLayer layer = getSelectedLayer();
        if (layer == null)
            return;

        this.uiState = uiState;

        {    // update brush radius
            Point3i dim;
            {    //get object that is currently selected
                int selectedObjectIndex = uiState.objectIndex;
                if (selectedObjectIndex < 0 || selectedObjectIndex >= layer.getObjectList().size())
                    return;
                WPObject object = layer.getObjectList().get(selectedObjectIndex);
                dim = object.getDimensions();
            }
            int desiredRadius = Math.max(dim.x, dim.y) / 2;
            if (desiredRadius != getBrush().getRadius() && getView() != null) {
                int diff = desiredRadius - getBrush().getRadius();
                RadiusControl control = getView().getRadiusControl();
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
        if (getViewAsWP () != null) {
            getViewAsWP().setBrushRotation(uiState.rotation.toCompass());
        }
        });


        //update list
        list.setSelectedIndex(uiState.objectIndex);

        optionsPanel.revalidate();
        optionsPanel.repaint();
    }

    private void applyToMapAndUI(CityLayer layer, ObjectState newState, ObjectState oldState) {
        if (newState.equals(oldState))
            return;
        if (layer == null || newState == null)
            return;
        if (oldState != null)
            layer.removeDataAt(getDimension(), oldState.xPos, oldState.yPos);
        layer.setDataAt(getDimension(), newState.xPos, newState.yPos, newState);
        layer.setSelected(newState);

        applyToUi(newState);
        if (getViewAsWP() != null) { // force a tile renderer update //FIXME use less frequently, this will force ALL tiles to be rerendered.
            getViewAsWP().refreshTilesForLayer(layer, false);
        }
    }

    private void onPickAt(int centreX, int centreY, CityLayer cityLayer) {
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

            getSelectedLayer().setSelected(mapState);
            if (getViewAsWP() != null) {
                getViewAsWP().refreshTilesForLayer(getSelectedLayer(), false);
            }

            lastCentreX = lastX;
            lastCentreY = lastY;
        } else {
            lastCentreX = Integer.MAX_VALUE;
            lastCentreY = Integer.MAX_VALUE;
        }
    }

    private void onRemoveAt(int centreX, int centreY, CityLayer cityLayer) {
        int radius = getBrush().getRadius();
        for (int x = centreX - radius; x < centreX + radius; x++) {
            for (int y = centreY - radius; y < centreY + radius; y++) {
                cityLayer.removeDataAt(getDimension(), x, y);
            }
        }
    }

    private void onAddAt(int centreX, int centreY, CityLayer cityLayer) {
        //add new object
        var newState = setCurrentStatePosition(centreX, centreY, uiState);
        lastCentreY = centreY;
        lastCentreX = centreX;

        //set position
        applyToMapAndUI(cityLayer, newState, null);

        // ----------- set state for next object -----------
        var nextUiState = newState;
        if (isAutoRandomRotate) {
            nextUiState = setRotation(CityLayer.Direction.fromCompass(random.nextInt(4) * 90), nextUiState);
        }

        if (isAutoRandomSelect) {
            nextUiState = setSelectedObjectIndex(random.nextInt(list.getModel().getSize()), nextUiState);
        }

        if (isAutoRandomMirror) {
            nextUiState = setIsMirrored(random.nextBoolean(), nextUiState);
        }
        applyToUi(nextUiState);
    }

    private ObjectState setRotation(CityLayer.Direction rotation, ObjectState oldState) {
        if (rotation == this.uiState.rotation)
            return oldState;
        System.out.println("set rotation from" + oldState.rotation + " to " + rotation);
        return new ObjectState(rotation, oldState.mirrored, oldState.objectIndex,
                oldState.xPos, oldState.yPos);
    }

    private ObjectState setIsMirrored(boolean mirrored, ObjectState oldState) {
        return new ObjectState(oldState.rotation, mirrored, oldState.objectIndex,
                oldState.xPos, oldState.yPos);
    }

    private ObjectState setSelectedObjectIndex(int index, ObjectState oldState) {
        if (index == oldState.objectIndex)
            return oldState;
        if (index < 0 && index >= list.getModel().getSize())
            return oldState;

        var newState = new ObjectState(oldState.rotation, oldState.mirrored, index, oldState.xPos,
                oldState.yPos);
        return newState;
    }

    /**
     * overwrites the current states position
     *
     * @param x
     * @param y
     */
    private ObjectState setCurrentStatePosition(int x, int y, ObjectState oldState) {
        return new ObjectState(oldState.rotation, oldState.mirrored, oldState.objectIndex, x,
                y);
    }

    private void init() {
        JPanel content = contentPanel;
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        optionsPanel.add(content);
        optionsPanel.add(warningLabel);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new WPObjectListCellRenderer());
        list.addListSelectionListener(l -> {
            if (l.getValueIsAdjusting())
                return;
            if (list.getSelectedIndex() != -1) {
                var newState = setSelectedObjectIndex(list.getSelectedIndex(), uiState);
                list.ensureIndexIsVisible(newState.objectIndex);
                applyToUi(newState);
            }
        });

        rotateCheckBox = new JCheckBox("random rotate");
        rotateCheckBox.setToolTipText("Randomly rotate the brush after each use");
        rotateCheckBox.addActionListener(l -> this.isAutoRandomRotate = rotateCheckBox.isSelected());

        randomSelectCheckBox = new JCheckBox("random select");
        randomSelectCheckBox.setToolTipText("Randomly select new schematic after each use");
        randomSelectCheckBox.addActionListener(l -> this.isAutoRandomSelect = randomSelectCheckBox.isSelected());

        isRandomMirroredCheckbox = new JCheckBox("random mirrored");
        isRandomMirroredCheckbox.setToolTipText("Randomly select new schematic after each use");
        isRandomMirroredCheckbox.addActionListener(l -> this.isAutoRandomMirror = isRandomMirroredCheckbox.isSelected());

        useHighlightColorsCheckbox = new JCheckBox("use highlight colors");
        useHighlightColorsCheckbox.setToolTipText("Use the layers color instead of painting the actual schematics");
        useHighlightColorsCheckbox.addActionListener(l -> {
            CityLayer layer = getSelectedLayer();
            if (layer != null) {
                layer.setUseHighlightColors(useHighlightColorsCheckbox.isSelected());
                if (getViewAsWP() != null) {
                    getViewAsWP().refreshTilesForLayer(layer, false);
                }
            }
        });

        // Put the icon into a JLabel
        JLabel previewPanel = getPreviewPanel();
        content.add(getHelpButton(HelpTitle, HELPTEXT));
        content.add(rotateCheckBox);
        content.add(randomSelectCheckBox);
        content.add(isRandomMirroredCheckbox);
        content.add(useHighlightColorsCheckbox);
        content.add(previewPanel);
        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setMaximumSize(new java.awt.Dimension(1000, 300));
        content.add(scrollPane);

        optionsPanel.revalidate();
        optionsPanel.repaint();
    }

    private JLabel getPreviewPanel() {
        JLabel previewPanel = new JLabel() {
            private int width = 100;

            @Override
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                Image original = Objects.requireNonNull(getSelectedLayer()).getSchematicImage(uiState);
                if (original == null)
                    return;
                int scale = Math.max(100, getHeight()) / original.getHeight(null);
                Image img = original.getScaledInstance(original.getWidth(null) * scale,
                        original.getHeight(null) * scale, Image.SCALE_REPLICATE);
                width = img.getWidth(null);
                g.drawImage(img, 0, 0, null);
            }

            @Override
            public java.awt.Dimension getPreferredSize() {
                return new java.awt.Dimension(width, Math.max(100, getHeight()));
            }
        };
        previewPanel.setPreferredSize(new java.awt.Dimension(50, 50));
        previewPanel.setMaximumSize(new java.awt.Dimension(300, 300));
        previewPanel.setMinimumSize(new java.awt.Dimension(50, 50));
        return previewPanel;
    }

    private void updatePanel() {
        if (getPaint() instanceof LayerPaint layerPaint && layerPaint.getLayer() instanceof CityLayer cityLayer) {
            DefaultListModel<WPObject> listModel = new DefaultListModel<>();
            listModel.setSize(cityLayer.getObjectList().size());
            for (int i = 0; i < listModel.getSize(); i++) {
                listModel.setElementAt(cityLayer.getObjectList().get(i), i);
            }
            list.setModel(listModel);
            applyToUi(setSelectedObjectIndex(0, uiState)); // some safety thing to always be inside of list bound?

            warningLabel.setVisible(false);
            contentPanel.setVisible(true);
            useHighlightColorsCheckbox.setSelected(cityLayer.isUseHighlightColors());
        } else {
            warningLabel.setVisible(true);
            contentPanel.setVisible(false);
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
