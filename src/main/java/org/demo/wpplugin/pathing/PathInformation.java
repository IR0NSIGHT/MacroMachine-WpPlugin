package org.demo.wpplugin.pathing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

public abstract class PathInformation<T> {
    private final HashMap<Integer, T> information;

    public PathInformation(int size) {
        information = new HashMap<>(size);
    }

    public T informationByIndex(int index) {
        return this.information.get(index);
    }

    public void setInformationByIndex(T information, int index) {
        //allow automatically growing
        this.information.put(index, information);
    }
}
