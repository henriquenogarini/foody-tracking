package br.com.foody.tracking.config;

import com.fasterxml.jackson.databind.SerializationFeature;
import java.util.TimeZone;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public final class JacksonConfig {

  // Dates must leave as ISO-8601 with offset in UTC regardless of the host JVM time zone, and
  // incoming offsets are normalized to UTC the same way.
  @Bean
  Jackson2ObjectMapperBuilderCustomizer utcIsoDates() {
    return builder ->
        builder
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .timeZone(TimeZone.getTimeZone("UTC"));
  }
}
