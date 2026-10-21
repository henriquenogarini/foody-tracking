package br.com.foody.tracking.modules.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Who receives the order")
public record CustomerResponseDTO(
    @Schema(description = "Customer full name", example = "José da Silva") String name,
    @Schema(description = "Phone in E.164 format, when informed", example = "+5511987521144")
        String phone,
    @Schema(description = "Customer e-mail, when informed", example = "jose@exemplo.com")
        String email) {}
