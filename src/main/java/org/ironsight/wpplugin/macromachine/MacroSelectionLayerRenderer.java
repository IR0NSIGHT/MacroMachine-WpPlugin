package org.ironsight.wpplugin.macromachine;

import org.pepsoft.worldpainter.ColourScheme;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.layers.renderers.*;

/**
 * The renderer is responsible for painting the layer in the editor view. There are various implementations provided,
 * or you can implement your own. Note that the renderer has to be of a type corresponding to the data size of the
 * layer (so it must implement {@link BitLayerRenderer}, {@link NibbleLayerRenderer} or {@link ByteLayerRenderer}.
 *
 * <p>The {@link Layer} by default finds the renderer by looking for a class in the {@code renderer} subpackage and with
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
public class MacroSelectionLayerRenderer extends TransparentColourRenderer {
    public static final MacroSelectionLayerRenderer instance = new MacroSelectionLayerRenderer();
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

    public MacroSelectionLayerRenderer() {
        super(0xFF0000); // Colour in 24-bit RGB format
    }

    @Override
    public int getPixelColour(int x, int y, int underlyingColour, int value) {
        int[] colors = {
                0xFF0000, // Red    1
                0x00FF00, // Lime   2
                0x8B0000, // Dark Red   3
                0x008000, // Dark Green 4
                0x0000FF, // Blue   5
                0x000080, //  public static Navy    6
                0x00FFFF, //  public static Cyan    7
                0x008B8B, //  public static Dark Cyan 8
                0xFF00FF, //  public static Magenta 9
                0x8B008B, //  public static Dark Magenta    10
                0xFFFF00, //  public static Yellow  11
                0xBDB76B, //  public static Dark Yellow 12
                0xFFA500, // public static  Orange  13
                0xFF8C00, //  public static Dark Orange 14
                0x800080, //  public static Purple  15
                0x4B0082  //  public static Dark Purpl e16
        };

        return colors[value];
    }
}