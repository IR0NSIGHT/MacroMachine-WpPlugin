package org.ironsight.wpplugin.macromachine.Layers.PathBuilder;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PathToolNodeStyleTest
{
    private static BufferedImage render(Consumer<Graphics2D> painter) {
        var image = new BufferedImage(60, 60, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            painter.accept(g);
        } finally {
            g.dispose();
        }
        return image;
    }

    private static Color at(BufferedImage image, int x, int y) {
        return new Color(image.getRGB(x, y), true);
    }

    @Test
    void selectedNodeIsLavenderAndUnselectedCharcoalWithALightRing() {
        var center = new Point(30, 30);
        var selected = render(g -> PathTool.paintNode(g, center, true));
        var unselected = render(g -> PathTool.paintNode(g, center, false));

        assertEquals(PathTool.SELECTED_NODE_FILL, at(selected, 30, 30));
        assertEquals(PathTool.UNSELECTED_NODE_FILL, at(unselected, 30, 30));
        // ring (radius 10): lavender for selected, light for unselected nodes
        assertEquals(PathTool.SELECTED_NODE_FILL, at(selected, 40, 30));
        assertEquals(PathTool.UNSELECTED_NODE_OUTLINE, at(unselected, 40, 30));
        // charcoal halo just outside the ring keeps both readable on any terrain
        assertEquals(PathTool.SELECTED_NODE_HALO, at(selected, 41, 30));
        assertEquals(PathTool.SELECTED_NODE_HALO, at(unselected, 41, 30));
        // marker never extends beyond the halo
        assertEquals(0, at(selected, 44, 30).getAlpha());
        assertEquals(0, at(unselected, 44, 30).getAlpha());
    }

    @Test
    void selectedLabelUsesAccentEmphasis() {
        var center = new Point(10, 30);
        var selected = render(g -> PathTool.paintNodeLabel(g, center, 8, true));
        var unselected = render(g -> PathTool.paintNodeLabel(g, center, 8, false));

        assertTrue(contains(selected, PathTool.SELECTED_NODE_FILL));
        assertTrue(contains(selected, PathTool.SELECTED_NODE_HALO));
        assertFalse(contains(unselected, PathTool.SELECTED_NODE_FILL));
        assertTrue(contains(unselected, Color.WHITE));
    }

    private static boolean contains(BufferedImage image, Color color) {
        for (int x = 0; x < image.getWidth(); x++)
            for (int y = 0; y < image.getHeight(); y++)
                if (image.getRGB(x, y) == color.getRGB())
                    return true;
        return false;
    }
}
