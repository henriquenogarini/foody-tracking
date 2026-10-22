package br.com.foody.tracking.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Base64;

@Validated
@ConfigurationProperties("app.security.jwt")
public record JwtProperties(@NotBlank String secret, @Positive @Max(7200) long expirationSeconds) {

    private static final int MIN_KEY_BYTES = 32;

    @AssertTrue(
            message =
                    "app.security.jwt.secret must be Base64 and decode to at least 32 bytes (HS256)")
    public boolean isSecretAStrongBase64Key() {
        if (secret == null || secret.isBlank()) {
            return true;
        }
        try {
            return Base64.getDecoder().decode(secret).length >= MIN_KEY_BYTES;
        } catch (IllegalArgumentException notBase64) {
            return false;
        }
    }

    public byte[] secretBytes() {
        return Base64.getDecoder().decode(secret);
    }
}
