package br.com.foody.tracking.shared.exception.type;

import br.com.foody.tracking.shared.exception.model.ApiErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthExceptionsTest {

    @Test
    void shouldFixCodeAndMessage_whenEmailAlreadyExists() {
        // When
        ApiException exception = new UserEmailAlreadyExistsException();

        // Then
        assertThat(exception.getCode()).isEqualTo(ApiErrorCode.USER_EMAIL_ALREADY_EXISTS);
        assertThat(exception).hasMessage("Já existe um usuário cadastrado com este e-mail.");
    }

    @Test
    void shouldFixCodeAndMessage_whenCredentialsAreInvalid() {
        // When
        ApiException exception = new InvalidCredentialsException();

        // Then
        assertThat(exception.getCode()).isEqualTo(ApiErrorCode.AUTH_INVALID_CREDENTIALS);
        assertThat(exception).hasMessage("E-mail ou senha inválidos.");
    }

    @Test
    void shouldFixCodeAndMessage_whenTokenIsInvalid() {
        // When
        ApiException exception = new InvalidTokenException();

        // Then
        assertThat(exception.getCode()).isEqualTo(ApiErrorCode.AUTH_TOKEN_INVALID);
        assertThat(exception).hasMessage("Token de acesso ausente, inválido ou expirado.");
    }
}
