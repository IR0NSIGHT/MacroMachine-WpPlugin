package org.ironsight.wpplugin.macromachine.operations;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UniqueList<T> {
    private List<T> list;
    private Set<T> set;

    public UniqueList() {
        this.list = new ArrayList<>();
        this.set = new HashSet<>();
    }

    // Method to add an item to the list if it's not already present
    public boolean add(T item) {
        if (!set.contains(item)) {
            list.add(item);
            set.add(item);
            return true;
        }
        return false;
    }

    // Method to get the list of items
    public List<T> getList() {
        return new ArrayList<>(list);
    }

    public static void main(String[] args) {
        UniqueList<String> uniqueList = new UniqueList<>();

        System.out.println(uniqueList.add("Apple")); // true
        System.out.println(uniqueList.add("Banana")); // true
        System.out.println(uniqueList.add("Apple")); // false, duplicate item

        System.out.println(uniqueList.getList()); // Output: [Apple, Banana]
    }
}
