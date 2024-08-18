package org.demo.wpplugin.layers;

import org.demo.wpplugin.layers.renderers.DemoLayerRenderer;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.layers.renderers.LayerRenderer;

import java.io.Serializable;

import static org.pepsoft.worldpainter.layers.Layer.DataSize.NIBBLE;

/**
 * The layer descriptor and provider class for your custom layer. This class is serialised in the .world file when it is
 * saved, so it must be {@link Serializable} and stable.
 */
public class PathPreviewLayer extends Layer {
    private PathPreviewLayer() {
        super(ID, NAME, DESCRIPTION, DATA_SIZE, DISCRETE, PRIORITY);
    }

    @Override
    public LayerRenderer getRenderer() {
        return DemoLayerRenderer.instance;
    }

    /**
     * The globally unique ID of the layer. It's up to you what to use here. It is not visible to the user. It can be a
     * FQDN or package and class name, like here, or you could use a UUID. As long as it is globally unique. The ID must
     * change if the layer changes in a non backwards compatible way, but not between versions of the plugin if it is
     * still backwards compatible.
     */
    static final String ID = "org.demo.wpplugin.PathPreviewLayer.v1";

    /**
     * Human-readable short name of the plugin.
     */
    static final String NAME = "PathPreviewLayer";

    /**
     * Human-readable description of the plugin. This is used e.g. in the tooltip of the layer selection button.
     */
    static final String DESCRIPTION = "Previewing the current path";

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
     * Whether the layer values represent discrete entities (e.g. different biomes or terrains) or points on a
     * continuous, sliding scale (e.g. the intensity with which a layer should be applied). This affects, for example,
     * whether importing a mask for this layer will perform antialiasing of the input values, which is not desirable for
     * discrete values.
     */
    static final boolean DISCRETE = false;

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

    // This needs to be last, otherwise the static fields are not yet initialised
    public static final PathPreviewLayer INSTANCE = new PathPreviewLayer();

    /**
     * This class is serialised in the .world file when it is saved, so it must be stable. It is recommended to give it
     * a fixed {@code serialVersionUID} and ensure that any changes are backwards compatible.
     */
    private static final long serialVersionUID = 1L;
}