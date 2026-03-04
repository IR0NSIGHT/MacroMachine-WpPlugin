package org.ironsight.wpplugin.rivertool.geometry.BoundingBoxes;

import java.awt.*;
import java.util.Collections;
import java.util.Iterator;

public class NeverBoundingBox extends AxisAlignedBoundingBox2d{
    public NeverBoundingBox(int id) {
        super(new Point(0,0), new Point(0,0), id);
    }

    @Override
    public boolean contains(Point p) {return false;}

    @Override
    public Iterator<Point> areaIterator() {
        return Collections.emptyIterator();
    }

    @Override
    public AxisAlignedBoundingBox2d expand(double size) {
        return this;
    }
}
