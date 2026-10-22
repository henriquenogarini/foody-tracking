package br.com.foody.tracking.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Access token issued after a successful login")
public record TokenResponseDTO(
        @Schema(description = "Signed JWT (HS256) to send as a Bearer token", example = "eyJ...")
                String accessToken,
        @Schema(description = "Token type for the Authorization header", example = "Bearer")
                String tokenType,
        @Schema(description = "Token lifetime in seconds", example = "7200") long expiresIn) {

    public static TokenResponseDTO bearer(String accessToken, long expiresIn) {
        return new TokenResponseDTO(accessToken, "Bearer", expiresIn);
    }
}
