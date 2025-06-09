package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import org.ironsight.wpplugin.macromachine.operations.ProviderType;
import org.pepsoft.worldpainter.Dimension;

import java.awt.*;

import static org.pepsoft.worldpainter.Constants.TILE_SIZE;

public class BlockFacingDirectionIO implements IPositionValueGetter {
    private final String[] directionNames = new String[]{"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

    /**
     * Calculates the slope's facing direction (0-360 degrees) at (x, y).
     *
     * @param x The x-coordinate of the block.
     * @param y The y-coordinate of the block.
     * @return The facing direction in degrees (0-360).
     */
    public static int calculateSlopeFacing(int x, int y, Dimension dimension) {
        // Get heights of the current block and its neighbors
        double heightNorth = dimension.getHeightAt(x, y + 1); // North (0 degrees)
        double heightSouth = dimension.getHeightAt(x, y - 1); // South (180 degrees)
        double heightEast = dimension.getHeightAt(x + 1, y); // East (90 degrees)
        double heightWest = dimension.getHeightAt(x - 1, y); // West (270 degrees)

        // Calculate gradients (y-axis points north-south, x-axis points east-west)
        double gradientX = (heightWest - heightEast) / 2.0; // Change in the east-west direction
        double gradientY = (heightNorth - heightSouth) / 2.0; // Change in the north-south direction

        // Calculate the angle using atan2 (note the negative gradientY for north alignment)
        double angleRadians = Math.atan2(-gradientY, gradientX);
        double angleDegrees = (Math.toDegrees(angleRadians) + 90) % 360;    //ChatGPT made it look to the east, 90°
        // offset
        // corerctts it

        // Normalize angle to the range [0, 360)
        if (angleDegrees < 0) {
            angleDegrees += 360;
        }
        assert angleDegrees >= 0;
        assert angleDegrees < 360;
        return (int) Math.floor(angleDegrees);
    }

    @Override
    public int getValueAt(Dimension dim, int x, int y) {
        if (x <= dim.getLowestX() * TILE_SIZE || y <= dim.getLowestY() * TILE_SIZE ||
                x >= (dim.getHighestX() + 1) * TILE_SIZE || y >= (dim.getHighestY() + 1) * TILE_SIZE) {
            return 0;
        }
        return calculateSlopeFacing(x, y, dim);
    }

    @Override
    public int hashCode() {
        return getProviderType().hashCode();
    }

    @Override
    public boolean isVirtual() {
        return false;
    }

    @Override
    public String getName() {
        return "Slope Direction";
    }

    @Override
    public String getDescription() {
        return "facing of a slope in 0 to 359° on the compass, 0 is north, 180 is south";
    }

    @Override
    public String getToolTipText() {
        return getDescription();
    }

    @Override
    public int getMaxValue() {
        return 359;
    }

    @Override
    public int getMinValue() {
        return 0;
    }

    @Override
    public void prepareForDimension(Dimension dim) {

    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && this.getClass().equals(obj.getClass());
    }

    @Override
    public IMappingValue instantiateFrom(Object[] data) {
        return new BlockFacingDirectionIO();
    }

    @Override
    public Object[] getSaveData() {
        return new Object[0];
    }

    @Override
    public String valueToString(int value) {
        assert value >= 0;
        assert value < 360 : "value = " + value;
        int point = Math.round(value / 45f) % 8;
        assert point >= 0;
        assert point < 8;
        return value + "° " + directionNames[point];
    }

    @Override
    public boolean isDiscrete() {
        return false;
    }

    @Override
    public void paint(Graphics g, int value, java.awt.Dimension dim) {
        Graphics2D graphics = (Graphics2D) g;
        //rotate local space so that local north points into desired direction
        graphics.translate(dim.getWidth() / 2.0, dim.getHeight() / 2.0);
        graphics.rotate(Math.toRadians(value));
        g.setColor(Color.red);
        ((Graphics2D) g).setStroke(new BasicStroke(dim.width * 0.05f));
        //g.fillRect(0, 0, (int) (dim.width * 0.1f), dim.height);
        g.drawLine(0, -dim.height, 0, 0);  //pointing north in local space always
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.BLOCK_DIRECTION;
    }
}
