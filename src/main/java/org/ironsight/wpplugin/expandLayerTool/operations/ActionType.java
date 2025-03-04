package org.ironsight.wpplugin.expandLayerTool.operations;

public enum ActionType {
    INCREMENT("adds"),
    DECREMENT("subtracts"),
    MULTIPLY("multiplies"),
    DIVIDE("divides"),
    SET("sets"),
    LIMIT_TO("limits"),
    AT_LEAST("sets minimum");

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
