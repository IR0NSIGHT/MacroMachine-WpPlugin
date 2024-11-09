package org.ironsight.wpplugin.rivertool.geometry.BoundingBoxes;

import java.awt.*;
import java.util.Iterator;

public interface BoundingBox {
    boolean contains(Point p);
    BoundingBox expand(double size);
    Iterator<Point> areaIterator();
}
