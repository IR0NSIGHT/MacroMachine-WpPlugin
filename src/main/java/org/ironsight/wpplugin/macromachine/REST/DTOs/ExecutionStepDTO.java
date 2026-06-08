package org.ironsight.wpplugin.macromachine.REST.DTOs;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(description = "Represents a single action step within a macro execution")
public record ExecutionStepDTO(
        @NotNull @Schema(format = "uuid", description = "Unique identifier of the action executed in this step", example = "550e8400-e29b-41d4-a716-446655440000", requiredMode = Schema.RequiredMode.REQUIRED) UUID actionId,
        @Schema(description = "Whether execution should pause before this step is executed", example = "false", requiredMode = Schema.RequiredMode.REQUIRED) boolean breakpoint,
        @DecimalMin(value = "0.0") @DecimalMax(value = "100.0") @Schema(description = "Completion percentage of this step, from 0 to 100", example = "42.5", minimum = "0", maximum = "100", requiredMode = Schema.RequiredMode.REQUIRED) float percentComplete) {
}
