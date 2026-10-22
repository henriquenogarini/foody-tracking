package br.com.foody.tracking.shared.exception.handler;

import br.com.foody.tracking.shared.exception.model.ApiErrorCode;
import br.com.foody.tracking.shared.exception.model.ValidationError;
import br.com.foody.tracking.shared.exception.type.ApiException;
import br.com.foody.tracking.shared.exception.type.InvalidSortException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSourceResolvable;
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

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public final class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String INVALID_FIELDS_DETAIL = "Um ou mais campos são inválidos.";
    private static final String INVALID_VALUE = "valor inválido";

    private static final Comparator<ValidationError> ERROR_ORDER =
            Comparator.comparing(ValidationError::field).thenComparing(ValidationError::message);

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

    // Let ExceptionTranslationFilter answer 401/403; the catch-all below would make them 500.
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
                        .map(error -> toValidationError(error, error.getObjectName()))
                        .sorted(ERROR_ORDER)
                        .toList();
        return respond(ex, invalidFields(errors), headers, request);
    }

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
                                                                toValidationError(
                                                                        error,
                                                                        result.getMethodParameter()
                                                                                .getParameterName())))
                        .sorted(ERROR_ORDER)
                        .toList();
        return respond(ex, invalidFields(errors), headers, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        if (ex.getCause() instanceof JsonMappingException mapping && !mapping.getPath().isEmpty()) {
            Class<?> targetType =
                    mapping instanceof InvalidFormatException invalid
                            ? invalid.getTargetType()
                            : null;
            ValidationError error =
                    new ValidationError(fieldPath(mapping), invalidValueMessage(targetType));
            return respond(ex, invalidFields(List.of(error)), headers, request);
        }
        return respond(
                ex,
                ProblemDetailFactory.create(
                        ApiErrorCode.VALIDATION_ERROR,
                        "Corpo da requisição inválido ou malformado."),
                headers,
                request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        String field =
                ex instanceof MethodArgumentTypeMismatchException mismatch
                        ? mismatch.getName()
                        : ex.getPropertyName();
        ValidationError error =
                new ValidationError(field, invalidValueMessage(ex.getRequiredType()));
        return respond(ex, invalidFields(List.of(error)), headers, request);
    }

    @Override
    protected ResponseEntity<Object> createResponseEntity(
            Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        if (body instanceof ProblemDetail problem) {
            if (!hasProperty(problem, ProblemDetailFactory.TIMESTAMP)) {
                problem.setProperty(
                        ProblemDetailFactory.TIMESTAMP, ProblemDetailFactory.timestamp());
            }
            ApiErrorCode frameworkCode = FRAMEWORK_CODES.get(problem.getStatus());
            if (frameworkCode != null && !hasProperty(problem, ProblemDetailFactory.CODE)) {
                problem.setProperty(ProblemDetailFactory.CODE, frameworkCode.name());
            }
        }
        return super.createResponseEntity(body, headers, statusCode, request);
    }

    private ResponseEntity<Object> respondInvalidSort(
            Exception ex, String property, WebRequest request) {
        ValidationError error =
                new ValidationError("sort", "propriedade de ordenação inválida: " + property);
        return respond(ex, invalidFields(List.of(error)), request);
    }

    private ResponseEntity<Object> respond(
            Exception ex, ProblemDetail problem, WebRequest request) {
        return respond(ex, problem, new HttpHeaders(), request);
    }

    private ResponseEntity<Object> respond(
            Exception ex, ProblemDetail problem, HttpHeaders headers, WebRequest request) {
        return handleExceptionInternal(
                ex, problem, headers, HttpStatusCode.valueOf(problem.getStatus()), request);
    }

    private static ValidationError toValidationError(
            MessageSourceResolvable error, String fallbackField) {
        return new ValidationError(
                error instanceof FieldError fieldError ? fieldError.getField() : fallbackField,
                Objects.requireNonNullElse(error.getDefaultMessage(), INVALID_VALUE));
    }

    private static ProblemDetail invalidFields(List<ValidationError> errors) {
        return ProblemDetailFactory.create(
                ApiErrorCode.VALIDATION_ERROR, INVALID_FIELDS_DETAIL, errors);
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
