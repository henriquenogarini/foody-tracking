package br.com.foody.tracking.modules.order.dto;

import br.com.foody.tracking.modules.order.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "One status change of an order, including its creation")
public record StatusHistoryResponseDTO(
        @Schema(description = "Previous status; null on the creation row", example = "RECEBIDO")
                OrderStatus fromStatus,
        @Schema(description = "New status", example = "EM_PREPARO") OrderStatus toStatus,
        @Schema(
                        description = "Change instant in the application zone (America/Sao_Paulo)",
                        example = "2026-10-20T14:05:00-03:00")
                OffsetDateTime changedAt,
        @Schema(
                        description = "Public identifier of the user who made the change",
                        example = "3f6c1a2e-8d4b-4c7a-9e1f-2b5d7a9c0e11")
                UUID changedBy,
        @Schema(
                        description =
                                "Current name of the user who made the change; null if that user no longer exists",
                        example = "Ana Souza",
                        nullable = true)
                String changedByName) {}
