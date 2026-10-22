package br.com.foody.tracking.shared.exception.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class ApiErrorCodeTest {

    @ParameterizedTest
    @CsvSource({
        "VALIDATION_ERROR, 400",
        "AUTH_INVALID_CREDENTIALS, 401",
        "AUTH_TOKEN_INVALID, 401",
        "ACCESS_DENIED, 403",
        "ORDER_NOT_FOUND, 404",
        "USER_EMAIL_ALREADY_EXISTS, 409",
        "ORDER_INVALID_TRANSITION, 409",
        "CONCURRENT_MODIFICATION, 409",
        "INTERNAL_ERROR, 500",
        "NOT_FOUND, 404",
        "METHOD_NOT_ALLOWED, 405",
        "NOT_ACCEPTABLE, 406",
        "UNSUPPORTED_MEDIA_TYPE, 415"
    })
    void shouldExposeHttpStatus_whenCodeIsMapped(ApiErrorCode code, int expectedStatus) {
        assertThat(code.httpStatus()).isEqualTo(expectedStatus);
    }

    @Test
    void shouldMapEveryCodeToClientOrServerError_whenEnumIsInspected() {
        assertThat(ApiErrorCode.values())
                .hasSize(13)
                .allSatisfy(code -> assertThat(code.httpStatus()).isBetween(400, 599));
    }
}
