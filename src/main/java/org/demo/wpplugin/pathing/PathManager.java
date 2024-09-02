package org.demo.wpplugin.pathing;

import java.awt.*;
import java.util.*;

public class PathManager {
    public PathManager() {
        addPath(new Path(Arrays.asList(new Point(0, 0), new Point(1, 1), new Point(100, 50), new Point(100, 150), new Point(101, 151))));
    }
    private int nextPathId = 0;
    public static final PathManager instance = new PathManager();
    private HashMap<Integer, Path> pathById = new HashMap<>();
    private HashMap<Integer, String> pathNames = new HashMap<>();

    public Path getPathBy(int id) throws IllegalArgumentException {
        if (!pathById.containsKey(id))
            throw new IllegalArgumentException("no path with this id exists:" + id);
        return pathById.get(id);
    }

    public void setPathBy(int id, Path path) throws IllegalArgumentException {
        if (path == null)
            throw new IllegalArgumentException("can not add null Path to path map.");
        if (!pathById.containsKey(id))
            throw new IllegalArgumentException("this path doesnt exist.");

        pathById.put(id, path);
    }

    public int addPath(Path path) {
        pathById.put(++nextPathId, path);
        setPathBy(nextPathId, path);
        nameExistingPath(nextPathId,"Path-"+nextPathId);
        return nextPathId;
    }

    public int getAnyValidId() {
        return pathById.keySet().iterator().next();
    }

    public void nameExistingPath(int id, String name) throws IllegalArgumentException {
        if (!pathById.containsKey(id))
            throw new IllegalArgumentException("can not rename non existent path");
        pathNames.put(id, name);
    }

    public Collection<NamedId> allPathNamedIds() {
        Collection<NamedId> names = new LinkedList<>();
        for (Map.Entry<Integer, String> e : pathNames.entrySet()) {
            names.add(new NamedId(e.getKey(), e.getValue()));
        }
        return names;
    }

    public NamedId getPathName(int pathId) {
        if (!pathNames.containsKey(pathId))
            throw new IllegalArgumentException("can not get name of non existent path");
        return new NamedId(pathId, pathNames.get(pathId));
    }

    public static class NamedId {
        public final int id;
        public final String name;

        public NamedId(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof NamedId && ((NamedId) obj).id == id;
        }

        @Override
        public String toString() {
            return name + "(" + id + ")";
        }
    }
}

