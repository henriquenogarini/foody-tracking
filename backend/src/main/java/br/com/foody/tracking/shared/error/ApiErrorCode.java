package br.com.foody.tracking.shared.error;

public enum ApiErrorCode {
  VALIDATION_ERROR(400),
  AUTH_INVALID_CREDENTIALS(401),
  AUTH_TOKEN_INVALID(401),
  ACCESS_DENIED(403),
  ORDER_NOT_FOUND(404),
  USER_EMAIL_ALREADY_EXISTS(409),
  ORDER_INVALID_TRANSITION(409),
  CONCURRENT_MODIFICATION(409),
  INTERNAL_ERROR(500);

  // int, not HttpStatus: DomainException takes this enum into domain, which never sees Spring Web.
  private final int httpStatus;

  ApiErrorCode(int httpStatus) {
    this.httpStatus = httpStatus;
  }

  public int httpStatus() {
    return httpStatus;
  }
}
