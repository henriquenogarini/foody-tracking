package br.com.foody.tracking.config;

import br.com.foody.tracking.modules.auth.helper.PasswordPolicy;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

// Evaluators run `cp .env.example .env` and boot as-is, so every value must be present and valid.
class EnvExampleTest {

    private static final Path ENV_EXAMPLE = Path.of(".env.example");

    @Test
    void shouldDefineNoBlankValue_whenEnvExampleIsCopiedAsIs() throws IOException {
        // When
        Properties properties = load();

        // Then
        assertThat(properties.stringPropertyNames())
                .isNotEmpty()
                .allSatisfy(key -> assertThat(properties.getProperty(key)).as(key).isNotBlank());
    }

    @Test
    void shouldDefineEveryDocumentedVariable_whenEnvExampleIsCopiedAsIs() throws IOException {
        // When
        Properties properties = load();

        // Then
        assertThat(properties.stringPropertyNames())
                .containsExactlyInAnyOrder(
                        "SPRING_PROFILES_ACTIVE",
                        "SERVER_PORT",
                        "APP_TIME_ZONE",
                        "TZ",
                        "SPRING_DATASOURCE_URL",
                        "APP_CORS_ALLOWED_ORIGINS",
                        "APP_SECURITY_JWT_SECRET",
                        "APP_SECURITY_ADMIN_EMAIL",
                        "APP_SECURITY_ADMIN_PASSWORD",
                        "APP_SECURITY_PASSWORD_RESET_ENABLED");
    }

    @Test
    void shouldShipAStrongJwtSecret_whenEnvExampleIsCopiedAsIs() throws IOException {
        // Given
        Properties properties = load();

        // When
        JwtProperties jwt =
                new JwtProperties(properties.getProperty("APP_SECURITY_JWT_SECRET"), 7200);

        // Then
        assertThat(jwt.isSecretAStrongBase64Key()).isTrue();
    }

    @Test
    void shouldShipAnAdminPasswordThatMeetsThePolicy_whenEnvExampleIsCopiedAsIs()
            throws IOException {
        // When
        String password = load().getProperty("APP_SECURITY_ADMIN_PASSWORD");

        // Then
        assertThat(password).hasSizeGreaterThanOrEqualTo(PasswordPolicy.MIN_LENGTH);
        assertThat(PasswordPolicy.exceedsMaxBytes(password)).isFalse();
    }

    @Test
    void shouldUseTheSameValidZoneForAppAndLogs_whenEnvExampleIsCopiedAsIs() throws IOException {
        // When
        Properties properties = load();

        // Then
        String appZone = properties.getProperty("APP_TIME_ZONE");
        assertThat(properties.getProperty("TZ")).isEqualTo(appZone);
        assertThat(ZoneId.of(appZone)).isNotNull();
    }

    private static Properties load() throws IOException {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(ENV_EXAMPLE, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return properties;
    }
}
