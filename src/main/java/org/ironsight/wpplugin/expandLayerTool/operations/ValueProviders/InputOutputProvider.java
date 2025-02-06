package org.ironsight.wpplugin.expandLayerTool.operations.ValueProviders;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.layers.Annotations;
import org.pepsoft.worldpainter.layers.Biome;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.layers.LayerManager;

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

        for (Layer l : LayerManager.getInstance().getLayers()) {
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

        for (Layer l : dimension.getAllLayers(false)) {
            if (l.dataSize.equals(Layer.DataSize.NIBBLE)) {
                setters.add(new NibbleLayerSetter(l));
                getters.add(new NibbleLayerSetter(l));
            }
            if (l.dataSize.equals(Layer.DataSize.BIT)) setters.add(new BitLayerBinarySpraypaintApplicator(l));
        }
        setters.add(new StonePaletteApplicator());

        setters.add(new AnnotationSetter());
        getters.add(new AnnotationSetter());

        getters.add(new HeightProvider());
        setters.add(new HeightProvider());

        getters.add(new SlopeProvider());

        getters.add(new VanillaBiomeProvider());
        setters.add(new VanillaBiomeProvider());
        notifyListeners();
    }

    private void notifyListeners() {
        for (Runnable r : genericNotifies)
            r.run();
    }
}
