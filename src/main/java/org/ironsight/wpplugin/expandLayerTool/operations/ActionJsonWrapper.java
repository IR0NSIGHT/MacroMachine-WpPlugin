package org.ironsight.wpplugin.expandLayerTool.operations;


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

    public ActionJsonWrapper() {
    }

    public ActionJsonWrapper(LayerMapping mapping) {
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
}
