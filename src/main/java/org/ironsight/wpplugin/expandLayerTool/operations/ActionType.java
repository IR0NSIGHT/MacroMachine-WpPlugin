package org.ironsight.wpplugin.expandLayerTool.operations;

public enum ActionType {
    INCREMENT("increment"), DECREMENT("decrement"), MULTIPLY("multiply with"), DIVIDE("divide by"), SET("set to"),
    MIN("limit to"), MAX("at least");

    public final String displayName;

    ActionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
