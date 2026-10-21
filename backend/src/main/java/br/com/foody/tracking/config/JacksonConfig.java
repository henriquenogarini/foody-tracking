package br.com.foody.tracking.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.time.ZoneId;
import java.util.TimeZone;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public final class JacksonConfig {

  // The database keeps UTC instants; the JSON boundary is where they become Brasília time. Every
  // OffsetDateTime leaves as ISO-8601 re-zoned to the application zone (values read back from the
  // database carry Z), and incoming offsets are adjusted to that same zone.
  @Bean
  public Jackson2ObjectMapperBuilderCustomizer applicationZoneIsoDates(ZoneId appZone) {
    return builder ->
        builder
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .featuresToEnable(
                SerializationFeature.WRITE_DATES_WITH_CONTEXT_TIME_ZONE,
                DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
            .timeZone(TimeZone.getTimeZone(appZone));
  }
}
