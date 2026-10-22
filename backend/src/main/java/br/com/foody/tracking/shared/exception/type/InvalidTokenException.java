package br.com.foody.tracking.shared.exception.type;

import br.com.foody.tracking.shared.exception.model.ApiErrorCode;

public final class InvalidTokenException extends ApiException {

    public static final String DETAIL = "Token de acesso ausente, inválido ou expirado.";

    public InvalidTokenException() {
        super(ApiErrorCode.AUTH_TOKEN_INVALID, DETAIL);
    }
}
