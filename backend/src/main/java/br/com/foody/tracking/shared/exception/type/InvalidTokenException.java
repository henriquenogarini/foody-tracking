package br.com.foody.tracking.shared.exception.type;

import br.com.foody.tracking.shared.exception.model.ApiErrorCode;

// A signature-valid token whose claims cannot identify a user (missing/unknown subject or role).
// Same detail as the security entry point, so every 401 AUTH_TOKEN_INVALID reads the same.
public final class InvalidTokenException extends ApiException {

  public static final String DETAIL = "Token de acesso ausente, inválido ou expirado.";

  public InvalidTokenException() {
    super(ApiErrorCode.AUTH_TOKEN_INVALID, DETAIL);
  }
}
