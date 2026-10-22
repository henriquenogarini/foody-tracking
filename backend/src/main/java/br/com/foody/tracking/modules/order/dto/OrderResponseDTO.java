package br.com.foody.tracking.modules.order.dto;

import br.com.foody.tracking.modules.order.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Full view of an order")
public record OrderResponseDTO(
        @Schema(
                        description = "Public identifier of the order",
                        example = "7b1e4c2a-5d3f-4a8b-9c6d-1e2f3a4b5c6d")
                UUID id,
        @Schema(description = "Readable sequential code", example = "1042") long code,
        @Schema(description = "Current status", example = "RECEBIDO") OrderStatus status,
        @Schema(description = "Who receives the order") CustomerResponseDTO customer,
        @Schema(description = "Ordered items") List<OrderItemResponseDTO> items,
        @Schema(description = "Where the order is delivered") AddressResponseDTO deliveryAddress,
        @Schema(description = "Notes, when informed", example = "sem cebola") String notes,
        @Schema(
                        description = "Sum of the item subtotals, computed by the server",
                        example = "59.90")
                BigDecimal total,
        @Schema(
                        description =
                                "Creation instant in the application zone (America/Sao_Paulo)",
                        example = "2026-10-20T14:00:00-03:00")
                OffsetDateTime createdAt,
        @Schema(
                        description =
                                "Last change instant in the application zone (America/Sao_Paulo)",
                        example = "2026-10-20T14:00:00-03:00")
                OffsetDateTime updatedAt) {}
