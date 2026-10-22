package br.com.foody.tracking.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
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

// HS256 is pinned on both sides: the decoder accepts no other algorithm and TokenService always
// sends the HS256 header (Nimbus would otherwise default to RS256).
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
    NimbusJwtDecoder decoder =
        NimbusJwtDecoder.withSecretKey(secretKey(properties))
            .macAlgorithm(MacAlgorithm.HS256)
            .build();
    // Expiry is checked against the same Clock that stamps iat/exp in TokenService. The timestamp
    // validator alone accepts a token without exp, which would never expire.
    JwtTimestampValidator timestamps = new JwtTimestampValidator();
    timestamps.setClock(clock);
    decoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(
            timestamps, new JwtClaimValidator<Instant>(JwtClaimNames.EXP, Objects::nonNull)));
    return decoder;
  }

  private static SecretKey secretKey(JwtProperties properties) {
    return new SecretKeySpec(properties.secretBytes(), "HmacSHA256");
  }
}
