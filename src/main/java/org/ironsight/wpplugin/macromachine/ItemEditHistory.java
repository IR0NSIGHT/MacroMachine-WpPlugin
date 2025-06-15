package org.ironsight.wpplugin.macromachine;

import java.util.Stack;

public class ItemEditHistory<T> {
    public ItemEditHistory(int maxSteps) {
        this.maxSteps = maxSteps;
    }
    private final int maxSteps;
    private Stack<T> history = new Stack<>();

    public void clear() {};
    public T undo() {
        if (history.isEmpty())
            throw new RuntimeException("can not undo, history is empty.");
        return history.pop();
    }

    public boolean canUndo() {
        return !history.isEmpty();
    }

    public void doEdit(T step) {
        if (!history.isEmpty() && step.equals(history.peek()))
            return;
        history.push(step);
        if (history.size() >= maxSteps) {
            history.remove(maxSteps);
        }
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder("ItemHistory{\n");
        history.forEach(f -> b.append(f.toString()).append("\n"));
        b.append("}");
        return b.toString();
    }
}
