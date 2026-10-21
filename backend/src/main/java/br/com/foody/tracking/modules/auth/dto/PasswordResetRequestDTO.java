package br.com.foody.tracking.modules.auth.dto;

import br.com.foody.tracking.shared.validation.MaxUtf8Bytes;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Demo password reset: the account e-mail and the new password")
public record PasswordResetRequestDTO(
    @Schema(description = "E-mail of the account (case-insensitive)", example = "ana@exemplo.com")
        @NotBlank
        @Email
        @Size(max = 254)
        String email,
    @Schema(
            description = "New password, 8 to 72 characters (at most 72 UTF-8 bytes)",
            example = "nova-senha-456",
            accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank
        @Size(min = 8, max = MaxUtf8Bytes.BCRYPT_MAX_BYTES)
        @MaxUtf8Bytes(MaxUtf8Bytes.BCRYPT_MAX_BYTES)
        String newPassword) {

  // Stripped at construction so @Email validates the clean value (same reason as register).
  public PasswordResetRequestDTO {
    email = email == null ? null : email.strip();
  }
}
