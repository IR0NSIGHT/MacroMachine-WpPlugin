package org.demo.wpplugin.operations.EditPath;

import javafx.geometry.Point2D;
import org.demo.wpplugin.ArrayUtility;
import org.demo.wpplugin.Gui.Heightmap3dApp;
import org.demo.wpplugin.Gui.OperationOptionsPanel;
import org.demo.wpplugin.Gui.OptionsLabel;
import org.demo.wpplugin.HalfWaySubdivider;
import org.demo.wpplugin.Subdivide;
import org.demo.wpplugin.geometry.HeightDimension;
import org.demo.wpplugin.geometry.PaintDimension;
import org.demo.wpplugin.layers.PathPreviewLayer;
import org.demo.wpplugin.layers.renderers.DemoLayerRenderer;
import org.demo.wpplugin.operations.ContinuousCurve;
import org.demo.wpplugin.operations.River.RiverHandleInformation;
import org.demo.wpplugin.pathing.Path;
import org.demo.wpplugin.pathing.PathManager;
import org.demo.wpplugin.pathing.PointInterpreter;
import org.demo.wpplugin.pathing.PointUtils;
import org.pepsoft.worldpainter.Terrain;
import org.pepsoft.worldpainter.brushes.Brush;
import org.pepsoft.worldpainter.operations.*;
import org.pepsoft.worldpainter.painting.Paint;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyVetoException;
import java.util.*;

import static org.demo.wpplugin.Gui.OptionsLabel.numericInput;
import static org.demo.wpplugin.operations.River.RiverHandleInformation.*;
import static org.demo.wpplugin.pathing.PointUtils.*;

/**
 * For any operation that is intended to be applied to the dimension in a
 * particular location as indicated by the user
 * by clicking or dragging with a mouse or pressing down on a tablet, it
 * makes sense to subclass
 * {@link MouseOrTabletOperation}, which automatically sets that up for you.
 *
 * <p>For more general kinds of operations you are free to subclass
 * {@link AbstractOperation} instead, or even just
 * implement {@link Operation} directly.
 *
 * <p>There are also more specific base classes you can use:
 *
 * <ul>
 *     <li>{@link AbstractBrushOperation} - for operations that need access
 *     to the currently selected brush and
 *     intensity setting.
 *     <li>{@link RadiusOperation} - for operations that perform an action in
 *     the shape of the brush.
 *     <li>{@link AbstractPaintOperation} - for operations that apply the
 *     currently selected paint in the shape of the
 *     brush.
 * </ul>
 *
 * <p><strong>Note</strong> that for now WorldPainter only supports
 * operations that
 */
public class EditPathOperation extends MouseOrTabletOperation implements PaintOperation, // Implement this if you
// need access to the currently selected paint; note that some base
        // classes already provide this
        BrushOperation // Implement this if you need access to the currently
        // selected brush; note that some base
        // classes already provide this
{

    public static final int COLOR_NONE = 0;
    public static final int COLOR_HANDLE = DemoLayerRenderer.Cyan;
    public static final int COLOR_CURVE = DemoLayerRenderer.BLUE;
    public static final int COLOR_SELECTED = DemoLayerRenderer.Orange;

    public static final int SIZE_SELECTED = 15;
    public static final int SIZE_DOT = 0;
    public static final int SIZE_MEDIUM_CROSS = 10;
    /**
     * The globally unique ID of the operation. It's up to you what to use
     * here. It is not visible to the user. It can
     * be a FQDN or package and class name, like here, or you could use a
     * UUID. As long as it is globally unique.
     */
    static final String ID = "org.demo.wpplugin.BezierPathTool.v1";
    /**
     * Human-readable short name of the operation.
     */
    static final String NAME = "Edit Path Operation";
    /**
     * Human-readable description of the operation. This is used e.g. in the
     * tooltip of the operation selection button.
     */
    static final String DESCRIPTION = "<html>Draw smooth, connected curves " + "with C1 continuity.<br>left click: " +
            "add " + "new" + " point " + "after selected<br>right click: delete selected<br>ctrl+click: " + "select " +
            "this " + "handle<br>shift+click: move " + "selected" + " " + "handle here</html>";
    //update path
    public static int PATH_ID = 1;
    private final EditPathOptions options = new EditPathOptions();
    private final LinkedList<ToolHistoryState> history = new LinkedList<>();
    EditPathOptionsPanel eOptionsPanel;
    int resolution3d = 2;
    private Brush brush;
    private Paint paint;
    private boolean shiftDown = false;
    private boolean altDown = false;
    private boolean ctrlDown = false;
    private StandardOptionsPanel panelContainer;
    private ToolHistoryState currentState;
    private boolean keyListening;
    public void addKeyListenerToComponent(JComponent component) {
        if (keyListening)
            return;
        keyListening = true;


        JFrame wpApp = (JFrame) SwingUtilities.getWindowAncestor(component);
        {
            KeyStroke controlA = KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_MASK);
            wpApp.getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).
                    put(controlA, "select_all");
            wpApp.getRootPane().getActionMap().put("select_all", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (getSelectedIdcs(false).length == getSelectedPath().amountHandles())  //all are selected
                        deselectAll();
                    else    //not all are selected
                        selectAll();
                    redrawSelectedPathLayer();
                }
            });
        }

        {
            wpApp.getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).
                    put(KeyStroke.getKeyStroke("ENTER"), "submit");
            wpApp.getRootPane().getActionMap().put("submit", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    setHandleSelection(currentState.cursorHandleIdx, !isHandleSelected(currentState.cursorHandleIdx, false));
                    redrawSelectedPathLayer();
                }
            });
        }
    }

    public EditPathOperation() {
        // Using this constructor will create a "single shot" operation. The
        // tick() method below will only be invoked
        // once for every time the user clicks the mouse or presses on the
        // tablet:
        super(NAME, DESCRIPTION, ID);
        // Using this constructor instead will create a continues operation.
        // The tick() method will be invoked once
        // every "delay" ms while the user has the mouse button down or
        // continues pressing on the tablet. The "first"
        // parameter will be true for the first invocation per mouse button
        // press and false for every subsequent
        // invocation:
        // super(NAME, DESCRIPTION, delay, ID);
        int selectedPathId = PathManager.instance.getAnyValidId();
        Path p = PathManager.instance.getPathBy(selectedPathId);
        this.currentState = new ToolHistoryState(
                p,
                new boolean[p.amountHandles()],
                0, selectedPathId
        );
        //Worldpainter Pen has a severe bug deep down that breaks
        // shift/alt/control after a button in the settings
        // pannel was used.
        //this shitty listener circumvents the bug
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
            @Override
            public boolean dispatchKeyEvent(KeyEvent e) {
                if (!EditPathOperation.super.isActive()) return false;
                shiftDown = e.isShiftDown();
                altDown = e.isAltDown();
                ctrlDown = e.isControlDown();

                return false;
            }
        });
    }



    private int[] getSelectedIdcs(boolean andCursor) {
        int count = 0;
        for (int i = 0; i < getSelectedPath().amountHandles(); i++) {
            if (isHandleSelected(i, andCursor))
                count++;
        }
        int[] idcs = new int[count];
        int sel = 0;
        for (int i = 0; i < getSelectedPath().amountHandles(); i++) {
            if (isHandleSelected(i,andCursor))
                idcs[sel++] = i;
        }
        return idcs;
    }

    private void deselectAll() {
        for (int i = 0; i < getSelectedPath().amountHandles(); i++) {
            setHandleSelection(i,false);
        }
    }

    private void selectAll() {
        for (int i = 0; i < getSelectedPath().amountHandles(); i++) {
            setHandleSelection(i,true);
        }
    }

    private void invertSelection() {
        for (int i = 0; i < getSelectedPath().amountHandles(); i++) {
            setHandleSelection(i, !isHandleSelected(i,false));
        }
    }

    private void setHandleSelection(int handle, boolean state) {
        currentState.selectedIdcs[handle] = state;
    }

    private boolean isHandleSelected(int idx, boolean orCursor) {
        if (idx < 0 || idx >= getSelectedPath().amountHandles())
            throw new ArrayIndexOutOfBoundsException("thats not a valid idx:" + idx);
        return (orCursor && currentState.cursorHandleIdx == idx) || currentState.selectedIdcs[idx];
    }

    void redrawSelectedPathLayer() {
        this.getDimension().setEventsInhibited(true);
        //erase old
        this.getDimension().clearLayerData(PathPreviewLayer.INSTANCE);
        PaintDimension paintDim = new PaintDimension() {
            @Override
            public int getValue(int x, int y) {
                return getDimension().getLayerValueAt(PathPreviewLayer.INSTANCE, x, y);
            }

            @Override
            public void setValue(int x, int y, int v) {
                getDimension().setLayerValueAt(PathPreviewLayer.INSTANCE, x, y, v);
            }
        };

        HeightDimension heightDim = new HeightDimension() {
            @Override
            public float getHeight(int x, int y) {
                return getDimension().getHeightAt(x, y);
            }

            @Override
            public void setHeight(int x, int y, float z) {
                getDimension().setHeightAt(x, y, z);
            }
        };

        for (int selectedIdx: getSelectedIdcs(false)) {
            float[] handle = getSelectedPath().handleByIndex(selectedIdx);
            markPoint(getPoint2D(handle), COLOR_SELECTED, SIZE_MEDIUM_CROSS, paintDim);
        }


        try {
            //redraw new
            DrawPathLayer(getSelectedPath().clone(), ContinuousCurve.fromPath(getSelectedPath(), heightDim), paintDim
                    , getSelectedPath().indexOfPosition(getCursorHandle()));
            if (getCursorHandle() != null)
                PointUtils.drawCircle(getPoint2D(getCursorHandle()), COLOR_SELECTED, SIZE_SELECTED, paintDim, false);
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        } finally {
            this.getDimension().setEventsInhibited(false);
        }
    }

    Path getSelectedPath() {
        return currentState.path;
    }

    /**
     * draws this path onto the map
     *
     * @param path
     */
    static void DrawPathLayer(Path path, ContinuousCurve curve, PaintDimension dim, int selectedPointIdx) throws IllegalAccessException {
        Path clone = path.clone();
        //nothing
        if (path.type == PointInterpreter.PointType.RIVER_2D) {
            DrawRiverPath(path, curve, dim, selectedPointIdx);
        }
        assert clone.equals(path) : "something mutated the path";
    }

    float[] getCursorHandle() {
        int selectedPointIdx = currentState.cursorHandleIdx;
        if (selectedPointIdx == -1) return null;
        if (selectedPointIdx < 0 || selectedPointIdx > getSelectedPath().amountHandles() - 1) return null;
        return getSelectedPath().handleByIndex(selectedPointIdx);
    }

    private void show3dAction() {
        Point selected = getPoint2D(getCursorHandle());
        //Create heightmap
        float[][] heightmap = new float[256][];
        float[][] waterMap = new float[256][];
        Heightmap3dApp.Texture[][] blockMap = new Heightmap3dApp.Texture[256][];

        for (int y = -128; y < 128; y++) {
            heightmap[y + 128] = new float[256];
            waterMap[y + 128] = new float[256];
            blockMap[y + 128] = new Heightmap3dApp.Texture[256];
            for (int x = -128; x < 128; x++) {
                Point thisP = new Point(selected.x + x * resolution3d, selected.y + y * resolution3d);

                float height = getDimension().getHeightAt(thisP) / resolution3d;
                heightmap[y + 128][x + 128] = height;

                float waterHeight = (float) getDimension().getWaterLevelAt(thisP.x, thisP.y) / resolution3d;
                waterMap[y + 128][x + 128] = waterHeight;

                Terrain t = getDimension().getTerrainAt(thisP.x, thisP.y);
                Heightmap3dApp.Texture tex;
                if (t == null) t = Terrain.GRASS;
                switch (t) {

                    case GRASS:
                    case BARE_GRASS:
                        tex = Heightmap3dApp.Texture.GRASS;
                        break;
                    case GRAVEL:
                        tex = Heightmap3dApp.Texture.GRAVEL;
                        break;
                    case SAND:
                    case DESERT:
                    case BEACHES:
                    case BARE_BEACHES:
                    case SANDSTONE:
                    case END_STONE:
                        tex = Heightmap3dApp.Texture.SAND;
                        break;
                    case ROCK:
                    case STONE:
                    case COBBLESTONE:
                    case MOSSY_COBBLESTONE:
                    case DIORITE:
                    case ANDESITE:
                    case BASALT:
                    case DEEPSLATE:
                        tex = Heightmap3dApp.Texture.ROCK;
                        break;
                    case DEEP_SNOW:
                    case SNOW:
                        tex = Heightmap3dApp.Texture.SNOW;
                        break;
                    case DIRT:
                        tex = Heightmap3dApp.Texture.DIRT;
                        break;
                    case WATER:
                        tex = Heightmap3dApp.Texture.WATER;
                        break;
                    default:
                        tex = Heightmap3dApp.Texture.ROCK;
                }
                blockMap[y + 128][x + 128] = tex;
            }
        }
        Heightmap3dApp.heightMap = heightmap;
        Heightmap3dApp.waterMap = waterMap;
        Heightmap3dApp.blockmap = blockMap;
        Heightmap3dApp.setHeightMap = point -> {
            getDimension().setHeightAt((int) point.getX(), (int) point.getY(), (float) point.getZ());
        };
        Heightmap3dApp.setWaterHeight = point -> {
            getDimension().setWaterLevelAt((int) point.getX(), (int) point.getY(), (int) point.getZ());
        };
        Heightmap3dApp.globalOffset = new Point2D(selected.x - 128, selected.y - 128);
        Heightmap3dApp.main();
    }

    /**
     * automatically advance the path downhill until it doesnt find any lower
     * point.
     */
    private boolean addHandleDownhill() {
        Point selected = getPoint2D(getCursorHandle());
        float selectedZ = getDimension().getHeightAt(selected);

        Point pMin = null;
        for (int radius = 1; radius < 25; radius++) {
            pMin = getLowestAtRadius(radius, selected);
            if (getDimension().getHeightAt(pMin) < selectedZ) break;
        }
        if (getDimension().getHeightAt(pMin) >= selectedZ) return false; //tested all radii, didnt find lower point

        Path p = getSelectedPath();
        assert pMin != null;

        float[] newHandle = RiverHandleInformation.riverInformation(pMin.x, pMin.y);
        p = p.insertPointAfter(getCursorHandle(), newHandle);
        overwriteSelectedPath(p);
        try {
            setSelectedPointIdx(p.getClosestHandleIdxTo(newHandle));
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    private Point getLowestAtRadius(int radius, Point center) {
        Point pMin = center;
        float zMin = Float.MAX_VALUE;
        ArrayList<Float> angles = new ArrayList<>(36);
        for (int i = 0; i < 36; i++) {
            angles.add((float) (i / 36f * Math.PI * 2f));
        }
        Collections.shuffle(angles);
        for (float alpha : angles) {
            Point newP = pointWithAngleAndRadius(center, alpha, radius);
            float z = getDimension().getHeightAt(newP);
            if (z < zMin) {
                zMin = z;
                pMin = newP;
            }
        }
        return pMin;
    }

    private void overwriteSelectedPath(Path p) {
        history.addFirst(currentState);

        //copy over handle selection, even if positions were added/deleted
        boolean[] newSelection = new boolean[p.amountHandles()];
        Path oldPath = getSelectedPath();
        for (int i = 0; i < getSelectedPath().amountHandles(); i++) {
            if (isHandleSelected(i, false)) {
                int newIdx = p.indexOfPosition(oldPath.handleByIndex(i));
                newSelection[newIdx] = true;
            }
        }

        this.currentState = new ToolHistoryState(p, newSelection, currentState.cursorHandleIdx,
                currentState.pathId);
        try {
            PathManager.instance.setPathBy(getSelectedPathId(), p);
        } catch (AssertionError err) {
            System.err.println(err);
        }
    }

    void setSelectedPointIdx(int selectedPointIdx) {
        assert selectedPointIdx >= 0 && selectedPointIdx < getSelectedPath().amountHandles();
        this.currentState.cursorHandleIdx = selectedPointIdx;
    }

    private static Point pointWithAngleAndRadius(Point p, float angleRad, int radius) {
        int x = (int) Math.round(radius * Math.cos(angleRad));
        int y = (int) Math.round(radius * Math.sin(angleRad));
        return new Point(p.x + x, p.y + y);
    }

    int getSelectedPathId() {
        return currentState.pathId;
    }

    private void selectPathById(int pathId) {
        history.addFirst(currentState);
        Path p = PathManager.instance.getPathBy(pathId);
        currentState = new ToolHistoryState(p, new boolean[p.amountHandles()], 0, pathId);
    }

    private void undo() {
        if (history.isEmpty())
            return;
        ToolHistoryState previousPath = history.pop();
        try {
            currentState = previousPath;
            PathManager.instance.setPathBy(previousPath.pathId, previousPath.path);
        } catch (AssertionError err) {
            System.err.println(err);
        }
    }

    @Override
    public Brush getBrush() {
        return brush;
    }

    @Override
    public void setBrush(Brush brush) {
        this.brush = brush;
    }

    @Override
    public Paint getPaint() {
        return paint;
    }

    @Override
    public void setPaint(Paint paint) {
        this.paint = paint;
    }

    @Override
    public JPanel getOptionsPanel() {
        panelContainer = new EditPathOptionsPanelContainer(getName(), "a " + "description");
        return panelContainer;
    }

    @Override
    protected void activate() throws PropertyVetoException {
        super.activate();
        altDown = false;
        ctrlDown = false;
        shiftDown = false;
        addKeyListenerToComponent(this.getView());

        redrawSelectedPathLayer();
    }

    /**
     * Perform the operation. For single shot operations this is invoked once
     * per mouse-down. For continuous operations
     * this is invoked once per {@code delay} ms while the mouse button is
     * down, with the first invocation having
     * {@code first} be {@code true} and subsequent invocations having it be
     * {@code false}.
     *
     * @param centreX      The x coordinate where the operation should be
     *                     applied, in world coordinates.
     * @param centreY      The y coordinate where the operation should be
     *                     applied, in world coordinates.
     * @param inverse      Whether to perform the "inverse" operation instead
     *                     of the regular operation, if applicable
     *                     . If the
     *                     operation has no inverse it should just apply the
     *                     normal operation.
     * @param first        Whether this is the first tick of a continuous
     *                     operation. For a one shot operation this
     *                     will always
     *                     be {@code true}.
     * @param dynamicLevel The dynamic level (from 0.0f to 1.0f inclusive) to
     *                     apply in addition to the {@code level}
     *                     property, for instance due to a pressure sensitive
     *                     stylus being used. In other words,
     *                     <strong>not</strong> the total level at which to
     *                     apply the operation! Operations are free to
     *                     ignore this if it is not applicable. If the
     *                     operation is being applied through a means which
     *                     doesn't provide a dynamic level (for instance the
     *                     mouse), this will be <em>exactly</em>
     *                     {@code 1.0f}.
     */
    @Override
    protected void tick(int centreX, int centreY, boolean inverse, boolean first, float dynamicLevel) {
        //  Perform the operation. In addition to the parameters you have the
        //  following methods available:
        // * getDimension() - obtain the dimension on which to perform the
        // operation
        // * getLevel() - obtain the current brush intensity setting as a
        // float between 0.0 and 1.0
        // * isAltDown() - whether the Alt key is currently pressed - NOTE:
        // this is already in use to indicate whether
        //                 the operation should be inverted, so should
        //                 probably not be overloaded
        // * isCtrlDown() - whether any of the Ctrl, Windows or Command keys
        // are currently pressed
        // * isShiftDown() - whether the Shift key is currently pressed
        // In addition you have the following fields in this class:
        // * brush - the currently selected brush
        // * paint - the currently selected paint
        final Path path = getSelectedPath();
        EditPathOperation.PATH_ID = getSelectedPathId();

        float[] userClickedCoord = RiverHandleInformation.riverInformation(centreX, centreY);
        if (path.type == PointInterpreter.PointType.RIVER_2D)
            setValue(userClickedCoord, RiverInformation.WATER_Z, getDimension().getHeightAt(centreX, centreY));

        assert getCursorHandle() != null;

        try {
            if (ctrlDown) {
                int idx = getHandleNear(userClickedCoord, path);
                if (idx != -1)
                    setSelectedPointIdx(idx);
            } else if (altDown) {
                overwriteSelectedPath(path.newEmpty());
                setSelectedPointIdx(-1);
            } else if (shiftDown) {
                int oldIdx = currentState.cursorHandleIdx;
                int newIdx = getHandleNear(userClickedCoord, path);
                if (newIdx != -1) {
                    setSelectedPointIdx(newIdx);
                }
                selectAllBetween(oldIdx, newIdx);
            } else if (inverse) {
            /*    //REMOVE SELECTED POINT
                if (path.amountHandles() > 1) {
                    try {
                        float[] pointBeforeSelected = path.getPreviousPoint(getCursorHandle());
                        Path newPath = path.removePoint(getCursorHandle());
                        overwriteSelectedPath(newPath);
                        int idx = getSelectedPath().indexOfPosition(pointBeforeSelected);
                        setSelectedPointIdx(idx);
                    } catch (Exception | AssertionError e) {
                        overwriteSelectedPath(path); // set old path
                        throw new RuntimeException(e);
                    }
                }

             */
                try {
                    float[] movedPoint = setPosition2D(getCursorHandle(), userClickedCoord);
                    int idx = path.indexOfPosition(getCursorHandle());
                    Path newPath = path.overwriteHandle(getCursorHandle(), movedPoint);
                    setSelectedPointIdx(idx);

                    overwriteSelectedPath(newPath);
                } catch (Exception ex) {
                    System.err.println("Error moving point " + getCursorHandle() + " to " + getSelectedPath());
                }
            } else {
                //add new point after selected
                overwriteSelectedPath(path.insertPointAfter(getCursorHandle(), userClickedCoord));
                setSelectedPointIdx(getSelectedPath().indexOfPosition(userClickedCoord));
            }


            assert getSelectedPath().amountHandles() == 0 || getCursorHandle() != null;


            assert getSelectedPath() == PathManager.instance.getPathBy(getSelectedPathId()) : "unsuccessfull " +
                    "setting " + "path in manager";
        } catch (Exception e) {
            System.out.println("Exception after user edit-path-action");
            System.out.println(e);
        }
        redrawSelectedPathLayer();

        if (this.eOptionsPanel != null) this.eOptionsPanel.onOptionsReconfigured();
    }

    private int getHandleNear(float[] userClickedCoord, Path path) {
        //SELECT POINT
        try {
            if (path.amountHandles() != 0) {
                int clostestIdx = path.getClosestHandleIdxTo(userClickedCoord);
                float[] closest = path.handleByIndex(clostestIdx);
                //dont allow very far away clicks
                if (getPositionalDistance(closest, userClickedCoord,
                        RiverHandleInformation.PositionSize.SIZE_2_D.value) < 50) {
                    return clostestIdx;
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return -1;
    }

    private void selectAllBetween(int start, int end) {
        for (int i = Math.min(start, end); i <= Math.max(start, end); i++) {
            setHandleSelection(i, true);
        }
    }

    private static class EditPathOptions {
        float subdivisions = 1;
        float subdivisionRange = .5f;
    }

    private class ToolHistoryState {
        final Path path;
        boolean[] selectedIdcs;
        int cursorHandleIdx;
        int pathId;

        public ToolHistoryState(Path path, boolean[] selectedIdcs, int cursorHandleIdx, int pathId) {
            this.path = path;
            this.selectedIdcs = selectedIdcs;
            this.pathId = pathId;
            this.cursorHandleIdx = cursorHandleIdx;
        }
    }

    private class EditPathOptionsPanelContainer extends StandardOptionsPanel {
        public EditPathOptionsPanelContainer(String name, String description) {
            super(name, description);
        }

        @Override
        protected void addAdditionalComponents(GridBagConstraints constraints) {
            JLabel desc = new JLabel(getDescription());
            add(desc, constraints);
            eOptionsPanel = new EditPathOptionsPanel(options);
            add(eOptionsPanel, constraints);
        }
    }

    private class EditPathOptionsPanel extends OperationOptionsPanel<EditPathOptions> {
        public EditPathOptionsPanel(EditPathOptions editPathOptions) {
            super(editPathOptions);
        }

        @Override
        protected ArrayList<OptionsLabel> addComponents(EditPathOptions editPathOptions,
                                                        Runnable onOptionsReconfigured) {
            ArrayList<OptionsLabel> inputs = new ArrayList<>();

            //select path dropdown
            Collection<PathManager.NamedId> availablePaths = PathManager.instance.allPathNamedIds();

            JComboBox<Object> comboBox = new JComboBox<>(availablePaths.toArray());
            comboBox.setSelectedItem(PathManager.instance.getPathName(getSelectedPathId()));
            comboBox.addActionListener(e -> {
                selectPathById(((PathManager.NamedId) comboBox.getSelectedItem()).id);
                setSelectedPointIdx(getSelectedPath().amountHandles() == 0 ? -1 :
                        getSelectedPath().amountHandles() - 1);
                redrawSelectedPathLayer();
                onOptionsReconfigured.run();
            });
            JLabel comboBoxLabel = new JLabel("Selected path");
            inputs.add(() -> new JComponent[]{comboBoxLabel, comboBox});

            // ADD BUTTON
            // Create a JButton with text
            JButton button = new JButton("Add empty path");
            // Add an ActionListener to handle button clicks
            button.addActionListener(e -> {
                float[][] handles = new float[][]{riverInformation(0, 0), riverInformation(10, 10, 1, 2, 3, 4, 5),
                        riverInformation(20, 20)};
                selectPathById(PathManager.instance.addPath(new Path(Arrays.asList(handles),
                        getSelectedPath().type)));
                setSelectedPointIdx(0);
                redrawSelectedPathLayer();
                onOptionsReconfigured.run();
            });
            inputs.add(() -> new JComponent[]{button});


            // Create a JTextField for text input
            final JTextField textField = new JTextField(20);

            // Create a JButton to trigger an action
            JButton submitNameChangeButton = new JButton("Change Name");
            textField.setText(PathManager.instance.getPathName(getSelectedPathId()).name);
            // Add ActionListener to handle button click
            submitNameChangeButton.addActionListener(e -> {
                // Get the text from the text field and display it in the label
                String inputText = textField.getText();
                PathManager.instance.nameExistingPath(getSelectedPathId(), inputText);
                onOptionsReconfigured.run();
            });

            {          // FLOW DOWNHILL BUTTON
                JButton myButton = new JButton("flow downhill");
                myButton.addActionListener(e -> {
                    //run downhill 25 handles max or until we hit a hole with
                    // no escape
                    for (int i = 0; i < 25; i++) {
                        boolean didFindSth = addHandleDownhill();
                        if (!didFindSth) break;
                    }
                    redrawSelectedPathLayer();
                });
                inputs.add(() -> new JComponent[]{myButton});
            }


            {

                // SHOW 3d BUTTON
                JButton myButton = new JButton("show 3d");
                myButton.addActionListener(e -> {
                    //run downhill 25 handles max or until we hit a hole with
                    // no escape
                    show3dAction();
                });
                inputs.add(() -> new JComponent[]{myButton});

                SpinnerNumberModel model = new SpinnerNumberModel(1f * resolution3d, 1, 10, 1f);

                OptionsLabel l = numericInput("3d resolution 1:x", "displays " + "area in 1:resolution", model,
                        newValue -> {
                            resolution3d = newValue.intValue();
                        }, EditPathOperation.this::show3dAction);
                inputs.add(l);
            }


            if (getCursorHandle() != null) {
                if (getSelectedPath().type == PointInterpreter.PointType.RIVER_2D) {
                    OptionsLabel[] riverInputs = RiverHandleInformation.Editor(getCursorHandle(), point -> {
                        Path oldPath = getSelectedPath();
                        try {
                            Path newPath = getSelectedPath().overwriteHandle(getCursorHandle(), point);
                            overwriteSelectedPath(newPath);
                        } catch (Exception ex) {
                            System.err.println(ex.getMessage());
                            overwriteSelectedPath(oldPath);
                        }
                        redrawSelectedPathLayer();
                    }, onOptionsReconfigured);

                    inputs.addAll(Arrays.asList(riverInputs));
                }
            }

            HeightDimension dim = new HeightDimension() {
                @Override
                public float getHeight(int x, int y) {
                    return getDimension().getHeightAt(x, y);
                }

                @Override
                public void setHeight(int x, int y, float z) {

                }
            };

            {
                JButton button1 = new JButton("Edit water height");
                button1.addActionListener(e -> {
                    JFrame c = (JFrame) SwingUtilities.getWindowAncestor(this.getParent());
                    JDialog dialog = riverRadiusEditor(c, getSelectedPath(), currentState.cursorHandleIdx,
                            EditPathOperation.this::overwriteSelectedPath, dim);
                    dialog.setVisible(true);
                    onOptionsReconfigured();
                    redrawSelectedPathLayer();
                });
                inputs.add(() -> new JComponent[]{button1});
            }

            {   // subdivide segment
                {
                    JButton subdivideButton = new JButton("subdivide segment");
                    subdivideButton.addActionListener(e -> {
                        Path p = getSelectedPath();
                        ArrayList<float[]> pathHandles = p.getHandles();
                        int newSelectedIdx = currentState.cursorHandleIdx;
                        int indexOffset = 0;
                        ArrayList<float[]> flatHandles = ArrayUtility.transposeMatrix(pathHandles);
                        Subdivide divider = new HalfWaySubdivider(options.subdivisionRange, options.subdivisionRange,
                                true);

                        //subdivide all marked segments
                        for (int oldSelectedIdx : getSelectedIdcs(true)) {
                            int selectedIdx = indexOffset + oldSelectedIdx;
                            if (selectedIdx < 0 || selectedIdx + 1 >= flatHandles.get(0).length)
                                continue;
                            ArrayList<float[]> newFlats = Subdivide.subdivide(flatHandles.get(0), flatHandles.get(1),
                                    selectedIdx, (int) options.subdivisions, divider);

                            //FIXME carry over the old values
                            for (int i = 2; i < p.type.size; i++) {
                                float[] filler = new float[newFlats.get(0).length];
                                Arrays.fill(filler, INHERIT_VALUE);
                                newFlats.add(filler);
                            }

                            int amountNewHandles = newFlats.get(0).length - flatHandles.get(0).length;
                            assert amountNewHandles >= 0;
                            indexOffset += amountNewHandles;
                            flatHandles = newFlats;
                        }

                        //write back new handles as selected path
                        overwriteSelectedPath(new Path(ArrayUtility.transposeMatrix(flatHandles), p.type));
                        setSelectedPointIdx(newSelectedIdx);

                        onOptionsReconfigured();
                        redrawSelectedPathLayer();
                    });
                    inputs.add(() -> new JComponent[]{subdivideButton});
                }

                {
                    SpinnerNumberModel model = new SpinnerNumberModel(options.subdivisions, 1f, 5f, 1f);
                    OptionsLabel label = numericInput("subdivisions", "how often to subdivide", model, f -> {
                        options.subdivisions = f;
                    }, onOptionsReconfigured);
                    inputs.add(label);
                }
                {
                    SpinnerNumberModel model = new SpinnerNumberModel(options.subdivisionRange * 200f, 1, 100, 1f);
                    OptionsLabel label = numericInput("subdivide range %",
                            "how far a new random point is allowed to be placed from the curve",
                            model, f -> {
                                options.subdivisionRange = f / 200f;
                            }, onOptionsReconfigured);
                    inputs.add(label);
                }
            }
            {
                JButton undoButton = new JButton("undo");
                undoButton.addActionListener(e -> {
                            undo();
                            onOptionsReconfigured();
                            redrawSelectedPathLayer();
                        }
                );
                inputs.add(() -> new JComponent[]{undoButton});
            }

            return inputs;
        }
    }
}