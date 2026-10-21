package br.com.foody.tracking.shared.exception.type;

import br.com.foody.tracking.shared.exception.model.ApiErrorCode;

// Thrown for an unknown e-mail and for a wrong password alike: one fixed message, so the response
// never reveals which accounts exist.
public final class InvalidCredentialsException extends ApiException {

  public InvalidCredentialsException() {
    super(ApiErrorCode.AUTH_INVALID_CREDENTIALS, "E-mail ou senha inválidos.");
  }
}
