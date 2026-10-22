package br.com.foody.tracking.modules.auth.security;

import br.com.foody.tracking.config.JwtConfig;
import br.com.foody.tracking.config.JwtProperties;
import br.com.foody.tracking.modules.auth.dto.TokenResponseDTO;
import br.com.foody.tracking.modules.auth.entity.User;
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

  public TokenResponseDTO issue(User user) {
    Instant issuedAt = clock.instant();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .subject(user.getPublicId().toString())
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plusSeconds(expirationSeconds))
            .claim("email", user.getEmail())
            .claim("name", user.getName())
            .claim(JwtConfig.ROLE_CLAIM, user.getRole().name())
            .build();
    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    return TokenResponseDTO.bearer(token, expirationSeconds);
  }
}
