package br.com.foody.tracking.shared.exception.handler;

import br.com.foody.tracking.shared.exception.model.ApiErrorCode;
import br.com.foody.tracking.shared.exception.type.InvalidTokenException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

// 401 in the contract format. Fixed detail: the exception message carries Nimbus parsing text
// that must not reach the client.
public final class ProblemDetailAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private static final Logger log =
      LoggerFactory.getLogger(ProblemDetailAuthenticationEntryPoint.class);

  private final ObjectMapper objectMapper;

  public ProblemDetailAuthenticationEntryPoint(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {
    log.debug(
        "Unauthenticated request rejected on {} {}", request.getMethod(), request.getRequestURI());
    ProblemDetail problem =
        ProblemDetailFactory.create(ApiErrorCode.AUTH_TOKEN_INVALID, InvalidTokenException.DETAIL);
    response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
    ProblemDetailWriter.write(request, response, problem, objectMapper);
  }
}
