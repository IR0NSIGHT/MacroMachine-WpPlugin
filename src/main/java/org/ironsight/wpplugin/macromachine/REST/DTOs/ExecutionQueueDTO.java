package org.ironsight.wpplugin.macromachine.REST.DTOs;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

@Schema(description = "Request to enqueue macros for execution")
public record ExecutionQueueDTO(
    @NotEmpty
        @Schema(
            description = "Ordered list of macro IDs to enqueue",
            requiredMode = Schema.RequiredMode.REQUIRED)
        List<UUID> queuedMacroIds) {}
