package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import org.ironsight.wpplugin.macromachine.ArrayUtils;
import org.ironsight.wpplugin.macromachine.operations.ILimitedMapOperation;
import org.ironsight.wpplugin.macromachine.operations.ProviderType;
import org.ironsight.wpplugin.macromachine.operations.specialOperations.ShadowMap;
import org.pepsoft.worldpainter.Dimension;

import java.awt.*;
import java.util.Objects;

public class ShadowMapIO implements IPositionValueGetter, ILimitedMapOperation {
    @Override
    public String getName() {
        return "Shadow";
    }

    @Override
    public String getDescription() {
        return "Calculate a naive shadowmap: sun hits from the south at 45° inclination";
    }

    @Override
    public String getToolTipText() {
        return getDescription();
    }

    @Override
    public boolean isVirtual() {
        return false;
    }

    @Override
    public int getMaxValue() {
        return 100;
    }

    @Override
    public int getMinValue() {
        return 0;
    }

    @Override
    public void prepareForDimension(Dimension dim) throws IllegalAccessError {

    }

    @Override
    public IMappingValue instantiateFrom(Object[] data) {
        return new ShadowMapIO();
    }

    @Override
    public Object[] getSaveData() {
        return new Object[0];
    }

    @Override
    public String valueToString(int value) {
        return value != 0 ? "Shaded " + value : "Sun";
    }

    @Override
    public boolean isDiscrete() {
        return false;
    }

    @Override
    public void paint(Graphics g, int value, java.awt.Dimension dim) {
        if (value != 0) {
            float point = ((float)value)/getMaxValue();
            int color = (int)(64 * point + (1-point) * 192);
            g.setColor(new Color(color,color,color));
        }
        else
            g.setColor(Color.WHITE);
        g.fillRect(0,0,dim.width,dim.height);
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.SHADOW;
    }
    private TileContainer shadowMap;
    @Override
    public int getValueAt(Dimension dim, int x, int y) {
        assert shadowMap != null;
        return shadowMap.getValueAt(x,y);
    }

    public void calculateShadowMap(Dimension dim, TerrainHeightIO heightIO, int[] tileX, int[] tileY) {
        int startX = ArrayUtils.findMin(tileX), endX = ArrayUtils.findMax(tileX);
        int startY = ArrayUtils.findMin(tileY), endY = ArrayUtils.findMax(tileY);
        Rectangle extent = new Rectangle(startX,startY,endX-startX + 1, endY-startY + 1);
        this.shadowMap = ShadowMap.calculateShadowMap(extent, heightIO, dim);
    }

    public void releaseShadowMap(){
        this.shadowMap = null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o != null && getClass() == o.getClass();
    }

    @Override
    public void prepareRightBeforeRun(Dimension dimension, int[] tileX, int[] tileY) {
        this.calculateShadowMap(dimension, new TerrainHeightIO(-5000,5000), tileX, tileY);
        assert shadowMap != null;
    }

    @Override
    public void releaseRightAfterRun() {
        this.releaseShadowMap();
        assert shadowMap == null;
    }
}
