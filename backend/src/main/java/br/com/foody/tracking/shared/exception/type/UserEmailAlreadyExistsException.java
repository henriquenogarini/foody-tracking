package br.com.foody.tracking.shared.exception.type;

import br.com.foody.tracking.shared.exception.model.ApiErrorCode;

public final class UserEmailAlreadyExistsException extends ApiException {

  public UserEmailAlreadyExistsException() {
    super(
        ApiErrorCode.USER_EMAIL_ALREADY_EXISTS, "Já existe um usuário cadastrado com este e-mail.");
  }
}
