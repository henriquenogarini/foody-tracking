package br.com.foody.tracking.shared.exception.type;

import br.com.foody.tracking.shared.exception.model.ApiErrorCode;

// Carries the rejected property so the handler can name it in errors[], exactly like Spring Data's
// PropertyReferenceException for paths that do not exist at all.
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
