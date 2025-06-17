package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.layers.*;
import org.pepsoft.worldpainter.selection.SelectionBlock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Collectors;

public class InputOutputProvider {
    public static InputOutputProvider INSTANCE = new InputOutputProvider();
    public final ArrayList<IMappingValue> setters = new ArrayList<>();
    private final ArrayList<Runnable> genericNotifies = new ArrayList<>();
    public ArrayList<IMappingValue> getters = new ArrayList<>();
    private AllowedLayerSettings inputSettings = new AllowedLayerSettings(false, true, true, true);
    private AllowedLayerSettings outputSettings = new AllowedLayerSettings(false, true, true, true);

    private InputOutputProvider() {
        updateFrom(null);
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
        };
    }

    public void updateFrom(Dimension dimension) {
        setters.clear();
        getters.clear();

        Iterable<Layer> layers;
        if (dimension != null) {
            layers = LayerManager.getInstance().getLayers();
        } else {
            layers = Arrays.stream(new Layer[]{PineForest.INSTANCE, DeciduousForest.INSTANCE, Frost.INSTANCE,})
                    .collect(Collectors.toCollection(ArrayList::new));
        }
        for (Layer l : layers) {
            if (l instanceof Annotations || l instanceof Biome) continue;
            if (l.dataSize.equals(Layer.DataSize.NIBBLE)) {
                setters.add(new NibbleLayerSetter(l));
                getters.add(new NibbleLayerSetter(l));
            }
            if (l.dataSize.equals(Layer.DataSize.BIT)) {
                setters.add(new BitLayerBinarySpraypaintApplicator(l));
                setters.add(new BinaryLayerIO(l));
                getters.add(new BinaryLayerIO(l));
            }
        }
        if (dimension != null) {
            for (Layer l : dimension.getCustomLayers()) {
                if (l.dataSize.equals(Layer.DataSize.NIBBLE)) {
                    if (inputSettings.allowCustomLayers)
                        setters.add(new NibbleLayerSetter(l));
                    if (outputSettings.allowCustomLayers)
                        getters.add(new NibbleLayerSetter(l));
                }
                if (l.dataSize.equals(Layer.DataSize.BIT))
                    if (outputSettings.allowCustomLayers)
                        setters.add(new BitLayerBinarySpraypaintApplicator(l));
            }
        }
    //    getters.add(new DistanceToLayerEdgeGetter(SelectionBlock.INSTANCE));
    //    getters.add(new DistanceToLayerEdgeGetter(MacroSelectionLayer.INSTANCE));
        getters.add(new TerrainProvider());
        setters.add(new TerrainProvider());
        setters.add(new StonePaletteApplicator());

        setters.add(new AnnotationSetter());
        getters.add(new AnnotationSetter());

        getters.add(new TerrainHeightIO(-64,319));
        setters.add(new TerrainHeightIO(-64,319));

        getters.add(new WaterHeightAbsoluteIO(-64,319));
        setters.add(new WaterHeightAbsoluteIO(-64,319));

        getters.add(new WaterDepthProvider());
        setters.add(new WaterDepthProvider());

        getters.add(new SlopeProvider());
        getters.add(new BlockFacingDirectionIO());

        getters.add(new SelectionIO());
        setters.add(new SelectionIO());
        setters.add(new BitLayerBinarySpraypaintApplicator(SelectionBlock.INSTANCE));

        getters.add(new VanillaBiomeProvider());
        setters.add(new VanillaBiomeProvider());

        setters.add(new IntermediateValueIO(0,100,""));
        getters.add(new IntermediateValueIO(0,100,""));

        getters.add(ActionFilterIO.instance);
        setters.add(ActionFilterIO.instance);

        getters.add(AlwaysIO.instance);

        setters.sort(Comparator.comparing(o -> o.getName().toLowerCase()));
        getters.sort(Comparator.comparing(o -> o.getName().toLowerCase()));

        getters.add(new PerlinNoiseIO(100, 100, 42069,5));

        notifyListeners();
    }

    private void notifyListeners() {
        for (Runnable r : genericNotifies)
            r.run();
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
