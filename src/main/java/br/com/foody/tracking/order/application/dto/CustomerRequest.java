package br.com.foody.tracking.order.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Who receives the order (plain data, not a user account)")
public record CustomerRequest(
    @Schema(description = "Customer full name", example = "José da Silva")
        @NotBlank
        @Size(max = 120)
        String name,
    @Schema(description = "Optional phone in E.164 format", example = "+5511987521144")
        @Pattern(
            regexp = "^$|^\\+[1-9]\\d{7,14}$",
            message = "deve estar no formato E.164, ex.: +5511987521144")
        String phone,
    @Schema(description = "Optional customer e-mail", example = "jose@exemplo.com")
        @Email
        @Size(max = 254)
        String email) {}
