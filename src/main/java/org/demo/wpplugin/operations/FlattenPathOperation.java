package org.demo.wpplugin.operations;

import org.demo.wpplugin.geometry.HeightDimension;
import org.demo.wpplugin.pathing.Path;
import org.pepsoft.worldpainter.brushes.Brush;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.operations.*;
import org.pepsoft.worldpainter.painting.Paint;
import org.pepsoft.worldpainter.selection.SelectionBlock;
import org.pepsoft.worldpainter.selection.SelectionChunk;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Function;

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
public class FlattenPathOperation extends MouseOrTabletOperation implements
        PaintOperation, // Implement this if you need access to the currently selected paint; note that some base
        // classes already provide this
        BrushOperation // Implement this if you need access to the currently selected brush; note that some base
        // classes already provide this
{

    /**
     * The globally unique ID of the operation. It's up to you what to use here. It is not visible to the user. It can
     * be a FQDN or package and class name, like here, or you could use a UUID. As long as it is globally unique.
     */
    static final String ID = "org.demo.wpplugin.FlattenPathOperation.v1";
    /**
     * Human-readable short name of the operation.
     */
    static final String NAME = "Flatten Path Tool";
    /**
     * Human-readable description of the operation. This is used e.g. in the tooltip of the operation selection button.
     */
    static final String DESCRIPTION = "Make the path wide and flat, similar to a road";

    private Brush brush;
    private Paint paint;


    public FlattenPathOperation() {
        // Using this constructor will create a "single shot" operation. The tick() method below will only be invoked
        // once for every time the user clicks the mouse or presses on the tablet:
        super(NAME, DESCRIPTION, ID);
        // Using this constructor instead will create a continues operation. The tick() method will be invoked once
        // every "delay" ms while the user has the mouse button down or continues pressing on the tablet. The "first"
        // parameter will be true for the first invocation per mouse button press and false for every subsequent
        // invocation:
        // super(NAME, DESCRIPTION, delay, ID);
    }

    public static void main(String[] args) {
        float[] heights = new float[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 3, 0, 0, 0, 0, 0, 5, 10, 17, 25, 28, 28,
                28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28,};
        float[] diffs = findMaxDifference(heights);
        while (diffs[1] > 1) {
            heights = applyMeanFilter(heights);
            diffs = findMaxDifference(heights);
        }
        heights = applyMinFilter(heights, 5);
        float[] smooth = applyMinFilter(heights, 5);
    }

    /**
     * Calculates the difference between each neighboring float in the input array and returns the index
     * and the maximum difference.
     *
     * @param input The input array of floats.
     * @return An array where the first element is the index of the maximum difference and the second element is the
     * maximum difference.
     */
    public static float[] findMaxDifference(float[] input) {
        if (input == null || input.length < 2) {
            throw new IllegalArgumentException("Input array must have at least two elements.");
        }

        int maxDiffIndex = 0;
        float maxDiff = 0;

        for (int i = 0; i < input.length - 1; i++) {
            // Calculate the absolute difference between neighboring elements
            float diff = Math.abs(input[i + 1] - input[i]);

            // Update maxDiff and maxDiffIndex if a larger difference is found
            if (diff > maxDiff) {
                maxDiff = diff;
                maxDiffIndex = i;
            }
        }

        // Return both the index and the maximum difference
        return new float[]{maxDiffIndex, maxDiff};
    }

    static float[] applyMeanFilter(float[] heights) {
        float[] smoothedHeight = heights.clone();

        float[] gaussKernel = new float[]{0.003f, 0.023f, 0.093f, 0.231f, 0.306f, 0.231f, 0.093f, 0.023f, 0.003f};

        float[] kernel = new float[50];
        Arrays.fill(kernel, 1);

        kernel = gaussKernel;
        float kernelSum = 0;
        for (Float f : kernel)
            kernelSum += f;

        for (int i = 0; i < heights.length - kernel.length; i++) {
            float sum = 0;
            for (int y = 0; y < kernel.length; y++) {
                sum += kernel[y] * heights[i + y];
            }
            sum /= kernelSum;
            smoothedHeight[i + (kernel.length / 2)] = sum;
        }

        return smoothedHeight;
    }

    /**
     * Applies a minimum filter to a float array.
     *
     * @param input      The input array of floats to be filtered.
     * @param windowSize The size of the min filter window (should be odd).
     * @return A new float array containing the filtered values.
     */
    public static float[] applyMinFilter(float[] input, int windowSize) {
        if (windowSize <= 0 || windowSize % 2 == 0) {
            throw new IllegalArgumentException("Window size must be a positive odd integer.");
        }

        int n = input.length;
        float[] output = new float[n];
        int halfWindow = windowSize / 2;

        for (int i = 0; i < n; i++) {
            float minValue = Float.MAX_VALUE; // Initialize to a very large value

            // Collect values for the current window and find the minimum
            for (int j = -halfWindow; j <= halfWindow; j++) {
                int index = i + j;
                if (index >= 0 && index < n) {
                    minValue = Math.min(minValue, input[index]);
                }
            }

            output[i] = minValue; // Assign the minimum value to the output
        }

        return output;
    }

    /**
     * Applies a median filter to a float array.
     *
     * @param input      The input array of floats to be filtered.
     * @param windowSize The size of the median filter window (should be odd).
     * @return A new float array containing the filtered values.
     */
    public static float[] applyMedianFilter(float[] input, int windowSize) {
        if (windowSize <= 0 || windowSize % 2 == 0) {
            throw new IllegalArgumentException("Window size must be a positive odd integer.");
        }

        int n = input.length;
        float[] output = new float[n];
        int halfWindow = windowSize / 2;

        for (int i = 0; i < n; i++) {
            float[] window = new float[windowSize];
            int count = 0;

            // Collect values for the current window
            for (int j = -halfWindow; j <= halfWindow; j++) {
                int index = i + j;
                if (index >= 0 && index < n) {
                    window[count++] = input[index];
                }
            }

            // Sort the window and find the median
            Arrays.sort(window, 0, count);
            output[i] = window[count / 2];
        }

        return output;
    }

    static float[] getPathHeight(Point[] curve, Function<Point, Float> getHeight) {
        float[] heights = new float[curve.length];
        int i = 0;
        for (Point p : curve)
            heights[i++] = getHeight.apply(p);

        return heights;
    }

    static float[] applyDownslopeFilter(float[] heights) {
        float[] smoothedHeight = heights.clone();
        float previousHeight = Float.MAX_VALUE;
        for (int i = 0; i < heights.length; i++) {
            float newHeight = Math.min(heights[i], previousHeight);
            previousHeight = newHeight;
            smoothedHeight[i] = newHeight;
        }

        return smoothedHeight;
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

            HeightDimension dim = new HeightDimension() {
                @Override
                public float getHeight(int x, int y) {
                    return getDimension().getHeightAt(x, y);
                }

                @Override
                public void setHeight(int x, int y, float z) {
                    if (getDimension().getHeightAt(x, y) != z) {
                        if (getDimension().getBitLayerValueAt(SelectionBlock.INSTANCE, x, y) || getDimension().getBitLayerValueAt(SelectionChunk.INSTANCE, x, y)) {
                            getDimension().setHeightAt(x, y, z);
                        }
                        //    getDimension().setLayerValueAt(PathPreviewLayer.INSTANCE, x, y, DemoLayerRenderer.RED);
                    }
                }
            };

            int startX = Integer.MAX_VALUE, startY = Integer.MAX_VALUE, endX = Integer.MIN_VALUE, endY =
                    Integer.MIN_VALUE;
            int selected = 0;
            for (int x = getDimension().getLowestX(); x < getDimension().getHighestX(); x++) {
                for (int y = getDimension().getLowestY(); y < getDimension().getHighestY(); y++) {
                    if (getDimension().getTile(x, y).hasLayer(SelectionBlock.INSTANCE) || getDimension().getTile(x,
                            y).hasLayer(SelectionChunk.INSTANCE)) {
                        startX = Math.min(startX, x);
                        startY = Math.min(startY, y);
                        endX = Math.max(endX, x);
                        endY = Math.max(endY, y);
                        selected++;
                    }
                }
            }

            /*

            int pathWidth = 2;
            int transitionDist = 10;
            int totalRadius = pathWidth + transitionDist;
            float maxHeightDiff = 0.7f;
            Path path = PathManager.instance.getPathBy(PATH_ID);

            HashSet<Point> seen = new HashSet<>();
            ArrayList<float[]> curve = path.continousCurve();
            LinkedList<Point> edge = new LinkedList<>();
            int totalRadiusSq = totalRadius * totalRadius;
            //collect all points within rough radius
            for (float[] pF : curve) {
                Point p = getPoint2D(pF);
                for (int x = -totalRadius; x < totalRadius; x++) {
                    for (int y = -totalRadius; y < totalRadius; y++) {
                        Point edgePoint = new Point(p.x + x, p.y + y);
                        if (edgePoint.distanceSq(p) > totalRadiusSq)
                            continue;
                        if (seen.contains(edgePoint))
                            continue;
                        seen.add(edgePoint);
                        edge.add(edgePoint);
                    }
                }
            }

            float[] curveHeights = getPathHeight(curve.toArray(new Point[0]), p -> getDimension().getHeightAt(p));
            curveHeights = applyMedianFilter(curveHeights, 11);
            float[] diffs = findMaxDifference(curveHeights);
            int safety = 0;
            while (diffs[1] > 0.5f && safety < 100) {
                curveHeights = applyMeanFilter(curveHeights);
                diffs = findMaxDifference(curveHeights);
                safety++;
            }
            curveHeights = applyMinFilter(curveHeights, 5);

            //curveHeights = applyDownslopeFilter(curveHeights);
            for (Point e : edge) {
                int curveIdx = getClosestPointIndexOnCurveTo(curve, e);
                Point closestCurvePoint = getPoint2D(curve.get(curveIdx));

                float interpolatedHeight = getHeightByDistanceToCurve(e.distance(closestCurvePoint),
                        getDimension().getHeightAt(e),
                        curveHeights[curveIdx], pathWidth, transitionDist
                );

              // if (closestCurvePoint.distance(e) < pathWidth - 1) {
              //     //water
              //     getDimension().setHeightAt(e, interpolatedHeight - 2);
              //     getDimension().setWaterLevelAt(e.x, e.y, (int) interpolatedHeight);

              // } else {
                    //edge
                    getDimension().setHeightAt(e, interpolatedHeight);
               // }
            }

*/
        } catch (Exception e) {
            throw e;
        } finally {
            this.getDimension().setEventsInhibited(false);

        }

    }

    private int getClosestPointIndexOnCurveTo(ArrayList<float[]> curve, Point nearby) {
        assert !curve.isEmpty() : "can not get point from empty curve";
        Point closest = null;
        int closestIdx = -1;
        double minDistSq = Double.MAX_VALUE;
        int i = 0;
        for (float[] pF : curve) {
            Point p = getPoint2D(pF);
            double thisDistSq = p.distanceSq(nearby);
            if (thisDistSq < minDistSq) {
                closest = p;
                minDistSq = thisDistSq;
                closestIdx = i;
            }
            i++;
        }
        return closestIdx;
    }

    float getHeightByDistanceToCurve(double distance, float ownHeight, float curveHeight, float pathWidth,
                                     float transition) {
        if (distance < pathWidth) {
            return curveHeight;
        } else if (distance < (transition + pathWidth)) {
            //interpolate
            double interpolatePoint = distance - pathWidth;
            assert interpolatePoint >= 0 && interpolatePoint < transition :
                    "not in [0,1]: interpolation point = " + interpolatePoint;
            double factor = interpolatePoint / transition;
            assert factor >= 0 && factor <= 1;

            double mixed = factor * ownHeight + (1 - factor) * curveHeight;
            return (float) mixed;
        } else
            return ownHeight;
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
}