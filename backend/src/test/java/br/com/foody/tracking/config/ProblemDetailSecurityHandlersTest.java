package br.com.foody.tracking.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

class ProblemDetailSecurityHandlersTest {

  private static final String SECRET_MESSAGE = "Nimbus internal: signature mismatch";

  private final ObjectMapper objectMapper = bootLikeObjectMapper();

  @Test
  void shouldWriteUnauthorizedProblem_whenAuthenticationFails() throws Exception {
    // Given
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/orders");
    MockHttpServletResponse response = new MockHttpServletResponse();

    // When
    new ProblemDetailAuthenticationEntryPoint(objectMapper)
        .commence(request, response, new InvalidBearerTokenException(SECRET_MESSAGE));

    // Then
    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).isEqualTo("Bearer");
    assertThat(MediaType.parseMediaType(response.getContentType()))
        .isEqualTo(MediaType.parseMediaType("application/problem+json;charset=UTF-8"));
    String body = response.getContentAsString(StandardCharsets.UTF_8);
    JsonNode json = objectMapper.readTree(body);
    assertThat(json.get("status").asInt()).isEqualTo(401);
    assertThat(json.get("code").asText()).isEqualTo("AUTH_TOKEN_INVALID");
    assertThat(json.get("detail").asText())
        .isEqualTo("Token de acesso ausente, inválido ou expirado.");
    assertThat(json.get("instance").asText()).isEqualTo("/api/v1/orders");
    assertThat(OffsetDateTime.parse(json.get("timestamp").asText()).getOffset())
        .isEqualTo(ZoneOffset.ofHours(-3));
    assertThat(body).doesNotContain(SECRET_MESSAGE);
  }

  @Test
  void shouldWriteForbiddenProblem_whenAccessIsDenied() throws Exception {
    // Given
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users");
    request.setUserPrincipal(new TestingAuthenticationToken("3f6c1a2e", null, "ROLE_USER"));
    MockHttpServletResponse response = new MockHttpServletResponse();

    // When
    new ProblemDetailAccessDeniedHandler(objectMapper)
        .handle(request, response, new AccessDeniedException(SECRET_MESSAGE));

    // Then
    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(MediaType.parseMediaType(response.getContentType()))
        .isEqualTo(MediaType.parseMediaType("application/problem+json;charset=UTF-8"));
    String body = response.getContentAsString(StandardCharsets.UTF_8);
    JsonNode json = objectMapper.readTree(body);
    assertThat(json.get("status").asInt()).isEqualTo(403);
    assertThat(json.get("code").asText()).isEqualTo("ACCESS_DENIED");
    assertThat(json.get("detail").asText()).isEqualTo("Acesso negado.");
    assertThat(json.get("instance").asText()).isEqualTo("/api/v1/users");
    assertThat(body).doesNotContain(SECRET_MESSAGE);
  }

  @Test
  void shouldWriteForbiddenProblem_whenThereIsNoPrincipal() throws Exception {
    // Given
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users");
    MockHttpServletResponse response = new MockHttpServletResponse();

    // When
    new ProblemDetailAccessDeniedHandler(objectMapper)
        .handle(request, response, new AccessDeniedException(SECRET_MESSAGE));

    // Then
    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(objectMapper.readTree(response.getContentAsByteArray()).get("code").asText())
        .isEqualTo("ACCESS_DENIED");
  }

  // Same builder + customizer Boot applies, so the ProblemDetail mixin and the -03:00 re-zoning
  // are exercised exactly as in production.
  private static ObjectMapper bootLikeObjectMapper() {
    Jackson2ObjectMapperBuilder builder = Jackson2ObjectMapperBuilder.json();
    new JacksonConfig().applicationZoneIsoDates(ZoneId.of("America/Sao_Paulo")).customize(builder);
    return builder.build();
  }
}
