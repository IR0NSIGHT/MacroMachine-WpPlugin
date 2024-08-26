package org.demo.wpplugin.operations;

import org.pepsoft.worldpainter.Terrain;
import org.pepsoft.worldpainter.brushes.Brush;
import org.pepsoft.worldpainter.layers.PineForest;
import org.pepsoft.worldpainter.operations.*;
import org.pepsoft.worldpainter.painting.Paint;

import java.awt.*;
import java.util.function.Function;

import static org.demo.wpplugin.PointUtils.pointExtent;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE;

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
public class LinearByAngleOperation extends MouseOrTabletOperation implements
        PaintOperation, // Implement this if you need access to the currently selected paint; note that some base classes already provide this
        BrushOperation // Implement this if you need access to the currently selected brush; note that some base classes already provide this
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


    public LinearByAngleOperation() {
        super(NAME, DESCRIPTION, ID);
    }

    public static void main(String[] args) {
        Function<Point, Float> getHeight = p -> (float)0.5* p.y;
        double slope = getSlopeAt(new Point(4, 4), getHeight);
        System.out.println(slope);
    }

    /**
     * returns slope of that point as defined: normal on a plane defined by the surrounding blocks
     * in degrees
     * @param p must be 1 block away from any edge!
     * @param getHeightAt
     * @return
     */
    private static double getSlopeAt(Point p, Function<Point, Float> getHeightAt) {
        int size = 1;
        double[][] heights = new double[size * 2 + 1][];
        for (int y = -size; y <= size; y++) {
            heights[y + size] = new double[size * 2 + 1];
            for (int x = -size; x <= size; x++) {
                heights[y + size][x + size] = getHeightAt.apply(new Point(p.x+x, p.y+y));
            }
        }

        double slope = calculateSlopeInDegrees(heights);
        return slope;
    }

    /**
     * Calculates the slope in degrees at a given point in a 2D array of heights using a 5x5 kernel.
     *
     * @param heights The 2D array of heights (elevation data).
     * @return The slope at the point (x, y) in degrees.
     */
    public static double calculateSlopeInDegrees(double[][] heights) {
        double[][] sobelKernel = {
                {-1,0,1},
                {-2,0,2},
                {-1,0,1}
        };

        double sumHorizontal = 0;
        double sumVertical = 0;
        double sumKernel = 0;
        for (int y = 0; y < sobelKernel.length; y++) {
            for (int x = 0; x < sobelKernel.length; x++) {
                sumHorizontal += heights[y][x] * sobelKernel[y][x];
                sumVertical += heights[y][x] * sobelKernel[x][y];
                sumKernel += Math.abs(sobelKernel[x][y]);
            }
        }

        sumHorizontal /= sumKernel;
        sumVertical /= sumKernel;

        // Calculate the magnitude of the gradient (slope)
        double gradient = Math.sqrt(sumVertical*sumVertical+sumHorizontal*sumHorizontal);

        // Convert slope to degrees
        double slopeInDegrees = Math.toDegrees(Math.atan(gradient));

    /*    gaga
        hhag
        scta
*/
        return slopeInDegrees;
    }

    /**
     * Perform the operation. For single shot operations this is invoked once per mouse-down. For continuous operations
     * this is invoked once per {@code delay} ms while the mouse button is down, with the first invocation having
     * {@code first} be {@code true} and subsequent invocations having it be {@code false}.
     *
     * @param centreX      The x coordinate where the operation should be applied, in world coordinates.
     * @param centreY      The y coordinate where the operation should be applied, in world coordinates.
     * @param inverse      Whether to perform the "inverse" operation instead of the regular operation, if applicable. If the
     *                     operation has no inverse it should just apply the normal operation.
     * @param first        Whether this is the first tick of a continuous operation. For a one shot operation this will always
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
            float minAngle = 10;
            float maxAngle = 90;
            int[] valueByBand = new int[]{1, 2, 4, 8, 12, 6, 3, 0};
            Terrain[] terrainByBand = new Terrain[]{
                    Terrain.GRASS,
                    Terrain.GRASS,
                    Terrain.GRANITE,
                    Terrain.DIORITE,
                    Terrain.COBBLESTONE,
                    Terrain.STONE,
                    Terrain.MOSSY_COBBLESTONE,
                    Terrain.BASALT};
            assert valueByBand.length == terrainByBand.length;
            Rectangle pExt = pointExtent(getDimension().getExtent());

            //1 smaller
            pExt.setBounds(pExt.x+1, pExt.y+1,pExt.width-1, pExt.height-1);
            getDimension().visitTilesForEditing().forSelection().andDo(tile -> {
                for (int x = 0; x < TILE_SIZE; x++) {
                    for (int y = 0; y < TILE_SIZE; y++) {
                        int xWorld, yWorld;
                        xWorld = tile.getX() * TILE_SIZE + x;
                        yWorld = tile.getY() * TILE_SIZE + y;
                        boolean isSelected = true; //getDimension().getBitLayerValueAt(SelectionChunk.INSTANCE, xWorld, yWorld) || getDimension().getBitLayerValueAt(SelectionBlock.INSTANCE, xWorld, yWorld);

                        if (pExt.contains(xWorld,yWorld) && isSelected) {
                            float slope = getDimension().getSlope(xWorld, yWorld);
                            slope = (float) (Math.atan(slope) * 180 / Math.PI);
                            float slopeNormalized = (slope - minAngle) / (maxAngle - minAngle);
                            if (slopeNormalized < 0 || slopeNormalized >= 1)
                                continue;
                            int slopeIdx = (int) (slopeNormalized * valueByBand.length);
                            tile.setLayerValue(PineForest.INSTANCE, x, y, valueByBand[slopeIdx]);
                            tile.setTerrain(x, y, terrainByBand[slopeIdx]);
                        }
                    }
                }
            });
        } catch (Exception e) {
            throw e;
        } finally {
            this.getDimension().setEventsInhibited(false);
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
}