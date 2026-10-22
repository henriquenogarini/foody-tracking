package br.com.foody.tracking.shared.exception.type;

import br.com.foody.tracking.shared.exception.model.ApiErrorCode;

public final class InvalidCredentialsException extends ApiException {

  public InvalidCredentialsException() {
    super(ApiErrorCode.AUTH_INVALID_CREDENTIALS, "E-mail ou senha inválidos.");
  }
}
