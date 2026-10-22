package br.com.foody.tracking.shared.exception.handler;

import br.com.foody.tracking.shared.exception.model.ApiErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

// 403 in the contract format; the exception message is never exposed.
@Slf4j
@RequiredArgsConstructor
public final class ProblemDetailAccessDeniedHandler implements AccessDeniedHandler {

  private static final String DETAIL = "Acesso negado.";

  private final ObjectMapper objectMapper;

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
    ProblemDetailWriter.write(request, response, problem, objectMapper);
  }
}
