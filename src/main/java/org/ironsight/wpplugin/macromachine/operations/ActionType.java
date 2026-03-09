package org.ironsight.wpplugin.macromachine.operations;

import org.ironsight.wpplugin.macromachine.Gui.MappingPointValue;

/**
 * defines how a MappingAction influences its output. SET: overwrite value, dont care what it was before INCREMENT: take existing value, add mapped value on top DECREMENT: increment but
 * negative MULTIPLY: see above DIVIDE: see above LIMIT_TO: take existing value and mapped value, choose the lower number. effecitlvy a "min" operation AT_LEAST: take existing value and
 * mapped value, choose bigger number. "max" operation
 * <p>
 * note: not all action types make sense for all MappingActions, because they are math operations but not all input/outputs are considered numbers by the user: Colors, ActionFilter
 * (true/false) etc.
 */

public enum ActionType {
    INCREMENT("increments"), DECREMENT("subtracts"), MULTIPLY("multiplies"), DIVIDE("divides"), SET("sets"), LIMIT_TO("limits"), AT_LEAST("sets minimum");

    public final String displayName;

    ActionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getExplanationFor(MappingPointValue mpv) {
        int numericValue = mpv.numericValue;
        String valueS = "'" + mpv.mappingValue.valueToString(mpv.numericValue) + "'";
        String setterGetter = mpv.mappingValue.getName();

        StringBuilder sb = new StringBuilder();

        switch (this) {
            case AT_LEAST -> {
                sb.append(setterGetter).append(" is set to be at least ")

                        .append(numericValue);

            }

            case MULTIPLY -> {
                sb.append(setterGetter).append(" is multiplied by ")


                        .append(numericValue);
            }

            case DIVIDE -> {
                sb.append(setterGetter).append(" is divided by ")


                        .append(numericValue);
            }

            case INCREMENT -> {
                sb.append(setterGetter).append(" is increased by ").append(mpv.numericValue >= 0 ? "+" : "")

                        .append(numericValue);
            }

            case DECREMENT -> {
                sb.append(setterGetter).append(" is decreased by ").append(mpv.numericValue >= 0 ? "+" : "")

                        .append(numericValue);
            }

            case LIMIT_TO -> {
                sb.append(setterGetter).append(" is limited to ")

                        .append(numericValue)

                ;
            }

            case SET -> {
                sb.append(setterGetter).append(" is set to ").append(valueS);
            }

            default -> {
            }
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return displayName;
    }
}
