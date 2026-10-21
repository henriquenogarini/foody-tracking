package br.com.foody.tracking.shared.exception.handler;

import br.com.foody.tracking.shared.exception.model.ApiErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

// Answers 403 in the contract's ProblemDetail format; the exception message is never exposed.
public final class ProblemDetailAccessDeniedHandler implements AccessDeniedHandler {

  static final String DETAIL = "Acesso negado.";

  private static final Logger log = LoggerFactory.getLogger(ProblemDetailAccessDeniedHandler.class);

  private final ObjectMapper objectMapper;

  public ProblemDetailAccessDeniedHandler(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException {
    // The principal name is the JWT subject (the user's publicId), never the e-mail.
    Principal principal = request.getUserPrincipal();
    log.warn(
        "Access denied on {} {} for {}",
        request.getMethod(),
        request.getRequestURI(),
        principal == null ? "anonymous" : principal.getName());
    ProblemDetail problem = ProblemDetailFactory.create(ApiErrorCode.ACCESS_DENIED, DETAIL);
    problem.setInstance(URI.create(request.getRequestURI()));
    response.setStatus(problem.getStatus());
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    objectMapper.writeValue(response.getOutputStream(), problem);
  }
}
