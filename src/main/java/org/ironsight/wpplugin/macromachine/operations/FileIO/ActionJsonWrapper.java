package org.ironsight.wpplugin.macromachine.operations.FileIO;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.ironsight.wpplugin.macromachine.operations.ActionType;
import org.ironsight.wpplugin.macromachine.operations.MappingAction;
import org.ironsight.wpplugin.macromachine.operations.MappingPoint;
import org.ironsight.wpplugin.macromachine.operations.ProviderType;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

public class ActionJsonWrapper {
    private ProviderType inputId;
    private Object[] inputData;
    private ProviderType outputId;
    private Object[] outputData;
    private ActionType actionType;
    private int[] inputPoints;
    private int[] outputPoints;
    private String name;
    private String description;
    private UUID uid;


    @JsonCreator
    public ActionJsonWrapper(
            @JsonProperty("inputId") ProviderType inputId,
            @JsonProperty("inputData") Object[] inputData,
            @JsonProperty("outputId") ProviderType outputId,
            @JsonProperty("outputData") Object[] outputData,
            @JsonProperty("actionType") ActionType actionType,
            @JsonProperty("inputPoints") int[] inputPoints,
            @JsonProperty("outputPoints") int[] outputPoints,
            @JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("uid") UUID uid) {
        this.inputId = inputId;
        this.inputData = inputData;
        this.outputId = outputId;
        this.outputData = outputData;
        this.actionType = actionType;
        this.inputPoints = inputPoints;
        this.outputPoints = outputPoints;
        this.name = name;
        this.description = description;
        this.uid = uid;

        assert uid != null;
    }

    public ActionJsonWrapper(MappingAction mapping) {
        setInputId(mapping.input.getProviderType());
        setInputData(mapping.input.getSaveData());

        setOutputId(mapping.output.getProviderType());
        setOutputData(mapping.output.getSaveData());

        setActionType(mapping.getActionType());
        setName(mapping.getName());
        setDescription(mapping.getDescription());
        setUid(mapping.getUid());

        int[] ins = new int[mapping.getMappingPoints().length], outs = new int[mapping.getMappingPoints().length];
        int i = 0;
        for (MappingPoint m : mapping.getMappingPoints()) {
            ins[i] = m.input;
            outs[i] = m.output;
            i++;
        }
        setInputPoints(ins);
        setOutputPoints(outs);

        assert uid != null;
    }

    public Object[] getInputData() {
        return inputData;
    }

    public void setInputData(Object[] inputData) {
        this.inputData = inputData;
    }

    public Object[] getOutputData() {
        return outputData;
    }

    public void setOutputData(Object[] outputData) {
        this.outputData = outputData;
    }

    public ProviderType getInputId() {
        return inputId;
    }

    public void setInputId(ProviderType inputId) {
        this.inputId = inputId;
    }

    public ProviderType getOutputId() {
        return outputId;
    }

    public void setOutputId(ProviderType outputId) {
        this.outputId = outputId;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }

    public int[] getInputPoints() {
        return inputPoints;
    }

    public void setInputPoints(int[] inputPoints) {
        this.inputPoints = inputPoints;
    }

    public int[] getOutputPoints() {
        return outputPoints;
    }

    public void setOutputPoints(int[] outputPoints) {
        this.outputPoints = outputPoints;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public UUID getUid() {
        return uid;
    }

    public void setUid(UUID uid) {
        this.uid = uid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActionJsonWrapper that = (ActionJsonWrapper) o;
        return getInputId() == that.getInputId() && Arrays.equals(getInputData(), that.getInputData()) &&
                getOutputId() == that.getOutputId() && Arrays.equals(getOutputData(), that.getOutputData()) &&
                getActionType() == that.getActionType() &&
                Arrays.equals(getInputPoints(), that.getInputPoints()) &&
                Arrays.equals(getOutputPoints(), that.getOutputPoints()) &&
                Objects.equals(getName(), that.getName()) &&
                Objects.equals(getDescription(), that.getDescription()) &&
                Objects.equals(getUid(), that.getUid());
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(getInputId(), getOutputId(), getActionType(), getName(), getDescription(), getUid());
        result = 31 * result + Arrays.hashCode(getInputData());
        result = 31 * result + Arrays.hashCode(getOutputData());
        result = 31 * result + Arrays.hashCode(getInputPoints());
        result = 31 * result + Arrays.hashCode(getOutputPoints());
        return result;
    }
}
