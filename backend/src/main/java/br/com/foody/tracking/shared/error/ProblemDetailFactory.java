package br.com.foody.tracking.shared.error;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

// Static so the security entry point and @WebMvcTest slices can use it without wiring.
public final class ProblemDetailFactory {

  private ProblemDetailFactory() {}

  public static ProblemDetail create(ApiErrorCode code, String detail) {
    return create(code, detail, List.of());
  }

  public static ProblemDetail create(
      ApiErrorCode code, String detail, List<ValidationError> errors) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.valueOf(code.httpStatus()), detail);
    problem.setProperty("code", code.name());
    problem.setProperty("timestamp", OffsetDateTime.now(ZoneOffset.UTC));
    if (!errors.isEmpty()) {
      problem.setProperty("errors", List.copyOf(errors));
    }
    return problem;
  }
}
