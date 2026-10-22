package br.com.foody.tracking.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;
import java.util.TimeZone;

@Configuration(proxyBeanMethods = false)
public final class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer applicationZoneIsoDates(ZoneId appZone) {
        return builder ->
                builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                        .featuresToEnable(
                                SerializationFeature.WRITE_DATES_WITH_CONTEXT_TIME_ZONE,
                                DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
                        .timeZone(TimeZone.getTimeZone(appZone));
    }
}
