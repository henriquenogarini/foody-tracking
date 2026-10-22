package br.com.foody.tracking.modules.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.foody.tracking.modules.auth.dto.LoginRequestDTO;
import br.com.foody.tracking.modules.auth.dto.PasswordResetRequestDTO;
import br.com.foody.tracking.modules.auth.dto.RegisterRequestDTO;
import br.com.foody.tracking.modules.auth.dto.TokenResponseDTO;
import br.com.foody.tracking.modules.auth.dto.UserResponseDTO;
import br.com.foody.tracking.modules.auth.entity.User;
import br.com.foody.tracking.modules.auth.enums.Role;
import br.com.foody.tracking.modules.auth.mapper.UserMapper;
import br.com.foody.tracking.modules.auth.repository.UserRepository;
import br.com.foody.tracking.modules.auth.security.AuthenticatedUser;
import br.com.foody.tracking.modules.auth.security.TokenService;
import br.com.foody.tracking.shared.exception.model.ApiErrorCode;
import br.com.foody.tracking.shared.exception.type.ApiException;
import br.com.foody.tracking.shared.exception.type.InvalidCredentialsException;
import br.com.foody.tracking.shared.exception.type.UserEmailAlreadyExistsException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  private static final ZoneId APP_ZONE = ZoneId.of("America/Sao_Paulo");
  private static final Instant NOW = Instant.parse("2026-10-21T17:13:27Z");
  private static final String PASSWORD = "senha-forte-123";
  private static final String NEW_PASSWORD = "nova-senha-456";

  @Mock private UserRepository userRepository;

  @Mock private TokenService tokenService;

  // Real BCrypt (low cost for speed) so hashing and matching behave as in production.
  private final PasswordEncoder passwordEncoder = spy(new BCryptPasswordEncoder(4));

  private AuthService authService;

  @BeforeEach
  void setUp() {
    authService =
        new AuthService(
            userRepository,
            passwordEncoder,
            tokenService,
            Mappers.getMapper(UserMapper.class),
            Clock.fixed(NOW, APP_ZONE));
  }

  @Test
  void shouldCreateUserWithNormalizedEmailAndHashedPassword_whenRegistering() {
    // Given
    RegisterRequestDTO request =
        new RegisterRequestDTO("Ana Souza", "  Ana@Exemplo.COM ", PASSWORD);
    when(userRepository.existsByEmail("ana@exemplo.com")).thenReturn(false);
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // When
    UserResponseDTO response = authService.register(request);

    // Then
    ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(saved.capture());
    User user = saved.getValue();
    assertThat(user.getEmail()).isEqualTo("ana@exemplo.com");
    assertThat(user.getRole()).isEqualTo(Role.USER);
    assertThat(user.getPasswordHash()).isNotEqualTo(PASSWORD);
    assertThat(passwordEncoder.matches(PASSWORD, user.getPasswordHash())).isTrue();
    assertThat(response.id()).isEqualTo(user.getPublicId());
    assertThat(response.email()).isEqualTo("ana@exemplo.com");
    assertThat(response.role()).isEqualTo(Role.USER);
    assertThat(response.createdAt().toInstant()).isEqualTo(NOW);
  }

  @Test
  void shouldRejectRegistration_whenEmailAlreadyExists() {
    // Given
    RegisterRequestDTO request = new RegisterRequestDTO("Ana Souza", "ANA@exemplo.com", PASSWORD);
    when(userRepository.existsByEmail("ana@exemplo.com")).thenReturn(true);

    // When / Then
    assertThatThrownBy(() -> authService.register(request))
        .isInstanceOf(UserEmailAlreadyExistsException.class);
    verify(userRepository, never()).save(any());
  }

  @Test
  void shouldAlwaysRegisterAsUser_whenBodyTriesToChooseAdminRole() throws Exception {
    // Given: unknown properties are ignored, as in Boot's ObjectMapper
    JsonMapper mapper =
        JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
    RegisterRequestDTO request =
        mapper.readValue(
            """
            {"name": "Eve", "email": "eve@exemplo.com", "password": "senha-forte-123",
             "role": "ADMIN"}
            """,
            RegisterRequestDTO.class);
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // When
    UserResponseDTO response = authService.register(request);

    // Then
    assertThat(response.role()).isEqualTo(Role.USER);
  }

  @Test
  void shouldIssueToken_whenCredentialsAreValid() {
    // Given
    User user = existingUser(passwordEncoder.encode(PASSWORD));
    TokenResponseDTO token = TokenResponseDTO.bearer("signed.jwt", 7200);
    when(userRepository.findByEmail("ana@exemplo.com")).thenReturn(Optional.of(user));
    when(tokenService.issue(user)).thenReturn(token);

    // When
    TokenResponseDTO response =
        authService.login(new LoginRequestDTO(" ANA@exemplo.com", PASSWORD));

    // Then
    assertThat(response).isEqualTo(token);
  }

  @Test
  void shouldFailWithSameErrorAndStillHash_whenEmailIsUnknownOrPasswordIsWrong() {
    // Given
    User user = existingUser(passwordEncoder.encode(PASSWORD));
    when(userRepository.findByEmail("ghost@exemplo.com")).thenReturn(Optional.empty());
    when(userRepository.findByEmail("ana@exemplo.com")).thenReturn(Optional.of(user));

    // When
    Throwable unknownEmail =
        catchThrowable(() -> authService.login(new LoginRequestDTO("ghost@exemplo.com", PASSWORD)));
    Throwable wrongPassword =
        catchThrowable(
            () -> authService.login(new LoginRequestDTO("ana@exemplo.com", "senha-errada-123")));

    // Then
    assertThat(unknownEmail).isInstanceOf(InvalidCredentialsException.class);
    assertThat(wrongPassword).isInstanceOf(InvalidCredentialsException.class);
    assertThat(unknownEmail).hasMessage(wrongPassword.getMessage());
    verify(passwordEncoder).matches(eq(PASSWORD), anyString());
    verifyNoInteractions(tokenService);
  }

  @Test
  void shouldRejectWithoutHashing_whenPasswordExceedsBcryptByteLimit() {
    // Given: 37 two-byte characters = 74 UTF-8 bytes
    LoginRequestDTO request = new LoginRequestDTO("ana@exemplo.com", "é".repeat(37));

    // When / Then
    assertThatThrownBy(() -> authService.login(request))
        .isInstanceOf(InvalidCredentialsException.class);
    verify(passwordEncoder, never()).matches(any(), any());
    verifyNoInteractions(userRepository, tokenService);
  }

  // The service cannot tell the three apart (the USER-only filter is in the query): it must hash
  // exactly once on each, so timing is the same whatever the e-mail belongs to.
  @ParameterizedTest(name = "{0}")
  @CsvSource({"existing USER, 1", "unknown e-mail, 0", "ADMIN e-mail, 0"})
  void shouldHashOnceAndUpdateUserOnly_whenResettingPassword(String path, int affectedRows) {
    // Given
    clearInvocations(passwordEncoder);
    when(userRepository.updatePasswordHashOfUser(eq("ana@exemplo.com"), anyString()))
        .thenReturn(affectedRows);

    // When
    authService.resetPassword(new PasswordResetRequestDTO(" Ana@Exemplo.COM ", NEW_PASSWORD));

    // Then
    verify(passwordEncoder, times(1)).encode(any());
    ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
    verify(userRepository).updatePasswordHashOfUser(eq("ana@exemplo.com"), hash.capture());
    assertThat(passwordEncoder.matches(NEW_PASSWORD, hash.getValue())).isTrue();
  }

  @Test
  void shouldReturnCurrentUser_whenTokenSubjectExists() {
    // Given
    User user = existingUser("hash");
    when(userRepository.findByPublicId(user.getPublicId())).thenReturn(Optional.of(user));

    // When
    UserResponseDTO response = authService.me(new AuthenticatedUser(user.getPublicId(), Role.USER));

    // Then
    assertThat(response.id()).isEqualTo(user.getPublicId());
    assertThat(response.name()).isEqualTo("Ana Souza");
  }

  @Test
  void shouldTreatTokenAsInvalid_whenTokenSubjectNoLongerExists() {
    // Given
    UUID unknown = UUID.randomUUID();
    when(userRepository.findByPublicId(unknown)).thenReturn(Optional.empty());

    // When
    Throwable thrown =
        catchThrowable(() -> authService.me(new AuthenticatedUser(unknown, Role.USER)));

    // Then
    assertThat(thrown).isInstanceOf(ApiException.class);
    assertThat(((ApiException) thrown).getCode()).isEqualTo(ApiErrorCode.AUTH_TOKEN_INVALID);
  }

  private static User existingUser(String passwordHash) {
    return new User(
        "Ana Souza",
        "ana@exemplo.com",
        passwordHash,
        Role.USER,
        OffsetDateTime.ofInstant(NOW, APP_ZONE));
  }
}
