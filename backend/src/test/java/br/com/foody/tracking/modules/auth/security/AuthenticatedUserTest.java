package br.com.foody.tracking.modules.auth.security;

import br.com.foody.tracking.modules.auth.enums.Role;
import br.com.foody.tracking.shared.exception.type.InvalidTokenException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthenticatedUserTest {

    @ParameterizedTest
    @EnumSource(Role.class)
    void shouldReadSubjectAndRole_whenBuiltFromJwt(Role role) {
        // Given
        UUID publicId = UUID.randomUUID();
        Jwt jwt =
                Jwt.withTokenValue("t")
                        .header("alg", "HS256")
                        .subject(publicId.toString())
                        .claim("role", role.name())
                        .build();

        // When
        AuthenticatedUser user = AuthenticatedUser.from(jwt);

        // Then
        assertThat(user.id()).isEqualTo(publicId);
        assertThat(user.role()).isEqualTo(role);
    }

    @Test
    void shouldThrowInvalidToken_whenRoleClaimIsMissing() {
        // Given
        Jwt jwt =
                Jwt.withTokenValue("t")
                        .header("alg", "HS256")
                        .subject(UUID.randomUUID().toString())
                        .build();

        // When / Then
        assertThatThrownBy(() -> AuthenticatedUser.from(jwt))
                .isInstanceOf(InvalidTokenException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", "SUPERUSER", "admin"})
    void shouldThrowInvalidToken_whenRoleClaimIsBlankOrUnknown(String role) {
        // Given
        Jwt jwt =
                Jwt.withTokenValue("t")
                        .header("alg", "HS256")
                        .subject(UUID.randomUUID().toString())
                        .claim("role", role)
                        .build();

        // When / Then
        assertThatThrownBy(() -> AuthenticatedUser.from(jwt))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void shouldThrowInvalidToken_whenSubjectIsMissing() {
        // Given
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "HS256").claim("role", "USER").build();

        // When / Then
        assertThatThrownBy(() -> AuthenticatedUser.from(jwt))
                .isInstanceOf(InvalidTokenException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", "not-a-uuid"})
    void shouldThrowInvalidToken_whenSubjectIsBlankOrNotUuid(String subject) {
        // Given
        Jwt jwt =
                Jwt.withTokenValue("t")
                        .header("alg", "HS256")
                        .subject(subject)
                        .claim("role", "USER")
                        .build();

        // When / Then
        assertThatThrownBy(() -> AuthenticatedUser.from(jwt))
                .isInstanceOf(InvalidTokenException.class);
    }
}
