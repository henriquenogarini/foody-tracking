package br.com.foody.tracking.order.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "Data to create an order; it is always born in RECEBIDO")
public record CreateOrderRequest(
    @Schema(description = "Who receives the order") @NotNull @Valid CustomerRequest customer,
    @Schema(description = "Ordered items, 1 to 50")
        @NotEmpty(message = "deve conter ao menos 1 item")
        @Size(max = 50)
        List<@NotNull @Valid OrderItemRequest> items,
    @Schema(description = "Where the order is delivered") @NotNull @Valid
        AddressRequest deliveryAddress,
    @Schema(description = "Optional notes for the kitchen or the courier", example = "sem cebola")
        @Size(max = 500)
        String notes) {}
