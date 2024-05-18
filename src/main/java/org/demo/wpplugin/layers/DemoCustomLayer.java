package org.demo.wpplugin.layers;

import org.demo.wpplugin.layers.exporters.DemoCustomLayerExporter;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.exporting.LayerExporter;
import org.pepsoft.worldpainter.layers.CustomLayer;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.layers.exporters.ExporterSettings;

import java.awt.*;

import static org.pepsoft.worldpainter.layers.Layer.DataSize.NIBBLE;

public class DemoCustomLayer extends CustomLayer {
    public DemoCustomLayer() {
        super(NAME, DESCRIPTION, DATA_SIZE, PRIORITY, COLOUR);
    }

    /**
     * A custom layer must override this method. The default implementation only works for singular non-configurable
     * {@link Layer}s.
     */
    @Override
    public LayerExporter getExporter(Dimension dimension, Platform platform, ExporterSettings settings) {
        return new DemoCustomLayerExporter(dimension, platform, settings, this);
    }

    /**
     * Human-readable short name of the plugin.
     */
    static final String NAME = "Bezier Preview Layer";

    /**
     * Human-readable description of the plugin. This is used e.g. in the tooltip of the layer selection button.
     */
    static final String DESCRIPTION = "Displaying the current Bezier Path Preview";

    /**
     * The data size (number of possible values) for this layer. If this changes, the ID must also change.
     * The choices are:
     *
     * <table>
     *     <tr><td>{@link DataSize#BIT}</td><td>Only on (1) or off (0) per block</td></tr>
     *     <tr><td>{@link DataSize#NIBBLE}</td><td>Sixteen values (0-15) per block</td></tr>
     *     <tr><td>{@link DataSize#BYTE}</td><td>256 values (0-255) per block</td></tr>
     *     <tr><td>{@link DataSize#BIT_PER_CHUNK}</td><td>Only on (1) or off (0) per chunk</td></tr>
     * </table>
     */
    static final DataSize DATA_SIZE = NIBBLE;

    /**
     * The priority in the export order for this layer. The exporters for layers with higher numbers will be invoked
     * <em>after</em> those for layers with lower numbers by default. Note that users can change the export order on the
     * Export screen. Some priorities of the built-in layers are:
     *
     * <table>
     *     <tr><td>0-9</td><td>Void</td></tr>
     *     <tr><td>10-19</td><td>Resources and Custom Underground Pockets</td></tr>
     *     <tr><td>20-29</td><td>Caverns, Chasms, Tunnels and Caves</td></tr>
     *     <tr><td>30-39</td><td>Custom Ground Cover, Custom Plants</td></tr>
     *     <tr><td>40-49</td><td>Deciduous Forest, Pine Forest, Swamp Land and Jungle</td></tr>
     *     <tr><td>50-59</td><td>Custom Objects</td></tr>
     *     <tr><td>60-69</td><td>Frost, Annotations</td></tr>
     * </table>
     *
     * <strong>Note</strong> that of course all the first pass exporters are always executed before the second pass
     * exporters.
     */
    static final int PRIORITY = 50;

    /**
     * The colour with which to render this layer in the editor. It must be passed to the superclass constructor, but it
     * can be subsequently ignored by implementing {@link #getRenderer()} and providing your own renderer.
     */
    static final Color COLOUR = Color.MAGENTA;

    /**
     * This class is serialised in the .world file when it is saved, so it must be stable. It is recommended to give it
     * a fixed {@code serialVersionUID} and ensure that any changes are backwards compatible.
     */
    private static final long serialVersionUID = 1L;
}