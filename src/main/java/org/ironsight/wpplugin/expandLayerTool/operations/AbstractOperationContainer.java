package org.ironsight.wpplugin.expandLayerTool.operations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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
    private final boolean suppressFileWriting = false;
    private String filePath;

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

    public UUID updateMapping(T mapping) {
        //filter for identity
        if (!mappings.containsKey(mapping.getUid()) || queryById(mapping.getUid()).equals(mapping)) {
            return mapping.getUid();
        }
        mappings.put(mapping.getUid(), mapping);
        notify(mapping.getUid());
        return mapping.getUid();
    }

    public T queryById(UUID uid) {
        return mappings.get(uid);
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

    public T addMapping() {
        T newMap = getNewAction();
        mappings.put(newMap.getUid(), newMap);

        notify(newMap.getUid());
        return newMap;
    }

    protected abstract T getNewAction();

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

    public ArrayList<T> queryAll() {
        ArrayList<T> list = new ArrayList<>(mappings.values());
        return list;
    }

    public void readFromFile() {
        if (suppressFileWriting) return;
        mappings.clear();
        try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(Paths.get(filePath)))) {
            // Read the JSON string from the file
            String jsonString = (String) ois.readObject();

            fromSaveObject(jsonString);
        } catch (IOException | ClassNotFoundException e) {
            System.err.println(getClass().getSimpleName() + " - Error during file reading: " + e.getMessage());
            e.printStackTrace();
        }
    }


    protected abstract void fromSaveObject(String jsonString) throws JsonProcessingException;

    protected void putMapping(T mapping) {
        assert !mappings.containsKey(mapping.getUid());
        mappings.put(mapping.getUid(), mapping);
    }

    public void writeToFile() {
        if (suppressFileWriting) return;

        ObjectMapper objectMapper = new ObjectMapper();

        try {
            // Serialize the object to a formatted JSON string
            String jsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(toSaveObject());

            // Write the formatted JSON string to a file
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
                oos.writeObject(jsonString);
                System.out.println(getClass().getSimpleName() + " saved successfully to " + filePath);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing object to JSON", e);
        } catch (IOException e) {
            System.err.println(getClass().getSimpleName() + " - Error during file writing: " + e.getMessage());
            e.printStackTrace();
        }
    }

    protected abstract <T extends Serializable> T toSaveObject();
}
