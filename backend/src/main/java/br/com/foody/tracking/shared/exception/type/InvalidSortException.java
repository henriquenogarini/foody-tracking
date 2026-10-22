package br.com.foody.tracking.shared.exception.type;

import br.com.foody.tracking.shared.exception.model.ApiErrorCode;

public final class InvalidSortException extends ApiException {

  private final String property;

  public InvalidSortException(String property) {
    super(ApiErrorCode.VALIDATION_ERROR, "Um ou mais campos são inválidos.");
    this.property = property;
  }

  public String getProperty() {
    return property;
  }
}
