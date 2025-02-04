package org.ironsight.wpplugin.expandLayerTool.operations;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public abstract class AbstractOperationContainer<T extends SaveableAction> {
    private final ArrayList<Runnable> genericNotifies = new ArrayList<>();
    private final HashMap<UUID, ArrayList<Runnable>> uidNotifies = new HashMap<>();
    private final HashMap<UUID, T> mappings = new HashMap<>();
    public String filePath = "/home/klipper/Documents/worldpainter/mappings.txt";
    Class<T> type;
    private boolean suppressFileWriting = false;

    protected AbstractOperationContainer(Class<T> type) {
        this.type = type;
    }

    public void updateMapping(T mapping) {
        //filter for identity
        if (!mappings.containsKey(mapping.getUid()) || mappings.get(mapping.getUid()).equals(mapping)) return;
        mappings.put(mapping.getUid(), mapping);
        notify(mapping);
    }

    public void deleteMapping(UUID uid) {
        T removed = mappings.remove(uid);
        if (removed != null) {
            notify(removed);
        }
    }

    protected UUID getUUID() {
        return UUID.randomUUID();
    }

    protected abstract T getNewAction();

    public T addMapping() {
        T newMap = getNewAction();
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
        ArrayList<Runnable> listeners = uidNotifies.getOrDefault(uid, null);
        if (listeners != null) listeners.remove(runnable);
    }

    public T queryMappingById(UUID uid) {
        return mappings.get(uid);
    }

    public ArrayList<T> queryMappingsAll() {
        ArrayList<T> list = new ArrayList<>(mappings.values());
        return list;
    }

    private void putMapping(T mapping) {
        assert !mappings.containsKey(mapping.getUid());
        mappings.put(mapping.getUid(), mapping);
    }

    private void notify(T mapping) {
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
        try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(Paths.get(filePath)))) {
            suppressFileWriting = true;
            // Read the object from the file
            deserializedObject = ois.readObject();

            Object[] arr = (Object[]) deserializedObject;
            for (Object o : arr) {
                if (type.isInstance(o)) putMapping((T) o);
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
