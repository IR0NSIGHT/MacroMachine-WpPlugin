package org.ironsight.wpplugin.macromachine.operations;

import org.ironsight.wpplugin.macromachine.operations.FileIO.ActionJsonWrapper;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.*;
import org.pepsoft.worldpainter.Dimension;

import javax.vecmath.Point2d;
import java.util.*;

import static org.ironsight.wpplugin.macromachine.operations.ProviderType.INTERMEDIATE_SELECTION;
import static org.ironsight.wpplugin.macromachine.operations.ProviderType.fromType;
/**
 * this class represents a single global-operation.
 * it can be applied to the map
 * each Actions has an input (f.e. terrain height) and an output (f.e. annotation)
 * the MappingPoints define which input value is mapped to which output value
 * f.e. ranges 62H to 85H -> Annotation.WHITE
 * Actions modifiy the output value, they can either Set the value (easiest) or do math. this is decided by the
 * action Type
 */
public class MappingAction implements SaveableAction {
    public final IPositionValueGetter input;

    public IPositionValueSetter getOutput() {
        return output;
    }

    public final IPositionValueSetter output;
    public final ActionType actionType;
    private final MappingPoint[] mappingPoints;
    private final String name;
    private final String description;
    private final UUID uid;    //TODO make final and private
    private final int[] mappings;
    public static MappingAction getNewEmptyAction(UUID id) {
        return new MappingAction(new TerrainHeightIO(-64,319),
                new AnnotationSetter(),
                new MappingPoint[0], ActionType.SET, "create new action", "new description",id);
    }
    public static MappingAction getNewEmptyAction() {
       return getNewEmptyAction(null);
    }
    public MappingAction(IPositionValueGetter input, IPositionValueSetter output, MappingPoint[] mappingPoints,
                         ActionType type, String name, String description, UUID uid) {
        assert name != null;
        assert description != null;
        assert input != null;
        assert output != null;
        assert mappingPoints != null;
        assert type != null;
        assert Arrays.stream(mappingPoints).noneMatch(Objects::isNull);


        this.name = name;
        this.description = description;
        this.input = input;
        this.output = output;
        this.actionType = type;
        this.uid = uid;

        //filter out illegal mapping points (user might have edited save file)
        mappingPoints =
                Arrays.stream(mappingPoints)
                        .filter(p -> sanitizeInput(p.input) == p.input) //input points
                        .map(p -> new MappingPoint(p.input,sanitizeOutput(p.output)))
                        .toArray(MappingPoint[]::new);

        assert Arrays.stream(mappingPoints).noneMatch(p -> sanitizeInput(p.input) != p.input) : "mapping points " +
                "contain illegal input values";
        assert Arrays.stream(mappingPoints).noneMatch(p -> sanitizeOutput(p.output) != p.output) : "mapping points " +
                "contain illegal output values";
        this.mappingPoints = Arrays.stream(mappingPoints)
                // .map(mp -> new MappingPoint(sanitizeInput(mp.input), sanitizeOutput(mp.output)))
                .sorted(Comparator.comparing(mp -> mp.input)).toArray(MappingPoint[]::new);

        //direct mapping: mappings[input] = output. fast and reliable
        this.mappings = new int[input.getMaxValue() + 1 - input.getMinValue()];
        if (output.isDiscrete()) {  // DO NOT INTERPOLATE OUTPUT
            if (this.mappingPoints.length == 0) return;
            int j = -1;
            for (int i = input.getMinValue(); i <= input.getMaxValue(); i++) {
                int mappingPointIdx = Math.max(Math.min(j + 1, mappingPoints.length - 1), 0);
                MappingPoint point = mappingPoints[mappingPointIdx];
                if (point.input == i) {
                    j++;
                    point = mappingPoints[Math.max(j, 0)];
                }

                mappings[i - input.getMinValue()] = point.output;
            }
        } else {
            if (this.mappingPoints.length == 0) return;
            if (this.mappingPoints.length == 1) {
                Arrays.fill(mappings, mappingPoints[0].output);
                return;
            }
            ;
            // 2 or more mapping points are enough to interpolate

            //prepare input points by adding min-input and max-input points for easier index finding of interpolation
            LinkedList<MappingPoint> points = new LinkedList<>(Arrays.asList(mappingPoints));
            if (points.getFirst().input != input.getMinValue())
                points.addFirst(new MappingPoint(input.getMinValue(), points.getFirst().output));

            if (points.getLast().input != input.getMaxValue())
                points.addLast(new MappingPoint(input.getMaxValue(), points.getLast().output));
            assert points.size() >= 2 : "at least min and max points on the input scale are defined";

            for (int i = 0; i < points.size() - 1; i++) {
                MappingPoint low = points.get(i);
                MappingPoint high = points.get(i + 1);
                mappings[low.input - input.getMinValue()] = low.output;
                int range = (high.input - low.input);
                for (int inputValue = low.input + 1; inputValue <= high.input; inputValue++) {
                    float t = (1f * inputValue - low.input) / range;
                    int outputValue = Math.round((1 - t) * low.output + (t) * high.output);

                    if (inputValue > input.getMaxValue()) return;
                    mappings[inputValue - input.getMinValue()] = outputValue;
                }
            }
        }

    }

    public static MappingAction fromJsonWrapper(ActionJsonWrapper wrapper) {
        IPositionValueGetter input = (IPositionValueGetter) fromType(wrapper.getInputData(), wrapper.getInputId());
        IPositionValueSetter output = (IPositionValueSetter) fromType(wrapper.getOutputData(), wrapper.getOutputId());

        assert input != null;
        assert output != null;
        MappingPoint[] points = new MappingPoint[wrapper.getInputPoints().length];
        for (int i = 0; i < points.length; i++) {
            points[i] = new MappingPoint(wrapper.getInputPoints()[i], wrapper.getOutputPoints()[i]);
        }
        MappingAction mapping = new MappingAction(input,
                output,
                points,
                wrapper.getActionType(),
                wrapper.getName(),
                wrapper.getDescription(),
                wrapper.getUid());
        return mapping;
    }

    public static List<Point2d> calculateRanges(MappingAction mapping) {
        LinkedList<Point2d> ranges = new LinkedList<>();
        int previousOutput = mapping.map(mapping.input.getMinValue());
        int previousInput = mapping.input.getMinValue();
        for (int i = mapping.input.getMinValue(); i <= mapping.input.getMaxValue(); i++) {
            if (i == mapping.input.getMaxValue()) {
                ranges.add(new Point2d(previousInput, i));
            }
            if (mapping.map(i) != previousOutput) {
                ranges.add(new Point2d(previousInput, i - 1));
                previousOutput = mapping.map(i);
                previousInput = i;
            }
        }
        return ranges;
    }

    public IPositionValueGetter getInput() {
        return input;
    }

    public MappingAction withInput(IPositionValueGetter input) {
        return new MappingAction(input, output, mappingPoints, actionType, name, description, uid);
    }

    public MappingAction withValuesFrom(MappingAction other) {
        return new MappingAction(other.input, other.output, other. mappingPoints,  other.actionType,  other.name,
                other.description, this.uid);
    }

    public MappingAction withOutput(IPositionValueSetter output) {
        return new MappingAction(input, output, mappingPoints, actionType, name, description, uid);
    }

    public MappingAction withType(ActionType actionType) {
        return new MappingAction(input, output, mappingPoints, actionType, name, description, uid);
    }

    public MappingAction withName(String name) {
        return new MappingAction(input, output, mappingPoints, actionType, name, description, uid);
    }

    public MappingAction withDescription(String description) {
        return new MappingAction(input, output, mappingPoints, actionType, name, description, uid);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(input, output, actionType, name, description);
        result = 31 * result + Arrays.hashCode(mappingPoints);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MappingAction mapping = (MappingAction) o;
        return Objects.equals(input, mapping.input) && Objects.equals(output, mapping.output) &&
                actionType == mapping.actionType && Arrays.equals(mappingPoints, mapping.mappingPoints) &&
                Objects.equals(name, mapping.name) && Objects.equals(description, mapping.description) &&
                Objects.equals(this.getUid(), mapping.getUid());
    }

    public boolean equalIgnoreUUID(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MappingAction mapping = (MappingAction) o;
        return Objects.equals(input, mapping.input) && Objects.equals(output, mapping.output) &&
                actionType == mapping.actionType && Arrays.equals(mappingPoints, mapping.mappingPoints) &&
                Objects.equals(name, mapping.name) && Objects.equals(description, mapping.description);
    }

    public UUID getUid() {
        return uid;
    }

    @Override
    public String toString() {
        return "LayerMapping{" + "name='" + name + '\'' + ", uid=" + uid + ", input=" + input + ", output=" + output +
                ", actionType=" + actionType + '}';
    }

    public MappingPoint[] getMappingPoints() {
        return mappingPoints.clone();
    }

    private MappingPoint sanitize(MappingPoint p) {
        return new MappingPoint(sanitizeInput(p.input), sanitizeOutput(p.output));
    }

    public MappingAction withNewPoints(MappingPoint[] mappingPoints) {
        mappingPoints = Arrays.stream(mappingPoints)
                .map(this::sanitize)
                .toArray(MappingPoint[]::new);

        TreeSet<MappingPoint> newPoints = new TreeSet<>(Comparator.comparingInt(o -> o.input));
        newPoints.addAll(Arrays.asList(mappingPoints));
        return new MappingAction(this.input,
                this.output,
                newPoints.toArray(new MappingPoint[0]),
                this.getActionType(),
                this.getName(),
                this.getDescription(),
                this.uid);
    }

    public ActionType getActionType() {
        return actionType;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getToolTipText() {
        return input.getName() +" " + actionType.displayName + " " + output.getName();
    }

    public void applyToPoint(Dimension dim, int x, int y) {
        if (!output.getProviderType().equals(INTERMEDIATE_SELECTION) && !ActionFilterIO.instance.isSelected())
            return;

        if (mappingPoints.length == 0) {
            return;
        }
        int value = (int)EditableIO.clamp( input.getValueAt(dim, x, y), input.getMinValue(), input.getMaxValue());

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
            case LIMIT_TO:
                outputValue = Math.min(existingValue, modifier);
                break;
            case AT_LEAST:
                outputValue = Math.max(existingValue, modifier);
                break;
            default:
                throw new EnumConstantNotPresentException(ActionType.class, actionType.displayName);
        }
        output.setValueAt(dim, x, y, this.sanitizeOutput(outputValue));
    }

    private boolean hasValueForInput(int input) {
        return Arrays.stream(mappingPoints).anyMatch(p -> p.input == input);
    }

    public int map(int input) {
        assert input >= this.input.getMinValue() :
                "input " + input + " is out of range for minimum" + this.input.getMinValue();
        assert input <= this.input.getMaxValue() :
                "invalid input" + input + " has to be lower equal than " + this.input.getMaxValue();

        int value = mappings[input - this.input.getMinValue()];
        return value;
    }

    public boolean isIllegalValue(int value, boolean input) {
        if (input) return value == sanitizeInput(value);
        else return value == sanitizeOutput(value);
    }

    public int sanitizeInput(int value) {
        return Math.min(input.getMaxValue(), Math.max(input.getMinValue(), value));
    }

    public int sanitizeOutput(int value) {
        return Math.min(output.getMaxValue(), Math.max(output.getMinValue(), value));
    }

}
