package br.com.foody.tracking.auth.application;

import br.com.foody.tracking.shared.error.ApiErrorCode;
import br.com.foody.tracking.shared.error.ApiException;

public final class UserEmailAlreadyExistsException extends ApiException {

  public UserEmailAlreadyExistsException() {
    super(
        ApiErrorCode.USER_EMAIL_ALREADY_EXISTS, "Já existe um usuário cadastrado com este e-mail.");
  }
}
