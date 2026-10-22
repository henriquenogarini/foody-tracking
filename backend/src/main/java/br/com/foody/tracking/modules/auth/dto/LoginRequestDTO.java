package br.com.foody.tracking.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credentials to obtain an access token")
public record LoginRequestDTO(
        @Schema(description = "Registered e-mail", example = "ana@exemplo.com") @NotBlank @Email
                String email,
        @Schema(
                        description = "Account password",
                        example = "senha-forte-123",
                        accessMode = Schema.AccessMode.WRITE_ONLY)
                @NotBlank
                String password) {

    // Stripped here: Jackson builds the record before Bean Validation, so @Email sees it clean.
    public LoginRequestDTO {
        email = email == null ? null : email.strip();
    }
}
