package br.com.foody.tracking.shared.exception.model;

public enum ApiErrorCode {
  VALIDATION_ERROR(400),
  AUTH_INVALID_CREDENTIALS(401),
  AUTH_TOKEN_INVALID(401),
  ACCESS_DENIED(403),
  ORDER_NOT_FOUND(404),
  USER_EMAIL_ALREADY_EXISTS(409),
  ORDER_INVALID_TRANSITION(409),
  CONCURRENT_MODIFICATION(409),
  INTERNAL_ERROR(500),
  NOT_FOUND(404),
  METHOD_NOT_ALLOWED(405),
  NOT_ACCEPTABLE(406),
  UNSUPPORTED_MEDIA_TYPE(415);

  private final int httpStatus;

  ApiErrorCode(int httpStatus) {
    this.httpStatus = httpStatus;
  }

  public int httpStatus() {
    return httpStatus;
  }
}
