package br.com.foody.tracking.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class ClockConfigTest {

  private final ClockConfig config = new ClockConfig();

  @Test
  void shouldUseConfiguredZone_whenBuildingClock() {
    // Given
    ZoneId appZone = config.appZone("America/Sao_Paulo");

    // When
    Clock clock = config.clock(appZone);

    // Then
    assertThat(appZone).isEqualTo(ZoneId.of("America/Sao_Paulo"));
    assertThat(clock.getZone()).isEqualTo(appZone);
  }

  @Test
  void shouldTickInWholeMilliseconds_whenReadingTheClock() {
    // Given
    Clock clock = config.clock(config.appZone("America/Sao_Paulo"));

    // When / Then: storage keeps milliseconds, so no sub-millisecond digits may appear
    for (int i = 0; i < 5; i++) {
      assertThat(clock.instant().getNano() % 1_000_000).isZero();
    }
  }
}
