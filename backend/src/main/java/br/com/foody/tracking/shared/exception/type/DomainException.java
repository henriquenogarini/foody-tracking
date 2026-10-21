package br.com.foody.tracking.shared.exception.type;

import br.com.foody.tracking.shared.exception.model.ApiErrorCode;

public abstract class DomainException extends ApiException {

  protected DomainException(ApiErrorCode code, String message) {
    super(code, message);
  }
}
