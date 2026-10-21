package br.com.foody.tracking.config;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Single source of the application zone: services take the instant from this Clock and Jackson
// re-zones dates with the same ZoneId, so changing `app.time-zone` moves both together.
@Configuration(proxyBeanMethods = false)
public final class ClockConfig {

  @Bean
  ZoneId appZone(@Value("${app.time-zone}") String timeZone) {
    return ZoneId.of(timeZone);
  }

  // SQLite stores instants as epoch millis; truncating at the source keeps the value returned on
  // write equal to the one read back later (no micro/nanosecond digits that vanish on reload).
  @Bean
  Clock clock(ZoneId appZone) {
    return Clock.tick(Clock.system(appZone), Duration.ofMillis(1));
  }
}
