package org.ironsight.wpplugin.macromachine.operations;

/**
 * defines how a MappingAction influences its output.
 * SET: overwrite value, dont care what it was before
 * INCREMENT: take existing value, add mapped value on top
 * DECREMENT: increment but negative
 * MULTIPLY: see above
 * DIVIDE: see above
 * LIMIT_TO: take existing value and mapped value, choose the lower number. effecitlvy a "min" operation
 * AT_LEAST: take existing value and mapped value, choose bigger number. "max" operation
 *
 * note: not all action types make sense for all MappingActions, because they are math operations but not all
 * input/outputs are considered numbers by the user: Colors, ActionFilter (true/false) etc.
 *
 */

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
