package org.demo.wpplugin.pathing;

import org.demo.wpplugin.operations.River.RiverHandleInformation;

import java.util.*;

public class PathManager {
    public static final PathManager instance = new PathManager();
    private final HashMap<Integer, Path> pathById = new HashMap<>();
    private final HashMap<Integer, String> pathNames = new HashMap<>();
    private int nextPathId = 0;

    public PathManager() {
        int i = 100;
        addPath(new Path(Arrays.asList(
                RiverHandleInformation.riverInformation(i*-1-50, i, 3, 2,3,20,200),
                RiverHandleInformation.riverInformation(i*-1, i),
                RiverHandleInformation.riverInformation(i*-0, -i),
                RiverHandleInformation.riverInformation(i*1, i),
                RiverHandleInformation.riverInformation(i*2, -i),
                RiverHandleInformation.riverInformation(i*3, i),
                RiverHandleInformation.riverInformation(i*3+50, i,7,4,10,0,-1)
                ), PointInterpreter.PointType.RIVER_2D));
    }

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
        nameExistingPath(nextPathId, "Path-" + nextPathId);
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
            names.add(new NamedId(e.getKey(), e.getValue(), getPathBy(e.getKey()).type));
        }
        return names;
    }

    public NamedId getPathName(int pathId) {
        if (!pathNames.containsKey(pathId))
            throw new IllegalArgumentException("can not get name of non existent path");
        return new NamedId(pathId, pathNames.get(pathId), getPathBy(pathId).type);
    }

    public static class NamedId {
        public final int id;
        public final String name;
        public final PointInterpreter.PointType type;

        public NamedId(int id, String name, PointInterpreter.PointType type) {
            this.id = id;
            this.type = type;
            this.name = name;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof NamedId && ((NamedId) obj).id == id;
        }

        @Override
        public String toString() {
            return name + ": " + type + "";
        }
    }
}

