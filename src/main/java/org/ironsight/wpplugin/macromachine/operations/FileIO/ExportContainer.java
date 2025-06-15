package org.ironsight.wpplugin.macromachine.operations.FileIO;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ExportContainer implements Serializable {
    private final String exportDate;
    private final String comment;
    private final MacroJsonWrapper[] macros;
    private final ActionJsonWrapper[] actions;

    @JsonCreator
    public ExportContainer(
            @JsonProperty("exportDate") String exportDate,
            @JsonProperty("comment") String comment,
            @JsonProperty("macros") MacroJsonWrapper[] macros,
            @JsonProperty("actions") ActionJsonWrapper[] actions) {
        this.exportDate = exportDate;
        this.comment = comment;
        this.macros = macros;
        this.actions = actions;
    }

    public String getExportDate() {
        return exportDate;
    }

    public String getComment() {
        return comment;
    }

    public MacroJsonWrapper[] getMacros() {
        return macros;
    }

    public ActionJsonWrapper[] getActions() {
        return actions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExportContainer container = (ExportContainer) o;
        return Objects.equals(getExportDate(), container.getExportDate()) &&
                Objects.equals(getComment(), container.getComment()) &&
                Arrays.equals(getMacros(), container.getMacros()) &&
                Arrays.equals(getActions(), container.getActions());
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(getExportDate(), getComment());
        result = 31 * result + Arrays.hashCode(getMacros());
        result = 31 * result + Arrays.hashCode(getActions());
        return result;
    }
}
