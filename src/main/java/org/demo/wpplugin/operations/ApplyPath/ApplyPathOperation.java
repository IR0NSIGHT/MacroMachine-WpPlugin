package org.demo.wpplugin.operations.ApplyPath;

import org.demo.wpplugin.geometry.HeightDimension;
import org.demo.wpplugin.geometry.Smoother;
import org.demo.wpplugin.layers.PathPreviewLayer;
import org.demo.wpplugin.operations.EditPath.EditPathOperation;
import org.demo.wpplugin.operations.OptionsLabel;
import org.demo.wpplugin.pathing.Path;
import org.demo.wpplugin.pathing.PathGeometryHelper;
import org.demo.wpplugin.pathing.PathManager;
import org.pepsoft.worldpainter.brushes.Brush;
import org.pepsoft.worldpainter.layers.Annotations;
import org.pepsoft.worldpainter.operations.*;
import org.pepsoft.worldpainter.painting.Paint;
import org.pepsoft.worldpainter.selection.SelectionBlock;

import javax.swing.*;
import java.awt.*;
import java.util.*;

import static org.demo.wpplugin.operations.OptionsLabel.numericInput;
import static org.demo.wpplugin.pathing.PointUtils.pointExtent;

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
public class ApplyPathOperation extends MouseOrTabletOperation implements
        PaintOperation, // Implement this if you need access to the currently selected paint; note that some base
        // classes already provide this
        BrushOperation // Implement this if you need access to the currently selected brush; note that some base
        // classes already provide this
{
    /**
     * The globally unique ID of the operation. It's up to you what to use here. It is not visible to the user. It can
     * be a FQDN or package and class name, like here, or you could use a UUID. As long as it is globally unique.
     */
    static final String ID = "org.demo.wpplugin.ApplyPathOperation.v1";
    /**
     * Human-readable short name of the operation.
     */
    static final String NAME = "Apply Path Operation";
    /**
     * Human-readable description of the operation. This is used e.g. in the tooltip of the operation selection button.
     */
    static final String DESCRIPTION = "Apply path to this world";
    private final ApplyPathOptions options = new ApplyPathOptions(3, 0, 1, 3);
    private final StandardOptionsPanel optionsPanel = new StandardOptionsPanel(getName(), getDescription()) {
        @Override
        protected void addAdditionalComponents(GridBagConstraints constraints) {
            add(new ApplyPathOptionsPanel(options), constraints);
        }
    };
    private Brush brush;
    private Paint paint;

    public ApplyPathOperation() {
        super(NAME, DESCRIPTION, ID);
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

        Brush brush = this.getBrush();
        Paint paint = this.getPaint();
        int transitionFactor = 5;
        Path path = PathManager.instance.getPathBy(EditPathOperation.PATH_ID);
        assert path != null : "Pathmanager delivered null path";

        ArrayList<Point> curve = path.continousCurve(point -> pointExtent(getDimension().getExtent()).contains(point));

        float baseRadius = options.getStartWidth();
        float randomPercent = (float) options.getRandomFluctuate() / 100f;

        Random rand = new Random(420);
        float[] randomEdge = new float[curve.size()];
        float randomWidth = 0;
        for (int i = 0; i < randomEdge.length; i++) {
            randomWidth += ((rand.nextBoolean() ? 1f : -1f) * rand.nextFloat() * 0.3f);
            randomWidth = Math.max(randomWidth, -1);
            randomWidth = Math.min(randomWidth, 1);
            randomEdge[i] = randomWidth;
        }

        double fluctuationSpeed = options.getFluctuationSpeed();
        fluctuationSpeed = Math.max(1, fluctuationSpeed);    //no divide by zero
        double maxRadius = Math.max(options.getFinalWidth(), options.getStartWidth()) * (1 + randomPercent);
        int transitionRadius = (int) (maxRadius * transitionFactor);
        maxRadius += transitionRadius;

        PathGeometryHelper helper = new PathGeometryHelper(path, curve, maxRadius);
        HashMap<Point, Collection<Point>> parentage = helper.getParentage(maxRadius);
        int curveIndex = 0;
        int[] heightProfile = {60, 61, 61, 62, 62, 63, 63, 64, 64, 65, 65};
        for (int i = 0; i < heightProfile.length; i++)
            heightProfile[i] -= 3;
        LinkedList<Point> transitionPoints = new LinkedList<>();
        for (Point curvePoint : curve) {
            Collection<Point> nearby = parentage.get(curvePoint);
            double interpol = curveIndex / (1f * curve.size());
            double baseRadiusAtIdx = interpol * options.getStartWidth() + (1 - interpol) * options.getFinalWidth();
            float randomFluxAtIdx = randomEdge[(int) ((curveIndex) / fluctuationSpeed)];
            final double totalRadiusAtIdx =
                    baseRadiusAtIdx * (1 + randomFluxAtIdx * randomPercent);
            double radiusSq = totalRadiusAtIdx * totalRadiusAtIdx;
            for (Point point : nearby) {
                double distSq = point.distanceSq(curvePoint);

                double distance = Math.sqrt(distSq);

                if (distSq < radiusSq) {
                    //its part of the river profile
                    double profileInterpolate = distance / (1f * totalRadiusAtIdx);
                    if (profileInterpolate < 1) {
                        assert profileInterpolate >= 0 && profileInterpolate < 1;
                        int profileIdx = (int) (profileInterpolate * heightProfile.length);
                        getDimension().setHeightAt(point.x, point.y, heightProfile[profileIdx]);
                    }
                    getDimension().setLayerValueAt(PathPreviewLayer.INSTANCE, point.x, point.y, 12);
                } else if (distSq <= maxRadius * maxRadius) {
                    //its part of the transition

                    distance = point.distance(curvePoint)- totalRadiusAtIdx; //transitionDistance
                    //calculate strength at which new height is applied based on distance
                    float profileInterpolate = (float) (distance / (1f * totalRadiusAtIdx*transitionFactor-1/*verzweiflungswert*/));
                    profileInterpolate = Math.min(1,profileInterpolate);
                    assert profileInterpolate >= 0 && profileInterpolate <= 1;

                    float interpolatedValue =   //interpolate between original terrain height and outermost riverprofile height
                            heightProfile[heightProfile.length-1] * (1 - profileInterpolate) + getDimension().getHeightAt(point) * profileInterpolate;
                    getDimension().setHeightAt(point.x, point.y, interpolatedValue);
                    getDimension().setLayerValueAt(PathPreviewLayer.INSTANCE, point.x, point.y,
                            (int) (profileInterpolate == 1 ? 5 : 7));

                    transitionPoints.add(point);
                }
            }
            curveIndex++;
        }

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

        Smoother smoother = new Smoother(transitionPoints, 25, dim);
        //smoother.smoothGauss();

        this.getDimension().setEventsInhibited(false);
    }

    private void markPoint(Point p) {
        getDimension().setBitLayerValueAt(SelectionBlock.INSTANCE, p.x, p.y, true);
        getDimension().setLayerValueAt(Annotations.INSTANCE, p.x, p.y, 9 /*cyan*/);
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

    private static class ApplyPathOptionsPanel extends OperationOptionsPanel<ApplyPathOptions> {
        public ApplyPathOptionsPanel(ApplyPathOptions panelOptions) {
            super(panelOptions);
        }

        @Override
        protected ArrayList<OptionsLabel> addComponents(ApplyPathOptions options, Runnable onOptionsReconfigured) {
            ArrayList<OptionsLabel> inputs = new ArrayList<>();

            inputs.add(numericInput("final width",
                    "width of the path at the end.",
                    new SpinnerNumberModel(options.getFinalWidth(), 0, 100, 1f),
                    w -> options.setFinalWidth(w.intValue()),
                    onOptionsReconfigured));

            inputs.add(numericInput("start width",
                    "width of the path at start.",
                    new SpinnerNumberModel(options.getStartWidth(), 0, 100, 1f),
                    w -> options.setStartWidth(w.intValue()),
                    onOptionsReconfigured
            ));

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