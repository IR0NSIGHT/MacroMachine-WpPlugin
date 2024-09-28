package org.demo.wpplugin.operations.ApplyPath;

import org.demo.wpplugin.geometry.HeightDimension;
import org.demo.wpplugin.geometry.KernelConvolution;
import org.demo.wpplugin.operations.EditPath.EditPathOperation;
import org.demo.wpplugin.operations.OptionsLabel;
import org.demo.wpplugin.operations.River.RiverHandleInformation;
import org.demo.wpplugin.pathing.Path;
import org.demo.wpplugin.pathing.PathGeometryHelper;
import org.demo.wpplugin.pathing.PathManager;
import org.demo.wpplugin.pathing.RingFinder;
import org.pepsoft.worldpainter.operations.*;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.function.Function;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.demo.wpplugin.operations.OptionsLabel.numericInput;
import static org.demo.wpplugin.operations.River.RiverHandleInformation.RiverInformation.*;
import static org.demo.wpplugin.operations.River.RiverHandleInformation.getValue;
import static org.demo.wpplugin.pathing.PointUtils.getPoint2D;

/**
 * For any operation that is intended to be applied to the dimension in a particular location as indicated by the user
 * by clicking or dragging with a mouse or pressing down on a tablet, it makes sense to subclass
 * {@link MouseOrTabletOperation}, which automatically sets that up for you.
 *
 * <p>For more general kinds of operations you are free to subclass {@link AbstractOperation} instead, or even just
 * implement {@link Operation} directly.
 *
 * <p>There are also more specific base classes you can use:
 *
 * <ul>
 *     <li>{@link AbstractBrushOperation} - for operations that need access to the currently selected brush and
 *     intensity setting.
 *     <li>{@link RadiusOperation} - for operations that perform an action in the shape of the brush.
 *     <li>{@link AbstractPaintOperation} - for operations that apply the currently selected paint in the shape of the
 *     brush.
 * </ul>
 *
 * <p><strong>Note</strong> that for now WorldPainter only supports operations that
 */
public class ApplyRiverOperation extends MouseOrTabletOperation {
    /**
     * The globally unique ID of the operation. It's up to you what to use here. It is not visible to the user. It can
     * be a FQDN or package and class name, like here, or you could use a UUID. As long as it is globally unique.
     */
    static final String ID = "org.demo.wpplugin.applyRiverOperation.v1";
    /**
     * Human-readable short name of the operation.
     */
    static final String NAME = "Apply River Operation";
    /**
     * Human-readable description of the operation. This is used e.g. in the tooltip of the operation selection button.
     */
    static final String DESCRIPTION = "<html>Apply river to this world<br>Last selected path gets applied into the " +
            "world.<br>Potentially slow and expensive</html>";
    private final ApplyPathOptions options = new ApplyPathOptions(3, 0, 1, 3);
    private final StandardOptionsPanel optionsPanel = new StandardOptionsPanel(getName(), getDescription()) {
        @Override
        protected void addAdditionalComponents(GridBagConstraints constraints) {
            add(new ApplyPathOptionsPanel(options), constraints);
        }
    };

    public ApplyRiverOperation() {
        super(NAME, DESCRIPTION, ID);
    }

    public static float[] randomEdge(int length) {
        Random rand = new Random(420);
        float[] randomEdge = new float[length];
        float randomWidth = 0;
        for (int i = 0; i < randomEdge.length; i++) {
            randomWidth += ((rand.nextBoolean() ? 1f : -1f) * rand.nextFloat() * 0.3f);
            randomWidth = max(randomWidth, -1);
            randomWidth = min(randomWidth, 1);
            randomEdge[i] = randomWidth;
        }
        return randomEdge;
    }

    /**
     * collect all max values into a single point
     * out[n] = Max(handles[all i][n])
     *
     * @param handles
     * @return
     */
    public static float[] getMaxValues(Iterable<float[]> handles, int handleSize) {
        float[] maxHandleValues = new float[handleSize];
        for (float[] handle : handles) {
            for (int n = RiverHandleInformation.PositionSize.SIZE_2_D.value; n < handle.length; n++) {
                maxHandleValues[n] = max(maxHandleValues[n], handle[n]);
            }
        }
        return maxHandleValues;
    }

    public static void applyRiverPath(Path path, ApplyPathOptions options, HeightDimension dimension,
                                      HeightDimension waterMap) {
        ArrayList<float[]> curve = path.continousCurve(false);
        float randomPercent = (float) options.getRandomFluctuate() / 100f;
        float[] randomEdge = randomEdge(curve.size());

        double fluctuationSpeed = options.getFluctuationSpeed();
        fluctuationSpeed = max(1, fluctuationSpeed);    //no divide by zero

        float[] maxHandleValues = getMaxValues(path, path.type.size);

        double totalSearchRadius =
                getValue(maxHandleValues, RIVER_RADIUS) + getValue(maxHandleValues, BEACH_RADIUS);// + getValue(maxHandleValues, TRANSITION_RADIUS);
        PathGeometryHelper helper = new PathGeometryHelper(path, curve, totalSearchRadius);
        HashMap<Point, Collection<Point>> parentage = helper.getParentage();

        HashMap<Point, Float> finalHeightmap = new HashMap<>();

        Function<float[], Float> riverDepthByDistance = (depthAndDist) -> {
            float depth = depthAndDist[0];
            float dist = depthAndDist[1];
            float maxDist = depthAndDist[2];
            float x = 2 * dist / maxDist;
            return (x * x) - depth;
        };
        float[] waterZs = Path.interpolateWaterZ(curve, dimension);
        for (int curveIdx = 0; curveIdx < curve.size(); curveIdx++) {
            float[] curvePointF = curve.get(curveIdx);
            Point curvePoint = getPoint2D(curvePointF);
            Collection<Point> nearby = parentage.get(curvePoint);

            float randomFluxAtIdx = randomEdge[(int) ((curveIdx) / fluctuationSpeed)];
            double riverRadius = getValue(curvePointF, RIVER_RADIUS) * (1 + randomFluxAtIdx * randomPercent);
            double beachRadius = getValue(curvePointF, BEACH_RADIUS);
            double transitionRadius = getValue(maxHandleValues, TRANSITION_RADIUS); //FIXME has to be fixed at max
            float waterHeight = waterZs[curveIdx];

            // Transition, becuase the gauss smoother can only do one value


            //FIXME: parentage is problematic: clostest point doenst guarentee to be the right parent on rivers that
            // grow fast and are very curvy
            // -> point in the transition layer chose the wrong parent. instead we need to test for the closest point
            // on outermost beach layer!
            for (Point point : nearby) {
                double distance = point.distance(curvePoint);
                if (distance < riverRadius) {
                    finalHeightmap.put(point,
                            waterHeight + riverDepthByDistance.apply(new float[]{getValue(curvePointF, RIVER_DEPTH),
                                    (float) distance, (float) riverRadius}));
                    waterMap.setHeight(point.x, point.y, waterHeight);
                } else if (distance - riverRadius <= beachRadius) {
                    finalHeightmap.put(point, waterHeight);
                    waterMap.setHeight(point.x, point.y, waterHeight);

                }
            }
        }


        float maxTransition = (getValue(maxHandleValues,
                TRANSITION_RADIUS));

        //apply beach and river heights
        for (Point p : finalHeightmap.keySet()) {
            dimension.setHeight(p.x, p.y, finalHeightmap.get(p));
        }

        //add transitions rings
        int amountRings = Math.round(maxTransition);
        RingFinder ringFinder = new RingFinder(new HashSet<>(finalHeightmap.keySet()), amountRings);
        for (int i = 1; i < amountRings; i++) {

            for (Point p: ringFinder.ring(i)) {
                dimension.setHeight(p.x, p.y, 72+2*i);
            }
        }
    }

    public static double angleOf(int x, int y) {
        return Math.atan(1f * y / x);
    }

    /**
     * return v1 if point = min, v2 if point = max, linear interpolate otherwise
     *
     * @param point
     * @param min
     * @param max
     * @param v1
     * @param v2
     * @return
     */
    private static float modifyValue(float point, float min, float max, float v1, float v2) {
        float y = getCubicInterpolation(point, min, max);
        return (1 - y) * v1 + (y) * v2;
    }

    /**
     * @param point
     * @param min
     * @param max
     * @return 1 if point = max, 0 if point = min, else linear interpolate
     */
    private static float getCubicInterpolation(float point, float min, float max) {
        float x = (point - min);
        float width = max - min;
        return x / width;
    }

    @Override
    public JPanel getOptionsPanel() {
        return optionsPanel;
    }

    /**
     * Perform the operation. For single shot operations this is invoked once per mouse-down. For continuous operations
     * this is invoked once per {@code delay} ms while the mouse button is down, with the first invocation having
     * {@code first} be {@code true} and subsequent invocations having it be {@code false}.
     *
     * @param centreX      The x coordinate where the operation should be applied, in world coordinates.
     * @param centreY      The y coordinate where the operation should be applied, in world coordinates.
     * @param inverse      Whether to perform the "inverse" operation instead of the regular operation, if applicable
     *                     . If the
     *                     operation has no inverse it should just apply the normal operation.
     * @param first        Whether this is the first tick of a continuous operation. For a one shot operation this
     *                     will always
     *                     be {@code true}.
     * @param dynamicLevel The dynamic level (from 0.0f to 1.0f inclusive) to apply in addition to the {@code level}
     *                     property, for instance due to a pressure sensitive stylus being used. In other words,
     *                     <strong>not</strong> the total level at which to apply the operation! Operations are free to
     *                     ignore this if it is not applicable. If the operation is being applied through a means which
     *                     doesn't provide a dynamic level (for instance the mouse), this will be <em>exactly</em>
     *                     {@code 1.0f}.
     */
    @Override
    protected void tick(int centreX, int centreY, boolean inverse, boolean first, float dynamicLevel) {
        //  Perform the operation. In addition to the parameters you have the following methods available:
        // * getDimension() - obtain the dimension on which to perform the operation
        // * getLevel() - obtain the current brush intensity setting as a float between 0.0 and 1.0
        // * isAltDown() - whether the Alt key is currently pressed - NOTE: this is already in use to indicate whether
        //                 the operation should be inverted, so should probably not be overloaded
        // * isCtrlDown() - whether any of the Ctrl, Windows or Command keys are currently pressed
        // * isShiftDown() - whether the Shift key is currently pressed
        // In addition you have the following fields in this class:
        // * brush - the currently selected brush
        // * paint - the currently selected paint

        this.getDimension().setEventsInhibited(true);
        try {
            Path path = PathManager.instance.getPathBy(EditPathOperation.PATH_ID);
            assert path != null : "Pathmanager delivered null path";
            HeightDimension dim = new HeightDimension() {
                @Override
                public float getHeight(int x, int y) {
                    return getDimension().getHeightAt(x, y);
                }

                @Override
                public void setHeight(int x, int y, float z) {
                    getDimension().setHeightAt(x, y, z);
                }
            };
            HeightDimension water = new HeightDimension() {
                @Override
                public float getHeight(int x, int y) {
                    return getDimension().getWaterLevelAt(x, y);
                }

                @Override
                public void setHeight(int x, int y, float z) {
                    getDimension().setWaterLevelAt(x, y, Math.round(z));
                }
            };
            applyRiverPath(path, options, dim, water);
        } catch (Exception ex) {
            System.err.println(ex);
        } finally {
            this.getDimension().setEventsInhibited(false);
        }
    }

    private static class ApplyPathOptionsPanel extends OperationOptionsPanel<ApplyPathOptions> {
        public ApplyPathOptionsPanel(ApplyPathOptions panelOptions) {
            super(panelOptions);
        }

        @Override
        protected ArrayList<OptionsLabel> addComponents(ApplyPathOptions options, Runnable onOptionsReconfigured) {
            ArrayList<OptionsLabel> inputs = new ArrayList<>();

            inputs.add(numericInput("random width",
                    "each step the rivers radius will randomly increase or decrease. It will stay within +/- percent " +
                            "of the normal width.",
                    new SpinnerNumberModel(options.getRandomFluctuate(), 0, 100, 1f),
                    w -> options.setRandomFluctuate(w.intValue()),
                    onOptionsReconfigured));

            inputs.add(numericInput("fluctuation speed",
                    "how fast the random fluctuation appears. low number = less extreme change",
                    new SpinnerNumberModel(options.getFluctuationSpeed(), 0, 100, 1f),
                    w -> options.setFluctuationSpeed(w.intValue()),
                    onOptionsReconfigured));

            return inputs;
        }
    }
}