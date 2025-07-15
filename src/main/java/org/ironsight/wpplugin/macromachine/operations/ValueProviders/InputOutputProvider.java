package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import org.ironsight.wpplugin.macromachine.MacroSelectionLayer;
import org.pepsoft.worldpainter.CustomLayerController;
import org.pepsoft.worldpainter.CustomLayerControllerWrapper;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.layers.*;
import org.pepsoft.worldpainter.plugins.LayerProvider;
import org.pepsoft.worldpainter.selection.SelectionBlock;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class InputOutputProvider implements IMappingValueProvider,
        org.ironsight.wpplugin.macromachine.operations.ValueProviders.LayerProvider {
    public static InputOutputProvider INSTANCE = new InputOutputProvider();
    public final ArrayList<IMappingValue> setters = new ArrayList<>();
    private final ArrayList<Runnable> genericNotifies = new ArrayList<>();
    public ArrayList<IMappingValue> getters = new ArrayList<>();
    HashSet<Layer> layers = new HashSet<>();
    private AllowedLayerSettings inputSettings = new AllowedLayerSettings(true, true, true, true);
    private AllowedLayerSettings outputSettings = new AllowedLayerSettings(true, true, true, true);
    private Dimension dimension = null;

    private InputOutputProvider() {
        updateFrom(null);
    }

    @Override
    public Layer getLayerById(String layerId, Consumer<String> layerNotFoundError) {
        for (Layer l : getLayers())
            if (l.getId().equals(layerId))
                return l;
        layerNotFoundError.accept("could not find layer with id=" + layerId);
        return null;
    }

    void subscribe(Runnable runnable) {
        genericNotifies.add(runnable);
    }

    public IMappingValueProvider asInputProvider() {
        return new IMappingValueProvider() {
            @Override
            public Collection<IMappingValue> getItems() {
                return getters;
            }

            @Override
            public void subscribeToUpdates(Runnable r) {
                subscribe(r);
            }

            @Override
            public boolean existsItem(Object item) {
                return getters.contains(item);
            }
        };
    }

    public IMappingValueProvider asOutputProvider() {
        return new IMappingValueProvider() {
            @Override
            public Collection<IMappingValue> getItems() {
                return setters;
            }

            @Override
            public void subscribeToUpdates(Runnable r) {
                subscribe(r);
            }

            @Override
            public boolean existsItem(Object item) {
                return setters.contains(item);
            }
        };
    }

    public void updateFrom(Dimension dimension) {
        this.dimension = dimension;
        setters.clear();
        getters.clear();

        Iterable<Layer> layers = getLayers();
        for (Layer l : layers) {
            if (l instanceof CustomLayer) continue;
            if (l instanceof Annotations || l instanceof Biome) continue;
            if (l.dataSize.equals(Layer.DataSize.NIBBLE)) {
                setters.add(new NibbleLayerSetter(l, false));
                getters.add(new NibbleLayerSetter(l, false));
            }
            if (l.dataSize.equals(Layer.DataSize.BIT)) {
                setters.add(new BitLayerBinarySpraypaintApplicator(l, false));
                setters.add(new BinaryLayerIO(l, false));
                getters.add(new BinaryLayerIO(l, false));
            }
        }
        for (Layer l : layers) {
            if (!(l instanceof CustomLayer)) continue;
            if (l.dataSize.equals(Layer.DataSize.NIBBLE)) {
                if (inputSettings.allowCustomLayers)
                    setters.add(new NibbleLayerSetter(l, true));
                if (outputSettings.allowCustomLayers)
                    getters.add(new NibbleLayerSetter(l, true));
            }
            if (l.dataSize.equals(Layer.DataSize.BIT)) {
                if (outputSettings.allowCustomLayers) {
                    setters.add(new BitLayerBinarySpraypaintApplicator(l, true));
                    setters.add(new BinaryLayerIO(l, true));
                }
                if (inputSettings.allowCustomLayers)
                    getters.add(new BinaryLayerIO(l, true));
            }
        }
        getters.add(new ShadowMapIO());
        getters.add(new DistanceToLayerEdgeGetter(MacroSelectionLayer.INSTANCE,100));
        getters.add(new TerrainProvider());
        setters.add(new TerrainProvider());
        setters.add(new StonePaletteApplicator());

        setters.add(new AnnotationSetter());
        getters.add(new AnnotationSetter());

        getters.add(new TerrainHeightIO(-64, 319));
        setters.add(new TerrainHeightIO(-64, 319));

        getters.add(new WaterHeightAbsoluteIO(-64, 319));
        setters.add(new WaterHeightAbsoluteIO(-64, 319));

        getters.add(new WaterDepthProvider());
        setters.add(new WaterDepthProvider());

        getters.add(new SlopeProvider());
        getters.add(new BlockFacingDirectionIO());

        getters.add(new SelectionIO());
        setters.add(new SelectionIO());
        setters.add(new BitLayerBinarySpraypaintApplicator(SelectionBlock.INSTANCE, false));

        getters.add(new VanillaBiomeProvider());
        setters.add(new VanillaBiomeProvider());

        setters.add(new IntermediateValueIO(0, 100, ""));
        getters.add(new IntermediateValueIO(0, 100, ""));

        getters.add(ActionFilterIO.instance);
        setters.add(ActionFilterIO.instance);

        getters.add(AlwaysIO.instance);

        setters.sort(Comparator.comparing(o -> o.getName().toLowerCase()));
        getters.sort(Comparator.comparing(o -> o.getName().toLowerCase()));

        getters.add(new PerlinNoiseIO(100, 100, 42069, 5));

        notifyListeners();
    }

    private void notifyListeners() {
        for (Runnable r : genericNotifies)
            r.run();
    }

    @Override
    public Collection<IMappingValue> getItems() {
        ArrayList out = new ArrayList<>();
        out.addAll(getters);
        out.addAll(setters);
        return out;
    }

    @Override
    public void subscribeToUpdates(Runnable r) {
        subscribe(r);
    }

    @Override
    public boolean existsItem(Object item) {
        return getItems().contains(item);
    }

    private Dimension getDimension() {
        return dimension;
    }

    @Override
    public List<Layer> getLayers() {
        LinkedList<Layer> layers = new LinkedList<>();
        if (getDimension() != null) {
            layers.addAll(LayerManager.getInstance().getLayers()); //vanilla layers

            layers.addAll(new CustomLayerControllerWrapper().getCustomLayers());
        } else {
            layers.addAll(Arrays.stream(new Layer[]{PineForest.INSTANCE, DeciduousForest.INSTANCE, Frost.INSTANCE,})
                    .collect(Collectors.toCollection(ArrayList::new)));
        }
        layers.add(Annotations.INSTANCE); // hardcoded bc not part by default
        this.layers.addAll(layers);
        return new ArrayList<>(this.layers);
    }

    @Override
    public void addLayer(Layer layer) {
        this.layers.add(layer);
        notifyListeners();
    }


    private class AllowedLayerSettings {
        boolean allowCustomLayers;
        boolean allowDefaultLayers;
        boolean allowSelection;
        boolean allowAnnotations;

        public AllowedLayerSettings(boolean allowCustomLayers, boolean allowDefaultLayers, boolean allowSelection,
                                    boolean allowAnnotations) {
            this.allowCustomLayers = allowCustomLayers;
            this.allowDefaultLayers = allowDefaultLayers;
            this.allowSelection = allowSelection;
            this.allowAnnotations = allowAnnotations;
        }
    }
}
