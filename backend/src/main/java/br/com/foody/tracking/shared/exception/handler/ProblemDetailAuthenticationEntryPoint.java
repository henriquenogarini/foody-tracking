package br.com.foody.tracking.shared.exception.handler;

import br.com.foody.tracking.shared.exception.model.ApiErrorCode;
import br.com.foody.tracking.shared.exception.type.InvalidTokenException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public final class ProblemDetailAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException)
            throws IOException {
        log.debug(
                "Unauthenticated request rejected on {} {}",
                request.getMethod(),
                request.getRequestURI());
        ProblemDetail problem =
                ProblemDetailFactory.create(
                        ApiErrorCode.AUTH_TOKEN_INVALID, InvalidTokenException.DETAIL);
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        ProblemDetailWriter.write(request, response, problem, objectMapper);
    }
}
