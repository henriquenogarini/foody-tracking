package br.com.foody.tracking.config;

import br.com.foody.tracking.modules.auth.entity.User;
import br.com.foody.tracking.modules.auth.enums.Role;
import br.com.foody.tracking.modules.auth.security.TokenService;
import br.com.foody.tracking.shared.exception.type.InvalidTokenException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.Clock;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityConfigTest.ProbeController.class)
@Import({
    SecurityConfig.class,
    JwtConfig.class,
    ClockConfig.class,
    JacksonConfig.class,
    SecurityConfigTest.ProbeController.class
})
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private JwtEncoder jwtEncoder;

    @Autowired private JwtProperties jwtProperties;

    @Autowired private Clock clock;

    @Test
    void shouldReturnUnauthorizedProblem_whenTokenIsMissing() throws Exception {
        // When / Then
        mockMvc.perform(get("/api/v1/probe"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("AUTH_TOKEN_INVALID"))
                .andExpect(jsonPath("$.instance").value("/api/v1/probe"));
    }

    @Test
    void shouldReturnUnauthorizedProblemWithoutParserDetails_whenTokenIsMalformed()
            throws Exception {
        // When
        String body =
                mockMvc.perform(
                                get("/api/v1/probe")
                                        .header(HttpHeaders.AUTHORIZATION, "Bearer not.a.jwt"))
                        .andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.code").value("AUTH_TOKEN_INVALID"))
                        .andExpect(jsonPath("$.detail").value(InvalidTokenException.DETAIL))
                        .andReturn()
                        .getResponse()
                        .getContentAsString(StandardCharsets.UTF_8);

        // Then
        assertThat(body).doesNotContainIgnoringCase("malformed").doesNotContain("Nimbus");
    }

    @Test
    void shouldAuthenticateWithAdminAuthority_whenTokenIssuedByTokenServiceCarriesAdminRole()
            throws Exception {
        // Given
        User admin =
                new User(
                        "Admin", "admin@foody.test", "hash", Role.ADMIN, OffsetDateTime.now(clock));
        String token =
                new TokenService(jwtEncoder, clock, jwtProperties).issue(admin).accessToken();

        // When / Then
        mockMvc.perform(
                        get("/api/v1/probe/admin")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string(admin.getPublicId().toString()));
    }

    @Test
    void shouldReturnForbiddenProblem_whenUserCallsAdminOnlyEndpoint() throws Exception {
        // When / Then
        mockMvc.perform(
                        get("/api/v1/probe/admin")
                                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.detail").value("Acesso negado."))
                .andExpect(jsonPath("$.instance").value("/api/v1/probe/admin"));
    }

    @Test
    void shouldAllowAdminOnlyEndpoint_whenCallerHasAdminRole() throws Exception {
        // When / Then
        mockMvc.perform(
                        get("/api/v1/probe/admin")
                                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    void shouldIgnoreStaleBearerHeader_whenRouteIsPublicLogin() throws Exception {
        // When / Then
        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer expired.or.bad"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldIgnoreStaleBearerHeader_whenRouteIsPublicRegister() throws Exception {
        // When / Then
        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer expired.or.bad"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRequireAuthentication_whenPublicPathIsCalledWithAnotherMethod() throws Exception {
        // When / Then
        mockMvc.perform(get("/api/v1/auth/login")).andExpect(status().isUnauthorized());
    }

    // The test profile does not override app.cors.allowed-origins, so this pins the default:
    // the Vite dev server (5173) and `vite preview` (4173).
    @ParameterizedTest
    @ValueSource(strings = {"http://localhost:5173", "http://localhost:4173"})
    void shouldAllowDefaultOrigin_whenBrowserSendsPreflight(String origin) throws Exception {
        // When / Then
        mockMvc.perform(
                        options("/api/v1/probe")
                                .header(HttpHeaders.ORIGIN, origin)
                                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PATCH"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin));
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://evil.example", "http://localhost:3000"})
    void shouldRejectUnlistedOrigin_whenBrowserSendsPreflight(String origin) throws Exception {
        // When / Then
        mockMvc.perform(
                        options("/api/v1/probe")
                                .header(HttpHeaders.ORIGIN, origin)
                                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }

    @RestController
    static class ProbeController {

        @GetMapping("/api/v1/probe")
        String probe() {
            return "ok";
        }

        @GetMapping("/api/v1/probe/admin")
        @PreAuthorize("hasRole('ADMIN')")
        String adminProbe(Principal principal) {
            return principal.getName();
        }

        @PostMapping("/api/v1/auth/login")
        String login() {
            return "public";
        }

        @PostMapping("/api/v1/auth/register")
        String register() {
            return "public";
        }
    }
}
