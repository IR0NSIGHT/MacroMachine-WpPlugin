package org.demo.wpplugin.pathing;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public abstract class InformedPath<T> {
    private final ArrayList<T> information;

    public InformedPath(List<Point> points) {
        information = new ArrayList<>(points.size());
    }

    public T informationByIndex(int index) {
        if (index < 0 || index >= this.information.size())
            throw new IndexOutOfBoundsException();
        return this.information.get(index);
    }

    public void setInformationByIndex(T information, int index) {
        if (index < 0 || index >= this.information.size())
            throw new IndexOutOfBoundsException();
        this.information.set(index, information);
    }
}
