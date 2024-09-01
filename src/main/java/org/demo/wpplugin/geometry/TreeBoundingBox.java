package org.demo.wpplugin.geometry;

import java.awt.*;
import java.util.Iterator;

public class TreeBoundingBox implements BoundingBox {
    private final BoundingBox[] boundingBoxes;

    public TreeBoundingBox(BoundingBox[] boundingBoxes) {
        this.boundingBoxes = boundingBoxes;
    }

    @Override
    public boolean contains(Point p) {
        return false;
    }

    @Override
    public BoundingBox expand(double size) {
        return null;
    }

    @Override
    public Iterator<Point> areaIterator() {
        return null;
    }
}
