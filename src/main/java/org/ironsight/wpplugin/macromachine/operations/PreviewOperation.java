package org.ironsight.wpplugin.macromachine.operations;

import org.ironsight.wpplugin.macromachine.Gui.GlobalActionPanel;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Terrain;
import org.pepsoft.worldpainter.brushes.Brush;
import org.pepsoft.worldpainter.operations.BrushOperation;
import org.pepsoft.worldpainter.operations.MouseOrTabletOperation;
import org.pepsoft.worldpainter.operations.PaintOperation;
import org.pepsoft.worldpainter.painting.Paint;

import javax.swing.*;

public class PreviewOperation extends MouseOrTabletOperation implements
        PaintOperation, // Implement this if you need access to the currently selected paint; note that some base classes already provide this
        BrushOperation // Implement this if you need access to the currently selected brush; note that some base classes already provide this
{
    public PreviewOperation() {
        // Using this constructor will create a "single shot" operation. The tick() method below will only be invoked
        // once for every time the user clicks the mouse or presses on the tablet:
        super(NAME, DESCRIPTION, ID);
        // Using this constructor instead will create a continues operation. The tick() method will be invoked once
        // every "delay" ms while the user has the mouse button down or continues pressing on the tablet. The "first"
        // parameter will be true for the first invocation per mouse button press and false for every subsequent
        // invocation:
        // super(NAME, DESCRIPTION, delay, ID);
    }

    /**
     * Perform the operation. For single shot operations this is invoked once per mouse-down. For continuous operations
     * this is invoked once per {@code delay} ms while the mouse button is down, with the first invocation having
     * {@code first} be {@code true} and subsequent invocations having it be {@code false}.
     *
     * @param centreX The x coordinate where the operation should be applied, in world coordinates.
     * @param centreY The y coordinate where the operation should be applied, in world coordinates.
     * @param inverse Whether to perform the "inverse" operation instead of the regular operation, if applicable. If the
     *                operation has no inverse it should just apply the normal operation.
     * @param first Whether this is the first tick of a continuous operation. For a one shot operation this will always
     *              be {@code true}.
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
        int size = 128;
        float[][] height = new float[size][];
        float[][] waterHeight = new float[size][];
        Terrain[][] terrain = new Terrain[size][];
        Dimension dim = getDimension();
        int offset = size/2;
        int startX = centreX - offset;
        int startY = centreY - offset;
        for (int x = 0; x < size; x++) {
            height[x] = new float[size];
            waterHeight[x] = new float[size];
            terrain[x] = new Terrain[size];
            for (int y = 0; y < size; y++) {
                height[x][y] = dim.getHeightAt(x + startX,y+startY);
                waterHeight[x][y] = dim.getWaterLevelAt(x+ startX,y+startY);
                terrain[x][y] = dim.getTerrainAt(x+ startX,y+startY);
            }
        }
        GlobalActionPanel.getSurfaceObject().setTerrainData(height,terrain,waterHeight);
        SwingUtilities.invokeLater(() -> GlobalActionPanel.getPreviewer().setObject(GlobalActionPanel.getSurfaceObject(), getDimension()));
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

    private Brush brush;
    private Paint paint;

    /**
     * The globally unique ID of the operation. It's up to you what to use here. It is not visible to the user. It can
     * be a FQDN or package and class name, like here, or you could use a UUID. As long as it is globally unique.
     */
    static final String ID = "org.demo.wpplugin.3D_preview_Operation";

    /**
     * Human-readable short name of the operation.
     */
    static final String NAME = "3D Preview";

    /**
     * Human-readable description of the operation. This is used e.g. in the tooltip of the operation selection button.
     */
    static final String DESCRIPTION = "Show terrain, height and waterheight in a 3d preview";
}