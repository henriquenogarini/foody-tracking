package br.com.foody.tracking.auth.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.foody.tracking.auth.application.AuthService;
import br.com.foody.tracking.auth.application.AuthenticatedUser;
import br.com.foody.tracking.auth.application.InvalidCredentialsException;
import br.com.foody.tracking.auth.application.InvalidTokenException;
import br.com.foody.tracking.auth.application.UserEmailAlreadyExistsException;
import br.com.foody.tracking.auth.application.dto.LoginRequest;
import br.com.foody.tracking.auth.application.dto.RegisterRequest;
import br.com.foody.tracking.auth.application.dto.TokenResponse;
import br.com.foody.tracking.auth.application.dto.UserResponse;
import br.com.foody.tracking.auth.domain.Role;
import br.com.foody.tracking.config.ClockConfig;
import br.com.foody.tracking.config.JacksonConfig;
import br.com.foody.tracking.config.JwtConfig;
import br.com.foody.tracking.config.SecurityConfig;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, JwtConfig.class, ClockConfig.class, JacksonConfig.class})
@ActiveProfiles("test")
class AuthControllerTest {

  private static final UUID USER_ID = UUID.fromString("3f6c1a2e-8d4b-4c7a-9e1f-2b5d7a9c0e11");
  // Stored in UTC; the JSON boundary must re-zone it to America/Sao_Paulo.
  private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-10-21T17:13:27Z");

  @Autowired private MockMvc mockMvc;

  @MockitoBean private AuthService authService;

  @Test
  void shouldReturnCreatedUser_whenRegisterRequestIsValid() throws Exception {
    // Given
    when(authService.register(any())).thenReturn(userResponse());

    // When / Then
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name": "Ana Souza", "email": "ana@exemplo.com", "password": "senha-forte-123"}
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(USER_ID.toString()))
        .andExpect(jsonPath("$.name").value("Ana Souza"))
        .andExpect(jsonPath("$.email").value("ana@exemplo.com"))
        .andExpect(jsonPath("$.role").value("USER"))
        .andExpect(jsonPath("$.createdAt").value("2026-10-21T14:13:27-03:00"))
        .andExpect(jsonPath("$.password").doesNotExist());

    ArgumentCaptor<RegisterRequest> request = ArgumentCaptor.forClass(RegisterRequest.class);
    verify(authService).register(request.capture());
    assertThat(request.getValue().email()).isEqualTo("ana@exemplo.com");
  }

  @Test
  void shouldReturnValidationProblem_whenRegisterFieldsAreInvalid() throws Exception {
    // When / Then
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name": " ", "email": "not-an-email", "password": "123"}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[*].field", hasItems("name", "email", "password")));

    verifyNoInteractions(authService);
  }

  @Test
  void shouldReturnConflictProblem_whenEmailAlreadyExists() throws Exception {
    // Given
    when(authService.register(any())).thenThrow(new UserEmailAlreadyExistsException());

    // When / Then
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name": "Ana Souza", "email": "ana@exemplo.com", "password": "senha-forte-123"}
                    """))
        .andExpect(status().isConflict())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.code").value("USER_EMAIL_ALREADY_EXISTS"));
  }

  @Test
  void shouldReturnToken_whenCredentialsAreValid() throws Exception {
    // Given
    when(authService.login(any(LoginRequest.class)))
        .thenReturn(TokenResponse.bearer("signed.jwt.token", 7200));

    // When / Then
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email": "ana@exemplo.com", "password": "senha-forte-123"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").value("signed.jwt.token"))
        .andExpect(jsonPath("$.tokenType").value("Bearer"))
        .andExpect(jsonPath("$.expiresIn").value(7200));
  }

  @Test
  void shouldReturnValidationProblem_whenLoginFieldsAreBlank() throws Exception {
    // When / Then
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email": "", "password": ""}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[*].field", hasItems("email", "password")));

    verifyNoInteractions(authService);
  }

  @Test
  void shouldReturnUnauthorizedProblem_whenCredentialsAreInvalid() throws Exception {
    // Given
    when(authService.login(any(LoginRequest.class))).thenThrow(new InvalidCredentialsException());

    // When / Then
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email": "ana@exemplo.com", "password": "wrong-password"}
                    """))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"));
  }

  @Test
  void shouldReturnCurrentUser_whenTokenIsValid() throws Exception {
    // Given
    when(authService.me(any())).thenReturn(userResponse());

    // When
    mockMvc
        .perform(
            get("/api/v1/auth/me")
                .with(jwt().jwt(token -> token.subject(USER_ID.toString()).claim("role", "USER"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(USER_ID.toString()))
        .andExpect(jsonPath("$.role").value("USER"));

    // Then
    ArgumentCaptor<AuthenticatedUser> principal = ArgumentCaptor.forClass(AuthenticatedUser.class);
    verify(authService).me(principal.capture());
    assertThat(principal.getValue()).isEqualTo(new AuthenticatedUser(USER_ID, Role.USER));
  }

  @Test
  void shouldReturnUnauthorizedProblem_whenMeIsCalledWithoutToken() throws Exception {
    // When / Then
    mockMvc
        .perform(get("/api/v1/auth/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("AUTH_TOKEN_INVALID"));

    verifyNoInteractions(authService);
  }

  @Test
  void shouldReturnUnauthorizedProblem_whenTokenUserNoLongerExists() throws Exception {
    // Given
    when(authService.me(any())).thenThrow(new InvalidTokenException());

    // When / Then
    mockMvc
        .perform(
            get("/api/v1/auth/me")
                .with(jwt().jwt(token -> token.subject(USER_ID.toString()).claim("role", "USER"))))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.code").value("AUTH_TOKEN_INVALID"))
        .andExpect(jsonPath("$.detail").value(InvalidTokenException.DETAIL))
        .andExpect(jsonPath("$.instance").value("/api/v1/auth/me"));
  }

  private static UserResponse userResponse() {
    return new UserResponse(USER_ID, "Ana Souza", "ana@exemplo.com", Role.USER, CREATED_AT);
  }
}
