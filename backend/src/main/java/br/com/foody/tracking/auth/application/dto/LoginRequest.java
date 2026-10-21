package br.com.foody.tracking.auth.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credentials to obtain an access token")
public record LoginRequest(
    @Schema(description = "Registered e-mail", example = "ana@exemplo.com") @NotBlank @Email
        String email,
    @Schema(
            description = "Account password",
            example = "senha-forte-123",
            accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank
        String password) {

  // Same reason as RegisterRequest: strip before @Email sees the value.
  public LoginRequest {
    email = email == null ? null : email.strip();
  }
}
