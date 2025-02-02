package org.ironsight.wpplugin.expandLayerTool.operations;

import org.ironsight.wpplugin.expandLayerTool.operations.ValueProviders.IDisplayUnit;
import org.ironsight.wpplugin.expandLayerTool.operations.ValueProviders.IPositionValueGetter;
import org.ironsight.wpplugin.expandLayerTool.operations.ValueProviders.IPositionValueSetter;
import org.pepsoft.worldpainter.Dimension;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;

public class LayerMapping implements IDisplayUnit, Serializable {
    public final IPositionValueGetter input;
    public final IPositionValueSetter output;
    public final ActionType actionType;
    private final MappingPoint[] mappingPoints;
    private final String name;
    private final String description;
    private final UUID uid;    //TODO make final and private

    public LayerMapping(IPositionValueGetter input, IPositionValueSetter output, MappingPoint[] mappingPoints,
                        ActionType type, String name, String description, UUID uid) {
        this.name = name;
        this.description = description;
        this.input = input;
        this.output = output;
        this.mappingPoints = mappingPoints.clone();
        this.actionType = type;
        this.uid = uid;
        Arrays.sort(this.mappingPoints, Comparator.comparing(mp -> mp.input));
    }

    public LayerMapping withInput(IPositionValueGetter input) {
        return new LayerMapping(input, output, mappingPoints, actionType, name, description, uid);
    }

    public LayerMapping withOutput(IPositionValueSetter output) {
        return new LayerMapping(input, output, mappingPoints, actionType, name, description, uid);
    }

    public LayerMapping withType(ActionType actionType) {
        return new LayerMapping(input, output, mappingPoints, actionType, name, description, uid);
    }

    public LayerMapping withName(String name) {
        return new LayerMapping(input, output, mappingPoints, actionType, name, description, uid);
    }

    public LayerMapping withDescription(String description) {
        return new LayerMapping(input, output, mappingPoints, actionType, name, description, uid);
    }

    public UUID getUid() {
        return uid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LayerMapping mapping = (LayerMapping) o;
        return Objects.equals(input, mapping.input) && Objects.equals(output, mapping.output) &&
                actionType == mapping.actionType && Arrays.equals(mappingPoints, mapping.mappingPoints) &&
                Objects.equals(name, mapping.name) && Objects.equals(description, mapping.description) &&
                Objects.equals(this.getUid(), mapping.getUid());
    }

    @Override
    public String toString() {
        return "LayerMapping{" + "name='" + name + '\'' + ", uid=" + uid + ", input=" + input + ", output=" + output +
                ", actionType=" + actionType + '}';
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(input, output, actionType, name, description);
        result = 31 * result + Arrays.hashCode(mappingPoints);
        return result;
    }

    public MappingPoint[] getMappingPoints() {
        return mappingPoints;
    }

    public LayerMapping withNewPoints(MappingPoint[] mappingPoints) {
        return new LayerMapping(this.input,
                this.output,
                mappingPoints,
                this.getActionType(),
                this.getName(),
                this.getDescription(),
                this.uid);
    }

    public ActionType getActionType() {
        return actionType;
    }

    public void applyToPoint(Dimension dim, int x, int y) {
        int value = input.getValueAt(dim, x, y);
        int modifier = map(value);

        int existingValue =
                output instanceof IPositionValueGetter ? ((IPositionValueGetter) output).getValueAt(dim, x, y) : 0;
        int outputValue;
        switch (actionType) {
            case SET:
                outputValue = modifier;
                break;
            case DIVIDE:
                outputValue = Math.round(1f * existingValue / modifier);
                break;
            case MULTIPLY:
                outputValue = existingValue * modifier;
                break;
            case DECREMENT:
                outputValue = existingValue - modifier;
                break;
            case INCREMENT:
                outputValue = existingValue + modifier;
                break;
            case MIN:
                outputValue = Math.min(existingValue, modifier);
                break;
            case MAX:
                outputValue = Math.max(existingValue, modifier);
                break;
            default:
                throw new EnumConstantNotPresentException(ActionType.class, actionType.displayName);
        }

        output.setValueAt(dim, x, y, outputValue);
    }

    int map(int input) {    //TODO do linear interpolation
        if (input < mappingPoints[0].input) return mappingPoints[0].output;
        for (int i = 0; i < mappingPoints.length - 1; i++) {
            if (mappingPoints[i].input <= input && mappingPoints[i + 1].input > input) {  //value inbetween i and i+1
                if (output.isDiscrete()) {
                    return mappingPoints[i + 1].output;
                } else {
                    int a = mappingPoints[i].input;
                    int b = mappingPoints[i + 1].input;
                    int dist = b - a;
                    float t = ((float) input - a) / dist;
                    float interpol = (1 - t) * mappingPoints[i].output + t * mappingPoints[i + 1].output;
                    return Math.round(interpol);
                }

            }
        }
        //no match, return highest value
        return mappingPoints[mappingPoints.length - 1].output;
    }

    public int sanitizeInput(int value) {
        return Math.min(input.getMaxValue(), Math.max(input.getMinValue(), value));
    }

    public int sanitizeOutput(int value) {
        return Math.min(output.getMaxValue(), Math.max(output.getMinValue(), value));
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

}
