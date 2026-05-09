package org.ironsight.wpplugin.macromachine.REST.DTOs;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "Represents the application's execution state")
public record ExecutionStateDTO(

        @Schema(format = "uuid", description = "ID of the currently executing macro", requiredMode = REQUIRED) UUID executionId,

        @NotEmpty @Schema(description = "Ordered execution steps for the current execution", requiredMode = REQUIRED) List<ExecutionStepDTO> steps,

        @PositiveOrZero @Schema(description = "Zero-based index of the currently active step", example = "0", requiredMode = REQUIRED) int currentStepIndex,
        @NotNull @Schema(description = "state of the execution", requiredMode = REQUIRED) ExecutionStatus status

) {
}
