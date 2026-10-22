package br.com.foody.tracking.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(JwtProperties.class)
public final class JwtConfig {

    public static final String ROLE_CLAIM = "role";

    @Bean
    public JwtEncoder jwtEncoder(JwtProperties properties) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey(properties)));
    }

    @Bean
    public JwtDecoder jwtDecoder(JwtProperties properties, Clock clock) {
        // Pinned on both sides: never trust the alg header of an incoming token.
        NimbusJwtDecoder decoder =
                NimbusJwtDecoder.withSecretKey(secretKey(properties))
                        .macAlgorithm(MacAlgorithm.HS256)
                        .build();
        JwtTimestampValidator timestamps = new JwtTimestampValidator();
        timestamps.setClock(clock);
        // JwtTimestampValidator alone accepts a token without exp, which would never expire.
        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        timestamps,
                        new JwtClaimValidator<Instant>(JwtClaimNames.EXP, Objects::nonNull)));
        return decoder;
    }

    private static SecretKey secretKey(JwtProperties properties) {
        return new SecretKeySpec(properties.secretBytes(), "HmacSHA256");
    }
}
