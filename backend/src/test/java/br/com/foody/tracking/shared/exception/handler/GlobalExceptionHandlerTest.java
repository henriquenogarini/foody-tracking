package br.com.foody.tracking.shared.exception.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.foody.tracking.config.ClockConfig;
import br.com.foody.tracking.config.JacksonConfig;
import br.com.foody.tracking.shared.exception.model.ApiErrorCode;
import br.com.foody.tracking.shared.exception.model.ValidationError;
import br.com.foody.tracking.shared.exception.type.ApiException;
import br.com.foody.tracking.shared.exception.type.DomainException;
import br.com.foody.tracking.shared.exception.type.InvalidSortException;
import com.jayway.jsonpath.JsonPath;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.MethodParameter;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.data.util.TypeInformation;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.ServletWebRequest;

@WebMvcTest(controllers = GlobalExceptionHandlerTest.ThrowingController.class)
@Import({
  ClockConfig.class,
  JacksonConfig.class,
  GlobalExceptionHandlerTest.ThrowingController.class
})
@AutoConfigureMockMvc(addFilters = false)
class GlobalExceptionHandlerTest {

  private static final String INVALID_FIELDS_DETAIL = "Um ou mais campos são inválidos.";

  @Autowired private MockMvc mockMvc;

  @Autowired private GlobalExceptionHandler handler;

  @Test
  void shouldReturnMappedStatusAndCode_whenApiExceptionIsThrown() throws Exception {
    // When / Then
    mockMvc
        .perform(get("/test/api-exception"))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.type").value("about:blank"))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.title").value("Not Found"))
        .andExpect(jsonPath("$.detail").value("Pedido não encontrado"))
        .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"))
        .andExpect(jsonPath("$.instance").value("/test/api-exception"))
        .andExpect(jsonPath("$.errors").doesNotExist())
        .andExpect(applicationZoneTimestamp());
  }

  @Test
  void shouldReturnConflict_whenDomainExceptionIsThrown() throws Exception {
    // When / Then
    mockMvc
        .perform(get("/test/domain-exception"))
        .andExpect(status().isConflict())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.title").value("Conflict"))
        .andExpect(jsonPath("$.detail").value("Transição inválida"))
        .andExpect(jsonPath("$.code").value("ORDER_INVALID_TRANSITION"))
        .andExpect(jsonPath("$.instance").value("/test/domain-exception"))
        .andExpect(jsonPath("$.errors").doesNotExist())
        .andExpect(applicationZoneTimestamp());
  }

  @Test
  void shouldReturnFieldErrors_whenBeanValidationFails() throws Exception {
    // Given
    String body = "{\"name\": \"\"}";

    // When / Then
    mockMvc
        .perform(post("/test/validate").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.title").value("Bad Request"))
        .andExpect(jsonPath("$.detail").value(INVALID_FIELDS_DETAIL))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.instance").value("/test/validate"))
        .andExpect(jsonPath("$.errors.length()").value(2))
        .andExpect(jsonPath("$.errors[0].field").value("name"))
        .andExpect(jsonPath("$.errors[0].message").value("não pode ficar em branco"))
        .andExpect(jsonPath("$.errors[1].field").value("status"))
        .andExpect(jsonPath("$.errors[1].message").value("é obrigatório"))
        .andExpect(applicationZoneTimestamp());
  }

  @Test
  void shouldSortErrorsByFieldThenMessage_whenViolationsArriveUnordered() throws Exception {
    // Given
    BeanPropertyBindingResult bindingResult =
        new BeanPropertyBindingResult(new Object(), "payload");
    bindingResult.addError(new FieldError("payload", "status", "é obrigatório"));
    bindingResult.addError(new ObjectError("payload", "lat e lng devem ser informados juntos"));
    bindingResult.addError(new FieldError("payload", "name", "tamanho inválido"));
    bindingResult.addError(new FieldError("payload", "name", "não pode ficar em branco"));
    MethodParameter parameter =
        new MethodParameter(
            ThrowingController.class.getDeclaredMethod(
                "validate", ThrowingController.Payload.class),
            0);
    MethodArgumentNotValidException exception =
        new MethodArgumentNotValidException(parameter, bindingResult);

    // When
    ResponseEntity<Object> response =
        handler.handleMethodArgumentNotValid(
            exception,
            new HttpHeaders(),
            HttpStatus.BAD_REQUEST,
            new ServletWebRequest(new MockHttpServletRequest()));

    // Then
    ProblemDetail problem = (ProblemDetail) response.getBody();
    assertThat(problem.getProperties())
        .containsEntry(
            "errors",
            List.of(
                new ValidationError("name", "não pode ficar em branco"),
                new ValidationError("name", "tamanho inválido"),
                new ValidationError("payload", "lat e lng devem ser informados juntos"),
                new ValidationError("status", "é obrigatório")));
  }

  @Test
  void shouldReturnMalformedBodyProblem_whenJsonCannotBeParsed() throws Exception {
    // When / Then
    mockMvc
        .perform(
            post("/test/validate").contentType(MediaType.APPLICATION_JSON).content("{not json"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.detail").value("Corpo da requisição inválido ou malformado."))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.instance").value("/test/validate"))
        .andExpect(jsonPath("$.errors").doesNotExist())
        .andExpect(applicationZoneTimestamp());
  }

  @Test
  void shouldListAcceptedValues_whenBodyEnumValueIsUnknown() throws Exception {
    // Given
    String body = "{\"name\": \"Ana\", \"status\": \"FOO\"}";

    // When / Then
    mockMvc
        .perform(post("/test/validate").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value(INVALID_FIELDS_DETAIL))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors.length()").value(1))
        .andExpect(jsonPath("$.errors[0].field").value("status"))
        .andExpect(
            jsonPath("$.errors[0].message")
                .value("valor inválido; valores aceitos: ACTIVE, INACTIVE"))
        .andExpect(applicationZoneTimestamp());
  }

  @Test
  void shouldReportInvalidValue_whenBodyFieldHasWrongJsonType() throws Exception {
    // Given
    String body = "{\"name\": \"Ana\", \"status\": \"ACTIVE\", \"items\": \"oops\"}";

    // When / Then
    mockMvc
        .perform(post("/test/validate").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value(INVALID_FIELDS_DETAIL))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors.length()").value(1))
        .andExpect(jsonPath("$.errors[0].field").value("items"))
        .andExpect(jsonPath("$.errors[0].message").value("valor inválido"))
        .andExpect(applicationZoneTimestamp());
  }

  @Test
  void shouldBuildNestedFieldPath_whenEnumValueInsideListIsUnknown() throws Exception {
    // Given
    String body = "{\"name\": \"Ana\", \"status\": \"ACTIVE\", \"items\": [{\"status\": \"FOO\"}]}";

    // When / Then
    mockMvc
        .perform(post("/test/validate").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors.length()").value(1))
        .andExpect(jsonPath("$.errors[0].field").value("items[0].status"))
        .andExpect(
            jsonPath("$.errors[0].message")
                .value("valor inválido; valores aceitos: ACTIVE, INACTIVE"));
  }

  @Test
  void shouldListAcceptedValues_whenQueryEnumValueIsUnknown() throws Exception {
    // When / Then
    mockMvc
        .perform(get("/test/enum-param").param("status", "FOO"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.detail").value(INVALID_FIELDS_DETAIL))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.instance").value("/test/enum-param"))
        .andExpect(jsonPath("$.errors.length()").value(1))
        .andExpect(jsonPath("$.errors[0].field").value("status"))
        .andExpect(
            jsonPath("$.errors[0].message")
                .value("valor inválido; valores aceitos: ACTIVE, INACTIVE"))
        .andExpect(applicationZoneTimestamp());
  }

  @Test
  void shouldReturnConcurrentModification_whenOptimisticLockFails() throws Exception {
    // When / Then
    mockMvc
        .perform(get("/test/optimistic-lock"))
        .andExpect(status().isConflict())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.title").value("Conflict"))
        .andExpect(
            jsonPath("$.detail")
                .value("O registro foi alterado por outra operação. Atualize e tente novamente."))
        .andExpect(jsonPath("$.code").value("CONCURRENT_MODIFICATION"))
        .andExpect(jsonPath("$.instance").value("/test/optimistic-lock"))
        .andExpect(jsonPath("$.errors").doesNotExist())
        .andExpect(applicationZoneTimestamp());
  }

  @Test
  void shouldHideCause_whenUnexpectedExceptionIsThrown() throws Exception {
    // When
    String body =
        mockMvc
            .perform(get("/test/unexpected"))
            .andExpect(status().isInternalServerError())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(500))
            .andExpect(jsonPath("$.title").value("Internal Server Error"))
            .andExpect(jsonPath("$.detail").value("Erro interno. Tente novamente mais tarde."))
            .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
            .andExpect(jsonPath("$.instance").value("/test/unexpected"))
            .andExpect(jsonPath("$.errors").doesNotExist())
            .andExpect(applicationZoneTimestamp())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);

    // Then
    assertThat(body).doesNotContain(ThrowingController.SECRET_CAUSE);
  }

  @Test
  void shouldStampNotFoundCode_whenRouteIsUnknown() throws Exception {
    // When / Then
    mockMvc
        .perform(get("/test/missing"))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.title").value("Not Found"))
        .andExpect(jsonPath("$.detail").value("Recurso não encontrado."))
        .andExpect(jsonPath("$.instance").value("/test/missing"))
        .andExpect(jsonPath("$.code").value("NOT_FOUND"))
        .andExpect(jsonPath("$.errors").doesNotExist())
        .andExpect(applicationZoneTimestamp());
  }

  @Test
  void shouldStampMethodNotAllowedCode_whenMethodIsNotAllowed() throws Exception {
    // When / Then
    mockMvc
        .perform(post("/test/api-exception"))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(header().exists(HttpHeaders.ALLOW))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(405))
        .andExpect(jsonPath("$.title").value("Method Not Allowed"))
        .andExpect(jsonPath("$.detail").value("Método POST não suportado."))
        .andExpect(jsonPath("$.instance").value("/test/api-exception"))
        .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"))
        .andExpect(jsonPath("$.errors").doesNotExist())
        .andExpect(applicationZoneTimestamp());
  }

  @Test
  void shouldStampUnsupportedMediaTypeCode_whenContentTypeIsNotSupported() throws Exception {
    // When / Then
    mockMvc
        .perform(post("/test/validate").contentType(MediaType.TEXT_PLAIN).content("name=Ana"))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(415))
        .andExpect(jsonPath("$.title").value("Unsupported Media Type"))
        .andExpect(jsonPath("$.instance").value("/test/validate"))
        .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"))
        .andExpect(jsonPath("$.errors").doesNotExist())
        .andExpect(applicationZoneTimestamp());
  }

  @Test
  void shouldStampNotAcceptableCode_whenAcceptHeaderCannotBeSatisfied() throws Exception {
    // When / Then
    mockMvc
        .perform(get("/test/json").accept(MediaType.APPLICATION_XML))
        .andExpect(status().isNotAcceptable())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(406))
        .andExpect(jsonPath("$.title").value("Not Acceptable"))
        .andExpect(jsonPath("$.detail").value("Formato de resposta não suportado."))
        .andExpect(jsonPath("$.instance").value("/test/json"))
        .andExpect(jsonPath("$.code").value("NOT_ACCEPTABLE"))
        .andExpect(jsonPath("$.errors").doesNotExist())
        .andExpect(applicationZoneTimestamp());
  }

  @Test
  void shouldStampValidationCode_whenRequiredParameterIsMissing() throws Exception {
    // When / Then
    mockMvc
        .perform(get("/test/enum-param"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.detail").value("Parâmetro obrigatório 'status' ausente."))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors").doesNotExist())
        .andExpect(applicationZoneTimestamp());
  }

  @Test
  void shouldStampInternalErrorCode_whenFrameworkAnswersServerError() throws Exception {
    // When / Then
    mockMvc
        .perform(get("/test/path-variable"))
        .andExpect(status().isInternalServerError())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(500))
        .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
        .andExpect(applicationZoneTimestamp());
  }

  @Test
  void shouldKeepContractCode_whenApiExceptionAnswersWithFrameworkStatus() {
    // Given
    ApiException exception =
        new ApiException(ApiErrorCode.ORDER_NOT_FOUND, "Pedido não encontrado");

    // When
    ResponseEntity<Object> response =
        handler.handleApiException(exception, new ServletWebRequest(new MockHttpServletRequest()));

    // Then
    ProblemDetail problem = (ProblemDetail) response.getBody();
    assertThat(problem.getStatus()).isEqualTo(404);
    assertThat(problem.getProperties()).containsEntry("code", "ORDER_NOT_FOUND");
  }

  @Test
  void shouldReturnSortFieldError_whenSortPropertyIsUnknown() throws Exception {
    // When / Then
    mockMvc
        .perform(get("/test/unknown-sort"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.detail").value(INVALID_FIELDS_DETAIL))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.instance").value("/test/unknown-sort"))
        .andExpect(jsonPath("$.errors.length()").value(1))
        .andExpect(jsonPath("$.errors[0].field").value("sort"))
        .andExpect(
            jsonPath("$.errors[0].message").value("propriedade de ordenação inválida: unknown"))
        .andExpect(applicationZoneTimestamp());
  }

  @Test
  void shouldReturnSameSortFieldError_whenSortPropertyIsNotWhitelisted() throws Exception {
    // When / Then
    mockMvc
        .perform(get("/test/disallowed-sort"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.detail").value(INVALID_FIELDS_DETAIL))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.instance").value("/test/disallowed-sort"))
        .andExpect(jsonPath("$.errors.length()").value(1))
        .andExpect(jsonPath("$.errors[0].field").value("sort"))
        .andExpect(
            jsonPath("$.errors[0].message")
                .value("propriedade de ordenação inválida: passwordHash"))
        .andExpect(applicationZoneTimestamp());
  }

  // Filters are off in this slice, so the rethrown exception surfaces from MockMvc; in the app,
  // Spring Security's ExceptionTranslationFilter turns it into the 403/401 ProblemDetail.
  @Test
  void shouldRethrowInsteadOfInternalError_whenAccessIsDenied() {
    // When / Then
    assertThatThrownBy(() -> mockMvc.perform(get("/test/access-denied")))
        .hasCauseInstanceOf(AuthorizationDeniedException.class);
  }

  @Test
  void shouldRethrowInsteadOfInternalError_whenAuthenticationFails() {
    // When / Then
    assertThatThrownBy(() -> mockMvc.perform(get("/test/authentication")))
        .hasCauseInstanceOf(BadCredentialsException.class);
  }

  private static ResultMatcher applicationZoneTimestamp() {
    return result -> {
      String json = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
      String timestamp = JsonPath.read(json, "$.timestamp");
      assertThat(OffsetDateTime.parse(timestamp).getOffset()).isEqualTo(ZoneOffset.ofHours(-3));
    };
  }

  @RestController
  @RequestMapping("/test")
  static class ThrowingController {

    static final String SECRET_CAUSE = "secret internal detail";

    enum Status {
      ACTIVE,
      INACTIVE
    }

    record Item(Status status) {}

    record Payload(
        @NotBlank(message = "não pode ficar em branco") String name,
        @NotNull(message = "é obrigatório") Status status,
        List<Item> items) {}

    @GetMapping("/api-exception")
    void apiException() {
      throw new ApiException(ApiErrorCode.ORDER_NOT_FOUND, "Pedido não encontrado");
    }

    @GetMapping("/domain-exception")
    void domainException() {
      throw new SampleDomainException();
    }

    @PostMapping("/validate")
    void validate(@Valid @RequestBody Payload payload) {}

    @GetMapping("/enum-param")
    void enumParam(@RequestParam Status status) {}

    @GetMapping(value = "/json", produces = MediaType.APPLICATION_JSON_VALUE)
    String json() {
      return "{}";
    }

    // The template lacks {id}, so the framework raises MissingPathVariableException (500).
    @GetMapping("/path-variable")
    void pathVariable(@PathVariable String id) {}

    @GetMapping("/optimistic-lock")
    void optimisticLock() {
      throw new ObjectOptimisticLockingFailureException(Object.class, 1L);
    }

    @GetMapping("/unexpected")
    void unexpected() {
      throw new IllegalStateException(SECRET_CAUSE);
    }

    @GetMapping("/unknown-sort")
    void unknownSort() {
      throw new PropertyReferenceException("unknown", TypeInformation.of(Payload.class), List.of());
    }

    @GetMapping("/disallowed-sort")
    void disallowedSort() {
      throw new InvalidSortException("passwordHash");
    }

    @GetMapping("/access-denied")
    void accessDenied() {
      throw new AuthorizationDeniedException("Access Denied");
    }

    @GetMapping("/authentication")
    void authentication() {
      throw new BadCredentialsException("bad credentials");
    }
  }

  private static final class SampleDomainException extends DomainException {

    SampleDomainException() {
      super(ApiErrorCode.ORDER_INVALID_TRANSITION, "Transição inválida");
    }
  }
}
