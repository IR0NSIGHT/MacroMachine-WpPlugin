package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import org.ironsight.wpplugin.macromachine.operations.ProviderType;
import org.pepsoft.worldpainter.Constants;
import org.pepsoft.worldpainter.Dimension;

import java.awt.*;

import static org.pepsoft.worldpainter.Constants.TILE_SIZE;

public class ActionFilterIO implements IPositionValueSetter, IPositionValueGetter {
    public static ActionFilterIO instance = new ActionFilterIO();
    public static final int PASS_VALUE = 1;
    public static final int BLOCK_VALUE = 0;
    private transient TileContainer tileContainer;
    protected ActionFilterIO() {
    }

    public boolean skipTile(int tileX, int tileY) {
        assert tileContainer != null;
        return tileContainer.getTileAt(tileX * TILE_SIZE,tileY * TILE_SIZE).getMax() == BLOCK_VALUE;
    }

    @Override
    public int hashCode() {
        return getProviderType().hashCode();
    }

    @Override
    public boolean isVirtual() {
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && this.getClass().equals(obj.getClass());
    }

    @Override
    public int getValueAt(Dimension dim, int x, int y) {
        return tileContainer.getValueAt(x,y);
    }

    @Override
    public void setValueAt(Dimension dim, int x, int y, int value) {
        tileContainer.setValueAt(x,y, value);
    }

    @Override
    public void prepareForDimension(Dimension dim) {
        if (tileContainer != null)
            return;
        Rectangle rect = dim.getExtent();
        tileContainer = new TileContainer(rect.width, rect.height, rect.x * Constants.TILE_SIZE,
                rect.y * Constants.TILE_SIZE, PASS_VALUE);
    }

    public TileContainer getTileContainer() {
        return tileContainer;
    }

    public void releaseAfterApplication() {
        tileContainer = null;
    }

    @Override
    public String getName() {
        return "Action Filter";
    }

    @Override
    public String getDescription() {
        return "only blocks that pass this filter will be used in following actions.";
    }

    @Override
    public int getMaxValue() {
        return 1;
    }

    @Override
    public int getMinValue() {
        return 0;
    }

    @Override
    public IMappingValue instantiateFrom(Object[] data) {
        return instance;
    }

    @Override
    public Object[] getSaveData() {
        return new Object[0];
    }

    @Override
    public String valueToString(int value) {
        return value == PASS_VALUE ? "PASS (1)" : "BLOCK (0)";
    }

    @Override
    public boolean isDiscrete() {
        return true;
    }

    @Override
    public void paint(Graphics g, int value, java.awt.Dimension dim) {
        g.setColor(value == PASS_VALUE ? Color.GREEN : Color.RED);
        g.fillRect(0, 0, dim.width, dim.height);
    }
    @Override
    public String getToolTipText() {
        return getDescription();
    }
    @Override
    public ProviderType getProviderType() {
        return ProviderType.INTERMEDIATE_SELECTION;
    }

    @Override
    public String toString() {
        return getName();
    }
}
