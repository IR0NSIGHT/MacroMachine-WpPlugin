package org.ironsight.wpplugin.expandLayerTool.operations.ValueProviders;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.layers.*;

import java.util.ArrayList;

public class InputOutputProvider {
    public static InputOutputProvider INSTANCE = new InputOutputProvider();
    public final ArrayList<IPositionValueSetter> setters = new ArrayList<>();
    private final ArrayList<Runnable> genericNotifies = new ArrayList<>();
    public ArrayList<IPositionValueGetter> getters = new ArrayList<>();

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
                setters.add(new NibbleLayerSetter(l));
                getters.add(new NibbleLayerSetter(l));
            }
            if (l.dataSize.equals(Layer.DataSize.BIT))
                setters.add(new BitLayerBinarySpraypaintApplicator(l));
        }

        for (Layer l : dimension.getAllLayers(false)) {
            if (l.dataSize.equals(Layer.DataSize.NIBBLE)) {
                setters.add(new NibbleLayerSetter(l));
                getters.add(new NibbleLayerSetter(l));
            }
            if (l.dataSize.equals(Layer.DataSize.BIT))
                setters.add(new BitLayerBinarySpraypaintApplicator(l));
        }
        setters.add(new StonePaletteApplicator());
        setters.add(new AnnotationSetter());
        getters.add(new AnnotationSetter());
        getters.add(new HeightProvider());
        getters.add(new SlopeProvider());
        getters.add(new VanillaBiomeProvider());

        notifyListeners();
    }

    private void notifyListeners() {
        for (Runnable r : genericNotifies)
            r.run();
    }
}
