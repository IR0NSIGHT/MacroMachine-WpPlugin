package org.ironsight.wpplugin.macromachine.Layers.CityBuilder;

import org.ironsight.wpplugin.macromachine.Gui.GlobalActionPanel;
import org.pepsoft.util.undo.UndoManager;
import org.pepsoft.worldpainter.DefaultCustomObjectProvider;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.RadiusControl;
import org.pepsoft.worldpainter.brushes.Brush;
import org.pepsoft.worldpainter.brushes.RotatedBrush;
import org.pepsoft.worldpainter.brushes.SymmetricBrush;
import org.pepsoft.worldpainter.layers.bo2.WPObjectListCellRenderer;
import org.pepsoft.worldpainter.objects.WPObject;
import org.pepsoft.worldpainter.operations.AbstractBrushOperation;
import org.pepsoft.worldpainter.operations.PaintOperation;
import org.pepsoft.worldpainter.operations.RadiusOperation;
import org.pepsoft.worldpainter.painting.LayerPaint;
import org.pepsoft.worldpainter.painting.NibbleLayerPaint;
import org.pepsoft.worldpainter.painting.Paint;

import javax.swing.*;
import javax.vecmath.Point3i;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseWheelEvent;
import java.beans.PropertyVetoException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.ironsight.wpplugin.macromachine.Gui.HelpDialog.getHelpButton;

/**
 * STARMADE MOD CREATOR: Max1M DATE: 19.08.2025 TIME: 14:54
 */
public class CityEditToolOperation extends AbstractBrushOperation implements PaintOperation  {
    private final static String HelpTitle = "City Editor";
    private final static String HELPTEXT = """
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
    private final JPanel optionsPanel;
    private final JPanel contentPanel;
    private final JList<WPObject> list;
    private final JLabel warningLabel;
    Random random = new Random();
    private ObjectState currentState = new ObjectState(CityLayer.Direction.NORTH, false, 0);
    private int lastCentreX = Integer.MAX_VALUE, lastCentreY = Integer.MAX_VALUE;
    private boolean isAutoRandomRotate = false;
    private boolean isAutoRandomSelect = false;
    private Paint paint;

    public CityEditToolOperation() {
        super("City Tool", "Edit city layers using this tool", "city-edit-tool-operation");
        optionsPanel = new JPanel();
        contentPanel = new JPanel();
        list = new JList<>();
        warningLabel = new JLabel("Please select a city layer");

        init();
        Toolkit.getDefaultToolkit().addAWTEventListener(e -> {

            if (e instanceof MouseWheelEvent ev && isActive() && (ev.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0) {
                if (ev.getComponent().equals(SwingUtilities.getDeepestComponentAt(
                        ev.getComponent(), ev.getX(), ev.getY()))) { // fire only once
                    onMouseWheel(ev.getWheelRotation());
                }
            }
        }, AWTEvent.MOUSE_WHEEL_EVENT_MASK);

        KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();

        // Add a global key event dispatcher
        manager.addKeyEventDispatcher(new KeyEventDispatcher() {
            @Override
            public boolean dispatchKeyEvent(KeyEvent e) {
                if (e.getID() == KeyEvent.KEY_PRESSED) {
                    if (e.isShiftDown() || e.isControlDown() || e.isAltDown() || e.isMetaDown())
                        return false;
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_W:
                            onMoveAt(lastCentreX, lastCentreY - 1, getSelectedLayer());
                            break;
                        case KeyEvent.VK_S:
                            onMoveAt(lastCentreX , lastCentreY + 1, getSelectedLayer());
                            break;

                        case KeyEvent.VK_A:
                            onMoveAt(lastCentreX -1, lastCentreY, getSelectedLayer());
                            break;
                        case KeyEvent.VK_D:
                            onMoveAt(lastCentreX + 1, lastCentreY, getSelectedLayer());
                            break;
                        case KeyEvent.VK_C:
                            setRotation(currentState.rotation.nextRotation());
                            onMoveAt(lastCentreX, lastCentreY, getSelectedLayer());
                            break;

                        case KeyEvent.VK_X: // MIRROR
                            setIsMirrored(!currentState.mirrored);
                            onMoveAt(lastCentreX, lastCentreY, getSelectedLayer());
                            break;
                    }
                }
                return false; // return false to allow other listeners to handle the event
            }
        });
    }


    public static void main(String[] args) throws IOException {
        // set up layer
        CityLayer layer = new CityLayer("test-city-layer", "this is a description");
        File dir = new File("C:/Users/Max1M/curseforge/minecraft/Instances/neoforge 1.12.1 camboi shaders/config/worldedit/schematics");
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

    private void onMouseWheel(int direction) {
        int max = list.getModel().getSize();
        if (max == 0)
            return;
        int nextIdx = Math.max(0, Math.min((currentState.objectIndex + direction), max - 1));
        System.out.println("change index by direction " + direction);
        setSelectedObjectIndex(nextIdx);
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

    private void onRemoveAt(int centreX, int centreY, CityLayer cityLayer) {
        int radius = getBrush().getRadius();
        for (int x = centreX - radius; x < centreX + radius; x++) {
            for (int y = centreY - radius; y < centreY + radius; y++) {
                cityLayer.removeDataAt(getDimension(), x, y);
            }
        }
    }

    private void onAddAt(int centreX, int centreY, CityLayer cityLayer) {
        cityLayer.setDataAt(getDimension(), centreX, centreY, currentState.rotation, currentState.mirrored, currentState.objectIndex);

        if (isAutoRandomRotate) {
            setRotation(CityLayer.Direction.fromCompass(random.nextInt(4) * 90));
        }

        if (isAutoRandomSelect) {
            setSelectedObjectIndex(random.nextInt(list.getModel().getSize()));
        }
        lastCentreY = centreY;
        lastCentreX = centreX;
    }

    private void setSelectedObjectIndex(int index) {
        if (index == currentState.objectIndex)
            return;
        if (index < 0 && index >= list.getModel().getSize())
            return;

        this.currentState = new ObjectState(currentState.rotation, currentState.mirrored, index);
        if (list.getSelectedIndex() != index)
            list.setSelectedIndex(index);
        onObjectStateChanged();
    }

    private float dist(int x1, int y1, int x2, int y2) {
        int dx = x1 - x2;
        int dy = y1 - y2;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private void onPickAt(int centreX, int centreY, CityLayer cityLayer) {
        int radius = getBrush().getRadius();
        int lastIndex = -1;
        float lastDist = Float.MAX_VALUE;
        int lastX = 0, lastY = 0;
        for (int x = centreX - radius; x < centreX + radius; x++) {
            for (int y = centreY - radius; y < centreY + radius; y++) {
                int index = cityLayer.getItemIndexAt(x, y);
                if (index != -1) {
                    float currentDist = dist(centreX, centreY, x, y);
                    if (currentDist < lastDist) {
                        lastDist = currentDist;
                        lastIndex = index;
                        lastX = x;
                        lastY = y;
                    }
                }
            }
        }
        if (lastIndex != -1) {
            ObjectState data = cityLayer.getInformationAt(lastX, lastY);
            setSelectedObjectIndex(data.objectIndex);
            setRotation(data.rotation);
            setIsMirrored(data.mirrored);

            lastCentreX = lastX;
            lastCentreY = lastY;
        } else {
            lastCentreX = Integer.MAX_VALUE;
            lastCentreY = Integer.MAX_VALUE;
        }
    }

    private void onMoveAt(int centreX, int centreY, CityLayer cityLayer) {
        if (lastCentreX == Integer.MAX_VALUE || lastCentreY == Integer.MAX_VALUE)
            return;
        if (cityLayer == null)
            return;
        cityLayer.removeDataAt(getDimension(), lastCentreX, lastCentreY);
        cityLayer.setDataAt(getDimension(), centreX, centreY, currentState.rotation, currentState.mirrored, currentState.objectIndex);
        lastCentreY = centreY;
        lastCentreX = centreX;
    }

    @Override
    protected void tick(int centreX, int centreY, boolean inverse, boolean first, float dynamicLevel) {
        if (!getDimension().isEventsInhibited())
            getDimension().setEventsInhibited(true);
        if (getPaint() instanceof LayerPaint layerPaint && layerPaint.getLayer() instanceof CityLayer cityLayer) {
            ensureLayerHasUndoManager(cityLayer, getDimension());
            if (this.isCtrlDown() && !inverse) {
                onPickAt(centreX, centreY, cityLayer);
            } else if (this.isCtrlDown() && inverse) {
                onMoveAt(centreX, centreY, cityLayer);
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
            undoManager.removeListener(layer); //gotta remove otherwise we add over and over
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
    protected void brushChanged(Brush newBrush) {
        super.brushChanged(newBrush);
        if (newBrush instanceof RotatedBrush)
            setRotation(CityLayer.Direction.fromCompass((((RotatedBrush) getBrush()).getDegrees() + 360) % 360));
        else
            setRotation(CityLayer.Direction.NORTH);
    }

    protected void paintChanged(Paint paint) {
        updatePanel();
    }

    private void setRotation(CityLayer.Direction rotation) {
        if (rotation == this.currentState.rotation)
            return;
        System.out.println("set rotation from" + currentState.rotation + " to " + rotation);
        this.currentState = new ObjectState(rotation, currentState.mirrored, currentState.objectIndex);
        onObjectStateChanged();
    }

    private void setIsMirrored(boolean mirrored) {
        this.currentState = new ObjectState(currentState.rotation, mirrored, currentState.objectIndex);
        onObjectStateChanged();
    }
    JCheckBox isMirroredCheckbox;
    JCheckBox randomSelectCheckBox;
    JCheckBox rotateCheckBox;
    private void init() {
        JPanel content = contentPanel;
        content.setLayout(new BoxLayout(content,BoxLayout.Y_AXIS));
        optionsPanel.add(content);
        optionsPanel.add(warningLabel);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new WPObjectListCellRenderer());
        list.addListSelectionListener(l -> {
            if (l.getValueIsAdjusting())
                return;
            if (list.getSelectedIndex() != -1) {
                setSelectedObjectIndex(list.getSelectedIndex());
                list.ensureIndexIsVisible(currentState.objectIndex);
                onObjectStateChanged();
            }
        });

        rotateCheckBox = new JCheckBox("random rotate");
        rotateCheckBox.setToolTipText("Randomly rotate the brush after each use");
        rotateCheckBox.addActionListener(l -> {
            this.isAutoRandomRotate = rotateCheckBox.isSelected();
        });

        randomSelectCheckBox = new JCheckBox("random select");
        randomSelectCheckBox.setToolTipText("Randomly select new schematic after each use");
        randomSelectCheckBox.addActionListener(l -> {
            this.isAutoRandomSelect = randomSelectCheckBox.isSelected();
        });

        isMirroredCheckbox = new JCheckBox("mirrored");
        isMirroredCheckbox.setToolTipText("Randomly select new schematic after each use");
        isMirroredCheckbox.addActionListener(l -> {
            setIsMirrored(isMirroredCheckbox.isSelected());
        });


        // Put the icon into a JLabel
        JLabel previewPanel = new JLabel() {
            private int width = 100;
            @Override
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                Image original = getSelectedLayer().getSchematicImage(currentState);
                if (original == null)
                    return;
                int scale =  Math.max(100, getHeight()) / original.getHeight(null);
                Image img = original.getScaledInstance(original.getWidth(null)*scale,original.getHeight(null)*scale,Image.SCALE_REPLICATE);
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
        content.add(getHelpButton(HelpTitle, HELPTEXT));
        content.add(rotateCheckBox);
        content.add(randomSelectCheckBox);
        content.add(isMirroredCheckbox);
        content.add(previewPanel);
        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setMaximumSize(new java.awt.Dimension(1000,300));
        content.add(scrollPane);


        optionsPanel.revalidate();
        optionsPanel.repaint();
    }

    private void updatePanel() {
        if (getPaint() instanceof LayerPaint layerPaint && layerPaint.getLayer() instanceof CityLayer cityLayer) {
            DefaultListModel<WPObject> listModel = new DefaultListModel<>();
            listModel.setSize(cityLayer.getObjectList().size());
            for (int i = 0; i < listModel.getSize(); i++) {
                listModel.setElementAt(cityLayer.getObjectList().get(i), i);
            }
            list.setModel(listModel);
            setSelectedObjectIndex(0);

            warningLabel.setVisible(false);
            contentPanel.setVisible(true);
        } else {
            warningLabel.setVisible(true);
            contentPanel.setVisible(false);
        }
        optionsPanel.revalidate();
        optionsPanel.repaint();
    }

    private void onObjectStateChanged() {
        CityLayer layer = getSelectedLayer();
        if (layer == null)
            return;
        int selectedObjectIndex = currentState.objectIndex;
        if (selectedObjectIndex < 0 || selectedObjectIndex >= layer.getObjectList().size())
            return;
        WPObject object = layer.getObjectList().get(selectedObjectIndex);
        Point3i dim = object.getDimensions();

        //update brush radius
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

        //update checkbox
        isMirroredCheckbox.setSelected(currentState.mirrored);

        list.setSelectedIndex(currentState.objectIndex);

        optionsPanel.revalidate();
        optionsPanel.repaint();
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
