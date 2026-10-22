package br.com.foody.tracking.modules.auth.security;

import br.com.foody.tracking.config.JwtConfig;
import br.com.foody.tracking.config.JwtProperties;
import br.com.foody.tracking.modules.auth.dto.TokenResponseDTO;
import br.com.foody.tracking.modules.auth.entity.User;
import br.com.foody.tracking.modules.auth.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenServiceTest {

    private static final ZoneId APP_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final String SECRET =
            "4F7QtQJCWv6h/csoRdvkKJ8G81a2ugKuXnD3raXwk8+PcTOWFEw1IfGPrOJArc8x";
    private static final String OTHER_SECRET =
            "6TxraCkFf6oRqgAdVNPUN2zbIVvQziInuKzha8i288LD8pPNB9SpaLEY7FqRHSPf";

    // A fixed instant in the past: the decoder validates exp against the injected Clock, not the
    // system clock, so tokens issued "now" stay valid regardless of when the test runs.
    private final Instant now = Instant.parse("2025-03-10T15:30:00Z");
    private final Clock clock = Clock.fixed(now, APP_ZONE);

    private final JwtConfig jwtConfig = new JwtConfig();
    private final JwtProperties properties = new JwtProperties(SECRET, 7200);
    private final JwtEncoder encoder = jwtConfig.jwtEncoder(properties);
    private final JwtDecoder decoder = jwtConfig.jwtDecoder(properties, clock);

    @Test
    void shouldIssueHs256TokenWithContractClaims_whenUserLogsIn() {
        // Given
        User user = new User("Ana Souza", "ana@exemplo.com", "bcrypt-hash", Role.ADMIN, at(now));
        TokenService service = new TokenService(encoder, clock, properties);

        // When
        TokenResponseDTO response = service.issue(user);
        Jwt jwt = decoder.decode(response.accessToken());

        // Then
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(7200);
        assertThat(jwt.getHeaders()).containsEntry("alg", "HS256");
        assertThat(jwt.getSubject()).isEqualTo(user.getPublicId().toString());
        assertThat(jwt.getClaimAsString("email")).isEqualTo("ana@exemplo.com");
        assertThat(jwt.getClaimAsString("name")).isEqualTo("Ana Souza");
        assertThat(jwt.getClaimAsString("role")).isEqualTo("ADMIN");
        assertThat(jwt.getIssuedAt()).isEqualTo(now);
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()))
                .isEqualTo(Duration.ofSeconds(7200));
        assertThat(jwt.getClaims()).containsOnlyKeys("sub", "email", "name", "role", "iat", "exp");
    }

    @Test
    void shouldRejectToken_whenSignedWithAnotherKey() {
        // Given
        JwtProperties otherProperties = new JwtProperties(OTHER_SECRET, 7200);
        TokenService foreignIssuer =
                new TokenService(jwtConfig.jwtEncoder(otherProperties), clock, otherProperties);
        User user = new User("Ana Souza", "ana@exemplo.com", "hash", Role.USER, at(now));
        String foreignToken = foreignIssuer.issue(user).accessToken();

        // When / Then
        assertThatThrownBy(() -> decoder.decode(foreignToken)).isInstanceOf(JwtException.class);
    }

    @Test
    void shouldRejectToken_whenExpClaimIsMissing() {
        // Given
        JwtClaimsSet claims =
                JwtClaimsSet.builder()
                        .subject("3f6c1a2e")
                        .issuedAt(now)
                        .claim("role", "USER")
                        .build();
        String token =
                encoder.encode(
                                JwtEncoderParameters.from(
                                        JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                        .getTokenValue();

        // When / Then
        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtValidationException.class);
    }

    @Test
    void shouldRejectToken_whenExpired() {
        // Given
        Instant threeHoursAgo = now.minus(Duration.ofHours(3));
        TokenService pastIssuer =
                new TokenService(encoder, Clock.fixed(threeHoursAgo, APP_ZONE), properties);
        User user = new User("Ana Souza", "ana@exemplo.com", "hash", Role.USER, at(threeHoursAgo));
        String expiredToken = pastIssuer.issue(user).accessToken();

        // When / Then
        assertThatThrownBy(() -> decoder.decode(expiredToken))
                .isInstanceOf(JwtValidationException.class);
    }

    @Test
    void shouldRejectToken_whenDecoderClockIsPastExpiry() {
        // Given
        User user = new User("Ana Souza", "ana@exemplo.com", "hash", Role.USER, at(now));
        String token = new TokenService(encoder, clock, properties).issue(user).accessToken();
        // Beyond exp plus the validator's default 60-second clock skew.
        Clock afterExpiry = Clock.fixed(now.plus(Duration.ofSeconds(7200 + 61)), APP_ZONE);
        JwtDecoder laterDecoder = jwtConfig.jwtDecoder(properties, afterExpiry);

        // When / Then
        assertThat(decoder.decode(token).getSubject()).isEqualTo(user.getPublicId().toString());
        assertThatThrownBy(() -> laterDecoder.decode(token))
                .isInstanceOf(JwtValidationException.class);
    }

    private static OffsetDateTime at(Instant instant) {
        return OffsetDateTime.ofInstant(instant, APP_ZONE);
    }
}
