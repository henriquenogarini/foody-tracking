package br.com.foody.tracking.auth.application;

import br.com.foody.tracking.auth.application.dto.TokenResponse;
import br.com.foody.tracking.auth.domain.User;
import br.com.foody.tracking.config.JwtProperties;
import java.time.Clock;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public final class TokenService {

  private final JwtEncoder jwtEncoder;
  private final Clock clock;
  private final long expirationSeconds;

  public TokenService(JwtEncoder jwtEncoder, Clock clock, JwtProperties jwtProperties) {
    this.jwtEncoder = jwtEncoder;
    this.clock = clock;
    this.expirationSeconds = jwtProperties.expirationSeconds();
  }

  // Claims are the contract's minimum: never the internal id nor the password hash.
  public TokenResponse issue(User user) {
    Instant issuedAt = clock.instant();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .subject(user.getPublicId().toString())
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plusSeconds(expirationSeconds))
            .claim("email", user.getEmail())
            .claim("name", user.getName())
            .claim("role", user.getRole().name())
            .build();
    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    return TokenResponse.bearer(token, expirationSeconds);
  }
}
