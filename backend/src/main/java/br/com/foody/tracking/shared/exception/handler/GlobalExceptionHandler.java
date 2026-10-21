package br.com.foody.tracking.shared.exception.handler;

import br.com.foody.tracking.shared.exception.model.ApiErrorCode;
import br.com.foody.tracking.shared.exception.model.ValidationError;
import br.com.foody.tracking.shared.exception.type.ApiException;
import br.com.foody.tracking.shared.exception.type.InvalidSortException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public final class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  private static final String INVALID_FIELDS_DETAIL = "Um ou mais campos são inválidos.";
  private static final String INVALID_VALUE = "valor inválido";
  private static final String TIMESTAMP = "timestamp";
  private static final String CODE = "code";

  private static final Comparator<ValidationError> ERROR_ORDER =
      Comparator.comparing(ValidationError::field).thenComparing(ValidationError::message);

  // 413 (multipart) and 503 (async timeout) are unreachable in this API and stay code-less.
  private static final Map<Integer, ApiErrorCode> FRAMEWORK_CODES =
      Map.of(
          400, ApiErrorCode.VALIDATION_ERROR,
          404, ApiErrorCode.NOT_FOUND,
          405, ApiErrorCode.METHOD_NOT_ALLOWED,
          406, ApiErrorCode.NOT_ACCEPTABLE,
          415, ApiErrorCode.UNSUPPORTED_MEDIA_TYPE,
          500, ApiErrorCode.INTERNAL_ERROR);

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<Object> handleApiException(ApiException ex, WebRequest request) {
    log.debug("{} on {}", ex.getCode(), request.getDescription(false));
    return respond(ex, ProblemDetailFactory.create(ex.getCode(), ex.getMessage()), request);
  }

  @ExceptionHandler(OptimisticLockingFailureException.class)
  public ResponseEntity<Object> handleOptimisticLocking(
      OptimisticLockingFailureException ex, WebRequest request) {
    log.warn("Optimistic locking conflict on {}", request.getDescription(false));
    ProblemDetail problem =
        ProblemDetailFactory.create(
            ApiErrorCode.CONCURRENT_MODIFICATION,
            "O registro foi alterado por outra operação. Atualize e tente novamente.");
    return respond(ex, problem, request);
  }

  @ExceptionHandler(PropertyReferenceException.class)
  public ResponseEntity<Object> handleUnknownSortProperty(
      PropertyReferenceException ex, WebRequest request) {
    log.debug("Unknown sort property on {}", request.getDescription(false));
    return respondInvalidSort(ex, ex.getPropertyName(), request);
  }

  @ExceptionHandler(InvalidSortException.class)
  public ResponseEntity<Object> handleDisallowedSortProperty(
      InvalidSortException ex, WebRequest request) {
    log.debug("Disallowed sort property on {}", request.getDescription(false));
    return respondInvalidSort(ex, ex.getProperty(), request);
  }

  // Security exceptions raised inside the MVC layer (e.g. @PreAuthorize) must reach Spring
  // Security's ExceptionTranslationFilter, which answers 401/403 in the contract format; the
  // catch-all below would otherwise turn them into 500.
  @ExceptionHandler({AccessDeniedException.class, AuthenticationException.class})
  public void rethrowSecurityException(RuntimeException ex) {
    throw ex;
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Object> handleUnexpected(Exception ex, WebRequest request) {
    log.error("Unhandled exception on {}", request.getDescription(false), ex);
    ProblemDetail problem =
        ProblemDetailFactory.create(
            ApiErrorCode.INTERNAL_ERROR, "Erro interno. Tente novamente mais tarde.");
    return respond(ex, problem, request);
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    List<ValidationError> errors =
        ex.getBindingResult().getAllErrors().stream()
            .map(
                error ->
                    new ValidationError(
                        error instanceof FieldError fieldError
                            ? fieldError.getField()
                            : error.getObjectName(),
                        Objects.requireNonNullElse(error.getDefaultMessage(), INVALID_VALUE)))
            // Violations arrive in hash order; sorting gives clients and tests a stable list.
            .sorted(ERROR_ORDER)
            .toList();
    return respond(
        ex,
        ProblemDetailFactory.create(ApiErrorCode.VALIDATION_ERROR, INVALID_FIELDS_DETAIL, errors),
        headers,
        request);
  }

  // Constraints on @RequestParam/@PathVariable (Spring 6.1 built-in method validation) answer
  // like body validation: VALIDATION_ERROR with errors[] naming the parameter. A 500 (return
  // value validation) is left to the framework.
  @Override
  protected ResponseEntity<Object> handleHandlerMethodValidationException(
      HandlerMethodValidationException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    if (status.value() != HttpStatus.BAD_REQUEST.value()) {
      return super.handleHandlerMethodValidationException(ex, headers, status, request);
    }
    List<ValidationError> errors =
        ex.getParameterValidationResults().stream()
            .flatMap(
                result ->
                    result.getResolvableErrors().stream()
                        .map(
                            error ->
                                new ValidationError(
                                    error instanceof FieldError fieldError
                                        ? fieldError.getField()
                                        : result.getMethodParameter().getParameterName(),
                                    Objects.requireNonNullElse(
                                        error.getDefaultMessage(), INVALID_VALUE))))
            .sorted(ERROR_ORDER)
            .toList();
    return respond(
        ex,
        ProblemDetailFactory.create(ApiErrorCode.VALIDATION_ERROR, INVALID_FIELDS_DETAIL, errors),
        headers,
        request);
  }

  @Override
  protected ResponseEntity<Object> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    // Spring wraps the Jackson exception as the direct cause; an empty path is a root-level
    // failure with no field to name.
    if (ex.getCause() instanceof JsonMappingException mapping && !mapping.getPath().isEmpty()) {
      Class<?> targetType =
          mapping instanceof InvalidFormatException invalid ? invalid.getTargetType() : null;
      ValidationError error =
          new ValidationError(fieldPath(mapping), invalidValueMessage(targetType));
      return respond(
          ex,
          ProblemDetailFactory.create(
              ApiErrorCode.VALIDATION_ERROR, INVALID_FIELDS_DETAIL, List.of(error)),
          headers,
          request);
    }
    return respond(
        ex,
        ProblemDetailFactory.create(
            ApiErrorCode.VALIDATION_ERROR, "Corpo da requisição inválido ou malformado."),
        headers,
        request);
  }

  @Override
  protected ResponseEntity<Object> handleTypeMismatch(
      TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    String field =
        ex instanceof MethodArgumentTypeMismatchException mismatch
            ? mismatch.getName()
            : ex.getPropertyName();
    ValidationError error = new ValidationError(field, invalidValueMessage(ex.getRequiredType()));
    return respond(
        ex,
        ProblemDetailFactory.create(
            ApiErrorCode.VALIDATION_ERROR, INVALID_FIELDS_DETAIL, List.of(error)),
        headers,
        request);
  }

  // Framework-built bodies (unknown route, 405, 406, 415, missing parameter...) get the contract's
  // timestamp and a code from a small fixed set. Keyed by status because this hook never sees the
  // exception; bodies built by ProblemDetailFactory already carry their code (ORDER_NOT_FOUND is
  // also a 404) and are never overwritten.
  @Override
  protected ResponseEntity<Object> createResponseEntity(
      Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
    if (body instanceof ProblemDetail problem) {
      if (!hasProperty(problem, TIMESTAMP)) {
        problem.setProperty(TIMESTAMP, OffsetDateTime.now(ZoneOffset.UTC));
      }
      ApiErrorCode frameworkCode = FRAMEWORK_CODES.get(problem.getStatus());
      if (frameworkCode != null && !hasProperty(problem, CODE)) {
        problem.setProperty(CODE, frameworkCode.name());
      }
    }
    return super.createResponseEntity(body, headers, statusCode, request);
  }

  // Unknown and disallowed sort properties answer with the same body, so a client cannot tell
  // which entity paths exist.
  private ResponseEntity<Object> respondInvalidSort(
      Exception ex, String property, WebRequest request) {
    ValidationError error =
        new ValidationError("sort", "propriedade de ordenação inválida: " + property);
    return respond(
        ex,
        ProblemDetailFactory.create(
            ApiErrorCode.VALIDATION_ERROR, INVALID_FIELDS_DETAIL, List.of(error)),
        request);
  }

  private ResponseEntity<Object> respond(Exception ex, ProblemDetail problem, WebRequest request) {
    return respond(ex, problem, new HttpHeaders(), request);
  }

  private ResponseEntity<Object> respond(
      Exception ex, ProblemDetail problem, HttpHeaders headers, WebRequest request) {
    return handleExceptionInternal(
        ex, problem, headers, HttpStatusCode.valueOf(problem.getStatus()), request);
  }

  private static boolean hasProperty(ProblemDetail problem, String name) {
    Map<String, Object> properties = problem.getProperties();
    return properties != null && properties.containsKey(name);
  }

  private static String fieldPath(JsonMappingException ex) {
    StringBuilder path = new StringBuilder();
    for (JsonMappingException.Reference reference : ex.getPath()) {
      if (reference.getFieldName() == null) {
        path.append('[').append(reference.getIndex()).append(']');
      } else {
        if (!path.isEmpty()) {
          path.append('.');
        }
        path.append(reference.getFieldName());
      }
    }
    return path.toString();
  }

  private static String invalidValueMessage(Class<?> targetType) {
    if (targetType == null || !targetType.isEnum()) {
      return INVALID_VALUE;
    }
    return Arrays.stream(targetType.getEnumConstants())
        .map(constant -> ((Enum<?>) constant).name())
        .collect(Collectors.joining(", ", INVALID_VALUE + "; valores aceitos: ", ""));
  }
}
