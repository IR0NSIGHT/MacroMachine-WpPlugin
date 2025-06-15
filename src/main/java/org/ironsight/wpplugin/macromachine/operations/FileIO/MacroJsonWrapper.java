package org.ironsight.wpplugin.macromachine.operations.FileIO;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class MacroJsonWrapper {

    @JsonCreator
    public MacroJsonWrapper(
            @JsonProperty("macroName") String macroName,
            @JsonProperty("description") String description,
            @JsonProperty("stepIds") UUID[] stepIds,
            @JsonProperty("selfId") UUID selfId) {
        this.macroName = macroName;
        this.description = description;
        this.stepIds = stepIds;
        this.selfId = selfId;
    }

    public UUID getSelfId() {
        return selfId;
    }

    private final UUID selfId;
    private final String macroName;
    private final String description;
    private final UUID[] stepIds;

    public String getMacroName() {
        return macroName;
    }

    public String getDescription() {
        return description;
    }

    public UUID[] getStepIds() {
        return stepIds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MacroJsonWrapper that = (MacroJsonWrapper) o;
        return Objects.equals(getSelfId(), that.getSelfId()) &&
                Objects.equals(getMacroName(), that.getMacroName()) &&
                Objects.equals(getDescription(), that.getDescription()) &&
                Arrays.equals(getStepIds(), that.getStepIds());
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(getSelfId(), getMacroName(), getDescription());
        result = 31 * result + Arrays.hashCode(getStepIds());
        return result;
    }
}
