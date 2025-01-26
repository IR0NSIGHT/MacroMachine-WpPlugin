package org.ironsight.wpplugin.expandLayerTool.Gui;

import org.ironsight.wpplugin.expandLayerTool.operations.IPositionValueGetter;
import org.ironsight.wpplugin.expandLayerTool.operations.IPositionValueSetter;
import org.ironsight.wpplugin.expandLayerTool.operations.LayerMapping;
import org.pepsoft.worldpainter.layers.Annotations;
import org.pepsoft.worldpainter.layers.DeciduousForest;
import org.pepsoft.worldpainter.layers.Frost;
import org.pepsoft.worldpainter.layers.PineForest;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

public class OutputComboBox extends JComboBox<String> {
    Map<String, IPositionValueSetter> stringToGetter = new HashMap<>();

    public OutputComboBox() {
        addGetter(new LayerMapping.BitLayerBinarySpraypaintApplicator(Frost.INSTANCE));
        addGetter(new LayerMapping.NibbleLayerSetter(DeciduousForest.INSTANCE));
        addGetter(new LayerMapping.NibbleLayerSetter(PineForest.INSTANCE));
        addGetter(new LayerMapping.NibbleLayerSetter(Annotations.INSTANCE));
        addGetter(new LayerMapping.StonePaletteApplicator());
    }

    private void addGetter(IPositionValueSetter getter) {
        this.stringToGetter.put(getter.getName(), getter);
        this.addItem(getter.getName());
    }

    public IPositionValueSetter getSelectedProvider() {
        return stringToGetter.get((String) getSelectedItem());
    }
    public void SetSelected(IPositionValueSetter getter) {
        this.setSelectedItem(getter.getName());
    }
}

