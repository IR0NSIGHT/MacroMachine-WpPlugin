package org.ironsight.wpplugin.macromachine.operations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ironsight.wpplugin.macromachine.Gui.GlobalActionPanel;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;

public abstract class AbstractOperationContainer<T extends SaveableAction>
{
    private final ArrayList<Runnable> genericNotifies = new ArrayList<>();
    private final HashMap<UUID, ArrayList<Runnable>> uidNotifies = new HashMap<>();
    private final HashMap<UUID, T> mappings = new HashMap<>();
    private final Class<T> type;
    private final boolean suppressFileWriting = false;
    private final String defaultFileResourcePath;
    private String filePath;

    /**
     * copy constructor
     *
     * @param source
     */
    protected AbstractOperationContainer(AbstractOperationContainer source) {
        type = source.type;
        defaultFileResourcePath = source.defaultFileResourcePath;
        mappings.putAll(source.mappings);
        filePath = source.filePath;
    }

    public synchronized AbstractOperationContainer<T> copy() {
        assert false;
        return null;
    };

    protected AbstractOperationContainer(Class<T> type, String filePath, String defaultFileResourcePath) {
        this.type = type;
        this.filePath = filePath;
        this.defaultFileResourcePath = defaultFileResourcePath;
    }

    public static void ensureSaveFileExists(String saveFilePath, String defaultFileResourcePath)
            throws URISyntaxException, IOException {
        File saveFile = new File(saveFilePath);

        if (!saveFile.exists()) {
            try {
                // Path to the default macros file in the resources directory
                URL url = AbstractOperationContainer.class.getResource(defaultFileResourcePath);

                if (url == null) {
                    throw new IOException("Resource not found: " + defaultFileResourcePath);
                }

                // Copy the default macros file to the save file path
                try (InputStream inputStream = AbstractOperationContainer.class
                        .getResourceAsStream(defaultFileResourcePath)) {
                    if (inputStream != null) {
                        Files.copy(inputStream, saveFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        System.out.println("Copied the default resource file from " + url + " to " + saveFilePath);
                    } else {
                        throw new IOException("Failed to open stream for resource: " + defaultFileResourcePath);
                    }
                }
            } catch (IOException e) {
                GlobalActionPanel.ErrorPopUpString("Failed to copy the default resource file: " + e.getMessage());
            }
        } else {
            // error("Save file already exists at: " + saveFilePath);
            createBackup(saveFile.getPath());
        }
    }

    public static void createBackup(String filePath) {
        // Convert the file path to a Path object
        Path source = Paths.get(filePath);

        // Create a human-readable timestamp
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm");
        String timestamp = now.format(formatter);

        // Create the backup file path by appending the timestamp
        String backupFilePath = filePath + ".backup_" + timestamp;
        Path target = Paths.get(backupFilePath);

        try {
            // Copy the file to the backup location
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("Failed to create backup: " + e.getMessage());
        }
    }

    public synchronized String getFilePath() {
        return filePath;
    }

    public synchronized void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public synchronized void updateMapping(T mapping, Consumer<String> onError) {
        updateMapping(onError, mapping);
    }

    public synchronized T queryById(UUID uid) {
        return mappings.get(uid);
    }

    public synchronized boolean queryContains(UUID uuid) {
        return mappings.containsKey(uuid);
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

    public synchronized void deleteMapping(UUID... uid) {
        LinkedList<UUID> list = new LinkedList<>();
        for (UUID u : uid) {
            T removed = mappings.remove(u);
            if (removed != null) {
                list.add(removed.getUid());
            }
        }
        notify(list.toArray(new UUID[0]));
    }

    public synchronized void updateMapping(Consumer<String> onError, T... items) {
        UUID[] uids = new UUID[items.length];
        int idx = 0;
        for (T mapping : items) {
            if (mapping == null || mapping.getUid() == null) {
                onError.accept("mapping has null UID:" + mapping);
                continue;
            }

            // filter for identity
            if (!mappings.containsKey(mapping.getUid()) || queryById(mapping.getUid()).equals(mapping)) {
                mapping.getUid();
            }
            mappings.put(mapping.getUid(), mapping);
            uids[idx++] = mapping.getUid();
        }
        notify(Arrays.copyOf(uids, idx));
    }

    protected synchronized UUID getUUID() {
        return UUID.randomUUID();
    }

    public synchronized T addMapping(UUID uuid) {
        T newMap = getNewAction(uuid);
        mappings.put(newMap.getUid(), newMap);

        notify(newMap.getUid());
        return newMap;
    }

    public synchronized T addMapping() {
        T newMap = getNewAction();
        mappings.put(newMap.getUid(), newMap);

        notify(newMap.getUid()); // FIXME push this to graphics thread?
        return newMap;
    }

    public synchronized T addMapping(T item) {
        if (item.getUid() == null) {
            assert false : " items HAVE to have a UUID";
            return item;
        }
        mappings.put(item.getUid(), item);
        notify(item.getUid());
        return item;
    }

    protected synchronized T getNewAction() {
        return null;
    };

    protected synchronized T getNewAction(UUID uuid) {
        return null;
    };

    public synchronized void subscribe(Runnable runnable) {
        genericNotifies.add(runnable);
    }

    public synchronized void unsubscribe(Runnable runnable) {
        genericNotifies.remove(runnable);
    }

    public synchronized void subscribeToMapping(UUID uid, Runnable runnable) {
        ArrayList<Runnable> listeners = uidNotifies.getOrDefault(uid, new ArrayList<>());
        listeners.add(runnable);
        uidNotifies.put(uid, listeners);
    }

    public synchronized void unsubscribeToMapping(UUID uid, Runnable runnable) {
        ArrayList<Runnable> listeners = uidNotifies.getOrDefault(uid, null);
        if (listeners != null)
            listeners.remove(runnable);
    }

    public synchronized ArrayList<T> queryAll() {
        ArrayList<T> list = new ArrayList<>(mappings.values());
        return list;
    }

    public synchronized void readFromFile() {
        if (suppressFileWriting)
            return;
        try {
            ensureSaveFileExists(filePath, defaultFileResourcePath);
            mappings.clear();
            List<String> lines = null;
            lines = Files.readAllLines(Paths.get(filePath), StandardCharsets.UTF_8);
            String jsonString = String.join("", lines);
            fromSaveObject(jsonString);
        } catch (NoSuchFileException | FileNotFoundException e) {
            GlobalActionPanel.ErrorPopUp(e);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    protected synchronized void fromSaveObject(String jsonString) throws JsonProcessingException {
    };

    protected synchronized void putMapping(T mapping) {
        assert !mappings.containsKey(mapping.getUid());
        mappings.put(mapping.getUid(), mapping);
    }

    public synchronized void writeToFile() {
        if (suppressFileWriting)
            return;

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
            GlobalActionPanel.ErrorPopUp(e);
        }
    }

    protected synchronized <T extends Serializable> T toSaveObject() {
        return null;
    };
}
