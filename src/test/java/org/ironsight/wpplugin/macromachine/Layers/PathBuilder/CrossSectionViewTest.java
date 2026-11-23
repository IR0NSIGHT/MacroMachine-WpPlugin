package org.ironsight.wpplugin.macromachine.Layers.PathBuilder;

import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

import static org.ironsight.wpplugin.macromachine.Layers.PathBuilder.CrossSectionView.BLOCK;
import static org.ironsight.wpplugin.macromachine.Layers.PathBuilder.CrossSectionView.NO_WATER;
import static org.junit.jupiter.api.Assertions.*;

class CrossSectionViewTest
{
    private static final Color GRASS = Color.GREEN, SAND = Color.YELLOW, DIRT = Color.ORANGE, WATER = Color.BLUE,
            SKY = Color.LIGHT_GRAY;

    private static CrossSectionView view() {
        var view = new CrossSectionView();
        view.setSubsurfaceColor(DIRT);
        view.setWaterColor(WATER);
        view.setSkyColor(SKY);
        // a bank at 64, a dip down to 58 holding water up to 62
        view.setColumns(List.of(new CrossSectionView.Column(64, NO_WATER, GRASS),
                new CrossSectionView.Column(58, 62, SAND), new CrossSectionView.Column(60, 62, SAND)));
        return view;
    }

    @Test
    void heightsAndSizeFollowTheColumns() {
        var view = view();
        assertEquals(64, view.maxHeight());
        assertEquals(58, view.minHeight());
        var size = view.getPreferredSize();
        assertEquals((64 - 58 + 3) * BLOCK, size.height, "a block of sky above, one of ground below");
        assertEquals(CrossSectionView.LABEL_WIDTH + 3 * BLOCK, size.width);
        assertTrue(new CrossSectionView.Column(60, 62, SAND).hasWater());
        assertFalse(new CrossSectionView.Column(62, 62, SAND).hasWater(), "water no higher than the terrain");
        assertFalse(new CrossSectionView.Column(60, NO_WATER, SAND).hasWater());
    }

    @Test
    void paintsSkyWaterSurfaceAndSubsurfaceToScale() {
        var view = view();
        var size = view.getPreferredSize();
        view.setSize(size);
        var image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
        var g = image.createGraphics();
        view.paint(g);
        g.dispose();
        int dip = view.xOf(1) + 2;
        assertEquals(SKY.getRGB(), image.getRGB(dip, view.rowOf(63) + 2));
        assertEquals(WATER.getRGB(), image.getRGB(dip, view.rowOf(62) + 2));
        assertEquals(WATER.getRGB(), image.getRGB(dip, view.rowOf(59) + 2));
        assertEquals(SAND.getRGB(), image.getRGB(dip, view.rowOf(58) + 2));
        assertEquals(DIRT.getRGB(), image.getRGB(dip, view.rowOf(57) + 2));
        int bank = view.xOf(0) + 2;
        assertEquals(SKY.getRGB(), image.getRGB(bank, 2));
        assertEquals(GRASS.getRGB(), image.getRGB(bank, view.rowOf(64) + 2));
        assertEquals(DIRT.getRGB(), image.getRGB(bank, view.rowOf(60) + 2));
    }
}
