package org.demo.wpplugin.geometry;

import java.awt.*;
import java.util.Iterator;

public interface BoundingBox {
    boolean contains(Point p);
    BoundingBox expand(double size);
    public Iterator<Point> areaIterator();
}
