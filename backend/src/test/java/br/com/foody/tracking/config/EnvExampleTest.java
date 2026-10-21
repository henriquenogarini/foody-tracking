package br.com.foody.tracking.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.Test;

// `cp .env.example .env` must boot the dev profile as-is. Spring imports .env as a properties
// file, and a blank value overrides the profile default instead of falling back to it.
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
  void shouldLeaveSecretsCommentedOut_whenEnvExampleIsCopiedAsIs() throws IOException {
    // When
    Properties properties = load();

    // Then
    assertThat(properties)
        .doesNotContainKeys(
            "APP_SECURITY_JWT_SECRET", "APP_SECURITY_ADMIN_EMAIL", "APP_SECURITY_ADMIN_PASSWORD");
  }

  private static Properties load() throws IOException {
    Properties properties = new Properties();
    try (Reader reader = Files.newBufferedReader(ENV_EXAMPLE, StandardCharsets.UTF_8)) {
      properties.load(reader);
    }
    return properties;
  }
}
