package org.demo.wpplugin.geometry.BoundingBoxes;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class TreeBoundingBox extends AxisAlignedBoundingBox2d {
    final int sumChilds;
    private final AxisAlignedBoundingBox2d leftChild;
    private final AxisAlignedBoundingBox2d rightChild;

    public TreeBoundingBox(AxisAlignedBoundingBox2d leftChild, AxisAlignedBoundingBox2d rightChild) {
        super(Arrays.asList(leftChild.minPoint, leftChild.maxPoint, rightChild.minPoint, rightChild.maxPoint),-1);
        this.leftChild = leftChild;
        this.rightChild = rightChild;
        this.sumChilds =
                (leftChild instanceof TreeBoundingBox ? ((TreeBoundingBox) leftChild).sumChilds : 1) + (rightChild instanceof TreeBoundingBox ? ((TreeBoundingBox) rightChild).sumChilds : 1);
    }

    public static TreeBoundingBox constructTree(Collection<AxisAlignedBoundingBox2d> neighbouringBoxes) {
        if (neighbouringBoxes.size() == 1) {
            neighbouringBoxes.add(new NeverBoundingBox(-1));
        } else if (neighbouringBoxes.size() == 0) {
            throw new IllegalArgumentException("will not construct tree for zero length list");
        }
        assert neighbouringBoxes.size() >= 2;
        java.util.List<AxisAlignedBoundingBox2d> oldList = new ArrayList<>(neighbouringBoxes);
        while (oldList.size() > 1) {
            List<AxisAlignedBoundingBox2d> newList = new ArrayList<>(oldList.size() / 2 + 1);
            for (int i = 0; i < oldList.size(); i++) {
                if (i < oldList.size() - 1) {
                    TreeBoundingBox parent = new TreeBoundingBox(oldList.get(i), oldList.get(i + 1));
                    newList.add(parent);
                    i++;
                } else {
                    newList.add(oldList.get(i));
                }
            }
            oldList = newList;
        }
        assert oldList.size() == 1;
        return (TreeBoundingBox) oldList.get(0);
    }

    @Override
    public boolean contains(Point p) {
        if (!super.contains(p)) {
            return false;
        }
        boolean isInChildren = (leftChild.contains(p) || rightChild.contains(p));
        return isInChildren;
    }

    public void collectContainingAABBxsIds(Point p, Collection<Integer> out) {
        if (!super.contains(p))
            return;
        if (leftChild instanceof TreeBoundingBox) {
            ((TreeBoundingBox) leftChild).collectContainingAABBxsIds(p, out);
        } else if (leftChild.contains(p))
            out.add(leftChild.id);

        if (rightChild instanceof TreeBoundingBox) {
            ((TreeBoundingBox) rightChild).collectContainingAABBxsIds(p, out);
        } else if (rightChild.contains(p))
            out.add(rightChild.id);
    }

    @Override
    public TreeBoundingBox expand(double size) {
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
                super.toString() +
                "childs=" + sumChilds +
                '}';
    }
}
