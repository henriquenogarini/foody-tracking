package br.com.foody.tracking.modules.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Schema(description = "One line of the order; the subtotal is computed by the server")
public record OrderItemRequestDTO(
    @Schema(description = "What is being ordered", example = "Pizza calabresa G")
        @NotBlank
        @Size(max = 200)
        String description,
    @Schema(description = "Quantity, 1 to 999", example = "1") @NotNull @Min(1) @Max(999)
        Integer quantity,
    @Schema(
            description = "Unit price with up to 2 decimal places, from 0.01 to 99999.99",
            example = "59.90")
        @NotNull
        @DecimalMin("0.01")
        // Bounded so the largest order total stays exact in SQLite's REAL-backed NUMERIC;
        // @Digits only guards the 2 decimals, so an over-limit price yields a single error.
        @DecimalMax("99999.99")
        @Digits(integer = 10, fraction = 2)
        BigDecimal unitPrice) {}
