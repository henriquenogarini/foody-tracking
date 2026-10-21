package br.com.foody.tracking.modules.auth.dto;

import br.com.foody.tracking.modules.auth.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Public view of a user account")
public record UserResponseDTO(
    @Schema(
            description = "Public identifier of the user",
            example = "3f6c1a2e-8d4b-4c7a-9e1f-2b5d7a9c0e11")
        UUID id,
    @Schema(description = "Full name of the user", example = "Ana Souza") String name,
    @Schema(description = "Normalized (lowercase) e-mail", example = "ana@exemplo.com")
        String email,
    @Schema(description = "Access level; USER on registration", example = "USER") Role role,
    @Schema(
            description = "Creation instant in the application zone (America/Sao_Paulo)",
            example = "2026-10-20T14:00:00-03:00")
        OffsetDateTime createdAt) {}
