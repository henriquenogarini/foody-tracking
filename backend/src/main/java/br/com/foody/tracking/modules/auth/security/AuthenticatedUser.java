package br.com.foody.tracking.modules.auth.security;

import br.com.foody.tracking.config.JwtConfig;
import br.com.foody.tracking.modules.auth.enums.Role;
import br.com.foody.tracking.shared.exception.type.InvalidTokenException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

public record AuthenticatedUser(UUID id, Role role) {

    public static AuthenticatedUser from(Jwt jwt) {
        return new AuthenticatedUser(
                parseSubject(jwt.getSubject()),
                parseRole(jwt.getClaimAsString(JwtConfig.ROLE_CLAIM)));
    }

    private static UUID parseSubject(String subject) {
        if (subject == null) {
            throw new InvalidTokenException();
        }
        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException ex) {
            throw new InvalidTokenException();
        }
    }

    private static Role parseRole(String role) {
        if (role == null) {
            throw new InvalidTokenException();
        }
        try {
            return Role.valueOf(role);
        } catch (IllegalArgumentException ex) {
            throw new InvalidTokenException();
        }
    }
}
