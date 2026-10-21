package br.com.foody.tracking.modules.auth.service;

import br.com.foody.tracking.modules.auth.entity.User;
import br.com.foody.tracking.modules.auth.enums.Role;
import br.com.foody.tracking.modules.auth.helper.EmailNormalizer;
import br.com.foody.tracking.modules.auth.repository.UserRepository;
import br.com.foody.tracking.shared.validation.MaxUtf8Bytes;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// Provisions the single ADMIN at startup; the API never creates or promotes one.
@Component
@Order(AdminBootstrap.RUNNER_ORDER)
public final class AdminBootstrap implements ApplicationRunner {

  /** Runners that need the admin (the dev order seeder) declare a higher order. */
  public static final int RUNNER_ORDER = 0;

  private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

  private static final String ADMIN_NAME = "Admin";
  private static final int MIN_PASSWORD_LENGTH = 8;

  private final String email;
  private final String password;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final Clock clock;

  public AdminBootstrap(
      @Value("${app.security.admin.email:}") String email,
      @Value("${app.security.admin.password:}") String password,
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      Clock clock) {
    this.email = email;
    this.password = password;
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.clock = clock;
  }

  @Override
  public void run(ApplicationArguments args) {
    // Blank counts as absent: the .env.example lines are `KEY=`.
    boolean hasEmail = email != null && !email.isBlank();
    boolean hasPassword = password != null && !password.isBlank();
    if (!hasEmail && !hasPassword) {
      log.warn(
          "No bootstrap admin configured (APP_SECURITY_ADMIN_EMAIL/PASSWORD absent); skipping");
      return;
    }
    if (hasEmail != hasPassword) {
      throw new IllegalStateException(
          "APP_SECURITY_ADMIN_EMAIL and APP_SECURITY_ADMIN_PASSWORD must be set together");
    }
    if (password.length() < MIN_PASSWORD_LENGTH
        || password.getBytes(StandardCharsets.UTF_8).length > MaxUtf8Bytes.BCRYPT_MAX_BYTES) {
      throw new IllegalStateException(
          "APP_SECURITY_ADMIN_PASSWORD must have at least 8 characters and at most 72 bytes");
    }

    String normalizedEmail = EmailNormalizer.normalize(email);
    Optional<User> existing = userRepository.findByEmail(normalizedEmail);
    if (existing.isPresent()) {
      User user = existing.get();
      // Never promote: anyone could have pre-registered the admin e-mail through the API.
      if (user.getRole() == Role.ADMIN) {
        log.info("Bootstrap admin {} already exists", user.getPublicId());
      } else {
        log.warn("Bootstrap admin e-mail belongs to USER {}; not promoting it", user.getPublicId());
      }
      return;
    }

    User admin =
        userRepository.save(
            new User(
                ADMIN_NAME,
                normalizedEmail,
                passwordEncoder.encode(password),
                Role.ADMIN,
                OffsetDateTime.now(clock)));
    log.info("Bootstrap admin {} created", admin.getPublicId());
  }
}
