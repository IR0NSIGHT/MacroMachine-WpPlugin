package org.ironsight.wpplugin.macromachine.Layers.PathBuilder;

import static java.lang.Math.PI;
import static org.ironsight.wpplugin.macromachine.Gui.HelpDialog.getHelpButton;
import static org.ironsight.wpplugin.macromachine.Layers.PathBuilder.PathToolBackend.*;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE_BITS;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.AWTEventListener;
import java.awt.image.BufferedImage;
import org.ironsight.wpplugin.macromachine.Gui.HelpDialog.HelpItem;
import java.beans.PropertyVetoException;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.JTextComponent;
import javax.vecmath.Point3i;
import javax.vecmath.Point4f;
import org.ironsight.wpplugin.macromachine.operations.PreviewOperation;
import org.ironsight.wpplugin.macromachine.Gui.MapToolOverlay;
import com.github.weisj.jsvg.parser.SVGLoader;
import org.pepsoft.util.AttributeKey;
import org.pepsoft.util.swing.TiledImageViewer;
import org.pepsoft.util.undo.BufferKey;
import org.pepsoft.util.undo.Cloneable;
import org.pepsoft.util.undo.UndoListener;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.WorldPainterView;
import org.pepsoft.worldpainter.brushes.Brush;
import org.pepsoft.worldpainter.operations.AbstractBrushOperation;
import org.pepsoft.worldpainter.operations.PaintOperation;
import org.pepsoft.worldpainter.painting.Paint;

public class PathTool extends AbstractBrushOperation implements PaintOperation, UndoListener, KeyEventDispatcher
{
    private enum TerrainMode {
        DONT_CHANGE("Don't change terrain"), FLATTEN("Flatten terrain"), CARVE("Carve terrain");

        private final String label;

        TerrainMode(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private enum WaterMode {
        SET_WATER("Set water"), SET_WATER_RESPECT_EXISTING_WATERLEVEL(
                "Set water, respect existing waterlevel"), DONT_SET_WATER("Don't set water");

        private final String label;

        WaterMode(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public static final String HELP_TITLE = "Road Tool";
    public static final String HELP_EXPLANATION = """
            Use the Road Tool to connect clicked positions into a smooth path that is blended into existing terrain.
            The path width follows the brush radius and is previewed on the map before applying. Use presets to start
            quickly, then fine-tune blending, slope and transition settings below.
            """;
    public static final List<HelpItem> HELP_ITEMS = List.of(new HelpItem("Right click", "Append a path node"),
            new HelpItem("Left click / drag", "Toggle selection, box select, or move selected nodes"),
            new HelpItem("New path", "Clear the path and preview"),
            new HelpItem("Delete / Ctrl+A / Escape", "Delete selected / select all / clear selection or cancel drag"),
            new HelpItem("Backspace", "Delete last point on current path"),
            new HelpItem("Undo / redo", "Cancel any drag and clear selection before undo or redo"),
            new HelpItem("Apply", "Commit previewed path to terrain / paint / water"),
            new HelpItem("Brush radius", "Controls path width and transition size"),
            new HelpItem("HUD controls", "Show or hide node labels, brush radii, path line and node markers"),
            new HelpItem("River / Road preset", "Apply recommended settings for rivers or roads"),
            new HelpItem("Only downhill", "Restrict path to horizontal or downhill movement"),
            new HelpItem("Snap to terrain", "Force path to follow terrain height"),
            new HelpItem("Set paint", "Apply selected paint along the path"),
            new HelpItem("Set water / Set terrain", "Choose how water level and height are edited"),
            new HelpItem("Curve strength", "-1 to 2, 0 = straight lines"),
            new HelpItem("Limit slope", "Max vertical blocks per 16 horizontal; 0 = unlimited"),
            new HelpItem("Transition multiplier / profile", "Width scale and falloff shape of path edges"));
    // use flat list of floats to not create any serialization dependecies to custom
    // classes. use like a float buffer
    private static final AttributeKey<ArrayList<Float>> PATHHANDLES_KEY = new AttributeKey<>("PATHTOOL-PATHHANDLES",
            new ArrayList<Float>());
    private final JPanel optionsPanel = new JPanel();
    private static final int NODE_DIAMETER = 18;
    private static final int NODE_OUTLINE_DIAMETER = 20;
    private static final int NODE_HIT_RADIUS = 18;
    private final JToggleButton showLabel = hudButton("Show label", "ShowLabel");
    private final JToggleButton showRadius = hudButton("Show radius", "ShowRadius");
    private final JToggleButton showLine = hudButton("Show line", "ShowLine");
    private final JToggleButton showNodes = hudButton("Show nodes", "ShowNodes");
    HashMap<Point3i, FloatTile> cachedTiles = new HashMap<>();
    private Paint paint;
    // only allow downwards movement
    private boolean onlyDown;
    private boolean snapToTerrain;
    private boolean fixHeightTo;
    private boolean usePaint;
    private WaterMode waterMode = WaterMode.DONT_SET_WATER;
    private TerrainMode terrainMode = TerrainMode.FLATTEN;
    private float slopeLimit = 0;
    private float handleStrength = 0f;
    private JCheckBox onlyDownCheckbox;
    private JCheckBox minCheckbox;
    private JCheckBox fixHeightCheckbox;
    private JCheckBox usePaintCheckbox;
    private JComboBox<WaterMode> waterModeDropdown;
    private JComboBox<TerrainMode> terrainModeDropdown;
    private JSpinner limitSlopeSpinner;
    private JSpinner handleFactorSpinner;
    private JSpinner transitionMultiSpinner;
    private CellPreviewPanel slopePreview;
    private JPanel brushQuerschnitt;
    private JComboBox<CrossSectionShape> profilesDropdown;
    private CrossSectionShape brushProfile;
    private float transitionMultiplier = 2;
    private JButton applyButton;
    private final JLabel selectionStatus = new JLabel("0 nodes selected");
    private boolean cursorOverMap;
    private boolean controlsReady;
    private boolean previewQueued;
    private final Set<Integer> selectedNodes = new LinkedHashSet<>();
    private final PathOverlay overlay = new PathOverlay();
    private WorldPainterView overlayView;
    private Dimension overlayDimension;
    private TiledImageViewer.ViewListener previousViewListener;
    private final TiledImageViewer.ViewListener overlayViewListener = view -> {
        if (previousViewListener != null)
            previousViewListener.viewChanged(view);
        syncDimension();
        overlay.repaint();
    };
    private final ComponentAdapter resizeListener = new ComponentAdapter() {
        @Override
        public void componentResized(ComponentEvent event) {
            resizeOverlay();
        }
    };
    private final MouseAdapter selectionListener = new MouseAdapter() {
        @Override
        public void mouseEntered(MouseEvent event) {
            cursorOverMap = true;
        }

        @Override
        public void mouseExited(MouseEvent event) {
            cursorOverMap = false;
        }

        @Override
        public void mousePressed(MouseEvent event) {
            if (SwingUtilities.isLeftMouseButton(event) && !event.isAltDown() && !event.isControlDown()
                    && !event.isShiftDown() && !event.isMetaDown())
                handleSelectionPress(event.getPoint());
        }

        @Override
        public void mouseDragged(MouseEvent event) {
            handleSelectionDrag(event.getPoint());
        }

        @Override
        public void mouseReleased(MouseEvent event) {
            if (SwingUtilities.isLeftMouseButton(event))
                handleSelectionRelease(event.getPoint());
        }
    };
    private Point gestureStart;
    private int pressedNode = -1;
    private boolean gestureDragged;
    private ArrayList<Point4f> gesturePath;
    private ArrayList<Point4f> movedPath;
    private Point gestureWorldStart;
    private final AWTEventListener gestureCapture = event -> {
        if (event instanceof MouseEvent mouse && mouse.getID() == MouseEvent.MOUSE_RELEASED
                && SwingUtilities.isLeftMouseButton(mouse) && gestureStart != null && overlayView != null
                && mouse.getSource() instanceof Component source && source != overlayView) {
            handleSelectionRelease(SwingUtilities.convertPoint(source, mouse.getPoint(), overlayView));
        }
    };

    public PathTool() {
        super("Road Tool", "Create smooth roads", "MacroMachine_RoadTool"); // ONE SHOT OP
        init();
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        if (e.getID() != KeyEvent.KEY_PRESSED || !isActive() || getDimension() == null)
            return false;
        Component focus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (!allowsMapShortcut(overlayView, focus))
            return false;
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE && gestureStart != null && !e.isShiftDown() && !e.isControlDown()
                && !e.isAltDown() && !e.isMetaDown()) {
            cancelGesture();
            selectedNodes.clear();
            selectionChanged();
            return true;
        }
        if (!cursorOverMap || overlayView == null)
            return false;
        if (e.isControlDown() && !e.isShiftDown() && !e.isAltDown() && !e.isMetaDown()
                && e.getKeyCode() == KeyEvent.VK_A) {
            selectedNodes.clear();
            for (int i = 0; i < getCurrentPath().size(); i++)
                selectedNodes.add(i);
            selectionChanged();
            return true;
        }
        if (e.isShiftDown() || e.isControlDown() || e.isAltDown() || e.isMetaDown())
            return false;
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE && (gestureStart != null || !selectedNodes.isEmpty())) {
            cancelGesture();
            selectedNodes.clear();
            selectionChanged();
            return true;
        }
        if (e.getKeyCode() == KeyEvent.VK_DELETE && !selectedNodes.isEmpty()) {
            deleteSelected();
            return true;
        }
        if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE && !getCurrentPath().isEmpty()) {
            deleteLastPathPoint();
            return true;
        }
        return false;
    }

    static boolean allowsMapShortcut(Component view, Component focus) {
        if (view == null)
            return false;
        if (focus == null)
            return true;
        Window window = SwingUtilities.getWindowAncestor(view);
        if (window == null || window != SwingUtilities.getWindowAncestor(focus))
            return false;
        for (Component component = focus; component != null && component != window; component = component.getParent())
            if (component instanceof JTextComponent || component instanceof JSpinner || component instanceof JComboBox)
                return false;
        return true;
    }

    @Override
    protected void activate() throws PropertyVetoException {
        super.activate();
        this.cachedTiles.clear();
        attachOverlay();
        schedulePreview();
    }

    @Override
    protected void deactivate() {
        detachOverlay();
        super.deactivate();
        this.cachedTiles.clear();
    }

    private void attachOverlay() {
        WorldPainterView view = getView();
        if (view == null || overlayView == view)
            return;
        detachOverlay();
        overlayView = view;
        overlayDimension = view.getDimension();
        overlay.setMapView(view);
        previousViewListener = view.getViewListener();
        view.setViewListener(overlayViewListener);
        view.addComponentListener(resizeListener);
        view.addMouseListener(selectionListener);
        view.addMouseMotionListener(selectionListener);
        view.add(overlay);
        view.setComponentZOrder(overlay, 0);
        resizeOverlay();
        overlay.showOverlayText("Road Tool");
    }

    private void resizeOverlay() {
        if (overlayView != null)
            overlay.setBounds(0, 0, overlayView.getWidth(), overlayView.getHeight());
    }

    private void detachOverlay() {
        cancelGesture();
        selectedNodes.clear();
        cursorOverMap = false;
        selectionStatus.setText("0 nodes selected");
        overlay.clearOverlayText();
        overlay.clearInfoText();
        if (overlayView == null)
            return;
        overlayView.removeMouseListener(selectionListener);
        overlayView.removeMouseMotionListener(selectionListener);
        overlayView.removeComponentListener(resizeListener);
        overlayView.remove(overlay);
        if (overlayView.getViewListener() == overlayViewListener)
            overlayView.setViewListener(previousViewListener);
        overlayView.repaint();
        overlay.setMapView(null);
        overlayView = null;
        overlayDimension = null;
        previousViewListener = null;
    }

    private void syncDimension() {
        if (overlayView != null && overlayDimension != overlayView.getDimension()) {
            overlayDimension = overlayView.getDimension();
            selectedNodes.clear();
            cancelGesture();
            cachedTiles.clear();
            selectionStatus.setText("0 nodes selected");
        }
    }

    void handleSelectionPress(Point point) {
        syncDimension();
        if (overlayView == null || overlayDimension == null)
            return;
        cancelGesture();
        gestureStart = new Point(point);
        pressedNode = nearestNode(point);
        gestureDragged = false;
        gesturePath = getCurrentPath();
        gestureWorldStart = overlayView.viewToWorld(point);
        Toolkit.getDefaultToolkit().addAWTEventListener(gestureCapture, AWTEvent.MOUSE_EVENT_MASK);
        if (pressedNode < 0)
            overlay.startDrag(point);
    }

    void handleSelectionDrag(Point point) {
        syncDimension();
        if (gestureStart == null)
            return;
        if (gestureStart.distanceSq(point) >= 9)
            gestureDragged = true;
        if (pressedNode < 0 && gestureDragged)
            overlay.updateDrag(point);
        else if (pressedNode >= 0 && gestureDragged) {
            if (!selectedNodes.contains(pressedNode)) {
                selectedNodes.clear();
                selectedNodes.add(pressedNode);
            }
            movedPath = movedPath(point);
            overlay.repaint();
        }
    }

    void handleSelectionRelease(Point point) {
        syncDimension();
        if (gestureStart == null)
            return;
        try {
            handleSelectionDrag(point);
            if (gestureDragged && pressedNode < 0) {
                Rectangle bounds = overlay.getDragBounds();
                selectedNodes.clear();
                var path = getCurrentPath();
                for (int i = 0; i < path.size(); i++) {
                    Point4f node = path.get(i);
                    if (bounds.contains(node.x, node.y))
                        selectedNodes.add(i);
                }
            } else if (gestureDragged && pressedNode >= 0 && movedPath != null) {
                completeEdit(movedPath);
            } else if (!gestureDragged) {
                if (pressedNode < 0)
                    selectedNodes.clear();
                else if (!selectedNodes.add(pressedNode))
                    selectedNodes.remove(pressedNode);
            }
            selectionChanged();
        } finally {
            cancelGesture();
            overlay.repaint();
        }
    }

    private void selectionChanged() {
        String text = selectedNodes.size() + " nodes selected";
        selectionStatus.setText(text);
        overlay.showTransientInfoText(text, 1800);
        overlay.repaint();
    }

    private void cancelGesture() {
        Toolkit.getDefaultToolkit().removeAWTEventListener(gestureCapture);
        gestureStart = null;
        pressedNode = -1;
        gestureDragged = false;
        gesturePath = null;
        movedPath = null;
        gestureWorldStart = null;
        overlay.endDrag();
    }

    private ArrayList<Point4f> movedPath(Point point) {
        Point world = overlayView.viewToWorld(point);
        int dx = world.x - gestureWorldStart.x;
        int dy = world.y - gestureWorldStart.y;
        var result = new ArrayList<Point4f>();
        for (int i = 0; i < gesturePath.size(); i++) {
            var node = new Point4f(gesturePath.get(i));
            if (selectedNodes.contains(i)) {
                node.x += dx;
                node.y += dy;
                int x = Math.round(node.x), y = Math.round(node.y);
                if (overlayDimension.getTile(x >> TILE_SIZE_BITS, y >> TILE_SIZE_BITS) == null)
                    return null;
                node.z = overlayDimension.getHeightAt(x, y);
                if (!Float.isFinite(node.z) || node.z < overlayDimension.getMinHeight()
                        || node.z >= overlayDimension.getMaxHeight())
                    return null;
            }
            result.add(node);
        }
        return result;
    }

    int nearestNode(Point point) {
        if (overlayView == null)
            return -1;
        int nearest = -1;
        double distance = NODE_HIT_RADIUS * NODE_HIT_RADIUS;
        var path = getCurrentPath();
        for (int i = 0; i < path.size(); i++) {
            Point4f node = path.get(i);
            Point screen = overlayView.worldToView(Math.round(node.x), Math.round(node.y));
            double candidate = screen.distanceSq(point);
            if (candidate <= NODE_HIT_RADIUS * NODE_HIT_RADIUS && (nearest < 0 || candidate < distance)) {
                nearest = i;
                distance = candidate;
            }
        }
        return nearest;
    }

    Set<Integer> selectedNodes() {
        syncDimension();
        return Set.copyOf(selectedNodes);
    }

    private class PathOverlay extends MapToolOverlay
    {
        private List<Point4f> cachedNodes;
        private float cachedStrength;
        private List<Point4f> cachedCurve;

        PathOverlay() {
            super(loadPathIcon());
        }

        private List<Point4f> curveFor(List<Point4f> nodes) {
            if (cachedNodes == null || !cachedNodes.equals(nodes)
                    || Float.compare(cachedStrength, handleStrength) != 0) {
                cachedNodes = nodes.stream().map(Point4f::new).toList();
                cachedStrength = handleStrength;
                cachedCurve = nodes.size() < 2 ? List.of() : getPathFromNodes(cachedNodes, handleStrength).path;
            }
            return cachedCurve;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            syncDimension();
            super.paintComponent(graphics);
            if (overlayView == null)
                return;
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                var path = movedPath != null ? movedPath : getCurrentPath();
                selectedNodes.removeIf(index -> index >= path.size());
                if (showRadius.isSelected()) {
                    g.setColor(new Color(255, 255, 255, 150));
                    for (Point4f node : path) {
                        Point left = overlayView.worldToView(Math.round(node.x - node.w), Math.round(node.y - node.w));
                        Point right = overlayView.worldToView(Math.round(node.x + node.w), Math.round(node.y + node.w));
                        g.drawOval(Math.min(left.x, right.x), Math.min(left.y, right.y), Math.abs(right.x - left.x),
                                Math.abs(right.y - left.y));
                    }
                }
                if (showLine.isSelected()) {
                    var curve = curveFor(path);
                    g.setColor(Color.WHITE);
                    for (int i = 1; i < curve.size(); i++) {
                        Point4f a = curve.get(i - 1), b = curve.get(i);
                        Point start = overlayView.worldToView(Math.round(a.x), Math.round(a.y));
                        Point end = overlayView.worldToView(Math.round(b.x), Math.round(b.y));
                        g.drawLine(start.x, start.y, end.x, end.y);
                    }
                }
                for (int i = 0; i < path.size(); i++) {
                    Point4f node = path.get(i);
                    Point point = overlayView.worldToView(Math.round(node.x), Math.round(node.y));
                    if (showNodes.isSelected()) {
                        g.setColor(Color.WHITE);
                        g.fillOval(point.x - NODE_DIAMETER / 2, point.y - NODE_DIAMETER / 2, NODE_DIAMETER,
                                NODE_DIAMETER);
                        g.setColor(selectedNodes.contains(i) ? Color.RED : Color.DARK_GRAY);
                        g.setStroke(new BasicStroke(selectedNodes.contains(i) ? 2f : 1f));
                        g.drawOval(point.x - NODE_OUTLINE_DIAMETER / 2, point.y - NODE_OUTLINE_DIAMETER / 2,
                                NODE_OUTLINE_DIAMETER, NODE_OUTLINE_DIAMETER);
                    }
                    if (showLabel.isSelected()) {
                        String label = Integer.toString(i + 1);
                        int x = point.x + NODE_OUTLINE_DIAMETER / 2 + 4;
                        int y = point.y + g.getFontMetrics().getAscent() / 2;
                        g.setColor(Color.BLACK);
                        g.drawString(label, x - 1, y);
                        g.drawString(label, x + 1, y);
                        g.drawString(label, x, y - 1);
                        g.drawString(label, x, y + 1);
                        g.setColor(Color.WHITE);
                        g.drawString(label, x, y);
                    }
                }
            } finally {
                g.dispose();
            }
        }
    }

    private JToggleButton hudButton(String text, String name) {
        var button = new JToggleButton(text, true);
        button.setName("PathTool." + name);
        button.addActionListener(event -> overlay.repaint());
        return button;
    }

    private static BufferedImage loadPathIcon() {
        var url = Objects.requireNonNull(PathTool.class.getResource("/icons/waypoints.svg"));
        var document = Objects.requireNonNull(new SVGLoader().load(url));
        var icon = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = icon.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            var viewBox = document.viewBox();
            graphics.scale(256 / viewBox.getWidth(), 256 / viewBox.getHeight());
            document.render(null, graphics);
        } finally {
            graphics.dispose();
        }
        return icon;
    }

    private void init() {
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        optionsPanel.setBorder(new EmptyBorder(8, 8, 8, 8));
        optionsPanel.add(getHelpButton(HELP_TITLE, HELP_EXPLANATION, HELP_ITEMS));
        JPanel editingPanel = new JPanel();
        JButton newPath = new JButton("New path");
        newPath.setName("PathTool.NewPath");
        newPath.addActionListener(e -> resetPath());
        editingPanel.add(newPath);
        editingPanel.add(selectionStatus);
        optionsPanel.add(editingPanel);
        JPanel hudPanel = new JPanel(new GridLayout(2, 2, 4, 4));
        hudPanel.setBorder(BorderFactory.createTitledBorder("HUD"));
        hudPanel.add(showLabel);
        hudPanel.add(showRadius);
        hudPanel.add(showLine);
        hudPanel.add(showNodes);
        optionsPanel.add(hudPanel);
        {
            JPanel presetPanel = new JPanel();
            JButton riverPresetButton = new JButton("River preset");
            riverPresetButton.addActionListener(
                    l -> applyPreset(true, true, false, WaterMode.SET_WATER, TerrainMode.CARVE, 1f, 5f, 0f, null));
            presetPanel.add(riverPresetButton);
            JButton roadPresetButton = new JButton("Road preset");
            roadPresetButton.addActionListener(l -> applyPreset(false, true, true, WaterMode.DONT_SET_WATER,
                    TerrainMode.FLATTEN, .3f, 1f, 4f, "Triangle"));
            presetPanel.add(roadPresetButton);
            optionsPanel.add(presetPanel);
        }
        {
            applyButton = new JButton("Apply");
            applyButton.setName("PathTool.Apply");
            applyButton.setToolTipText("apply the path to the map.");
            applyButton.addActionListener((e) -> {
                var dim = getDimension();
                if (dim == null)
                    return;
                boolean inhibited = dim.isEventsInhibited();
                if (!inhibited)
                    dim.setEventsInhibited(true);
                try {
                    cachedTiles.clear();
                    OnSmoothPath(copyPath(getCurrentPath()), false);
                } finally {
                    cachedTiles.clear();
                    if (!inhibited)
                        dim.setEventsInhibited(false);
                }
                refreshPreview();
                overlay.showTransientInfoText("Path applied", 1800);
            });
            optionsPanel.add(applyButton);
        }

        {
            onlyDownCheckbox = new JCheckBox("onlyDownCheckbox");
            onlyDownCheckbox.setToolTipText("If active, the path will only move horizontally or downwards.");
            onlyDownCheckbox.addActionListener(l -> {
                this.onlyDown = onlyDownCheckbox.isSelected();
                schedulePreview();
            });
            this.onlyDown = onlyDownCheckbox.isSelected();
            optionsPanel.add(onlyDownCheckbox);
        }
        {
            minCheckbox = new JCheckBox("snap to terrain");
            minCheckbox.setToolTipText("if active, the path will follow the terrain instead of building 'bridges'.");
            minCheckbox.addActionListener(l -> {
                this.snapToTerrain = minCheckbox.isSelected();
                schedulePreview();
            });
            this.snapToTerrain = minCheckbox.isSelected();
            optionsPanel.add(minCheckbox);
        }
        {
            usePaintCheckbox = new JCheckBox("usePaintCheckbox");
            usePaintCheckbox.setSelected(true);
            usePaintCheckbox.setToolTipText("set currently selected paint along the path");
            usePaintCheckbox.addActionListener(l -> {
                this.usePaint = usePaintCheckbox.isSelected();
                schedulePreview();
            });
            this.usePaint = usePaintCheckbox.isSelected();
            optionsPanel.add(usePaintCheckbox);
        }
        {
            waterModeDropdown = new JComboBox<>(WaterMode.values());
            waterModeDropdown.setSelectedItem(waterMode);
            waterModeDropdown.setToolTipText("Controls how water level is changed along the path.");
            waterModeDropdown.addActionListener(l -> {
                this.waterMode = (WaterMode) waterModeDropdown.getSelectedItem();
                schedulePreview();
            });
            JPanel panel = new JPanel();
            panel.add(new JLabel("set water:"));
            panel.add(waterModeDropdown);
            optionsPanel.add(panel);
        }
        {
            terrainModeDropdown = new JComboBox<>(TerrainMode.values());
            terrainModeDropdown.setSelectedItem(terrainMode);
            terrainModeDropdown.setToolTipText("Controls how terrain height is changed along the path.");
            terrainModeDropdown.addActionListener(l -> {
                this.terrainMode = (TerrainMode) terrainModeDropdown.getSelectedItem();
                schedulePreview();
            });
            JPanel panel = new JPanel();
            panel.add(new JLabel("set terrain:"));
            panel.add(terrainModeDropdown);
            optionsPanel.add(panel);
        }
        {
            fixHeightCheckbox = new JCheckBox("fixHeightCheckbox");
            fixHeightCheckbox.setToolTipText("If active, the path will only move at the specified height.");
            fixHeightCheckbox.addActionListener(l -> {
                this.fixHeightTo = fixHeightCheckbox.isSelected();
                schedulePreview();
            });
            this.fixHeightTo = fixHeightCheckbox.isSelected();
            // optionsPanel.add(fixHeightCheckbox); // FIXME: disabled because it causes
            // wierd artifacts across tile lines
        }
        {
            handleFactorSpinner = new JSpinner(new SpinnerNumberModel(handleStrength, -1d, 2d, 0.1d));
            handleFactorSpinner.setToolTipText(
                    "Controls how curve the path is. 0 = straight lines, 1 = normal curves, 2 = exaggerated curves, -1 = cursive");
            handleFactorSpinner.addChangeListener(l -> {
                handleStrength = ((Number) handleFactorSpinner.getValue()).floatValue();
                overlay.repaint();
                schedulePreview();
            });
            JPanel panel = new JPanel();
            panel.add(new JLabel("Curve strength"));
            panel.add(handleFactorSpinner);
            optionsPanel.add(panel);
        }
        {
            limitSlopeSpinner = new JSpinner(new SpinnerNumberModel(0d, 0d, 100d, 1d));
            limitSlopeSpinner.setToolTipText(
                    "Limits the allowed slope to x block vertical per 16 blocks horizontal. 0 to disable.");
            limitSlopeSpinner.addChangeListener(l -> {
                slopeLimit = ((Number) limitSlopeSpinner.getValue()).floatValue();
                if (slopePreview != null)
                    slopePreview.repaint();
                schedulePreview();
            });
            slopeLimit = ((Number) limitSlopeSpinner.getValue()).floatValue();
            JPanel panel = new JPanel();
            panel.add(new JLabel("Limit slope to x/16 blocks"));
            panel.add(limitSlopeSpinner);
            optionsPanel.add(panel);

            slopePreview = new CellPreviewPanel((x, y) -> {
                if (slopeLimit == 0)
                    return false;
                float centreRow = (slopePreview.getCellRows() - 1) / 2f;
                float centreColumn = (slopePreview.getCellColumns() - 1) / 2f;
                int hypotenuseRow = Math.min(slopePreview.getCellRows() - 1,
                        Math.max(0, Math.round(centreRow + (x - centreColumn) * slopeLimit / 16f)));
                return y == hypotenuseRow;
            });
            slopePreview.setToolTipText("Shows the maximum allowed slope; 0 means unlimited");
            optionsPanel.add(slopePreview);
        }
        {
            transitionMultiSpinner = new JSpinner(new SpinnerNumberModel(1d, 0d, 100d, .25d));
            transitionMultiSpinner.setToolTipText(
                    "How many times bigger the transition is compared to the brush radius. 0 = no transition, 1 = one brush radius width transition");
            transitionMultiSpinner.addChangeListener(l -> {
                transitionMultiplier = ((Number) transitionMultiSpinner.getValue()).floatValue() + 1;
                if (brushQuerschnitt != null)
                    brushQuerschnitt.repaint();
                schedulePreview();
            });
            transitionMultiSpinner.setValue(1d);
            JPanel panel = new JPanel();
            panel.add(new JLabel("Transition multiplier"));
            panel.add(transitionMultiSpinner);
            optionsPanel.add(panel);
        }
        {
            brushQuerschnitt = new JPanel() {
                @Override
                public void paint(Graphics g) {
                    super.paint(g);
                    if (brushProfile == null)
                        return;
                    g.setColor(Color.WHITE);
                    g.fillRect(0, 0, getWidth(), getHeight());

                    g.setColor(Color.BLACK);
                    for (int x = 0; x < getWidth() / 2f; x++) {
                        float strengthAtX = filterStrengthFor(x * transitionMultiplier, getWidth() / 2f,
                                getTransitionMultiplier(), brushProfile);
                        int height = Math.round(getHeight() * strengthAtX);
                        if (height > getHeight() * 0.99f)
                            g.setColor(Color.RED);
                        else
                            g.setColor(Color.BLACK);

                        g.fillRect(x + getWidth() / 2, getHeight() - height, 1, getHeight());
                        g.fillRect(getWidth() - (x + getWidth() / 2), getHeight() - height, 1, getHeight());

                        // g.fillRect(getW x + getWidth()/2, getHeight() - height, 1, getHeight());
                    }
                }
            };
            brushQuerschnitt.setPreferredSize(new java.awt.Dimension(0, 48));
            brushQuerschnitt.setMinimumSize(new java.awt.Dimension(0, 48));
            brushQuerschnitt.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 48));
            brushQuerschnitt.setToolTipText("Shows the cross section of the brush strength. Red area = 100% strength");
            optionsPanel.add(brushQuerschnitt);
        }
        {
            DefaultComboBoxModel<CrossSectionShape> model = new DefaultComboBoxModel<>();
            model.addElement(new CrossSectionShape("Square Root", "") {
                @Override
                public float getStrengthAt(float t) {
                    return (float) Math.sqrt(t);
                }
            });
            model.addElement(new CrossSectionShape("Logarithmic", "") {
                final float log_2 = (float) Math.log(2);

                @Override
                public float getStrengthAt(float t) {
                    return (float) (Math.log(t + 1) / log_2);
                }
            });
            model.addElement(new CrossSectionShape("Triangle", "") {
                @Override
                public float getStrengthAt(float t) {
                    return t;
                }
            });
            model.addElement(new CrossSectionShape("Sinus", "") {
                @Override
                public float getStrengthAt(float t) {
                    return (float) (Math.sin((t - 0.5f) * PI) / 2f + 0.5f);
                }
            });
            model.addElement(new CrossSectionShape("Quadratic", "") {
                @Override
                public float getStrengthAt(float t) {
                    return t * t;
                }
            });
            model.addElement(new CrossSectionShape("Cubic", "") {
                @Override
                public float getStrengthAt(float t) {
                    return t * t * t;
                }
            });

            profilesDropdown = new JComboBox<>(model);
            profilesDropdown.setName("PathTool.TransitionProfiles");
            profilesDropdown.addActionListener(l -> {
                this.brushProfile = (CrossSectionShape) profilesDropdown.getSelectedItem();
                brushQuerschnitt.repaint();
                schedulePreview();
            });
            profilesDropdown.setSelectedItem(model.getElementAt(3));
            this.brushProfile = (CrossSectionShape) profilesDropdown.getSelectedItem();
            JPanel panel = new JPanel();
            panel.add(new JLabel("Transition profile"));
            panel.add(profilesDropdown);
            optionsPanel.add(panel);
        }

        updateCheckboxTexts();
        controlsReady = true;
    }

    private void applyPreset(boolean onlyDown, boolean snapToTerrain, boolean usePaint, WaterMode waterMode,
            TerrainMode terrainMode, float curveStrength, float transition, float slopeLimit,
            String transitionProfile) {
        this.onlyDown = onlyDown;
        this.snapToTerrain = snapToTerrain;
        this.usePaint = usePaint;
        this.waterMode = waterMode;
        this.terrainMode = terrainMode;
        onlyDownCheckbox.setSelected(onlyDown);
        minCheckbox.setSelected(snapToTerrain);
        usePaintCheckbox.setSelected(usePaint);
        waterModeDropdown.setSelectedItem(waterMode);
        terrainModeDropdown.setSelectedItem(terrainMode);
        handleFactorSpinner.setValue((double) curveStrength);
        transitionMultiSpinner.setValue((double) transition);
        limitSlopeSpinner.setValue((double) slopeLimit);
        if (transitionProfile != null) {
            for (int i = 0; i < profilesDropdown.getItemCount(); i++) {
                if (transitionProfile.equals(String.valueOf(profilesDropdown.getItemAt(i)))) {
                    profilesDropdown.setSelectedIndex(i);
                    break;
                }
            }
        }
        updateCheckboxTexts();
        schedulePreview();
    }

    private void updateCheckboxTexts() {
        onlyDownCheckbox.setText("Only downhill");
        fixHeightCheckbox.setText("Fix height to: " + getFixHeight());
        usePaintCheckbox.setText("Set paint");
    }

    @Override
    public JPanel getOptionsPanel() {
        return optionsPanel;
    }

    private float getFixHeight() {
        var currentPath = getCurrentPath();
        return currentPath.isEmpty() ? 62 : currentPath.get(currentPath.size() - 1).z;
    }

    @Override
    protected void brushChanged(Brush newBrush) {
        super.brushChanged(newBrush);
        brushQuerschnitt.revalidate();
        brushQuerschnitt.repaint();
    }

    ArrayList<Point4f> getCurrentPath() {
        Dimension dimension = getView() == null ? null : getDimension();
        if (dimension == null)
            return new ArrayList<>();
        // pull data from attributes
        var pathHandles = new ArrayList<Point4f>();
        var rawData = dimension.getAttribute(PATHHANDLES_KEY);
        assert rawData.size() % 4 == 0;
        for (int i = 0; i < rawData.size(); i += 4) {
            pathHandles.add(new Point4f(rawData.get(i), rawData.get(i + 1), rawData.get(i + 2), rawData.get(i + 3)));
        }
        return pathHandles;
    }

    private void setCurrentPath(ArrayList<Point4f> pathHandles) {
        Dimension dimension = getView() == null ? null : getDimension();
        if (pathHandles != null && dimension != null) {
            var flatData = new ArrayList<Float>();
            for (var point : pathHandles) {
                flatData.add(point.x);
                flatData.add(point.y);
                flatData.add(point.z);
                flatData.add(point.w);
            }
            dimension.setAttribute(PATHHANDLES_KEY, flatData, true);
        }
    }

    private void deleteLastPathPoint() {
        cancelGesture();
        var pathHandles = getCurrentPath();
        if (pathHandles.isEmpty())
            return;
        pathHandles.removeLast();
        selectedNodes.removeIf(index -> index >= pathHandles.size());
        completeEdit(pathHandles);
        selectionChanged();
    }

    private void deleteSelected() {
        var path = getCurrentPath();
        for (int i = path.size() - 1; i >= 0; i--)
            if (selectedNodes.contains(i))
                path.remove(i);
        selectedNodes.clear();
        cancelGesture();
        completeEdit(path);
        selectionChanged();
    }

    private void resetPath() {
        cancelGesture();
        selectedNodes.clear();
        cachedTiles.clear();
        completeEdit(new ArrayList<>());
        selectionChanged();
        overlay.showTransientInfoText("New path", 1800);
    }

    private static ArrayList<Point4f> copyPath(List<Point4f> path) {
        return new ArrayList<>(path.stream().map(Point4f::new).toList());
    }

    private void schedulePreview() {
        if (!controlsReady || previewQueued)
            return;
        previewQueued = true;
        SwingUtilities.invokeLater(() -> {
            previewQueued = false;
            if (isActive() && getDimension() != null)
                refreshPreview();
        });
    }

    private void refreshPreview() {
        Dimension dimension = getDimension();
        if (dimension == null)
            return;
        boolean inhibited = dimension.isEventsInhibited();
        if (!inhibited)
            dimension.setEventsInhibited(true);
        try {
            cachedTiles.clear();
            dimension.clearLayerData(PreviewOperation.annotationLayer);
            var path = getCurrentPath();
            if (!path.isEmpty())
                OnSmoothPath(copyPath(path), true);
            else
                cachedTiles.clear();
        } finally {
            if (!inhibited)
                dimension.setEventsInhibited(false);
        }
    }

    @Override
    protected void tick(int centreX, int centreY, boolean inverse, boolean first, float dynamicLevel) {
        if (!inverse || !first || isAltDown() || isCtrlDown() || isShiftDown())
            return;
        Dimension dim = getDimension();
        if (dim == null || getBrush() == null
                || dim.getTile(centreX >> TILE_SIZE_BITS, centreY >> TILE_SIZE_BITS) == null)
            return;
        cancelGesture();
        var pathHandles = getCurrentPath();
        int pathRadius = getBrush().getEffectiveRadius();
        pathHandles.add(new Point4f(centreX, centreY, dim.getHeightAt(centreX, centreY), pathRadius));
        completeEdit(pathHandles);
    }

    private void completeEdit(ArrayList<Point4f> path) {
        Dimension dimension = getDimension();
        if (dimension == null)
            return;
        boolean inhibited = dimension.isEventsInhibited();
        if (!inhibited)
            dimension.setEventsInhibited(true);
        try {
            setCurrentPath(path);
            cachedTiles.clear();
            dimension.clearLayerData(PreviewOperation.annotationLayer);
            if (!path.isEmpty())
                OnSmoothPath(copyPath(path), true);
            else
                cachedTiles.clear();
        } finally {
            if (!inhibited)
                dimension.setEventsInhibited(false);
        }
        SwingUtilities.invokeLater(this::updateCheckboxTexts);
        overlay.repaint();
    }

    private void OnSmoothPath(List<Point4f> pathHandles, boolean onlyPreview) {
        if (pathHandles.isEmpty()) {
            return;
        }

        Dimension dimension = getDimension();

        {
            // get new path section and append it
            var pathRes = getPathFromNodes(pathHandles, handleStrength);

            dimension.clearLayerData(PreviewOperation.annotationLayer);

            final var path = pathRes.path;
            // // cut off last segment that will change on next click anyways
            // Point4f secondLastHandle = pathHandles.get(pathHandles.size() - 2);
            // final var path = pathRes.path.subList(0,
            // pathRes.handlesToPathIndex.getOrDefault(secondLastHandle,
            // pathHandles.size()));
            //
            // // changed segment: thirdlast to secondLast handle
            // Point4f thirdLastHandle = pathHandles.get(pathHandles.size() - 3);
            // var newPathSection =
            // path.subList(pathRes.handlesToPathIndex.getOrDefault(thirdLastHandle, 0),
            // pathRes.handlesToPathIndex.getOrDefault(secondLastHandle,
            // pathHandles.size()));

            // mutate path with filters based on user input
            if (snapToTerrain)
                PathToolBackend.forcePathToMinPos(path,
                        point4f -> dimension.getHeightAt(Math.round(point4f.x), Math.round(point4f.y)));
            if (onlyDown)
                PathToolBackend.forcePathOnlyDownhill(path);
            if (fixHeightTo)
                PathToolBackend.forcePathToHeight(path, getFixHeight());

            if (slopeLimit != 0)
                PathToolBackend.enforceSlopeLimit(path, slopeLimit);

            forceRadiusAtLeast(path, .5f); // always at least 1 thick

            // collect tiles where the newly added path section passed through
            Set<Point3i> newPathTiles = PathToolBackend.collectTilesAroundPath(path, transitionMultiplier);

            // cache all tiles that might be affected
            for (var tilePos : newPathTiles) {
                var tile = dimension.getTile(tilePos.x, tilePos.y);
                if (tile == null)
                    continue;
                if (cachedTiles.containsKey(tilePos))
                    continue;
                var clone = PathToolBackend.cloneHeightMapData(tile);
                cachedTiles.put(tilePos, clone);
            }

            HashMap<Point2i, Point4f> posToData = new HashMap<>();
            path.forEach(pathPoint -> {
                var pos = new Point2i(pathPoint.x, pathPoint.y);
                posToData.put(pos, pathPoint);
            });

            // iterate all cachedTiles and all points inside the cachedTiles
            System.out.println("PROCESSING " + newPathTiles.size() + " Tiles");
            AtomicInteger totalProcessedPath = new AtomicInteger();
            AtomicInteger totalTiles = new AtomicInteger();
            HashMap<Point3i, FloatTile> paintOutputMap = new HashMap<>();
            HashMap<Point3i, FloatTile> waterHeightMap = new HashMap<>();
            var heightInfoTiles = newPathTiles.stream().map(tilePos -> {
                Point2i tileAreaStart = new Point2i((tilePos.x) << TILE_SIZE_BITS, (tilePos.y) << TILE_SIZE_BITS);
                Point2i tileAreaEnd = new Point2i(TILE_SIZE + ((tilePos.x) << TILE_SIZE_BITS),
                        TILE_SIZE + ((tilePos.y) << TILE_SIZE_BITS));
                var paintOutput = new FloatTile(tilePos);
                paintOutputMap.put(tilePos, paintOutput);
                var waterHeightTile = new FloatTile(tilePos);
                waterHeightMap.put(tilePos, waterHeightTile);

                return PathToolBackend.applyToTile(cachedTiles.get(tilePos), paintOutput, waterHeightTile, tilePos,
                        brushProfile,
                        PathToolBackend.getSubPathFor(tileAreaStart, tileAreaEnd, path, transitionMultiplier),
                        getTransitionMultiplier(), terrainMode == TerrainMode.CARVE); // FIXME use actual "dig down"
                                                                                      // checkbox?
            }).toList();
            heightInfoTiles.forEach(heightInfoTile -> {
                if (heightInfoTile == null)
                    return;
                Tile wpTile = dimension.getTileForEditing(heightInfoTile.tilePosX, heightInfoTile.tilePosY);
                if (wpTile == null)
                    return;
                if (!onlyPreview && terrainMode != TerrainMode.DONT_CHANGE)
                    PathToolBackend.writeHeightMapDataToTile(heightInfoTile, wpTile);
                if (!onlyPreview && waterMode != WaterMode.DONT_SET_WATER) {
                    var waterHeightTile = waterHeightMap.get(new Point3i(wpTile.getX(), wpTile.getY(), 0));
                    PathToolBackend.writeWaterHeightDataToDimension(waterHeightTile, wpTile,
                            waterMode == WaterMode.SET_WATER_RESPECT_EXISTING_WATERLEVEL);
                }
                if (!onlyPreview && usePaint && getPaint() != null) {
                    var paintTile = paintOutputMap.get(new Point3i(wpTile.getX(), wpTile.getY(), 0));
                    PathToolBackend.writePaintDataToDimension(paintTile, dimension, getPaint());
                }
                if (onlyPreview) {
                    var paintTile = paintOutputMap.get(new Point3i(wpTile.getX(), wpTile.getY(), 0));
                    PathToolBackend.writeBinaryLayerDataToDimension(paintTile, wpTile,
                            PreviewOperation.annotationLayer);
                }
            });

            System.out.printf("Processed %d cachedTiles, %d path points total, current path length %d",
                    totalTiles.get(), totalProcessedPath.get(), path.size());
        }
    }

    /**
     * how much larger the transition is compared to the brush radius 1 = no
     * transition 2 = double radius
     *
     * @return
     */
    private float getTransitionMultiplier() {
        return transitionMultiplier;
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
    public void savePointArmed() {
    }

    @Override
    public void savePointCreated() {
    }

    @Override
    public void undoPerformed() {
    }

    @Override
    public void redoPerformed() {
    }

    @Override
    public void bufferChanged(BufferKey<?> key) {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            PathTool pathTool = new PathTool();
            JFrame frame = new JFrame("Road Tool Options");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(pathTool.getOptionsPanel());
            frame.pack();
            frame.setLocationByPlatform(true);
            frame.setVisible(true);
        });
    }

    private static class PathHandlesContainer implements Cloneable, Serializable
    {
        @Serial
        private static final long serialVersionUID = -1L;
        List<Point4f> data;

        public PathHandlesContainer(List<Point4f> data) {
            this.data = data;
        }

        @Override
        public PathHandlesContainer clone() {
            return new PathHandlesContainer(data.stream().map(p -> (Point4f) p.clone()).collect(Collectors.toList()));
        }
    }

    private class PathPosition
    {
        final float x, y, height, pathRadius, transitionRadius;

        private PathPosition(float x, float y, float pathRadius, float transitionRadius, float height) {
            this.x = x;
            this.y = y;
            this.pathRadius = pathRadius;
            this.transitionRadius = transitionRadius;
            this.height = height;
        }

        PathPosition interpolateWith(PathPosition other, float t) {
            float t1 = 1 - t;
            return new PathPosition(this.x * t + other.x * t1, this.y * t + other.y * t1,
                    this.pathRadius * t + other.pathRadius * t1,
                    this.transitionRadius * t + other.transitionRadius * t1, this.height * t + other.height * t1);
        }
    }
}
