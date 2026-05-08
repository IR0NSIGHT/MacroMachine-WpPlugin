package org.ironsight.wpplugin.macromachine.REST.DTOs;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.ironsight.wpplugin.macromachine.operations.ActionType;
import org.ironsight.wpplugin.macromachine.operations.MappingAction;
import org.ironsight.wpplugin.macromachine.operations.MappingPoint;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

@Schema(description = "Represents an executable mapping action, that reads one inputtype from the map and based on that writes output values to the map.")
public class ActionDTO
{

    @Schema(description = "Input", requiredMode = Schema.RequiredMode.REQUIRED)
    private final InputOutputDTO input;

    @Schema(description = "Output", requiredMode = Schema.RequiredMode.REQUIRED)
    private final InputOutputDTO output;

    @Schema(description = "Type of action", requiredMode = Schema.RequiredMode.REQUIRED)
    private final ActionType actionType;

    @Schema(description = "Human readable name", requiredMode = Schema.RequiredMode.REQUIRED)
    private final String name;

    @Schema(description = "Description of the action", requiredMode = Schema.RequiredMode.REQUIRED)
    private final String description;

    @Schema(description = "Unique identifier", requiredMode = Schema.RequiredMode.REQUIRED)
    private final UUID uid;

    @Schema(description = "numeric input values for the mapping points", requiredMode = Schema.RequiredMode.REQUIRED)
    private final int[] mappingPointsX;

    @Schema(description = "numeric output values for the mapping points", requiredMode = Schema.RequiredMode.REQUIRED)
    private final int[] mappingPointsY;

    @Schema(description = "actual numeric input values for all inputs", requiredMode = Schema.RequiredMode.REQUIRED)
    private final int[] mappedInputs;

    @Schema(description = "actual numeric output values for all inputs", requiredMode = Schema.RequiredMode.REQUIRED)
    private final int[] mappedOutputs;

    public ActionDTO(MappingAction action) {
        this(InputOutputDTO.fromInputGetter(action.getInput()), InputOutputDTO.fromOutputSetter(action.getOutput()),
                action.getActionType(), action.getName(), action.getDescription(), action.getUid(),
                Arrays.stream(action.getMappingPoints()).mapToInt(p -> p.input).toArray(),
                Arrays.stream(action.getMappingPoints()).mapToInt(p -> p.output).toArray(),
                Arrays.stream(action.getInput().getAllInputValues()).map(action::map).toArray(),
                Arrays.stream(action.getInput().getAllInputValues()).toArray());
    }

    @JsonCreator
    public ActionDTO(@JsonProperty("input") InputOutputDTO input, @JsonProperty("output") InputOutputDTO output,
            @JsonProperty("actionType") ActionType actionType, @JsonProperty("name") String name,
            @JsonProperty("description") String description, @JsonProperty("uid") UUID uid,
            @JsonProperty("mappingPointsX") int[] mappingPointsX, @JsonProperty("mappingPointsY") int[] mappingPointsY,
            @JsonProperty("mappedOutputs") int[] mappedOutputs, @JsonProperty("mappedInputs") int[] mappedInputs) {
        this.input = input;
        this.output = output;
        this.actionType = actionType;
        this.name = name;
        this.description = description;
        this.uid = uid;
        this.mappingPointsX = mappingPointsX;
        this.mappingPointsY = mappingPointsY;
        this.mappedOutputs = mappedOutputs;
        this.mappedInputs = mappedInputs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ActionDTO actionDTO = (ActionDTO) o;
        return Objects.equals(getInput(), actionDTO.getInput()) && Objects.equals(getOutput(), actionDTO.getOutput())
                && getActionType() == actionDTO.getActionType() && Objects.equals(getName(), actionDTO.getName())
                && Objects.equals(getDescription(), actionDTO.getDescription())
                && Objects.equals(getUid(), actionDTO.getUid())
                && Arrays.equals(getMappingPointsX(), actionDTO.getMappingPointsX())
                && Arrays.equals(getMappingPointsY(), actionDTO.getMappingPointsY())
                && Arrays.equals(mappedInputs, actionDTO.mappedInputs)
                && Arrays.equals(getMappedOutputs(), actionDTO.getMappedOutputs());
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(getInput(), getOutput(), getActionType(), getName(), getDescription(), getUid());
        result = 31 * result + Arrays.hashCode(getMappingPointsX());
        result = 31 * result + Arrays.hashCode(getMappingPointsY());
        result = 31 * result + Arrays.hashCode(mappedInputs);
        result = 31 * result + Arrays.hashCode(getMappedOutputs());
        return result;
    }

    @JsonProperty
    public InputOutputDTO getInput() {
        return input;
    }

    @JsonProperty
    public InputOutputDTO getOutput() {
        return output;
    }

    @JsonProperty
    public ActionType getActionType() {
        return actionType;
    }

    @JsonProperty
    public String getName() {
        return name;
    }

    @JsonProperty
    public String getDescription() {
        return description;
    }

    @JsonProperty
    public UUID getUid() {
        return uid;
    }

    @JsonProperty
    public int[] getMappingPointsX() {
        return mappingPointsX;
    }

    @JsonProperty
    public int[] getMappingPointsY() {
        return mappingPointsY;
    }

    @JsonProperty
    public int[] getMappedOutputs() {
        return mappedOutputs;
    }

    public MappingAction toAction() {
        MappingPoint[] mappingPoints = new MappingPoint[this.mappingPointsX.length];
        for (int i = 0; i < this.mappingPointsX.length && i < this.mappingPointsY.length; i++) {
            mappingPoints[i] = new MappingPoint(mappingPointsX[i], mappingPointsY[i]);
        }
        return new MappingAction(input.toGetter(), output.toSetter(), mappingPoints, actionType, name, description,
                uid);
    }

    @Override
    public String toString() {
        return "ActionDTO{" + "input=" + input + ", output=" + output + ", actionType=" + actionType + ", name='" + name
                + '\'' + ", description='" + description + '\'' + ", uid=" + uid + ", mappingPointsX="
                + Arrays.toString(mappingPointsX) + ", mappingPointsY=" + Arrays.toString(mappingPointsY)
                + ", mappedInputs=" + Arrays.toString(mappedInputs) + ", mappedOutputs="
                + Arrays.toString(mappedOutputs) + '}';
    }
}
