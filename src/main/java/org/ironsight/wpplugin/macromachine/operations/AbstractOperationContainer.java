package org.ironsight.wpplugin.macromachine.operations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Consumer;

public abstract class AbstractOperationContainer<T extends SaveableAction> {
    private final ArrayList<Runnable> genericNotifies = new ArrayList<>();
    private final HashMap<UUID, ArrayList<Runnable>> uidNotifies = new HashMap<>();
    private final HashMap<UUID, T> mappings = new HashMap<>();
    private final Class<T> type;
    private final boolean suppressFileWriting = false;
    private final String defaultFileResourcePath;
    private String filePath;

    protected AbstractOperationContainer(Class<T> type, String filePath, String defaultFileResourcePath) {
        this.type = type;
        this.filePath = filePath;
        this.defaultFileResourcePath = defaultFileResourcePath;
        System.out.println("container" + type + " path="+filePath + " default resource=" + defaultFileResourcePath);
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void updateMapping(T mapping, Consumer<String> onError) {
        //filter for identity
        if (!mappings.containsKey(mapping.getUid()) || queryById(mapping.getUid()).equals(mapping)) {
            mapping.getUid();
        }
        mappings.put(mapping.getUid(), mapping);
        notify(mapping.getUid());
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
            if (removed != null) {
                list.add(removed.getUid());
            }
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

    public static void ensureSaveFileExists(String saveFilePath, String defaultFileResourcePath) {
        File saveFile = new File(saveFilePath);

        if (!saveFile.exists()) {
            try {
                // Path to the default macros file in the resources directory
                URL url = AbstractOperationContainer.class.getResource(defaultFileResourcePath);
                assert url != null : "Resource not found: " + defaultFileResourcePath;
                Path defaultMacrosPath = Paths.get(url.toURI());

                // Copy the default macros file to the save file path
                Files.copy(defaultMacrosPath, saveFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                System.err.println("copy the default resource file from " + defaultFileResourcePath + " to " + saveFilePath);

            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Failed to copy the default resource file from " + defaultFileResourcePath);
            }
        } else {
            System.out.println("Save file already exists at: " + saveFilePath);
        }
    }

    public void readFromFile() {
        if (suppressFileWriting) return;
        ensureSaveFileExists(filePath, defaultFileResourcePath);
        mappings.clear();
        List<String> lines = null;
        try {
            lines = Files.readAllLines(Paths.get(filePath), StandardCharsets.UTF_8);
            String jsonString = String.join("", lines);
            fromSaveObject(jsonString);
        }
        catch (FileNotFoundException e) {
            System.err.println("save file not found: " + filePath);
        } catch (JsonProcessingException e) {
            System.err.println(filePath);
            System.err.println(getClass().getSimpleName() + " - Error during file reading: " + e.getMessage());
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
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
            Object o = toSaveObject();
            String jsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(o);

            // Write the formatted JSON string to a file using UTF-8 encoding
            Files.write(Paths.get(filePath), jsonString.getBytes(StandardCharsets.UTF_8));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing object to JSON", e);
        } catch (IOException e) {
            System.err.println(getClass().getSimpleName() + " - Error during file writing: " + e.getMessage());
            e.printStackTrace();
        }
    }

    protected abstract <T extends Serializable> T toSaveObject();
}
