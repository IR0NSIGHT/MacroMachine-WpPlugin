package org.ironsight.wpplugin.macromachine.Layers.PathBuilder;

import static java.lang.Math.PI;
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
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import org.ironsight.wpplugin.macromachine.Gui.HelpDialog.HelpItem;
import java.beans.PropertyVetoException;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.JTextComponent;
import javax.vecmath.Point3i;
import javax.vecmath.Point4f;
import org.ironsight.wpplugin.macromachine.operations.PreviewOperation;
import org.ironsight.wpplugin.macromachine.operations.ProviderType;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IPositionValueSetter;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.InputOutputProvider;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IoParameter;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.TerrainProvider;
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

    public static final String HELP_TITLE = "Path Tool";
    public static final String HELP_EXPLANATION = """
            Use the Path Tool to connect clicked positions into a smooth path that is blended into existing terrain.
            The path width follows the brush radius and is previewed on the map before applying. Use presets to start
            quickly, then fine-tune blending, slope and transition settings below.
            """;
    public static final List<HelpItem> HELP_ITEMS = List.of(
            new HelpItem("Ctrl + left click (empty map)", "Add a node after the last selected node (or at the end)"),
            new HelpItem("Double-tap Ctrl / Add button",
                    "Add mode: left click on empty map adds nodes; Esc or double-tap to leave"),
            new HelpItem("Left click / drag", "Select one, box select, or move selected nodes"),
            new HelpItem("Ctrl + left click / drag",
                    "Toggle node in selection, add box to selection, or move the group"),
            new HelpItem("Node / Segment / Path menus (top left)",
                    "Node: randomize position, randomize radius, snap to terrain, only downhill. Segment: subdivide. Path: new path, reverse direction, apply as road / river / paint (settings dialog), help. Preview, then Apply (Enter) or Cancel (Esc); type a number for the main value, Tab for the next"),
            new HelpItem("Editor button (top left)", "Shows the current editor; click to pick another"),
            new HelpItem("Click selected node", "Cycle radius, curve strength, height and transform handles"),
            new HelpItem("Transform editor",
                    "Box around the selection: drag the corner arrow to scale, the side / bottom arrows to stretch along the box, the top handle to rotate, the left handle to move (a group does not drag by its nodes; a single node does); drag the pivot (crosshair) to rotate / scale around another point, double-click it to put it back in the centre; typed numbers are degrees or the scale factor. A single node only gets the move handle"),
            new HelpItem("Drag radius / tangent / height handle",
                    "Scale radius of selected nodes / change curve strength / raise or lower selected nodes"),
            new HelpItem("Shift while dragging", "Precise drag: nodes and handles move at 1/10 speed"),
            new HelpItem("Type a number (nodes selected)",
                    "Sets radius / height above ground / curve strength of the selected nodes; - negates, / inverts; Enter or left click applies, Esc or right click cancels"),
            new HelpItem("New path", "Clear the path and preview"),
            new HelpItem("Reverse direction", "Reverse node order, e.g. so a river flows the other way"),
            new HelpItem("Delete / Ctrl+A / Escape", "Delete selected / select all / clear selection or cancel drag"),
            new HelpItem("Backspace", "Delete last point on current path"),
            new HelpItem("Undo / redo", "Cancel any drag and clear selection before undo or redo"),
            new HelpItem("Apply", "Commit previewed path to terrain / paint / water"),
            new HelpItem("Brush radius", "Controls path width and transition size"),
            new HelpItem("Overlays (top left)",
                    "Toggle all overlays; the arrow lists each part with toggle / Only / All but buttons"),
            new HelpItem("Only downhill", "Restrict path to horizontal or downhill movement"),
            new HelpItem("Snap to terrain", "Force path to follow terrain height"),
            new HelpItem("Set paint", "Paint the road with the chosen output (terrain, layer, ...) and value"),
            new HelpItem("Set water", "Set the water level to the path height"),
            new HelpItem("Flatten road / Carve river bed", "Level the ground under a road / dig a bed under a river"),
            new HelpItem("Curve strength", "-1 to 2, 0 = straight lines"),
            new HelpItem("Limit slope", "Max vertical blocks per 16 horizontal; 0 = unlimited"),
            new HelpItem("Transition multiplier / profile", "Width scale and falloff shape of path edges"));
    // use flat list of floats to not create any serialization dependecies to custom
    // classes. use like a float buffer
    private static final AttributeKey<ArrayList<Float>> PATHHANDLES_KEY = new AttributeKey<>("PATHTOOL-PATHHANDLES",
            new ArrayList<Float>());
    /**
     * The Apply dialogs' settings (flat floats) and paints (per paint "on", the
     * output's provider type, its save data and the value), stored on the dimension
     * apart from the nodes (a new path keeps them) and outside WorldPainter's undo
     * (undoing an Apply must not drop the settings it was made with).
     */
    private static final AttributeKey<ArrayList<Float>> ROAD_SETTINGS = new AttributeKey<>(
            "PATHTOOL-ROAD-APPLY-SETTINGS", new ArrayList<Float>()),
            RIVER_SETTINGS = new AttributeKey<>("PATHTOOL-RIVER-APPLY-SETTINGS", new ArrayList<Float>()),
            PAINT_SETTINGS = new AttributeKey<>("PATHTOOL-PAINT-APPLY-SETTINGS", new ArrayList<Float>());
    private static final AttributeKey<ArrayList<Object>> ROAD_PAINTS = new AttributeKey<>("PATHTOOL-ROAD-APPLY-PAINTS",
            new ArrayList<Object>()),
            RIVER_PAINTS = new AttributeKey<>("PATHTOOL-RIVER-APPLY-PAINTS", new ArrayList<Object>()),
            PAINT_PAINTS = new AttributeKey<>("PATHTOOL-PAINT-APPLY-PAINTS", new ArrayList<Object>());

    /**
     * Painting with an action output (a terrain, a layer, ...) at {@code value}, as
     * a MappingAction would.
     */
    record OutputPaint(boolean enabled, IPositionValueSetter output, int value) {
        static final OutputPaint RIVERBED = new OutputPaint(true, new TerrainProvider(),
                org.pepsoft.worldpainter.Terrain.GRAVEL.ordinal());
        static final OutputPaint RIVER_TRANSITION = new OutputPaint(true, new TerrainProvider(),
                org.pepsoft.worldpainter.Terrain.SAND.ordinal());
        static final OutputPaint PATH = new OutputPaint(true, new TerrainProvider(),
                org.pepsoft.worldpainter.Terrain.GRAVEL.ordinal());

        List<Object> toData() {
            return List.of(enabled, output.getProviderType().name(), IoParameter.unwrap(output.getSaveData()), value);
        }

        /** The paint stored at {@code offset} of {@code data}, or {@code fallback}. */
        static OutputPaint fromData(List<Object> data, int offset, OutputPaint fallback) {
            try {
                if (data == null || data.size() < offset + 4)
                    return fallback;
                var output = ProviderType.fromType((Object[]) data.get(offset + 2),
                        ProviderType.valueOf((String) data.get(offset + 1)));
                if (!(output instanceof IPositionValueSetter setter))
                    return fallback;
                return new OutputPaint((Boolean) data.get(offset), setter, (Integer) data.get(offset + 3));
            } catch (RuntimeException unreadable) {
                return fallback;
            }
        }
    }

    /**
     * What an Apply dialog remembers. Curve strength is not part of it: it shapes
     * the path itself. {@code profile} indexes the transition profiles.
     */
    record ApplySettings(boolean onlyDown, boolean snapToTerrain, boolean usePaint, boolean setWater,
            boolean changeTerrain, float transition, float slopeLimit, int profile, OutputPaint riverbed,
            OutputPaint riverTransition, OutputPaint paint) {
        private static final float VERSION = 1;
        static final ApplySettings ROAD = new ApplySettings(false, true, true, false, true, 1f, 4f, 2,
                OutputPaint.RIVERBED, OutputPaint.RIVER_TRANSITION, OutputPaint.PATH);
        static final ApplySettings RIVER = new ApplySettings(true, true, false, true, true, 5f, 0f, 3,
                OutputPaint.RIVERBED, OutputPaint.RIVER_TRANSITION, OutputPaint.PATH);
        /** Only the paint: no heights, no terrain, no water, no transition. */
        static final ApplySettings PAINT = new ApplySettings(false, false, false, false, false, 0f, 0f, 3,
                OutputPaint.RIVERBED, OutputPaint.RIVER_TRANSITION, OutputPaint.PATH);

        ArrayList<Float> toFloats() {
            return new ArrayList<>(List.of(VERSION, flag(onlyDown), flag(snapToTerrain), flag(usePaint), flag(setWater),
                    flag(changeTerrain), transition, slopeLimit, (float) profile));
        }

        /** The stored settings, or {@code fallback} if none (or unreadable). */
        static ApplySettings fromFloats(List<Float> data, ApplySettings fallback) {
            if (data == null || data.size() != 9 || data.get(0) != VERSION)
                return fallback;
            return new ApplySettings(data.get(1) != 0, data.get(2) != 0, data.get(3) != 0, data.get(4) != 0,
                    data.get(5) != 0, data.get(6), data.get(7), Math.round(data.get(8)), fallback.riverbed(),
                    fallback.riverTransition(), fallback.paint());
        }

        ArrayList<Object> paintData() {
            var data = new ArrayList<Object>(riverbed.toData());
            data.addAll(riverTransition.toData());
            data.addAll(paint.toData());
            return data;
        }

        ApplySettings withPaintData(List<Object> data) {
            return new ApplySettings(onlyDown, snapToTerrain, usePaint, setWater, changeTerrain, transition, slopeLimit,
                    profile, OutputPaint.fromData(data, 0, riverbed), OutputPaint.fromData(data, 4, riverTransition),
                    OutputPaint.fromData(data, 8, paint));
        }

        private static float flag(boolean value) {
            return value ? 1 : 0;
        }
    }
    private final JPanel optionsPanel = new JPanel();
    /**
     * Generation settings (presets, modes, slope, transition, Apply): hosted by the
     * path settings dialog.
     */
    private final JPanel pathSettingsPanel = new WidthTrackingPanel();

    /**
     * Scrolls only vertically: the dialog sets the width, wide content (the cross
     * section) is cut to it instead of growing a horizontal scroll bar.
     */
    static class WidthTrackingPanel extends JPanel implements Scrollable
    {
        @Override
        public java.awt.Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return orientation == SwingConstants.VERTICAL ? visibleRect.height : visibleRect.width;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
    /**
     * The settings dialog's groups: River / Road, Paint path, Blend with terrain.
     */
    private final JPanel optionsGroup = settingsGroup("River"), paintGroup = settingsGroup("Paint path"),
            blendGroup = settingsGroup("Blend with terrain");

    private static JPanel settingsGroup(String title) {
        return org.ironsight.wpplugin.macromachine.Gui.DialogStyle.group(title);
    }

    private List<JPanel> settingsGroups() {
        return List.of(optionsGroup, paintGroup, blendGroup);
    }
    private JDialog pathSettingsDialog;
    private static final int NODE_DIAMETER = 18;
    private static final int NODE_OUTLINE_DIAMETER = 20;
    /**
     * Selected nodes and handles on the map: the website's light teal (#8BC4D3).
     */
    static final Color SELECTED_NODE_FILL = org.ironsight.wpplugin.macromachine.Gui.OverlayStyle.MAP_SELECTION;
    /**
     * Helper geometry and its handles (radius circles, tangents, height gauge,
     * transform box, operation circles): the primary text colour (white); while
     * hovered.
     */
    static final Color HANDLE_COLOR = org.ironsight.wpplugin.macromachine.Gui.OverlayStyle.TEXT;
    /** Hovered things (nodes, handles) turn primary text colour and grow. */
    static final Color HANDLE_HOVER = org.ironsight.wpplugin.macromachine.Gui.OverlayStyle.TEXT;
    /**
     * Thin dark edge behind text on the map (markings themselves have no outline).
     */
    static final Color SELECTED_NODE_HALO = org.ironsight.wpplugin.macromachine.Gui.OverlayStyle.OUTLINE;
    /**
     * Path group (nodes, segments, arrows) when not selected: light grey (#B0B0B0).
     */
    static final Color UNSELECTED_NODE_FILL = org.ironsight.wpplugin.macromachine.Gui.OverlayStyle.MAP_INACTIVE;
    /** The unselected path in the (dark) profile strip. */
    static final Color STRIP_INACTIVE = org.ironsight.wpplugin.macromachine.Gui.OverlayStyle.MAP_INACTIVE_ON_DARK;
    private static final int NODE_HIT_RADIUS = 18;
    private final JToggleButton showLabel = hudButton("Label", "ShowLabel");
    private final JToggleButton showRadius = hudButton("Radius", "ShowRadius");
    private final JToggleButton showLine = hudButton("Line", "ShowLine");
    private final JToggleButton showNodes = hudButton("Nodes", "ShowNodes");
    private final JToggleButton showLayer = hudButton("Layer", "ShowLayer");
    private final JToggleButton showDirection = hudButton("Direction", "ShowDirection");
    private final JToggleButton showProfile = hudButton("Profile", "ShowProfile");
    /**
     * Master switch for all Path Tool overlays (Blender-style), shown in the
     * viewport.
     */
    private final JToggleButton showOverlays = hudButton("Overlays", "ShowOverlays");
    /**
     * Sticky add mode: plain left clicks on empty map add nodes. Toggled by
     * double-tapping Ctrl or the viewport "Add" button; off on every activation.
     */
    private final JToggleButton addMode = addModeSwitch();
    /**
     * Double-tap Ctrl detection: press/release times of a clean (uninterrupted)
     * tap.
     */
    private boolean ctrlHeld;
    private boolean ctrlTapInterrupted;
    private long ctrlPressedAt;
    private long lastCtrlTapAt;
    static final long CTRL_TAP_MAX_HOLD_MS = 300, CTRL_DOUBLE_TAP_MS = 400;
    HashMap<Point3i, FloatTile> cachedTiles = new HashMap<>();
    boolean measurePreview;
    PreviewTiming previewTiming;
    record PreviewTiming(int samples, int candidateTiles, int existingTiles, long samplingNanos, long collectionNanos,
            long calculationNanos, long publicationNanos) {
    }
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
    /** "Only downhill" and, behind it, whether the nodes already run downhill. */
    private JPanel onlyDownRow;
    private JLabel downhillStatus;
    static final Color STATUS_OK = new Color(46, 160, 67);
    static final Color STATUS_BAD = new Color(207, 34, 46);
    private JCheckBox minCheckbox;
    private JCheckBox fixHeightCheckbox;
    /** Road only: "Set paint" with an action output and its value. */
    private OutputPaintRow roadPaintRow;
    private JSpinner limitSlopeSpinner;
    private JSpinner handleFactorSpinner;
    private JSpinner transitionMultiSpinner;
    private CellPreviewPanel slopePreview;
    /** Settings rows a river does not use (slope limit). */
    private final List<JComponent> roadOnlyRows = new ArrayList<>();
    /** Road only: flatten the ground along the road (else leave it untouched). */
    private JCheckBox flattenRoadCheckbox;
    /**
     * River only: carve a bed below the water (else leave the ground untouched).
     */
    private JCheckBox carveRiverCheckbox;
    /** Set the water level to the path height (else leave water alone). */
    private JCheckBox setWaterCheckbox;
    /** River only: paint the bed / the transition with an action output. */
    private OutputPaintRow riverbedPaintRow, riverTransitionPaintRow;
    /** Apply as paint: what the path is painted with. */
    private OutputPaintRow pathPaintRow;

    /** Which tool the settings dialog was opened for. */
    enum SettingsKind {
        ROAD, RIVER, PAINT
    }

    /**
     * The tool the settings were last opened for: a river ignores the (hidden)
     * slope limit instead of resetting it.
     */
    private SettingsKind settingsKind = SettingsKind.ROAD;
    /**
     * The settings when the dialog opened (or last applied): what Cancel restores.
     */
    private ApplySettings settingsAtOpen;
    /** True while settings are pushed into the controls (no storing meanwhile). */
    private boolean loadingSettings;
    static final Color CROSS_SECTION_WATER = new Color(52, 120, 220), CROSS_SECTION_GRASS = new Color(86, 160, 58),
            CROSS_SECTION_SKY = new Color(214, 220, 226), CROSS_SECTION_SAND = new Color(219, 200, 140),
            CROSS_SECTION_ROAD = new Color(128, 128, 128);
    /** How far (blocks) the terrain beside the river lies above its water. */
    static final int SECTION_BANK = 12;
    static final Color CROSS_SECTION_DIRT = new Color(134, 96, 67);
    /** The river or road and its transition cut open (Apply as river / road). */
    private final CrossSectionView crossSectionView = new CrossSectionView();

    /** The river's water level: the nodes' mean height (62 without nodes). */
    int sectionWaterLevel() {
        var nodes = getCurrentPath();
        return nodes.isEmpty() ? 62 : (int) Math.round(nodes.stream().mapToDouble(n -> n.z).average().orElse(62));
    }

    /**
     * The river and its transition as the apply builds them, one column a block
     * from bank to bank: inside the radius the water at the path height over the
     * carved bed (one block deep at the banks, one deeper for every block towards
     * the middle) of sand; beside it the terrain blends from the water level up to
     * the bank ({@link #SECTION_BANK} blocks higher).
     */
    List<CrossSectionView.Column> riverColumns() {
        float radius = averageRadius();
        int water = sectionWaterLevel();
        int half = (int) Math.ceil(radius * getTransitionMultiplier()) + 2;
        var columns = new ArrayList<CrossSectionView.Column>();
        for (int i = -half; i < half; i++) {
            float distance = Math.abs(i + 0.5f);
            if (distance <= radius) {
                int bed = terrainMode == TerrainMode.CARVE ? water - Math.round(radius - distance + 1) : water;
                columns.add(new CrossSectionView.Column(bed, water, CROSS_SECTION_SAND));
            } else {
                float strength = distance > radius * getTransitionMultiplier()
                        ? 0
                        : filterStrengthFor(distance, radius, getTransitionMultiplier(), brushProfile);
                int ground = water + Math.round((1 - strength) * SECTION_BANK);
                columns.add(new CrossSectionView.Column(ground, CrossSectionView.NO_WATER, CROSS_SECTION_GRASS));
            }
        }
        return columns;
    }

    /**
     * The road and its transition as the apply builds them, cut across a hillside
     * that falls {@link #SECTION_BANK} blocks from left to right: flattened, the
     * road lies level at the path height (in its paint) and the terrain blends from
     * it into the hillside, cut on the high side and filled on the low one; not
     * flattened, the road is only painted onto the hillside.
     */
    List<CrossSectionView.Column> roadColumns() {
        float radius = averageRadius();
        int road = sectionWaterLevel();
        int half = (int) Math.ceil(radius * getTransitionMultiplier()) + 2;
        boolean flatten = terrainMode == TerrainMode.FLATTEN;
        boolean painted = roadPaintRow == null || roadPaintRow.get().enabled();
        var columns = new ArrayList<CrossSectionView.Column>();
        for (int i = -half; i < half; i++) {
            float distance = Math.abs(i + 0.5f);
            float hillside = road - (i + 0.5f) / half * SECTION_BANK / 2f;
            float strength = distance <= radius
                    ? 1
                    : distance > radius * getTransitionMultiplier()
                            ? 0
                            : filterStrengthFor(distance, radius, getTransitionMultiplier(), brushProfile);
            int ground = Math.round(flatten ? strength * road + (1 - strength) * hillside : hillside);
            columns.add(new CrossSectionView.Column(ground, CrossSectionView.NO_WATER,
                    distance <= radius && painted ? CROSS_SECTION_ROAD : CROSS_SECTION_GRASS));
        }
        return columns;
    }

    /**
     * The cross section follows the transition, profile, terrain option, paint and
     * nodes.
     */
    private void refreshCrossSection() {
        if (brushProfile == null)
            return;
        if (settingsKind == SettingsKind.RIVER)
            crossSectionView.setColumns(riverColumns());
        else if (settingsKind == SettingsKind.ROAD)
            crossSectionView.setColumns(roadColumns());
    }

    /** The river's / road's cross section (for tests). */
    CrossSectionView crossSectionView() {
        return crossSectionView;
    }

    /** The nodes' mean radius in blocks (8 without nodes). */
    float averageRadius() {
        var nodes = getCurrentPath();
        return nodes.isEmpty() ? 8f : (float) nodes.stream().mapToDouble(n -> n.w).average().orElse(8);
    }
    private JComboBox<CrossSectionShape> profilesDropdown;
    private CrossSectionShape brushProfile;
    private float transitionMultiplier = 2;
    private JButton applyButton;
    private final JLabel selectionStatus = new JLabel("0 nodes selected");
    private boolean cursorOverMap;
    private boolean controlsReady;
    private boolean previewQueued;
    private long scheduleRevision;
    private long previewRevision;
    private final Supplier<PathPreviewWorker> workerFactory;
    private PathPreviewWorker previewWorker;
    private Dimension previewDimension;
    private Map<PathPreviewMask.TilePosition, BitSet> previewMask = Map.of();
    /**
     * Set when the map's preview layer may no longer match {@link #previewMask}
     * (undo / redo restore whole tiles, preview bits included). The next publish
     * then clears the whole layer first instead of only writing the difference,
     * which would leave restored old preview bits behind as residue.
     */
    private boolean previewLayerUntrusted;
    private org.pepsoft.util.undo.UndoManager observedUndoManager;
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
            // Moving onto a child (e.g. a HUD button) also fires an exit; only leaving the
            // bounds counts.
            if (!new Rectangle(event.getComponent().getSize()).contains(event.getPoint())) {
                cursorOverMap = false;
                setHovered(null);
            }
        }

        @Override
        public void mouseMoved(MouseEvent event) {
            cursorOverMap = true;
            if (gestureStart == null)
                setHovered(hitTest(event.getPoint()));
        }

        @Override
        public void mousePressed(MouseEvent event) {
            cursorOverMap = true;
            reclaimKeyboardFocus();
            interruptCtrlTap();
            if (keyboardTransform && SwingUtilities.isLeftMouseButton(event)) {
                commitKeyboardTransform(); // like Blender: the confirming click selects nothing
                return;
            }
            if (SwingUtilities.isLeftMouseButton(event) && !event.isAltDown() && !event.isMetaDown())
                handleSelectionPress(event.getPoint(), event.isControlDown(), event.isShiftDown());
        }

        @Override
        public void mouseDragged(MouseEvent event) {
            cursorOverMap = true;
            handleSelectionDrag(event.getPoint(), event.isShiftDown());
        }

        @Override
        public void mouseReleased(MouseEvent event) {
            if (SwingUtilities.isLeftMouseButton(event))
                handleSelectionRelease(event.getPoint(), event.isShiftDown());
        }
    };
    /**
     * Which handles selected nodes show; clicking an already-selected node cycles
     * through these (cf. Inkscape).
     */
    enum EditMode {
        RADIUS("Radius", "Radius handles"), CURVE("Curve strength", "Curve strength handles"), HEIGHT("Height",
                "Height handles"), TRANSFORM("Transform", "Transform: move, rotate and scale the selection");

        final String title;
        final String label;

        EditMode(String title, String label) {
            this.title = title;
            this.label = label;
        }

        EditMode next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    private EditMode editMode = EditMode.RADIUS;
    /**
     * Transform editor: pivot for rotating and scaling (world coordinates), set by
     * dragging it; belongs to the selection it was placed for, otherwise the box
     * centre is the pivot.
     */
    private Point2D.Double transformPivot;
    private Set<Integer> transformPivotSelection = Set.of();
    /**
     * Angle of the transform box (degrees, counter-clockwise on screen): rotating
     * turns the box with the nodes; it lines up with the axes again only for a new
     * selection.
     */
    private double transformAngle;
    private Set<Integer> transformAngleSelection = Set.of();
    /** Rotation of the current rotate drag / typed rotation (degrees). */
    private double transformDegrees;
    /** Transform handle being dragged, or {@code null}. */
    private HitKind transformKind;
    /**
     * Pivot (world) when the transform started. The transform itself is kept in
     * world terms (pivot, degrees, factor, offset) and turned into view coordinates
     * only when used, so panning or zooming the map mid-transform keeps box, pivot
     * and nodes together.
     */
    private Point2D.Double transformPivotStart;
    /** Where the handle was grabbed (world, not rounded to blocks). */
    private Point2D.Double transformGrabWorld;
    /** Kind of transform applied so far (rotate, scale, move), or {@code null}. */
    private HitKind transformOp;
    /** Scale so far along the box's x / y axes (uniform: both). */
    private double transformScaleX = 1, transformScaleY = 1;
    /**
     * Scale handle being dragged: uniform, box x or box y
     * (PathTransformBox.SCALE_*).
     */
    private int transformScaleAxis;
    /** Pivot while it is dragged (world). */
    private Point2D.Double draggedPivot;
    /** World offset of a move drag (whole blocks). */
    private int transformMoveX, transformMoveY;
    private int radiusHandleNode = -1;
    private int strengthHandleNode = -1;
    private int heightHandleNode = -1;
    private int randomizeHandleNode = -1;
    private int circleHandleIndex = -1;
    private float randomizeDragStart;
    private int strengthHandleSide;
    /**
     * Curve strength while a tangent handle is dragged; committed to the spinner on
     * release.
     */
    private Float dragStrength;
    /**
     * Blender-style typed value during a handle drag; {@code null} until a number
     * key is typed.
     */
    private NumericInput numericInput;
    /**
     * Typed-value transform started from the keyboard (no mouse drag): typing a
     * number with nodes selected in an editor, like Blender after pressing S.
     */
    private boolean keyboardTransform;
    /**
     * Header text shown while a handle is dragged (operation and value), or
     * {@code null}.
     */
    private String gestureHeader;
    private Point gestureStart;
    private int pressedNode = -1;
    private boolean gestureDragged;
    private boolean gestureAdditive;
    private ArrayList<Point4f> gesturePath;
    private ArrayList<Point4f> movedPath;

    /** The path as dragged so far (exposed for tests). */
    List<Point4f> movedPathForTest() {
        return movedPath;
    }
    private Point gestureWorldStart;
    /**
     * Virtual cursor (view coordinates) driving node/handle drags. It follows the
     * mouse 1:1, or at {@link #PRECISE_DRAG_SCALE} while Shift is held (Blender
     * style), so toggling Shift mid-drag never makes the dragged item jump.
     */
    private Point2D.Double gestureCursor;
    private Point gestureLastRaw;
    private boolean gesturePrecise;
    /** World units per view pixel, captured on press. */
    private double worldPerPixelX = 1, worldPerPixelY = 1;
    static final double PRECISE_DRAG_SCALE = 0.1;
    private final AWTEventListener gestureCapture = event -> {
        if (event instanceof MouseEvent mouse && mouse.getID() == MouseEvent.MOUSE_RELEASED
                && SwingUtilities.isLeftMouseButton(mouse) && gestureStart != null && overlayView != null
                && mouse.getSource() instanceof Component source && source != overlayView) {
            handleSelectionRelease(SwingUtilities.convertPoint(source, mouse.getPoint(), overlayView),
                    mouse.isShiftDown());
        }
    };

    public PathTool() {
        this(PathPreviewWorker::new);
    }

    PathTool(Supplier<PathPreviewWorker> workerFactory) {
        super("Path Tool", "Create smooth roads and rivers", "MacroMachine_RoadTool"); // ONE SHOT OP
        this.workerFactory = workerFactory;
        init();
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        if (!isActive() || getDimension() == null)
            return false;
        Component focus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (operationPanel != null && !operationFields.isEmpty()
                && (focus == null || allowsMapShortcut(overlayView, focus))) {
            if (operationTyping != null) {
                if (e.getID() == KeyEvent.KEY_TYPED)
                    return true;
                if (e.getID() == KeyEvent.KEY_PRESSED)
                    return handleOperationTyping(e);
            } else if (e.getID() == KeyEvent.KEY_PRESSED && cursorOverMap && gestureStart == null && !e.isControlDown()
                    && !e.isAltDown() && !e.isMetaDown() && NumericInput.startsTyping(e.getKeyCode(), e.getKeyChar())) {
                operationTyping = new NumericInput();
                operationTypingRevert = operationFields.get(operationFieldIndex).spinner().getValue();
                return handleOperationTyping(e);
            }
        }
        if (operationPanel != null && e.getID() == KeyEvent.KEY_PRESSED
                && (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_ESCAPE)
                && (focus == null || SwingUtilities.isDescendingFrom(focus, operationPanel)
                        || allowsMapShortcut(overlayView, focus))) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER)
                applyOperation();
            else
                closeOperation();
            return true;
        }
        if (!allowsMapShortcut(overlayView, focus))
            return false;
        trackCtrlTaps(e);
        if (handleDragActive() || keyboardTransform) {
            if (e.getID() == KeyEvent.KEY_TYPED)
                return numericInput != null; // typed characters belong to the numeric input
            if (e.getID() == KeyEvent.KEY_PRESSED && handleNumericKey(e))
                return true;
        }
        if (e.getID() != KeyEvent.KEY_PRESSED)
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
        if (operationPanel == null && !selectedNodes.isEmpty()
                && !(editMode == EditMode.TRANSFORM && selectedNodes.size() < 2) && gestureStart == null
                && !e.isControlDown() && !e.isAltDown() && !e.isMetaDown()
                && NumericInput.startsTyping(e.getKeyCode(), e.getKeyChar())) {
            startKeyboardTransform();
            handleNumericKey(e);
            return true;
        }
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
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE && addMode.isSelected() && gestureStart == null) {
            addMode.setSelected(false);
            return true;
        }
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

    /**
     * Clicking the map moves keyboard focus off a settings spinner/text
     * field/dropdown so that map shortcuts (Delete, Ctrl+A, Escape) work again.
     * Focus is left alone when shortcuts are already allowed.
     */
    private void reclaimKeyboardFocus() {
        if (overlayView == null)
            return;
        Component focus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (!allowsMapShortcut(overlayView, focus))
            overlay.requestFocusInWindow();
    }

    /**
     * Double-tapping Ctrl (two quick, clean taps with the mouse over the map)
     * toggles add mode. Key auto-repeat while holding Ctrl, a long hold, any other
     * key or a mouse press in between do not count.
     */
    private void trackCtrlTaps(KeyEvent e) {
        boolean ctrl = e.getKeyCode() == KeyEvent.VK_CONTROL;
        long now = e.getWhen() != 0 ? e.getWhen() : System.currentTimeMillis();
        if (e.getID() == KeyEvent.KEY_PRESSED) {
            if (!ctrl) {
                interruptCtrlTap();
                return;
            }
            if (ctrlHeld)
                return; // auto-repeat
            ctrlHeld = true;
            ctrlTapInterrupted = false;
            ctrlPressedAt = now;
        } else if (e.getID() == KeyEvent.KEY_RELEASED && ctrl) {
            ctrlHeld = false;
            boolean tap = !ctrlTapInterrupted && now - ctrlPressedAt <= CTRL_TAP_MAX_HOLD_MS;
            if (tap && lastCtrlTapAt != 0 && now - lastCtrlTapAt <= CTRL_DOUBLE_TAP_MS && cursorOverMap
                    && gestureStart == null) {
                lastCtrlTapAt = 0;
                addMode.setSelected(!addMode.isSelected());
            } else
                lastCtrlTapAt = tap ? now : 0;
        }
    }

    private void interruptCtrlTap() {
        ctrlTapInterrupted = true;
        lastCtrlTapAt = 0;
    }

    private boolean handleDragActive() {
        return gestureStart != null && (radiusHandleNode >= 0 || heightHandleNode >= 0 || strengthHandleNode >= 0
                || randomizeHandleNode >= 0 || transformKind == HitKind.TRANSFORM_ROTATE
                || transformKind == HitKind.TRANSFORM_SCALE);
    }

    /**
     * Blender's "type a number" during a transform: number keys start numeric
     * input, Enter confirms (like releasing the mouse), Escape falls through to
     * cancel the drag.
     */
    private boolean handleNumericKey(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_ENTER) {
            if (keyboardTransform)
                commitKeyboardTransform();
            else {
                gestureDragged = true;
                handleSelectionRelease(gestureLastRaw, gesturePrecise);
            }
            return true;
        }
        if (code == KeyEvent.VK_ESCAPE) {
            if (!keyboardTransform)
                return false; // falls through to cancelling the drag
            cancelKeyboardTransform();
            return true;
        }
        if (numericInput == null) {
            if (!NumericInput.starts(code, e.getKeyChar()))
                return false;
            numericInput = new NumericInput();
        }
        if (numericInput.handleKey(code, e.getKeyChar(), e.isControlDown())) {
            if (keyboardTransform)
                updateKeyboardTransform();
            else {
                gestureDragged = true; // typing alone (without moving) is a transform too
                handleSelectionDrag(gestureLastRaw, gesturePrecise);
            }
        }
        return true; // while typing, other keys do nothing
    }

    /**
     * Starts typing a value for the current editor on the selected nodes. The typed
     * number is absolute: it becomes the radius, the height above ground (blocks)
     * or the curve strength of all selected nodes.
     */
    private void startKeyboardTransform() {
        cancelGesture();
        keyboardTransform = true;
        gesturePath = getCurrentPath();
        numericInput = new NumericInput();
    }

    /**
     * Live preview of the typed value; the header shows the current value until
     * typing starts.
     */
    private void updateKeyboardTransform() {
        int reference = selectedNodes.stream().min(Integer::compare).orElse(0);
        boolean edited = numericInput.edited();
        switch (editMode) {
            case RADIUS -> {
                float current = gesturePath.get(reference).w;
                movedPath = edited ? radiusSetPath((float) numericInput.apply(current)) : null;
                gestureHeader = header("Radius", formatValue(current));
            }
            case HEIGHT -> {
                Float ground = groundAt(gesturePath.get(reference));
                float current = ground == null ? 0 : Math.round((gesturePath.get(reference).z - ground) * 10) / 10f;
                movedPath = edited ? heightSetPath((float) numericInput.apply(current)) : null;
                gestureHeader = header("Height", PathEditHandles.signed(current));
            }
            case CURVE -> {
                dragStrength = edited ? clampStrength(numericInput.apply(handleStrength)) : null;
                gestureHeader = header("Curve strength", String.format(Locale.ROOT, "%.2f", handleStrength));
            }
            case TRANSFORM -> {
                // typing without a drag rotates around the pivot (degrees, counter-clockwise)
                transformPivotStart = transformPivotWorld();
                transformDegrees = edited ? numericInput.apply(0) : 0;
                transformOp = edited ? HitKind.TRANSFORM_ROTATE : null;
                movedPath = transformOp == null ? null : transformedPath(transformViewNow());
                gestureHeader = header("Rotate", "0\u00b0");
            }
        }
        overlay.repaint();
    }

    /** Enter or a left click on the map applies the typed value. */
    private void commitKeyboardTransform() {
        var base = gesturePath;
        var path = movedPath;
        Float strength = dragStrength;
        double degrees = transformDegrees;
        cancelGesture();
        if (path != null && !path.equals(base)) {
            if (editMode == EditMode.TRANSFORM)
                turnTransformBox(degrees);
            completeEdit(path);
        }
        if (strength != null)
            handleFactorSpinner.setValue(Math.round(strength * 100) / 100.0);
        overlay.repaint();
    }

    /** Escape or a right click cancels; the selection stays (like Blender). */
    private void cancelKeyboardTransform() {
        cancelGesture();
        overlay.repaint();
    }

    boolean typingValue() {
        return keyboardTransform;
    }

    // ------------------------------------------------------------- Node / Segment
    // menu operations

    /** Preview of the Node / Segment operation being adjusted, or {@code null}. */
    private PathOperations.Result operationPreview;
    /**
     * Neighbouring nodes of the preview on the same block: merged on apply
     * (warning).
     */
    private List<int[]> operationMerges = List.of();
    private JLabel operationMergeNote;
    /**
     * Path when the operation was opened (old node positions for the movement
     * lines).
     */
    private List<Point4f> operationBase;
    private JPanel operationPanel;
    private Runnable operationRefresh;

    /** "Node" menu: operations on the selected nodes (exposed for tests). */
    JPopupMenu nodeMenu() {
        var menu = new JPopupMenu();
        boolean any = !selectedNodes.isEmpty();
        menu.add(operationItem("Randomize position...", "PathTool.Node.Randomize", any, "Select nodes first",
                this::openRandomize));
        menu.add(operationItem("Randomize radius...", "PathTool.Node.RandomizeRadius", any, "Select nodes first",
                this::openRandomizeRadius));
        menu.add(operationItem("Snap to terrain...", "PathTool.Node.Snap", any, "Select nodes first",
                this::openSnapToTerrain));
        menu.add(operationItem("Only downhill...", "PathTool.Node.OnlyDownhill", any, "Select nodes first",
                this::openOnlyDownhill));
        return menu;
    }

    /**
     * "Segment" menu: operations on segments between two selected nodes (exposed
     * for tests).
     */
    JPopupMenu segmentMenu() {
        var menu = new JPopupMenu();
        boolean any = !PathOperations.selectedSegments(getCurrentPath().size(), selectedNodes).isEmpty();
        menu.add(operationItem("Subdivide...", "PathTool.Segment.Subdivide", any, "Select two neighbouring nodes first",
                this::openSubdivide));
        return menu;
    }

    private static JMenuItem operationItem(String text, String name, boolean enabled, String disabledHint,
            Runnable action) {
        var item = new JMenuItem(text);
        item.setName(name);
        item.setEnabled(enabled);
        if (!enabled)
            item.setToolTipText(disabledHint);
        item.addActionListener(event -> action.run());
        return item;
    }

    private static JSpinner operationSpinner(String name, SpinnerNumberModel model) {
        var spinner = org.ironsight.wpplugin.macromachine.Gui.OverlayStyle.spinner(new JSpinner(model));
        spinner.setName(name);
        // live preview while typing, not only on Enter / focus loss
        if (spinner.getEditor() instanceof JSpinner.DefaultEditor editor
                && editor.getTextField().getFormatter() instanceof javax.swing.text.DefaultFormatter formatter)
            formatter.setCommitsOnValidEdit(true);
        return spinner;
    }

    private static JPanel operationForm(Object... labelsAndControls) {
        var form = new JPanel(new GridLayout(0, 2, 6, 4));
        form.setOpaque(false);
        for (int i = 0; i < labelsAndControls.length; i += 2) {
            form.add(org.ironsight.wpplugin.macromachine.Gui.OverlayStyle
                    .label(new JLabel((String) labelsAndControls[i]), true));
            form.add((Component) labelsAndControls[i + 1]);
        }
        return form;
    }

    private void refreshOperation() {
        if (operationRefresh != null)
            operationRefresh.run();
    }

    void openSubdivide() {
        var base = getCurrentPath();
        var selection = Set.copyOf(selectedNodes);
        float strength = handleStrength;
        var cuts = operationSpinner("PathTool.Subdivide.Cuts", new SpinnerNumberModel(1, 1, 32, 1));
        openOperation("Subdivide", operationForm("Number of cuts", cuts),
                () -> PathOperations.subdivide(base, selection, ((Number) cuts.getValue()).intValue(), strength));
        cuts.addChangeListener(event -> refreshOperation());
        typeableFields(new TypeableField("Number of cuts", cuts));
    }

    /**
     * Draggable radius circle of an open Node operation (Randomize position's
     * range, Randomize radius' min and max): a value of its own (blocks),
     * independent of the nodes' radius, living only while the operation is open.
     * Drawn around every selected node, with its handle on the right ({@code side}
     * 1) or left (-1).
     */
    record OperationCircle(JSpinner spinner, int side, String label) {
        float value() {
            return ((Number) spinner.getValue()).floatValue();
        }
    }

    private List<OperationCircle> operationCircles = List.of();
    private List<Point4f> circleBase;
    private Set<Integer> circleSelection;

    private void showOperationCircles(List<Point4f> base, Set<Integer> selection, OperationCircle... circles) {
        circleBase = base;
        circleSelection = selection;
        operationCircles = List.of(circles);
        for (OperationCircle circle : circles)
            circle.spinner().addChangeListener(event -> overlay.repaint());
        overlay.repaint();
    }

    void openRandomize() {
        var base = getCurrentPath();
        var selection = Set.copyOf(selectedNodes);
        var radius = operationSpinner("PathTool.Randomize.Radius", new SpinnerNumberModel(4.0, 0.0, 1000.0, 0.5));
        var seed = operationSpinner("PathTool.Randomize.Seed", new SpinnerNumberModel(1, 0, 99999, 1));
        openOperation("Randomize position", operationForm("Radius (blocks)", radius, "Seed", seed),
                () -> keepHeightAboveGround(base, PathOperations.randomize(base, selection,
                        ((Number) radius.getValue()).doubleValue(), ((Number) seed.getValue()).longValue())));
        showOperationCircles(base, selection, new OperationCircle(radius, 1, "random"));
        typeableFields(new TypeableField("Radius (blocks)", radius));
        radius.addChangeListener(event -> refreshOperation());
        seed.addChangeListener(event -> refreshOperation());
    }

    /**
     * Gives every selected node a random radius between min and max (blocks), e.g.
     * so a river widens and narrows. Min and max are draggable circles (min handle
     * left, max handle right) and cannot cross.
     */
    void openRandomizeRadius() {
        var base = getCurrentPath();
        var selection = Set.copyOf(selectedNodes);
        float current = (float) selection.stream().mapToDouble(i -> base.get(i).w).average().orElse(4);
        double low = Math.max(1, Math.round(current * 0.5 * 2) / 2.0),
                high = Math.max(low, Math.round(current * 1.5 * 2) / 2.0);
        var min = operationSpinner("PathTool.RandomizeRadius.Min", new SpinnerNumberModel(low, 1.0, 1000.0, 0.5));
        var max = operationSpinner("PathTool.RandomizeRadius.Max", new SpinnerNumberModel(high, 1.0, 1000.0, 0.5));
        var seed = operationSpinner("PathTool.RandomizeRadius.Seed", new SpinnerNumberModel(1, 0, 99999, 1));
        openOperation("Randomize radius",
                operationForm("Min radius (blocks)", min, "Max radius (blocks)", max, "Seed", seed),
                () -> PathOperations.randomizeRadius(base, selection, ((Number) min.getValue()).doubleValue(),
                        ((Number) max.getValue()).doubleValue(), ((Number) seed.getValue()).longValue()));
        // min and max cannot cross: the value being changed stops at the other one
        min.addChangeListener(event -> {
            if (((Number) min.getValue()).doubleValue() > ((Number) max.getValue()).doubleValue())
                min.setValue(max.getValue());
            refreshOperation();
        });
        max.addChangeListener(event -> {
            if (((Number) max.getValue()).doubleValue() < ((Number) min.getValue()).doubleValue())
                max.setValue(min.getValue());
            refreshOperation();
        });
        seed.addChangeListener(event -> refreshOperation());
        showOperationCircles(base, selection, new OperationCircle(min, -1, "min"), new OperationCircle(max, 1, "max"));
        typeableFields(new TypeableField("Min radius (blocks)", min), new TypeableField("Max radius (blocks)", max));
    }

    /** Screen radius of a circle of {@code radius} blocks around a node. */
    private double radiusInView(Point4f node, float radius) {
        Point center = overlayView.worldToView(Math.round(node.x), Math.round(node.y));
        Point edge = overlayView.worldToView(Math.round(node.x + radius), Math.round(node.y));
        return center.distance(edge);
    }

    /**
     * Handle of a circle: on its right or left edge, never closer than the minimum
     * offset.
     */
    private static Point2D.Double circleHandle(Point2D center, double radiusPx, int side) {
        var right = PathEditHandles.radiusHandle(center, radiusPx);
        return side > 0 ? right : new Point2D.Double(2 * center.getX() - right.x, right.y);
    }

    /**
     * The operation circle handle under {@code point}: node index and circle index
     * (as side).
     */
    private Hit operationCircleHandleAt(Point point) {
        if (operationCircles.isEmpty())
            return null;
        var view = viewNodes(circleBase);
        double best = PathEditHandles.HANDLE_HIT_RADIUS * PathEditHandles.HANDLE_HIT_RADIUS;
        Hit hit = null;
        for (int c = 0; c < operationCircles.size(); c++) {
            OperationCircle circle = operationCircles.get(c);
            for (int i : circleSelection) {
                if (i >= circleBase.size())
                    continue;
                double distance = circleHandle(view.get(i), radiusInView(circleBase.get(i), circle.value()),
                        circle.side()).distanceSq(point);
                if (distance <= best) {
                    best = distance;
                    hit = new Hit(HitKind.OPERATION_RADIUS, i, c);
                }
            }
        }
        return hit;
    }

    void openSnapToTerrain() {
        var base = getCurrentPath();
        var selection = Set.copyOf(selectedNodes);
        var radius = operationSpinner("PathTool.Snap.Radius", new SpinnerNumberModel(8, 1, 64, 1));
        var target = new JComboBox<>(PathOperations.SnapTarget.values());
        target.setName("PathTool.Snap.Target");
        openOperation("Snap to terrain", operationForm("Search radius (blocks)", radius, "Snap to", target),
                () -> keepHeightAboveGround(base,
                        PathOperations.snapToTerrain(base, selection, ((Number) radius.getValue()).intValue(),
                                (PathOperations.SnapTarget) target.getSelectedItem(), this::groundAt)));
        radius.addChangeListener(event -> refreshOperation());
        target.addActionListener(event -> refreshOperation());
        showOperationCircles(base, selection, new OperationCircle(radius, 1, "search"));
        typeableFields(new TypeableField("Search radius (blocks)", radius));
    }

    /**
     * Moved nodes keep their height above ground (like dragged nodes); a node moved
     * off the map stays where it was.
     */
    private PathOperations.Result keepHeightAboveGround(List<Point4f> base, PathOperations.Result result) {
        var path = new ArrayList<Point4f>(result.path().size());
        for (int i = 0; i < result.path().size(); i++) {
            var node = new Point4f(result.path().get(i));
            var original = base.get(i);
            if (node.x != original.x || node.y != original.y) {
                Float oldGround = groundAt(original), newGround = groundAt(node);
                if (newGround == null)
                    node = new Point4f(original);
                else
                    node.z = clampHeight(newGround + (oldGround == null ? 0 : original.z - oldGround));
            }
            path.add(node);
        }
        return new PathOperations.Result(path, result.selection());
    }

    /**
     * Opens the operation's settings panel (bottom left of the map, like Blender's
     * "Adjust Last Operation") with a live preview; Apply / Enter commits, Cancel /
     * Escape or clicking the map discards.
     */
    /**
     * Lowers the selected nodes so the path never rises along its direction (node 1
     * to n): each selected node ends up no higher than the node before it.
     */
    void openOnlyDownhill() {
        var base = getCurrentPath();
        var selection = Set.copyOf(selectedNodes);
        var note = new JLabel(
                "<html>Lowers selected nodes so the path never rises along its direction<br>(node 1 to n) and never floats above the terrain.</html>");
        openOperation("Only downhill", note, () -> PathOperations.onlyDownhill(base, selection, this::groundAt));
    }

    private String operationTitle;

    /**
     * Labels and notes inside an operation panel: light text on the dark surface.
     */
    private static void styleOperationControls(java.awt.Container container) {
        // the controls may be a bare text (Only downhill's note), not a panel of them
        if (container instanceof JLabel label) {
            styleOperationLabel(label);
            return;
        }
        if (container instanceof JComponent component && !(container instanceof JSpinner))
            component.setOpaque(false);
        for (Component child : container.getComponents()) {
            if (child instanceof JLabel label)
                styleOperationLabel(label);
            else if (child instanceof AbstractButton button)
                org.ironsight.wpplugin.macromachine.Gui.OverlayStyle.button(button);
            else if (child instanceof java.awt.Container inner && !(child instanceof JSpinner))
                styleOperationControls(inner);
        }
    }

    /**
     * Texts on the operation panel: muted light text (unless coloured on purpose).
     */
    private static void styleOperationLabel(JLabel label) {
        label.setOpaque(false);
        if (label.getForeground() == null || label.getForeground() instanceof javax.swing.plaf.UIResource)
            org.ironsight.wpplugin.macromachine.Gui.OverlayStyle.label(label, true);
    }

    private void openOperation(String title, JComponent controls, Supplier<PathOperations.Result> compute) {
        closeOperation();
        cancelGesture();
        operationTitle = title;
        JPanel panel = new org.ironsight.wpplugin.macromachine.Gui.OverlayStyle.SurfacePanel(new BorderLayout(8, 8));
        panel.setName("PathTool.OperationPanel");
        var heading = new JLabel(title);
        heading.setForeground(org.ironsight.wpplugin.macromachine.Gui.OverlayStyle.TEXT);
        heading.setFont(org.ironsight.wpplugin.macromachine.Gui.OverlayStyle.font(Font.BOLD, 15f));
        panel.add(heading, BorderLayout.NORTH);
        styleOperationControls(controls);
        panel.add(controls, BorderLayout.CENTER);
        operationMergeNote = new JLabel();
        operationMergeNote.setName("PathTool.OperationMergeNote");
        operationMergeNote.setForeground(WARNING_COLOR);
        operationMergeNote.setFont(org.ironsight.wpplugin.macromachine.Gui.OverlayStyle.font(Font.PLAIN, 13.5f));
        operationMergeNote.setVisible(false);
        var buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        buttons.setOpaque(false);
        var cancel = org.ironsight.wpplugin.macromachine.Gui.OverlayStyle.button(new JButton("Cancel"));
        cancel.setName("PathTool.OperationCancel");
        cancel.addActionListener(event -> closeOperation());
        var apply = org.ironsight.wpplugin.macromachine.Gui.OverlayStyle.primaryButton(new JButton("Apply"));
        apply.setName("PathTool.OperationApply");
        apply.addActionListener(event -> applyOperation());
        buttons.add(cancel);
        buttons.add(apply);
        var south = new JPanel(new BorderLayout(0, 4));
        south.setOpaque(false);
        south.add(operationMergeNote, BorderLayout.NORTH);
        south.add(buttons, BorderLayout.SOUTH);
        panel.add(south, BorderLayout.SOUTH);
        operationPanel = panel;
        operationBase = getCurrentPath();
        operationRefresh = () -> {
            operationPreview = compute.get();
            // only clumps this operation creates (not neighbours that already shared a
            // block)
            operationMerges = PathOperations.createdCoincidentRuns(operationBase, operationPreview);
            updateMergeNote();
            overlay.repaint();
        };
        overlay.showOperationPanel(panel);
        operationRefresh.run();
    }

    /**
     * Tells which nodes will be merged (1-based, e.g. "3+4"), or hides the note.
     */
    private void updateMergeNote() {
        if (operationMergeNote == null)
            return;
        var groups = new ArrayList<String>();
        for (int[] run : operationMerges) {
            var numbers = new ArrayList<String>();
            for (int index : run)
                numbers.add(Integer.toString(index + 1));
            groups.add(String.join("+", numbers));
        }
        boolean show = !groups.isEmpty();
        String text = show ? "Merges nodes on the same block: " + String.join(", ", groups) : "";
        if (show == operationMergeNote.isVisible() && text.equals(operationMergeNote.getText()))
            return;
        operationMergeNote.setText(text);
        operationMergeNote.setVisible(show);
        overlay.relayoutOperationPanel();
    }

    String operationMergeNote() {
        return operationMergeNote != null && operationMergeNote.isVisible() ? operationMergeNote.getText() : null;
    }

    void applyOperation() {
        commitSpinnerEdits(operationPanel);
        refreshOperation();
        var result = operationPreview == null ? null : PathOperations.mergeRuns(operationPreview, operationMerges);
        closeOperation();
        if (result == null)
            return;
        if (!result.path().equals(getCurrentPath()))
            completeEdit(result.path());
        selectedNodes.clear();
        selectedNodes.addAll(result.selection());
        selectionChanged();
    }

    void closeOperation() {
        if (operationPanel != null)
            overlay.hideOperationPanel(operationPanel);
        operationPanel = null;
        operationPreview = null;
        operationMerges = List.of();
        operationMergeNote = null;
        operationRefresh = null;
        operationBase = null;
        operationCircles = List.of(); // operation circles only live as long as their operation
        operationFields = List.of();
        operationTyping = null;
        circleBase = null;
        circleSelection = null;
        overlay.repaint();
    }

    private static void commitSpinnerEdits(Component component) {
        if (component instanceof JSpinner spinner) {
            try {
                spinner.commitEdit();
            } catch (java.text.ParseException ignored) {
                // keep the last valid value
            }
        } else if (component instanceof Container container)
            for (Component child : container.getComponents())
                commitSpinnerEdits(child);
    }

    private static String headerLabel(OperationCircle circle) {
        return switch (circle.label()) {
            case "min" -> "Min radius (blocks)";
            case "max" -> "Max radius (blocks)";
            case "search" -> "Search radius (blocks)";
            default -> "Randomize radius (blocks)";
        };
    }

    /** A number of the open operation that can be typed into the viewport. */
    record TypeableField(String label, JSpinner spinner) {
    }

    /**
     * Fields typing in the viewport goes to; the first is the most important, Tab
     * cycles.
     */
    private List<TypeableField> operationFields = List.of();
    private int operationFieldIndex;
    private NumericInput operationTyping;
    private Object operationTypingRevert;

    private void typeableFields(TypeableField... fields) {
        operationFields = List.of(fields);
        operationFieldIndex = 0;
        operationTyping = null;
    }

    /**
     * Sets a spinner within its model's bounds and number type (Integer models get
     * whole numbers).
     */
    private static void setSpinnerNumber(JSpinner spinner, double value) {
        var model = (SpinnerNumberModel) spinner.getModel();
        double clamped = Math.max(((Number) model.getMinimum()).doubleValue(),
                Math.min(((Number) model.getMaximum()).doubleValue(), value));
        spinner.setValue(model.getValue() instanceof Integer ? (Object) (int) Math.round(clamped) : (Object) clamped);
    }

    /**
     * Typing a number into the viewport while an operation is open (Blender style):
     * digits edit the current field live, Tab moves to the next field, Enter
     * applies the operation, Escape restores the field's previous value.
     */
    private boolean handleOperationTyping(KeyEvent e) {
        int code = e.getKeyCode();
        var field = operationFields.get(operationFieldIndex);
        if (code == KeyEvent.VK_ENTER) {
            operationTyping = null;
            applyOperation();
            return true;
        }
        if (code == KeyEvent.VK_ESCAPE) {
            field.spinner().setValue(operationTypingRevert);
            operationTyping = null;
            overlay.repaint();
            return true;
        }
        if (code == KeyEvent.VK_TAB) {
            operationFieldIndex = (operationFieldIndex + 1) % operationFields.size();
            operationTyping = new NumericInput();
            operationTypingRevert = operationFields.get(operationFieldIndex).spinner().getValue();
            overlay.repaint();
            return true;
        }
        if (operationTyping.handleKey(code, e.getKeyChar(), e.isControlDown())) {
            double before = ((Number) operationTypingRevert).doubleValue();
            if (operationTyping.edited())
                setSpinnerNumber(field.spinner(), operationTyping.apply(before));
            else
                field.spinner().setValue(operationTypingRevert);
            overlay.repaint();
        }
        return true; // while typing, other keys do nothing
    }

    /**
     * Header shown at the top of the viewport, always: the live text while dragging
     * or typing, otherwise which value is being edited and its current value.
     */
    String viewportHeader() {
        if (gestureHeader != null)
            return gestureHeader;
        String typing = operationTypingHeader();
        return typing != null ? typing : idleHeader();
    }

    /**
     * Which value the current editor / open operation edits, with its current
     * value.
     */
    private String idleHeader() {
        if (operationPanel != null) {
            if (operationFields.isEmpty()) {
                // no number to type: show what the operation will change
                if (operationPreview == null || operationBase == null)
                    return operationTitle;
                int changed = 0;
                for (int i = 0; i < Math.min(operationBase.size(), operationPreview.path().size()); i++)
                    if (!operationBase.get(i).equals(operationPreview.path().get(i)))
                        changed++;
                return operationTitle + ": " + changed + (changed == 1 ? " node changes" : " nodes change");
            }
            var parts = new ArrayList<String>();
            for (TypeableField field : operationFields)
                parts.add(field.label() + ": " + formatValue(((Number) field.spinner().getValue()).floatValue()));
            return String.join("   ", parts);
        }
        if (editMode == EditMode.CURVE)
            return editMode.title + ": " + String.format(Locale.ROOT, "%.2f", handleStrength);
        if (editMode == EditMode.TRANSFORM)
            return selectedNodes.isEmpty()
                    ? editMode.title + ": (select nodes)"
                    : editMode.title + ": " + selectedNodes.size() + (selectedNodes.size() == 1 ? " node" : " nodes");
        var path = getCurrentPath();
        var values = new ArrayList<Float>();
        for (int i : selectedNodes) {
            if (i >= path.size())
                continue;
            if (editMode == EditMode.RADIUS)
                values.add(path.get(i).w);
            else {
                Float ground = groundAt(path.get(i));
                if (ground != null)
                    values.add(Math.round((path.get(i).z - ground) * 10) / 10f);
            }
        }
        if (values.isEmpty())
            return editMode.title + ": (select nodes)";
        float low = values.stream().min(Float::compare).orElseThrow();
        float high = values.stream().max(Float::compare).orElseThrow();
        java.util.function.Function<Float, String> format = editMode == EditMode.HEIGHT
                ? PathEditHandles::signed
                : PathTool::formatValue;
        String value = Math.abs(high - low) < 0.05f
                ? format.apply(low)
                : format.apply(low) + " - " + format.apply(high);
        return editMode.title + ": " + value;
    }

    /** Header while typing into an operation field, or {@code null}. */
    String operationTypingHeader() {
        if (operationTyping == null || operationFields.isEmpty())
            return null;
        var field = operationFields.get(operationFieldIndex);
        String text = field.label() + ": "
                + operationTyping.display(formatValue(((Number) operationTypingRevert).floatValue()));
        if (operationFields.size() > 1)
            text += "   (Tab: " + operationFields.get((operationFieldIndex + 1) % operationFields.size()).label() + ")";
        return text;
    }

    /** Value spinner of the open operation's first circle (exposed for tests). */
    JSpinner randomizeRadiusSpinner() {
        return operationCircles.isEmpty() ? null : operationCircles.get(0).spinner();
    }

    List<OperationCircle> operationCircles() {
        return operationCircles;
    }

    PathOperations.Result operationPreview() {
        return operationPreview;
    }

    /**
     * Selection to draw: the previewed operation's (indices of the preview path) or
     * the real one.
     */
    private Set<Integer> shownSelection() {
        return operationPreview != null ? operationPreview.selection() : selectedNodes;
    }

    /** Header text of the running handle drag (exposed for tests). */
    String gestureHeader() {
        return gestureHeader;
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
        // overlay switches are not persisted: every activation starts with all on. Done
        // before the preview worker exists, so the switches' listeners trigger no
        // preview
        // work; the schedulePreview() below covers it.
        showOverlays.setSelected(true);
        addMode.setSelected(false);
        for (JToggleButton overlaySwitch : overlayParts())
            overlaySwitch.setSelected(true);
        previewWorker = workerFactory.get();
        attachOverlay();
        observeUndo(getDimension());
        if (getDimension() != null)
            getDimension().clearLayerData(PreviewOperation.annotationLayer);
        schedulePreview();
    }

    @Override
    protected void deactivate() {
        if (pathSettingsDialog != null)
            pathSettingsDialog.setVisible(false);
        observeUndo(null);
        invalidatePreview();
        clearPreview();
        if (previewWorker != null) {
            previewWorker.close();
            previewWorker = null;
        }
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
        overlay.setFocusable(true);
        view.add(overlay);
        view.setComponentZOrder(overlay, 0);
        resizeOverlay();
        overlay.showOverlayText("Path Tool");
    }

    private void resizeOverlay() {
        if (overlayView != null)
            overlay.setBounds(0, 0, overlayView.getWidth(), overlayView.getHeight());
    }

    private void detachOverlay() {
        closeOperation();
        cancelGesture();
        setHovered(null); // restores the map's own cursor
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
            invalidatePreview();
            clearPreview();
            previewLayerUntrusted = false;
            observeUndo(isActive() ? overlayView.getDimension() : null);
            overlayDimension = overlayView.getDimension();
            selectedNodes.clear();
            cancelGesture();
            cachedTiles.clear();
            selectionStatus.setText("0 nodes selected");
            schedulePreview();
        }
    }

    void handleSelectionPress(Point point) {
        handleSelectionPress(point, false);
    }

    void handleSelectionPress(Point point, boolean additive) {
        handleSelectionPress(point, additive, false);
    }

    /**
     * @param shift
     *            Shift held on press: only starts a (precise) gesture on a node or
     *            handle; Shift+press on empty map is left to WorldPainter.
     */
    void handleSelectionPress(Point point, boolean additive, boolean shift) {
        syncDimension();
        if (overlayView == null || overlayDimension == null)
            return;
        Hit hit = hitTest(point);
        if (hit == null || hit.kind() != HitKind.OPERATION_RADIUS) {
            closeOperation(); // clicking the map discards a previewed operation
            hit = hitTest(point);
        }
        cancelGesture();
        gestureStart = new Point(point);
        pressedNode = hit != null && hit.kind() == HitKind.NODE ? hit.node() : -1;
        gestureDragged = false;
        gestureAdditive = additive;
        gesturePath = getCurrentPath();
        gestureWorldStart = overlayView.viewToWorld(point);
        Point far = overlayView.viewToWorld(new Point(point.x + 1024, point.y + 1024));
        worldPerPixelX = (far.x - gestureWorldStart.x) / 1024.0;
        worldPerPixelY = (far.y - gestureWorldStart.y) / 1024.0;
        gestureCursor = new Point2D.Double(point.x, point.y);
        gestureLastRaw = new Point(point);
        if (hit != null) {
            switch (hit.kind()) {
                case RADIUS -> radiusHandleNode = hit.node();
                case STRENGTH -> {
                    strengthHandleNode = hit.node();
                    strengthHandleSide = hit.side();
                }
                case HEIGHT -> heightHandleNode = hit.node();
                case TRANSFORM_MOVE, TRANSFORM_ROTATE, TRANSFORM_SCALE, TRANSFORM_PIVOT -> {
                    transformKind = hit.kind();
                    transformScaleAxis = hit.side();
                    transformPivotStart = transformPivotWorld();
                    transformGrabWorld = toWorldExact(point);
                }
                case OPERATION_RADIUS -> {
                    randomizeHandleNode = hit.node();
                    circleHandleIndex = hit.side();
                    randomizeDragStart = operationCircles.get(hit.side()).value();
                }
                case NODE -> {
                }
            }
        }
        if (shift && hit == null) {
            cancelGesture();
            return;
        }
        setHovered(hit); // the grabbed element stays highlighted while dragging
        Toolkit.getDefaultToolkit().addAWTEventListener(gestureCapture, AWTEvent.MOUSE_EVENT_MASK);
        if (hit == null)
            overlay.startDrag(point);
    }

    /** World position of the virtual cursor, with sub-block precision. */
    private Point2D.Double gestureWorld() {
        return new Point2D.Double(gestureWorldStart.x + (gestureCursor.x - gestureStart.x) * worldPerPixelX,
                gestureWorldStart.y + (gestureCursor.y - gestureStart.y) * worldPerPixelY);
    }

    /** Something on the map that reacts to the mouse. */
    enum HitKind {
        NODE(Cursor.MOVE_CURSOR), RADIUS(Cursor.HAND_CURSOR), STRENGTH(Cursor.HAND_CURSOR), HEIGHT(
                Cursor.N_RESIZE_CURSOR), OPERATION_RADIUS(Cursor.HAND_CURSOR), TRANSFORM_MOVE(
                        Cursor.MOVE_CURSOR), TRANSFORM_ROTATE(Cursor.HAND_CURSOR), TRANSFORM_SCALE(
                                Cursor.HAND_CURSOR), TRANSFORM_PIVOT(Cursor.MOVE_CURSOR);

        final int cursor;

        HitKind(int cursor) {
            this.cursor = cursor;
        }
    }

    /**
     * @param side
     *            for tangent handles: -1 backward, +1 forward; otherwise 0
     */
    record Hit(HitKind kind, int node, int side) {
    }

    /** Element under the mouse (or being dragged), highlighted as interactive. */
    private Hit hovered;
    private Cursor cursorBeforeHover;

    Hit hovered() {
        return hovered;
    }

    private void setHovered(Hit hit) {
        if (Objects.equals(hit, hovered))
            return;
        hovered = hit;
        updateCursor();
        overlay.repaint();
    }

    /**
     * Cursor for a hovered element; nodes do not drag in the Transform editor (only
     * its move handle does), so there they only show they can be clicked.
     */
    int hoverCursor(Hit hit) {
        return hit.kind() == HitKind.NODE && !nodeDrags(hit.node(), false) ? Cursor.HAND_CURSOR : hit.kind().cursor;
    }

    /**
     * Whether dragging {@code node} moves it. Always outside the Transform editor;
     * there only when it is (or becomes, by a plain press) the single selected
     * node: a group moves with the move handle, which carries the pivot along.
     */
    boolean nodeDrags(int node, boolean additive) {
        if (editMode != EditMode.TRANSFORM)
            return true;
        boolean selected = selectedNodes.contains(node);
        return selected ? selectedNodes.size() == 1 : !additive;
    }

    private boolean cursorOverridden;

    /**
     * Cursor for the hovered element, a crosshair on empty map in add mode, else
     * the map's own.
     */
    private void updateCursor() {
        if (overlayView == null)
            return;
        Cursor wanted = hovered != null
                ? Cursor.getPredefinedCursor(hoverCursor(hovered))
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

    /**
     * What a press at {@code point} would grab: a handle of a selected node or a
     * node. A handle wins over a node only when the point is closer to the handle
     * than to that node's centre, so handles stay grabbable inside a node's
     * generous hit area without blocking clicks on a neighbouring node they happen
     * to overlap. Shared by pressing and hovering, so hover feedback always matches
     * the press.
     */
    Hit hitTest(Point point) {
        syncDimension();
        if (overlayView == null || overlayDimension == null)
            return null;
        Hit circle = operationCircleHandleAt(point);
        if (circle != null)
            return circle;
        var path = getCurrentPath();
        int node = nearestNode(point);
        var view = viewNodes(path);
        double best = PathEditHandles.HANDLE_HIT_RADIUS * PathEditHandles.HANDLE_HIT_RADIUS;
        if (node >= 0)
            best = Math.min(best, view.get(node).distanceSq(point) - 1e-9);
        Hit hit = null;
        if (editMode == EditMode.TRANSFORM) {
            if (showOverlays.isSelected() && operationPreview == null) {
                double angle = transformBoxAngle();
                var box = PathTransformBox.box(view, selectedNodes, angle);
                var frame = PathTransformBox.frame(angle);
                if (box != null && selectedNodes.size() < 2) {
                    double distance = frame.transform(PathTransformBox.singleMoveHandle(box), null).distanceSq(point);
                    if (distance <= best)
                        hit = new Hit(HitKind.TRANSFORM_MOVE, -1, 0);
                } else if (box != null) {
                    var candidates = new ArrayList<Hit>();
                    var points = new ArrayList<Point2D>();
                    for (int axis : new int[]{PathTransformBox.SCALE_UNIFORM, PathTransformBox.SCALE_X,
                            PathTransformBox.SCALE_Y}) {
                        candidates.add(new Hit(HitKind.TRANSFORM_SCALE, -1, axis));
                        points.add(frame.transform(PathTransformBox.scaleHandle(box, axis), null));
                    }
                    candidates.add(new Hit(HitKind.TRANSFORM_ROTATE, -1, 0));
                    points.add(frame.transform(PathTransformBox.rotateHandle(box), null));
                    candidates.add(new Hit(HitKind.TRANSFORM_MOVE, -1, 0));
                    points.add(frame.transform(PathTransformBox.moveHandle(box), null));
                    candidates.add(new Hit(HitKind.TRANSFORM_PIVOT, -1, 0));
                    points.add(toViewExact(transformPivotWorld()));
                    for (int k = 0; k < candidates.size(); k++) {
                        double distance = points.get(k).distance(point);
                        distance *= distance;
                        // a handle exactly over a node wins the tie: nodes do not drag here
                        if (distance <= best + 2e-9) {
                            best = distance;
                            hit = candidates.get(k);
                        }
                    }
                }
            }
            return hit != null ? hit : node >= 0 ? new Hit(HitKind.NODE, node, 0) : null;
        }
        for (int i : showOverlays.isSelected() && operationPreview == null ? selectedNodes : Set.<Integer>of()) {
            if (i >= path.size())
                continue;
            if (editMode == EditMode.RADIUS) {
                double distance = PathEditHandles.radiusHandle(view.get(i), radiusInView(path.get(i)))
                        .distanceSq(point);
                if (distance <= best) {
                    best = distance;
                    hit = new Hit(HitKind.RADIUS, i, 0);
                }
            } else if (editMode == EditMode.CURVE) {
                var tangent = PathEditHandles.tangent(view, i);
                if (tangent == null)
                    continue;
                for (int side : new int[]{-1, 1}) {
                    double distance = tangent.handle(side, handleStrength).distanceSq(point);
                    if (distance <= best) {
                        best = distance;
                        hit = new Hit(HitKind.STRENGTH, i, side);
                    }
                }
            } else {
                Float ground = groundAt(path.get(i));
                if (ground == null)
                    continue;
                var axis = new Point2D.Double(view.get(i).x + heightAxisOffset(), view.get(i).y);
                double distance = PathEditHandles.heightHandle(axis, path.get(i).z - ground).distanceSq(point);
                if (distance <= best) {
                    best = distance;
                    hit = new Hit(HitKind.HEIGHT, i, 0);
                }
            }
        }
        return hit != null ? hit : node >= 0 ? new Hit(HitKind.NODE, node, 0) : null;
    }

    /** Terrain height under a node, or {@code null} where the map has no tile. */
    private Float groundAt(Point4f node) {
        return groundAt(Math.round(node.x), Math.round(node.y));
    }

    private Float groundAt(int x, int y) {
        if (overlayDimension == null || overlayDimension.getTile(x >> TILE_SIZE_BITS, y >> TILE_SIZE_BITS) == null)
            return null;
        float ground = overlayDimension.getHeightAt(x, y);
        return Float.isFinite(ground) ? ground : null;
    }

    private float clampHeight(float z) {
        return Math.max(overlayDimension.getMinHeight(), Math.min(overlayDimension.getMaxHeight() - 1, z));
    }

    /**
     * Shifts the height of all selected nodes by the same amount; the dragged node
     * lands on a whole number of blocks above/below ground.
     */
    /**
     * Height change the mouse asks for: the dragged node lands on a whole number of
     * blocks above/below ground. {@code null} where the node has no ground.
     */
    private Float mouseHeightDelta() {
        Point2D center = viewNodes(gesturePath).get(heightHandleNode);
        Point4f dragged = gesturePath.get(heightHandleNode);
        Float ground = groundAt(dragged);
        if (ground == null)
            return null;
        float oldRelative = dragged.z - ground;
        double moved = PathEditHandles.relativeHeightAt(center, gestureCursor)
                - PathEditHandles.relativeHeightAt(center, gestureStart);
        return Math.round(oldRelative + moved) - oldRelative;
    }

    /** Shifts the height of all selected nodes by {@code delta} blocks. */
    private ArrayList<Point4f> raisedPath(float delta) {
        var result = new ArrayList<Point4f>(gesturePath.size());
        for (int i = 0; i < gesturePath.size(); i++) {
            var node = new Point4f(gesturePath.get(i));
            if (selectedNodes.contains(i))
                node.z = clampHeight(node.z + delta);
            result.add(node);
        }
        return result;
    }

    private List<Point2D.Double> viewNodes(List<Point4f> path) {
        var result = new ArrayList<Point2D.Double>(path.size());
        for (Point4f node : path) {
            Point point = overlayView.worldToView(Math.round(node.x), Math.round(node.y));
            result.add(new Point2D.Double(point.x, point.y));
        }
        return result;
    }

    private double radiusInView(Point4f node) {
        Point center = overlayView.worldToView(Math.round(node.x), Math.round(node.y));
        Point edge = overlayView.worldToView(Math.round(node.x + node.w), Math.round(node.y));
        return center.distance(edge);
    }

    /**
     * Scale factor the mouse asks for, in world coordinates (immune to
     * panning/zooming).
     */
    private float mouseScaleFactor() {
        Point4f dragged = gesturePath.get(radiusHandleNode);
        return PathEditHandles.scaleFactor(new Point2D.Float(dragged.x, dragged.y), gestureWorldStart, gestureWorld());
    }

    /**
     * Scales the radius of all selected nodes by a mouse-driven factor; the dragged
     * node snaps to whole blocks.
     */
    private ArrayList<Point4f> resizedPath(float factor) {
        var result = new ArrayList<Point4f>(gesturePath.size());
        for (int i = 0; i < gesturePath.size(); i++) {
            var node = new Point4f(gesturePath.get(i));
            if (selectedNodes.contains(i))
                node.w = PathEditHandles.scaledRadius(node.w, factor, i == radiusHandleNode);
            result.add(node);
        }
        return result;
    }

    /** Sets the radius of all selected nodes (typed value, to 0.01, at least 1). */
    private ArrayList<Point4f> radiusSetPath(float radius) {
        float value = Math.max(1f, Math.round(radius * 100) / 100f);
        var result = new ArrayList<Point4f>(gesturePath.size());
        for (int i = 0; i < gesturePath.size(); i++) {
            var node = new Point4f(gesturePath.get(i));
            if (selectedNodes.contains(i))
                node.w = value;
            result.add(node);
        }
        return result;
    }

    /**
     * Puts all selected nodes {@code relative} blocks above (negative: below) their
     * ground.
     */
    private ArrayList<Point4f> heightSetPath(float relative) {
        float value = Math.round(relative * 100) / 100f;
        var result = new ArrayList<Point4f>(gesturePath.size());
        for (int i = 0; i < gesturePath.size(); i++) {
            var node = new Point4f(gesturePath.get(i));
            Float ground = selectedNodes.contains(i) ? groundAt(node) : null;
            if (ground != null)
                node.z = clampHeight(ground + value);
            result.add(node);
        }
        return result;
    }

    private static float clampStrength(double strength) {
        double clamped = Math.max(PathEditHandles.MIN_STRENGTH, Math.min(PathEditHandles.MAX_STRENGTH, strength));
        return Math.round(clamped * 100) / 100f;
    }

    private boolean typing() {
        return numericInput != null && numericInput.edited();
    }

    /**
     * Header text like Blender's: operation and value, the typed text with a cursor
     * while typing.
     */
    private String header(String operation, String mouseValue) {
        return operation + ": " + (numericInput == null ? mouseValue : numericInput.display(mouseValue));
    }

    void handleSelectionDrag(Point point) {
        handleSelectionDrag(point, false);
    }

    void handleSelectionDrag(Point point, boolean precise) {
        syncDimension();
        if (gestureStart == null)
            return;
        double scale = precise ? PRECISE_DRAG_SCALE : 1;
        gestureCursor.x += (point.x - gestureLastRaw.x) * scale;
        gestureCursor.y += (point.y - gestureLastRaw.y) * scale;
        gestureLastRaw = new Point(point);
        if (gestureStart.distanceSq(point) >= 9)
            gestureDragged = true;
        boolean editsGeometry = radiusHandleNode >= 0 || strengthHandleNode >= 0 || heightHandleNode >= 0
                || randomizeHandleNode >= 0 || pressedNode >= 0 || transformKind != null;
        if (editsGeometry && precise && !gesturePrecise)
            overlay.showTransientInfoText("Precise drag", 1200);
        gesturePrecise = precise;
        // typed numbers are absolute values; the mouse value is shown until typing
        if (randomizeHandleNode >= 0) {
            if (gestureDragged && circleHandleIndex >= 0 && circleHandleIndex < operationCircles.size()) {
                OperationCircle circle = operationCircles.get(circleHandleIndex);
                var model = (SpinnerNumberModel) circle.spinner().getModel();
                double lowest = ((Number) model.getMinimum()).doubleValue();
                double highest = ((Number) model.getMaximum()).doubleValue();
                Point4f node = circleBase.get(randomizeHandleNode);
                var center = new Point2D.Double(node.x, node.y);
                // grows/shrinks by how far the handle moved away from the node: no jump on grab
                double moved = center.distance(gestureWorld()) - center.distance(gestureWorldStart);
                float mouse = (float) Math.min(highest,
                        Math.max(lowest, Math.round((randomizeDragStart + moved) * 2) / 2.0));
                double value = typing() ? Math.min(highest, Math.max(lowest, numericInput.apply(mouse))) : mouse;
                setSpinnerNumber(circle.spinner(), value);
                gestureHeader = header(headerLabel(circle), formatValue(mouse));
                overlay.repaint();
            }
            return;
        }
        if (transformKind != null) {
            if (gestureDragged)
                updateTransformDrag();
            return;
        }
        if (radiusHandleNode >= 0) {
            if (gestureDragged) {
                var mousePath = resizedPath(mouseScaleFactor());
                float mouse = mousePath.get(radiusHandleNode).w;
                movedPath = typing() ? radiusSetPath((float) numericInput.apply(mouse)) : mousePath;
                gestureHeader = header("Radius", formatValue(mouse));
                overlay.repaint();
            }
            return;
        }
        if (heightHandleNode >= 0) {
            if (gestureDragged) {
                Float delta = mouseHeightDelta();
                Float ground = groundAt(gesturePath.get(heightHandleNode));
                if (delta != null && ground != null) {
                    float mouse = Math.round((gesturePath.get(heightHandleNode).z + delta - ground) * 10) / 10f;
                    movedPath = typing() ? heightSetPath((float) numericInput.apply(mouse)) : raisedPath(delta);
                    gestureHeader = header("Height", PathEditHandles.signed(mouse));
                }
                overlay.repaint();
            }
            return;
        }
        if (strengthHandleNode >= 0) {
            var tangent = PathEditHandles.tangent(viewNodes(gesturePath), strengthHandleNode);
            if (gestureDragged && tangent != null) {
                float mouse = tangent.strengthAt(strengthHandleSide, gestureCursor);
                dragStrength = typing() ? clampStrength(numericInput.apply(mouse)) : mouse;
                gestureHeader = header("Curve strength", String.format(Locale.ROOT, "%.2f", mouse));
                overlay.repaint();
            }
            return;
        }
        if (pressedNode < 0 && gestureDragged)
            overlay.updateDrag(point);
        else if (pressedNode >= 0 && gestureDragged && nodeDrags(pressedNode, gestureAdditive)) {
            if (!selectedNodes.contains(pressedNode)) {
                if (!gestureAdditive)
                    selectedNodes.clear();
                selectedNodes.add(pressedNode);
            }
            movedPath = movedPath();
            overlay.repaint();
        }
    }

    void handleSelectionRelease(Point point) {
        handleSelectionRelease(point, false);
    }

    void handleSelectionRelease(Point point, boolean precise) {
        syncDimension();
        if (gestureStart == null)
            return;
        try {
            handleSelectionDrag(point, precise);
            if (randomizeHandleNode >= 0)
                return; // the spinner already holds the new radius; Apply commits the operation
            if (transformKind != null) {
                if (!gestureDragged) {
                    if (transformKind == HitKind.TRANSFORM_PIVOT)
                        pivotClicked();
                    return;
                }
                if (transformKind == HitKind.TRANSFORM_PIVOT) {
                    if (draggedPivot != null) {
                        transformPivot = draggedPivot;
                        transformPivotSelection = Set.copyOf(selectedNodes);
                    }
                    return;
                }
                if (movedPath != null && !movedPath.equals(gesturePath)) {
                    // a placed pivot travels with a moved selection
                    if (transformKind == HitKind.TRANSFORM_MOVE && customPivot())
                        transformPivot = new Point2D.Double(transformPivot.x + transformMoveX,
                                transformPivot.y + transformMoveY);
                    // the box keeps its orientation (rotating turns it further)
                    turnTransformBox(transformKind == HitKind.TRANSFORM_ROTATE ? transformDegrees : 0);
                    completeEdit(movedPath);
                }
                return;
            }
            if (radiusHandleNode >= 0 || heightHandleNode >= 0) {
                if (gestureDragged && movedPath != null && !movedPath.equals(gesturePath))
                    completeEdit(movedPath);
                return;
            }
            if (strengthHandleNode >= 0) {
                if (gestureDragged && dragStrength != null)
                    handleFactorSpinner.setValue(Math.round(dragStrength * 100) / 100.0);
                return;
            }
            if (!gestureDragged && pressedNode >= 0 && !gestureAdditive && selectedNodes.contains(pressedNode)) {
                setEditMode(editMode.next());
                return;
            }
            if (gestureDragged && pressedNode < 0) {
                Rectangle bounds = overlay.getDragBounds();
                if (!gestureAdditive)
                    selectedNodes.clear();
                var path = getCurrentPath();
                for (int i = 0; i < path.size(); i++) {
                    Point4f node = path.get(i);
                    if (bounds.contains(node.x, node.y))
                        selectedNodes.add(i);
                }
            } else if (gestureDragged && pressedNode >= 0 && movedPath != null) {
                completeEdit(movedPath);
            } else if (!gestureDragged && pressedNode < 0 && (gestureAdditive || addMode.isSelected())) {
                // Ctrl+click on empty map, or any click there in add mode, adds a node
                Point world = overlayView.viewToWorld(gestureStart);
                addNode(world.x, world.y);
                return;
            } else if (!gestureDragged) {
                if (!gestureAdditive)
                    selectedNodes.clear();
                if (pressedNode >= 0 && !selectedNodes.add(pressedNode))
                    selectedNodes.remove(pressedNode); // Ctrl+click toggles
            }
            selectionChanged();
        } finally {
            cancelGesture();
            setHovered(cursorOverMap ? hitTest(point) : null);
            overlay.repaint();
        }
    }

    /**
     * Switches the editor (handles shown on selected nodes). The mode persists
     * across selections until changed by clicking a selected node or via the editor
     * menu.
     */
    void setEditMode(EditMode mode) {
        if (keyboardTransform)
            cancelKeyboardTransform();
        editMode = Objects.requireNonNull(mode);
        overlay.resetStripHeightZoom();
        overlay.updateModeButton();
        overlay.showTransientInfoText(editMode == EditMode.HEIGHT && snapToTerrain
                ? editMode.label + " (ignored while 'snap to terrain' is on)"
                : editMode.label, 2400);
        overlay.repaint();
    }

    /** Small glyph per editor, drawn in the selection colours. */
    static Icon editModeIcon(EditMode mode) {
        return LucideIcons.icon(switch (mode) {
            case RADIUS -> "radius";
            case CURVE -> "spline";
            case HEIGHT -> "arrow-up-from-line";
            case TRANSFORM -> "scaling";
        }, 16, org.ironsight.wpplugin.macromachine.Gui.OverlayStyle.TEXT);
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
        gestureAdditive = false;
        gesturePath = null;
        movedPath = null;
        gestureWorldStart = null;
        gestureCursor = null;
        gestureLastRaw = null;
        gesturePrecise = false;
        radiusHandleNode = -1;
        strengthHandleNode = -1;
        heightHandleNode = -1;
        randomizeHandleNode = -1;
        transformKind = null;
        transformOp = null;
        transformPivotStart = null;
        transformGrabWorld = null;
        transformScaleX = transformScaleY = 1;
        transformScaleAxis = 0;
        transformDegrees = 0;
        draggedPivot = null;
        transformMoveX = transformMoveY = 0;
        circleHandleIndex = -1;
        strengthHandleSide = 0;
        dragStrength = null;
        numericInput = null;
        keyboardTransform = false;
        gestureHeader = null;
        overlay.endDrag();
    }

    EditMode editMode() {
        return editMode;
    }

    /** The dropdown the editor button opens (exposed for tests). */
    JPopupMenu editorMenu() {
        return overlay.editorMenu();
    }

    /**
     * Strip zoom state as {from, to} fractions of the path length (exposed for
     * tests).
     */
    double[] stripZoom() {
        return new double[]{overlay.zoomFrom, overlay.zoomTo};
    }

    /**
     * Height zoom of the strip, or {@code null} when automatic (exposed for tests).
     */
    Double stripZoomCentre() {
        return overlay.zoomCentre;
    }

    /**
     * Pixels per block along the path and in height in the last painted strip
     * (exposed for tests).
     */
    double[] stripPixelsPerBlock() {
        return new double[]{overlay.pixelsPerBlock(), (overlay.stripBottom - overlay.stripTop) / overlay.stripRange};
    }

    /** The strip's bounds as last painted (exposed for tests). */
    Rectangle stripBounds() {
        return overlay.unravelledBounds();
    }

    /** Where node {@code index} was last drawn in the strip (exposed for tests). */
    Point stripNodePoint(int index) {
        return overlay.stripNodes[index];
    }

    /** The dropdown next to the overlays button (exposed for tests). */
    JPopupMenu overlaysMenu() {
        return overlay.overlaysMenu();
    }

    float curveStrength() {
        return handleStrength;
    }

    /**
     * Pivot as shown (world): while it is dragged, where it is dragged to; during a
     * move, carried along with the selection; otherwise where it is.
     */
    Point2D.Double displayedPivot() {
        if (draggedPivot != null)
            return draggedPivot;
        if (transformPivotStart == null)
            return transformPivotWorld();
        if (transformOp == HitKind.TRANSFORM_MOVE)
            return new Point2D.Double(transformPivotStart.x + transformMoveX, transformPivotStart.y + transformMoveY);
        return transformPivotStart;
    }

    /**
     * The transform box as drawn: {@code box} in the frame that {@code frame} maps
     * to the view.
     */
    record ShownBox(java.awt.geom.AffineTransform frame, java.awt.geom.Rectangle2D.Double box) {
    }

    /**
     * Always the tight box around the nodes as drawn ({@code path}: moved while
     * dragging), turned by the box angle plus the rotation so far, so it never runs
     * ahead of the nodes and nothing jumps on release. {@code null} without a
     * selection.
     */
    ShownBox shownTransformBox(List<Point4f> path) {
        double angle = transformBoxAngle() + (transformOp == HitKind.TRANSFORM_ROTATE ? transformDegrees : 0);
        var box = PathTransformBox.box(viewNodes(path), selectedNodes, angle);
        return box == null ? null : new ShownBox(PathTransformBox.frame(angle), box);
    }

    /** Angle of the transform box: 0 (axis aligned) for a new selection. */
    double transformBoxAngle() {
        return transformAngleSelection.equals(selectedNodes) ? transformAngle : 0;
    }

    /**
     * After a transform of the current selection: keeps the box angle, turned by
     * {@code degrees}.
     */
    private void turnTransformBox(double degrees) {
        double angle = transformBoxAngle() + degrees;
        transformAngle = angle - 360 * Math.floor((angle + 180) / 360);
        transformAngleSelection = Set.copyOf(selectedNodes);
    }

    private long lastPivotClickAt;

    /**
     * A click on the pivot; the second of a double click puts it back on the box
     * centre.
     */
    private void pivotClicked() {
        long now = System.currentTimeMillis();
        Object interval = Toolkit.getDefaultToolkit().getDesktopProperty("awt.multiClickInterval");
        long limit = interval instanceof Integer value ? value : 500;
        if (now - lastPivotClickAt <= limit) {
            lastPivotClickAt = 0;
            transformPivot = null;
            overlay.showTransientInfoText("Pivot reset to the centre", 1500);
        } else
            lastPivotClickAt = now;
    }

    /** The pivot was placed by the user for the current selection. */
    private boolean customPivot() {
        return transformPivot != null && transformPivotSelection.equals(selectedNodes);
    }

    /**
     * Pivot for rotating and scaling (world): placed by the user, else the
     * selection's centre.
     */
    Point2D.Double transformPivotWorld() {
        if (customPivot())
            return new Point2D.Double(transformPivot.x, transformPivot.y);
        var path = getCurrentPath();
        if (overlayView != null) {
            double angle = transformBoxAngle();
            var box = PathTransformBox.box(viewNodes(path), selectedNodes, angle);
            if (box != null)
                return toWorldExact(PathTransformBox.frame(angle).transform(PathTransformBox.centre(box), null));
        }
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (int i : selectedNodes) {
            if (i >= path.size())
                continue;
            minX = Math.min(minX, path.get(i).x);
            minY = Math.min(minY, path.get(i).y);
            maxX = Math.max(maxX, path.get(i).x);
            maxY = Math.max(maxY, path.get(i).y);
        }
        return minX > maxX ? new Point2D.Double() : new Point2D.Double((minX + maxX) / 2, (minY + maxY) / 2);
    }

    /**
     * World to view without rounding to blocks (the map view is a scale and an
     * offset).
     */
    Point2D.Double toViewExact(Point2D world) {
        Point origin = overlayView.worldToView(0, 0), far = overlayView.worldToView(1024, 1024);
        return new Point2D.Double(origin.x + (far.x - origin.x) / 1024.0 * world.getX(),
                origin.y + (far.y - origin.y) / 1024.0 * world.getY());
    }

    private Point2D.Double toWorldExact(Point2D view) {
        Point origin = overlayView.worldToView(0, 0), far = overlayView.worldToView(1024, 1024);
        return new Point2D.Double((view.getX() - origin.x) * 1024.0 / (far.x - origin.x),
                (view.getY() - origin.y) * 1024.0 / (far.y - origin.y));
    }

    /**
     * Applies a view-space transform to the selected nodes: they land on whole
     * blocks and keep their height above ground; {@code null} if a node would leave
     * the map. Radii stay as they are.
     */
    private ArrayList<Point4f> transformedPath(java.awt.geom.AffineTransform view) {
        var result = new ArrayList<Point4f>(gesturePath.size());
        for (int i = 0; i < gesturePath.size(); i++) {
            var node = new Point4f(gesturePath.get(i));
            if (selectedNodes.contains(i)) {
                var moved = toWorldExact(view.transform(toViewExact(new Point2D.Double(node.x, node.y)), null));
                int x = (int) Math.round(moved.x), y = (int) Math.round(moved.y);
                Float ground = groundAt(x, y);
                if (ground == null || ground < overlayDimension.getMinHeight()
                        || ground >= overlayDimension.getMaxHeight())
                    return null;
                Float oldGround = groundAt(gesturePath.get(i));
                float relative = oldGround == null ? 0 : node.z - oldGround;
                node.x = x;
                node.y = y;
                node.z = clampHeight(ground + relative);
            }
            result.add(node);
        }
        return result;
    }

    /**
     * The transform so far in current view coordinates (rebuilt from its world
     * terms on every use), or {@code null} before anything was transformed.
     */
    java.awt.geom.AffineTransform transformViewNow() {
        if (transformOp == null || transformPivotStart == null)
            return null;
        var pivot = toViewExact(transformPivotStart);
        return switch (transformOp) {
            case TRANSFORM_ROTATE -> PathTransformBox.rotation(pivot, transformDegrees);
            case TRANSFORM_SCALE ->
                PathTransformBox.axisScaling(pivot, transformScaleX, transformScaleY, transformBoxAngle());
            case TRANSFORM_MOVE -> {
                var from = toViewExact(new Point2D.Double(0, 0));
                var to = toViewExact(new Point2D.Double(transformMoveX, transformMoveY));
                yield java.awt.geom.AffineTransform.getTranslateInstance(to.x - from.x, to.y - from.y);
            }
            default -> null;
        };
    }

    /**
     * Mouse (or typed value) to transform while a transform handle is dragged. All
     * in world coordinates: where the handle was grabbed plus how far the mouse
     * moved since (Shift: precise).
     */
    private void updateTransformDrag() {
        var grab = transformGrabWorld;
        var now = new Point2D.Double(grab.x + (gestureCursor.x - gestureStart.x) * worldPerPixelX,
                grab.y + (gestureCursor.y - gestureStart.y) * worldPerPixelY);
        switch (transformKind) {
            case TRANSFORM_PIVOT -> {
                draggedPivot = new Point2D.Double(Math.round(transformPivotStart.x + now.x - grab.x),
                        Math.round(transformPivotStart.y + now.y - grab.y));
                gestureHeader = "Pivot: " + Math.round(draggedPivot.x) + ", " + Math.round(draggedPivot.y);
            }
            case TRANSFORM_MOVE -> {
                transformMoveX = (int) Math.round(now.x - grab.x);
                transformMoveY = (int) Math.round(now.y - grab.y);
                transformOp = HitKind.TRANSFORM_MOVE;
                movedPath = transformedPath(transformViewNow());
                gestureHeader = "Move: " + transformMoveX + ", " + transformMoveY;
            }
            case TRANSFORM_ROTATE -> {
                double mouse = PathTransformBox.rotationDegrees(transformPivotStart, grab, now);
                transformDegrees = typing() ? numericInput.apply(mouse) : mouse;
                transformOp = HitKind.TRANSFORM_ROTATE;
                movedPath = transformedPath(transformViewNow());
                gestureHeader = header("Rotate", formatValue((float) mouse) + "\u00b0");
            }
            case TRANSFORM_SCALE -> {
                int axis = transformScaleAxis;
                double mouse = axis == PathTransformBox.SCALE_UNIFORM
                        ? PathTransformBox.scaleFactor(transformPivotStart, grab, now)
                        : PathTransformBox.axisScaleFactor(transformPivotStart, grab, now, transformBoxAngle(), axis);
                double factor = typing() ? PathTransformBox.clampScale(numericInput.apply(mouse)) : mouse;
                transformScaleX = axis == PathTransformBox.SCALE_Y ? 1 : factor;
                transformScaleY = axis == PathTransformBox.SCALE_X ? 1 : factor;
                transformOp = HitKind.TRANSFORM_SCALE;
                movedPath = transformedPath(transformViewNow());
                String label = switch (axis) {
                    case PathTransformBox.SCALE_X -> "Scale X";
                    case PathTransformBox.SCALE_Y -> "Scale Y";
                    default -> "Scale";
                };
                gestureHeader = header(label, String.format(Locale.ROOT, "%.2f", mouse) + "\u00d7");
            }
            default -> {
            }
        }
        overlay.repaint();
    }

    private ArrayList<Point4f> movedPath() {
        Point2D.Double world = gestureWorld();
        int dx = (int) Math.round(world.x - gestureWorldStart.x);
        int dy = (int) Math.round(world.y - gestureWorldStart.y);
        var result = new ArrayList<Point4f>();
        for (int i = 0; i < gesturePath.size(); i++) {
            var node = new Point4f(gesturePath.get(i));
            if (selectedNodes.contains(i)) {
                node.x += dx;
                node.y += dy;
                int x = Math.round(node.x), y = Math.round(node.y);
                if (overlayDimension.getTile(x >> TILE_SIZE_BITS, y >> TILE_SIZE_BITS) == null)
                    return null;
                float ground = overlayDimension.getHeightAt(x, y);
                if (!Float.isFinite(ground) || ground < overlayDimension.getMinHeight()
                        || ground >= overlayDimension.getMaxHeight())
                    return null;
                // keep the node's height above/below ground
                Float oldGround = groundAt(gesturePath.get(i));
                float relative = oldGround == null ? 0 : gesturePath.get(i).z - oldGround;
                node.z = clampHeight(ground + relative);
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
        /**
         * Per curve sample: last node passed, see
         * {@link PathEditHandles#segmentIndices}.
         */
        private int[] cachedSegments;
        /**
         * Editor button: mode icon and name, with a painted dropdown arrow on the
         * right.
         */
        private final DropdownButton modeButton = new DropdownButton("");
        private final JToggleButton overlaysButton = new JToggleButton(overlaysIcon());
        private final JButton overlaysMenuButton = new JButton(DROPDOWN_ARROW);
        private final DropdownButton nodeMenuButton = new DropdownButton("Node");
        private final DropdownButton segmentMenuButton = new DropdownButton("Segment");
        private final DropdownButton pathMenuButton = new DropdownButton("Path");
        private JPanel shownOperationPanel;
        private final JToggleButton addButton = new JToggleButton("Add", addIcon());

        PathOverlay() {
            super(loadPathIcon());
            modeButton.setName("PathTool.EditorMode");
            modeButton.setToolTipText("Editor for selected nodes (also: click a selected node to cycle)");
            modeButton.addActionListener(event -> {
                if (modeButton.isShowing())
                    org.ironsight.wpplugin.macromachine.Gui.OverlayStyle.popup(editorMenu())
                            .show(modeButton, 0, modeButton.getHeight());
            });
            add(modeButton);

            overlaysButton.setName("PathTool.OverlaysButton");
            overlaysButton.setModel(showOverlays.getModel());
            overlaysButton.setFocusable(false);
            overlaysButton.setToolTipText("Show overlays (nodes, path line, radii, labels, handles, preview layer)");
            add(overlaysButton);
            overlaysMenuButton.setName("PathTool.OverlaysMenu");
            overlaysMenuButton.setFocusable(false);
            overlaysMenuButton.setMargin(new Insets(2, 3, 2, 3));
            overlaysMenuButton.setToolTipText("Choose which overlays to show");
            overlaysMenuButton.addActionListener(event -> {
                if (overlaysMenuButton.isShowing())
                    org.ironsight.wpplugin.macromachine.Gui.OverlayStyle.popup(overlaysMenu())
                            .show(overlaysMenuButton, 0, overlaysMenuButton.getHeight());
            });
            add(overlaysMenuButton);
            nodeMenuButton.setName("PathTool.NodeMenu");
            nodeMenuButton.setToolTipText("Operations on the selected nodes");
            nodeMenuButton.addActionListener(event -> {
                if (nodeMenuButton.isShowing())
                    org.ironsight.wpplugin.macromachine.Gui.OverlayStyle.popup(nodeMenu())
                            .show(nodeMenuButton, 0, nodeMenuButton.getHeight());
            });
            add(nodeMenuButton);
            segmentMenuButton.setName("PathTool.SegmentMenu");
            segmentMenuButton.setToolTipText("Operations on segments between two selected nodes");
            segmentMenuButton.addActionListener(event -> {
                if (segmentMenuButton.isShowing())
                    org.ironsight.wpplugin.macromachine.Gui.OverlayStyle.popup(segmentMenu())
                            .show(segmentMenuButton, 0, segmentMenuButton.getHeight());
            });
            add(segmentMenuButton);
            pathMenuButton.setName("PathTool.PathMenu");
            pathMenuButton.setToolTipText("Build the path: apply as road or river, path settings");
            pathMenuButton.addActionListener(event -> {
                if (pathMenuButton.isShowing())
                    org.ironsight.wpplugin.macromachine.Gui.OverlayStyle.popup(pathMenu())
                            .show(pathMenuButton, 0, pathMenuButton.getHeight());
            });
            add(pathMenuButton);
            addButton.setName("PathTool.AddButton");
            addButton.setModel(addMode.getModel());
            addButton.setFocusable(false);
            addButton.setToolTipText(
                    "Add mode: left click on empty map adds a node (double-tap Ctrl). Ctrl+click always adds.");
            add(addButton);
            for (AbstractButton control : List.of(nodeMenuButton, segmentMenuButton, pathMenuButton, modeButton,
                    overlaysButton, overlaysMenuButton, addButton))
                org.ironsight.wpplugin.macromachine.Gui.OverlayStyle.button(control);
            overlaysMenuButton.setBorder(new EmptyBorder(5, 6, 5, 6));
            // active toggles are solid lavender: their icons switch to charcoal
            var onAccent = org.ironsight.wpplugin.macromachine.Gui.OverlayStyle.ON_ACCENT;
            overlaysButton.setSelectedIcon(LucideIcons.icon("eye", 16, onAccent));
            addButton.setSelectedIcon(LucideIcons.icon("plus", 16, onAccent));
            setFont(org.ironsight.wpplugin.macromachine.Gui.OverlayStyle.font(Font.PLAIN, 14f)); // painted labels
            installStripMouse();
            updateModeButton();
        }

        /**
         * One row of buttons per overlay part, each a single click: the part's toggle
         * button (sharing the switch's model) toggles it, "Only" shows only this part,
         * "All but" shows everything except it; "All" in the title row shows all. Plain
         * buttons, not menu items, so the popup stays open while clicking (menu items
         * close it, and WorldPainter's look and feel ignores the "do not close" hint).
         * It closes on a click outside or Escape.
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
            var all = overlayMenuButton("All", "PathTool.Overlays.All", "Show all overlays");
            all.addActionListener(event -> showOverlayParts(part -> true));
            cell.gridx = 2;
            cell.gridwidth = 1;
            rows.add(all, cell);

            for (JToggleButton part : overlayParts()) {
                cell.gridy++;
                var toggle = new JToggleButton(overlayPartTitle(part));
                toggle.setModel(part.getModel());
                toggle.setName(part.getName());
                toggle.setFocusable(false);
                toggle.setToolTipText("Show / hide " + overlayPartTitle(part).toLowerCase(Locale.ROOT));
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

        void updateModeButton() {
            // the overlay is created during field initialisation, before editMode is set
            EditMode mode = editMode == null ? EditMode.RADIUS : editMode;
            modeButton.setContent("Editor: " + mode.title, editModeIcon(mode));
            layoutModeButton();
        }

        JPopupMenu editorMenu() {
            var menu = new JPopupMenu();
            var group = new ButtonGroup();
            for (EditMode mode : EditMode.values()) {
                var item = new JRadioButtonMenuItem(mode.title, editModeIcon(mode), mode == editMode);
                item.setName("PathTool.EditorMode." + mode.name());
                item.addActionListener(event -> setEditMode(mode));
                group.add(item);
                menu.add(item);
            }
            return menu;
        }

        private void layoutModeButton() {
            java.awt.Dimension size = modeButton.getPreferredSize();
            // Node / Segment menus first (like Blender's Vertex / Edge menus), then the
            // editor
            nodeMenuButton.setBounds(8, 8, nodeMenuButton.getPreferredSize().width, size.height);
            segmentMenuButton.setBounds(nodeMenuButton.getX() + nodeMenuButton.getWidth() + 4, 8,
                    segmentMenuButton.getPreferredSize().width, size.height);
            pathMenuButton.setBounds(segmentMenuButton.getX() + segmentMenuButton.getWidth() + 4, 8,
                    pathMenuButton.getPreferredSize().width, size.height);
            modeButton.setBounds(pathMenuButton.getX() + pathMenuButton.getWidth() + 8, 8, size.width, size.height);
            layoutOperationPanel();
            layoutResetZoomButton();
            int x = modeButton.getX() + size.width + 8;
            int toggleWidth = overlaysButton.getPreferredSize().width;
            overlaysButton.setBounds(x, 8, toggleWidth, size.height);
            overlaysMenuButton.setBounds(x + toggleWidth, 8, overlaysMenuButton.getPreferredSize().width, size.height);
            int addX = overlaysMenuButton.getX() + overlaysMenuButton.getWidth() + 8;
            addButton.setBounds(addX, 8, addButton.getPreferredSize().width, size.height);
            // lay out the dropdowns' text and arrow for their new size now (null layout:
            // nothing else re-validates them, leaving truncated text)
            for (DropdownButton dropdown : List.of(nodeMenuButton, segmentMenuButton, pathMenuButton, modeButton))
                dropdown.validate();
            repaint();
        }

        /** Bottom of the toolbar row (the header and operation panel go below it). */
        int toolbarBottom() {
            return modeButton.getY() + modeButton.getHeight();
        }

        /** Right edge of the open operation panel, or 0. */
        int operationPanelRight() {
            return shownOperationPanel == null ? 0 : shownOperationPanel.getX() + shownOperationPanel.getWidth();
        }

        void relayoutOperationPanel() {
            layoutOperationPanel();
            repaint();
        }

        void showOperationPanel(JPanel panel) {
            shownOperationPanel = panel;
            add(panel);
            layoutOperationPanel();
            revalidate();
            repaint();
        }

        void hideOperationPanel(JPanel panel) {
            remove(panel);
            if (shownOperationPanel == panel)
                shownOperationPanel = null;
            repaint();
        }

        /**
         * Top left, right below the toolbar. (Blender puts its "Adjust Last Operation"
         * panel bottom left, but there WorldPainter draws its compass and scale bar.)
         */
        private void layoutOperationPanel() {
            if (shownOperationPanel == null)
                return;
            java.awt.Dimension size = shownOperationPanel.getPreferredSize();
            int y = modeButton.getY() + modeButton.getHeight() + 8;
            shownOperationPanel.setBounds(8, y, size.width, size.height);
            shownOperationPanel.validate();
        }

        @Override
        public void setBounds(int x, int y, int width, int height) {
            super.setBounds(x, y, width, height);
            layoutModeButton();
        }

        /** Mouse-transparent like the base overlay, except over the editor button. */
        @Override
        public boolean contains(int x, int y) {
            // the profile strip takes its clicks (they must not select or add nodes below
            // it)
            if (lastBottomReserve > 0 && unravelledBounds().contains(x, y))
                return true;
            if (shownOperationPanel != null && shownOperationPanel.getBounds().contains(x, y))
                return true;
            for (Component control : List.of(nodeMenuButton, segmentMenuButton, pathMenuButton, modeButton,
                    overlaysButton, overlaysMenuButton, addButton))
                if (control.isVisible() && control.getBounds().contains(x, y))
                    return true;
            return false;
        }

        private List<Point4f> curveFor(List<Point4f> nodes) {
            float strength = displayedStrength();
            if (cachedNodes == null || !cachedNodes.equals(nodes) || Float.compare(cachedStrength, strength) != 0) {
                cachedNodes = nodes.stream().map(Point4f::new).toList();
                cachedStrength = strength;
                cachedCurve = nodes.size() < 2 ? List.of() : getPathFromNodes(cachedNodes, strength).path;
                cachedSegments = PathEditHandles.segmentIndices(cachedNodes, cachedCurve);
            }
            return cachedCurve;
        }

        /**
         * One chevron per segment at its middle sample, pointing along the curve from
         * the lower to the higher node index (the flow direction for rivers). Segments
         * too short on screen for an arrow are skipped.
         */
        private void paintDirectionArrows(Graphics2D g, List<Point4f> path) {
            var curve = curveFor(path);
            int[] segments = cachedSegments;
            int count = Math.max(0, path.size() - 1);
            int[] first = new int[count], last = new int[count];
            java.util.Arrays.fill(first, -1);
            for (int k = 0; k < segments.length; k++) {
                int segment = segments[k];
                if (segment < 0 || segment >= count)
                    continue;
                if (first[segment] < 0)
                    first[segment] = k;
                last[segment] = k;
            }
            for (int j = 0; j < count; j++) {
                if (first[j] < 0)
                    continue;
                Point from = toView(path.get(j)), to = toView(path.get(j + 1));
                if (from.distance(to) < MIN_ARROW_SEGMENT_PX)
                    continue;
                int middle = (first[j] + last[j] + 1) / 2;
                double[] direction = arrowTangent(curve, first[j], Math.min(curve.size() - 1, last[j] + 1), middle,
                        sample -> toViewExact(new Point2D.Double(sample.x, sample.y)));
                if (direction == null)
                    continue;
                paintChevron(g, toView(curve.get(middle)), direction[0], direction[1],
                        PathEditHandles.segmentSelected(j, shownSelection()));
            }
        }

        private Point toView(Point4f point) {
            return overlayView.worldToView(Math.round(point.x), Math.round(point.y));
        }

        /**
         * Curve as screen lines; segments between two selected nodes go on top,
         * highlighted.
         */
        private void paintCurve(Graphics2D g, List<Point4f> path) {
            var curve = curveFor(path);
            var highlighted = new Path2D.Double();
            g.setColor(UNSELECTED_NODE_FILL);
            g.setStroke(new BasicStroke(1.5f));
            for (int i = 1; i < curve.size(); i++) {
                Point4f a = curve.get(i - 1), b = curve.get(i);
                Point start = overlayView.worldToView(Math.round(a.x), Math.round(a.y));
                Point end = overlayView.worldToView(Math.round(b.x), Math.round(b.y));
                if (PathEditHandles.segmentSelected(cachedSegments[i - 1], shownSelection())) {
                    highlighted.moveTo(start.x, start.y);
                    highlighted.lineTo(end.x, end.y);
                } else
                    g.drawLine(start.x, start.y, end.x, end.y);
            }
            g.setColor(SELECTED_NODE_FILL);
            g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(highlighted);
        }

        /**
         * Visible part of the path in the strip (fractions of its length), and height
         * zoom.
         */
        private double zoomFrom = 0, zoomTo = 1;
        /**
         * Height view: blocks are square (one scale for distance and height); this is
         * the height at the strip's vertical centre, {@code null} = centred on the
         * profile.
         */
        private Double zoomCentre;
        private Point zoomDragStart, zoomDragNow;
        /**
         * Last strip mapping, for turning mouse positions back into path distance /
         * height.
         */
        private double stripLength = 1, stripLeft, stripRight = 1, stripTop, stripBottom = 1, stripLowest,
                stripRange = 1;
        private boolean stripHeightView;
        private final JButton resetZoomButton = new JButton("Reset zoom");

        boolean stripZoomed() {
            return zoomFrom > 0 || zoomTo < 1 || zoomCentre != null;
        }

        void resetStripZoom() {
            zoomFrom = 0;
            zoomTo = 1;
            zoomCentre = null;
            resetZoomButton.setEnabled(false);
            repaint();
        }

        void resetStripHeightZoom() {
            zoomCentre = null;
            resetZoomButton.setEnabled(stripZoomed());
        }

        private double fractionAt(double x) {
            return zoomFrom + (x - stripLeft) / (stripRight - stripLeft) * (zoomTo - zoomFrom);
        }

        private double heightAt(double y) {
            return stripLowest + (stripBottom - y) / (stripBottom - stripTop) * stripRange;
        }

        /** Smallest visible part: about 8 blocks of path. */
        private double minimumZoomWidth() {
            return Math.min(1, 8 / Math.max(1, stripLength));
        }

        /**
         * Zooms the strip to a rectangle dragged on it (distance; in the height view
         * also height).
         */
        /**
         * What a zoom drag will select. Axis drags are unmistakable: a band across the
         * whole strip with bracket edges and a double-headed arrow along the axis; a
         * box drag is a dashed rectangle.
         */
        private void paintZoomBand(Graphics2D g, Rectangle box, ZoomDrag drag) {
            g.setColor(org.ironsight.wpplugin.macromachine.Gui.OverlayStyle.ACCENT_SOFT);
            g.fill(box);
            if (drag == ZoomDrag.BOX) {
                g.setStroke(
                        new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10, new float[]{4, 3}, 0));
                g.setColor(org.ironsight.wpplugin.macromachine.Gui.OverlayStyle.ACCENT);
                g.draw(box);
                return;
            }
            boolean horizontal = drag == ZoomDrag.HORIZONTAL;
            int tick = 6;
            var shape = new Path2D.Double();
            if (horizontal) {
                int x0 = box.x, x1 = box.x + box.width, y0 = box.y, y1 = box.y + box.height,
                        mid = box.y + box.height / 2;
                for (int x : new int[]{x0, x1}) { // bracket edges |- ... -|
                    int inward = x == x0 ? tick : -tick;
                    shape.moveTo(x + inward, y0);
                    shape.lineTo(x, y0);
                    shape.lineTo(x, y1);
                    shape.lineTo(x + inward, y1);
                }
                shape.moveTo(x0 + 2, mid); // <---->
                shape.lineTo(x1 - 2, mid);
                shape.moveTo(x0 + 2 + tick, mid - tick);
                shape.lineTo(x0 + 2, mid);
                shape.lineTo(x0 + 2 + tick, mid + tick);
                shape.moveTo(x1 - 2 - tick, mid - tick);
                shape.lineTo(x1 - 2, mid);
                shape.lineTo(x1 - 2 - tick, mid + tick);
            } else {
                int y0 = box.y, y1 = box.y + box.height, x0 = box.x, x1 = box.x + box.width,
                        mid = box.x + box.width / 2;
                for (int y : new int[]{y0, y1}) {
                    int inward = y == y0 ? tick : -tick;
                    shape.moveTo(x0, y + inward);
                    shape.lineTo(x0, y);
                    shape.lineTo(x1, y);
                    shape.lineTo(x1, y + inward);
                }
                shape.moveTo(mid, y0 + 2);
                shape.lineTo(mid, y1 - 2);
                shape.moveTo(mid - tick, y0 + 2 + tick);
                shape.lineTo(mid, y0 + 2);
                shape.lineTo(mid + tick, y0 + 2 + tick);
                shape.moveTo(mid - tick, y1 - 2 - tick);
                shape.lineTo(mid, y1 - 2);
                shape.lineTo(mid + tick, y1 - 2 - tick);
            }
            strokeHandle(g, shape, 2f);
        }

        /** Kind of zoom drag, like plotly: along one axis or a box. */
        enum ZoomDrag {
            HORIZONTAL, VERTICAL, BOX
        }

        /**
         * Below this many pixels along an axis, a drag counts as not moving along it
         * (plotly: 8).
         */
        static final int ZOOM_DRAG_THRESHOLD = 8;

        /**
         * Classifies a drag; {@code null} for a click (or vertical outside the height
         * view).
         */
        ZoomDrag zoomDrag(Point start, Point end) {
            int dx = Math.abs(end.x - start.x), dy = Math.abs(end.y - start.y);
            if (dx < ZOOM_DRAG_THRESHOLD && dy < ZOOM_DRAG_THRESHOLD)
                return null;
            // lenient: within ~22 degrees of an axis (or barely moving across it) counts as
            // a drag along that axis
            if (dy < AXIS_DRAG_SLACK_PX || dy < dx * AXIS_DRAG_RATIO)
                return ZoomDrag.HORIZONTAL;
            if (dx < AXIS_DRAG_SLACK_PX || dx < dy * AXIS_DRAG_RATIO)
                return stripHeightView ? ZoomDrag.VERTICAL : null;
            return stripHeightView ? ZoomDrag.BOX : ZoomDrag.HORIZONTAL;
        }

        /** Movement across an axis below which a drag counts as along that axis. */
        static final int AXIS_DRAG_SLACK_PX = 12;
        /** ... or below this share of the movement along it (tan 22 degrees). */
        static final double AXIS_DRAG_RATIO = 0.4;

        /** The band a drag selects: full strip height / width for single-axis drags. */
        Rectangle zoomBand(Point start, Point end, ZoomDrag drag) {
            var box = new Rectangle(start);
            box.add(end);
            return switch (drag) {
                case HORIZONTAL -> new Rectangle(box.x, (int) stripTop, box.width, (int) (stripBottom - stripTop));
                case VERTICAL -> new Rectangle((int) stripLeft, box.y, (int) (stripRight - stripLeft), box.height);
                case BOX -> box;
            };
        }

        /**
         * Shows {@code width} (fraction of the path) centred on {@code centre}, within
         * the path.
         */
        private void showDistance(double centre, double width) {
            width = Math.max(minimumZoomWidth(), Math.min(1, width));
            zoomFrom = Math.max(0, Math.min(1 - width, centre - width / 2));
            zoomTo = zoomFrom + width;
        }

        /**
         * Pixels per block along the path (in the height view also vertically: square
         * blocks).
         */
        private double pixelsPerBlock() {
            return (stripRight - stripLeft) / Math.max(1e-9, (zoomTo - zoomFrom) * stripLength);
        }

        /**
         * Zooms to a dragged band. Horizontal: that distance range. In the height view
         * blocks stay square, so vertical and box drags set one scale for both axes:
         * vertical = the dragged heights fill the strip, box = the whole box fits; both
         * centre on the dragged area.
         */
        void zoomStripTo(Rectangle box, ZoomDrag drag) {
            double from = fractionAt(box.x), to = fractionAt(box.x + box.width);
            double viewWidth = stripRight - stripLeft, viewHeight = stripBottom - stripTop;
            switch (drag) {
                case HORIZONTAL -> showDistance((from + to) / 2, to - from);
                case VERTICAL -> {
                    double blocksTall = Math.max(2, heightAt(box.y) - heightAt(box.y + box.height));
                    double perBlock = viewHeight / blocksTall;
                    showDistance((zoomFrom + zoomTo) / 2, viewWidth / perBlock / stripLength);
                    zoomCentre = heightAt(box.y + box.height / 2.0);
                }
                case BOX -> {
                    double blocksWide = Math.max(1e-9, (to - from) * stripLength);
                    double blocksTall = Math.max(2, heightAt(box.y) - heightAt(box.y + box.height));
                    double perBlock = Math.min(viewWidth / blocksWide, viewHeight / blocksTall);
                    double centreHeight = heightAt(box.y + box.height / 2.0);
                    showDistance((from + to) / 2, viewWidth / perBlock / stripLength);
                    zoomCentre = centreHeight;
                }
            }
            resetZoomButton.setEnabled(stripZoomed());
            repaintStrip();
        }

        /**
         * Repaints only the strip's area (zoom, pan, drag band), not the whole overlay
         * with its toolbar and the map below. In the height view the strip can change
         * height, so the bottom half (its maximum) is repainted.
         */
        void repaintStrip() {
            if (stripHeightView)
                repaint(0, Math.max(0, getHeight() / 2 - 16), getWidth(), getHeight());
            else
                repaint(unravelledBounds());
        }

        /** Node markers as last painted in the strip, by node index. */
        private Point[] stripNodes = new Point[0];

        /**
         * A plain click on a node in the strip selects just that node (nothing else: no
         * toggling, no editor cycling). Ignored while an operation is previewed, whose
         * node numbers can differ from the path's.
         */
        void selectStripNodeAt(Point point) {
            if (operationPreview != null)
                return;
            int nearest = -1;
            double best = NODE_HIT_RADIUS * NODE_HIT_RADIUS;
            for (int i = 0; i < stripNodes.length; i++) {
                double distance = stripNodes[i].distanceSq(point);
                if (distance <= best) {
                    best = distance;
                    nearest = i;
                }
            }
            if (nearest < 0 || nearest >= getCurrentPath().size())
                return;
            selectedNodes.clear();
            selectedNodes.add(nearest);
            selectionChanged();
        }

        /** Middle-mouse panning of the strip (like WorldPainter's map). */
        private Point panStart;
        private double panFrom, panCentre;

        void startStripPan(Point point) {
            panStart = point;
            panFrom = zoomFrom;
            panCentre = zoomCentre != null ? zoomCentre : stripLowest + stripRange / 2;
            setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        }

        /**
         * Moves the visible part with the mouse: along the path, and in height in the
         * height view.
         */
        void panStripTo(Point point) {
            double width = zoomTo - zoomFrom;
            double shift = (point.x - panStart.x) / (stripRight - stripLeft) * width;
            zoomFrom = Math.max(0, Math.min(1 - width, panFrom - shift));
            zoomTo = zoomFrom + width;
            if (stripHeightView) {
                double perBlock = (stripBottom - stripTop) / stripRange;
                zoomCentre = panCentre + (point.y - panStart.y) / perBlock;
            }
            resetZoomButton.setEnabled(stripZoomed());
            repaintStrip();
        }

        /**
         * Mouse wheel: zooms in (rotation < 0) or out around the mouse position; the
         * block under the mouse stays where it is (both axes in the height view).
         */
        void zoomStripAround(Point mouse, int rotation) {
            double factor = rotation > 0 ? 1.25 : 0.8;
            double at = fractionAt(mouse.x), share = (mouse.x - stripLeft) / (stripRight - stripLeft);
            double oldWidth = zoomTo - zoomFrom;
            double width = Math.max(minimumZoomWidth(), Math.min(1, oldWidth * factor));
            double from = Math.max(0, Math.min(1 - width, at - share * width));
            zoomFrom = from;
            zoomTo = from + width;
            if (stripHeightView) {
                if (zoomFrom <= 0 && zoomTo >= 1 && rotation > 0)
                    zoomCentre = null; // fully out: centred on the profile again
                else {
                    double underMouse = heightAt(mouse.y);
                    double range = stripRange * width / oldWidth;
                    double lowest = underMouse - (stripBottom - mouse.y) / (stripBottom - stripTop) * range;
                    zoomCentre = lowest + range / 2;
                }
            }
            resetZoomButton.setEnabled(stripZoomed());
            repaintStrip();
        }

        private void installStripMouse() {
            var mouse = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent event) {
                    if (!unravelledBounds().contains(event.getPoint()))
                        return;
                    if (SwingUtilities.isLeftMouseButton(event)) {
                        zoomDragStart = event.getPoint();
                        zoomDragNow = null;
                    } else if (SwingUtilities.isMiddleMouseButton(event))
                        startStripPan(event.getPoint());
                }

                @Override
                public void mouseDragged(MouseEvent event) {
                    if (panStart != null)
                        panStripTo(event.getPoint());
                    else if (zoomDragStart != null) {
                        zoomDragNow = event.getPoint();
                        repaintStrip();
                    }
                }

                @Override
                public void mouseReleased(MouseEvent event) {
                    if (panStart != null && SwingUtilities.isMiddleMouseButton(event)) {
                        panStart = null;
                        setCursor(null);
                        return;
                    }
                    if (zoomDragStart == null)
                        return;
                    var start = zoomDragStart;
                    zoomDragStart = null;
                    zoomDragNow = null;
                    var drag = zoomDrag(start, event.getPoint());
                    if (drag != null)
                        zoomStripTo(zoomBand(start, event.getPoint(), drag), drag);
                    else
                        selectStripNodeAt(event.getPoint());
                    repaintStrip();
                }

                @Override
                public void mouseWheelMoved(java.awt.event.MouseWheelEvent event) {
                    if (unravelledBounds().contains(event.getPoint()))
                        zoomStripAround(event.getPoint(), event.getWheelRotation());
                }
            };
            // listeners make the strip itself the mouse target: clicks, drags and the wheel
            // there no longer reach the map below (contains() limits this to the strip)
            addMouseListener(mouse);
            addMouseMotionListener(mouse);
            addMouseWheelListener(mouse);
            resetZoomButton.setName("PathTool.ProfileResetZoom");
            resetZoomButton.setFocusable(false);
            org.ironsight.wpplugin.macromachine.Gui.OverlayStyle.smallButton(resetZoomButton);
            resetZoomButton.setEnabled(false);
            resetZoomButton.setVisible(false);
            resetZoomButton.addActionListener(event -> resetStripZoom());
            add(resetZoomButton);
        }

        private void layoutResetZoomButton() {
            Rectangle strip = unravelledBounds();
            java.awt.Dimension size = resetZoomButton.getPreferredSize();
            resetZoomButton.setBounds(strip.x + strip.width - size.width - 6, strip.y + 2, size.width,
                    Math.min(size.height, 16));
        }

        /** Block a height belongs to, rounded like WorldPainter's int heights. */
        private static int blockOf(float height) {
            return Math.round(height);
        }

        /**
         * Surface of a height profile block by block: flat across each block's column
         * at the top of its block, with vertical steps between blocks.
         */
        private static Path2D blockSteps(int[] blocks, int from, int to, double[] columnLeft, double[] columnRight,
                java.util.function.DoubleUnaryOperator yAt) {
            var steps = new Path2D.Double();
            for (int k = from; k <= to; k++) {
                double y = yAt.applyAsDouble(blocks[k] + 1);
                if (k == from)
                    steps.moveTo(columnLeft[k], y);
                else
                    steps.lineTo(columnLeft[k], y);
                steps.lineTo(columnRight[k], y);
            }
            return steps;
        }

        private static void closeRun(Path2D shape, double x, double floor) {
            shape.lineTo(x, floor);
            shape.closePath();
        }

        /** Closes a water run: back along the terrain surface under it. */
        private static void closeWaterRun(Path2D shape, HeightProfile profile, int end, double[] columnLeft,
                double[] columnRight, java.util.function.DoubleUnaryOperator yAt) {
            int k = end;
            while (k >= 0 && profile.water()[k] != null && profile.terrain()[k] != null) {
                double groundY = yAt.applyAsDouble(profile.terrain()[k] + 1);
                shape.lineTo(columnRight[k], groundY);
                shape.lineTo(columnLeft[k], groundY);
                k--;
            }
            shape.closePath();
        }

        /**
         * Block data of the height view along the sampled curve: terrain and water
         * blocks, the path's own block per sample (the curve through its nodes; the
         * Apply settings only act when applying) and the height range.
         */
        record HeightProfile(List<Point4f> curve, long revision, Dimension dimension, Integer[] terrain,
                Integer[] water, int[] heights, double low, double high) {
        }

        private HeightProfile cachedProfile;

        /**
         * The height profile, recomputed only when the curve, the settings or the
         * terrain may have changed (every such change refreshes the preview and bumps
         * its revision).
         */
        HeightProfile heightProfile(List<Point4f> curve, int first, int last) {
            var cached = cachedProfile;
            if (cached != null && cached.curve() == curve && cached.revision() == previewRevision
                    && cached.dimension() == overlayDimension)
                return cached;
            var terrain = new Integer[curve.size()];
            var water = new Integer[curve.size()];
            int[] heights = new int[curve.size()];
            double low = Double.MAX_VALUE, high = -Double.MAX_VALUE;
            for (int k = first; k <= last; k++) {
                Point4f sample = curve.get(k);
                heights[k] = blockOf(sample.z);
                low = Math.min(low, heights[k]);
                high = Math.max(high, heights[k] + 1);
                Float ground = groundAt(sample);
                if (ground == null)
                    continue;
                terrain[k] = Math.round(ground);
                low = Math.min(low, terrain[k]);
                high = Math.max(high, terrain[k] + 1);
                if (overlayDimension != null) {
                    int level = overlayDimension.getWaterLevelAt(Math.round(sample.x), Math.round(sample.y));
                    if (level > terrain[k]) {
                        water[k] = level;
                        high = Math.max(high, level + 1);
                    }
                }
            }
            cachedProfile = new HeightProfile(curve, previewRevision, overlayDimension, terrain, water, heights, low,
                    high);
            return cachedProfile;
        }

        /** Block height numbers along the left edge, spaced to stay readable. */
        private static void paintHeightScale(Graphics2D g, Rectangle strip, double lowest, double range,
                java.util.function.DoubleUnaryOperator yAt) {
            double pixelsPerBlock = (yAt.applyAsDouble(lowest) - yAt.applyAsDouble(lowest + range)) / range;
            int step = 1;
            for (int candidate : new int[]{1, 2, 5, 10, 20, 50, 100})
                if (candidate * pixelsPerBlock >= 16) {
                    step = candidate;
                    break;
                } else
                    step = candidate;
            Font previous = g.getFont();
            g.setFont(previous.deriveFont(Font.PLAIN, 11f));
            g.setColor(new Color(255, 255, 255, 170));
            for (int height = (int) Math.ceil(lowest / step) * step; height <= lowest + range; height += step) {
                int y = (int) Math.round(yAt.applyAsDouble(height));
                g.drawLine(strip.x + 2, y, strip.x + 6, y);
                g.drawString(Integer.toString(height), strip.x + 2, y - 2);
            }
            g.setFont(previous);
        }

        /**
         * Bounds of the unravelled path strip at the bottom of the viewport (exposed
         * for tests).
         */
        Rectangle unravelledBounds() {
            return new Rectangle(8, getHeight() - stripHeight - 8, Math.max(0, getWidth() - 16), stripHeight);
        }

        /**
         * Current strip height: grows in the height view to fit the whole height axis.
         */
        private int stripHeight = UNRAVELLED_HEIGHT;

        /**
         * Keeps info messages and the reset button clear of the strip (layout outside
         * painting).
         */
        private void reserveStripSpace(int reserve) {
            if (reserve == lastBottomReserve)
                return;
            lastBottomReserve = reserve;
            SwingUtilities.invokeLater(() -> {
                setBottomReserve(reserve);
                resetZoomButton.setVisible(reserve > 0);
                layoutResetZoomButton();
            });
        }

        /**
         * Height view: blocks are square, so the strip needs (height range in blocks) x
         * (pixels per block at the current zoom) to show the whole height axis. It
         * grows to that, from the normal height up to half the viewport; beyond that
         * the view is panned / zoomed vertically.
         */
        private int heightViewStripHeight(List<Point4f> curve, int first, int last, double length) {
            HeightProfile profile = heightProfile(curve, first, last);
            double low = profile.low(), high = profile.high();
            // same span as the painted profile: a band of ground below, a little air above
            double blocks = high + 2 - (low - Math.max(3, (high - low) * 0.3));
            double innerWidth = getWidth() - 16 - 48;
            double perBlock = innerWidth / Math.max(1e-9, (zoomTo - zoomFrom) * length);
            int needed = (int) Math.ceil(blocks * perBlock) + 22 + 14;
            return Math.max(stripMinimumHeight(), Math.min(stripMaximumHeight(), needed));
        }

        /** The strip takes at least 20% of the viewport's height... */
        int stripMinimumHeight() {
            return (int) Math.round(getHeight() * STRIP_MIN_SHARE);
        }

        /** ...and at most half of it. */
        int stripMaximumHeight() {
            return Math.max(stripMinimumHeight(), (int) Math.round(getHeight() * STRIP_MAX_SHARE));
        }

        /**
         * The path unravelled into a horizontal strip at the bottom of the viewport:
         * nodes in order along the path, spaced by their distance along the curve, in
         * the path's own styling. Each editor has its own view: radius as a ribbon
         * around the line, height above ground as an elevation profile with terrain and
         * water read from the dimension, curve strength as the plain linked list of
         * nodes.
         */
        /** The strip as last rendered, reused while nothing it shows has changed. */
        private BufferedImage stripImage;
        private List<Object> stripImageKey;
        private int stripImageTop;

        /**
         * Everything the strip's picture depends on. Repaints for other reasons (hover,
         * info message animations, drags on the map) reuse the rendered strip instead
         * of rasterising terrain, water and the profile again.
         */
        private List<Object> stripKey(List<Point4f> path) {
            return java.util.Arrays.asList(curveFor(path), path, previewRevision, overlayDimension, zoomFrom, zoomTo,
                    zoomCentre, getWidth(), getHeight(), editMode, Set.copyOf(shownSelection()), shows(showLabel),
                    displayedStrength(), operationPreview);
        }

        private void paintUnravelled(Graphics2D g, List<Point4f> path) {
            if (!(shows(showProfile) && path.size() >= 2)) {
                reserveStripSpace(0);
                return;
            }
            var key = stripKey(path);
            int regionTop = Math.max(0, getHeight() / 2 - 16); // the strip never reaches above this
            if (stripImage == null || !key.equals(stripImageKey) || stripImage.getWidth() != Math.max(1, getWidth())
                    || stripImageTop != regionTop) {
                // in the screen's own format, so drawing the cached picture needs no conversion
                var image = g.getDeviceConfiguration()
                        .createCompatibleImage(Math.max(1, getWidth()), Math.max(1, getHeight() - regionTop),
                                Transparency.TRANSLUCENT);
                Graphics2D target = image.createGraphics();
                try {
                    target.setRenderingHints(g.getRenderingHints());
                    target.setFont(g.getFont());
                    target.translate(0, -regionTop);
                    renderStrip(target, path);
                } finally {
                    target.dispose();
                }
                stripImage = image;
                stripImageKey = key;
                stripImageTop = regionTop;
            }
            Rectangle strip = unravelledBounds();
            g.drawImage(stripImage, strip.x, strip.y, strip.x + strip.width, strip.y + strip.height, strip.x,
                    strip.y - stripImageTop, strip.x + strip.width, strip.y + strip.height - stripImageTop, null);
            // the zoom drag band is live on top of the cached picture
            var drag = zoomDragStart != null && zoomDragNow != null ? zoomDrag(zoomDragStart, zoomDragNow) : null;
            if (drag != null) {
                Shape oldClip = g.getClip();
                g.clipRect(strip.x + 2, strip.y + 18, strip.width - 4, strip.height - 20);
                paintZoomBand(g, zoomBand(zoomDragStart, zoomDragNow, drag), drag);
                g.setClip(oldClip);
            }
        }

        /** Draws the strip (into the cached picture). */
        private void renderStrip(Graphics2D g, List<Point4f> path) {
            boolean visible = shows(showProfile) && path.size() >= 2;
            if (!visible) {
                reserveStripSpace(0);
                return;
            }
            var curve = curveFor(path);
            int[] segments = cachedSegments;
            int n = path.size();
            // first curve sample of every node, and distance along the curve per sample
            int[] nodeSample = new int[n];
            for (int i = 0, k = 0; i < n; i++) {
                while (k < segments.length && segments[k] < i)
                    k++;
                nodeSample[i] = Math.min(k, curve.size() - 1);
            }
            int first = nodeSample[0], last = curve.size() - 1;
            if (last <= first)
                return;
            double[] along = new double[curve.size()];
            for (int k = first + 1; k <= last; k++)
                along[k] = along[k - 1]
                        + Math.hypot(curve.get(k).x - curve.get(k - 1).x, curve.get(k).y - curve.get(k - 1).y);
            double length = along[last];
            if (length <= 0)
                return;
            stripHeight = editMode == EditMode.HEIGHT
                    ? heightViewStripHeight(curve, first, last, length)
                    : stripMinimumHeight();
            reserveStripSpace(stripHeight + 12);

            Rectangle strip = unravelledBounds();
            org.ironsight.wpplugin.macromachine.Gui.OverlayStyle.paintSurface(g, strip.x, strip.y, strip.width,
                    strip.height, org.ironsight.wpplugin.macromachine.Gui.OverlayStyle.SURFACE);
            int left = strip.x + 24, right = strip.x + strip.width - 24;
            int top = strip.y + 22, bottom = strip.y + strip.height - 14;
            // zoom: the visible part of the path (fractions of its length)
            double x0 = zoomFrom, x1 = zoomTo;
            java.util.function.DoubleUnaryOperator xAt = s -> left + (s / length - x0) / (x1 - x0) * (right - left);
            stripLength = length;
            stripLeft = left;
            stripRight = right;
            stripTop = top;
            stripBottom = bottom;
            stripHeightView = editMode == EditMode.HEIGHT;
            Font previous = g.getFont();
            g.setFont(org.ironsight.wpplugin.macromachine.Gui.OverlayStyle.font(Font.BOLD, 13f));
            g.setColor(org.ironsight.wpplugin.macromachine.Gui.OverlayStyle.TEXT_MUTED);
            g.drawString(switch (editMode) {
                case RADIUS -> "Radius along the path";
                case HEIGHT -> "Height along the path (terrain, water, path)";
                case CURVE -> String.format(Locale.ROOT, "Path (curve strength %.2f)", displayedStrength());
                case TRANSFORM -> "Path";
            }, strip.x + 10, strip.y + 15);
            g.setFont(previous);
            Shape oldClip = g.getClip();
            g.clipRect(strip.x + 2, strip.y + 18, strip.width - 4, strip.height - 20);

            // y of each node in the strip, depending on the editor
            double[] nodeY = new double[n];
            Set<Integer> selection = shownSelection();
            if (editMode == EditMode.HEIGHT) {
                // block data along the path, cached: sampled once per path / settings / terrain
                // state, not on every repaint
                HeightProfile profile = heightProfile(curve, first, last);
                double low = profile.low(), high = profile.high();
                // square blocks: the height scale is the distance scale; centred on the
                // profile (keeping a band of ground below it) unless zoomed elsewhere
                double perBlock = (right - left) / Math.max(1e-9, (x1 - x0) * length);
                double range = (bottom - top) / perBlock;
                double autoCentre = (low - Math.max(3, (high - low) * 0.3) + high + 2) / 2;
                double lowest = (zoomCentre != null ? zoomCentre : autoCentre) - range / 2;
                stripLowest = lowest;
                stripRange = range;
                java.util.function.DoubleUnaryOperator yAt = v -> bottom - (v - lowest) / range * (bottom - top);
                // only the columns that are visible (zoomed in: a small part of the path)
                int from = first, to = last;
                while (from < last && xAt.applyAsDouble(along[from + 1]) < strip.x)
                    from++;
                while (to > from && xAt.applyAsDouble(along[to - 1]) > strip.x + strip.width)
                    to--;
                // column of every block along the path: halfway to its neighbours
                double[] columnLeft = new double[curve.size()], columnRight = new double[curve.size()];
                for (int k = from; k <= to; k++) {
                    columnLeft[k] = xAt.applyAsDouble(k > first ? (along[k - 1] + along[k]) / 2 : along[k]);
                    columnRight[k] = xAt.applyAsDouble(k < last ? (along[k] + along[k + 1]) / 2 : along[k]);
                }
                // terrain and water as one stepped outline each (not a shape per block)
                double floor = strip.y + strip.height;
                var terrainShape = new Path2D.Double();
                var waterShape = new Path2D.Double();
                boolean inTerrain = false, inWater = false;
                for (int k = from; k <= to; k++) {
                    Integer ground = profile.terrain()[k];
                    if (ground == null) {
                        if (inTerrain)
                            closeRun(terrainShape, columnRight[k - 1], floor);
                        if (inWater)
                            closeWaterRun(waterShape, profile, k - 1, columnLeft, columnRight, yAt);
                        inTerrain = inWater = false;
                        continue;
                    }
                    double groundY = yAt.applyAsDouble(ground + 1);
                    if (!inTerrain) {
                        terrainShape.moveTo(columnLeft[k], floor);
                        inTerrain = true;
                    }
                    terrainShape.lineTo(columnLeft[k], groundY);
                    terrainShape.lineTo(columnRight[k], groundY);
                    Integer level = profile.water()[k];
                    if (level != null) {
                        double surfaceY = yAt.applyAsDouble(level + 1);
                        if (!inWater) {
                            waterShape.moveTo(columnLeft[k], surfaceY);
                            inWater = true;
                        }
                        waterShape.lineTo(columnLeft[k], surfaceY);
                        waterShape.lineTo(columnRight[k], surfaceY);
                    } else if (inWater) {
                        closeWaterRun(waterShape, profile, k - 1, columnLeft, columnRight, yAt);
                        inWater = false;
                    }
                }
                if (inTerrain)
                    closeRun(terrainShape, columnRight[to], floor);
                if (inWater)
                    closeWaterRun(waterShape, profile, to, columnLeft, columnRight, yAt);
                g.setColor(PROFILE_TERRAIN);
                g.fill(terrainShape);
                g.setColor(PROFILE_WATER);
                g.fill(waterShape);
                // zoomed in far enough: block grid as lines (one per visible row and column
                // edge), clipped to terrain and water, instead of an outline per block
                if (perBlock >= BLOCK_GRID_MIN_PX) {
                    Shape clipBefore = g.getClip();
                    var solid = new java.awt.geom.Area(terrainShape);
                    solid.add(new java.awt.geom.Area(waterShape));
                    g.clip(solid);
                    g.setStroke(new BasicStroke(1f));
                    g.setColor(new Color(0, 0, 0, 70));
                    for (int block = (int) Math.floor(lowest); block <= lowest + range + 1; block++) {
                        int y = (int) Math.round(yAt.applyAsDouble(block));
                        g.drawLine(strip.x, y, strip.x + strip.width, y);
                    }
                    for (int k = from; k <= to; k++) {
                        int x = (int) Math.round(columnLeft[k]);
                        g.drawLine(x, strip.y, x, (int) floor);
                    }
                    g.setClip(clipBefore);
                }
                paintHeightScale(g, strip, lowest, range, yAt);
                // the path's height per segment, block by block: red where it rises from the
                // previous node, green where it falls or stays level; selected segments get a
                // cyan underlay
                for (int i = 0; i + 1 < n; i++) {
                    int start = Math.max(nodeSample[i], from), stop = Math.min(nodeSample[i + 1], to);
                    if (stop <= start)
                        continue;
                    var segment = blockSteps(profile.heights(), start, stop, columnLeft, columnRight, yAt);
                    if (PathEditHandles.segmentSelected(i, selection)) {
                        g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        g.setColor(SELECTED_NODE_FILL);
                        g.draw(segment);
                    }
                    boolean rises = profile.heights()[nodeSample[i + 1]] > profile.heights()[nodeSample[i]];
                    g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.setColor(rises ? SLOPE_UP_COLOR : SLOPE_DOWN_COLOR);
                    g.draw(segment);
                }
                for (int i = 0; i < n; i++)
                    nodeY[i] = yAt.applyAsDouble(blockOf(path.get(i).z) + 1);
            } else {
                double middle = (top + bottom) / 2.0;
                if (editMode == EditMode.RADIUS) {
                    double widest = 1;
                    for (int k = first; k <= last; k++)
                        widest = Math.max(widest, curve.get(k).w);
                    double scale = (bottom - top) / 2.0 / widest;
                    var ribbon = new Path2D.Double();
                    ribbon.moveTo(xAt.applyAsDouble(along[first]), middle - curve.get(first).w * scale);
                    for (int k = first + 1; k <= last; k++)
                        ribbon.lineTo(xAt.applyAsDouble(along[k]), middle - curve.get(k).w * scale);
                    for (int k = last; k >= first; k--)
                        ribbon.lineTo(xAt.applyAsDouble(along[k]), middle + curve.get(k).w * scale);
                    ribbon.closePath();
                    g.setColor(new Color(255, 255, 255, 60));
                    g.fill(ribbon);
                    g.setColor(new Color(255, 255, 255, 150));
                    g.setStroke(new BasicStroke(1f));
                    g.draw(ribbon);
                }
                var line = new Path2D.Double();
                var highlighted = new Path2D.Double();
                for (int i = 0; i + 1 < n; i++) {
                    var target = PathEditHandles.segmentSelected(i, selection) ? highlighted : line;
                    target.moveTo(xAt.applyAsDouble(along[nodeSample[i]]), middle);
                    target.lineTo(xAt.applyAsDouble(along[nodeSample[i + 1]]), middle);
                }
                g.setColor(STRIP_INACTIVE);
                g.setStroke(new BasicStroke(1.5f));
                g.draw(line);
                strokeHandle(g, highlighted, 2f);
                for (int i = 0; i < n; i++)
                    nodeY[i] = middle;
            }
            var points = new Point[n];
            for (int i = 0; i < n; i++) {
                var point = new Point((int) Math.round(xAt.applyAsDouble(along[nodeSample[i]])),
                        (int) Math.round(nodeY[i]));
                points[i] = point;
                boolean selected = selection.contains(i);
                paintNode(g, point, selected, false, STRIP_INACTIVE);
                if (shows(showLabel))
                    paintNodeLabel(g, point, i + 1, selected);
            }
            stripNodes = points;
            g.setClip(oldClip);
        }

        /**
         * Dotted line from each node's old to its previewed position (randomize, snap),
         * drawn below the nodes; keeps dense node clusters readable. Only for
         * operations that keep the node count (not subdivide).
         */
        private void paintMovementLines(Graphics2D g, List<Point4f> before, List<Point4f> after) {
            if (before.size() != after.size())
                return;
            var dotted = new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10, new float[]{1, 4}, 0);
            var halo = new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10, new float[]{1, 4}, 0);
            for (int i = 0; i < before.size(); i++) {
                Point from = toView(before.get(i)), to = toView(after.get(i));
                if (from.equals(to))
                    continue;
                var line = new Line2D.Double(from, to);
                g.setStroke(dotted);
                g.setColor(PREVIEW_COLOR);
                g.draw(line);
            }
        }

        /**
         * The open operation's circles around every selected (original) node, each with
         * its handle, in the blue handle styling: things the user drags and edits are
         * blue; amber is reserved for previewed, not yet applied results.
         */
        private void paintOperationCircles(Graphics2D g) {
            var view = viewNodes(circleBase);
            for (int c = 0; c < operationCircles.size(); c++) {
                OperationCircle circle = operationCircles.get(c);
                for (int i : circleSelection) {
                    if (i >= circleBase.size())
                        continue;
                    Point2D.Double center = view.get(i);
                    double radius = radiusInView(circleBase.get(i), circle.value());
                    var shape = new Ellipse2D.Double(center.x - radius, center.y - radius, 2 * radius, 2 * radius);
                    // a handle's circle: blue handle styling (amber is for previewed results only)
                    strokeHandle(g, shape, 2f);
                    var handle = circleHandle(center, radius, circle.side());
                    if (Math.abs(handle.x - center.x) > radius + 1)
                        strokeHandle(g,
                                new Line2D.Double(center.x + circle.side() * radius, center.y, handle.x, handle.y), 1f);
                    paintSquareHandle(g, handle, isHovered(HitKind.OPERATION_RADIUS, i, c));
                    String text = circle.label() + " " + formatValue(circle.value());
                    if (circle.side() > 0)
                        paintHandleLabel(g, text, handle);
                    else
                        paintHandleLabel(g, text, new Point2D.Double(
                                handle.x - g.getFontMetrics().stringWidth(text) - 2 * (HOVERED_HANDLE_SIZE / 2 + 4),
                                handle.y));
                }
            }
        }

        /**
         * Heights an operation would change, in the preview style: a dashed amber ring
         * around the node and the change in blocks ("-3").
         */
        private void paintPreviewHeights(Graphics2D g, List<Point4f> before, List<Point4f> after) {
            if (before.size() != after.size())
                return;
            var dashed = new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10, new float[]{4, 3}, 0);
            for (int i = 0; i < after.size(); i++) {
                float change = after.get(i).z - before.get(i).z;
                if (Math.abs(change) < 0.05f)
                    continue;
                Point center = toView(after.get(i));
                int ring = NODE_OUTLINE_DIAMETER + 10;
                var shape = new Ellipse2D.Double(center.x - ring / 2.0, center.y - ring / 2.0, ring, ring);
                g.setStroke(dashed);
                g.setColor(PREVIEW_COLOR);
                g.draw(shape);
                paintPreviewLabel(g, PathEditHandles.signed(Math.round(change * 10) / 10f),
                        new Point2D.Double(center.x + ring / 2.0, center.y + ring / 2.0 + 12));
            }
        }

        /**
         * Radii an operation would change, in the preview style (dashed amber), on top
         * of the regular radius circles.
         */
        private void paintPreviewRadii(Graphics2D g, List<Point4f> before, List<Point4f> after) {
            if (before.size() != after.size())
                return;
            var dashed = new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10, new float[]{6, 4}, 0);
            for (int i = 0; i < after.size(); i++) {
                if (Float.compare(before.get(i).w, after.get(i).w) == 0)
                    continue;
                Point center = toView(after.get(i));
                double radius = radiusInView(after.get(i), after.get(i).w);
                var shape = new Ellipse2D.Double(center.x - radius, center.y - radius, 2 * radius, 2 * radius);
                g.setStroke(dashed);
                g.setColor(PREVIEW_COLOR);
                g.draw(shape);
            }
        }

        /**
         * Transform editor: box around the selection with scale handles on the corners,
         * rotate handle above, move handle below and the pivot. While dragging, box and
         * handles follow the transform.
         */
        private void paintTransformBox(Graphics2D g, List<Point4f> path) {
            var shown = shownTransformBox(path);
            if (shown == null)
                return;
            var box = shown.box();
            var transform = shown.frame();
            java.util.function.UnaryOperator<Point2D> at = p -> transform.transform(p, null);
            if (selectedNodes.size() < 2) {
                // a single node only moves: no box, rotate, scale or pivot
                paintIconHandle(g, "move", at.apply(PathTransformBox.singleMoveHandle(box)), 0,
                        isHovered(HitKind.TRANSFORM_MOVE, -1, 0));
                return;
            }
            Shape outline = transform.createTransformedShape(box);
            g.setStroke(new BasicStroke(1.5f)); // solid, not dashed
            g.setColor(HANDLE_COLOR);
            g.draw(outline);
            strokeHandle(g, new Line2D.Double(at.apply(PathTransformBox.topCentre(box)),
                    at.apply(PathTransformBox.rotateHandle(box))), 1.5f);
            strokeHandle(g, new Line2D.Double(at.apply(PathTransformBox.leftCentre(box)),
                    at.apply(PathTransformBox.moveHandle(box))), 1.5f);
            // handles are Lucide icons turned with the box (scale arrows point along
            // their axis)
            double turned = -Math.toDegrees(Math.atan2(transform.getShearY(), transform.getScaleX()));
            paintIconHandle(g, "move-diagonal-2",
                    at.apply(PathTransformBox.scaleHandle(box, PathTransformBox.SCALE_UNIFORM)), turned,
                    isHovered(HitKind.TRANSFORM_SCALE, -1, PathTransformBox.SCALE_UNIFORM));
            paintIconHandle(g, "move-horizontal", at.apply(PathTransformBox.scaleHandle(box, PathTransformBox.SCALE_X)),
                    turned, isHovered(HitKind.TRANSFORM_SCALE, -1, PathTransformBox.SCALE_X));
            paintIconHandle(g, "move-vertical", at.apply(PathTransformBox.scaleHandle(box, PathTransformBox.SCALE_Y)),
                    turned, isHovered(HitKind.TRANSFORM_SCALE, -1, PathTransformBox.SCALE_Y));
            paintIconHandle(g, "rotate-ccw", at.apply(PathTransformBox.rotateHandle(box)), turned,
                    isHovered(HitKind.TRANSFORM_ROTATE, -1, 0));
            paintIconHandle(g, "move", at.apply(PathTransformBox.moveHandle(box)), turned,
                    isHovered(HitKind.TRANSFORM_MOVE, -1, 0));
            paintIconHandle(g, "crosshair", toViewExact(displayedPivot()), 0,
                    isHovered(HitKind.TRANSFORM_PIVOT, -1, 0));
        }

        private void paintEditHandles(Graphics2D g, List<Point4f> path) {
            if (editMode == EditMode.TRANSFORM) {
                paintTransformBox(g, path);
                return;
            }
            var view = viewNodes(path);
            float strength = displayedStrength();
            for (int i : selectedNodes) {
                if (i >= path.size())
                    continue;
                Point2D.Double center = view.get(i);
                if (editMode == EditMode.RADIUS) {
                    double radius = radiusInView(path.get(i));
                    var circle = new Ellipse2D.Double(center.x - radius, center.y - radius, 2 * radius, 2 * radius);
                    strokeHandle(g, circle, 2f);
                    var handle = PathEditHandles.radiusHandle(center, radius);
                    if (handle.x - center.x > radius + 1)
                        strokeHandle(g, new Line2D.Double(center.x + radius, center.y, handle.x, handle.y), 1f);
                    paintSquareHandle(g, handle, isHovered(HitKind.RADIUS, i, 0));
                    paintHandleLabel(g, formatValue(path.get(i).w), handle);
                } else if (editMode == EditMode.HEIGHT) {
                    Point4f node = path.get(i);
                    Float ground = groundAt(node);
                    if (ground == null)
                        continue;
                    float relative = node.z - ground;
                    // ground level: horizontal line through the node (gap for the marker),
                    // extended right to the height gauge, which sits beside the node
                    double gap = NODE_OUTLINE_DIAMETER / 2.0 + 2, reach = 34;
                    var axis = new Point2D.Double(center.x + heightAxisOffset(), center.y);
                    strokeHandle(g, new Line2D.Double(center.x - reach, center.y, center.x - gap, center.y), 1.5f);
                    strokeHandle(g, new Line2D.Double(center.x + gap, center.y, axis.x + 14, center.y), 1.5f);
                    var handle = PathEditHandles.heightHandle(axis, relative);
                    // ground label on the opposite side of the line from the handle
                    paintHandleLabel(g, "ground " + formatValue(Math.round(ground)),
                            new Point2D.Double(axis.x, handle.y <= center.y ? center.y + 20 : center.y - 2));
                    strokeHandle(g, new Line2D.Double(axis, handle), 1.5f);
                    paintSquareHandle(g, handle, isHovered(HitKind.HEIGHT, i, 0));
                    paintHandleLabel(g, PathEditHandles.signed(Math.round(relative * 10) / 10f) + " (abs "
                            + formatValue(Math.round(node.z * 10) / 10f) + ")", handle);
                } else {
                    var tangent = PathEditHandles.tangent(view, i);
                    if (tangent == null)
                        continue;
                    var back = tangent.handle(-1, strength);
                    var forward = tangent.handle(1, strength);
                    strokeHandle(g, new Line2D.Double(back, forward), 1.5f);
                    paintRoundHandle(g, back, isHovered(HitKind.STRENGTH, i, -1));
                    paintRoundHandle(g, forward, isHovered(HitKind.STRENGTH, i, 1));
                    paintHandleLabel(g, String.format(Locale.ROOT, "%.2f", strength), forward);
                }
            }
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
                var path = movedPath != null
                        ? movedPath
                        : operationPreview != null ? operationPreview.path() : getCurrentPath();
                if (operationPreview == null)
                    selectedNodes.removeIf(index -> index >= path.size());
                if (shows(showRadius)) {
                    g.setColor(new Color(255, 255, 255, 150));
                    for (Point4f node : path) {
                        Point left = overlayView.worldToView(Math.round(node.x - node.w), Math.round(node.y - node.w));
                        Point right = overlayView.worldToView(Math.round(node.x + node.w), Math.round(node.y + node.w));
                        g.drawOval(Math.min(left.x, right.x), Math.min(left.y, right.y), Math.abs(right.x - left.x),
                                Math.abs(right.y - left.y));
                    }
                }
                if (shows(showLine))
                    paintCurve(g, path);
                if (shows(showDirection))
                    paintDirectionArrows(g, path);
                if (operationPreview != null && operationBase != null && movedPath == null)
                    paintMovementLines(g, operationBase, operationPreview.path());
                for (int i = 0; i < path.size(); i++) {
                    Point4f node = path.get(i);
                    Point point = overlayView.worldToView(Math.round(node.x), Math.round(node.y));
                    boolean selected = shownSelection().contains(i);
                    if (operationPreview != null && merging(i))
                        paintMergeWarning(g, point);
                    boolean inserted = operationPreview != null && operationPreview.inserted().contains(i);
                    if (shows(showNodes) && inserted)
                        paintInsertedNode(g, point);
                    else if (shows(showNodes))
                        paintNode(g, point, selected, isHovered(HitKind.NODE, i, 0));
                    if (shows(showLabel))
                        paintNodeLabel(g, point, i + 1, selected);
                }
                if (showOverlays.isSelected() && operationPreview == null)
                    paintEditHandles(g, path);
                if (operationPreview != null && operationBase != null) {
                    paintPreviewRadii(g, operationBase, operationPreview.path());
                    paintPreviewHeights(g, operationBase, operationPreview.path());
                }
                if (!operationCircles.isEmpty())
                    paintOperationCircles(g);
                paintUnravelled(g, path);
                String header = viewportHeader();
                if (header != null)
                    paintHeader(g, header);
            } finally {
                g.dispose();
            }
        }
    }

    /** Horizontal distance of the height gauge from its node, in digit widths. */
    static final int HEIGHT_AXIS_OFFSET_CHARS = 5;

    double heightAxisOffset() {
        Font font = overlay.getFont();
        int digit = font == null ? 7 : overlay.getFontMetrics(font).charWidth('0');
        return HEIGHT_AXIS_OFFSET_CHARS * digit;
    }

    /** Blender-like header box at the top centre of the viewport. */
    private void paintHeader(Graphics2D g, String text) {
        Font previous = g.getFont();
        g.setFont(org.ironsight.wpplugin.macromachine.Gui.OverlayStyle.font(Font.BOLD, 16.5f));
        FontMetrics metrics = g.getFontMetrics();
        int padding = 8;
        int width = metrics.stringWidth(text) + 2 * padding, height = metrics.getHeight() + padding;
        // below the toolbar (it is too wide to share the top row), centred, but right
        // of an
        // open operation panel
        int x = Math.max((overlay.getWidth() - width) / 2, overlay.operationPanelRight() + 8);
        int y = overlay.toolbarBottom() + 8;
        org.ironsight.wpplugin.macromachine.Gui.OverlayStyle.paintSurface(g, x, y, width, height,
                org.ironsight.wpplugin.macromachine.Gui.OverlayStyle.SURFACE);
        g.setColor(org.ironsight.wpplugin.macromachine.Gui.OverlayStyle.TEXT);
        g.drawString(text, x + padding, y + padding / 2 + metrics.getAscent());
        g.setFont(previous);
    }

    private float displayedStrength() {
        return dragStrength != null ? dragStrength : handleStrength;
    }

    /**
     * Distance (px) on either side of a direction arrow over which the curve's
     * direction is measured. The curve is sampled per block, so its direction over
     * just a few samples follows the block staircase (snapping to 0, 27, 45...
     * degrees) instead of the smooth line.
     */
    static final double ARROW_TANGENT_PX = 12;

    /**
     * Unit direction of the curve at sample {@code middle} on screen, measured
     * between the samples about {@link #ARROW_TANGENT_PX} before and after it,
     * staying within samples {@code from}..{@code to} of the segment; {@code null}
     * where it has no length.
     */
    static double[] arrowTangent(List<Point4f> curve, int from, int to, int middle,
            java.util.function.Function<Point4f, Point2D> toScreen) {
        Point2D centre = toScreen.apply(curve.get(middle));
        int before = middle, after = middle;
        while (before > from && toScreen.apply(curve.get(before)).distance(centre) < ARROW_TANGENT_PX)
            before--;
        while (after < to && toScreen.apply(curve.get(after)).distance(centre) < ARROW_TANGENT_PX)
            after++;
        Point2D a = toScreen.apply(curve.get(before)), b = toScreen.apply(curve.get(after));
        double dx = b.getX() - a.getX(), dy = b.getY() - a.getY(), length = Math.hypot(dx, dy);
        return length < 1e-6 ? null : new double[]{dx / length, dy / length};
    }

    /** Segments shorter than this on screen get no direction arrow. */
    static final int MIN_ARROW_SEGMENT_PX = 24;

    /**
     * A single ">" chevron centred on {@code center}, pointing along (dirX, dirY).
     */
    static void paintChevron(Graphics2D g, Point center, double dirX, double dirY, boolean highlighted) {
        double length = 5, spread = 5;
        double tipX = center.x + dirX * length, tipY = center.y + dirY * length;
        double backX = center.x - dirX * length, backY = center.y - dirY * length;
        var chevron = new Path2D.Double();
        chevron.moveTo(backX - dirY * spread, backY + dirX * spread);
        chevron.lineTo(tipX, tipY);
        chevron.lineTo(backX + dirY * spread, backY - dirX * spread);
        // same colour as the segment's line, no outline
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(highlighted ? SELECTED_NODE_FILL : UNSELECTED_NODE_FILL);
        g.draw(chevron);
    }

    static String formatValue(float value) {
        return value == Math.rint(value)
                ? Integer.toString(Math.round(value))
                : String.format(Locale.ROOT, "%.1f", value);
    }

    /** A handle line or ring: handle colour, no outline. */
    private static void strokeHandle(Graphics2D g, Shape shape, float width) {
        g.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(HANDLE_COLOR);
        g.draw(shape);
    }

    private boolean isHovered(HitKind kind, int node, int side) {
        return hovered != null && hovered.kind() == kind && hovered.node() == node && hovered.side() == side;
    }

    /** Height of the unravelled path strip at the bottom of the viewport. */
    static final int UNRAVELLED_HEIGHT = 120;
    /** Share of the viewport's height the path profile strip takes: 20% to 50%. */
    static final double STRIP_MIN_SHARE = 0.2, STRIP_MAX_SHARE = 0.5;
    static final Color PROFILE_TERRAIN = new Color(136, 110, 84, 235);
    static final Color PROFILE_WATER = new Color(64, 128, 214, 235);
    /**
     * Block outlines in the height profile appear from this many pixels per block.
     */
    static final double BLOCK_GRID_MIN_PX = 6;
    /**
     * Height view: segment rising from its previous node (red) / falling or level
     * (green).
     */
    static final Color SLOPE_UP_COLOR = org.ironsight.wpplugin.macromachine.Gui.OverlayStyle.DANGER;
    static final Color SLOPE_DOWN_COLOR = org.ironsight.wpplugin.macromachine.Gui.OverlayStyle.SUCCESS;
    private int lastBottomReserve = -1;

    /**
     * Preview styling (amber): results of an operation that are not applied yet.
     * Handles and everything the user drags or edits use the blue handle styling
     * instead.
     */
    static final Color PREVIEW_COLOR = org.ironsight.wpplugin.macromachine.Gui.OverlayStyle.PREVIEW;
    /** Drawn handle size (px); hovered handles grow to signal interactivity. */
    static final int HANDLE_SIZE = 12, HOVERED_HANDLE_SIZE = 16;

    static void paintSquareHandle(Graphics2D g, Point2D center, boolean hovered) {
        int size = hovered ? HOVERED_HANDLE_SIZE : HANDLE_SIZE;
        int x = (int) Math.round(center.getX()) - size / 2, y = (int) Math.round(center.getY()) - size / 2;
        g.setColor(hovered ? HANDLE_HOVER : HANDLE_COLOR);
        g.fillRect(x, y, size, size);
    }

    /** Transform handles: Lucide icon size (px), larger while hovered. */
    static final int ICON_HANDLE_SIZE = 20, HOVERED_ICON_HANDLE_SIZE = 24;

    /**
     * A handle drawn as a Lucide icon in the handle styling: cyan with a dark
     * outline, white and larger while hovered.
     */
    static void paintIconHandle(Graphics2D g, String icon, Point2D center, double degrees, boolean hovered) {
        LucideIcons.paint(g, icon, center, hovered ? HOVERED_ICON_HANDLE_SIZE : ICON_HANDLE_SIZE, degrees,
                hovered ? HANDLE_HOVER : HANDLE_COLOR, null);
    }

    static void paintRoundHandle(Graphics2D g, Point2D center, boolean hovered) {
        int size = hovered ? HOVERED_HANDLE_SIZE : HANDLE_SIZE;
        int x = (int) Math.round(center.getX()) - size / 2, y = (int) Math.round(center.getY()) - size / 2;
        g.setColor(hovered ? HANDLE_HOVER : HANDLE_COLOR);
        g.fillOval(x, y, size, size);
    }

    /**
     * Label of a previewed result: a chip with amber text ({@code at}: text
     * baseline start).
     */
    static void paintPreviewLabel(Graphics2D g, String text, Point2D at) {
        paintChip(g, text, at.getX(), at.getY() - 4, PREVIEW_COLOR, true);
    }

    /** Value next to a handle: a chip with text in the handle colour (white). */
    private static void paintHandleLabel(Graphics2D g, String text, Point2D handle) {
        paintChip(g, text, handle.getX() + HOVERED_HANDLE_SIZE / 2.0 + 4, handle.getY() - 11, HANDLE_COLOR, true);
    }

    /**
     * A label on the map: a small dark chip like the viewport header (no outlined
     * text), left edge at {@code left}, vertically centred on {@code centreY}.
     */
    static void paintChip(Graphics2D g, String text, double left, double centreY, Color textColour, boolean bold) {
        Font previous = g.getFont();
        Object textHint = g.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
        g.setFont(org.ironsight.wpplugin.macromachine.Gui.OverlayStyle.font(bold ? Font.BOLD : Font.PLAIN, 12.5f));
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        FontMetrics metrics = g.getFontMetrics();
        int padX = 6, padY = 2;
        int width = metrics.stringWidth(text) + 2 * padX;
        int height = metrics.getAscent() + metrics.getDescent() + 2 * padY;
        int x = (int) Math.round(left), y = (int) Math.round(centreY - height / 2.0);
        org.ironsight.wpplugin.macromachine.Gui.OverlayStyle.paintSurface(g, x, y, width, height,
                org.ironsight.wpplugin.macromachine.Gui.OverlayStyle.SURFACE);
        g.setColor(textColour);
        g.drawString(text, x + padX, y + padY + metrics.getAscent());
        g.setFont(previous);
        if (textHint != null)
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, textHint);
    }

    /**
     * A node an operation would insert, not yet applied: dashed amber ring with a
     * dark halo, the style of the randomize radius, instead of the filled marker.
     */
    static void paintInsertedNode(Graphics2D g, Point point) {
        int outline = NODE_OUTLINE_DIAMETER;
        var ring = new Ellipse2D.Double(point.x - outline / 2.0, point.y - outline / 2.0, outline, outline);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10, new float[]{4, 3}, 0));
        g.setColor(PREVIEW_COLOR);
        g.draw(ring);
    }

    private boolean merging(int index) {
        for (int[] run : operationMerges)
            if (index >= run[0] && index <= run[run.length - 1])
                return true;
        return false;
    }

    /**
     * Warning (orange) ring around a node that the operation will merge with a
     * neighbour.
     */
    static void paintMergeWarning(Graphics2D g, Point point) {
        int ring = NODE_OUTLINE_DIAMETER + 12;
        g.setStroke(new BasicStroke(3f));
        g.setColor(WARNING_COLOR);
        g.drawOval(point.x - ring / 2, point.y - ring / 2, ring, ring);
    }

    /** Warning colour (orange): e.g. nodes an operation will merge. */
    static final Color WARNING_COLOR = org.ironsight.wpplugin.macromachine.Gui.OverlayStyle.WARNING;

    /**
     * A node: a hollow ring, plum when selected (a little bolder), light grey
     * otherwise.
     */
    static void paintNode(Graphics2D g, Point point, boolean selected) {
        paintNode(g, point, selected, false);
    }

    /**
     * Hovered: the ring turns primary text colour (white) and grows; hit area
     * unchanged.
     */
    static void paintNode(Graphics2D g, Point point, boolean selected, boolean hovered) {
        paintNode(g, point, selected, hovered, UNSELECTED_NODE_FILL);
    }

    /**
     * {@code inactive}: colour of an unselected node (lighter on the dark strip).
     */
    static void paintNode(Graphics2D g, Point point, boolean selected, boolean hovered, Color inactive) {
        double size = NODE_DIAMETER - 2 + (hovered ? 6 : 0);
        g.setColor(hovered ? HANDLE_HOVER : selected ? SELECTED_NODE_FILL : inactive);
        g.setStroke(new BasicStroke(selected || hovered ? 3f : 2f));
        g.draw(new Ellipse2D.Double(point.x - size / 2, point.y - size / 2, size, size));
    }

    /** Node number: a chip above right of the node, hot pink text when selected. */
    static void paintNodeLabel(Graphics2D g, Point point, int number, boolean selected) {
        // above right of the node: handles sit straight right of it
        paintChip(g, Integer.toString(number), point.x + NODE_DIAMETER / 2.0 - 2, point.y - NODE_DIAMETER / 2.0 - 7,
                selected
                        ? org.ironsight.wpplugin.macromachine.Gui.OverlayStyle.MAP_SELECTION_TEXT
                        : org.ironsight.wpplugin.macromachine.Gui.OverlayStyle.TEXT_MUTED,
                selected);
    }

    /**
     * Overlay switches are plain state holders: the viewport's overlay button and
     * menu share their {@link ButtonModel}, so listeners sit on the model and fire
     * whichever control changed it.
     */
    private JToggleButton hudButton(String text, String name) {
        var button = new JToggleButton(text, true);
        button.setName("PathTool." + name);
        button.getModel().addItemListener(event -> overlay.repaint());
        return button;
    }

    private JToggleButton addModeSwitch() {
        var toggle = new JToggleButton("Add", false);
        toggle.setName("PathTool.AddMode");
        toggle.getModel().addItemListener(event -> {
            updateCursor();
            overlay.showTransientInfoText(toggle.isSelected()
                    ? "Add mode: left click adds nodes (double-tap Ctrl or Esc to leave)"
                    : "Add mode off", 2400);
            overlay.repaint();
        });
        return toggle;
    }

    /** The add-mode switch (exposed for tests). */
    JToggleButton addModeToggle() {
        return addMode;
    }

    /** Plus sign for the viewport "Add" button. */
    static Icon addIcon() {
        return LucideIcons.icon("plus", 16, org.ironsight.wpplugin.macromachine.Gui.OverlayStyle.TEXT);
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
     * Whether an overlay part is drawn: its own switch and the master switch are
     * on.
     */
    private boolean shows(JToggleButton part) {
        return showOverlays.isSelected() && part.isSelected();
    }

    /** The individual overlay switches, in menu order (exposed for tests). */
    List<JToggleButton> overlayParts() {
        return List.of(showLabel, showRadius, showLine, showDirection, showNodes, showLayer, showProfile);
    }

    JToggleButton overlaysSwitch() {
        return showOverlays;
    }

    private static String overlayPartTitle(JToggleButton part) {
        return switch (part.getText()) {
            case "Label" -> "Node labels";
            case "Radius" -> "Radius circles";
            case "Line" -> "Path line";
            case "Direction" -> "Flow direction (arrows)";
            case "Profile" -> "Path profile (bottom)";
            case "Nodes" -> "Nodes";
            case "Layer" -> "Preview layer (yellow)";
            default -> part.getText();
        };
    }

    /**
     * Downward triangle marking dropdown buttons. Painted rather than a text glyph
     * such as U+25BE, which WorldPainter's UI font lacks (it showed as an empty
     * box).
     */
    static final Icon DROPDOWN_ARROW = LucideIcons.icon("chevron-down", 14,
            org.ironsight.wpplugin.macromachine.Gui.OverlayStyle.TEXT_MUTED);

    /**
     * Button with a painted dropdown arrow on its right (text glyphs may be missing
     * in the font).
     */
    static class DropdownButton extends JButton
    {
        /*
         * Text/icon and arrow are child labels laid out side by side, so they can never
         * overlap: look and feels differ in whether they honour button margins
         * (painting the arrow into a margin overlapped the text in WorldPainter). The
         * button's own text stays empty; the labels have no mouse listeners, so clicks
         * and rollover still reach the button.
         */
        private final JLabel content = new JLabel();
        private final JLabel arrow = new JLabel(DROPDOWN_ARROW);

        DropdownButton(String text) {
            setLayout(new BorderLayout(6, 0));
            setFocusable(false);
            content.setText(text);
            content.setHorizontalTextPosition(SwingConstants.RIGHT);
            add(content, BorderLayout.CENTER);
            add(arrow, BorderLayout.EAST);
        }

        void setContent(String text, Icon icon) {
            content.setText(text);
            content.setIcon(icon);
            revalidate();
            repaint();
        }

        JLabel contentLabel() {
            return content;
        }

        JLabel arrowLabel() {
            return arrow;
        }

        /** Sized by its labels (plus the look and feel's border / margin insets). */
        @Override
        public java.awt.Dimension getPreferredSize() {
            return getLayout().preferredLayoutSize(this);
        }

        /**
         * Lays its labels out right away: otherwise they keep the layout of an older
         * size or font until the next validation pass and show clipped text.
         */
        @Override
        public void setBounds(int x, int y, int width, int height) {
            super.setBounds(x, y, width, height);
            doLayout();
        }
    }

    /** Overlay visibility (Lucide eye). */
    static Icon overlaysIcon() {
        return LucideIcons.icon("eye", 16, org.ironsight.wpplugin.macromachine.Gui.OverlayStyle.TEXT);
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
        // the tool settings panel stays empty: everything lives in the viewport
        // (Node / Segment / Path menus, overlays) and the path settings dialog
        pathSettingsPanel.setLayout(new BoxLayout(pathSettingsPanel, BoxLayout.Y_AXIS));
        pathSettingsPanel.setBorder(new EmptyBorder(8, 8, 8, 8));
        // overlay switches live in the viewport (see PathOverlay); the yellow layer
        // follows both its own switch and the master switch
        showLayer.getModel().addItemListener(event -> refreshPreview());
        showOverlays.getModel().addItemListener(event -> refreshPreview());
        {
            applyButton = new JButton("Apply");
            applyButton.setName("PathTool.Apply");
            applyButton.setToolTipText("apply the path to the map.");
            applyButton.addActionListener((e) -> {
                var dim = getDimension();
                if (dim == null)
                    return;
                invalidatePreview();
                clearPreview();
                boolean inhibited = dim.isEventsInhibited();
                if (!inhibited)
                    dim.setEventsInhibited(true);
                try {
                    cachedTiles.clear();
                    OnSmoothPath(copyPath(getCurrentPath()), false);
                    previewMask = Map.of();
                    previewDimension = null;
                } finally {
                    cachedTiles.clear();
                    if (!inhibited)
                        dim.setEventsInhibited(false);
                }
                refreshPreview();
                settingsAtOpen = currentSettings();
                overlay.showTransientInfoText("Path applied", 1800);
            });
            // not a settings row: the dialog keeps it bottom right (see
            // openPathSettings)
        }

        {
            onlyDownCheckbox = new JCheckBox("onlyDownCheckbox");
            onlyDownCheckbox.setToolTipText(
                    "The path never rises: every position is at most as high as the one before it, so the river flows downhill or level");
            onlyDownCheckbox.addActionListener(l -> {
                this.onlyDown = onlyDownCheckbox.isSelected();
                settingChanged();
            });
            this.onlyDown = onlyDownCheckbox.isSelected();
            downhillStatus = new JLabel();
            downhillStatus.setName("PathTool.DownhillStatus");
            downhillStatus.setBorder(new EmptyBorder(0, 4, 0, 0));
            onlyDownRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            onlyDownRow.add(onlyDownCheckbox);
            onlyDownRow.add(downhillStatus);
            pathSettingsPanel.add(onlyDownRow);
        }
        {
            minCheckbox = new JCheckBox("Snap to terrain");
            minCheckbox.setToolTipText(
                    "Every position in the path follows the terrain height, even if the path would float below or above the terrain");
            minCheckbox.addActionListener(l -> {
                this.snapToTerrain = minCheckbox.isSelected();
                settingChanged();
            });
            this.snapToTerrain = minCheckbox.isSelected();
            pathSettingsPanel.add(minCheckbox);
        }
        {
            roadPaintRow = new OutputPaintRow("Set paint",
                    "Paints the road (inside the path radius) with the chosen output and value", this::refreshOutputs);
            roadPaintRow.set(OutputPaint.PATH);
            roadPaintRow.onChange(() -> {
                usePaint = roadPaintRow.get().enabled();
                settingChanged();
            });
            usePaint = roadPaintRow.get().enabled();
            pathSettingsPanel.add(roadPaintRow);
        }
        {
            setWaterCheckbox = new JCheckBox("Set water", waterMode != WaterMode.DONT_SET_WATER);
            setWaterCheckbox.setToolTipText("Sets the water level to the path height inside the path radius");
            setWaterCheckbox.addActionListener(l -> {
                waterMode = setWaterCheckbox.isSelected() ? WaterMode.SET_WATER : WaterMode.DONT_SET_WATER;
                settingChanged();
            });
            pathSettingsPanel.add(setWaterCheckbox);
        }
        {
            flattenRoadCheckbox = new JCheckBox("Flatten road", terrainMode == TerrainMode.FLATTEN);
            flattenRoadCheckbox.setToolTipText(
                    "The ground under the road is set to the road height and blends into the terrain around it");
            flattenRoadCheckbox.addActionListener(l -> {
                terrainMode = flattenRoadCheckbox.isSelected() ? TerrainMode.FLATTEN : TerrainMode.DONT_CHANGE;
                settingChanged();
            });
            pathSettingsPanel.add(flattenRoadCheckbox);

            carveRiverCheckbox = new JCheckBox("Carve river bed", terrainMode == TerrainMode.CARVE);
            carveRiverCheckbox.setToolTipText(
                    "Digs a bed below the water: one block deep at the banks, deeper towards the middle, blending into the terrain around it");
            carveRiverCheckbox.addActionListener(l -> {
                terrainMode = carveRiverCheckbox.isSelected() ? TerrainMode.CARVE : TerrainMode.DONT_CHANGE;
                refreshCrossSection();
                settingChanged();
            });
            pathSettingsPanel.add(carveRiverCheckbox);

            riverbedPaintRow = new OutputPaintRow("Paint riverbed with",
                    "Paints the riverbed (inside the path radius) with the chosen output and value",
                    this::refreshOutputs);
            riverbedPaintRow.set(OutputPaint.RIVERBED);
            riverbedPaintRow.onChange(this::settingChanged);
            pathSettingsPanel.add(riverbedPaintRow);
            riverTransitionPaintRow = new OutputPaintRow("Paint transition with",
                    "Paints the banks (the blend around the riverbed) with the chosen output and value",
                    this::refreshOutputs);
            riverTransitionPaintRow.set(OutputPaint.RIVER_TRANSITION);
            riverTransitionPaintRow.onChange(this::settingChanged);
            pathSettingsPanel.add(riverTransitionPaintRow);

            pathPaintRow = new OutputPaintRow("Paint with",
                    "Paints the path (inside its radius) with this output and value", this::refreshOutputs);
            pathPaintRow.labelOnly(); // Apply as paint always paints
            pathPaintRow.set(OutputPaint.PATH);
            pathPaintRow.onChange(this::settingChanged);
            pathSettingsPanel.add(pathPaintRow);
        }
        {
            fixHeightCheckbox = new JCheckBox("fixHeightCheckbox");
            fixHeightCheckbox.setToolTipText("The whole path stays at the given height");
            fixHeightCheckbox.addActionListener(l -> {
                this.fixHeightTo = fixHeightCheckbox.isSelected();
                settingChanged();
            });
            this.fixHeightTo = fixHeightCheckbox.isSelected();
            // pathSettingsPanel.add(fixHeightCheckbox); // FIXME: disabled because it
            // causes
            // wierd artifacts across tile lines
        }
        {
            handleFactorSpinner = new JSpinner(new SpinnerNumberModel(handleStrength, -1d, 2d, 0.1d));
            handleFactorSpinner.setToolTipText(
                    "Controls how curve the path is. 0 = straight lines, 1 = normal curves, 2 = exaggerated curves, -1 = cursive");
            handleFactorSpinner.addChangeListener(l -> {
                handleStrength = ((Number) handleFactorSpinner.getValue()).floatValue();
                overlay.repaint();
                schedulePreview(); // the path's shape, not an Apply setting
            });
            // not in the Apply dialogs: the curve strength is edited on the map (curve
            // handles, typed values); this spinner only holds it
        }
        {
            limitSlopeSpinner = new JSpinner(new SpinnerNumberModel(0d, 0d, 100d, 1d));
            limitSlopeSpinner.setToolTipText(
                    "Limits the allowed slope to x block vertical per 16 blocks horizontal. 0 to disable.");
            limitSlopeSpinner.addChangeListener(l -> {
                slopeLimit = ((Number) limitSlopeSpinner.getValue()).floatValue();
                if (slopePreview != null)
                    slopePreview.repaint();
                settingChanged();
            });
            slopeLimit = ((Number) limitSlopeSpinner.getValue()).floatValue();
            JPanel panel = new JPanel();
            panel.add(new JLabel("Limit slope to x/16 blocks"));
            panel.add(limitSlopeSpinner);
            pathSettingsPanel.add(panel);
            roadOnlyRows.add(panel);

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
            pathSettingsPanel.add(slopePreview);
            roadOnlyRows.add(slopePreview);
        }
        {
            transitionMultiSpinner = new JSpinner(new SpinnerNumberModel(1d, 0d, 100d, .25d));
            transitionMultiSpinner.setToolTipText(
                    "<html>How far beyond the path the terrain blends into it, in path radii.<br>0 = a hard edge, 1 = the blend reaches one path radius beyond the edge, 5 = five radii.</html>");
            transitionMultiSpinner.addChangeListener(l -> {
                transitionMultiplier = ((Number) transitionMultiSpinner.getValue()).floatValue() + 1;
                refreshCrossSection();
                settingChanged();
            });
            transitionMultiSpinner.setValue(1d);
            JPanel panel = new JPanel();
            var label = new JLabel("Transition multiplier");
            label.setToolTipText(transitionMultiSpinner.getToolTipText());
            panel.add(label);
            panel.add(transitionMultiSpinner);
            pathSettingsPanel.add(panel);
        }
        {
            crossSectionView.setName("PathTool.CrossSection");
            crossSectionView.setSubsurfaceColor(CROSS_SECTION_DIRT);
            crossSectionView.setWaterColor(CROSS_SECTION_WATER);
            crossSectionView.setSkyColor(CROSS_SECTION_SKY);
            pathSettingsPanel.add(crossSectionView);
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
                refreshCrossSection();
                settingChanged();
            });
            profilesDropdown.setSelectedItem(model.getElementAt(3));
            this.brushProfile = (CrossSectionShape) profilesDropdown.getSelectedItem();
            profilesDropdown.setToolTipText(
                    "<html>The shape of the blend from the path edge out into the terrain:<br>Triangle: an even slope<br>Sinus: a smooth S-curve, soft at both ends<br>Square Root / Logarithmic: stays near the path height, then drops steeply at the outer end<br>Quadratic / Cubic: drops steeply right at the path, then runs out flat into the terrain</html>");
            JPanel panel = new JPanel();
            var label = new JLabel("Transition profile");
            label.setToolTipText(profilesDropdown.getToolTipText());
            panel.add(label);
            panel.add(profilesDropdown);
            pathSettingsPanel.add(panel);
        }

        // three groups: the tool's own options (River / Road), "Paint path" and
        // "Blend with terrain"
        var blendRows = Set.of(transitionMultiSpinner.getParent(), crossSectionView, profilesDropdown.getParent());
        var paintRows = Set.<Component>of(roadPaintRow, riverbedPaintRow, riverTransitionPaintRow, pathPaintRow);
        for (var row : pathSettingsPanel.getComponents()) {
            pathSettingsPanel.remove(row);
            (blendRows.contains(row) ? blendGroup : paintRows.contains(row) ? paintGroup : optionsGroup).add(row);
        }
        for (var group : settingsGroups()) {
            group.setAlignmentX(Component.LEFT_ALIGNMENT);
            pathSettingsPanel.add(group);
            // one left edge for every row: BoxLayout misplaces rows when checkboxes
            // (left) and panels (centre) are mixed
            for (var row : group.getComponents()) {
                ((JComponent) row).setAlignmentX(Component.LEFT_ALIGNMENT);
                if (row instanceof JPanel panel && panel.getLayout() instanceof FlowLayout flow)
                    flow.setAlignment(FlowLayout.LEFT);
            }
        }
        updateCheckboxTexts();
        controlsReady = true;
    }

    /** Pushes {@code settings} into the controls (and the fields behind them). */
    private void showSettings(ApplySettings settings) {
        loadingSettings = true;
        try {
            // a road neither runs only downhill nor sets water (not in its dialog)
            boolean river = settingsKind == SettingsKind.RIVER;
            // Apply as paint changes no heights, terrain or water and has no transition
            boolean paintOnly = settingsKind == SettingsKind.PAINT;
            onlyDown = river && settings.onlyDown();
            snapToTerrain = !paintOnly && settings.snapToTerrain();
            usePaint = !paintOnly && settings.usePaint();
            waterMode = river && settings.setWater() ? WaterMode.SET_WATER : WaterMode.DONT_SET_WATER;
            terrainMode = paintOnly || !settings.changeTerrain()
                    ? TerrainMode.DONT_CHANGE
                    : settingsKind == SettingsKind.RIVER ? TerrainMode.CARVE : TerrainMode.FLATTEN;
            onlyDownCheckbox.setSelected(onlyDown);
            minCheckbox.setSelected(snapToTerrain);
            setWaterCheckbox.setSelected(waterMode != WaterMode.DONT_SET_WATER);
            flattenRoadCheckbox.setSelected(terrainMode == TerrainMode.FLATTEN);
            carveRiverCheckbox.setSelected(terrainMode == TerrainMode.CARVE);
            transitionMultiSpinner.setValue(paintOnly ? 0d : (double) settings.transition());
            limitSlopeSpinner.setValue((double) settings.slopeLimit());
            if (settings.profile() >= 0 && settings.profile() < profilesDropdown.getItemCount())
                profilesDropdown.setSelectedIndex(settings.profile());
            riverbedPaintRow.set(settings.riverbed());
            riverTransitionPaintRow.set(settings.riverTransition());
            pathPaintRow.set(settings.paint());
            roadPaintRow.set(new OutputPaint(usePaint, settings.paint().output(), settings.paint().value()));
        } finally {
            loadingSettings = false;
        }
        updateCheckboxTexts();
        schedulePreview();
    }

    /** The settings the controls show now. */
    ApplySettings currentSettings() {
        return new ApplySettings(onlyDown, snapToTerrain, usePaint, waterMode != WaterMode.DONT_SET_WATER,
                terrainMode != TerrainMode.DONT_CHANGE, ((Number) transitionMultiSpinner.getValue()).floatValue(),
                slopeLimit, profilesDropdown.getSelectedIndex(), riverbedPaintRow.get(), riverTransitionPaintRow.get(),
                settingsKind == SettingsKind.ROAD ? roadPaintRow.get() : pathPaintRow.get());
    }

    private static AttributeKey<ArrayList<Float>> settingsKey(SettingsKind kind) {
        return switch (kind) {
            case ROAD -> ROAD_SETTINGS;
            case RIVER -> RIVER_SETTINGS;
            case PAINT -> PAINT_SETTINGS;
        };
    }

    /** Where the paints of {@code kind} are stored. */
    private static AttributeKey<ArrayList<Object>> paintsKey(SettingsKind kind) {
        return switch (kind) {
            case ROAD -> ROAD_PAINTS;
            case RIVER -> RIVER_PAINTS;
            case PAINT -> PAINT_PAINTS;
        };
    }

    /** The stored settings of {@code kind} (its preset if none are stored yet). */
    ApplySettings storedSettings(SettingsKind kind) {
        var fallback = switch (kind) {
            case ROAD -> ApplySettings.ROAD;
            case RIVER -> ApplySettings.RIVER;
            case PAINT -> ApplySettings.PAINT;
        };
        Dimension dimension = getView() == null ? null : getDimension();
        if (dimension == null)
            return fallback;
        var settings = ApplySettings.fromFloats(dimension.getAttribute(settingsKey(kind)), fallback);
        return settings.withPaintData(dimension.getAttribute(paintsKey(kind)));
    }

    private void storeSettings() {
        Dimension dimension = getView() == null ? null : getDimension();
        // only edits made in an open Apply dialog belong to its settings
        if (dimension == null || !controlsReady || loadingSettings || pathSettingsDialog == null
                || !pathSettingsDialog.isVisible())
            return;
        var settings = currentSettings();
        var data = settings.toFloats();
        // not undoable: undoing an Apply keeps the settings it was made with
        if (!data.equals(dimension.getAttribute(settingsKey(settingsKind))))
            dimension.setAttribute(settingsKey(settingsKind), data, false);
        var paints = settings.paintData();
        if (!paintDataEquals(paints, dimension.getAttribute(paintsKey(settingsKind))))
            dimension.setAttribute(paintsKey(settingsKind), paints, false);
    }

    /** Equal paint data (the save data are arrays, compared by content). */
    private static boolean paintDataEquals(List<Object> a, List<Object> b) {
        return b != null && java.util.Arrays.deepEquals(a.toArray(), b.toArray());
    }

    /** The river's output pickers list the terrains and this world's layers. */
    private void refreshOutputs() {
        if (getView() != null && getDimension() != null)
            InputOutputProvider.INSTANCE.updateFrom(getDimension());
    }

    /** A setting was edited: remember it for this dialog and update the preview. */
    private void settingChanged() {
        storeSettings();
        refreshCrossSection();
        schedulePreview();
    }

    /** Cancel (or closing the dialog): back to the settings it opened with. */
    void cancelPathSettings() {
        if (settingsAtOpen != null) {
            showSettings(settingsAtOpen);
            storeSettings();
        }
        if (pathSettingsDialog != null)
            pathSettingsDialog.setVisible(false);
    }

    private void updateCheckboxTexts() {
        onlyDownCheckbox.setText("Only downhill");
        fixHeightCheckbox.setText("Fix height to: " + getFixHeight());
    }

    @Override
    public JPanel getOptionsPanel() {
        return optionsPanel;
    }

    /** The generation settings (exposed for tests). */
    JPanel pathSettingsPanel() {
        return pathSettingsPanel;
    }

    /** Apply as road / river: the settings dialog's Apply button (for tests). */
    JButton applyButton() {
        return applyButton;
    }

    JDialog pathSettingsDialog() {
        return pathSettingsDialog;
    }

    /**
     * Road preset into the controls: flatten, paint, slope limit, triangle
     * transition (for screenshots and benchmarks; not stored).
     */
    void applyRoadPreset() {
        settingsKind = SettingsKind.ROAD;
        showSettings(ApplySettings.ROAD);
    }

    /**
     * River preset into the controls: carve, set water, only downhill, wide
     * transition (for screenshots and benchmarks; not stored).
     */
    void applyRiverPreset() {
        settingsKind = SettingsKind.RIVER;
        showSettings(ApplySettings.RIVER);
    }

    /**
     * Opens the settings of Apply as road / river in a non-modal, freely resizable
     * dialog (the map stays usable and its preview follows the settings). A river
     * gets only downhill, snap to terrain, water, carve, riverbed / transition
     * paint and the transition; a road snap to terrain, paint, flatten, the slope
     * limit and the transition.
     */
    void openPathSettings(String title, SettingsKind kind) {
        // each dialog brings back its own stored settings
        settingsKind = kind;
        settingsAtOpen = storedSettings(kind);
        showSettings(settingsAtOpen);
        // Apply as paint shows only its paint; road and river start from every row
        for (var group : settingsGroups())
            for (var row : group.getComponents())
                row.setVisible(kind != SettingsKind.PAINT);
        pathPaintRow.setVisible(kind == SettingsKind.PAINT);
        for (var row : roadOnlyRows)
            row.setVisible(kind == SettingsKind.ROAD);
        flattenRoadCheckbox.setVisible(kind == SettingsKind.ROAD);
        carveRiverCheckbox.setVisible(kind == SettingsKind.RIVER);
        roadPaintRow.setVisible(kind == SettingsKind.ROAD);
        onlyDownRow.setVisible(kind == SettingsKind.RIVER);
        updateDownhillStatus();
        setWaterCheckbox.setVisible(kind == SettingsKind.RIVER);
        riverbedPaintRow.setVisible(kind == SettingsKind.RIVER);
        riverTransitionPaintRow.setVisible(kind == SettingsKind.RIVER);
        crossSectionView.setToolTipText(kind == SettingsKind.RIVER
                ? "Cross section of the river and its transition, one square a block: the terrain blends down to"
                        + " the river, carved out and filled with water on its sand bed. Left: the highest and lowest height."
                : "Cross section of the road and its transition across a hillside, one square a block: the road"
                        + " lies level and the terrain blends into it. Left: the highest and lowest height.");
        refreshCrossSection();
        ((javax.swing.border.TitledBorder) optionsGroup.getBorder())
                .setTitle(kind == SettingsKind.RIVER ? "River" : "Road");
        for (var group : settingsGroups())
            group.setVisible(java.util.Arrays.stream(group.getComponents()).anyMatch(Component::isVisible));
        boolean created = pathSettingsDialog == null;
        if (created) {
            Window owner = overlayView == null ? null : SwingUtilities.getWindowAncestor(overlayView);
            pathSettingsDialog = new JDialog(owner, Dialog.ModalityType.MODELESS);
            pathSettingsDialog.setName("PathTool.PathSettingsDialog");
            pathSettingsDialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            pathSettingsDialog.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent event) {
                    cancelPathSettings();
                }
            });
            pathSettingsDialog.setResizable(true);
            // settings scroll; Apply stays bottom right, outside the scrolling part
            JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            JButton cancel = new JButton("Cancel");
            cancel.setName("PathTool.Cancel");
            cancel.setToolTipText("Close without applying; settings go back to how they were");
            cancel.addActionListener(event -> cancelPathSettings());
            footer.add(cancel);
            footer.add(applyButton);
            JPanel content = new JPanel(new BorderLayout());
            // the scroll bar always takes its place (its thumb shows only when there is
            // something to scroll): one that comes and goes changes the content's width,
            // the cross section's height follows that width and flipped it back and forth
            content.add(new JScrollPane(pathSettingsPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
            content.add(footer, BorderLayout.SOUTH);
            // the dialog's own dark look, whatever WorldPainter's look and feel is
            org.ironsight.wpplugin.macromachine.Gui.DialogStyle.apply(content);
            org.ironsight.wpplugin.macromachine.Gui.DialogStyle.primaryButton(applyButton);
            content.setBackground(org.ironsight.wpplugin.macromachine.Gui.DialogStyle.WINDOW);
            content.setOpaque(true);
            int padding = org.ironsight.wpplugin.macromachine.Gui.DialogStyle.PADDING;
            // the always present scroll bar is part of the right padding
            pathSettingsPanel.setBorder(new EmptyBorder(padding, padding, 0,
                    padding - org.ironsight.wpplugin.macromachine.Gui.DialogStyle.SCROLLBAR_WIDTH));
            footer.setBorder(new EmptyBorder(4, padding - 8, padding - 4, padding - 8)); // FlowLayout adds 8 at the
                                                                                         // edges
            crossSectionView.setForeground(org.ironsight.wpplugin.macromachine.Gui.OverlayStyle.TEXT_MUTED);
            pathSettingsDialog.setContentPane(content);
            pathSettingsDialog.pack();
            if (owner != null)
                pathSettingsDialog.setLocationRelativeTo(owner);
        }
        pathSettingsDialog.setTitle(title);
        pathSettingsPanel.revalidate();
        // the cross section's height follows its width: lay out once to give it the
        // width, then again for the height that width allows
        pathSettingsDialog.validate();
        crossSectionView.invalidate();
        pathSettingsDialog.validate();
        // fit the shown rows, but never shrink a dialog the user made bigger
        var fit = pathSettingsDialog.getPreferredSize();
        var size = created ? fit : pathSettingsDialog.getSize();
        pathSettingsDialog.setSize(Math.max(fit.width, size.width), Math.max(fit.height, size.height));
        if (!pathSettingsDialog.isVisible())
            pathSettingsDialog.setVisible(true);
        pathSettingsDialog.toFront();
    }

    /** "Path" menu: what to build from the path (exposed for tests). */
    JPopupMenu pathMenu() {
        var menu = new JPopupMenu();
        boolean any = getCurrentPath().size() >= 2;
        menu.add(operationItem("New path", "PathTool.NewPath", true, null, this::resetPath));
        menu.add(operationItem("Reverse direction", "PathTool.ReversePath", any, "Place at least two nodes first",
                this::reversePath));
        menu.addSeparator();
        menu.add(operationItem("Apply as road...", "PathTool.Path.ApplyRoad", any, "Place at least two nodes first",
                () -> openPathSettings("Apply as road", SettingsKind.ROAD)));
        menu.add(operationItem("Apply as river...", "PathTool.Path.ApplyRiver", any, "Place at least two nodes first",
                () -> openPathSettings("Apply as river", SettingsKind.RIVER)));
        menu.add(operationItem("Apply as paint...", "PathTool.Path.ApplyPaint", any, "Place at least two nodes first",
                () -> openPathSettings("Apply as paint", SettingsKind.PAINT)));
        menu.addSeparator();
        menu.add(operationItem("Help...", "PathTool.Help", true, null,
                () -> new org.ironsight.wpplugin.macromachine.Gui.HelpDialog(null, HELP_TITLE, HELP_EXPLANATION,
                        HELP_ITEMS).setVisible(true)));
        return menu;
    }

    /**
     * The heights the generated path really gets: snap to terrain, only downhill,
     * fixed height and the slope limit applied to the sampled curve (mutates it).
     * Only Apply uses them: until then the path knows its nodes' heights only.
     */
    void resolveHeights(List<Point4f> path, Dimension dimension) {
        if (snapToTerrain)
            PathToolBackend.forcePathToMinPos(path,
                    point4f -> dimension.getHeightAt(Math.round(point4f.x), Math.round(point4f.y)));
        if (onlyDown)
            PathToolBackend.forcePathOnlyDownhill(path);
        if (fixHeightTo)
            PathToolBackend.forcePathToHeight(path, getFixHeight());
        if (slopeLimit != 0 && settingsKind == SettingsKind.ROAD)
            PathToolBackend.enforceSlopeLimit(path, slopeLimit);
    }

    private float getFixHeight() {
        var currentPath = getCurrentPath();
        return currentPath.isEmpty() ? 62 : currentPath.get(currentPath.size() - 1).z;
    }

    @Override
    protected void brushChanged(Brush newBrush) {
        super.brushChanged(newBrush);
        refreshCrossSection();
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

    /**
     * The first node (index) higher than the node before it, or -1 if the nodes
     * only run downhill or level.
     */
    static int firstRisingNode(List<Point4f> nodes) {
        for (int i = 1; i < nodes.size(); i++)
            if (nodes.get(i).z > nodes.get(i - 1).z)
                return i;
        return -1;
    }

    /** Green check or red x behind "Only downhill": do the nodes already comply? */
    void updateDownhillStatus() {
        if (downhillStatus == null)
            return;
        var nodes = getCurrentPath();
        if (nodes.size() < 2) {
            downhillStatus.setIcon(null);
            downhillStatus.setToolTipText(null);
            return;
        }
        int rising = firstRisingNode(nodes);
        if (rising < 0) {
            downhillStatus.setIcon(LucideIcons.icon("check", 16, STATUS_OK));
            downhillStatus.setToolTipText(
                    "The nodes already run only downhill (or level): " + "'Only downhill' changes nothing.");
        } else {
            float rise = nodes.get(rising).z - nodes.get(rising - 1).z;
            downhillStatus.setIcon(LucideIcons.icon("x", 16, STATUS_BAD));
            downhillStatus.setToolTipText(String.format(Locale.ROOT,
                    "Node %d is %s higher than node %d: the nodes run uphill there. "
                            + "'Only downhill' lowers the river so it never flows uphill.",
                    rising + 1, rise == Math.round(rise) ? Math.round(rise) + " blocks" : rise + " blocks", rising));
        }
    }

    /** The downhill status label (for tests). */
    JLabel downhillStatus() {
        return downhillStatus;
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

    /**
     * Reverses the node order (flow direction); selected nodes stay selected. Goes
     * through the normal edit path, so it refreshes the preview and can be undone.
     */
    void reversePath() {
        var path = getCurrentPath();
        if (path.size() < 2)
            return;
        cancelGesture();
        java.util.Collections.reverse(path);
        int last = path.size() - 1;
        var remapped = new LinkedHashSet<Integer>();
        for (int index : selectedNodes)
            remapped.add(last - index);
        selectedNodes.clear();
        selectedNodes.addAll(remapped);
        completeEdit(path);
        overlay.showTransientInfoText("Path direction reversed", 1800);
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
        if (!controlsReady)
            return;
        invalidatePreview();
        if (previewQueued)
            return;
        previewQueued = true;
        long scheduled = ++scheduleRevision;
        SwingUtilities.invokeLater(() -> {
            if (!previewQueued || scheduleRevision != scheduled)
                return;
            previewQueued = false;
            if (isActive() && getDimension() != null)
                refreshPreview();
        });
    }

    private void refreshPreview() {
        refreshCrossSection();
        updateDownhillStatus();
        previewQueued = false;
        scheduleRevision++;
        invalidatePreview();
        Dimension dimension = getDimension();
        if (dimension == null || previewWorker == null)
            return;
        var path = getCurrentPath();
        if (!shows(showLayer) || path.isEmpty()) {
            clearPreview();
            return;
        }
        long started = measurePreview ? System.nanoTime() : 0;
        var sampledPath = getPathFromNodes(path, handleStrength).path;
        forceRadiusAtLeast(sampledPath, .5f);
        var snapshot = PathPreviewMask.snapshot(sampledPath);
        long sampled = measurePreview ? System.nanoTime() : 0;
        var candidates = collectTilesAroundPath(sampledPath, transitionMultiplier);
        var existing = new ArrayList<PathPreviewMask.TilePosition>();
        for (var tile : candidates)
            if (dimension.getTile(tile.x, tile.y) != null)
                existing.add(new PathPreviewMask.TilePosition(tile.x, tile.y));
        long collected = measurePreview ? System.nanoTime() : 0;
        long revision = previewRevision;
        float strength = handleStrength, multiplier = transitionMultiplier;
        CrossSectionShape profile = brushProfile;
        previewWorker.submit(snapshot, existing, multiplier, profile, masks -> {
            if (previewRevision != revision || getDimension() != dimension || !shows(showLayer)
                    || !getCurrentPath().equals(path) || Float.compare(handleStrength, strength) != 0
                    || Float.compare(transitionMultiplier, multiplier) != 0 || brushProfile != profile)
                return;
            long published = measurePreview ? System.nanoTime() : 0;
            publishPreview(dimension, masks);
            if (measurePreview)
                previewTiming = new PreviewTiming(snapshot.size(), candidates.size(), existing.size(),
                        sampled - started, collected - sampled, published - collected, System.nanoTime() - published);
        });
    }

    private void invalidatePreview() {
        previewRevision++;
        if (previewWorker != null)
            previewWorker.invalidate();
    }

    private void clearPreview() {
        if (previewDimension != null)
            publishPreview(previewDimension, Map.of());
    }

    private void publishPreview(Dimension dimension, Map<PathPreviewMask.TilePosition, BitSet> masks) {
        if (previewDimension != null && previewDimension != dimension)
            clearPreview();
        boolean inhibited = dimension.isEventsInhibited();
        if (!inhibited)
            dimension.setEventsInhibited(true);
        try {
            if (previewLayerUntrusted) {
                dimension.clearLayerData(PreviewOperation.annotationLayer);
                previewMask = Map.of();
                previewLayerUntrusted = false;
            }
            var positions = new java.util.HashSet<>(previewMask.keySet());
            positions.addAll(masks.keySet());
            for (var position : positions) {
                var previous = previewMask.getOrDefault(position, new BitSet());
                var next = masks.getOrDefault(position, new BitSet());
                var changed = (BitSet) previous.clone();
                changed.xor(next);
                if (changed.isEmpty())
                    continue;
                var tile = dimension.getTileForEditing(position.x(), position.y());
                if (tile != null)
                    for (int bit = changed.nextSetBit(0); bit >= 0; bit = changed.nextSetBit(bit + 1))
                        tile.setBitLayerValue(PreviewOperation.annotationLayer, bit % TILE_SIZE, bit / TILE_SIZE,
                                next.get(bit));
            }
            previewMask = masks;
            previewDimension = masks.isEmpty() ? null : dimension;
        } finally {
            if (!inhibited)
                dimension.setEventsInhibited(false);
        }
    }

    @Override
    protected void tick(int centreX, int centreY, boolean inverse, boolean first, float dynamicLevel) {
        if (!inverse || !first || isAltDown() || isCtrlDown() || isShiftDown())
            return;
        // right click is kept free (for a future context menu); it only cancels typing
        if (keyboardTransform)
            cancelKeyboardTransform();
    }

    /**
     * Adds exactly one node at a world position: after the highest selected node,
     * or at the end when nothing is selected; the new node becomes the selection
     * then. Used by Ctrl+click and by clicks in add mode.
     */
    void addNode(int centreX, int centreY) {
        Dimension dim = getDimension();
        if (dim == null || dim.getTile(centreX >> TILE_SIZE_BITS, centreY >> TILE_SIZE_BITS) == null)
            return;
        cancelGesture();
        var pathHandles = getCurrentPath();
        float pathRadius = getBrush() != null
                ? getBrush().getEffectiveRadius()
                : pathHandles.isEmpty() ? 5 : pathHandles.get(pathHandles.size() - 1).w;
        int index = insertionIndex(selectedNodes, pathHandles.size());
        pathHandles.add(index, new Point4f(centreX, centreY, dim.getHeightAt(centreX, centreY), pathRadius));
        boolean inserting = !selectedNodes.isEmpty();
        completeEdit(pathHandles);
        if (inserting) {
            // continue from the new node, so repeated inserts keep their order
            selectedNodes.clear();
            selectedNodes.add(index);
            selectionChanged();
        }
    }

    /**
     * Where a new node goes: right after the selected node with the highest index
     * (inserting into a segment, or appending after the last node), or at the end
     * when nothing is selected. Always exactly one node.
     */
    static int insertionIndex(Set<Integer> selectedNodes, int pathSize) {
        return selectedNodes.stream()
                .filter(index -> index < pathSize)
                .max(Integer::compare)
                .map(index -> index + 1)
                .orElse(pathSize);
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
            refreshPreview();
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
            long started = measurePreview ? System.nanoTime() : 0;
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
            resolveHeights(path, dimension);

            forceRadiusAtLeast(path, .5f); // always at least 1 thick

            long sampled = measurePreview ? System.nanoTime() : 0;

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
            long collected = measurePreview ? System.nanoTime() : 0;
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
            long calculated = measurePreview ? System.nanoTime() : 0;
            // river paints: the bed is where the water goes (inside the radius), the
            // transition the rest
            var riverbed = riverbedPaintRow.get();
            var riverTransition = riverTransitionPaintRow.get();
            boolean paintRiver = !onlyPreview && settingsKind == SettingsKind.RIVER;
            // Apply as paint: the path itself (inside the radius, like a riverbed)
            var pathPaint = pathPaintRow.get();
            boolean paintPath = !onlyPreview && settingsKind == SettingsKind.PAINT && pathPaint.output() != null;
            if (paintRiver)
                for (var paint : List.of(riverbed, riverTransition))
                    if (paint.enabled() && paint.output() != null)
                        paint.output().prepareForDimension(dimension);
            if (paintPath)
                pathPaint.output().prepareForDimension(dimension);
            var roadPaint = roadPaintRow.get();
            boolean paintRoad = !onlyPreview && settingsKind == SettingsKind.ROAD && roadPaint.enabled()
                    && roadPaint.output() != null;
            if (paintRoad)
                roadPaint.output().prepareForDimension(dimension);
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
                if (paintRiver) {
                    var tilePos = new Point3i(wpTile.getX(), wpTile.getY(), 0);
                    if (riverbed.enabled() && riverbed.output() != null)
                        PathToolBackend.writeOutputToDimension(paintOutputMap.get(tilePos), waterHeightMap.get(tilePos),
                                dimension, riverbed.output(), riverbed.value(), true);
                    if (riverTransition.enabled() && riverTransition.output() != null)
                        PathToolBackend.writeOutputToDimension(paintOutputMap.get(tilePos), waterHeightMap.get(tilePos),
                                dimension, riverTransition.output(), riverTransition.value(), false);
                } else if (paintPath) {
                    var tilePos = new Point3i(wpTile.getX(), wpTile.getY(), 0);
                    PathToolBackend.writeOutputToDimension(paintOutputMap.get(tilePos), waterHeightMap.get(tilePos),
                            dimension, pathPaint.output(), pathPaint.value(), true);
                } else if (paintRoad) {
                    var tilePos = new Point3i(wpTile.getX(), wpTile.getY(), 0);
                    PathToolBackend.writeOutputToDimension(paintOutputMap.get(tilePos), waterHeightMap.get(tilePos),
                            dimension, roadPaint.output(), roadPaint.value(), true);
                }
                if (onlyPreview) {
                    var paintTile = paintOutputMap.get(new Point3i(wpTile.getX(), wpTile.getY(), 0));
                    PathToolBackend.writeBinaryLayerDataToDimension(paintTile, wpTile,
                            PreviewOperation.annotationLayer);
                }
            });

            if (measurePreview)
                previewTiming = new PreviewTiming(path.size(), newPathTiles.size(), cachedTiles.size(),
                        sampled - started, collected - sampled, calculated - collected, System.nanoTime() - calculated);
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
        afterUndoOrRedo();
    }

    @Override
    public void redoPerformed() {
        afterUndoOrRedo();
    }

    /**
     * Undo / redo restored tiles (preview bits included) and possibly the path.
     * Nothing is written to the map here, so WorldPainter's redo history stays
     * intact; the next preview update clears the layer completely and redraws it.
     */
    void afterUndoOrRedo() {
        closeOperation();
        cancelGesture();
        invalidatePreview();
        Dimension dimension = getDimension();
        if (dimension != null) {
            previewLayerUntrusted = true;
            previewDimension = dimension; // so clearing on deactivation also clears restored bits
        }
        int size = getCurrentPath().size();
        selectedNodes.removeIf(index -> index >= size);
        overlay.repaint();
    }

    /**
     * Listens to undo / redo of the dimension being edited (only while the tool is
     * active).
     */
    private void observeUndo(Dimension dimension) {
        org.pepsoft.util.undo.UndoManager manager = null;
        if (dimension != null)
            try {
                manager = org.ironsight.wpplugin.macromachine.Layers.CityBuilder.CityEditToolOperation
                        .getUndoManager(dimension);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // no undo manager (e.g. tests): nothing to observe
            }
        if (manager == observedUndoManager)
            return;
        if (observedUndoManager != null)
            observedUndoManager.removeListener(this);
        observedUndoManager = manager;
        if (manager != null)
            manager.addListener(this);
    }

    @Override
    public void bufferChanged(BufferKey<?> key) {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            PathTool pathTool = new PathTool();
            JFrame frame = new JFrame("Path Tool Options");
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
