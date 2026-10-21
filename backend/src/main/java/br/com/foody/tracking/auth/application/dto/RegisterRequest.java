package br.com.foody.tracking.auth.application.dto;

import br.com.foody.tracking.shared.validation.MaxUtf8Bytes;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Data to register a new user account")
public record RegisterRequest(
    @Schema(description = "Full name of the user", example = "Ana Souza") @NotBlank @Size(max = 100)
        String name,
    @Schema(description = "E-mail used to log in (case-insensitive)", example = "ana@exemplo.com")
        @NotBlank
        @Email
        @Size(max = 254)
        String email,
    @Schema(
            description = "Password, 8 to 72 characters (at most 72 UTF-8 bytes)",
            example = "senha-forte-123",
            accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank
        @Size(min = 8, max = MaxUtf8Bytes.BCRYPT_MAX_BYTES)
        @MaxUtf8Bytes(MaxUtf8Bytes.BCRYPT_MAX_BYTES)
        String password) {}
