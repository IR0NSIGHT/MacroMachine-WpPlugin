package org.ironsight.wpplugin.expandLayerTool.operations;

import org.ironsight.wpplugin.expandLayerTool.Gui.MappingEditorPanel;
import org.pepsoft.worldpainter.layers.Frost;
import org.pepsoft.worldpainter.layers.PineForest;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;


public class LayerMappingContainer {
    public static LayerMappingContainer INSTANCE = new LayerMappingContainer();
    private final ArrayList<Runnable> genericNotifies = new ArrayList<>();
    private final HashMap<Integer, ArrayList<Runnable>> uidNotifies = new HashMap<>();
    private final HashMap<Integer, LayerMapping> mappings = new HashMap<>();
    private int nextUid = 1;

    public LayerMappingContainer() {

    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("TEST PANEL");

        INSTANCE.addMapping(new LayerMapping(new LayerMapping.SlopeProvider(),
                new LayerMapping.StonePaletteApplicator(), new LayerMapping.MappingPoint[0],
                LayerMapping.ActionType.SET, "paint mountainsides", ""));
        INSTANCE.addMapping(new LayerMapping(new LayerMapping.HeightProvider(),
                new LayerMapping.BitLayerBinarySpraypaintApplicator(Frost.INSTANCE), new LayerMapping.MappingPoint[0]
                , LayerMapping.ActionType.SET, "frost mountain tops", ""));
        INSTANCE.addMapping(new LayerMapping(new LayerMapping.SlopeProvider(),
                new LayerMapping.NibbleLayerSetter(PineForest.INSTANCE), new LayerMapping.MappingPoint[0],
                LayerMapping.ActionType.SET, "no steep pines", ""));

        JDialog log = MappingEditorPanel.createDialog(frame, f -> {
        });
        log.setVisible(true);


        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    public void updateMapping(LayerMapping mapping) {
        //filter for identity
        if (!mappings.containsKey(mapping.uid))
            return;
        mappings.put(mapping.uid, mapping);
        notify(mapping);
    }

    public void deleteMapping(int uid) {
        LayerMapping removed = mappings.remove(uid);
        if (removed != null) notify(removed);
    }

    public int addMapping(LayerMapping mapping) {
        if (mappings.containsKey(mapping.uid)) return -1;
        this.mappings.put(nextUid, mapping);
        mapping.uid = nextUid;
        nextUid++;
        notify(mapping);
        return mapping.uid;
    }

    public void subscribe(Runnable runnable) {
        genericNotifies.add(runnable);
    }

    public void unsubscribe(Runnable runnable) {
        genericNotifies.remove(runnable);
    }

    public void subscribeToMapping(int uid, Runnable runnable) {
        ArrayList<Runnable> listeners = uidNotifies.getOrDefault(uid, new ArrayList<>());
        listeners.add(runnable);
        uidNotifies.put(uid, listeners);
    }

    public void unsubscribeToMapping(int uid, Runnable runnable) {
        ArrayList<Runnable> listeners = uidNotifies.getOrDefault(uid, new ArrayList<>());
        listeners.remove(runnable);
    }

    public LayerMapping queryMappingById(int uid) {
        return mappings.get(uid);
    }

    public LayerMapping[] queryMappingsAll() {
        return mappings.values().toArray(new LayerMapping[0]);
    }

    private void notify(LayerMapping mapping) {
        for (Runnable r : genericNotifies)
            r.run();
        if (mapping != null && uidNotifies.containsKey(mapping.uid)) {
            for (Runnable r : uidNotifies.get(mapping.uid))
                r.run();
        }
    }
}

