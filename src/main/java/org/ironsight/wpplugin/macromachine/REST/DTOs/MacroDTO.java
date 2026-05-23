package org.ironsight.wpplugin.macromachine.REST.DTOs;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import org.ironsight.wpplugin.macromachine.operations.Macro;

@Schema(description = "Represents a macro: collection of macros and actions")
public class MacroDTO {
  @Schema(
      description =
          "UUIDs of executions associated with this macro. can be of a macro or an action",
      example = "[\"550e8400-e29b-41d4-a716-446655440000\"]",
      requiredMode = Schema.RequiredMode.REQUIRED)
  public final UUID[] executionUUIDs;

  @Schema(
      description = "Flags indicating whether each action is active",
      example = "[true, false, true]",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private final boolean[] activeActions;

  @Schema(
      description = "Human readable macro name",
      example = "Morning Routine",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private final String name;

  @Schema(
      description = "Detailed description of the macro",
      example = "Runs all startup automation tasks",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private final String description;

  @Schema(
      description = "Unique identifier of the macro",
      example = "550e8400-e29b-41d4-a716-446655440000",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private final UUID uid;

  @JsonCreator
  public MacroDTO(
      @JsonProperty("executionUUIDs") UUID[] executionUUIDs,
      @JsonProperty("activeActions") boolean[] activeActions,
      @JsonProperty("name") String name,
      @JsonProperty("description") String description,
      @JsonProperty("uid") UUID uid) {
    this.executionUUIDs = executionUUIDs;
    this.activeActions = activeActions;
    this.name = name;
    this.description = description;
    this.uid = uid;
  }

  public MacroDTO(Macro macro) {
    this(
        macro.executionUUIDs,
        macro.getActiveActions(),
        macro.getName(),
        macro.getDescription(),
        macro.getUid());
  }

  public Macro toMacro() {
    return new Macro(name, description, executionUUIDs, uid, activeActions);
  }

  public boolean[] getActiveActions() {
    return activeActions;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public UUID getUid() {
    return uid;
  }
}
