package org.ironsight.wpplugin.expandLayerTool.operations;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.layers.*;

import java.util.ArrayList;

public class InputOutputProvider {
    private final ArrayList<Runnable> genericNotifies = new ArrayList<>();
    public final ArrayList<IPositionValueGetter> getters = new ArrayList<>();
    public final ArrayList<IPositionValueSetter> setters = new ArrayList<>();
    public static InputOutputProvider INSTANCE = new InputOutputProvider();

    public void subscribe(Runnable runnable) {
        genericNotifies.add(runnable);
    }

    public void unsubscribe(Runnable runnable) {
        genericNotifies.remove(runnable);
    }

    public void updateFrom(Dimension dimension) {
        setters.clear();
        getters.clear();

        Layer[] layers = new Layer[]{
                Frost.INSTANCE,
                DeciduousForest.INSTANCE,
                PineForest.INSTANCE,
                ReadOnly.INSTANCE,
                Resources.INSTANCE,
                Caverns.INSTANCE,
                Caves.INSTANCE,
        };
        for (Layer l : layers) {
            if (l.dataSize.equals(Layer.DataSize.NIBBLE)) {
                setters.add(new LayerMapping.NibbleLayerSetter(l));
                getters.add(new LayerMapping.NibbleLayerSetter(l));
            }
            if (l.dataSize.equals(Layer.DataSize.BIT))
                setters.add(new LayerMapping.BitLayerBinarySpraypaintApplicator(l));
        }

        for (Layer l : dimension.getAllLayers(false)) {
            if (l.dataSize.equals(Layer.DataSize.NIBBLE)) {
                setters.add(new LayerMapping.NibbleLayerSetter(l));
                getters.add(new LayerMapping.NibbleLayerSetter(l));
            }
            if (l.dataSize.equals(Layer.DataSize.BIT))
                setters.add(new LayerMapping.BitLayerBinarySpraypaintApplicator(l));
        }
        setters.add(new LayerMapping.StonePaletteApplicator());
        setters.add(new LayerMapping.AnnotationSetter());
        getters.add(new LayerMapping.AnnotationSetter());
        getters.add(new LayerMapping.HeightProvider());
        getters.add(new LayerMapping.SlopeProvider());
        getters.add(new LayerMapping.VanillaBiomeProvider());

        notifyListeners();
    }

    private void notifyListeners() {
        for (Runnable r : genericNotifies)
            r.run();
    }
}
