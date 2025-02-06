package org.ironsight.wpplugin.expandLayerTool.operations;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.UUID;

public abstract class AbstractOperationContainer<T extends SaveableAction> {
    private final ArrayList<Runnable> genericNotifies = new ArrayList<>();
    private final HashMap<UUID, ArrayList<Runnable>> uidNotifies = new HashMap<>();
    private final HashMap<UUID, T> mappings = new HashMap<>();
    private final Class<T> type;
    public String filePath;
    private boolean suppressFileWriting = false;

    protected AbstractOperationContainer(Class<T> type, String filePath) {
        this.type = type;
        this.filePath = filePath;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void updateMapping(T mapping) {
        //filter for identity
        if (!mappings.containsKey(mapping.getUid()) || queryById(mapping.getUid()).equals(mapping)) {
            return;
        }
        mappings.put(mapping.getUid(), mapping);
        notify(mapping.getUid());
    }

    public void deleteMapping(UUID... uid) {
        LinkedList<UUID> list = new LinkedList<>();
        for (UUID u : uid) {
            T removed = mappings.remove(u);
            if (removed != null) list.add(removed.getUid());
            System.out.println("removed objet from container: " + removed.getName());
        }
        notify(list.toArray(new UUID[0]));
    }

    protected UUID getUUID() {
        return UUID.randomUUID();
    }

    protected abstract T getNewAction();

    public T addMapping() {
        T newMap = getNewAction();
        mappings.put(newMap.getUid(), newMap);

        notify(newMap.getUid());
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

    public T queryById(UUID uid) {
        return mappings.get(uid);
    }

    public ArrayList<T> queryAll() {
        ArrayList<T> list = new ArrayList<>(mappings.values());
        return list;
    }

    private void putMapping(T mapping) {
        assert !mappings.containsKey(mapping.getUid());
        mappings.put(mapping.getUid(), mapping);
    }

    private void notify(UUID... mapping) {
        for (Runnable r : genericNotifies)
            r.run();
        for (UUID obj : mapping)
            if (uidNotifies.containsKey(obj)) {
                for (Runnable r : uidNotifies.get(obj))
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
                if (type.isInstance(o)) putMapping((T) o);
            }
            System.out.println(
                    this.getClass().getSimpleName() + " successfully loaded " + mappings.size() + " objects from " +
                            ": " + filePath);

        } catch (FileNotFoundException ignored) {
            System.out.println(this.getClass().getSimpleName() + " File not found: " + filePath);
        } catch (IOException | ClassNotFoundException e) {
            System.err.println(this.getClass().getSimpleName() + " Error during deserialization: " + e.getMessage());
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
            System.out.println(this.getClass().getSimpleName() + " saved successfully to " + filePath);
        } catch (IOException e) {
            System.err.println(this.getClass().getSimpleName() + "Error during serialization: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
