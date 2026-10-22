package br.com.foody.tracking.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;

@Configuration(proxyBeanMethods = false)
public final class ClockConfig {

    @Bean
    ZoneId appZone(@Value("${app.time-zone}") String timeZone) {
        return ZoneId.of(timeZone);
    }

    // SQLite keeps epoch millis: truncating here makes written and reloaded instants equal.
    @Bean
    Clock clock(ZoneId appZone) {
        return Clock.tick(Clock.system(appZone), Duration.ofMillis(1));
    }
}
