package br.com.foody.tracking.modules.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "One line of the order")
public record OrderItemResponse(
    @Schema(description = "What was ordered", example = "Pizza calabresa G") String description,
    @Schema(description = "Quantity", example = "1") int quantity,
    @Schema(description = "Unit price, scale 2", example = "59.90") BigDecimal unitPrice,
    @Schema(description = "quantity x unitPrice, computed by the server", example = "59.90")
        BigDecimal subtotal) {}
