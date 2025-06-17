package org.ironsight.wpplugin.macromachine.Layers;

import org.pepsoft.worldpainter.ColourScheme;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.layers.renderers.*;

/**
 * The renderer is responsible for painting the layer in the editor view. There are various implementations provided, or
 * you can implement your own. Note that the renderer has to be of a type corresponding to the data size of the layer
 * (so it must implement {@link BitLayerRenderer}, {@link NibbleLayerRenderer} or {@link ByteLayerRenderer}.
 *
 * <p>The {@link Layer} by default finds the renderer by looking for a class in the {@code renderer} subpackage and
 * with
 * the word {@code Renderer} appended to its own class name. In that case the renderer must have a default (public, no
 * arguments) constructor. You could also override the {@link Layer#getRenderer()} method if you want to do something
 * more complicated.
 *
 * <p>This demo uses the simplest provided implementation to create a very simple solid colour-based renderer. Another
 * provided implementation you could look at is {@link ColouredPatternRenderer}, which allows you to specify a simple
 * pattern.
 *
 * <p>There are also decorator interfaces the renderer can implement if it needs more information:
 * {@link DimensionAwareRenderer} to get access to the {@link Dimension} that is being rendered, and
 * {@link ColourSchemeRenderer} to get access to the current WorldPainter {@link ColourScheme}.
 */
@SuppressWarnings("unused") // Instantiated by the Layer class
public class HeatMapLayerRenderer extends TransparentColourRenderer {
    public static final HeatMapLayerRenderer instance = new HeatMapLayerRenderer();
    public static int RED = 1;
    public static int DARK_RED = 2;
    public static int LIME = 3;
    public static int DARK_GREEN = 4;
    public static int BLUE = 5;
    public static int Navy = 6;
    public static int Cyan = 7;
    public static int Dark_Cyan = 8;
    public static int Magenta = 9;
    public static int Dark_Magenta = 10;
    public static int Yellow = 11;
    public static int Dark_Yellow = 12;
    public static int Orange = 13;
    public static int Dark_Orange = 14;
    public static int Purple = 15;
    public static int Dark_Purple = 16;

    public HeatMapLayerRenderer() {
        super(0xFF0000); // Colour in 24-bit RGB format
    }

    @Override
    public int getPixelColour(int x, int y, int underlyingColour, int value) {
        int[] colors = {
                0xff2400,
                0xff6000,
                0xff9a00,
                0xffd700,

                0xebff00,
                0xa5ff00,
                0x62ff00,
                0x18ff00,

                0x00ff25,
                0x00ff68,
                0x00ffa4,
                0x00ffeb,

                0x00daff,
                0x009aff,
                0x005fff,
                0x001fff
        };
        return colors[15-value];
        //return 0x010000 * value * 16 | ((15 - value) * 16) * 0x000001;
    }
}