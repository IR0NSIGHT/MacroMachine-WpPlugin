package org.ironsight.wpplugin.expandLayerTool.operations;

import org.ironsight.wpplugin.expandLayerTool.Gui.MappingEditorPanel;
import org.ironsight.wpplugin.expandLayerTool.operations.ValueProviders.*;
import org.pepsoft.worldpainter.layers.Frost;
import org.pepsoft.worldpainter.layers.PineForest;

import javax.swing.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class LayerMappingContainer {
    public static LayerMappingContainer INSTANCE = new LayerMappingContainer();
    private final ArrayList<Runnable> genericNotifies = new ArrayList<>();
    private final HashMap<UUID, ArrayList<Runnable>> uidNotifies = new HashMap<>();
    private final HashMap<UUID, LayerMapping> mappings = new HashMap<>();
    public String filePath = "/home/klipper/Documents/worldpainter/mappings.txt";
    private boolean suppressFileWriting = false;

    public LayerMappingContainer() {
    }

    public static void addDefaultMappings(LayerMappingContainer container) {
        LayerMapping m = container.addMapping();
        m = new LayerMapping(new SlopeProvider(),
                new StonePaletteApplicator(),
                new MappingPoint[]{new MappingPoint(30, 3),
                        new MappingPoint(50, 8),
                        new MappingPoint(70, 5),
                        new MappingPoint(80, 9)},
                ActionType.SET,
                "paint mountainsides",
                "apply stone and rocks " + "based" + " on slope to make mountain sides colorful and interesting",
                m.getUid());
        container.updateMapping(m);

        m = container.addMapping();
        m = new LayerMapping(new HeightProvider(),
                new BitLayerBinarySpraypaintApplicator(Frost.INSTANCE),
                new MappingPoint[]{new MappingPoint(150, 0), new MappingPoint(230, 100)},
                ActionType.MAX,
                "frosted " + "peaks",
                "gradually add snow the higher a mountain goes",
                m.getUid());
        container.updateMapping(m);

        m = container.addMapping();
        m = new LayerMapping(new SlopeProvider(),
                new NibbleLayerSetter(PineForest.INSTANCE),
                new MappingPoint[]{new MappingPoint(0, 15), new MappingPoint(70, 15), new MappingPoint(80, 0)},
                ActionType.MIN,
                "no steep pines",
                "limit pines from growing on vertical cliffs",
                m.getUid());
        container.updateMapping(m);

        m = container.addMapping();
        m = new LayerMapping(new AnnotationSetter(),
                new TestInputOutput(),
                new MappingPoint[0],
                ActionType.SET,
                "colors",
                "",
                m.getUid());
        container.updateMapping(m);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("TEST PANEL");
        LayerMappingContainer.addDefaultMappings(INSTANCE);

        JDialog log = MappingEditorPanel.createDialog(frame, f -> {
        });
        log.setVisible(true);


        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    public void updateMapping(LayerMapping mapping) {
        //filter for identity
        if (!mappings.containsKey(mapping.getUid()) || mappings.get(mapping.getUid()).equals(mapping)) return;
        mappings.put(mapping.getUid(), mapping);
        notify(mapping);
    }

    public void deleteMapping(UUID uid) {
        LayerMapping removed = mappings.remove(uid);
        if (removed != null) {
            notify(removed);
        }
    }

    private UUID getUUID() {
        return UUID.randomUUID();
    }

    public LayerMapping addMapping() {
        LayerMapping newMap = new LayerMapping(new HeightProvider(),
                new AnnotationSetter(),
                new MappingPoint[0],
                ActionType.SET,
                "colors",
                "",
                getUUID());

        mappings.put(newMap.getUid(), newMap);

        notify(newMap);
        return newMap;
    }

    public void subscribe(Runnable runnable) {
        genericNotifies.add(runnable);
    }

    public void unsubscribe(Runnable runnable) {
        genericNotifies.remove(runnable);
    }

    public void subscribeToMapping(UUID uid, Runnable runnable) {
        ArrayList<Runnable> listeners = uidNotifies.getOrDefault(uid, new ArrayList<>());
        listeners.add(runnable);
        uidNotifies.put(uid, listeners);
    }

    public void unsubscribeToMapping(UUID uid, Runnable runnable) {
        ArrayList<Runnable> listeners = uidNotifies.getOrDefault(uid, new ArrayList<>());
        listeners.remove(runnable);
    }

    public LayerMapping queryMappingById(UUID uid) {
        return mappings.get(uid);
    }

    public LayerMapping[] queryMappingsAll() {
        LayerMapping[] arr = mappings.values().toArray(new LayerMapping[0]);
        Arrays.sort(arr, new Comparator<LayerMapping>() {
            @Override
            public int compare(LayerMapping o1, LayerMapping o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        return arr;
    }

    private void putMapping(LayerMapping mapping) {
        assert !mappings.containsKey(mapping.getUid());
        mappings.put(mapping.getUid(), mapping);
    }

    private void notify(LayerMapping mapping) {
        for (Runnable r : genericNotifies)
            r.run();
        if (mapping != null && uidNotifies.containsKey(mapping.getUid())) {
            for (Runnable r : uidNotifies.get(mapping.getUid()))
                r.run();
        }
    }

    public void readFromFile() {
        mappings.clear();
        Object deserializedObject;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath))) {
            suppressFileWriting = true;
            // Read the object from the file
            deserializedObject = ois.readObject();

            Object[] arr = (Object[]) deserializedObject;
            for (Object o : arr) {
                if (o instanceof LayerMapping) {
                    putMapping((LayerMapping) o);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error during deserialization: " + e.getMessage());
            e.printStackTrace();
        } finally {
            suppressFileWriting = false;
        }
    }

    public void writeToFile() {
        if (suppressFileWriting) return;
        Object obj = mappings.values().toArray();
        try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(Paths.get(filePath)))) {
            oos.writeObject(obj);
            System.out.println("Object successfully serialized to " + filePath);
        } catch (IOException e) {
            System.err.println("Error during serialization: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

