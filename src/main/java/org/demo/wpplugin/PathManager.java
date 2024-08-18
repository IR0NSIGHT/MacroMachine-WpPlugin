package org.demo.wpplugin;

import java.util.HashMap;

public class PathManager {

    private HashMap<Integer, Path> pathById = new HashMap<>();
    public Path getPathBy(int id) throws IllegalArgumentException {
        if (!pathById.containsKey(id))
            throw new IllegalArgumentException("no path with this id exists:" + id);
        return pathById.get(id);
    }

    public void setPathBy(int id, Path path) throws IllegalArgumentException {
        if (path == null)
            throw new IllegalArgumentException("can not add null Path to path map.");
        pathById.put(id, path);
    }

    public static final PathManager instance = new PathManager();
}

