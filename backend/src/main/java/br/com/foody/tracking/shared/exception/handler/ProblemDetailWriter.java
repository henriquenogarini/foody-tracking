package br.com.foody.tracking.shared.exception.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;

final class ProblemDetailWriter {

  private ProblemDetailWriter() {}

  static void write(
      HttpServletRequest request,
      HttpServletResponse response,
      ProblemDetail problem,
      ObjectMapper objectMapper)
      throws IOException {
    problem.setInstance(URI.create(request.getRequestURI()));
    response.setStatus(problem.getStatus());
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    objectMapper.writeValue(response.getOutputStream(), problem);
  }
}
