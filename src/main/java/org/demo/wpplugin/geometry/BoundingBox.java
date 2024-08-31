package org.demo.wpplugin.geometry;

import java.awt.*;

public interface BoundingBox {
    boolean contains(Point p);
    BoundingBox expand(double size);
}
