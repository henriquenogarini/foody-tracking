package br.com.foody.tracking.shared.error;

public abstract class DomainException extends ApiException {

  protected DomainException(ApiErrorCode code, String message) {
    super(code, message);
  }
}
