package br.com.foody.tracking.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

class JacksonConfigTest {

  private static final ZoneId APP_ZONE = ZoneId.of("America/Sao_Paulo");

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
    new JacksonConfig().applicationZoneIsoDates(APP_ZONE).customize(builder);
    objectMapper = builder.build();
  }

  @Test
  void shouldSerializeInApplicationZone_whenValueCarriesUtcOffset() throws Exception {
    // Given a value as read back from the database
    OffsetDateTime fromDatabase = OffsetDateTime.parse("2026-10-21T17:13:27Z");

    // When
    String json = objectMapper.writeValueAsString(fromDatabase);

    // Then
    assertThat(json).isEqualTo("\"2026-10-21T14:13:27-03:00\"");
  }

  @Test
  void shouldSerializeUnchanged_whenValueIsAlreadyInApplicationZone() throws Exception {
    // Given
    OffsetDateTime local = OffsetDateTime.parse("2026-10-21T14:13:27-03:00");

    // When
    String json = objectMapper.writeValueAsString(local);

    // Then
    assertThat(json).isEqualTo("\"2026-10-21T14:13:27-03:00\"");
  }

  @Test
  void shouldKeepInstant_whenDeserializingUtcValue() throws Exception {
    // Given
    OffsetDateTime expected = OffsetDateTime.parse("2026-10-21T17:13:27Z");

    // When
    OffsetDateTime parsed =
        objectMapper.readValue("\"2026-10-21T17:13:27Z\"", OffsetDateTime.class);

    // Then
    assertThat(parsed).isAtSameInstantAs(expected);
  }
}
