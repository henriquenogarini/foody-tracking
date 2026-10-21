package br.com.foody.tracking.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.foody.tracking.auth.domain.Role;
import br.com.foody.tracking.auth.domain.User;
import br.com.foody.tracking.auth.domain.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapTest {

  private static final ZoneId APP_ZONE = ZoneId.of("America/Sao_Paulo");
  private static final Instant NOW = Instant.parse("2026-10-21T17:13:27Z");
  private static final String PASSWORD = "admin-password";

  @Mock private UserRepository userRepository;

  private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

  private final ApplicationArguments args = mock(ApplicationArguments.class);

  @Test
  void shouldSkip_whenEmailAndPasswordAreAbsent() {
    // When
    bootstrap(null, null).run(args);

    // Then
    verifyNoInteractions(userRepository);
  }

  @Test
  void shouldSkip_whenEmailAndPasswordAreBlank() {
    // When
    bootstrap("", "  ").run(args);

    // Then
    verifyNoInteractions(userRepository);
  }

  @Test
  void shouldFailStartup_whenOnlyEmailIsSet() {
    // When / Then
    assertThatThrownBy(() -> bootstrap("admin@foody.local", "").run(args))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("must be set together");
    verifyNoInteractions(userRepository);
  }

  @Test
  void shouldFailStartup_whenOnlyPasswordIsSet() {
    // When / Then
    assertThatThrownBy(() -> bootstrap(" ", PASSWORD).run(args))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("must be set together");
  }

  @Test
  void shouldFailStartup_whenPasswordIsTooShort() {
    // When / Then
    assertThatThrownBy(() -> bootstrap("admin@foody.local", "short").run(args))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("at least 8 characters");
    verifyNoInteractions(userRepository);
  }

  @Test
  void shouldFailStartup_whenPasswordExceedsBcryptByteLimit() {
    // When / Then
    assertThatThrownBy(() -> bootstrap("admin@foody.local", "é".repeat(37)).run(args))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void shouldNotPromote_whenEmailBelongsToExistingUser() {
    // Given
    User squatter = existing(Role.USER);
    when(userRepository.findByEmail("admin@foody.local")).thenReturn(Optional.of(squatter));

    // When
    bootstrap("Admin@Foody.local", PASSWORD).run(args);

    // Then
    assertThat(squatter.getRole()).isEqualTo(Role.USER);
    verify(userRepository, never()).save(any());
  }

  @Test
  void shouldSkip_whenAdminAlreadyExists() {
    // Given
    when(userRepository.findByEmail("admin@foody.local"))
        .thenReturn(Optional.of(existing(Role.ADMIN)));

    // When
    bootstrap("admin@foody.local", PASSWORD).run(args);

    // Then
    verify(userRepository, never()).save(any());
  }

  @Test
  void shouldCreateAdminWithNormalizedEmail_whenEmailIsNew() {
    // Given
    when(userRepository.findByEmail("admin@foody.local")).thenReturn(Optional.empty());
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // When
    bootstrap("  Admin@Foody.LOCAL ", PASSWORD).run(args);

    // Then
    ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(saved.capture());
    User admin = saved.getValue();
    assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
    assertThat(admin.getEmail()).isEqualTo("admin@foody.local");
    assertThat(passwordEncoder.matches(PASSWORD, admin.getPasswordHash())).isTrue();
    assertThat(admin.getCreatedAt().toInstant()).isEqualTo(NOW);
  }

  private AdminBootstrap bootstrap(String email, String password) {
    return new AdminBootstrap(
        email, password, userRepository, passwordEncoder, Clock.fixed(NOW, APP_ZONE));
  }

  private static User existing(Role role) {
    return new User(
        "Someone", "admin@foody.local", "hash", role, OffsetDateTime.ofInstant(NOW, APP_ZONE));
  }
}
