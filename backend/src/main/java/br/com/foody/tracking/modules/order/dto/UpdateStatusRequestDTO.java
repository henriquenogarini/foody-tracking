package br.com.foody.tracking.modules.order.dto;

import br.com.foody.tracking.modules.order.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Target status of the order; must be allowed by the state machine")
public record UpdateStatusRequestDTO(
    @Schema(description = "Target status", example = "EM_PREPARO") @NotNull OrderStatus status) {}
