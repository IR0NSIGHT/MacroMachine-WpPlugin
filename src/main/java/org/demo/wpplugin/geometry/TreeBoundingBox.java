package org.demo.wpplugin.geometry;

import java.awt.*;
import java.util.Iterator;

public class TreeBoundingBox implements BoundingBox {
    final int sumChilds;
    private final BoundingBox leftChild;
    private final BoundingBox rightChild;

    public TreeBoundingBox(BoundingBox leftChild, BoundingBox rightChild) {
        this.leftChild = leftChild;
        this.rightChild = rightChild;
        this.sumChilds =
                (leftChild instanceof TreeBoundingBox ? ((TreeBoundingBox) leftChild).sumChilds : 1) + (rightChild instanceof TreeBoundingBox ? ((TreeBoundingBox) rightChild).sumChilds : 1);
    }

    @Override
    public boolean contains(Point p) {
        return leftChild.contains(p) || rightChild.contains(p);
    }

    @Override
    public BoundingBox expand(double size) {
        return new TreeBoundingBox(leftChild.expand(size), rightChild.expand(size));
    }

    @Override
    public Iterator<Point> areaIterator() {
        return new Iterator<Point>() {
            final Iterator<Point> leftChildIterator = leftChild.areaIterator();
            final Iterator<Point> rightChildIterator = rightChild.areaIterator();

            @Override
            public boolean hasNext() {
                return leftChildIterator.hasNext() || rightChildIterator.hasNext();
            }

            @Override
            public Point next() {
                if (leftChildIterator.hasNext()) {
                    return leftChildIterator.next();
                } else {
                    return rightChildIterator.next();
                }
            }
        };
    }

    @Override
    public String toString() {
        return "TreeBoundingBox{" +
                "childs=" + sumChilds +
                '}';
    }
}
