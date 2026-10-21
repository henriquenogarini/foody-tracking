package br.com.foody.tracking.modules.order.dto;

import br.com.foody.tracking.modules.order.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Compact view of an order for the paged list")
public record OrderSummaryResponseDTO(
    @Schema(
            description = "Public identifier of the order",
            example = "7b1e4c2a-5d3f-4a8b-9c6d-1e2f3a4b5c6d")
        UUID id,
    @Schema(description = "Readable sequential code", example = "1042") long code,
    @Schema(description = "Current status", example = "RECEBIDO") OrderStatus status,
    @Schema(description = "Customer full name", example = "José da Silva") String customerName,
    @Schema(description = "Order total, scale 2", example = "59.90") BigDecimal total,
    @Schema(description = "Number of item lines", example = "1") int itemsCount,
    @Schema(description = "Delivery neighborhood", example = "Moema") String neighborhood,
    @Schema(description = "Delivery city", example = "São Paulo") String city,
    @Schema(
            description = "Creation instant in the application zone (America/Sao_Paulo)",
            example = "2026-10-20T14:00:00-03:00")
        OffsetDateTime createdAt,
    @Schema(
            description = "Last change instant in the application zone (America/Sao_Paulo)",
            example = "2026-10-20T14:00:00-03:00")
        OffsetDateTime updatedAt) {}
