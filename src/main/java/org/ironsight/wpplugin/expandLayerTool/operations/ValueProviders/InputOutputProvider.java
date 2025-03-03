package org.ironsight.wpplugin.expandLayerTool.operations.ValueProviders;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.layers.*;
import org.pepsoft.worldpainter.selection.SelectionBlock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;

public class InputOutputProvider {
    public static InputOutputProvider INSTANCE = new InputOutputProvider();
    public final ArrayList<IPositionValueSetter> setters = new ArrayList<>();
    private final ArrayList<Runnable> genericNotifies = new ArrayList<>();
    public ArrayList<IPositionValueGetter> getters = new ArrayList<>();

    private InputOutputProvider() {
        updateFrom(null);
    }

    public void subscribe(Runnable runnable) {
        genericNotifies.add(runnable);
    }

    public void unsubscribe(Runnable runnable) {
        genericNotifies.remove(runnable);
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

        if (dimension != null) {    //TODO check: does this actually collect custom layers?
            for (Layer l : dimension.getCustomLayers()) {
                if (l.dataSize.equals(Layer.DataSize.NIBBLE)) {
                    setters.add(new NibbleLayerSetter(l));
                    getters.add(new NibbleLayerSetter(l));
                }
                if (l.dataSize.equals(Layer.DataSize.BIT)) setters.add(new BitLayerBinarySpraypaintApplicator(l));
            }
        }
        getters.add(new TerrainProvider());
        setters.add(new TerrainProvider());
        setters.add(new StonePaletteApplicator());

        setters.add(new AnnotationSetter());
        getters.add(new AnnotationSetter());

        getters.add(new HeightProvider());
        setters.add(new HeightProvider());

        getters.add(new WaterHeightAbsoluteIO());
        setters.add(new WaterHeightAbsoluteIO());

        getters.add(new WaterDepthProvider());
        setters.add(new WaterDepthProvider());

        getters.add(new SlopeProvider());
        getters.add(new BlockFacingDirectionIO());

        getters.add(new SelectionIO());
        setters.add(new SelectionIO());
        setters.add(new BitLayerBinarySpraypaintApplicator(SelectionBlock.INSTANCE));

        getters.add(new VanillaBiomeProvider());
        setters.add(new VanillaBiomeProvider());

        getters.add(new IntermediateValueIO());
        setters.add(new IntermediateValueIO());

        setters.sort(Comparator.comparing(o -> o.getName().toLowerCase()));

        getters.sort(Comparator.comparing(o -> o.getName().toLowerCase()));

        notifyListeners();
    }

    private void notifyListeners() {
        for (Runnable r : genericNotifies)
            r.run();
    }
}
