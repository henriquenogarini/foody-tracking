package br.com.foody.tracking.modules.auth.security;

import br.com.foody.tracking.modules.auth.enums.Role;
import br.com.foody.tracking.shared.exception.type.InvalidTokenException;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

public record AuthenticatedUser(UUID id, Role role) {

  // Malformed claims are a client-side token problem (401), not a server failure (500).
  public static AuthenticatedUser from(Jwt jwt) {
    return new AuthenticatedUser(subject(jwt.getSubject()), role(jwt.getClaimAsString("role")));
  }

  private static UUID subject(String subject) {
    if (subject == null || subject.isBlank()) {
      throw new InvalidTokenException();
    }
    try {
      return UUID.fromString(subject);
    } catch (IllegalArgumentException ex) {
      throw new InvalidTokenException();
    }
  }

  private static Role role(String role) {
    if (role == null || role.isBlank()) {
      throw new InvalidTokenException();
    }
    try {
      return Role.valueOf(role);
    } catch (IllegalArgumentException ex) {
      throw new InvalidTokenException();
    }
  }
}
