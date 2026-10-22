package br.com.foody.tracking.shared.exception.handler;

import br.com.foody.tracking.shared.exception.model.ApiErrorCode;
import br.com.foody.tracking.shared.exception.model.ValidationError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

public final class ProblemDetailFactory {

    static final String CODE = "code";
    static final String TIMESTAMP = "timestamp";
    static final String ERRORS = "errors";

    private ProblemDetailFactory() {}

    public static ProblemDetail create(ApiErrorCode code, String detail) {
        return create(code, detail, List.of());
    }

    public static ProblemDetail create(
            ApiErrorCode code, String detail, List<ValidationError> errors) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.valueOf(code.httpStatus()), detail);
        problem.setProperty(CODE, code.name());
        problem.setProperty(TIMESTAMP, timestamp());
        if (!errors.isEmpty()) {
            problem.setProperty(ERRORS, List.copyOf(errors));
        }
        return problem;
    }

    static OffsetDateTime timestamp() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }
}
