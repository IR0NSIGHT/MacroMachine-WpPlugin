package org.ironsight.wpplugin.expandLayerTool.operations;

import org.ironsight.wpplugin.expandLayerTool.Gui.MappingEditorPanel;
import org.pepsoft.worldpainter.layers.Frost;
import org.pepsoft.worldpainter.layers.PineForest;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Objects;


public class LayerMappingContainer {
    public static LayerMappingContainer INSTANCE = new LayerMappingContainer();
    public ActionListener onChange;
    private ArrayList<LayerMapping> mappings = new ArrayList<>();

    public LayerMappingContainer() {
        mappings.add(new LayerMapping(new LayerMapping.SlopeProvider(), new LayerMapping.StonePaletteApplicator(),
                new LayerMapping.MappingPoint[0], LayerMapping.ActionType.SET, "paint mountainsides", ""));
        mappings.add(new LayerMapping(new LayerMapping.HeightProvider(),
                new LayerMapping.BitLayerBinarySpraypaintApplicator(Frost.INSTANCE), new LayerMapping.MappingPoint[0]
                , LayerMapping.ActionType.SET, "frost mountain tops", ""));
        mappings.add(new LayerMapping(new LayerMapping.SlopeProvider(),
                new LayerMapping.NibbleLayerSetter(PineForest.INSTANCE), new LayerMapping.MappingPoint[0],
                LayerMapping.ActionType.SET, "no steep pines", ""));
    }

    public void putMapping(LayerMapping mapping, String name) {
        //filter for identity
        if (mappings.contains(mapping)) return;

        //filter for name collision -> replaces old versions of self
        ArrayList<LayerMapping> newMapping = new ArrayList<>();
        mappings.stream().filter(m -> !Objects.equals(m.getName(), name)).forEach(newMapping::add);
        mappings = newMapping;
        mappings.add(mapping);
        if (onChange != null)
            onChange.actionPerformed(null);
    }

    public LayerMapping getMapping(String name) {
        for (LayerMapping m : mappings)
            if (m.getName().equals(name)) return m;
        return null;
    }

    public LayerMapping[] getMappings() {
        return mappings.toArray(new LayerMapping[0]);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("TEST PANEL");

        JDialog log = MappingEditorPanel.createDialog(frame,f -> {});
        log.setVisible(true);


        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}

